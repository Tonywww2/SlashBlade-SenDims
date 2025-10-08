package com.tonywww.slashblade_sendims.kubejs.events;

import com.tonywww.slashblade_sendims.jade.NewTierToolHandler;
import dev.latvian.mods.kubejs.event.StartupEventJS;
import dev.latvian.mods.kubejs.typings.Info;
import dev.latvian.mods.kubejs.typings.Param;
import dev.latvian.mods.rhino.util.HideFromJS;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.ForgeTier;
import net.minecraftforge.common.TierSortingRegistry;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import snownee.jade.Jade;

import java.util.List;

public class TierRegisterEventJS extends StartupEventJS {
    @HideFromJS
    public void registerTier(int level, int uses, float speed, float attackDamageBonus, int enchantmentValue, ResourceLocation tag, Ingredient repairIngredient, List<ResourceLocation> after, ResourceLocation name) {
        var tier = new ForgeTier(level, uses, speed, attackDamageBonus, enchantmentValue, BlockTags.create(tag), () -> repairIngredient);
        TierSortingRegistry.registerTier(
                tier,
                name,
                (List) after,
                List.of()
        );
        if (ModList.get().isLoaded(Jade.MODID)&&repairIngredient.getItems().length>0) {
            NewTierToolHandler.registerTierHandler(tier, name.toString());
        }
    }

    @Info("""
            第一个参数为挖掘等级
            第二个是ResourceLocation的方块tag
            第三个是修理的原材料，也是jade显示的
            第四个是after
            第五个是名字
            """)
    public void registerTier(int level, ResourceLocation tag, Ingredient repairIngredient, List<ResourceLocation> after, ResourceLocation name) {
        registerTier(level, 520, level * 4 - 7, 0, 30, tag, repairIngredient, after, name);
    }

    public static void tierRegisterHandler(FMLCommonSetupEvent event) {
        SBSDEvents.TierRegister.post(new TierRegisterEventJS());
    }
}
