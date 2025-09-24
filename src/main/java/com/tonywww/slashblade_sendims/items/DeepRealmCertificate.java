package com.tonywww.slashblade_sendims.items;

import com.tonywww.slashblade_sendims.curios.DeepRealmCertificateCapProvider;
import com.tonywww.slashblade_sendims.registeries.SBSDTags;
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

    public DeepRealmCertificate(Properties properties) {
        super(properties);
    }

    @Override
    public boolean overrideOtherStackedOnMe(@NotNull ItemStack self, @NotNull ItemStack other, @NotNull Slot slot, @NotNull ClickAction action, @NotNull Player player, SlotAccess access) {
        if (action != ClickAction.SECONDARY)
            return false;

        CompoundTag drTag = getDRTag(self);
        int currentRank = getRank(drTag);

        if (other.is(SBSDTags.Items.DRC_RANK_MATERIALS)) {
            int targetRank = -1;
            switch (currentRank) {
                case 0:
                    if (other.is(SBSDTags.Items.DRC_RANK_MATERIAL_1)) targetRank = 1;
                    break;
                case 1:
                    if (other.is(SBSDTags.Items.DRC_RANK_MATERIAL_2)) targetRank = 2;
                    break;
                case 2:
                    if (other.is(SBSDTags.Items.DRC_RANK_MATERIAL_3)) targetRank = 3;
                    break;
                case 3:
                    if (other.is(SBSDTags.Items.DRC_RANK_MATERIAL_4)) targetRank = 4;
                    break;
                case 4:
                    if (other.is(SBSDTags.Items.DRC_RANK_MATERIAL_5)) targetRank = 5;
                    break;
                default:
                    player.playSound(SoundEvents.VILLAGER_NO);
                    break;
            }
            if (targetRank != -1) {
                drTag.putInt(RANK, targetRank);
                player.playSound(SoundEvents.PLAYER_LEVELUP);
                other.shrink(1);

                return true;
            }

        } else if (other.is(SBSDTags.Items.DRC_HEALTH_MATERIALS)) {

        } else if (other.is(SBSDTags.Items.DRC_DAMAGE_MATERIALS)) {

        }

        return false;
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

    public static int getSpecificProgress(CompoundTag drTag, String type) {
        if (!drTag.contains(type)) {
            drTag.putInt(type, 0);
        }
        return drTag.getInt(type);
    }

    public static int getRank(CompoundTag drTag) {
        return getSpecificProgress(drTag, RANK);
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
        int healthProgress = getSpecificProgress(drTag, HEALTH_PROGRESS);
        int damageProgress = getSpecificProgress(drTag, DAMAGE_RATE_PROGRESS);

        toolTips.add(Component.translatable("ui.slashblade_sendims.deeprealm_certificate.rank")
                .append(Component.translatable("ui.slashblade_sendims.deeprealm_certificate.rank." + rank))
        );

        int progressCap = (rank + 1) * MATERIAL_COUNT_PER_PROGRESS;

        MutableComponent health = Component.translatable("ui.slashblade_sendims.deeprealm_certificate.healthprogress");
        String healthText = "[" +
                healthProgress / MATERIAL_COUNT_PER_PROGRESS +
                "] ";
        if (healthProgress >= progressCap) {
            health.append(Component.literal("MAX"));
        } else {
            healthText +=
                    healthProgress % MATERIAL_COUNT_PER_PROGRESS +
                            "/" +
                            MATERIAL_COUNT_PER_PROGRESS;
            health.append(Component.literal(healthText));
        }

        MutableComponent damage = Component.translatable("ui.slashblade_sendims.deeprealm_certificate.damageprogress");
        String damageText = "[" +
                damageProgress / MATERIAL_COUNT_PER_PROGRESS +
                "] ";
        if (damageProgress >= progressCap) {
            damage.append(Component.literal("MAX"));
        } else {
            damageText +=
                    damageProgress % MATERIAL_COUNT_PER_PROGRESS +
                            "/" +
                            MATERIAL_COUNT_PER_PROGRESS;
            damage.append(Component.literal(damageText));
        }
        toolTips.add(health);
        toolTips.add(damage);

        super.appendHoverText(stack, level, toolTips, isAdvanced);
    }
}
