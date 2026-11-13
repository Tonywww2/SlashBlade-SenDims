package com.tonywww.slashblade_sendims.entities.boss;

/** 暮刃守卫当前前摇的类型，同步到客户端供指示器与渲染使用。 */
public enum TelegraphKind {
    NONE,
    /** 蓝刀：不可招架，前摇显示 Leader 的蓝火粒子环。 */
    BLUE,
    /** 红刀：可招架，前摇按 Leader 标准先显示蓝火粒子环，最后 PARRY_TICK 打开招架窗口并显示「危」。 */
    RED;

    static TelegraphKind byId(int id) {
        TelegraphKind[] values = values();
        return id >= 0 && id < values.length ? values[id] : NONE;
    }
}
