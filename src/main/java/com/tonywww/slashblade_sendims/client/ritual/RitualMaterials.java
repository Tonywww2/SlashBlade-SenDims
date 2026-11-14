package com.tonywww.slashblade_sendims.client.ritual;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * 世界几何用的原版材质，集中在这里，光影实测后可以整体替换。
 * <p>
 * 首选 {@link RenderType#eyes}：叠加混合、不写深度、全亮、受雾淡出；开着光影时 Iris 把它换成
 * gbuffers_spidereyes，多数光影按自发光处理并加泛光。备选是 {@code RenderType.beaconBeam(texture, true)}
 * （gbuffers_beaconbeam）。画布里存的是预乘过的颜色。
 */
final class RitualMaterials {
    private RitualMaterials() {
    }

    static RenderType glow(ResourceLocation texture) {
        return RenderType.eyes(texture);
    }
}
