package com.tonywww.slashblade_sendims.registeries;

import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.entities.EntityChaoticBlisteringSwords;
import com.tonywww.slashblade_sendims.entities.EntityChaoticJudgementCut;
import com.tonywww.slashblade_sendims.entities.EntityChaoticSlashEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class SBSDEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, SenDims.MOD_ID);

    public static final RegistryObject<EntityType<EntityChaoticJudgementCut>> CHAOTIC_JUDGEMENT_CUT =
            ENTITY_TYPES.register("chaotic_judgement_cut", () -> EntityType.Builder
                    .<EntityChaoticJudgementCut>of(EntityChaoticJudgementCut::new, MobCategory.MISC)
                    .sized(2.5F, 2.5F)
                    .clientTrackingRange(4)
                    .updateInterval(20)
                    .build(SenDims.prefix("chaotic_judgement_cut").toString()));

    public static final RegistryObject<EntityType<EntityChaoticSlashEffect>> CHAOTIC_SLASH_EFFECT =
            ENTITY_TYPES.register("chaotic_slash_effect", () -> EntityType.Builder
                    .<EntityChaoticSlashEffect>of(EntityChaoticSlashEffect::new, MobCategory.MISC)
                    .sized(3.0F, 3.0F)
                    .clientTrackingRange(4)
                    .updateInterval(20)
                    .build(SenDims.prefix("chaotic_slash_effect").toString()));

    public static final RegistryObject<EntityType<EntityChaoticBlisteringSwords>> CHAOTIC_BLISTERING_SWORDS =
            ENTITY_TYPES.register("chaotic_blistering_swords", () -> EntityType.Builder
                    .<EntityChaoticBlisteringSwords>of(EntityChaoticBlisteringSwords::new, MobCategory.MISC)
                    .sized(0.9F, 0.9F)
                    .clientTrackingRange(4)
                    .updateInterval(20)
                    .build(SenDims.prefix("chaotic_blistering_swords").toString()));

    private SBSDEntities() {
    }

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}