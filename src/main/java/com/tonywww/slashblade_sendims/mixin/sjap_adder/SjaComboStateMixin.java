package com.tonywww.slashblade_sendims.mixin.sjap_adder;

import com.dinzeer.sjapadder.Config;
import com.dinzeer.sjapadder.Sjap_adder;
import com.dinzeer.sjapadder.register.SjaComboRegistry;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.ability.StunManager;
import mods.flammpfeil.slashblade.event.client.UserPoseOverrider;
import mods.flammpfeil.slashblade.registry.combo.ComboState;
import mods.flammpfeil.slashblade.slasharts.Drive;
import mods.flammpfeil.slashblade.util.AttackManager;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.DeferredRegister;
import com.dinzeer.legendreliclib.lib.compat.slashblade.Drives;
import com.dinzeer.sjapadder.sa.DriveSum;
import mods.flammpfeil.slashblade.init.DefaultResources;
import net.minecraftforge.registries.RegistryObject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Supplier;

@Mixin(value = SjaComboRegistry.class, remap = false)
public class SjaComboStateMixin {

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
            case "cold_drive":
                return deferredRegister.register(name, () ->
                        ComboState.Builder.newInstance().startAndEnd(400, 459).priority(50)
                                .motionLoc(DefaultResources.ExMotionLocation)
                                .next(ComboState.TimeoutNext.buildFromFrame(15, (entity) -> SlashBlade.prefix("none")))
                                .nextOfTimeout((entity) -> Sjap_adder.prefix("all_reuse"))
                                .addTickAction(ComboState.TimeLineTickAction.getBuilder()
                                        .put(2, (entityIn) -> AttackManager.doSlash(entityIn, -30.0F, Vec3.ZERO, false, false, 0.4))
                                        .put(3, (entityIn) -> Drives.spawnForwardDrive(entityIn, 0.6F, 1, 90.0F, 40.0F))
                                        .put(4, DriveSum::doSlashForQueen)
                                        .build())
                                .addHitEffect(StunManager::setStun)
                                .build()
                );

            case "sakura_drive_left":
                return deferredRegister.register(name, () ->
                        ComboState.Builder.newInstance().startAndEnd(1816, 1859).speed(6.0F).priority(50)
                                .next((entity) -> Sjap_adder.prefix("sakura_drive_right"))
                                .nextOfTimeout((entity) -> Sjap_adder.prefix("sakura_drive_right"))
                                .clickAction((entityIn) -> AttackManager.doSlash(entityIn, 22.5F, Vec3.ZERO, false, false, Config.sakuraDriveLeftComboRatio))
                                .addTickAction(UserPoseOverrider::resetRot)
                                .addTickAction(ComboState.TimeLineTickAction.getBuilder()
                                        .put(1, (entityIn) -> Drive.doSlash(entityIn, 22.5F, 20, Vec3.ZERO, true, 2.5D, 0.2F)).build())
                                .addHitEffect(StunManager::setStun)
                                .build()
                );

            case "sakura_drive_right":
                return deferredRegister.register(name, () ->
                        ComboState.Builder.newInstance().startAndEnd(204, 218).speed(1.1F).priority(50)
                                .next((entity) -> SlashBlade.prefix("none"))
                                .nextOfTimeout((entity) -> Sjap_adder.prefix("sakura_wave_edge_finish"))
                                .clickAction((entityIn) -> AttackManager.doSlash(entityIn, 157.5F, Vec3.ZERO, false, true, Config.sakuraDriveRightComboRatio))
                                .addTickAction(UserPoseOverrider::resetRot)
                                .addTickAction(ComboState.TimeLineTickAction.getBuilder()
                                        .put(1, (entityIn) -> Drive.doSlash(entityIn, -22.5F, 20, Vec3.ZERO, true, 1.5D, 0.2F)).build())
                                .addHitEffect((t, a) -> StunManager.setStun(t, 36L))
                                .build()
                );

        }

        return deferredRegister.register(name, supplier);
    }
}
