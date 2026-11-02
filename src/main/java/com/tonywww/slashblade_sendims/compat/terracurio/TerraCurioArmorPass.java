package com.tonywww.slashblade_sendims.compat.terracurio;

import dev.shadowsoffire.attributeslib.api.ALObjects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

/**
 * TerraCurio maps its armor pass to {@code attributeslib:armor_pierce} when Apothic Attributes is loaded,
 * then subtracts that value from armor in its own wrapper around the vanilla armor formula.
 * Apothic Attributes applies the same attribute again (with toughness mitigation), so the pierce counted twice.
 *
 * <p>While {@code LivingEntity#getDamageAfterArmorAbsorb} runs, TerraCurio's lookup is pointed at its own
 * {@code terra_curio:armor_pass} attribute instead. Entities do not carry that attribute in this setup, so
 * TerraCurio subtracts nothing and only Apothic's pierce remains. Item modifiers built outside this window keep
 * using {@code attributeslib:armor_pierce}.</p>
 */
public final class TerraCurioArmorPass {
    private static final ResourceLocation OWN_ARMOR_PASS = new ResourceLocation("terra_curio", "armor_pass");
    private static final ThreadLocal<int[]> ARMOR_ABSORB_DEPTH = ThreadLocal.withInitial(() -> new int[1]);
    private static Attribute ownArmorPass;

    private TerraCurioArmorPass() {
    }

    public static void enterArmorAbsorb() {
        ARMOR_ABSORB_DEPTH.get()[0]++;
    }

    public static void exitArmorAbsorb() {
        int[] depth = ARMOR_ABSORB_DEPTH.get();
        if (depth[0] > 0) depth[0]--;
    }

    /**
     * @param mapped the attribute TerraCurio's {@code ModAttributes#getArmorPass} resolved
     * @return the replacement while armor absorption runs, or null to keep {@code mapped}
     */
    @Nullable
    public static Attribute replaceDuringArmorAbsorb(@Nullable Attribute mapped) {
        if (mapped == null || ARMOR_ABSORB_DEPTH.get()[0] == 0) return null;
        // Only the Apothic mapping is applied twice; any other mapping is TerraCurio's sole armor pass.
        if (mapped != ALObjects.Attributes.ARMOR_PIERCE.get()) return null;
        if (ownArmorPass == null) ownArmorPass = ForgeRegistries.ATTRIBUTES.getValue(OWN_ARMOR_PASS);
        return ownArmorPass == null || ownArmorPass == mapped ? null : ownArmorPass;
    }
}
