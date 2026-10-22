package com.tonywww.slashblade_sendims.client.startup;

import java.util.concurrent.atomic.LongAdder;

public final class ObjLoadStats {
    public static final LongAdder PARSED = new LongAdder();
    public static final LongAdder DISK_HITS = new LongAdder();
    public static final LongAdder FALLBACKS = new LongAdder();
    private ObjLoadStats() {}
}
