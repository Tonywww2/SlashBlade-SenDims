package com.tonywww.slashblade_sendims.compat.jei;

import com.tonywww.slashblade_sendims.registeries.SBSDItems;
import com.tonywww.slashblade_sendims.registeries.SBSDTags;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import mods.flammpfeil.slashblade.registry.SlashBladeItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.util.List;

public record StackData(String lang, Ingredient putIngredient, ItemStack targetItem) {
    public static List<StackData> AllData = new ObjectArrayList<>();

    static {
        if (FMLEnvironment.production)
            register("debug_jei_info", Ingredient.of(Items.ANDESITE.getDefaultInstance()), Items.ALLIUM.getDefaultInstance());

        register("jei.stacking.drc_ranking", Ingredient.of(SBSDTags.Items.DRC_RANK_MATERIALS), SBSDItems.DEEPREALM_CERTIFICATE.get().getDefaultInstance());
        register("jei.stacking.drc_health", Ingredient.of(SBSDTags.Items.DRC_HEALTH_MATERIALS), SBSDItems.DEEPREALM_CERTIFICATE.get().getDefaultInstance());
        register("jei.stacking.drc_damage", Ingredient.of(SBSDTags.Items.DRC_DAMAGE_MATERIALS), SBSDItems.DEEPREALM_CERTIFICATE.get().getDefaultInstance());

        register("jei.stacking.blessing_petals", Ingredient.of(SBSDTags.Items.BLESSING_PETALS_ITEMS), SBSDItems.BLESSING_PETALS.get().getDefaultInstance());
        register("jei.stacking.principle_of_sword_arts", Ingredient.of(SlashBladeItems.PROUDSOUL_SPHERE.get()), SBSDItems.PRINCIPLE_OF_SWORD_ARTS.get().getDefaultInstance());
        register("jei.stacking.the_nectar_quest", Ingredient.of(SBSDTags.Items.THE_NECTAR_QUEST_ITEMS), SBSDItems.THE_NECTAR_QUEST.get().getDefaultInstance());

        register("jei.stacking.blood_jade", Ingredient.of(SBSDItems.BLOOD_JADE.get()), SlashBladeItems.SLASHBLADE.get().getDefaultInstance());
    }

    public static void clear() {
        AllData.clear();
    }

    public static void register(StackData data) {
        AllData.add(data);
    }

    public static void register(String lang, Ingredient putItem, ItemStack targetItem) {
        register(new StackData(lang, putItem, targetItem));
    }
}
