package com.tonywww.slashblade_sendims.client.performance;

import org.lwjgl.system.MemoryUtil;

/**
 * SketchRender 接管区块绘制后，共享四边形索引缓冲只按显卡算出、再读回 CPU 的「最大索引数」扩容。
 * 读回用的持久映射缓冲没有一致性标志、读之前也没有同步，可能长时间停在旧值；容量不够时，
 * 面数多的区块里超出容量的那部分面画不出来。
 * <p>
 * 这里在区块网格数据写入时直接在 CPU 上记下每个区块各朝向索引数之和的最大值，绘制前与读回值取较大者。
 * 区块数据布局与 Embeddium 的 SectionRenderDataUnsafe 相同：偏移 8 起 7 个朝向，每个 12 字节（顶点偏移、索引数、索引偏移）。
 * 只在渲染线程上读写。
 */
public final class SketchIndexCapacity {
    private static final long OFFSET_SLICE_RANGES = 8L;
    private static final long DATA_PER_FACING = 12L;
    private static final long ELEMENT_COUNT_OFFSET = 4L;
    private static final int FACING_COUNT = 7;

    private static int maxElements;

    private SketchIndexCapacity() {
    }

    /** 记录一个区块的索引数；只增不减，容量多留一些没有坏处。 */
    public static void record(long sectionData) {
        if (sectionData == 0L) {
            return;
        }
        int total = 0;
        for (int facing = 0; facing < FACING_COUNT; facing++) {
            total += MemoryUtil.memGetInt(sectionData + OFFSET_SLICE_RANGES + facing * DATA_PER_FACING + ELEMENT_COUNT_OFFSET);
        }
        if (total > maxElements) {
            maxElements = total;
        }
    }

    public static int maxElements() {
        return maxElements;
    }
}
