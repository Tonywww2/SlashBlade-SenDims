package com.tonywww.slashblade_sendims.compat.jei;

import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.items.StructureQuill;
import com.tonywww.slashblade_sendims.registeries.SBSDItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.ISubtypeRegistration;
import mods.flammpfeil.slashblade.registry.SlashBladeItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

@JeiPlugin
public class JEICompat implements IModPlugin {

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return SenDims.prefix(SenDims.MOD_ID);
    }

    @Override
    public void registerItemSubtypes(@NotNull ISubtypeRegistration registration) {
        IModPlugin.super.registerItemSubtypes(registration);
        registration.registerSubtypeInterpreter(SBSDItems.STRUCTURE_QUILL.get(),
                ((stack, uidContext) -> {
                    if (!stack.hasTag()) return "";
                    CompoundTag tag = stack.getTag();
                    if (!tag.contains(StructureQuill.TAG_STRUCTURE)) return "";
                    return tag.getString(StructureQuill.TAG_STRUCTURE);
                }));

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
                var list = tag.getList("Enchantments", Tag.TAG_COMPOUND);
                if (!list.isEmpty()) {
                    return ((CompoundTag) list.get(0)).getString("id");
                }
            }
            return "";
        });
    }

}
