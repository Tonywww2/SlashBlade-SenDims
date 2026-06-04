package com.tonywww.slashblade_sendims.api;

import com.tonywww.slashblade_sendims.SBSDValues;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.Set;

public class RecipeRemovalHelper {

    /**
     * 添加一个需要被移除的配方 ID
     */
    public static void addRemovedId(ResourceLocation id) {
        SBSDValues.REMOVED_RECIPE_IDS.add(id);
    }

    /**
     * 添加一个需要被移除的配方 ID（字符串形式）
     */
    public static void addRemovedIdStr(String id) {
        addRemovedId(ResourceLocation.parse(id));
    }

    /**
     * 移除一个 ID
     */
    public static void removeId(ResourceLocation id) {
        SBSDValues.REMOVED_RECIPE_IDS.remove(id);
    }

    /**
     * 移除一个 ID（字符串形式）
     */
    public static void removeIdStr(String id) {
        removeId(ResourceLocation.parse(id));
    }

    /**
     * 清空所有已移除的 ID
     */
    public static void clearAll() {
        SBSDValues.REMOVED_RECIPE_IDS.clear();
    }

    /**
     * 检查一个 ID 是否在移除列表中
     */
    public static boolean isRemoved(ResourceLocation id) {
        return SBSDValues.REMOVED_RECIPE_IDS.contains(id);
    }

    /**
     * 获取移除列表（返回副本）
     */
    public static Set<ResourceLocation> getRemovedIds() {
        return new HashSet<>(SBSDValues.REMOVED_RECIPE_IDS);
    }
}
