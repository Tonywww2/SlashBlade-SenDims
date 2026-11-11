package com.tonywww.slashblade_sendims.compat.placebo;

/**
 * 拔刀剑合成转移附魔期间，让 {@code ItemStack#getAllEnchantments()} 只返回物品上真正存储的附魔。
 * <p>
 * Placebo 在 getAllEnchantments 末尾发布 GetEnchantmentLevelEvent，宝石、Tetra 材料等借此临时加等级。
 * 拔刀剑配方把这张合并后的表写回成品 NBT，会让临时加成变成永久附魔。作用域内跳过该事件即可。
 */
public final class StoredEnchantmentScope {
    private static final ThreadLocal<int[]> DEPTH = ThreadLocal.withInitial(() -> new int[1]);

    private StoredEnchantmentScope() {
    }

    public static boolean active() {
        return DEPTH.get()[0] > 0;
    }

    public static void run(Runnable action) {
        int[] depth = DEPTH.get();
        depth[0]++;
        try {
            action.run();
        } finally {
            depth[0]--;
        }
    }
}
