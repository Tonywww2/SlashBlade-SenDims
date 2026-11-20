package com.tonywww.slashblade_sendims.mixin.sbr_core;

import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.entity.living.LivingEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * 按 mrqx 拔刀剑核心 1.4.1 编写。它的 LivingTickEvent 处理器在两端对每个非玩家生物每 tick：查一次脚下方块、
 * 新建步高加成修饰符、先移除再按"主手是拔刀剑"加回，然后递减三个闪避/空中突进计数。移除即使修饰符不存在也会把
 * 同步属性 forge:step_height_addition 标脏，服务端因此为每只被追踪的生物定期发属性同步包。
 * 主手没有拔刀剑、身上没有这个加成、三个计数也都不存在时，原处理器什么都不会改变，这里直接跳过；其余情况照常交给原处理器。
 */
@Pseudo
@Mixin(targets = "net.mrqx.sbr_core.utils.MrqxSlayerStyleArts", remap = false)
public abstract class MrqxSlayerStyleArtsTickMixin {
    @Unique
    private static final UUID SDBF$STEP_UP_BONUS = UUID.fromString("c8158a43-a96f-4db9-8858-57294fbe0ebb");

    @Inject(method = "onLivingTickEvent(Lnet/minecraftforge/event/entity/living/LivingEvent$LivingTickEvent;)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private static void sdbf$skipIdleMobs(LivingEvent.LivingTickEvent event, CallbackInfo ci) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player) return;
        CompoundTag data = entity.getPersistentData();
        if (data.contains("sb.avoid.trickup") || data.contains("sb.avoid.counter") || data.contains("sb.airtrick.counter")) return;
        AttributeInstance stepHeight = entity.getAttribute(ForgeMod.STEP_HEIGHT_ADDITION.get());
        if (stepHeight != null && stepHeight.getModifier(SDBF$STEP_UP_BONUS) != null) return;
        if (entity.getMainHandItem().getCapability(ItemSlashBlade.BLADESTATE).isPresent()) return;
        ci.cancel();
    }
}
