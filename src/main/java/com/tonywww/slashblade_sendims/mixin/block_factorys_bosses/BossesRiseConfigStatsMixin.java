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
 * 让 BossesRise 的 BOSS 在 finalizeSpawn 中不进行任何属性设置操作，
 * 从而忽略 ServerConfiguration 中配置的 血量/护甲/攻击 数值。
 *
 * 实体创建时已按 createAttributes() 的默认属性满血生成，这里只是把
 * finalizeSpawn（以及 Yeti 的私有 assignIfPresent）里的 setBaseValue / setHealth
 * 调用全部置为空操作。
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
            method = {"finalizeSpawn", "m_6518_", "assignIfPresent"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/attributes/AttributeInstance;setBaseValue(D)V", remap = true)
    )
    private void slashBlade_SenDims$skipConfigSetBaseValue(AttributeInstance instance, double value) {
        // 不在 finalizeSpawn 内进行任何属性基础值设置
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
            method = {"finalizeSpawn", "m_6518_"},
            at = @At(value = "INVOKE", target = "Lnet/unusual/block_factorys_bosses/entity/boss/dragon/boss/InfernalDragonEntity;setHealth(F)V", remap = true),
            require = 0
    )
    private void slashBlade_SenDims$skipDragonSetHealth(InfernalDragonEntity instance, float value) {
        // 不在 finalizeSpawn 内根据配置设置生命值
    }
}
