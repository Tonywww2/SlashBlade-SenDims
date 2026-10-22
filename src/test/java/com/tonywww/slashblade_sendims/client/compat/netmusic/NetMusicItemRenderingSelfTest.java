package com.tonywww.slashblade_sendims.client.compat.netmusic;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.ZipFile;
import net.minecraft.core.Direction;
import org.joml.Vector4f;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/** Checks both native poses and every installed item-renderer singleton path offline. */
public final class NetMusicItemRenderingSelfTest {
    private static final String PACKAGE = "com/tonywww/slashblade_sendims/client/compat/netmusic/";
    private static final Map<String, String[]> MODELS = Map.of(
            "BigMegaphone", new String[]{"ModelBigMegaphone", "big_megaphone_off.png"},
            "MusicPlayer", new String[]{"ModelMusicPlayer", "music_player.png"});
    private static int checks;

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }

    public static void main(String[] args) throws Exception {
        for (Direction facing : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
            PoseStack actual = new PoseStack();
            BigMegaphoneItemRendering.applyBodyPose(actual, facing);
            // Native transforms verified from the installed renderer bytecode.
            PoseStack nativePose = new PoseStack();
            nativePose.translate(.5, 1.5, .5);
            nativePose.mulPose(Axis.YP.rotationDegrees(facing.get2DDataValue() * 90));
            nativePose.mulPose(Axis.ZP.rotationDegrees(180));
            comparePose(actual, nativePose, "megaphone " + facing);

            actual = new PoseStack();
            MusicPlayerItemRendering.applyItemPose(actual, facing);
            nativePose = new PoseStack();
            nativePose.scale(1.3333334f, 1.3333334f, 1.3333334f);
            nativePose.translate(-0.16666666666666663, 0, -0.16666666666666663);
            nativePose.scale(.75f, .75f, .75f);
            nativePose.translate(.6666666666666666, 1.5, .6666666666666666);
            nativePose.mulPose(Axis.YP.rotationDegrees(180 - facing.get2DDataValue() * 90));
            nativePose.mulPose(Axis.ZP.rotationDegrees(180));
            comparePose(actual, nativePose, "music player " + facing);
        }
        for (String name : MODELS.keySet()) {
            ClassNode replacement = resource(PACKAGE + name + "ItemRendering.class");
            check(replacement.methods.stream().flatMap(NetMusicItemRenderingSelfTest::instructions)
                    .noneMatch(i -> i instanceof FieldInsnNode f && f.owner.startsWith("com/github/tartaricacid/netmusic/client/renderer/")),
                    name + ": no nullable block renderer singleton or shared model access");
            MethodNode render = replacement.methods.stream().filter(m -> m.name.equals("render")).findFirst().orElseThrow();
            check(instructions(render).anyMatch(i -> i instanceof MethodInsnNode m && m.name.equals("entityTranslucent")),
                    name + ": native translucent layer preserved");
            check(!render.tryCatchBlocks.isEmpty(), name + ": balanced pose cleanup even on rendering failure");
        }
        ClassNode cache = resource(PACKAGE + "NetMusicItemModels.class");
        check(cache.methods.stream().flatMap(NetMusicItemRenderingSelfTest::instructions)
                .anyMatch(i -> i instanceof MethodInsnNode m && m.owner.endsWith("/RenderResourceEpoch") && m.name.equals("current")),
                "models participate in existing resource reload invalidation");
        if (args.length != 0) installed(Path.of(args[0]));
        System.out.println("Net Music item rendering checks passed: " + checks);
    }

    private static void comparePose(PoseStack actual, PoseStack expected, String label) {
        check(actual.last().pose().equals(expected.last().pose(), 1e-6f), label + ": native matrix parity");
        for (Vector4f corner : new Vector4f[]{new Vector4f(0, 0, 0, 1), new Vector4f(1, 1, 1, 1),
                new Vector4f(-1, .25f, 2, 1)}) {
            check(actual.last().pose().transform(new Vector4f(corner)).equals(
                    expected.last().pose().transform(new Vector4f(corner)), 1e-6f), label + ": native vertex parity");
        }
    }

    private static Stream<AbstractInsnNode> instructions(MethodNode method) {
        return StreamSupport.stream(method.instructions.spliterator(), false);
    }

    private static ClassNode resource(String path) throws Exception {
        try (var input = NetMusicItemRenderingSelfTest.class.getClassLoader().getResourceAsStream(path)) {
            if (input == null) throw new AssertionError("Missing resource: " + path);
            ClassNode node = new ClassNode();
            new ClassReader(input.readAllBytes()).accept(node, 0);
            return node;
        }
    }

    private static ClassNode read(ZipFile zip, String path) throws Exception {
        try (var input = zip.getInputStream(zip.getEntry(path))) {
            ClassNode node = new ClassNode();
            new ClassReader(input.readAllBytes()).accept(node, 0);
            return node;
        }
    }

    private static void installed(Path mods) throws Exception {
        Path jar;
        try (var files = Files.list(mods)) {
            jar = files.filter(p -> p.getFileName().toString().startsWith("netmusic-") && p.toString().endsWith(".jar"))
                    .findFirst().orElseThrow();
        }
        JsonObject config;
        try (var input = NetMusicItemRenderingSelfTest.class.getClassLoader().getResourceAsStream("slashblade_sendims.mixins.json")) {
            if (input == null) throw new AssertionError("Missing mixin configuration");
            config = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
        }
        Set<String> affected = new HashSet<>();
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            // Discover all item renderers rather than only checking the reported one.
            for (var entry : zip.stream().filter(e -> e.getName().endsWith("ItemRenderer.class")).toList()) {
                ClassNode renderer = read(zip, entry.getName());
                boolean nullableSingleton = renderer.methods.stream().flatMap(NetMusicItemRenderingSelfTest::instructions)
                        .anyMatch(i -> i instanceof FieldInsnNode f && f.getOpcode() == Opcodes.GETSTATIC
                                && f.name.equals("INSTANCE") && f.owner.startsWith("com/github/tartaricacid/netmusic/client/renderer/"));
                if (!nullableSingleton) continue;
                String simple = renderer.name.substring(renderer.name.lastIndexOf('/') + 1);
                String name = simple.substring(0, simple.length() - "ItemRenderer".length());
                affected.add(name);
                check(MODELS.containsKey(name), "every affected item renderer has a supported independent model: " + name);
                String mixin = "netmusic." + simple + "Mixin";
                check(StreamSupport.stream(config.getAsJsonArray("client").spliterator(), false)
                        .anyMatch(value -> value.getAsString().equals(mixin)), "client fix registered: " + name);
                check(StreamSupport.stream(config.getAsJsonArray("mixins").spliterator(), false)
                        .noneMatch(value -> value.getAsString().equals(mixin)), "client renderer never loaded on dedicated servers: " + name);
                String[] api = MODELS.get(name);
                ClassNode model = read(zip, "com/github/tartaricacid/netmusic/client/model/" + api[0] + ".class");
                check(model.superName.equals("net/minecraft/client/model/Model"), name + ": native model render API");
                check(model.fields.stream().anyMatch(f -> f.name.equals("LAYER") && (f.access & Opcodes.ACC_PUBLIC) != 0
                        && f.desc.equals("Lnet/minecraft/client/model/geom/ModelLayerLocation;")), name + ": public native bake layer");
                check(model.methods.stream().anyMatch(m -> m.name.equals("<init>") && (m.access & Opcodes.ACC_PUBLIC) != 0
                        && m.desc.equals("(Lnet/minecraft/client/model/geom/ModelPart;)V")), name + ": native model constructor");
                check(zip.getEntry("assets/netmusic/textures/block/" + api[1]) != null, name + ": original texture exists");
            }
        }
        check(affected.equals(MODELS.keySet()), "both reported crash paths found and covered in the installed Net Music jar");
    }
}
