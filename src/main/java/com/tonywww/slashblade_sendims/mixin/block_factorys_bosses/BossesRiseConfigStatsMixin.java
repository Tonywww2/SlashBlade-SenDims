package com.tonywww.slashblade_sendims.mixin.block_factorys_bosses;

import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.unusual.block_factorys_bosses.entity.boss.dragon.boss.InfernalDragonEntity;
import net.unusual.block_factorys_bosses.entity.boss.knight.UnderworldKnightEntity;
import net.unusual.block_factorys_bosses.entity.boss.kraken.KrakenEntity;
import net.unusual.block_factorys_bosses.entity.boss.sandworm.SandwormEntity;
import net.unusual.block_factorys_bosses.entity.boss.yeti.YetiEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 让 BossesRise 的 BOSS 不再用 ServerConfiguration 中配置的 血量/护甲/攻击 覆盖实体属性，
 * 从而让外部（如 KubeJS EntityEvents.spawned）设置的属性为准。
 *
 * 覆盖发生在以下位置，这里统一把其中的 setBaseValue / setHealth 调用置为空操作：
 * - finalizeSpawn（5 个 BOSS；Yeti 走私有 assignIfPresent）。
 * - InfernalDragonEntity.onBossSpawnerSpawn：在 addFreshEntity（即 EntityEvents.spawned）之后
 *   仍会用 DRAGON_HEALTH 重写最大生命并 setHealth，覆盖外部设置。
 * - SandwormEntity 的状态动作 lambda$static$20(ActiveState)：进入该状态时用配置重写属性。
 */
@Mixin(value = {
        SandwormEntity.class,
        YetiEntity.class,
        UnderworldKnightEntity.class,
        KrakenEntity.class,
        InfernalDragonEntity.class
}, remap = false)
public abstract class BossesRiseConfigStatsMixin {

    @Redirect(
            method = {"finalizeSpawn", "m_6518_", "assignIfPresent", "onBossSpawnerSpawn"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/attributes/AttributeInstance;setBaseValue(D)V", remap = true)
    )
    private void slashBlade_SenDims$skipConfigSetBaseValue(AttributeInstance instance, double value) {
        // 不根据配置写入任何属性基础值（实例方法）
    }

    @Redirect(
            method = "lambda$static$20",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/attributes/AttributeInstance;setBaseValue(D)V", remap = true),
            require = 0
    )
    private static void slashBlade_SenDims$skipConfigSetBaseValueState(AttributeInstance instance, double value) {
        // 不根据配置写入任何属性基础值（沙虫状态机静态 lambda）
    }

    @Redirect(
            method = {"finalizeSpawn", "m_6518_"},
            at = @At(value = "INVOKE", target = "Lnet/unusual/block_factorys_bosses/entity/boss/sandworm/SandwormEntity;setHealth(F)V", remap = true),
            require = 0
    )
    private void slashBlade_SenDims$skipSandwormSetHealth(SandwormEntity instance, float value) {
        // 不在 finalizeSpawn 内根据配置设置生命值
    }

    @Redirect(
            method = {"finalizeSpawn", "m_6518_"},
            at = @At(value = "INVOKE", target = "Lnet/unusual/block_factorys_bosses/entity/boss/yeti/YetiEntity;setHealth(F)V", remap = true),
            require = 0
    )
    private void slashBlade_SenDims$skipYetiSetHealth(YetiEntity instance, float value) {
        // 不在 finalizeSpawn 内根据配置设置生命值
    }

    @Redirect(
            method = {"finalizeSpawn", "m_6518_"},
            at = @At(value = "INVOKE", target = "Lnet/unusual/block_factorys_bosses/entity/boss/knight/UnderworldKnightEntity;setHealth(F)V", remap = true),
            require = 0
    )
    private void slashBlade_SenDims$skipKnightSetHealth(UnderworldKnightEntity instance, float value) {
        // 不在 finalizeSpawn 内根据配置设置生命值
    }

    @Redirect(
            method = {"finalizeSpawn", "m_6518_"},
            at = @At(value = "INVOKE", target = "Lnet/unusual/block_factorys_bosses/entity/boss/kraken/KrakenEntity;setHealth(F)V", remap = true),
            require = 0
    )
    private void slashBlade_SenDims$skipKrakenSetHealth(KrakenEntity instance, float value) {
        // 不在 finalizeSpawn 内根据配置设置生命值
    }

    @Redirect(
            method = {"finalizeSpawn", "m_6518_", "onBossSpawnerSpawn"},
            at = @At(value = "INVOKE", target = "Lnet/unusual/block_factorys_bosses/entity/boss/dragon/boss/InfernalDragonEntity;setHealth(F)V", remap = true),
            require = 0
    )
    private void slashBlade_SenDims$skipDragonSetHealth(InfernalDragonEntity instance, float value) {
        // 不根据配置设置生命值（含召唤方块生成后的 onBossSpawnerSpawn）
    }
}
