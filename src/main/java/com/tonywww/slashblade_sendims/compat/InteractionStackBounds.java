package com.tonywww.slashblade_sendims.compat;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;

/** Repair post-coremod stack bounds before Quests Additions captures useItemOn locals. */
public final class InteractionStackBounds {
    private InteractionStackBounds() {}

    public static void prepare(String targetName, ClassNode target, String mixinName) {
        if (!mixinName.endsWith(".startup.ServerInteractionStackBoundsMixin")
                || !targetName.equals("net.minecraft.server.level.ServerPlayerGameMode")) {
            return;
        }
        for (MethodNode method : target.methods) {
            if ((method.name.equals("m_7179_") || method.name.equals("useItemOn"))
                    && method.desc.equals("(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;")) {
                try {
                    // BasicInterpreter needs no Minecraft classes or class hierarchy lookups.
                    new Analyzer<>(new BasicInterpreter()).analyzeAndComputeMaxs(target.name, method);
                } catch (AnalyzerException e) {
                    throw new IllegalStateException("Cannot compute useItemOn stack bounds", e);
                }
            }
        }
    }
}
