package com.tonywww.slashblade_sendims.entities;

import com.tonywww.slashblade_sendims.registeries.SBSDEntities;
import com.tonywww.slashblade_sendims.utils.ChaoticAttackManager;
import com.tonywww.slashblade_sendims.utils.ChaoticSlashArtEffects;
import mods.flammpfeil.slashblade.entity.EntityJudgementCut;
import mods.flammpfeil.slashblade.entity.Projectile;
import mods.flammpfeil.slashblade.util.KnockBacks;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

public class EntityChaoticJudgementCut extends EntityJudgementCut {
    public EntityChaoticJudgementCut(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public void tick() {
        if (!this.hasBeenShot) {
            this.gameEvent(GameEvent.PROJECTILE_SHOOT, this.getOwner());
            this.hasBeenShot = true;
        }
        if (!this.leftOwner) {
            this.leftOwner = this.checkLeftOwner();
        }
        this.baseTick();

        ChaoticSlashArtEffects.spawnJudgementCutParticles(this, this.random);

        if (this.tickCount < 8 && this.tickCount % 2 == 0) {
            this.playSound(this.getHitEntitySound(), 0.2F, 0.5F + 0.25F * this.random.nextFloat());
        }

        if (this.getShooter() != null) {
            if (this.tickCount % 2 == 0) {
                KnockBacks knockBack = this.getIsCritical() ? KnockBacks.toss : KnockBacks.cancel;
                ChaoticAttackManager.areaAttack(this, knockBack.action, 4.0D, true, false, 0.16F, null);
            }

            if (this.getIsCritical() && this.tickCount > 0 && this.tickCount <= 3) {
                EntityChaoticSlashEffect slash = new EntityChaoticSlashEffect(
                        SBSDEntities.CHAOTIC_SLASH_EFFECT.get(),
                        this.level()
                );
                slash.absMoveTo(
                        this.getX(),
                        this.getY(),
                        this.getZ(),
                        120.0F * this.tickCount + this.getSeed(),
                        0.0F
                );
                slash.setRotationRoll(30.0F);
                slash.setOwner(this.getShooter());
                slash.setMute(false);
                slash.setIsCritical(true);
                slash.setDamage(0.1F);
                slash.setColor(this.getColor());
                slash.setBaseSize(1.0F);
                slash.setKnockBack(KnockBacks.cancel);
                slash.setIndirect(true);
                slash.setRank(this.getRank());
                this.level().addFreshEntity(slash);
            }
        }

        this.tryDespawn();
    }
}