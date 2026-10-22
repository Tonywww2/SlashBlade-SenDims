package com.tonywww.slashblade_sendims.registeries;

import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.entities.EntityChaoticBlisteringSwords;
import com.tonywww.slashblade_sendims.entities.EntityChaoticJudgementCut;
import com.tonywww.slashblade_sendims.entities.EntityChaoticSlashEffect;
import com.tonywww.slashblade_sendims.entities.EntityStableDrive;
import com.tonywww.slashblade_sendims.entities.EntityHolySwordLight;
import com.tonywww.slashblade_sendims.entities.EntityYakumanDrive;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class SBSDEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, SenDims.MOD_ID);

    public static final RegistryObject<EntityType<EntityHolySwordLight>> HOLY_SWORD_LIGHT =
            ENTITY_TYPES.register("holy_sword_light", () -> EntityType.Builder
                    .<EntityHolySwordLight>of(EntityHolySwordLight::new, MobCategory.MISC)
                    .sized(0.1F, 0.1F).clientTrackingRange(8).updateInterval(20).noSave()
                    .build(SenDims.prefix("holy_sword_light").toString()));

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

    public static final RegistryObject<EntityType<EntityStableDrive>> STABLE_DRIVE =
            ENTITY_TYPES.register("stable_drive", () -> EntityType.Builder
                    .<EntityStableDrive>of(EntityStableDrive::new, MobCategory.MISC)
                    .sized(3.0F, 3.0F)
                    .clientTrackingRange(4)
                    .updateInterval(20)
                    .build(SenDims.prefix("stable_drive").toString()));

    public static final RegistryObject<EntityType<EntityYakumanDrive>> YAKUMAN_DRIVE =
            ENTITY_TYPES.register("yakuman_drive", () -> EntityType.Builder
                    .<EntityYakumanDrive>of(EntityYakumanDrive::new, MobCategory.MISC)
                    .sized(0.2F, 0.2F).clientTrackingRange(6).updateInterval(3).noSave()
                    .build(SenDims.prefix("yakuman_drive").toString()));

    private SBSDEntities() {
    }

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
