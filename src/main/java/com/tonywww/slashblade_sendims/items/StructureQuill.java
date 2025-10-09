package com.tonywww.slashblade_sendims.items;

import com.mojang.datafixers.util.Pair;
import com.tonywww.slashblade_sendims.registeries.SBSDItems;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.minecraft.ChatFormatting;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheckResult;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// 部分参照Supplementaries
public class StructureQuill extends Item {

    public static final String TAG_STRUCTURE = "targetStructure";
    public static final String TAG_SKIP_KNOWN = "skipKnown";
    public static final String TAG_SEARCH_RADIUS = "maxSearchRadius";
    public static final String TAG_ZOOM = "zoomLevel";
    public static final String TAG_DECORATION = "decoration";
    public static final String TAG_NAME = "name";
    public static final String TAG_IS_SEARCHING = "isSearching";
    public static final String TAG_SOURCE_X = "sourceX";
    public static final String TAG_SOURCE_Z = "sourceZ";
    private static final ExecutorService EXECUTORS = Executors.newFixedThreadPool(2);

    public StructureQuill(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltips, TooltipFlag flag) {
        var tag = stack.getTag();
        if (tag != null) {
            if (tag.getBoolean(TAG_IS_SEARCHING)) {
                tooltips.add(Component.translatable("message.slashblade_sendims.structure_quill.searching").withStyle(ChatFormatting.BLUE));
            }
            tooltips.add(Component.translatable("message.slashblade_sendims.structure_quill.tooltip").withStyle(ChatFormatting.AQUA));
            if (tag.contains(TAG_STRUCTURE)) {
                tooltips.add(Component.literal(tag.getString(TAG_STRUCTURE)).withStyle(ChatFormatting.AQUA));

            }
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level instanceof ServerLevel serverLevel) {
            CompoundTag tag = stack.getOrCreateTag();

            // 如果正在搜索，返回
            if (tag.getBoolean(TAG_IS_SEARCHING)) {
                return InteractionResultHolder.pass(stack);
            }

            // 如果没有目标结构，随机选择一个
            if (!tag.contains(TAG_STRUCTURE)) {
                tag.putString(TAG_STRUCTURE, "minecraft:stronghold");
            }

            // 开始搜索
            return startSearch(stack, serverLevel, player, hand);
        }

        return InteractionResultHolder.pass(stack);
    }

    private InteractionResultHolder<ItemStack> startSearch(ItemStack stack, ServerLevel level, Player player, InteractionHand hand) {
        CompoundTag tag = stack.getOrCreateTag();
        String structureName = tag.getString(TAG_STRUCTURE);

        if (structureName.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.slashblade_sendims.structure_quill.no_target"), true);
            return InteractionResultHolder.fail(stack);
        }

        ResourceLocation structureKey = ResourceLocation.parse(structureName);
        Holder<Structure> structureHolder = getStructureHolder(level, structureKey);

        if (structureHolder == null) {
            player.displayClientMessage(Component.translatable("message.slashblade_sendims.structure_quill.invalid_structure"), true);
            return InteractionResultHolder.fail(stack);
        }

        tag.putBoolean(TAG_IS_SEARCHING, true);
        BlockPos startPos = getOrCreateStartPos(tag, player);
        int searchRadius = getSearchRadius(tag);
        boolean skipKnown = getSkipKnown(tag);

        // 在异步线程中搜索
        EXECUTORS.submit(() -> {
            try {
                var result = findNearestStructure(level, structureHolder, searchRadius, startPos, skipKnown);

                // 在主线程中处理结果
                level.getServer().execute(() -> {
                    tag.putBoolean(TAG_IS_SEARCHING, false);

                    if (result != null && result.getResult() == InteractionResult.SUCCESS) {
                        BlockPos foundPos = result.getObject();
                        ItemStack mapStack = createMap(level, foundPos, structureKey, stack);

                        // 替换物品
                        if (hand == InteractionHand.MAIN_HAND) {
                            player.setItemInHand(InteractionHand.MAIN_HAND, mapStack);
                        } else {
                            player.setItemInHand(InteractionHand.OFF_HAND, mapStack);
                        }

                        player.displayClientMessage(Component.translatable("message.slashblade_sendims.structure_quill.success"), true);
                    } else {
                        player.displayClientMessage(Component.translatable("message.slashblade_sendims.structure_quill.failed"), true);
                    }
                });
            } catch (Exception e) {
                level.getServer().execute(() -> {
                    tag.putBoolean(TAG_IS_SEARCHING, false);
                    player.displayClientMessage(Component.translatable("message.slashblade_sendims.structure_quill.error"), true);
                });
            }
        });

        player.displayClientMessage(Component.translatable("message.slashblade_sendims.structure_quill.start_search"), true);
        return InteractionResultHolder.success(stack);
    }

    @Nullable
    private Holder<Structure> getStructureHolder(ServerLevel level, ResourceLocation key) {
        Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        var structure = registry.getHolder(ResourceKey.create(registry.key(), key));
        return structure.orElse(null);
    }

    private ItemStack createMap(ServerLevel level, BlockPos targetPos, ResourceLocation structure, ItemStack original) {
        CompoundTag tag = original.getOrCreateTag();

        // 创建地图
        ItemStack mapStack = MapItem.create(level, targetPos.getX(), targetPos.getZ(), getZoomLevel(tag), true, true);
        MapItem.renderBiomePreviewMap(level, mapStack);

        // 添加结构标记
        MapItemSavedData mapData = MapItem.getSavedData(mapStack, level);
        if (mapData != null) {
            String mapName = getMapName(tag);
            if (mapName != null) {
                mapStack.setHoverName(Component.literal(mapName));
            }

            // 添加结构装饰标记
            MapDecoration.Type decoration = getDecorationType(tag);
            if (decoration != null) {
                mapData.addDecoration(decoration, level, "structure", targetPos.getX(), targetPos.getZ(), 180.0, null);
            }
        }

        return mapStack;
    }

    @Nullable
    private InteractionResultHolder<BlockPos> findNearestStructure(ServerLevel level, Holder<Structure> holder,
                                                                   int searchRadius, BlockPos center, boolean skipKnownStructures) {
        if (!level.getServer().getWorldData().worldGenOptions().generateStructures()) {
            return null;
        }

        ServerChunkCache chunkSource = level.getChunkSource();
        ChunkGenerator generator = chunkSource.getGenerator();
        ChunkGeneratorStructureState structureState = chunkSource.getGeneratorState();

        Map<StructurePlacement, Set<Holder<Structure>>> placementMap = new Object2ObjectArrayMap<>();

        for (StructurePlacement placement : structureState.getPlacementsForStructure(holder)) {
            placementMap.computeIfAbsent(placement, (p) -> new ObjectArraySet<>()).add(holder);
        }

        if (placementMap.isEmpty()) {
            return InteractionResultHolder.fail(BlockPos.ZERO);
        }

        StructureManager structureManager = level.structureManager();

        // 处理同心环结构放置
        for (Map.Entry<StructurePlacement, Set<Holder<Structure>>> entry : placementMap.entrySet()) {
            StructurePlacement placement = entry.getKey();
            if (placement instanceof ConcentricRingsStructurePlacement concentricRings) {
                Pair<BlockPos, Holder<Structure>> pair = generator.getNearestGeneratedStructure(
                        entry.getValue(), level, structureManager, center, skipKnownStructures, concentricRings);
                if (pair != null) {
                    return InteractionResultHolder.success(pair.getFirst());
                }
            }
        }

        // 处理随机分布结构放置
        List<Pair<RandomSpreadStructurePlacement, Set<Holder<Structure>>>> randomPlacements = new ArrayList<>();
        for (Map.Entry<StructurePlacement, Set<Holder<Structure>>> entry : placementMap.entrySet()) {
            if (entry.getKey() instanceof RandomSpreadStructurePlacement randomPlacement) {
                randomPlacements.add(Pair.of(randomPlacement, entry.getValue()));
            }
        }

        if (randomPlacements.isEmpty()) {
            return null;
        }

        return searchRandomSpreadStructures(level, randomPlacements, center, searchRadius, skipKnownStructures);
    }

    private InteractionResultHolder<BlockPos> searchRandomSpreadStructures(ServerLevel level,
                                                                           List<Pair<RandomSpreadStructurePlacement, Set<Holder<Structure>>>> placements,
                                                                           BlockPos center, int searchRadius, boolean skipKnownStructures) {
        int centerX = SectionPos.blockToSectionCoord(center.getX());
        int centerZ = SectionPos.blockToSectionCoord(center.getZ());
        long seed = level.getSeed();

        double closestDistance = Double.MAX_VALUE;
        BlockPos closestPos = null;

        for (int radius = 0; radius <= searchRadius; radius++) {
            for (var placementPair : placements) {
                RandomSpreadStructurePlacement placement = placementPair.getFirst();
                Set<Holder<Structure>> structures = placementPair.getSecond();
                int spacing = placement.spacing();

                for (int x = -radius; x <= radius; x++) {
                    boolean onEdgeX = x == -radius || x == radius;

                    for (int z = -radius; z <= radius; z++) {
                        boolean onEdgeZ = z == -radius || z == radius;
                        if (radius == 0 || onEdgeX || onEdgeZ) {
                            int testX = centerX + spacing * x;
                            int testZ = centerZ + spacing * z;
                            ChunkPos chunkPos = placement.getPotentialStructureChunk(seed, testX, testZ);

                            BlockPos foundPos = checkStructureAt(level, structures, placement, chunkPos, skipKnownStructures);
                            if (foundPos != null) {
                                double distance = center.distSqr(foundPos);
                                if (distance < closestDistance) {
                                    closestDistance = distance;
                                    closestPos = foundPos;
                                }
                            }
                        }
                    }
                }
            }

            if (closestPos != null) {
                return InteractionResultHolder.success(closestPos);
            }
        }

        return InteractionResultHolder.fail(BlockPos.ZERO);
    }

    @Nullable
    private BlockPos checkStructureAt(ServerLevel level, Set<Holder<Structure>> structures,
                                      StructurePlacement placement, ChunkPos chunkPos, boolean skipKnownStructures) {
        ServerChunkCache chunkCache = level.getChunkSource();
        StructureManager structureManager = level.structureManager();

        for (Holder<Structure> holder : structures) {
            Structure structure = holder.value();

            StructureCheckResult checkResult = structureManager.checkStructurePresence(chunkPos, structure, skipKnownStructures);
            if (checkResult == StructureCheckResult.START_NOT_PRESENT) {
                continue;
            }

            if (!skipKnownStructures && checkResult == StructureCheckResult.START_PRESENT) {
                return placement.getLocatePos(chunkPos);
            }

            ChunkAccess chunkAccess = chunkCache.getChunk(chunkPos.x, chunkPos.z, ChunkStatus.STRUCTURE_STARTS, true);
            if (chunkAccess == null) continue;

            StructureStart structureStart = structureManager.getStartForStructure(
                    SectionPos.bottomOf(chunkAccess), structure, chunkAccess);

            if (structureStart != null && structureStart.isValid()) {
                if (!skipKnownStructures || structureStart.canBeReferenced()) {
                    if (structureStart.canBeReferenced()) {
                        structureManager.addReference(structureStart);
                    }
                    return placement.getLocatePos(structureStart.getChunkPos());
                }
            }
        }

        return null;
    }

    private BlockPos getOrCreateStartPos(CompoundTag tag, Player player) {
        if (tag.contains(TAG_SOURCE_X) && tag.contains(TAG_SOURCE_Z)) {
            int sourceX = tag.getInt(TAG_SOURCE_X);
            int sourceZ = tag.getInt(TAG_SOURCE_Z);
            return new BlockPos(sourceX, 64, sourceZ);
        } else {
            BlockPos pos = player.blockPosition();
            tag.putInt(TAG_SOURCE_X, pos.getX());
            tag.putInt(TAG_SOURCE_Z, pos.getZ());
            return pos;
        }
    }

    private int getSearchRadius(CompoundTag tag) {
        return tag.contains(TAG_SEARCH_RADIUS) ? tag.getInt(TAG_SEARCH_RADIUS) : 5000;
    }

    private boolean getSkipKnown(CompoundTag tag) {
        return tag.contains(TAG_SKIP_KNOWN) && tag.getBoolean(TAG_SKIP_KNOWN);
    }

    private byte getZoomLevel(CompoundTag tag) {
        return tag.contains(TAG_ZOOM) ? tag.getByte(TAG_ZOOM) : 2;
    }

    @Nullable
    private String getMapName(CompoundTag tag) {
        return tag.contains(TAG_NAME) ? tag.getString(TAG_NAME) : null;
    }

    @Nullable
    private MapDecoration.Type getDecorationType(CompoundTag tag) {
        if (tag.contains(TAG_DECORATION)) {
            String decorationName = tag.getString(TAG_DECORATION);
            try {
                return MapDecoration.Type.valueOf(decorationName.toUpperCase());
            } catch (IllegalArgumentException e) {
                return MapDecoration.Type.RED_X;
            }
        }
        return MapDecoration.Type.RED_X;
    }

    // 静态工厂方法
    public static ItemStack forStructure(String structureName) {
        ItemStack stack = new ItemStack(SBSDItems.STRUCTURE_QUILL.get());
        stack.getOrCreateTag().putString(TAG_STRUCTURE, structureName);
        return stack;
    }

    public static ItemStack forStructure(String structureName, int searchRadius, boolean skipKnown,
                                         byte zoomLevel, String decorationType, String mapName) {
        ItemStack stack = forStructure(structureName);
        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt(TAG_SEARCH_RADIUS, searchRadius);
        tag.putBoolean(TAG_SKIP_KNOWN, skipKnown);
        tag.putByte(TAG_ZOOM, zoomLevel);
        if (decorationType != null) {
            tag.putString(TAG_DECORATION, decorationType);
        }
        if (mapName != null) {
            tag.putString(TAG_NAME, mapName);
        }
        return stack;
    }
}