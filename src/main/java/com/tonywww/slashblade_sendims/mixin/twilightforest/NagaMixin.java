package com.tonywww.slashblade_sendims.mixin.twilightforest;

import com.tonywww.slashblade_sendims.leader.SBSDLeader;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import twilightforest.entity.boss.Naga;

@Mixin(value = Naga.class)
public class NagaMixin extends Monster {

    protected NagaMixin(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Inject(method = "Ltwilightforest/entity/boss/Naga;tick()V", at = @At("RETURN"))
    private void injectTick(CallbackInfo ci) {
        if (this.level() instanceof ServerLevel serverLevel) {
            SBSDLeader.tickBossLeader(this, serverLevel, this.getPersistentData(), this.tickCount);
        }
    }
}
