package com.tonywww.slashblade_sendims.mixin.pseudoedge;

import com.dinzeer.pseudoedge_break_dawn.register.PbdComboState;
import mods.flammpfeil.slashblade.ability.StunManager;
import mods.flammpfeil.slashblade.init.DefaultResources;
import mods.flammpfeil.slashblade.registry.combo.ComboState;
import mods.flammpfeil.slashblade.slasharts.JudgementCut;
import mods.flammpfeil.slashblade.util.AttackManager;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import com.dinzeer.pseudoedge_break_dawn.Pseudoedge_break_dawn;
import com.dinzeer.pseudoedge_break_dawn.sa.DriveSumon;
import com.dinzeer.pseudoedge_break_dawn.sa.SommonSwordSommon;
import com.dinzeer.pseudoedge_break_dawn.sa.Thrust;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.slasharts.CircleSlash;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.function.Supplier;

@Mixin(value = PbdComboState.class, remap = false)
public class PbdComboStateMixin {

    @Redirect(
            method = "<clinit>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraftforge/registries/DeferredRegister;register(Ljava/lang/String;Ljava/util/function/Supplier;)Lnet/minecraftforge/registries/RegistryObject;"),
            remap = false
    )
    private static RegistryObject<ComboState> redirectComboRegister(
            DeferredRegister<ComboState> deferredRegister,
            String name,
            Supplier<ComboState> supplier) {

        switch (name) {
            case "all_reuse":
                return deferredRegister.register(name, () ->
                        ComboState.Builder.newInstance().startAndEnd(459, 488).priority(50)
                                .motionLoc(DefaultResources.ExMotionLocation)
                                .next((entity) -> SlashBlade.prefix("none"))
                                .nextOfTimeout((entity) -> SlashBlade.prefix("none"))
                                .addTickAction(ComboState.TimeLineTickAction.getBuilder()
                                        .put(0, AttackManager::playQuickSheathSoundAction)
                                        .build())
                                .releaseAction(ComboState::releaseActionQuickCharge)
                                .build()
                );

            case "thrusteex":
                return deferredRegister.register(name, () ->
                        ComboState.Builder.newInstance().startAndEnd(400, 459).priority(50)
                                .motionLoc(DefaultResources.ExMotionLocation)
                                .next(ComboState.TimeoutNext.buildFromFrame(15, (entity) -> SlashBlade.prefix("none")))
                                .nextOfTimeout((entity) -> Pseudoedge_break_dawn.prefix("all_reuse"))
                                .addTickAction(ComboState.TimeLineTickAction.getBuilder()
                                        .put(1, (entityIn) -> AttackManager.doSlash(entityIn, -30.0F, Vec3.ZERO, false, false, 0.1D))
                                        .put(2, (entityIn) -> Thrust.doSlash(entityIn, 90.0F))
                                        .build())
                                .addHitEffect(StunManager::setStun)
                                .build()
                );

            case "dragon_boost":
                return deferredRegister.register(name, () ->
                        ComboState.Builder.newInstance().startAndEnd(400, 459).priority(50)
                                .motionLoc(DefaultResources.ExMotionLocation)
                                .next(ComboState.TimeoutNext.buildFromFrame(15, (entity) -> SlashBlade.prefix("none")))
                                .nextOfTimeout((entity) -> Pseudoedge_break_dawn.prefix("all_reuse"))
                                .addTickAction(ComboState.TimeLineTickAction.getBuilder()
                                        .put(1, (entityIn) -> AttackManager.doSlash(entityIn, 901.0F, Vec3.ZERO, false, false, 0.75D))
                                        .put(2, (entityIn) -> DriveSumon.doSlash(entityIn, 45.0F, Vec3.ZERO, true, 5.0F, 0.4F, 10, 16711680))
                                        .put(3, (entityIn) -> SommonSwordSommon.doSlash(entityIn, 16711680, true, 8.0D, 3.0F, 1, 4, 1))
                                        .put(4, (entityIn) -> SommonSwordSommon.doSlash(entityIn, 16711680, true, 8.0D, 4.0F, 1, 2, 1))
                                        .put(5, (entityIn) -> {
                                            entityIn.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 900, 3));
                                            entityIn.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 900, 3));
                                        }).build())
                                .addHitEffect(StunManager::setStun)
                                .build()
                );

            case "black_hole":
                return deferredRegister.register(name, () ->
                        ComboState.Builder.newInstance().startAndEnd(400, 459).priority(50)
                                .motionLoc(DefaultResources.ExMotionLocation)
                                .next(ComboState.TimeoutNext.buildFromFrame(15, (entity) -> SlashBlade.prefix("none")))
                                .nextOfTimeout((entity) -> Pseudoedge_break_dawn.prefix("all_reuse"))
                                .addTickAction(ComboState.TimeLineTickAction.getBuilder()
                                        .put(1, JudgementCut::doJudgementCutJust)
                                        .put(3, (entityIn) -> SommonSwordSommon.doSlash(entityIn, 16777215, true, 10.0D, 3.0F, 1, 5, 1))
                                        .put(4, (entityIn) -> SommonSwordSommon.doSlash(entityIn, 16777215, true, 10.0D, 3.0F, 1, 2, 1))
                                        .build())
                                .addHitEffect(StunManager::setStun)
                                .build()
                );

            case "kingblade":
                return deferredRegister.register(name, () ->
                        ComboState.Builder.newInstance().startAndEnd(400, 459).priority(50)
                                .motionLoc(DefaultResources.ExMotionLocation)
                                .next(ComboState.TimeoutNext.buildFromFrame(15, (entity) -> SlashBlade.prefix("none")))
                                .nextOfTimeout((entity) -> Pseudoedge_break_dawn.prefix("all_reuse"))
                                .addTickAction(ComboState.TimeLineTickAction.getBuilder()
                                        .put(1, (entityIn) -> AttackManager.doSlash(entityIn, 45.0F, Vec3.ZERO, false, false, 0.75D))
                                        .put(2, (entityIn) -> AttackManager.doSlash(entityIn, -45.0F, Vec3.ZERO, false, false, 0.75D))
                                        .put(3, (entityIn) -> AttackManager.doSlash(entityIn, 45.0F, Vec3.ZERO, false, false, 0.75D))
                                        .put(4, (entityIn) -> AttackManager.doSlash(entityIn, -45.0F, Vec3.ZERO, false, false, 0.75D))
                                        .put(5, (entityIn) -> DriveSumon.doSlash(entityIn, 45.0F, Vec3.ZERO, false, 1.0F, 1.0F, 10, 16711680))
                                        .put(6, (entityIn) -> DriveSumon.doSlash(entityIn, -45.0F, Vec3.ZERO, false, 1.0F, 1.0F, 10, 16711680))
                                        .put(7, (entityIn) -> DriveSumon.doSlash(entityIn, 45.0F, Vec3.ZERO, false, 1.0F, 1.0F, 10, 16711680))
                                        .put(8, (entityIn) -> DriveSumon.doSlash(entityIn, -45.0F, Vec3.ZERO, false, 1.0F, 1.0F, 10, 16711680))
                                        .build())
                                .addHitEffect(StunManager::setStun)
                                .build()
                );

            case "black_slash":
                return deferredRegister.register(name, () ->
                        ComboState.Builder.newInstance().startAndEnd(400, 459).priority(50)
                                .motionLoc(DefaultResources.ExMotionLocation)
                                .next(ComboState.TimeoutNext.buildFromFrame(15, (entity) -> SlashBlade.prefix("none")))
                                .nextOfTimeout((entity) -> Pseudoedge_break_dawn.prefix("all_reuse"))
                                .addTickAction(ComboState.TimeLineTickAction.getBuilder()
                                        .put(1, (entityIn) -> AttackManager.doSlash(entityIn, 90.0F, Vec3.ZERO, false, false, 0.75D))
                                        .put(3, (entityIn) -> SommonSwordSommon.doSlash(entityIn, 0, true, 5.0D, 3.0F, 1, 6, 1))
                                        .put(4, (entityIn) -> SommonSwordSommon.doSlash(entityIn, 0, true, 5.0D, 3.0F, 1, 4, 1))
                                        .build())
                                .addHitEffect(StunManager::setStun)
                                .build()
                );

            case "thrustslash":
                return deferredRegister.register(name, () ->
                        ComboState.Builder.newInstance().startAndEnd(400, 459).priority(50)
                                .motionLoc(DefaultResources.ExMotionLocation)
                                .next(ComboState.TimeoutNext.buildFromFrame(15, (entity) -> SlashBlade.prefix("none")))
                                .nextOfTimeout((entity) -> Pseudoedge_break_dawn.prefix("all_reuse"))
                                .addTickAction(ComboState.TimeLineTickAction.getBuilder()
                                        .put(1, (entityIn) -> AttackManager.doSlash(entityIn, -30.0F, Vec3.ZERO, false, false, 0.1D))
                                        .put(2, (entityIn) -> Thrust.doSlash(entityIn, 9.0F))
                                        .put(6, (entityIn) -> AttackManager.doSlash(entityIn, -45.0F, Vec3.ZERO, false, false, 0.2D))
                                        .put(9, (entityIn) -> AttackManager.doSlash(entityIn, -45.0F, Vec3.ZERO, false, false, 3.0D))
                                        .build())
                                .addHitEffect(StunManager::setStun)
                                .build()
                );

            case "thrustslash_ex":
                return deferredRegister.register(name, () ->
                        ComboState.Builder.newInstance().startAndEnd(400, 459).priority(50)
                                .motionLoc(DefaultResources.ExMotionLocation)
                                .next(ComboState.TimeoutNext.buildFromFrame(15, (entity) -> SlashBlade.prefix("none")))
                                .nextOfTimeout((entity) -> Pseudoedge_break_dawn.prefix("all_reuse"))
                                .addTickAction(ComboState.TimeLineTickAction.getBuilder()
                                        .put(1, (entityIn) -> CircleSlash.doCircleSlashAttack(entityIn, 180.0F))
                                        .put(2, (entityIn) -> CircleSlash.doCircleSlashAttack(entityIn, 90.0F))
                                        .put(3, (entityIn) -> CircleSlash.doCircleSlashAttack(entityIn, 0.0F))
                                        .put(4, (entityIn) -> CircleSlash.doCircleSlashAttack(entityIn, -90.0F))
                                        .put(5, (entityIn) -> Thrust.doSlash(entityIn, 9.0F))
                                        .put(6, (entityIn) -> CircleSlash.doCircleSlashAttack(entityIn, 180.0F))
                                        .put(7, (entityIn) -> CircleSlash.doCircleSlashAttack(entityIn, 90.0F))
                                        .put(8, (entityIn) -> CircleSlash.doCircleSlashAttack(entityIn, 0.0F))
                                        .put(9, (entityIn) -> CircleSlash.doCircleSlashAttack(entityIn, -90.0F))
                                        .build())
                                .addHitEffect(StunManager::setStun)
                                .build()
                );
        }

        return deferredRegister.register(name, supplier);
    }
}
