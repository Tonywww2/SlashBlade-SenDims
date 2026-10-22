package com.tonywww.slashblade_sendims.startup;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Reuses an already installed agent. Never attaches an agent or edits JVM arguments. */
public final class EarlyStartupOptimizer {
    private static final Logger LOGGER = LogManager.getLogger("SenDims Startup");
    private static final AtomicBoolean ATTEMPTED = new AtomicBoolean();
    private static volatile boolean opcodeActive;

    private EarlyStartupOptimizer() {}

    public static boolean opcodeActive() { return opcodeActive; }

    public static void install() {
        if (!StartupSettings.INSTANCE.opcodeNameCache || !ATTEMPTED.compareAndSet(false, true)) return;
        try {
            Class<?> target = Class.forName(OpcodeLookupTransformer.TARGET.replace('/', '.'), false,
                    EarlyStartupOptimizer.class.getClassLoader());
            Instrumentation instrumentation = systemInstrumentation();
            if (instrumentation == null) instrumentation = installedInstrumentation(target.getModule().getLayer(), new HashSet<>());
            if (instrumentation == null) instrumentation = installedInstrumentation(ModuleLayer.boot(), new HashSet<>());
            if (instrumentation == null || !instrumentation.isRetransformClassesSupported()
                    || !instrumentation.isModifiableClass(target)) {
                LOGGER.info("Opcode cache inactive: no existing retransformation-capable agent");
                return;
            }
            AtomicBoolean applied = new AtomicBoolean();
            ClassFileTransformer transformer = new ClassFileTransformer() {
                @Override
                public byte[] transform(ClassLoader loader, String name, Class<?> redefined,
                                        ProtectionDomain domain, byte[] bytes) {
                    if (redefined != target || !OpcodeLookupTransformer.TARGET.equals(name)) return null;
                    try {
                        byte[] transformed = OpcodeLookupTransformer.transform(bytes);
                        if (transformed != null) applied.set(true);
                        return transformed;
                    } catch (RuntimeException | IllegalAccessException error) {
                        LOGGER.warn("Opcode cache patch skipped", error);
                        return null;
                    }
                }
            };
            instrumentation.addTransformer(transformer, true);
            try {
                instrumentation.retransformClasses(target);
                opcodeActive = applied.get();
            } finally {
                instrumentation.removeTransformer(transformer);
            }
            LOGGER.info("Opcode lookup cache {}", opcodeActive ? "active" : "skipped for unknown upstream bytecode");
        } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
            LOGGER.warn("Opcode cache unavailable; keeping original Mixin behavior", error);
        } catch (java.lang.instrument.UnmodifiableClassException error) {
            LOGGER.warn("Opcode cache target is not modifiable; keeping original behavior", error);
        }
    }

    private static Instrumentation systemInstrumentation() {
        for (String name : new String[]{"net.bytebuddy.agent.Installer", "net.bytebuddy.agent.ByteBuddyAgent"}) {
            try {
                Class<?> provider = Class.forName(name, false, ClassLoader.getSystemClassLoader());
                Object value = provider.getMethod("getInstrumentation").invoke(null);
                if (value instanceof Instrumentation instrumentation) return instrumentation;
            } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
                // Optional existing providers only; no self-attachment fallback.
            }
        }
        return null;
    }

    private static Instrumentation installedInstrumentation(ModuleLayer layer, Set<ModuleLayer> visited) {
        if (layer == null || !visited.add(layer)) return null;
        for (Module module : layer.modules()) {
            if (!module.getPackages().contains("net.bytebuddy.agent")) continue;
            for (String name : new String[]{"net.bytebuddy.agent.ByteBuddyAgent", "net.bytebuddy.agent.Installer"}) {
                try {
                    Class<?> provider = Class.forName(module, name);
                    if (provider == null) continue;
                    Object value = provider.getMethod("getInstrumentation").invoke(null);
                    if (value instanceof Instrumentation instrumentation) return instrumentation;
                } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
                    // An optional agent provider may be present but not installed in this JVM.
                }
            }
        }
        for (ModuleLayer parent : layer.parents()) {
            Instrumentation found = installedInstrumentation(parent, visited);
            if (found != null) return found;
        }
        return null;
    }
}
