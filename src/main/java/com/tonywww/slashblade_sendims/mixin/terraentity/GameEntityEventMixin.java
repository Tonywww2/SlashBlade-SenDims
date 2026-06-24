package com.tonywww.slashblade_sendims.mixin.terraentity;

import org.confluence.terraentity.event.GameEntityEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = GameEntityEvent.class, remap = false)
public class GameEntityEventMixin {
    @Redirect(
            method = "livingDamageEntity(Lnet/minecraftforge/event/entity/living/LivingDamageEvent;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/EntityType;create(Lnet/minecraft/world/level/Level;)Lnet/minecraft/world/entity/Entity;",
                    ordinal = 0
            ),
            remap = false
    )
    private static Entity sdbf$disableSoulEaterSpawn(EntityType<?> entityType, Level level) {
        return null;
    }
}
