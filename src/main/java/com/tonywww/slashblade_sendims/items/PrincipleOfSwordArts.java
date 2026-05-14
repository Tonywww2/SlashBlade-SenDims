package com.tonywww.slashblade_sendims.items;

import com.tonywww.slashblade_sendims.curios.PrincipleOfSwordArtsCapProvider;
import mods.flammpfeil.slashblade.registry.SlashArtsRegistry;
import mods.flammpfeil.slashblade.registry.SlashBladeItems;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PrincipleOfSwordArts extends AbstractCollectionItem {
    public static final String PATH = "principle_of_sword_arts";
    public static final String SLOT = "principle_of_sword_arts";

    public static final int TOTAL_SA = 70;

    public PrincipleOfSwordArts(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new PrincipleOfSwordArtsCapProvider(stack);
    }

    @Override
    protected String getCollectionPath() {
        return PATH;
    }

    @Override
    protected boolean shouldConsumeFound() {
        return true;
    }

    @Override
    protected @Nullable String getTargetKey(ItemStack targetStack) {
        if (!targetStack.is(SlashBladeItems.PROUDSOUL_SPHERE.get()) || !targetStack.hasTag() || !targetStack.getTag().contains("SpecialAttackType")) {
            return null;
        }

        String saKey = targetStack.getTag().getString("SpecialAttackType");
        if (saKey.isEmpty() || saKey.equals("slashblade:none")) {
            return null;
        }

        return saKey;
    }

    @Override
    protected long getTotalCollectionCount() {
        return TOTAL_SA;
    }

    @Override
    protected String getTranslationKeyBase() {
        return "ui.slashblade_sendims.principle_of_sword_arts";
    }

    @Override
    protected void addSpecificHoverDetails(List<Component> toolTips, ListTag itemList) {
        for (int i = 0; i < itemList.size(); i++) {
            String saKey = itemList.getString(i);
            ResourceLocation rl = ResourceLocation.tryParse(saKey);
            if (rl != null && SlashArtsRegistry.REGISTRY.get().containsKey(rl)) {
                SlashArts sa = SlashArtsRegistry.REGISTRY.get().getValue(rl);
                if (sa != null) {
                    toolTips.add(Component.literal("- ")
                            .append(Component.translatable("slashblade.tooltip.slash_art", sa.getDescription())).withStyle(ChatFormatting.GRAY));
                }
            }
        }
    }

    public static CompoundTag getPSATag(ItemStack stack) {
        return getCollectionTag(stack, PATH);
    }
}
