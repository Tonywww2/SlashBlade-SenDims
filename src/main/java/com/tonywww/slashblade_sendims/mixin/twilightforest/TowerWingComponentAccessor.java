package com.tonywww.slashblade_sendims.mixin.twilightforest;

import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import twilightforest.world.components.structures.lichtower.TowerWingComponent;

import java.util.ArrayList;

@Mixin(value = TowerWingComponent.class, remap = false)
public interface TowerWingComponentAccessor {
    /** Door positions (local); the first one is the door the tower was entered through. */
    @Accessor("openings")
    ArrayList<BlockPos> sendims$getOpenings();
}
