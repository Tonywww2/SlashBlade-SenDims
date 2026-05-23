package com.tonywww.slashblade_sendims.mixin.minecraft;

import com.tonywww.slashblade_sendims.utils.MobAttackManager;
import mods.flammpfeil.slashblade.util.KnockBacks;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.minecraft.world.entity.monster.Shulker$ShulkerAttackGoal")
public class MixinShulkerAttackGoal {

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean onShoot(Level instance, Entity entity) {
        if (entity instanceof ShulkerBullet) {
            Entity owner = ((ShulkerBullet) entity).getOwner();
            if (owner instanceof Shulker) {
                MobAttackManager.doSlash((Shulker) owner, 2.0F, 7d, 1f, 0xc163ff, Vec3.ZERO,
                        true, false, true, 0.65f, KnockBacks.toss);
            }
        }
        return instance.addFreshEntity(entity);
    }
}
