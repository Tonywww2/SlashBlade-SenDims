package com.tonywww.slashblade_sendims.registeries;

import com.tonywww.slashblade_sendims.SenDims;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class SBSDTags {

    public static class Blocks {

    }

    public static class Items {

        public static final TagKey<Item> DRC_RANK_MATERIALS = createTag("drc_rank_materials");
        public static final TagKey<Item> DRC_RANK_MATERIAL_1 = createTag("drc_rank_material_1");
        public static final TagKey<Item> DRC_RANK_MATERIAL_2 = createTag("drc_rank_material_2");
        public static final TagKey<Item> DRC_RANK_MATERIAL_3 = createTag("drc_rank_material_3");
        public static final TagKey<Item> DRC_RANK_MATERIAL_4 = createTag("drc_rank_material_4");
        public static final TagKey<Item> DRC_RANK_MATERIAL_5 = createTag("drc_rank_material_5");

        public static final TagKey<Item> DRC_HEALTH_MATERIALS = createTag("drc_health_materials");
        public static final TagKey<Item> DRC_HEALTH_MATERIAL_0 = createTag("drc_health_material_0");
        public static final TagKey<Item> DRC_HEALTH_MATERIAL_1 = createTag("drc_health_material_1");
        public static final TagKey<Item> DRC_HEALTH_MATERIAL_2 = createTag("drc_health_material_2");
        public static final TagKey<Item> DRC_HEALTH_MATERIAL_3 = createTag("drc_health_material_3");
        public static final TagKey<Item> DRC_HEALTH_MATERIAL_4 = createTag("drc_health_material_4");
        public static final TagKey<Item> DRC_HEALTH_MATERIAL_5 = createTag("drc_health_material_5");

        public static final TagKey<Item> DRC_DAMAGE_MATERIALS = createTag("drc_damage_materials");
        public static final TagKey<Item> DRC_DAMAGE_MATERIAL_0 = createTag("drc_damage_material_0");
        public static final TagKey<Item> DRC_DAMAGE_MATERIAL_1 = createTag("drc_damage_material_1");
        public static final TagKey<Item> DRC_DAMAGE_MATERIAL_2 = createTag("drc_damage_material_2");
        public static final TagKey<Item> DRC_DAMAGE_MATERIAL_3 = createTag("drc_damage_material_3");
        public static final TagKey<Item> DRC_DAMAGE_MATERIAL_4 = createTag("drc_damage_material_4");
        public static final TagKey<Item> DRC_DAMAGE_MATERIAL_5 = createTag("drc_damage_material_5");

        private static TagKey<Item> createTag(String name) {
            return ItemTags.create(SenDims.prefix(name));
        }

        private static TagKey<Item> createForgeTag(String name) {
            return ItemTags.create(ResourceLocation.fromNamespaceAndPath("forge", name));
        }

    }

}
