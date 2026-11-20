package com.tonywww.slashblade_sendims.client.performance;

import org.cyclops.integrateddynamics.api.part.IPartContainer;
import org.cyclops.integrateddynamics.capability.partcontainer.PartContainerDefault;
import org.cyclops.integrateddynamics.client.render.part.PartOverlayRenderers;
import org.cyclops.integrateddynamics.core.helper.PartHelpers;

/**
 * RenderCable 对每个部件查覆盖层渲染器注册表，查不到就什么也不画。
 * 这里按同一张注册表实时判断（不缓存，附属模组晚注册也照常生效），未知的部件容器一律交给原实现。
 */
public final class CableOverlayFilter {
    private CableOverlayFilter() {}

    public static boolean hasOverlay(IPartContainer container) {
        if (!(container instanceof PartContainerDefault parts)) return true;
        if (!parts.hasParts()) return false;
        for (PartHelpers.PartStateHolder<?, ?> holder : parts.getPartData().values()) {
            if (holder == null || !PartOverlayRenderers.REGISTRY.getRenderers(holder.getPart()).isEmpty()) return true;
        }
        return false;
    }
}
