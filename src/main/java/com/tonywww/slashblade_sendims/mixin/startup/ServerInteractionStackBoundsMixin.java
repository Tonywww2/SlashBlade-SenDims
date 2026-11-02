package com.tonywww.slashblade_sendims.mixin.startup;

import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.server.level.ServerPlayerGameMode;

/** An early preApply hook only; no gameplay instructions are injected or replaced. */
@Mixin(value = ServerPlayerGameMode.class, priority = 10000)
public abstract class ServerInteractionStackBoundsMixin {
}
