package com.tonywww.slashblade_sendims.client;

import com.tonywww.slashblade_sendims.SenDims;
import mods.flammpfeil.slashblade.client.renderer.model.BladeModelManager;
import mods.flammpfeil.slashblade.client.renderer.model.obj.WavefrontObject;
import mods.flammpfeil.slashblade.event.client.RenderOverrideEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.Set;

/**
 * Named-blade model overrides, moved from kubejs/client_scripts/zenith_models.js and damnation_models.js.
 * RenderOverrideEvent fires for every blade part of every rendered blade each frame, so the lookup stays in Java.
 * BladeModelManager keeps owning the OBJ cache and its reload invalidation.
 */
@Mod.EventBusSubscriber(modid = SenDims.MOD_ID, value = Dist.CLIENT)
public final class BladeModelOverrides {
    private BladeModelOverrides() {}

    /** requireOwnTexture: only replace when the blade is still drawn with one of its own textures (damnation line). */
    private record Visual(ResourceLocation model, ResourceLocation texture, boolean requireOwnTexture) {}

    private static final Set<String> TARGETS = Set.of(
            "blade", "sheath", "blade_damaged", "item_blade", "item_bladens", "item_damaged");

    // Zenith: Magic keeps its SJAP model; the four S5 blades share the Zenith geometry.
    private static final ResourceLocation ZENITH_MODEL = new ResourceLocation("sdbf", "model/zenith/zenith.obj");
    // Greatsword of Damnation line.
    private static final ResourceLocation DAMNATION_ORIGINAL_MODEL = new ResourceLocation("slashblade", "model/named/dios/dios.obj");
    private static final ResourceLocation DAMNATION_GREATSWORD_MODEL = new ResourceLocation("sdbf", "model/damnation/greatsword.obj");
    private static final ResourceLocation DAMNATION_FINAL_MODEL = new ResourceLocation("sdbf", "model/damnation/damnation.obj");
    private static final ResourceLocation DAMNATION_GOLD = new ResourceLocation("sdbf", "model/damnation/damnation_gold.png");

    private static final Map<String, Visual> VISUALS = Map.of(
            "item.sjap_adder.dragon_steel_ice", new Visual(
                    new ResourceLocation("sjap_adder", "model/icf/icfex/model.obj"),
                    new ResourceLocation("sjap_adder", "model/icf/icfex/ice.png"), false),
            "item.slashblade_sendims.zenith_false_black_crown", new Visual(ZENITH_MODEL,
                    new ResourceLocation("sdbf", "model/zenith/zenith_black_crown.png"), false),
            "item.slashblade_sendims.zenith_false_divine_might", new Visual(ZENITH_MODEL,
                    new ResourceLocation("sdbf", "model/zenith/zenith_divine_might.png"), false),
            "item.slashblade_sendims.zenith_false_kingship", new Visual(ZENITH_MODEL,
                    new ResourceLocation("sdbf", "model/zenith/zenith_kingship.png"), false),
            "item.slashblade_sendims.zenith_false_dark_curse", new Visual(ZENITH_MODEL,
                    new ResourceLocation("sdbf", "model/zenith/zenith_dark_curse.png"), false),
            "item.slashblade.greatsword_of_damnation", new Visual(DAMNATION_ORIGINAL_MODEL,
                    new ResourceLocation("slashblade", "model/greatsword_of_damnation/greatsword_of_damnation.png"), true),
            "item.slashblade.greatsword_of_damnation_plus", new Visual(DAMNATION_ORIGINAL_MODEL,
                    new ResourceLocation("slashblade", "model/greatsword_of_damnation/greatsword_of_damnation_plus.png"), true),
            "item.slashblade.greatsword_of_damnation_full", new Visual(DAMNATION_GREATSWORD_MODEL, DAMNATION_GOLD, true),
            "item.slashblade_sendims.greatsword_of_damnation_final", new Visual(DAMNATION_FINAL_MODEL, DAMNATION_GOLD, true));

    private static final Set<ResourceLocation> DAMNATION_OWN_TEXTURES = Set.of(
            DAMNATION_GOLD,
            new ResourceLocation("slashblade", "model/greatsword_of_damnation/greatsword_of_damnation.png"),
            new ResourceLocation("slashblade", "model/greatsword_of_damnation/greatsword_of_damnation_plus.png"),
            new ResourceLocation("slashblade", "model/greatsword_of_damnation/greatsword_of_damnation_full.png"));

    @SubscribeEvent
    public static void onRenderOverride(RenderOverrideEvent event) {
        String target = event.getTarget();
        if (target == null || !TARGETS.contains(target)) return;
        ItemStack stack = event.getStack();
        if (stack == null || stack.isEmpty()) return;
        Visual visual = VISUALS.get(stack.getDescriptionId());
        if (visual == null) return;
        if (visual.requireOwnTexture()) {
            ResourceLocation texture = event.getTexture();
            if (texture == null || !DAMNATION_OWN_TEXTURES.contains(texture)) return;
        }
        BladeModelManager manager = BladeModelManager.getInstance();
        WavefrontObject model = manager.getModel(visual.model());
        if (model == manager.defaultModel) return;
        event.setModel(model);
        event.setTexture(visual.texture());
    }
}
