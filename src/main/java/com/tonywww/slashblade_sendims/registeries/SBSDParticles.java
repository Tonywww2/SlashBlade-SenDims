package com.tonywww.slashblade_sendims.registeries;

import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.particle.AcidBubbleParticle;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class SBSDParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, SenDims.MOD_ID);

    public static final RegistryObject<SimpleParticleType> ACID_BUBBLE =
            PARTICLES.register("acid_bubble", () -> new SimpleParticleType(false));

    public static void onParticleFactoryRegistration(RegisterParticleProvidersEvent event) {
        SenDims.LOGGER.info("Begin register particles.");
        event.registerSpriteSet(SBSDParticles.ACID_BUBBLE.get(), AcidBubbleParticle.Factory::new);
    }

    public static void register(IEventBus eventBus) {
        PARTICLES.register(eventBus);
        eventBus.addListener(SBSDParticles::onParticleFactoryRegistration);

    }

}
