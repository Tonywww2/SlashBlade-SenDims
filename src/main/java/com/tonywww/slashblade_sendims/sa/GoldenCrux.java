package com.tonywww.slashblade_sendims.sa;

import com.tonywww.slashblade_sendims.utils.SlashBladeUtil;
import mods.flammpfeil.slashblade.SlashBlade.RegistryEvents;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.entity.EntitySlashEffect;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class GoldenCrux {

    // 第 0 tick 触发，判定是否位移
    public static void doGoldenCruxStart(LivingEntity attacker) {
        if (!(attacker.level() instanceof ServerLevel level)) return;

        ItemStack bladeStack = attacker.getMainHandItem();
        if (!(bladeStack.getItem() instanceof ItemSlashBlade)) return;
        ISlashBladeState state = SlashBladeUtil.getState(bladeStack);

        Entity target = state.getTargetEntity(level);
        if (target != null) {
            Vec3 targetPos = target.position();
            // 判定目标上方 2~5 格（共4格高，确保装下头顶2格距离+玩家2格高）是否安全
            boolean hasSpace = true;
            for (int i = 2; i <= 5; i++) {
                BlockPos checkPos = BlockPos.containing(targetPos.x, targetPos.y + i, targetPos.z);
                if (!level.getBlockState(checkPos).getCollisionShape(level, checkPos).isEmpty()) {
                    hasSpace = false;
                    break;
                }
            }

            if (hasSpace) {
                // 如果有空间，将玩家位移到目标正上方2格
                attacker.teleportTo(targetPos.x, targetPos.y + 2.0, targetPos.z);
                attacker.setDeltaMovement(0, 0.25, 0);
                attacker.hurtMarked = true;
            } else {
                // 空间不足，原地蓄力
                attacker.setDeltaMovement(0, 0, 0);
                attacker.hurtMarked = true;
            }
        } else {
            // 没有锁定目标时默认原地蓄力
            attacker.setDeltaMovement(0, 0, 0);
            attacker.hurtMarked = true;
        }
    }

    // 落地后
    public static void doGoldenCruxImpact(LivingEntity attacker) {
        if (!(attacker.level() instanceof ServerLevel level)) return;

        // 大刺 - 11道
        for (int i = 0; i < 11; i++) {
            float angle = i * (360f / 11f);

            EntitySlashEffect slashEffect = new EntitySlashEffect(RegistryEvents.SlashEffect, level);

            slashEffect.setPos(
                    attacker.getX(),
                    attacker.getY() - 0.75f,
                    attacker.getZ()
            );

            slashEffect.setDamage(0.3f);
            slashEffect.setRank(7);
            slashEffect.setBaseSize(1.75f);

            slashEffect.setYRot(angle);
            slashEffect.setXRot(0f);

            slashEffect.setRotationRoll(90.0F);
            slashEffect.setColor(0xffff00);

            slashEffect.setOwner(attacker);

            level.addFreshEntity(slashEffect);
        }

        // 小刺
        for (int i = 0; i < 5; i++) {
            float angle = i * (360f / 5f);

            EntitySlashEffect slashEffect = new EntitySlashEffect(RegistryEvents.SlashEffect, level); // 注意这里，之前代码是 RegistryEvents.Drive，如果替换成斩击应都是 SlashEffect

            slashEffect.setPos(
                    attacker.getX(),
                    attacker.getY() - 0.5f,
                    attacker.getZ()
            );

            slashEffect.setDamage(0.4f);
            slashEffect.setRank(7);
            slashEffect.setBaseSize(1f);

            slashEffect.setYRot(angle);
            slashEffect.setXRot(0f);

            slashEffect.setRotationRoll(90.0F);
            slashEffect.setColor(0xFFD700);

            slashEffect.setOwner(attacker);

            level.addFreshEntity(slashEffect);
        }

    }

    public static void doGoldenCruxHover(LivingEntity attacker) {
        if (!attacker.level().isClientSide()) {
            attacker.setDeltaMovement(0, 0.1d, 0);
            attacker.hurtMarked = true;
        }
    }

    public static void doGoldenCruxDrop(LivingEntity attacker) {
        if (!attacker.level().isClientSide()) {
            attacker.setDeltaMovement(0, -8.0, 0);
            attacker.hurtMarked = true;
        }
    }

}