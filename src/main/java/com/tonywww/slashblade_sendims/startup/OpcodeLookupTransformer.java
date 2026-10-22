package com.tonywww.slashblade_sendims.startup;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Schema-preserving patch: no helper class references or fields are added to Mixin's loader. */
public final class OpcodeLookupTransformer {
    public static final String TARGET = "org/spongepowered/asm/util/Bytecode";

    private OpcodeLookupTransformer() {}

    public static Map<Integer, String> names(String marker, int minimum) throws IllegalAccessException {
        Map<Integer, String> result = new LinkedHashMap<>();
        boolean started = false;
        for (Field field : Opcodes.class.getDeclaredFields()) {
            if (marker.equals(field.getName())) started = true;
            if (started && field.getType() == int.class && field.getInt(null) >= minimum) result.putIfAbsent(field.getInt(null), field.getName());
        }
        return result;
    }

    public static byte[] transform(byte[] original) throws IllegalAccessException {
        ClassNode type = new ClassNode();
        new ClassReader(original).accept(type, 0);
        if (!TARGET.equals(type.name)) return null;
        MethodNode method = type.methods.stream()
                .filter(m -> m.name.equals("getOpcodeName") && m.desc.equals("(I)Ljava/lang/String;"))
                .findFirst().orElse(null);
        if (method == null || (method.access & Opcodes.ACC_STATIC) == 0) return null;
        String marker = null;
        int minimum = -1;
        boolean delegate = false;
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof LdcInsnNode constant && constant.cst instanceof String text) marker = text;
            if (instruction.getOpcode() >= Opcodes.ICONST_0 && instruction.getOpcode() <= Opcodes.ICONST_5) minimum = instruction.getOpcode() - Opcodes.ICONST_0;
            if (instruction instanceof MethodInsnNode call && call.owner.equals(TARGET)
                    && call.name.equals("getOpcodeName") && call.desc.equals("(ILjava/lang/String;I)Ljava/lang/String;")) {
                delegate = true;
            }
        }
        if (marker == null || minimum < 0 || !delegate) return null; // Unknown implementation: leave it intact.
        Map<Integer, String> names = names(marker, minimum);
        int maximum = names.keySet().stream().mapToInt(Integer::intValue).max().orElse(-1);
        if (!"IADD".equals(names.get(Opcodes.IADD)) || maximum > 255 || maximum < 199) return null;
        method.instructions.clear();
        method.tryCatchBlocks.clear();
        method.localVariables = null;
        method.visibleLocalVariableAnnotations = null;
        method.invisibleLocalVariableAnnotations = null;
        LabelNode nonnegative = new LabelNode(), unknown = new LabelNode();
        LabelNode[] labels = new LabelNode[maximum + 1];
        for (int i = 0; i < labels.length; i++) labels[i] = names.containsKey(i) ? new LabelNode() : unknown;
        method.instructions.add(new VarInsnNode(Opcodes.ILOAD, 0));
        method.instructions.add(new JumpInsnNode(Opcodes.IFGE, nonnegative));
        method.instructions.add(new LdcInsnNode("UNKNOWN"));
        method.instructions.add(new InsnNode(Opcodes.ARETURN));
        method.instructions.add(nonnegative);
        method.instructions.add(new VarInsnNode(Opcodes.ILOAD, 0));
        method.instructions.add(new TableSwitchInsnNode(0, maximum, unknown, labels));
        for (int i = 0; i < labels.length; i++) {
            if (labels[i] == unknown) continue;
            method.instructions.add(labels[i]);
            method.instructions.add(new LdcInsnNode(names.get(i)));
            method.instructions.add(new InsnNode(Opcodes.ARETURN));
        }
        method.instructions.add(unknown);
        method.instructions.add(new VarInsnNode(Opcodes.ILOAD, 0));
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/String", "valueOf", "(I)Ljava/lang/String;", false));
        method.instructions.add(new InsnNode(Opcodes.ARETURN));
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        type.accept(writer);
        return writer.toByteArray();
    }
}
