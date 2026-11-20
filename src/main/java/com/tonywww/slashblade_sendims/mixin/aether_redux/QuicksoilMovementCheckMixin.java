package com.tonywww.slashblade_sendims.mixin.aether_redux;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Aether Redux 的流沙移动处理器在两端对每个生物每 tick 运行，先查脚下影响移动的方块是否已加载（这一步还会经过
 * YUNG 洞穴群系的注入再读一次方块），之后才判断是否着地、是否在水中、是否在动。这里把这几个只读字段的判断提到最前面，
 * 任一不满足就跳过；全部满足时仍由原处理器按原顺序完整判断，结果与原来一致。
 */
@Pseudo
@Mixin(targets = "net.zepalesque.redux.event.listener.MobListener", remap = false)
public abstract class QuicksoilMovementCheckMixin {
    @Inject(method = "movementHandling(Lnet/minecraftforge/event/entity/living/LivingEvent$LivingTickEvent;)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private static void sdbf$cheapChecksFirst(LivingEvent.LivingTickEvent event, CallbackInfo ci) {
        LivingEntity entity = event.getEntity();
        Vec3 motion = entity.getDeltaMovement();
        if (!entity.onGround() || entity.isInWater()
                || Math.abs(motion.x + motion.y + motion.z) < 0.001
                || entity instanceof Player player && player.isSpectator()) {
            ci.cancel();
        }
    }
}
