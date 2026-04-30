package com.tonywww.slashblade_sendims.mixin.redtassel;

import com.alpha.redtassel.Redtassel;
import com.alpha.redtassel.regsiter.RTComboAndSA;
import com.alpha.redtassel.sa.GoGoGo;
import com.alpha.redtassel.sa.TheString;
import com.dinzeer.legendreliclib.lib.util.WaitingTick;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.ability.StunManager;
import mods.flammpfeil.slashblade.init.DefaultResources;
import mods.flammpfeil.slashblade.registry.combo.ComboState;
import mods.flammpfeil.slashblade.util.AttackManager;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Supplier;

@Mixin(value = RTComboAndSA.class, remap = false)
public class RTComboAndSAMixin {

    @Redirect(
            method = "<clinit>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraftforge/registries/DeferredRegister;register(Ljava/lang/String;Ljava/util/function/Supplier;)Lnet/minecraftforge/registries/RegistryObject;"
            ),
            remap = false
    )
    private static RegistryObject<ComboState> redirectComboRegister(
            DeferredRegister<ComboState> deferredRegister,
            String name,
            Supplier<ComboState> supplier) {

        if (name.equals("xdrive")) {
            return deferredRegister.register(name, () ->
                    ComboState.Builder.newInstance().startAndEnd(400, 459).priority(50)
                            .motionLoc(DefaultResources.ExMotionLocation)
                            .next(ComboState.TimeoutNext.buildFromFrame(15, (entity) -> SlashBlade.prefix("none")))
                            .nextOfTimeout((entity) -> Redtassel.prefix("all_reuse"))
                            .addTickAction(ComboState.TimeLineTickAction.getBuilder()
                                    .put(2, (entityIn) -> AttackManager.doSlash(entityIn, -45.0F, Vec3.ZERO, false, false, 1.0D))
                                    .put(3, GoGoGo::dois)
                                    .put(4, (entityIn) -> AttackManager.doSlash(entityIn, 45.0F, Vec3.ZERO, false, false, 1.0D))
                                    .put(5, (entityIn) -> {
                                        for (int i = 0; i < 12; ++i) {
                                            WaitingTick.schedule(i, () -> {
                                                AttackManager.doSlash(entityIn, (float) entityIn.getRandom().nextInt(360), Vec3.ZERO, false, false, 0.6D);
                                                TheString.DoSlash(entityIn);
                                            });
                                        }
                                    })
                                    .put(6, GoGoGo::dois2)
                                    .put(15, (entityIn) -> {
                                        for (int i = 0; i < 12; ++i) {
                                            WaitingTick.schedule(i, () -> {
                                                AttackManager.doSlash(entityIn, (float) entityIn.getRandom().nextInt(360), Vec3.ZERO, false, false, 0.6D);
                                                TheString.DoSlash(entityIn);
                                            });
                                        }
                                    }).build())
                            .addHitEffect(StunManager::setStun)
                            .build()
            );
        }

        return deferredRegister.register(name, supplier);
    }
}
