package com.tonywww.slashblade_sendims.client.compat.xaero;

import com.tonywww.slashblade_sendims.SenDims;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 本整合包的 Xaero 洞穴模式规则（2026-11-11）。
 * <p>
 * 世界地图里每个维度只有两档：
 * <ul>
 *     <li>普通（自动）：显示地表；保留 Xaero 原有的自动切换，周围头顶都有遮挡（进入地下）时自动进入洞穴视图；</li>
 *     <li>部分层级：始终是洞穴视图（相当于 Xaero 的"手动洞穴模式"，起始高度为"自动"）。</li>
 * </ul>
 * 切片沿用 Xaero 原本的范围：顶端为头顶第一个遮挡方块，最高不超过脚下 +4；从顶端往下 30 层。
 * 起始高度、深度、自动检测的天花板尺寸都固定为 Xaero 默认值，不允许玩家调整；没有"完全"档。
 * 小地图和世界地图共用这一档位：世界地图的洞穴模式按钮和小地图的"手动洞穴模式"快捷键都在两档之间切换。
 * 进入世界时各维度一律回到普通（自动）。相关设置从设置界面移除。
 * <p>
 * Xaero 不是编译依赖，世界地图的类通过反射访问，找不到时小地图保持 Xaero 原逻辑。
 */
public final class XaeroCaveMode {
    public static final int OFF = 0;
    public static final int LAYERED = 1;
    /** 洞穴深度：从切片顶端往下渲染的层数（Xaero 默认值） */
    public static final int CAVE_DEPTH = 30;
    /** 小地图自动洞穴模式的天花板尺寸（Xaero 默认值 2，即检测周围 3×3） */
    public static final int MINIMAP_AUTO_CEILING = 2;
    /** 世界地图自动洞穴模式：-1 = 跟随小地图（Xaero 默认值） */
    public static final int WORLD_MAP_AUTO_CAVE_MODE = -1;
    /** 手动洞穴模式、起始高度为"自动"且头顶没有遮挡时，Xaero 取脚下 +4 作为切片顶端 */
    private static final int MANUAL_TOP_ABOVE_FEET = 4;

    /** 世界地图未安装 */
    public static final int NO_WORLD_MAP = -2;
    /** 世界地图已安装，但会话或当前维度还没就绪 */
    public static final int NOT_READY = -1;

    private static final String KEY_ON = "message.slashblade_sendims.xaero_cave_mode.on";
    private static final String KEY_OFF = "message.slashblade_sendims.xaero_cave_mode.off";
    private static final String KEY_BUTTON = "gui.slashblade_sendims.xaero_cave_mode";
    private static final String KEY_NORMAL = "gui.slashblade_sendims.xaero_cave_mode.normal";
    private static final String KEY_LAYERED = "gui.slashblade_sendims.xaero_cave_mode.layered";

    /** 从小地图和世界地图设置界面里移除的洞穴模式选项（Xaero 的配置项 id） */
    private static final Set<String> HIDDEN_OPTIONS = Set.of(
            "minimap_cave_mode_allowed",
            "minimap_cave_mode_allowed_dimensions",
            "minimap_auto_cave_mode",
            "minimap_cave_mode_depth",
            "minimap_manual_cave_mode_start",
            "minimap_legible_cave_maps",
            "minimap_cave_mode_toggle_timer",
            "minimap_cave_mode_extra_zoom",
            "cave_mode_allowed",
            "cave_mode_allowed_dimensions",
            "cave_mode_depth",
            "legible_cave_maps",
            "auto_cave_mode",
            "cave_mode_toggle_timer",
            "default_cave_mode_type");
    private static final String OPTION_ENTRY_CLASS = "xaero.lib.client.config.option.ui.ConfigOptionScreenEntry";

    /** 世界地图计算洞穴起始高度期间为 true：此时"普通（自动）"也要按分层计算，起始高度由自动检测决定。 */
    private static final ThreadLocal<Boolean> UPDATING_CAVE_START = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private static volatile boolean worldMapResolved;
    private static boolean worldMapPresent;
    private static Method currentSession;
    private static Method mapProcessor;
    private static Method mapWorld;
    private static Method currentDimension;
    private static Method toggleCaveModeType;
    private static Method saveDimensionConfig;
    private static Field caveModeTypeField;
    private static Field uiSync;
    private static boolean settingsFilterFailed;

    private XaeroCaveMode() {}

    /** 部分层级下世界地图自己检测不到遮挡时的切片顶端，与 Xaero 小地图手动洞穴模式相同。 */
    public static int manualTop(double y) {
        return Mth.floor(y) + MANUAL_TOP_ABOVE_FEET;
    }

    public static void beginCaveStartUpdate() {
        UPDATING_CAVE_START.set(Boolean.TRUE);
    }

    public static void endCaveStartUpdate() {
        UPDATING_CAVE_START.set(Boolean.FALSE);
    }

    public static boolean isUpdatingCaveStart() {
        return UPDATING_CAVE_START.get();
    }

    /** 世界地图当前维度保存的档位（不受计算期间的临时值影响），或 NO_WORLD_MAP / NOT_READY。 */
    public static int worldMapCaveType() {
        resolveWorldMap();
        if (!worldMapPresent) return NO_WORLD_MAP;
        try {
            Object dimension = currentWorldMapDimension();
            return dimension == null ? NOT_READY : storedType(dimension);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return NOT_READY;
        }
    }

    /** 世界地图洞穴模式面板的档位按钮文字；面板还没有维度时返回 null，保留原文字。 */
    public static Component caveModeButtonLabel(Object caveModeOptions) {
        resolveWorldMap();
        if (!worldMapPresent) return null;
        try {
            Field field = findField(caveModeOptions.getClass(), "dimension");
            Object dimension = field == null ? null : field.get(caveModeOptions);
            if (dimension == null) return null;
            String mode = storedType(dimension) == LAYERED ? KEY_LAYERED : KEY_NORMAL;
            return Component.translatable(KEY_BUTTON, Component.translatable(mode));
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    /**
     * 小地图快捷键：在当前维度切换"普通（自动）/ 部分层级"，并在动作栏提示。
     * 返回 false 表示没装世界地图，由调用方继续走 Xaero 原逻辑。
     */
    public static boolean toggleFromKey() {
        resolveWorldMap();
        if (!worldMapPresent) return false;
        Boolean layered = null;
        try {
            Object session = currentSession.invoke(null);
            Object processor = session == null ? null : mapProcessor.invoke(session);
            Object world = processor == null ? null : mapWorld.invoke(processor);
            Object dimension = world == null ? null : currentDimension.invoke(world);
            if (dimension != null) {
                synchronized (uiSync.get(processor)) {
                    toggleCaveModeType.invoke(dimension, true);
                    saveDimensionConfig.invoke(dimension);
                    layered = storedType(dimension) == LAYERED;
                }
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            SenDims.LOGGER.warn("Failed to toggle Xaero world map cave mode", e);
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && layered != null) player.displayClientMessage(Component.translatable(layered ? KEY_ON : KEY_OFF), true);
        return true;
    }

    /** 从 Xaero 设置界面的条目数组里去掉洞穴模式相关选项。 */
    public static void hideCaveSettings(Object screen) {
        if (settingsFilterFailed) return;
        try {
            Field entriesField = findField(screen.getClass(), "entries");
            Object[] entries = entriesField == null ? null : (Object[]) entriesField.get(screen);
            if (entries == null) return;
            List<Object> kept = new ArrayList<>(entries.length);
            for (Object entry : entries) {
                if (!isHiddenOption(entry)) kept.add(entry);
            }
            if (kept.size() == entries.length) return;
            Object[] filtered = (Object[]) Array.newInstance(entries.getClass().getComponentType(), kept.size());
            entriesField.set(screen, kept.toArray(filtered));
        } catch (ReflectiveOperationException | RuntimeException e) {
            settingsFilterFailed = true;
            SenDims.LOGGER.warn("Failed to hide Xaero cave mode settings", e);
        }
    }

    private static int storedType(Object dimension) throws IllegalAccessException {
        return caveModeTypeField.getInt(dimension) == OFF ? OFF : LAYERED;
    }

    private static boolean isHiddenOption(Object entry) throws ReflectiveOperationException {
        if (entry == null || !isOptionEntry(entry.getClass())) return false;
        Field optionField = findField(entry.getClass(), "option");
        Object option = optionField == null ? null : optionField.get(entry);
        if (option == null) return false;
        Object id = option.getClass().getMethod("getId").invoke(option);
        return HIDDEN_OPTIONS.contains(id);
    }

    private static boolean isOptionEntry(Class<?> type) {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            if (OPTION_ENTRY_CLASS.equals(c.getName())) return true;
        }
        return false;
    }

    private static Field findField(Class<?> type, String name) {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            try {
                Field field = c.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
            }
        }
        return null;
    }

    private static Object currentWorldMapDimension() throws ReflectiveOperationException {
        Object session = currentSession.invoke(null);
        if (session == null) return null;
        Object processor = mapProcessor.invoke(session);
        if (processor == null) return null;
        Object world = mapWorld.invoke(processor);
        return world == null ? null : currentDimension.invoke(world);
    }

    private static void resolveWorldMap() {
        if (worldMapResolved) return;
        synchronized (XaeroCaveMode.class) {
            if (worldMapResolved) return;
            try {
                ClassLoader loader = XaeroCaveMode.class.getClassLoader();
                Class<?> session = Class.forName("xaero.map.WorldMapSession", false, loader);
                Class<?> processor = Class.forName("xaero.map.MapProcessor", false, loader);
                Class<?> world = Class.forName("xaero.map.world.MapWorld", false, loader);
                Class<?> dimension = Class.forName("xaero.map.world.MapDimension", false, loader);
                currentSession = session.getMethod("getCurrentSession");
                mapProcessor = session.getMethod("getMapProcessor");
                mapWorld = processor.getMethod("getMapWorld");
                currentDimension = world.getMethod("getCurrentDimension");
                toggleCaveModeType = dimension.getMethod("toggleCaveModeType", boolean.class);
                saveDimensionConfig = dimension.getMethod("saveConfigUnsynced");
                caveModeTypeField = dimension.getDeclaredField("caveModeType");
                caveModeTypeField.setAccessible(true);
                uiSync = processor.getField("uiSync");
                worldMapPresent = true;
            } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
                worldMapPresent = false;
            }
            worldMapResolved = true;
        }
    }
}
