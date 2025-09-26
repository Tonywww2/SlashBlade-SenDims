package com.tonywww.slashblade_sendims.items;

import com.tonywww.slashblade_sendims.curios.DeepRealmCertificateCapProvider;
import com.tonywww.slashblade_sendims.registeries.SBSDTags;
import com.tonywww.slashblade_sendims.utils.NBTUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DeepRealmCertificate extends Item {

    public static final String PATH = "deeprealm";
    public static final String RANK = "rank";
    public static final String HEALTH_PROGRESS = "health_progress";
    public static final String DAMAGE_RATE_PROGRESS = "damage_rate_progress";

    public static final String SLOT = "drc";

    public static final int MATERIAL_COUNT_PER_PROGRESS = 30;
    public static final ChatFormatting[] COLOR_LIST = new ChatFormatting[]{
            ChatFormatting.GRAY,
            ChatFormatting.GOLD,
            ChatFormatting.WHITE,
            ChatFormatting.YELLOW,
            ChatFormatting.AQUA,
            ChatFormatting.LIGHT_PURPLE
    };

    public DeepRealmCertificate(Properties properties) {
        super(properties);
    }

    @Override
    public boolean overrideOtherStackedOnMe(@NotNull ItemStack self, @NotNull ItemStack other, @NotNull Slot slot, @NotNull ClickAction action, @NotNull Player player, SlotAccess access) {
        if (action != ClickAction.SECONDARY) {
            return false;
        }
        CompoundTag drTag = getDRTag(self);
        int currentRank = getRank(drTag);

        if (other.is(SBSDTags.Items.DRC_RANK_MATERIALS)) {
            return processRankUpgrade(drTag, other, player, currentRank);
        } else if (other.is(SBSDTags.Items.DRC_HEALTH_MATERIALS)) {
            return processProgressUpgrade(drTag, other, player, currentRank,
                    HEALTH_PROGRESS,
                    SBSDTags.Items.DRC_HEALTH_MATERIAL_0,
                    SBSDTags.Items.DRC_HEALTH_MATERIAL_1,
                    SBSDTags.Items.DRC_HEALTH_MATERIAL_2,
                    SBSDTags.Items.DRC_HEALTH_MATERIAL_3,
                    SBSDTags.Items.DRC_HEALTH_MATERIAL_4,
                    SBSDTags.Items.DRC_HEALTH_MATERIAL_5
            );
        } else if (other.is(SBSDTags.Items.DRC_DAMAGE_MATERIALS)) {
            return processProgressUpgrade(drTag, other, player, currentRank,
                    DAMAGE_RATE_PROGRESS,
                    SBSDTags.Items.DRC_DAMAGE_MATERIAL_0,
                    SBSDTags.Items.DRC_DAMAGE_MATERIAL_1,
                    SBSDTags.Items.DRC_DAMAGE_MATERIAL_2,
                    SBSDTags.Items.DRC_DAMAGE_MATERIAL_3,
                    SBSDTags.Items.DRC_DAMAGE_MATERIAL_4,
                    SBSDTags.Items.DRC_DAMAGE_MATERIAL_5

            );
        }

        return false;
    }

    private boolean processRankUpgrade(CompoundTag drTag, ItemStack other, Player player, int currentRank) {
        int targetRank = getTargetRank(currentRank, other);
        if (targetRank != -1) {
            drTag.putInt(RANK, targetRank);
            player.playSound(SoundEvents.PLAYER_LEVELUP);
            other.shrink(1);
            return true;
        }
        player.playSound(SoundEvents.VILLAGER_NO);
        return false;
    }

    private int getTargetRank(int currentRank, ItemStack other) {
        return switch (currentRank) {
            case 0 -> other.is(SBSDTags.Items.DRC_RANK_MATERIAL_1) ? 1 : -1;
            case 1 -> other.is(SBSDTags.Items.DRC_RANK_MATERIAL_2) ? 2 : -1;
            case 2 -> other.is(SBSDTags.Items.DRC_RANK_MATERIAL_3) ? 3 : -1;
            case 3 -> other.is(SBSDTags.Items.DRC_RANK_MATERIAL_4) ? 4 : -1;
            case 4 -> other.is(SBSDTags.Items.DRC_RANK_MATERIAL_5) ? 5 : -1;
            default -> -1;
        };
    }

    @SafeVarargs
    private boolean processProgressUpgrade(CompoundTag drTag, ItemStack other, Player player, int currentRank,
                                           String progressType, net.minecraft.tags.TagKey<Item>... materialTags) {
        int currentTotal = NBTUtils.getSpecificIntField(drTag, progressType);
        int currentProgressRank = currentTotal / MATERIAL_COUNT_PER_PROGRESS;

        if (currentProgressRank > currentRank) {
            player.playSound(SoundEvents.VILLAGER_NO);
            return true;
        }

        if (currentProgressRank >= materialTags.length || !other.is(materialTags[currentProgressRank])) {
            player.playSound(SoundEvents.VILLAGER_NO);
            return false;
        }

        int current = currentTotal - (MATERIAL_COUNT_PER_PROGRESS * currentProgressRank);
        int toConsume = Math.min(MATERIAL_COUNT_PER_PROGRESS - current, other.getCount());

        if (toConsume <= 0) {
            player.playSound(SoundEvents.VILLAGER_NO);
            return true;
        }

        drTag.putInt(progressType, currentTotal + toConsume);
        player.playSound(SoundEvents.PLAYER_LEVELUP);
        other.shrink(toConsume);
        return true;
    }

    @Override
    public @Nullable ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new DeepRealmCertificateCapProvider(stack);
    }

    public static CompoundTag getDRTag(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.contains(PATH)) tag.put(PATH, new CompoundTag());
        return tag.getCompound(PATH);
    }

    public static int getRank(CompoundTag drTag) {
        return NBTUtils.getSpecificIntField(drTag, RANK);
    }

    public static double calcFinalValue(int x, int count, double give) {
        double scale = (double) x / count;
        return give * scale * scale;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> toolTips, TooltipFlag isAdvanced) {
        if (stack.getTag() == null) {
            return;
        }

        CompoundTag drTag = getDRTag(stack);
        int rank = getRank(drTag);
        int healthProgress = NBTUtils.getSpecificIntField(drTag, HEALTH_PROGRESS);
        int damageProgress = NBTUtils.getSpecificIntField(drTag, DAMAGE_RATE_PROGRESS);

        // 阶级
        MutableComponent rankComponent = Component.translatable("ui.slashblade_sendims.deeprealm_certificate.rank");
        rankComponent.append(
                Component.translatable("ui.slashblade_sendims.deeprealm_certificate.rank." + rank)
                        .setStyle(rankComponent.getStyle().withColor(COLOR_LIST[rank]))
        );
        toolTips.add(rankComponent);

        // 生命
        int progressCap = (rank + 1) * MATERIAL_COUNT_PER_PROGRESS;
        int healthProgressCount = healthProgress / MATERIAL_COUNT_PER_PROGRESS;

        MutableComponent health = Component.translatable("ui.slashblade_sendims.deeprealm_certificate.healthprogress");
        health.append(
                Component.literal("[" + healthProgressCount + "] ")
                        .setStyle(health.getStyle().withColor(COLOR_LIST[healthProgressCount]))
        );
        if (healthProgress >= progressCap) {
            health.append(Component.literal("MAX"));
        } else {
            health.append(Component.literal(healthProgress % MATERIAL_COUNT_PER_PROGRESS + "/" + MATERIAL_COUNT_PER_PROGRESS));
        }

        // 伤害
        int damageProgressCount = damageProgress / MATERIAL_COUNT_PER_PROGRESS;

        MutableComponent damage = Component.translatable("ui.slashblade_sendims.deeprealm_certificate.damageprogress");
        damage.append(
                Component.literal("[" + damageProgressCount + "] ")
                        .setStyle(damage.getStyle().withColor(COLOR_LIST[damageProgressCount]))
        );
        if (damageProgress >= progressCap) {
            damage.append(Component.literal("MAX"));
        } else {
            damage.append(Component.literal(damageProgress % MATERIAL_COUNT_PER_PROGRESS + "/" + MATERIAL_COUNT_PER_PROGRESS));
        }
        toolTips.add(health);
        toolTips.add(damage);

        super.appendHoverText(stack, level, toolTips, isAdvanced);
    }
}
