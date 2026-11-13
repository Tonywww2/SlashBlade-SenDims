package com.tonywww.slashblade_sendims.entities;

import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.entity.EntitySlashEffect;
import mods.flammpfeil.slashblade.entity.Projectile;
import mods.flammpfeil.slashblade.util.KnockBacks;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 只有画面的拔刀剑斩击特效：沿用拔刀剑的 SlashEffect 实体类型，客户端照常渲染刀光，
 * 服务端不做任何范围判定和伤害，伤害由调用方自行结算。
 */
public class EntityVisualSlashEffect extends EntitySlashEffect {

    public EntityVisualSlashEffect(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
    }

    public static EntityVisualSlashEffect spawn(LivingEntity owner, Vec3 pos, float yaw, float roll,
                                                int color, float size, boolean silent) {
        EntityVisualSlashEffect effect = new EntityVisualSlashEffect(SlashBlade.RegistryEvents.SlashEffect, owner.level());
        effect.setPos(pos.x, pos.y, pos.z);
        effect.setOwner(owner);
        effect.setRotationRoll(roll);
        effect.setYRot(yaw);
        effect.setXRot(0.0F);
        effect.setColor(color);
        effect.setMute(true);
        effect.setIsCritical(false);
        effect.setDamage(0.0D);
        effect.setKnockBack(KnockBacks.cancel);
        effect.setRank(7.0F);
        effect.setBaseSize(size);
        effect.setSilent(silent);
        owner.level().addFreshEntity(effect);
        return effect;
    }

    @Override
    public void tick() {
        this.baseTick();
        if (this.tickCount == 2) {
            this.playSound(this.getSlashSound(), 0.8F, 0.625F + 0.1F * this.random.nextFloat());
        }
        this.tryDespawn();
    }
}
