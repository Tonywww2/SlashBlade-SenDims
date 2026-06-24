package com.tonywww.slashblade_sendims.mixin.block_factorys_bosses;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.unusual.block_factorys_bosses.entity.boss.AbstractStateBossEntity;
import net.unusual.block_factorys_bosses.entity.boss.part.AbstractEntityPartParent;
import net.unusual.block_factorys_bosses.entity.boss.sandworm.SandwormEntity;
import net.unusual.block_factorys_bosses.entity.boss.sandworm.SandwormEntityPart;
import net.unusual.block_factorys_bosses.geckolib.ServerAnimationPlayer;
import net.unusual.block_factorys_bosses.init.BossesRiseParticleTypes;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SandwormEntity.class, remap = false)
public abstract class SandwormEntityMixin extends AbstractStateBossEntity implements AbstractEntityPartParent<SandwormEntity, SandwormEntityPart> {

    @Final
    @Shadow(remap = false)
    private ServerAnimationPlayer<SandwormEntity> serverAnimationPlayer;

    @Unique
    private int slashBlade_SenDims$particleCoolDown = 0;

    protected SandwormEntityMixin(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    @Unique
    public boolean slashBlade_SenDims$isSegmentDamaged(int index) {
        return (this.getEntityData().get(SandwormEntity.DATA_DAMAGED_SEGMENTS) & 1 << index) != 0;
    }

    @Unique
    public void slashBlade_SenDims$setSegmentDamaged(int index, boolean damaged) {
        this.getEntityData().set(SandwormEntity.DATA_DAMAGED_SEGMENTS, this.getEntityData().get(SandwormEntity.DATA_DAMAGED_SEGMENTS) & ~(1 << index) | (damaged ? 1 : 0) << index);
    }

    @Unique
    public Vector3f slashBlade_SenDims$getBoneWorldPosition(String boneName) {
        return this.serverAnimationPlayer.getBoneWorldPosition(boneName);
    }

    @Inject(method = "makeSegmentDamaged(Lnet/unusual/block_factorys_bosses/entity/boss/sandworm/SandwormEntityPart;)V", at = @At("HEAD"), remap = false, cancellable = true)
    private void slashBlade_SenDims$makeSegmentDamaged(SandwormEntityPart part, CallbackInfo ci) {
        if (!this.slashBlade_SenDims$isSegmentDamaged(part.getPartIndex())) {
            this.slashBlade_SenDims$setSegmentDamaged(part.getPartIndex(), true);
            Level var3 = this.level();
            if (var3 instanceof ServerLevel && slashBlade_SenDims$particleCoolDown < 1) {
                ServerLevel level = (ServerLevel) var3;
                Vector3f bonePosition = this.slashBlade_SenDims$getBoneWorldPosition(part.getAnchorBoneName());

                slashBlade_SenDims$particleCoolDown = 10;

                level.sendParticles(BossesRiseParticleTypes.POISON_SPIT.get(), bonePosition.x(), bonePosition.y(), bonePosition.z(), 4, 0.4, 0.4, 0.4, 0.1);
            }

        }
        ci.cancel();
    }

    @Inject(method = {"tick()V", "m_8119_"}, at = @At("RETURN"))
    private void slashBlade_SenDims$tick(CallbackInfo ci) {
        if (slashBlade_SenDims$particleCoolDown > 0) slashBlade_SenDims$particleCoolDown--;

    }
}
