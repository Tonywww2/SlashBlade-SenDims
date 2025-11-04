package com.tonywww.slashblade_sendims.registeries;

import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.items.BloodJade;
import com.tonywww.slashblade_sendims.items.DeepRealmCertificate;
import com.tonywww.slashblade_sendims.items.EstusFlask;
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

    public static final RegistryObject<Item> ESTUS_FLASK_0 = ITEMS.register("estus_flask_0",
            () -> new EstusFlask(new Item.Properties()
                    .stacksTo(1)
                    .fireResistant()
                    .rarity(Rarity.COMMON),
                    10, 0f, 320
            ));
    public static final RegistryObject<Item> ESTUS_FLASK_1 = ITEMS.register("estus_flask_1",
            () -> new EstusFlask(new Item.Properties()
                    .stacksTo(1)
                    .fireResistant()
                    .rarity(Rarity.UNCOMMON),
                    20, 0.1f, 300
            ));
    public static final RegistryObject<Item> ESTUS_FLASK_2= ITEMS.register("estus_flask_2",
            () -> new EstusFlask(new Item.Properties()
                    .stacksTo(1)
                    .fireResistant()
                    .rarity(Rarity.UNCOMMON),
                    40, 0.2f, 280
            ));
    public static final RegistryObject<Item> ESTUS_FLASK_3 = ITEMS.register("estus_flask_3",
            () -> new EstusFlask(new Item.Properties()
                    .stacksTo(1)
                    .fireResistant()
                    .rarity(Rarity.RARE),
                    80, 0.3f, 260
            ));
    public static final RegistryObject<Item> ESTUS_FLASK_4 = ITEMS.register("estus_flask_4",
            () -> new EstusFlask(new Item.Properties()
                    .stacksTo(1)
                    .fireResistant()
                    .rarity(Rarity.RARE),
                    160, 0.4f, 240
            ));
    public static final RegistryObject<Item> ESTUS_FLASK_5 = ITEMS.register("estus_flask_5",
            () -> new EstusFlask(new Item.Properties()
                    .stacksTo(1)
                    .fireResistant()
                    .rarity(Rarity.EPIC),
                    320, 0.5f, 220
            ));
    public static final RegistryObject<Item> ESTUS_FLASK_6 = ITEMS.register("estus_flask_6",
            () -> new EstusFlask(new Item.Properties()
                    .stacksTo(1)
                    .fireResistant()
                    .rarity(Rarity.EPIC),
                    640, 0.55f, 200
            ));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);

    }
}
