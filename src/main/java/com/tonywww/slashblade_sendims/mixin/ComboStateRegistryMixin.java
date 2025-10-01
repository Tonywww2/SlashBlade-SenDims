package com.tonywww.slashblade_sendims.mixin;

import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.ability.StunManager;
import mods.flammpfeil.slashblade.event.client.UserPoseOverrider;
import mods.flammpfeil.slashblade.init.DefaultResources;
import mods.flammpfeil.slashblade.registry.ComboStateRegistry;
import mods.flammpfeil.slashblade.registry.combo.ComboState;
import mods.flammpfeil.slashblade.util.AdvancementHelper;
import mods.flammpfeil.slashblade.util.AttackManager;
import net.minecraftforge.registries.RegistryObject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Supplier;

@Mixin(value = ComboStateRegistry.class, remap = false)
public class ComboStateRegistryMixin {

    @Redirect(
            method = "<clinit>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraftforge/registries/DeferredRegister;register(Ljava/lang/String;Ljava/util/function/Supplier;)Lnet/minecraftforge/registries/RegistryObject;"),
            remap = false
    )
    private static RegistryObject<ComboState> redirectComboRegister(
            net.minecraftforge.registries.DeferredRegister<ComboState> deferredRegister,
            String name,
            Supplier<ComboState> supplier) {
        if ("combo_a4_ex".equals(name)) {
            return deferredRegister.register(name, () ->
                    ComboState.Builder.newInstance().startAndEnd(800, 839).priority(100)
                            .motionLoc(DefaultResources.ExMotionLocation)
                            .next(ComboState.TimeoutNext.buildFromFrame(22, entity -> SlashBlade.prefix("combo_a5ex")))
                            .nextOfTimeout(entity -> SlashBlade.prefix("combo_a4_ex_end"))
                            .addTickAction(ComboState.TimeLineTickAction.getBuilder()
                                    .put(7, (entityIn) -> AttackManager.doSlash(entityIn, 70, false, false, 0.75f))
                                    .put(14, (entityIn) -> AttackManager.doSlash(entityIn, 180 + 75, false, false, 0.75f)).build())

                            .addHitEffect(StunManager::setStun)
                            .build()
            );
        } else if ("combo_a5ex".equals(name)) {
            return deferredRegister.register(name, () ->
                    ComboState.Builder.newInstance().startAndEnd(900, 1013).priority(100)
                            .motionLoc(DefaultResources.ExMotionLocation)
                            .next(ComboState.TimeoutNext.buildFromFrame(33, entity -> SlashBlade.prefix("none")))
                            .nextOfTimeout(entity -> SlashBlade.prefix("combo_a5ex_end"))
                            .addTickAction(ComboState.TimeLineTickAction.getBuilder()
                                    .put(15, (entityIn) -> AttackManager.doSlash(entityIn, 35, false, true, 0.75f))
                                    .put(17, (entityIn) -> AttackManager.doSlash(entityIn, 40, true, true, 0.75f))
                                    .put(19, (entityIn) -> AttackManager.doSlash(entityIn, 30, true, true, 0.75f)).build())
                            .addTickAction(ComboState.TimeLineTickAction.getBuilder()
                                    .put(13 + 0, (entityIn) -> UserPoseOverrider.setRot(entityIn, 72, true))
                                    .put(13 + 1, (entityIn) -> UserPoseOverrider.setRot(entityIn, 72, true))
                                    .put(13 + 2, (entityIn) -> UserPoseOverrider.setRot(entityIn, 72, true))
                                    .put(13 + 3, (entityIn) -> UserPoseOverrider.setRot(entityIn, 72, true))
                                    .put(13 + 4, (entityIn) -> UserPoseOverrider.setRot(entityIn, 72, true))
                                    .put(13 + 5, (entityIn) -> UserPoseOverrider.resetRot(entityIn)).build())
                            .clickAction(a -> AdvancementHelper.grantCriterion(a, AdvancementHelper.ADVANCEMENT_COMBO_A_EX))
                            .addHitEffect(StunManager::setStun)
                            .build()
            );
        }
        return deferredRegister.register(name, supplier);
    }
}