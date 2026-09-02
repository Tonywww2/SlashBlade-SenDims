package com.tonywww.slashblade_sendims.mixin.cataclysm;

import com.github.L_Ender.cataclysm.entity.InternalAnimationMonster.Internal_Animation_Monster;
import com.github.L_Ender.cataclysm.entity.InternalAnimationMonster.Wadjet_Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Wadjet_Entity.class)
public abstract class WadjetEntityMixin extends Internal_Animation_Monster {

    protected WadjetEntityMixin(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    @Shadow(remap = false)
    public abstract boolean getAwaken();

    @Inject(
            method = "canBeSeenAsEnemy",
            at = @At("HEAD"),
            cancellable = true
    )
    private void sendims$allowTargetingAfterAwakening(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(this.getAwaken() && super.canBeSeenAsEnemy());
    }
}