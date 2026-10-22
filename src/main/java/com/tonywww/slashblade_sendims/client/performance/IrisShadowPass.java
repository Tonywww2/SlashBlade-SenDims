package com.tonywww.slashblade_sendims.client.performance;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

/** Resolve the optional API once, then query its dynamic state without reflective varargs. */
public final class IrisShadowPass {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static MethodHandle query;
    private static boolean initialized;

    private IrisShadowPass() {}

    public static boolean isActive() {
        if (!initialized) {
            initialized = true;
            try {
                Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                Object instance = api.getMethod("getInstance").invoke(null);
                query = MethodHandles.publicLookup().unreflect(api.getMethod("isRenderingShadowPass"))
                        .bindTo(instance);
            } catch (ClassNotFoundException ignored) {
                // Oculus is optional. This class is first queried from a loaded client's renderer.
            } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
                LOGGER.warn("Could not resolve Iris shadow API; keeping ordinary Neat rendering", error);
            }
        }
        if (query != null) {
            try {
                return (boolean) query.invokeExact();
            } catch (Throwable error) {
                if (error instanceof VirtualMachineError fatal) {
                    throw fatal;
                }
                if (error instanceof ThreadDeath fatal) {
                    throw fatal;
                }
                query = null;
                LOGGER.warn("Iris shadow API failed; keeping ordinary Neat rendering", error);
            }
        }
        return false;
    }
}
