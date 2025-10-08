package com.tonywww.slashblade_sendims.registeries;

import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.items.BloodJade;
import com.tonywww.slashblade_sendims.items.DeepRealmCertificate;
import com.tonywww.slashblade_sendims.items.StructureQuill;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class SBSDItems {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, SenDims.MOD_ID);

    public static final RegistryObject<Item> DEEPREALM_CERTIFICATE = ITEMS.register("deeprealm_certificate",
            () -> new DeepRealmCertificate(new Item.Properties()
                    .stacksTo(1)
                    .fireResistant()
                    .rarity(Rarity.EPIC)
            ));

    public static final RegistryObject<Item> STRUCTURE_QUILL = ITEMS.register("structure_quill",
            () -> new StructureQuill(new Item.Properties()
                    .stacksTo(1)
                    .fireResistant()
                    .rarity(Rarity.UNCOMMON)
            ));
    public static final RegistryObject<Item> BLOOD_JADE = ITEMS.register("blood_jade",
            () -> new BloodJade(new Item.Properties()
                    .stacksTo(64)
                    .fireResistant()
                    .rarity(Rarity.RARE)
            ));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);

    }
}
