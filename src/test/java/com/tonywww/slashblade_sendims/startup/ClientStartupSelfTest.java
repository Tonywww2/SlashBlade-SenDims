package com.tonywww.slashblade_sendims.startup;

import com.tonywww.slashblade_sendims.client.startup.ObjModelAdapter;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipFile;
import mods.flammpfeil.slashblade.client.renderer.model.obj.Face;
import mods.flammpfeil.slashblade.client.renderer.model.obj.GroupObject;
import mods.flammpfeil.slashblade.client.renderer.model.obj.TextureCoordinate;
import mods.flammpfeil.slashblade.client.renderer.model.obj.Vertex;
import mods.flammpfeil.slashblade.client.renderer.model.obj.WavefrontObject;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;

/** Runs the real upstream parser as oracle, without a Minecraft window or Mixin runtime. */
public final class ClientStartupSelfTest {
    private static int checks;
    private static final String TRIANGLE = "v 0 0 0\nv 1 0 0\nv 0 1 0\nvt 0 0\nvt 1 0\nvt 0 1\nvn 0 0 1\ng test\nf 1/1/1 2/2/1 3/3/1\n";
    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }

    public static void main(String[] args) throws Exception {
        opcodes(); settings(); scopedCache(); fixtures(); diskCache(); concurrency(); imagePolicy();
        if (args.length != 0) corpus(Path.of(args[0]));
        System.out.println("Client startup checks passed: " + checks);
    }

    private static void imagePolicy() {
        check(ConfigImagePolicy.defer("sounds", "textures/gui/chat_sounds.webp"), "known optional preview deferred");
        check(!ConfigImagePolicy.defer("quest_enhance", "textures/ftb/basic_tech.gif"), "quest images remain eager");
        check(!ConfigImagePolicy.defer("sounds", "textures/block/example.gif"), "non-config texture remains eager");
        check(!ConfigImagePolicy.defer("other", "textures/gui/preview.webp"), "unknown mod images remain eager");
    }

    private static void opcodes() throws Exception {
        byte[] original;
        try (var input = ClientStartupSelfTest.class.getClassLoader().getResourceAsStream(OpcodeLookupTransformer.TARGET + ".class")) {
            if (input == null) throw new AssertionError("Mixin library missing");
            original = input.readAllBytes();
        }
        byte[] patched = OpcodeLookupTransformer.transform(original);
        check(patched != null, "known upstream opcode method patched");
        check(OpcodeLookupTransformer.transform(patched) == null, "opcode transformation is idempotent");
        Class<?> before = isolated(original), after = isolated(patched);
        Method expected = before.getMethod("getOpcodeName", int.class), actual = after.getMethod("getOpcodeName", int.class);
        for (int opcode = -1024; opcode <= 4096; opcode++) {
            check(expected.invoke(null, opcode).equals(actual.invoke(null, opcode)), "opcode parity " + opcode);
        }
        for (int opcode : new int[]{Integer.MIN_VALUE, Integer.MAX_VALUE}) {
            check(expected.invoke(null, opcode).equals(actual.invoke(null, opcode)), "opcode integer boundary");
        }
        Method nodeBefore = before.getMethod("getOpcodeName", AbstractInsnNode.class);
        Method nodeAfter = after.getMethod("getOpcodeName", AbstractInsnNode.class);
        for (AbstractInsnNode node : Arrays.asList(null, new LabelNode(), new InsnNode(0), new InsnNode(199))) {
            check(nodeBefore.invoke(null, node).equals(nodeAfter.invoke(null, node)), "opcode node overload parity");
        }
    }

    private static Class<?> isolated(byte[] bytecode) throws ClassNotFoundException {
        return new ClassLoader(ClientStartupSelfTest.class.getClassLoader()) {
            @Override protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (!name.equals(OpcodeLookupTransformer.TARGET.replace('/', '.'))) return super.loadClass(name, resolve);
                Class<?> type = findLoadedClass(name);
                if (type == null) type = defineClass(name, bytecode, 0, bytecode.length);
                if (resolve) resolveClass(type);
                return type;
            }
        }.loadClass(OpcodeLookupTransformer.TARGET.replace('/', '.'));
    }

    private static void settings() throws Exception {
        Path directory = Files.createTempDirectory("startup-settings-");
        Path config = directory.resolve(StartupSettings.FILE_NAME);
        try {
            Files.writeString(config, "[startup]\nenabled=false\nobjWorkers=999\nmodelLocationEntries=-1\n");
            StartupSettings disabled = StartupSettings.load(config);
            check(!disabled.opcodeNameCache && !disabled.objParser && !disabled.parallelObjPreparation
                    && !disabled.modelLocationCache && !disabled.modelInputBatches, "global switch disables all features");
            check(disabled.objWorkers >= 1 && disabled.objWorkers <= 8, "invalid worker budget falls back");
            check(disabled.modelLocationEntries == 262144, "invalid memo budget falls back");
            Files.writeString(config, "[startup]\nenabled=true\nobjParser=false\nparallelObjPreparation=true\nobjDiskCache=true\nobjWorkers=2\n");
            StartupSettings serial = StartupSettings.load(config);
            check(!serial.parallelObjPreparation && !serial.objDiskCache, "unsafe upstream parser cannot run in parallel");
            check(serial.objWorkers == 2, "explicit parser budget");
        } finally { Files.deleteIfExists(config); Files.deleteIfExists(directory); }
    }

    private static void scopedCache() {
        ScopedIdentityCache<Object, String, String> cache = new ScopedIdentityCache<>();
        Object first = new String("state"), second = new String("state"), third = new Object();
        long previous = cache.begin(2);
        cache.put(first, "block-a", "a"); cache.put(second, "block-a", "b"); cache.put(third, "block-a", "c");
        check("a".equals(cache.get(first, "block-a")) && "b".equals(cache.get(second, "block-a")), "state identity preserved");
        check(cache.get(first, "block-b") == null && cache.get(third, "block-a") == null, "context and capacity isolation");
        long current = cache.begin(2); cache.put(first, "block-b", "new"); cache.finish(previous);
        check("new".equals(cache.get(first, "block-b")), "old reload completion cannot clear a newer generation");
        cache.finish(current); check(cache.size() == 0 && cache.get(first, "block-b") == null, "reload completion releases keys");
    }

    private static void fixtures() throws Exception {
        for (String source : List.of(TRIANGLE, TRIANGLE.replace(' ', '\t'), TRIANGLE.replace("g test", "o test.1"),
                TRIANGLE.replace("f 1/1/1 2/2/1 3/3/1", "f 1 2 3"), TRIANGLE.replace("f 1/1/1 2/2/1 3/3/1", "f 1/1 2/2 3/3"),
                TRIANGLE.replace("f 1/1/1 2/2/1 3/3/1", "f 1//1 2//1 3//1"),
                "# comment\nmtllib some path with many tokens that must be ignored\n" + TRIANGLE,
                "v 0 0 0 1\nvn 0 0 1 1\ng empty\n" + TRIANGLE,
                TRIANGLE.replace("0 0 0", "-0.0 0.0 0.000"),
                "v 0 0 0\nv 1 0 0\nv 1 1 0\nv 0 1 0\nf 1 2 3 4\n")) {
            byte[] bytes = source.getBytes(Charset.defaultCharset());
            check(digest(upstream(bytes)).equals(digest(optimized(bytes))), "fixture parser parity");
        }
        for (String source : List.of("v 1 2\n", "v 1e2 2 3\n", "v +1 2 3\n", "g invalid-name\n", "f -1 -2 -3\n", "f 0 0 0\n",
                TRIANGLE + "f 1 2 3 1\n", "v 1 2 3 # comment\n", "f 999999999999999999999 2 3\n")) {
            boolean fallback = false;
            try { ObjMeshData.parse(new StringReader(source)); }
            catch (ObjMeshData.UnsupportedSyntax unsupported) { fallback = true; }
            check(fallback, "unsupported input retains upstream error path");
        }
    }

    private static WavefrontObject upstream(byte[] bytes) { return new WavefrontObject("parity.obj", new ByteArrayInputStream(bytes)); }
    private static WavefrontObject optimized(byte[] bytes) throws Exception {
        ObjMeshData data = ObjMeshData.parse(new java.io.InputStreamReader(new ByteArrayInputStream(bytes), Charset.defaultCharset()));
        data.validate();
        WavefrontObject result = upstream(new byte[0]); ObjModelAdapter.apply(data, result); return result;
    }

    private static String digest(WavefrontObject model) throws Exception {
        MessageDigest hash = MessageDigest.getInstance("SHA-256");
        try (DataOutputStream out = new DataOutputStream(new DigestOutputStream(OutputStream.nullOutputStream(), hash))) {
            IdentityHashMap<Vertex, Integer> vertices = points(out, model.vertices), normals = points(out, model.vertexNormals);
            IdentityHashMap<TextureCoordinate, Integer> textures = new IdentityHashMap<>();
            out.writeInt(model.textureCoordinates.size());
            for (int i = 0; i < model.textureCoordinates.size(); i++) {
                TextureCoordinate texture = model.textureCoordinates.get(i); textures.put(texture, i);
                out.writeInt(Float.floatToRawIntBits(texture.u)); out.writeInt(Float.floatToRawIntBits(texture.v)); out.writeInt(Float.floatToRawIntBits(texture.w));
            }
            out.writeInt(model.groupObjects.size());
            for (GroupObject group : model.groupObjects) {
                out.writeUTF(group.name); out.writeInt(group.glDrawingMode); out.writeInt(group.faces.size());
                for (Face face : group.faces) {
                    out.writeInt(face.vertices.length);
                    out.writeBoolean(face.textureCoordinates != null); out.writeBoolean(face.vertexNormals != null);
                    for (int i = 0; i < face.vertices.length; i++) {
                        out.writeInt(vertices.get(face.vertices[i]));
                        if (face.textureCoordinates != null) out.writeInt(textures.get(face.textureCoordinates[i]));
                        if (face.vertexNormals != null) out.writeInt(normals.get(face.vertexNormals[i]));
                    }
                    out.writeInt(Float.floatToRawIntBits(face.faceNormal.x)); out.writeInt(Float.floatToRawIntBits(face.faceNormal.y));
                    out.writeInt(Float.floatToRawIntBits(face.faceNormal.z));
                }
            }
        }
        return HexFormat.of().formatHex(hash.digest());
    }

    private static IdentityHashMap<Vertex, Integer> points(DataOutputStream out, List<Vertex> values) throws Exception {
        IdentityHashMap<Vertex, Integer> identities = new IdentityHashMap<>(); out.writeInt(values.size());
        for (int i = 0; i < values.size(); i++) {
            Vertex point = values.get(i); identities.put(point, i);
            out.writeInt(Float.floatToRawIntBits(point.x)); out.writeInt(Float.floatToRawIntBits(point.y)); out.writeInt(Float.floatToRawIntBits(point.z));
        }
        return identities;
    }

    private static void diskCache() throws Exception {
        Path directory = Files.createTempDirectory("obj-cache-");
        ObjDiskCache cache = new ObjDiskCache(directory, 1024 * 1024);
        String key = "a".repeat(64), other = "b".repeat(64);
        try {
            ObjMeshData data = ObjMeshData.parse(new StringReader(TRIANGLE)); cache.put(key, data);
            ObjMeshData decoded = cache.get(key); check(decoded != null, "binary cache round trip");
            WavefrontObject one = upstream(new byte[0]), two = upstream(new byte[0]);
            ObjModelAdapter.apply(decoded, one); ObjModelAdapter.apply(decoded, two);
            check(digest(one).equals(digest(two)), "binary model materialization parity");
            one.vertices.get(0).x = 17;
            check(two.vertices.get(0).x == 0, "cache hits never share mutable model vertices");
            check(cache.get(other) == null && cache.get("../bad") == null, "content change and invalid path miss");
            Files.write(directory.resolve(key + ".bin"), new byte[]{1, 2, 3}); check(cache.get(key) == null, "corrupt cache falls back");
            cache.put(other, data); new ObjDiskCache(directory, 0).prune();
            check(!Files.exists(directory.resolve(other + ".bin")), "disk cache budget eviction");
        } finally {
            try (var files = Files.newDirectoryStream(directory)) { for (Path path : files) Files.delete(path); }
            Files.delete(directory);
        }
    }

    private static void concurrency() throws Exception {
        var executor = Executors.newFixedThreadPool(8);
        AtomicInteger active = new AtomicInteger(), peak = new AtomicInteger();
        try {
            List<Integer> values = BoundedPreparation.map(1024, 4, executor, index -> {
                int now = active.incrementAndGet(); peak.accumulateAndGet(now, Math::max);
                try { ObjMeshData.parse(new StringReader(TRIANGLE)).validate(); return index; }
                catch (Exception error) { throw new AssertionError(error); }
                finally { active.decrementAndGet(); }
            }).get(30, TimeUnit.SECONDS);
            check(peak.get() <= 4 && active.get() == 0, "parallel parser job budget");
            for (int i = 0; i < values.size(); i++) check(values.get(i) == i, "parallel output ordering");
            check(BoundedPreparation.map(0, 1, executor, index -> index).join().isEmpty(), "empty batch completes");
            try {
                BoundedPreparation.map(8, 2, executor, index -> { if (index == 3) throw new IllegalStateException("expected"); return index; }).join();
                throw new AssertionError("exception swallowed");
            } catch (java.util.concurrent.CompletionException expected) { check(expected.getCause() instanceof IllegalStateException, "async error propagation"); }
            var single = Executors.newSingleThreadExecutor();
            try { check(BoundedPreparation.map(10, 4, single, index -> index).get(10, TimeUnit.SECONDS).size() == 10, "single-thread executor does not deadlock"); }
            finally { single.shutdownNow(); }
        } finally { executor.shutdownNow(); }
    }

    private static void corpus(Path mods) throws Exception {
        int models = 0, supported = 0;
        List<String> rows = new ArrayList<>(List.of("jar\tresource\tfast_parser\tsha256"));
        try (var paths = Files.newDirectoryStream(mods, "*.jar")) {
            for (Path path : paths) try (ZipFile archive = new ZipFile(path.toFile())) {
                var entries = archive.entries();
                while (entries.hasMoreElements()) {
                    var entry = entries.nextElement(); String name = entry.getName();
                    if (!name.startsWith("assets/") || !name.contains("/model/") || !name.endsWith(".obj")) continue;
                    byte[] source; try (var input = archive.getInputStream(entry)) { source = input.readAllBytes(); }
                    models++;
                    WavefrontObject before;
                    try { before = upstream(source); }
                    catch (RuntimeException invalid) {
                        boolean rejected = false;
                        try { optimized(source); } catch (Exception expected) { rejected = true; }
                        check(rejected, "invalid upstream asset is not accepted: " + name);
                        rows.add(path.getFileName() + "\t" + name + "\trejected\t-"); continue;
                    }
                    String expected = digest(before);
                    try {
                        WavefrontObject after = optimized(source);
                        String actual = digest(after); check(expected.equals(actual), "asset parity: " + path.getFileName() + "/" + name);
                        supported++; rows.add(path.getFileName() + "\t" + name + "\ttrue\t" + actual);
                    } catch (ObjMeshData.UnsupportedSyntax fallback) {
                        rows.add(path.getFileName() + "\t" + name + "\tfallback\t" + expected);
                    }
                }
            }
        }
        check(models > 0, "asset corpus is present"); check(supported > 0, "optimized parser covers real assets");
        Files.write(Path.of("obj-corpus-parity.tsv"), rows, StandardCharsets.UTF_8);
        System.out.println("OBJ corpus: entries=" + models + " optimized=" + supported + " exact parity verified");
    }
}
