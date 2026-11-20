package com.tonywww.slashblade_sendims.mixin.apricityui;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ApricityUI 1.3.1 的 JS 定时器（setTimeout / setInterval）由 java.util.Timer 触发，回调本应交回客户端主线程：
 * ClientScheduler.runOnClientThread 用反射查找 Minecraft.getInstance 再调用 execute。正式环境里该方法名是混淆后的
 * m_91087_，查找失败被吞掉，回调直接在 ApricityUI-Timer 线程执行。界面脚本在这个线程里改 DOM、触发样式重算，
 * 与渲染线程同时使用文档共享的选择器索引（Selector.Index 的 scratchCandidates），会抛 ConcurrentModificationException。
 * 这里改为直接交给 Minecraft 的任务队列，由渲染线程执行。
 */
@Pseudo
@Mixin(targets = "com.sighs.apricityui.task.ClientScheduler", remap = false)
public abstract class ClientSchedulerMainThreadMixin {
    @Inject(method = "runOnClientThread(Ljava/lang/Runnable;)V", at = @At("HEAD"), cancellable = true, remap = false)
    private static void sdbf$runOnRenderThread(Runnable runnable, CallbackInfo ci) {
        if (runnable == null) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) return;
        minecraft.execute(runnable);
        ci.cancel();
    }
}
