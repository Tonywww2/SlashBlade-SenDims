package com.tonywww.slashblade_sendims.mixin.botania;

import mods.flammpfeil.slashblade.util.AttackManager;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import vazkii.botania.api.BotaniaAPI;
import vazkii.botania.api.internal.ManaBurst;
import vazkii.botania.common.item.equipment.tool.terrasteel.TerraBladeItem;

import java.util.List;

@Mixin(value = TerraBladeItem.class)
public class TerraBladeItemMixin {

    /**
     * @author SlashBlade-SenDims
     * @reason Execute AttackManager.doMeleeAttack after hurt
     */
    @Overwrite(remap = false)
    public void updateBurst(ManaBurst burst, ItemStack stack) {
        ThrowableProjectile entity = burst.entity();
        AABB axis = (new AABB(entity.getX(), entity.getY(), entity.getZ(), entity.xOld, entity.yOld, entity.zOld)).inflate(1.0F);
        List<LivingEntity> entities = entity.level().getEntitiesOfClass(LivingEntity.class, axis);
        Entity thrower = entity.getOwner();

        for (LivingEntity living : entities) {
            if (living != thrower) {
                if (living instanceof Player livingPlayer) {
                    if (thrower instanceof Player throwingPlayer) {
                        if (!throwingPlayer.canHarmPlayer(livingPlayer)) {
                            continue;
                        }
                    }
                }

                if (living.hurtTime == 0) {
                    int cost = 33;
                    int mana = burst.getMana();
                    if (mana >= cost) {
                        burst.setMana(mana - cost);
                        float damage = 4.0F + BotaniaAPI.instance().getTerrasteelItemTier().getAttackDamageBonus();
                        if (!burst.isFake() && !entity.level().isClientSide) {
                            DamageSource source = living.damageSources().magic();
                            if (thrower instanceof Player) {
                                Player player = (Player) thrower;
                                source = player.damageSources().playerAttack(player);
                            } else if (thrower instanceof LivingEntity) {
                                LivingEntity livingEntity = (LivingEntity) thrower;
                                source = livingEntity.damageSources().mobAttack(livingEntity);
                            }

                            living.hurt(source, damage);

                            // 在 hurt 之后执行 AttackManager.doMeleeAttack
                            if (thrower instanceof LivingEntity liveAttacker) {
                                AttackManager.doMeleeAttack(liveAttacker, living, true, false, 0.2f);
                            }

                            entity.discard();
                            break;
                        }
                    }
                }
            }
        }
    }
}
