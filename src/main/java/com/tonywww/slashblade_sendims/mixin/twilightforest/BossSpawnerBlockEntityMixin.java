package com.tonywww.slashblade_sendims.mixin.twilightforest;

import com.tonywww.slashblade_sendims.twilight.TwilightProgression;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import twilightforest.block.entity.spawner.BossSpawnerBlockEntity;

/**
 * Boss spawners only wake for players who have reached that boss in the reworked order. The naga courtyard and the
 * Aurora Palace Regeared have no structure lock, so this is their only gate.
 */
@Mixin(value = BossSpawnerBlockEntity.class, remap = false)
public abstract class BossSpawnerBlockEntityMixin {
    @Shadow
    @Final
    protected EntityType<?> entityType;

    @Shadow
    protected abstract int getRange();

    @Inject(method = "anyPlayerInRange", at = @At("HEAD"), cancellable = true)
    private void sendims$gateByAdvancement(CallbackInfoReturnable<Boolean> cir) {
        ResourceLocation advancement = TwilightProgression.spawnerGate(this.entityType);
        if (advancement == null) {
            return;
        }
        BlockEntity self = (BlockEntity) (Object) this;
        Level level = self.getLevel();
        if (level != null) {
            cir.setReturnValue(TwilightProgression.anyEligiblePlayerInRange(level, self.getBlockPos(), this.getRange(), advancement));
        }
    }
}
