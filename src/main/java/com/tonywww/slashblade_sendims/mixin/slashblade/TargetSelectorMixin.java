package com.tonywww.slashblade_sendims.mixin.slashblade;

import mods.flammpfeil.slashblade.util.TargetSelector;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.extensions.IForgeEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Function;
import java.util.stream.Stream;

@Mixin(value = TargetSelector.class,remap = false)
public class TargetSelectorMixin {
    @Redirect(method = "getTargettableEntitiesWithinAABB(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/phys/AABB;D)Ljava/util/List;", at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;flatMap(Ljava/util/function/Function;)Ljava/util/stream/Stream;"))
    private static Stream<Entity> fl(Stream<LivingEntity> instance, Function<Entity, Stream<Entity>> function) {
        return instance.flatMap(entity -> entity.isMultipartEntity() ?
                Stream.concat(Stream.of(entity.getParts()), Stream.of(entity)) :
                Stream.of(entity));
    }
}
