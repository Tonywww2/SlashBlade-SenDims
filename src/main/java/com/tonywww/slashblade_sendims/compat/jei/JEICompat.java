package com.tonywww.slashblade_sendims.compat.jei;

import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.items.StructureQuill;
import com.tonywww.slashblade_sendims.registeries.SBSDItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.nbt.CompoundTag;
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
    }
}
