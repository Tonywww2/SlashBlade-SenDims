package com.tonywww.slashblade_sendims.mixin.bumblezone;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(
        targets = {
                "com.telepathicgrunt.the_bumblezone.fluids.HoneyFluidBlock",
                "com.telepathicgrunt.the_bumblezone.fluids.RoyalJellyFluidBlock"
        },
        remap = false
)
public abstract class HoneyCrystalFormationMixin {

    @Inject(
            method = "neighboringFluidInteractions",
            at = @At(
                    value = "FIELD",
                    target = "Lcom/telepathicgrunt/the_bumblezone/modinit/BzBlocks;"
                            + "GLISTERING_HONEY_CRYSTAL:"
                            + "Lcom/telepathicgrunt/the_bumblezone/modinit/registry/RegistryEntry;",
                    opcode = Opcodes.GETSTATIC,
                    remap = false
            ),
            cancellable = true,
            require = 3,
            expect = 3,
            allow = 3,
            remap = false
    )
    private void sendims$skipGlisteringCrystalFormation(
            Level level,
            BlockPos pos,
            CallbackInfoReturnable<Boolean> cir
    ) {
        cir.setReturnValue(true);
    }
}