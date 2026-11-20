package com.tonywww.slashblade_sendims.compat.lightspeed;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Lightspeed 只在标题界面出现时关闭启动线程池（GlobalCache.finishStartupCaches）。专用服务端没有标题界面，
 * 客户端用 quick play 直接进存档也会跳过它，这两种情况下二十多个空闲线程会一直留到退出。这里在对应时机补一次。
 * 只做反射联动：没装 Lightspeed 或类结构变了就什么也不做。finishStartupCaches 不能重复调用，所以先检查是否已经关过。
 */
public final class LightspeedStartup {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String GLOBAL_CACHE = "com.ccr4ft3r.lightspeed.cache.GlobalCache";
    private static final String TITLE_SCREEN_INJECTOR = "com.ccr4ft3r.lightspeed.events.TitleScreenInjector";
    private static boolean attempted;

    private LightspeedStartup() {}

    /** 专用服务端启动完成后调用。startupPersistence 非空表示已经关过。 */
    public static synchronized void finishOnDedicatedServer() {
        if (attempted || !ModList.get().isLoaded("lightspeed")) return;
        attempted = true;
        try {
            Class<?> cache = Class.forName(GLOBAL_CACHE);
            Field persistence = cache.getDeclaredField("startupPersistence");
            persistence.setAccessible(true);
            if (persistence.get(null) != null) return;
            cache.getMethod("finishStartupCaches").invoke(null);
            LOGGER.info("Lightspeed startup executors shut down after dedicated server start");
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOGGER.warn("Could not shut down Lightspeed startup executors", e);
        }
    }

    /** 客户端进入世界时调用。走 Lightspeed 自己的标题界面收尾流程，同时置 launchComplete，之后回到标题界面不会再关一次。 */
    public static synchronized void finishSkippedTitleScreen() {
        if (attempted || !ModList.get().isLoaded("lightspeed")) return;
        attempted = true;
        try {
            Class<?> injector = Class.forName(TITLE_SCREEN_INJECTOR);
            Field launchComplete = injector.getDeclaredField("launchComplete");
            launchComplete.setAccessible(true);
            if (launchComplete.getBoolean(null)) return;
            Method finish = injector.getDeclaredMethod("finishTitleInit");
            finish.setAccessible(true);
            finish.invoke(null);
            LOGGER.info("Lightspeed startup executors shut down after joining a world without the title screen");
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOGGER.warn("Could not shut down Lightspeed startup executors", e);
        }
    }
}
