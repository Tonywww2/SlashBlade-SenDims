package com.tonywww.slashblade_sendims.mixin.adastra;

import com.tonywww.slashblade_sendims.compat.adastra.EtrionicFurnaceBatch;
import earth.terrarium.adastra.common.blockentities.machines.EtrionicBlastFurnaceBlockEntity;
import earth.terrarium.adastra.common.config.MachineConfig;
import earth.terrarium.botarium.common.energy.impl.WrappedBlockEnergyContainer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.BlastingRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EtrionicBlastFurnaceBlockEntity.class)
public abstract class EtrionicBlastFurnaceBlockEntityMixin {
    @Shadow(remap = false) protected int cookTime;
    @Shadow(remap = false) protected int cookTimeTotal;

    @Unique private static final String SBS_BATCH_TAG = "SenDimsFurnaceBatch";
    @Unique private EtrionicFurnaceBatch sbs$batch;
    @Unique private ItemStack[] sbs$inputs;
    @Unique private EtrionicBlastFurnaceBlockEntity.Mode sbs$mode;
    @Unique private long sbs$energyConfig;
    @Unique private CompoundTag sbs$savedBatch;

    @Unique
    private EtrionicBlastFurnaceBlockEntity sbs$self() {
        return (EtrionicBlastFurnaceBlockEntity) (Object) this;
    }

    @Unique
    private boolean sbs$server() {
        return sbs$self().getLevel() != null && !sbs$self().getLevel().isClientSide;
    }

    @Unique
    private void sbs$refresh(boolean force) {
        EtrionicBlastFurnaceBlockEntity entity = sbs$self();
        if (!sbs$server()) return;
        boolean changed = sbs$inputs == null || sbs$mode != entity.mode()
                || sbs$energyConfig != MachineConfig.etrionicBlastFurnaceBlastingEnergyPerItem
                || sbs$batch != null && !sbs$batch.recipesCurrent(entity.getLevel().getRecipeManager());
        if (!changed) {
            for (int i = 0; i < 4; i++) {
                ItemStack current = entity.getItem(i + 1);
                if (!ItemStack.isSameItemSameTags(current, sbs$inputs[i])
                        || entity.mode() == EtrionicBlastFurnaceBlockEntity.Mode.ALLOYING
                        && current.getCount() != sbs$inputs[i].getCount()) {
                    changed = true;
                    break;
                }
            }
        }
        if (!force && !changed) return;
        EtrionicFurnaceBatch next = EtrionicFurnaceBatch.find(entity);
        boolean resume = sbs$inputs == null && sbs$savedBatch != null && next != null
                && sbs$savedBatch.equals(next.fingerprint());
        if (resume) cookTime = Math.max(0, Math.min(cookTime, next.duration() - 1));
        else if (next == null || !next.sameWork(sbs$batch)) cookTime = 0;
        cookTimeTotal = next == null ? 0 : next.duration();
        sbs$batch = next;
        sbs$inputs = EtrionicFurnaceBatch.copyInputs(entity);
        sbs$mode = entity.mode();
        sbs$energyConfig = MachineConfig.etrionicBlastFurnaceBlastingEnergyPerItem;
        sbs$savedBatch = null;
    }

    @Unique
    private void sbs$tick(WrappedBlockEnergyContainer energy) {
        if (!sbs$server()) return;
        sbs$refresh(false);
        if (sbs$batch == null || !sbs$batch.canCommit(sbs$self())) return;
        long cost = sbs$batch.energyPerTick();
        if (energy.internalExtract(cost, true) != cost) return;
        long paid = energy.internalExtract(cost, false);
        if (paid != cost) {
            if (paid > 0) energy.internalInsert(paid, false);
            return;
        }
        cookTime++;
        if (cookTime >= cookTimeTotal) {
            if (sbs$batch.commit(sbs$self())) {
                cookTime = 0;
                sbs$refresh(true);
            } else {
                // Defensive rollback if an energy callback changed the inventory.
                energy.internalInsert(paid, false);
                cookTime--;
                sbs$refresh(false);
            }
        }
        sbs$self().setChanged();
    }

    @Inject(method = "recipeTick", at = @At("HEAD"), cancellable = true, remap = false)
    private void sbs$processBatch(WrappedBlockEnergyContainer energy, CallbackInfo ci) {
        sbs$tick(energy);
        ci.cancel();
    }

    @Inject(method = "alloyingRecipeTick", at = @At("HEAD"), cancellable = true, remap = false)
    private void sbs$processAlloyBatch(WrappedBlockEnergyContainer energy, CallbackInfo ci) {
        if (sbs$self().mode() == EtrionicBlastFurnaceBlockEntity.Mode.ALLOYING) sbs$tick(energy);
        ci.cancel();
    }

    @Inject(method = "update", at = @At("HEAD"), cancellable = true, remap = false)
    private void sbs$updateBatch(CallbackInfo ci) {
        sbs$refresh(true);
        ci.cancel();
    }

    @Inject(method = "canCraftAlloying", at = @At("HEAD"), cancellable = true, remap = false)
    private void sbs$canAlloy(CallbackInfoReturnable<Boolean> cir) {
        sbs$refresh(false);
        cir.setReturnValue(sbs$server() && sbs$self().mode() == EtrionicBlastFurnaceBlockEntity.Mode.ALLOYING
                && sbs$batch != null && sbs$batch.canCommit(sbs$self())
                && sbs$self().getEnergyStorage().internalExtract(sbs$batch.energyPerTick(), true) == sbs$batch.energyPerTick());
    }

    // Legacy per-slot settlement must never bypass the paid, validated batch transaction.
    @Inject(method = "craft", at = @At("HEAD"), cancellable = true, remap = false)
    private void sbs$disablePartialCraft(BlastingRecipe recipe, int slot, CallbackInfo ci) { ci.cancel(); }

    @Inject(method = "craftAlloying", at = @At("HEAD"), cancellable = true, remap = false)
    private void sbs$disableUnpaidAlloy(CallbackInfo ci) { ci.cancel(); }

    @Unique
    private void sbs$invalidate() {
        sbs$batch = null;
        sbs$inputs = null;
        sbs$savedBatch = null;
        cookTime = 0;
        cookTimeTotal = 0;
    }

    @Inject(method = "setMode", at = @At("HEAD"), remap = false)
    private void sbs$changeMode(EtrionicBlastFurnaceBlockEntity.Mode mode, CallbackInfo ci) {
        if (mode != sbs$self().mode()) sbs$invalidate();
    }

    @Inject(method = "clearRecipe", at = @At("TAIL"), remap = false)
    private void sbs$clearBlasting(int index, CallbackInfo ci) { sbs$invalidate(); }

    @Inject(method = "clearAlloyingRecipe", at = @At("TAIL"), remap = false)
    private void sbs$clearAlloying(CallbackInfo ci) { sbs$invalidate(); }

    @Inject(method = "saveAdditional", at = @At("HEAD"))
    private void sbs$validateBeforeSave(CompoundTag tag, CallbackInfo ci) { sbs$refresh(false); }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void sbs$saveBatch(CompoundTag tag, CallbackInfo ci) {
        if (sbs$batch != null) tag.put(SBS_BATCH_TAG, sbs$batch.fingerprint());
        else tag.remove(SBS_BATCH_TAG);
    }

    @Inject(method = "load", at = @At("TAIL"))
    private void sbs$loadBatch(CompoundTag tag, CallbackInfo ci) {
        sbs$batch = null;
        sbs$inputs = null;
        sbs$savedBatch = tag.contains(SBS_BATCH_TAG, Tag.TAG_COMPOUND) ? tag.getCompound(SBS_BATCH_TAG).copy() : null;
    }
}
