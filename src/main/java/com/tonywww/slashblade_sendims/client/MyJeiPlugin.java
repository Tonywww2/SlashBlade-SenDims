package com.tonywww.slashblade_sendims.client;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.ISubtypeRegistration;
import mods.flammpfeil.slashblade.registry.SlashBladeItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
public class MyJeiPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.parse("yiran:common");
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        registration.registerSubtypeInterpreter(SlashBladeItems.PROUDSOUL_SPHERE.get(), (stack, uidContext) -> {
            var tag = stack.getTag();
            if (tag != null && tag.contains("SpecialAttackType")) {
                return tag.getString("SpecialAttackType");
            }
            return "";
        });
        registration.registerSubtypeInterpreter(SlashBladeItems.PROUDSOUL_TINY.get(), (stack, uidContext) -> {
            var tag = stack.getTag();
            if (tag != null && tag.contains("Enchantments")) {
                var list =tag.getList("Enchantments", Tag.TAG_COMPOUND);
                if(!list.isEmpty()) {
                    return ((CompoundTag)list.get(0)).getString("id");
                }
            }
            return "";
        });

    }

}
