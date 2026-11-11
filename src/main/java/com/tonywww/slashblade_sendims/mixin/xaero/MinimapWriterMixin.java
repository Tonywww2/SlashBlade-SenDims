package com.tonywww.slashblade_sendims.mixin.xaero;

import com.tonywww.slashblade_sendims.client.compat.xaero.XaeroCaveMode;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 小地图洞穴切片沿用 Xaero 原逻辑（部分层级档由 MinimapProcessorMixin 映射为手动洞穴模式），
 * 只把可调参数固定为 Xaero 默认值：自动检测的天花板尺寸、手动起始高度（自动）、洞穴深度。
 */
@Pseudo
@Mixin(targets = "xaero.common.minimap.write.MinimapWriter", remap = false)
public abstract class MinimapWriterMixin {
    @Shadow(remap = false)
    private int loadingCaveMapsDepth;

    /** getCaving 里第一个 intValue 读取自动洞穴模式（天花板尺寸）。 */
    @Redirect(method = "getCaving", at = @At(value = "INVOKE", target = "Ljava/lang/Integer;intValue()I", ordinal = 0), remap = false)
    private int sdbf$defaultAutoCeiling(Integer configured) {
        return XaeroCaveMode.MINIMAP_AUTO_CEILING;
    }

    /** 第二个 intValue 读取手动洞穴起始高度，按"自动"处理。 */
    @Redirect(method = "getCaving", at = @At(value = "INVOKE", target = "Ljava/lang/Integer;intValue()I", ordinal = 1), remap = false)
    private int sdbf$noManualStart(Integer configured) {
        return Integer.MAX_VALUE;
    }

    @Inject(method = "writeChunk", at = @At(value = "FIELD",
            target = "Lxaero/common/minimap/write/MinimapWriter;loadingCaveMapsDepth:I",
            opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER), remap = false)
    private void sdbf$defaultDepth(CallbackInfoReturnable<Boolean> cir) {
        this.loadingCaveMapsDepth = XaeroCaveMode.CAVE_DEPTH;
    }
}
