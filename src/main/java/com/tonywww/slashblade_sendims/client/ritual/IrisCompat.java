package com.tonywww.slashblade_sendims.client.ritual;

import com.mojang.logging.LogUtils;
import com.tonywww.slashblade_sendims.client.performance.IrisShadowPass;
import org.slf4j.Logger;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

/**
 * 光影（Oculus/Iris）状态查询。可选 API 只解析一次并绑定 MethodHandle；没装 Oculus 时一律返回 false。
 * 光影开关是动态状态，每次都实时查询，不缓存结果。
 */
final class IrisCompat {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static MethodHandle packInUse;
    private static boolean initialized;

    private IrisCompat() {
    }

    /** 是否正在使用光影包（光影包自带泛光，烘焙光晕据此缩小）。 */
    static boolean shaderPackInUse() {
        if (!initialized) {
            initialized = true;
            try {
                Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                Object instance = api.getMethod("getInstance").invoke(null);
                packInUse = MethodHandles.publicLookup().unreflect(api.getMethod("isShaderPackInUse")).bindTo(instance);
            } catch (ClassNotFoundException ignored) {
                // 没装 Oculus。
            } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
                LOGGER.warn("Could not resolve Iris shader pack API; ritual visuals assume no shader pack", error);
            }
        }
        if (packInUse != null) {
            try {
                return (boolean) packInUse.invokeExact();
            } catch (Throwable error) {
                if (error instanceof VirtualMachineError fatal) throw fatal;
                packInUse = null;
                LOGGER.warn("Iris shader pack API failed; ritual visuals assume no shader pack", error);
            }
        }
        return false;
    }

    /** 光影正在画阴影时跳过世界几何，法阵和光柱不投影。 */
    static boolean shadowPass() {
        return IrisShadowPass.isActive();
    }
}
