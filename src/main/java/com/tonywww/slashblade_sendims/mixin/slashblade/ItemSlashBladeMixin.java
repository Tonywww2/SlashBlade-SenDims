package com.tonywww.slashblade_sendims.mixin.slashblade;

import com.tonywww.slashblade_sendims.items.BloodJade;
import com.tonywww.slashblade_sendims.utils.SlashBladeUtil;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ItemSlashBlade.class)
public abstract class ItemSlashBladeMixin extends SwordItem {
    public ItemSlashBladeMixin(Tier pTier, int pAttackDamageModifier, float pAttackSpeedModifier, Properties pProperties) {
        super(pTier, pAttackDamageModifier, pAttackSpeedModifier, pProperties);
    }

    // m_6832_ 为 isValidRepairItem 的正式环境混淆名
    @Inject(
            method = {
                    "isValidRepairItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z",
                    "m_6832_(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z"
            },
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void rejectStoneToolMaterials(ItemStack blade, ItemStack repairMaterial,
                                          CallbackInfoReturnable<Boolean> cir) {
        if (repairMaterial.is(ItemTags.STONE_TOOL_MATERIALS)) {
            cir.setReturnValue(false);
        }
    }

    @Override
    public boolean overrideOtherStackedOnMe(@NotNull ItemStack me, @NotNull ItemStack other, @NotNull Slot slot, @NotNull ClickAction action, @NotNull Player player, @NotNull SlotAccess slotAccess) {
        if (action != ClickAction.SECONDARY) return false;
        CompoundTag data = other.getOrCreateTag();
        if (!data.contains(BloodJade.KILL_COUNT_PATH)) return false;
        int countToIncrease = data.getInt(BloodJade.KILL_COUNT_PATH);
        ISlashBladeState bladeState = SlashBladeUtil.getState(me);
        if (bladeState == null) return false;
        int newCount = bladeState.getKillCount() + countToIncrease;
        if (me.getTag() == null) return false;
        me.getTag().getCompound("bladeState").putInt("killCount", newCount);
        bladeState.setKillCount(newCount);

        other.shrink(1);
        player.playSound(SoundEvents.PLAYER_LEVELUP);
        slot.setChanged();

        return true;
    }
}
