package com.tonywww.slashblade_sendims.client.startup;

import com.tonywww.slashblade_sendims.startup.ObjDiskCache;
import com.tonywww.slashblade_sendims.startup.ObjMeshData;
import com.tonywww.slashblade_sendims.startup.StartupSettings;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class ObjParserBridge {
    public static final Object UPSTREAM_LOCK = new Object();
    public static final ThreadLocal<Boolean> BYPASS = ThreadLocal.withInitial(() -> false);
    private static final Logger LOGGER = LogManager.getLogger("SenDims Startup");
    private static final StartupSettings SETTINGS = StartupSettings.INSTANCE;
    private static final ObjDiskCache CACHE = new ObjDiskCache(Path.of("cache", "slashblade_sendims", "obj-v1"), SETTINGS.objCacheMiB * 1024L * 1024);
    private static final long PENDING_LIMIT = 32L * 1024 * 1024;
    private static final AtomicLong PENDING = new AtomicLong();
    private static final ThreadPoolExecutor WRITER = writer();

    private ObjParserBridge() {}

    public static ObjMeshData parse(byte[] source) throws IOException, ObjMeshData.UnsupportedSyntax {
        String key = null;
        if (SETTINGS.objDiskCache && SETTINGS.objCacheMiB > 0) {
            key = key(source);
            ObjMeshData cached = CACHE.get(key);
            if (cached != null) { ObjLoadStats.DISK_HITS.increment(); return cached; }
        }
        ObjMeshData parsed;
        try (var reader = new InputStreamReader(new ByteArrayInputStream(source), Charset.defaultCharset())) {
            parsed = ObjMeshData.parse(reader);
        }
        ObjLoadStats.PARSED.increment();
        if (key != null) enqueue(key, parsed);
        return parsed;
    }

    private static String key(byte[] source) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update("sendims-obj-v1\0".getBytes(StandardCharsets.UTF_8));
            digest.update(Charset.defaultCharset().name().getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0); digest.update(source);
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    private static void enqueue(String key, ObjMeshData data) {
        long bytes = data.estimatedBytes();
        if (bytes > ObjDiskCache.MAX_ENTRY_BYTES || PENDING.addAndGet(bytes) > PENDING_LIMIT) {
            if (bytes <= ObjDiskCache.MAX_ENTRY_BYTES) PENDING.addAndGet(-bytes);
            return;
        }
        try {
            WRITER.execute(() -> {
                try { CACHE.put(key, data); CACHE.prune(); }
                catch (IOException | RuntimeException error) { LOGGER.debug("OBJ cache write skipped", error); }
                finally { PENDING.addAndGet(-bytes); }
            });
        } catch (java.util.concurrent.RejectedExecutionException full) {
            PENDING.addAndGet(-bytes);
        }
    }

    public static void maintain() {
        try {
            WRITER.execute(() -> {
                try { CACHE.prune(); }
                catch (IOException | RuntimeException error) { LOGGER.debug("OBJ cache cleanup skipped", error); }
            });
        } catch (java.util.concurrent.RejectedExecutionException ignored) {
            // A full writer queue is bounded; maintenance is retried after the next reload.
        }
    }

    private static ThreadPoolExecutor writer() {
        ThreadPoolExecutor result = new ThreadPoolExecutor(1, 1, 10, TimeUnit.SECONDS, new ArrayBlockingQueue<>(128), task -> {
            Thread thread = new Thread(task, "SenDims-OBJ-Cache");
            thread.setDaemon(true); thread.setPriority(Thread.MIN_PRIORITY); return thread;
        });
        result.allowCoreThreadTimeOut(true);
        return result;
    }
}
