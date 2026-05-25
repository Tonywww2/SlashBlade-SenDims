package com.tonywww.slashblade_sendims.mixin.minecraft;

import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DamageSource.class)
public abstract class DamageSourceMixin {

    /**
     * 拦截 DamageSource 的 scalesWithDifficulty 方法 (SRG: m_7986_)。
     * 在方法执行的最开头 (HEAD) 注入，强制返回 false。
     * * Intercept the scalesWithDifficulty method in DamageSource.
     * Inject at the very beginning (HEAD) and forcefully return false.
     */
    @Inject(method = "scalesWithDifficulty", at = @At("HEAD"), cancellable = true)
    private void disableDifficultyScaling(CallbackInfoReturnable<Boolean> cir) {
        // 强行修改返回值为 false，告诉游戏：“这个伤害绝对不随难度变化”
        // Forcefully change the return value to false, telling the game: "This damage NEVER scales with difficulty"
        cir.setReturnValue(false);
    }
}