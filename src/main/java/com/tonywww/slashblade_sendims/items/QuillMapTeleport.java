package com.tonywww.slashblade_sendims.items;

import com.tonywww.slashblade_sendims.SenDims;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

/**
 * 结构羽毛笔生成的成品地图：主手持地图右键传送到地图记录的结构位置（水平坐标取目标点，高度保持玩家当前高度）。
 * <ul>
 *     <li>水平距离 200 格以内：直接传送；</li>
 *     <li>200～3000 格：副手持末影珍珠，消耗 1 颗传送；</li>
 *     <li>3000 格以上：只能通过地图传送仪式（KubeJS：custom_spawn/map_teleport_ritual.js，调用 {@link #ritualTeleport}）。</li>
 * </ul>
 * 落点头部方块会被破坏以免窒息；头部是方块实体或不可破坏的方块时，依次向上、向下寻找最近的可用高度。
 */
@Mod.EventBusSubscriber(modid = SenDims.MOD_ID)
public final class QuillMapTeleport {
    public static final String TAG_TARGET = "sbsdQuillTarget";
    public static final double DIRECT_RANGE = 200.0D;
    public static final double PEARL_RANGE = 3000.0D;
    /** 头部为方块实体时，上下各最多移动的格数。 */
    public static final int SHIFT_LIMIT = 64;
    private static final int COOLDOWN_TICKS = 20;
    private static final String MSG = "message.slashblade_sendims.quill_map.";

    private QuillMapTeleport() {
    }

    public record Target(ResourceKey<Level> dimension, int x, int y, int z, String structure) {
    }

    public static void writeTarget(ItemStack map, ResourceKey<Level> dimension, BlockPos pos, ResourceLocation structure) {
        CompoundTag tag = new CompoundTag();
        tag.putString("dimension", dimension.location().toString());
        tag.putInt("x", pos.getX());
        tag.putInt("y", pos.getY());
        tag.putInt("z", pos.getZ());
        tag.putString("structure", structure.toString());
        map.getOrCreateTag().put(TAG_TARGET, tag);
    }

    @Nullable
    public static Target readTarget(ItemStack stack) {
        if (!stack.is(Items.FILLED_MAP)) {
            return null;
        }
        CompoundTag root = stack.getTag();
        if (root == null || !root.contains(TAG_TARGET, CompoundTag.TAG_COMPOUND)) {
            return null;
        }
        CompoundTag tag = root.getCompound(TAG_TARGET);
        ResourceLocation dimension = ResourceLocation.tryParse(tag.getString("dimension"));
        if (dimension == null) {
            return null;
        }
        return new Target(ResourceKey.create(Registries.DIMENSION, dimension),
                tag.getInt("x"), tag.getInt("y"), tag.getInt("z"), tag.getString("structure"));
    }

    public static double horizontalDistance(Entity entity, Target target) {
        double dx = entity.getX() - (target.x() + 0.5D);
        double dz = entity.getZ() - (target.z() + 0.5D);
        return Math.sqrt(dx * dx + dz * dz);
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        Target target = readTarget(event.getItemStack());
        if (target == null) {
            return;
        }
        Player player = event.getEntity();
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(player.level().isClientSide));
        if (!(player instanceof ServerPlayer serverPlayer) || serverPlayer.getCooldowns().isOnCooldown(Items.FILLED_MAP)) {
            return;
        }
        if (!serverPlayer.level().dimension().equals(target.dimension())) {
            serverPlayer.displayClientMessage(Component.translatable(MSG + "wrong_dimension").withStyle(ChatFormatting.RED), true);
            return;
        }
        double distance = horizontalDistance(serverPlayer, target);
        String shown = String.valueOf(Math.round(distance));
        if (distance <= DIRECT_RANGE) {
            teleport(serverPlayer, target);
        } else if (distance <= PEARL_RANGE) {
            ItemStack offhand = serverPlayer.getOffhandItem();
            if (!offhand.is(Items.ENDER_PEARL)) {
                serverPlayer.displayClientMessage(Component.translatable(MSG + "need_pearl", shown).withStyle(ChatFormatting.YELLOW), true);
                return;
            }
            if (teleport(serverPlayer, target) && !serverPlayer.getAbilities().instabuild) {
                offhand.shrink(1);
            }
        } else {
            serverPlayer.displayClientMessage(Component.translatable(MSG + "need_ritual", shown).withStyle(ChatFormatting.YELLOW), true);
        }
    }

    /**
     * 地图传送仪式的前置检查。返回 null 表示可以传送，否则返回提示的语言键。
     */
    @Nullable
    public static String checkRitual(ServerPlayer player, ItemStack map) {
        Target target = readTarget(map);
        if (target == null) {
            return MSG + "no_target";
        }
        if (!player.level().dimension().equals(target.dimension())) {
            return MSG + "wrong_dimension";
        }
        return null;
    }

    /** 地图传送仪式释放时调用：不检查距离，也不消耗任何物品（传送粉由仪式脚本扣除）。 */
    public static boolean ritualTeleport(ServerPlayer player, ItemStack map) {
        Target target = readTarget(map);
        if (target == null || !player.level().dimension().equals(target.dimension())) {
            return false;
        }
        return teleport(player, target);
    }

    public static boolean teleport(ServerPlayer player, Target target) {
        ServerLevel level = player.serverLevel();
        BlockPos feet = prepareDestination(level, target.x(), Mth.floor(player.getY()), target.z(), player);
        if (feet == null) {
            player.displayClientMessage(Component.translatable(MSG + "blocked").withStyle(ChatFormatting.RED), true);
            return false;
        }
        double x = feet.getX() + 0.5D, y = feet.getY(), z = feet.getZ() + 0.5D;
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
        player.teleportTo(level, x, y, z, player.getYRot(), player.getXRot());
        player.resetFallDistance();
        level.playSound(null, x, y, z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
        player.getCooldowns().addCooldown(Items.FILLED_MAP, COOLDOWN_TICKS);
        return true;
    }

    /**
     * 计算落点并清理头部方块：从 startY 开始，依次检查 startY、+1、-1、+2、-2……，
     * 取第一个头部不是方块实体、也不是不可破坏方块的高度，然后破坏头部的实体方块（带掉落）。
     * 找不到时返回 null。
     */
    @Nullable
    public static BlockPos prepareDestination(ServerLevel level, int x, int startY, int z, @Nullable Entity breaker) {
        level.getChunk(x >> 4, z >> 4);
        int minFeet = level.getMinBuildHeight();
        int maxFeet = level.getMaxBuildHeight() - 2;
        int base = Mth.clamp(startY, minFeet, maxFeet);
        for (int shift = 0; shift <= SHIFT_LIMIT; shift++) {
            for (int sign = 1; sign >= -1; sign -= 2) {
                if (shift == 0 && sign < 0) {
                    continue;
                }
                int feetY = base + sign * shift;
                if (feetY < minFeet || feetY > maxFeet) {
                    continue;
                }
                BlockPos head = new BlockPos(x, feetY + 1, z);
                if (level.getBlockEntity(head) != null) {
                    continue;
                }
                BlockState state = level.getBlockState(head);
                if (state.isAir()) {
                    return new BlockPos(x, feetY, z);
                }
                if (state.getDestroySpeed(level, head) < 0.0F) {
                    continue;
                }
                if (!state.getCollisionShape(level, head).isEmpty()) {
                    level.destroyBlock(head, true, breaker);
                }
                return new BlockPos(x, feetY, z);
            }
        }
        return null;
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        Target target = readTarget(event.getItemStack());
        if (target == null) {
            return;
        }
        var tooltip = event.getToolTip();
        tooltip.add(Component.translatable(MSG + "tooltip.target", target.x(), target.z()).withStyle(ChatFormatting.GRAY));
        Player player = event.getEntity();
        int tier = -1;
        if (player != null && player.level().dimension().equals(target.dimension())) {
            double distance = horizontalDistance(player, target);
            tooltip.add(Component.translatable(MSG + "tooltip.distance", Math.round(distance)).withStyle(ChatFormatting.GRAY));
            tier = distance <= DIRECT_RANGE ? 0 : distance <= PEARL_RANGE ? 1 : 2;
        } else if (player != null) {
            tooltip.add(Component.translatable(MSG + "wrong_dimension").withStyle(ChatFormatting.RED));
        }
        tooltip.add(Component.translatable(MSG + "tooltip.usage").withStyle(ChatFormatting.DARK_AQUA));
        String[] tiers = {"tooltip.direct", "tooltip.pearl", "tooltip.ritual"};
        for (int i = 0; i < tiers.length; i++) {
            tooltip.add(Component.translatable(MSG + tiers[i]).withStyle(i == tier ? ChatFormatting.AQUA : ChatFormatting.DARK_GRAY));
        }
    }
}
