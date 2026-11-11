package com.tonywww.slashblade_sendims.mixin.draconicevolution;

import com.brandon3055.draconicevolution.api.modules.entities.ShieldControlEntity;
import com.brandon3055.draconicevolution.handlers.ModularArmorEventHandler;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** The shield leaves {@code LivingDamageEvent} (LOW); undying modules there are unchanged. */
@Mixin(value = ModularArmorEventHandler.class, remap = false)
public abstract class ModularArmorEventHandlerMixin {
    @Redirect(method = "onEntityDamaged",
            at = @At(value = "INVOKE", target = "Lcom/brandon3055/draconicevolution/api/modules/entities/ShieldControlEntity;tryBlockDamage(Lnet/minecraftforge/event/entity/living/LivingDamageEvent;)V"))
    private static void sendims$deferShield(ShieldControlEntity shield, LivingDamageEvent event) {
    }
}
