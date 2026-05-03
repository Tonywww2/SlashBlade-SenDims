package com.tonywww.slashblade_sendims.mixin.legendreliclib;

import com.dinzeer.legendreliclib.lib.util.ArmorPiercingUtil;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = ArmorPiercingUtil.class)
public class ArmorPiercingUtilMixin {

    /**
     * @author SlashBlade-SenDims
     * @reason Disable armor piercing effect - do nothing
     */
    @Overwrite(remap = false)
    public static void addArmorPiercingEffect(LivingEntity target, LivingEntity source, int duration, float piercingPercent) {
        // Do nothing
    }

    /**
     * @author SlashBlade-SenDims
     * @reason Disable armor piercing effect - do nothing
     */
    @Overwrite(remap = false)
    public static void removeArmorPiercingEffect(LivingEntity target, LivingEntity source) {
        // Do nothing
    }

    /**
     * @author SlashBlade-SenDims
     * @reason Disable armor piercing effect - always return false
     */
    @Overwrite(remap = false)
    public static boolean hasArmorPiercingEffect(LivingEntity target, LivingEntity source) {
        return false;
    }

    /**
     * @author SlashBlade-SenDims
     * @reason Disable armor piercing effect - always return 0.0f
     */
    @Overwrite(remap = false)
    public static float getArmorPiercingPercent(LivingEntity target, LivingEntity source) {
        return 0.0F;
    }

    /**
     * @author SlashBlade-SenDims
     * @reason Disable armor piercing effect - do nothing on hurt
     */
    @Overwrite(remap = false)
    public static void onLivingHurt(LivingHurtEvent event) {
        // Do nothing
    }

    /**
     * @author SlashBlade-SenDims
     * @reason Disable armor piercing effect - return original damage
     */
    @Overwrite(remap = false)
    private static float applyArmorPiercing(float damage, float armor, float toughness, float piercingPercent) {
        return damage;
    }
}

