package com.tonywww.slashblade_sendims.mixin.xaero;

import com.tonywww.slashblade_sendims.client.performance.ClientPerformanceOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * 世界地图每帧在渲染线程上写地图：写入速度固定为每秒 1500 个地块，每次写入的时间上限为距上次写入时长的约 8.7%。
 * 两者按配置倍率同比降低，地图刷新一圈的时间相应变长；小地图有自己的写入器，不受影响。
 * 默认 1.0（与原版相同）：2026-11-17 实测减半后帧时间没有可测差别，开销主要在不受写入速度控制的纹理上传准备。
 */
@Pseudo
@Mixin(targets = "xaero.map.MapWriter", remap = false)
public abstract class WorldMapWriterRateMixin {
    @ModifyConstant(method = "onRender(Lxaero/map/biome/BiomeColorCalculator;Lxaero/map/region/OverlayManager;)V",
            constant = @Constant(intValue = 1500), remap = false)
    private int sdbf$tilesPerSecond(int original) {
        return Math.max(1, (int) Math.round(original * ClientPerformanceOptions.xaeroWorldMapWriteRate));
    }

    @ModifyConstant(method = "onRender(Lxaero/map/biome/BiomeColorCalculator;Lxaero/map/region/OverlayManager;)V",
            constant = @Constant(longValue = 86960L), remap = false)
    private long sdbf$writeBudgetPerMilli(long original) {
        return Math.max(1L, Math.round(original * ClientPerformanceOptions.xaeroWorldMapWriteRate));
    }
}
