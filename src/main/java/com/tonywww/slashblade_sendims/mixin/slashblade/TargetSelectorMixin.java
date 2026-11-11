package com.tonywww.slashblade_sendims.mixin.slashblade;

import com.tonywww.slashblade_sendims.registeries.SBSDTags;
import mods.flammpfeil.slashblade.util.TargetSelector;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
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

    @Inject(method = "getTargettableEntitiesWithinAABB(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/phys/AABB;D)Ljava/util/List;", at = @At("RETURN"))
    private static void sendims$addNonLivingMeleeTargets(Level level, LivingEntity attacker, AABB area,
                                                        double reach, CallbackInfoReturnable<List<Entity>> cir) {
        List<Entity> targets = cir.getReturnValue();
        targets.addAll(level.getEntities(attacker, area, entity -> entity.isAlive()
                && entity.getType().is(SBSDTags.EntityTypes.NON_LIVING_SLASH_TARGETS)
                && entity.distanceToSqr(attacker) < reach * reach && !targets.contains(entity)));
    }

    @Inject(method = "getTargettableEntitiesWithinAABB(Lnet/minecraft/world/level/Level;DLnet/minecraft/world/entity/Entity;)Ljava/util/List;", at = @At("RETURN"))
    private static void sendims$addNonLivingSlashTargets(Level level, double reach, Entity slash,
                                                        CallbackInfoReturnable<List<Entity>> cir) {
        List<Entity> targets = cir.getReturnValue();
        targets.addAll(level.getEntities(slash, slash.getBoundingBox().inflate(reach), entity -> entity.isAlive()
                && entity.getType().is(SBSDTags.EntityTypes.NON_LIVING_SLASH_TARGETS) && !targets.contains(entity)));
    }
}
