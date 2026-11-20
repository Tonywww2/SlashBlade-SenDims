package com.tonywww.slashblade_sendims.items;

import com.tonywww.slashblade_sendims.curios.PrincipleOfSwordArtsCapProvider;
import mods.flammpfeil.slashblade.registry.SlashArtsRegistry;
import mods.flammpfeil.slashblade.registry.SlashBladeItems;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class PrincipleOfSwordArts extends AbstractCollectionItem {
    public static final String PATH = "principle_of_sword_arts";
    public static final String SLOT = "principle_of_sword_arts";

    public static final int TOTAL_SA = 70;

    public PrincipleOfSwordArts(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new PrincipleOfSwordArtsCapProvider(stack);
    }

    @Override
    protected String getCollectionPath() {
        return PATH;
    }

    @Override
    protected boolean shouldConsumeFound() {
        return true;
    }

    @Override
    protected @Nullable String getTargetKey(ItemStack targetStack) {
        if (!targetStack.is(SlashBladeItems.PROUDSOUL_SPHERE.get()) || !targetStack.hasTag() || !targetStack.getTag().contains("SpecialAttackType")) {
            return null;
        }

        String saKey = targetStack.getTag().getString("SpecialAttackType");
        if (saKey.isEmpty() || saKey.equals("slashblade:none")) {
            return null;
        }

        return saKey;
    }

    @Override
    protected long getTotalCollectionCount() {
        return TOTAL_SA;
    }

    @Override
    protected String getTranslationKeyBase() {
        return "ui.slashblade_sendims.principle_of_sword_arts";
    }

    @Override
    protected void addSpecificHoverDetails(List<Component> toolTips, ListTag itemList) {
        for (int i = 0; i < itemList.size(); i++) {
            String saKey = itemList.getString(i);
            ResourceLocation rl = ResourceLocation.tryParse(saKey);
            if (rl != null && SlashArtsRegistry.REGISTRY.get().containsKey(rl)) {
                SlashArts sa = SlashArtsRegistry.REGISTRY.get().getValue(rl);
                if (sa != null) {
                    toolTips.add(Component.literal("- ")
                            .append(Component.translatable("slashblade.tooltip.slash_art", sa.getDescription())).withStyle(ChatFormatting.GRAY));
                }
            }
        }
    }

    public static CompoundTag getPSATag(ItemStack stack) {
        return getCollectionTag(stack, PATH);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, List<Component> toolTips, @NotNull TooltipFlag isAdvanced) {
        super.appendHoverText(stack, level, toolTips, isAdvanced);
        toolTips.add(Component.translatable(getTranslationKeyBase() + ".sync_hint").withStyle(ChatFormatting.GRAY));
    }

    /**
     * 主副手都拿着总纲时潜行右键：两本总纲互相补齐对方缺少的 SA。
     * 每补 1 个 SA 消耗快捷栏里 1 个不带 SA、SE 的耀魂宝珠；数量不够时整次不同步。
     */
    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        if (!player.isShiftKeyDown() || !(main.getItem() instanceof PrincipleOfSwordArts) || !(off.getItem() instanceof PrincipleOfSwordArts)) {
            return super.use(level, player, hand);
        }
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(held);
        }
        player.getCooldowns().addCooldown(this, 20);
        if (!level.isClientSide) {
            syncSlashArts(level, player, main, off);
        }
        return InteractionResultHolder.sidedSuccess(held, level.isClientSide);
    }

    private void syncSlashArts(Level level, Player player, ItemStack first, ItemStack second) {
        String base = getTranslationKeyBase();
        CompoundTag firstTag = getPSATag(first);
        CompoundTag secondTag = getPSATag(second);
        ListTag firstList = firstTag.getList(ITEM_LIST, Tag.TAG_STRING);
        ListTag secondList = secondTag.getList(ITEM_LIST, Tag.TAG_STRING);
        List<String> toFirst = missingFrom(secondList, firstList);
        List<String> toSecond = missingFrom(firstList, secondList);
        int cost = toFirst.size() + toSecond.size();
        if (cost == 0) {
            player.displayClientMessage(Component.translatable(base + ".sync_same"), true);
            level.playSound(null, player.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0F, 1.0F);
            return;
        }

        boolean free = player.getAbilities().instabuild;
        int available = countPlainSpheres(player.getInventory());
        if (!free && available < cost) {
            player.displayClientMessage(Component.translatable(base + ".sync_not_enough", cost, available).withStyle(ChatFormatting.RED), true);
            level.playSound(null, player.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0F, 1.0F);
            return;
        }
        if (!free) {
            consumePlainSpheres(player.getInventory(), cost);
        }

        append(firstTag, firstList, toFirst);
        append(secondTag, secondList, toSecond);
        player.displayClientMessage(Component.translatable(base + ".sync_done", cost).withStyle(ChatFormatting.GREEN), true);
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    /** source 里有、target 里没有的 SA，按 source 的顺序。 */
    private static List<String> missingFrom(ListTag source, ListTag target) {
        Set<String> owned = new LinkedHashSet<>();
        for (int i = 0; i < target.size(); i++) {
            owned.add(target.getString(i));
        }
        List<String> missing = new ArrayList<>();
        for (int i = 0; i < source.size(); i++) {
            String saKey = source.getString(i);
            if (!saKey.isEmpty() && owned.add(saKey)) {
                missing.add(saKey);
            }
        }
        return missing;
    }

    private static void append(CompoundTag tag, ListTag list, List<String> added) {
        if (added.isEmpty()) return;
        for (String saKey : added) {
            list.add(StringTag.valueOf(saKey));
        }
        tag.put(ITEM_LIST, list);
        tag.putInt(ITEM_COUNTS, tag.getInt(ITEM_COUNTS) + added.size());
    }

    /** 不带 SA、SE 的耀魂宝珠；带剑技或特效的宝珠不会被当作消耗品。 */
    private static boolean isPlainSphere(ItemStack stack) {
        if (!stack.is(SlashBladeItems.PROUDSOUL_SPHERE.get())) return false;
        CompoundTag tag = stack.getTag();
        if (tag == null) return true;
        String saKey = tag.getString("SpecialAttackType");
        boolean hasSlashArt = !saKey.isEmpty() && !saKey.equals("slashblade:none");
        return !hasSlashArt && !tag.contains("SpecialEffectType");
    }

    private static int countPlainSpheres(Inventory inventory) {
        int count = 0;
        for (int i = 0; i < Inventory.getSelectionSize(); i++) {
            ItemStack stack = inventory.items.get(i);
            if (isPlainSphere(stack)) count += stack.getCount();
        }
        return count;
    }

    private static void consumePlainSpheres(Inventory inventory, int amount) {
        for (int i = 0; i < Inventory.getSelectionSize() && amount > 0; i++) {
            ItemStack stack = inventory.items.get(i);
            if (!isPlainSphere(stack)) continue;
            int taken = Math.min(amount, stack.getCount());
            stack.shrink(taken);
            amount -= taken;
        }
        inventory.setChanged();
    }
}
