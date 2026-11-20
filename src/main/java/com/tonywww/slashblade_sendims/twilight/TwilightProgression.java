package com.tonywww.slashblade_sendims.twilight;

import com.tonywww.slashblade_sendims.config.SenDimsConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import twilightforest.init.custom.Enforcement;
import twilightforest.init.custom.Restrictions;
import twilightforest.util.PlayerHelper;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Twilight Forest progression changes for the reworked order (docs/development/暮色森林流程重做-调查与设计-2026-11-18.md
 * in the modpack): trophy pedestal requirement, boss spawner gates and the acid rain side effects.
 */
public final class TwilightProgression {
    private static List<? extends String> cachedGateConfig;
    private static Map<ResourceLocation, ResourceLocation> cachedGates = Map.of();

    private TwilightProgression() {
    }

    public static boolean isPedestalEligible(Player player) {
        ResourceLocation advancement = ResourceLocation.tryParse(SenDimsConfig.TF_PEDESTAL_ADVANCEMENT.get());
        return advancement == null || PlayerHelper.doesPlayerHaveRequiredAdvancements(player, advancement);
    }

    /** Advancement required by the boss spawner of this entity type, or null when the spawner is not gated. */
    @Nullable
    public static ResourceLocation spawnerGate(EntityType<?> type) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(type);
        return id == null ? null : gates().get(id);
    }

    private static synchronized Map<ResourceLocation, ResourceLocation> gates() {
        List<? extends String> config = SenDimsConfig.TF_BOSS_SPAWNER_GATES.get();
        if (config != cachedGateConfig) {
            Map<ResourceLocation, ResourceLocation> parsed = new HashMap<>();
            for (String entry : config) {
                int split = entry.indexOf('=');
                if (split <= 0) {
                    continue;
                }
                ResourceLocation entity = ResourceLocation.tryParse(entry.substring(0, split).trim());
                ResourceLocation advancement = ResourceLocation.tryParse(entry.substring(split + 1).trim());
                if (entity != null && advancement != null) {
                    parsed.put(entity, advancement);
                }
            }
            cachedGates = Map.copyOf(parsed);
            cachedGateConfig = config;
        }
        return cachedGates;
    }

    /** Same as the boss spawner's own range check, but only counting players who have the advancement. */
    public static boolean anyEligiblePlayerInRange(Level level, BlockPos pos, double range, ResourceLocation advancement) {
        double rangeSqr = range * range;
        for (Player player : level.players()) {
            if (!player.isAlive() || player.isSpectator()) {
                continue;
            }
            if (player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) > rangeSqr) {
                continue;
            }
            if (player.isCreative() || PlayerHelper.doesPlayerHaveRequiredAdvancements(player, advancement)) {
                return true;
            }
        }
        return false;
    }

    /** Called with the Twilight Forest biome penalty (every 20 ticks): acid rain also slows the player down. */
    public static void applyAcidRainExtras(Player player, ServerLevel level) {
        Restrictions.getRestrictionForBiome(level.getBiome(player.blockPosition()).value(), player).ifPresent(restriction -> {
            if (!restriction.enforcement().location().equals(Enforcement.ACID_RAIN.getId())) {
                return;
            }
            int ticks = SenDimsConfig.TF_ACID_RAIN_EFFECT_TICKS.get();
            addEffect(player, MobEffects.MOVEMENT_SLOWDOWN, SenDimsConfig.TF_ACID_RAIN_SLOWNESS.get(), ticks);
            addEffect(player, MobEffects.DIG_SLOWDOWN, SenDimsConfig.TF_ACID_RAIN_MINING_FATIGUE.get(), ticks);
        });
    }

    private static void addEffect(Player player, MobEffect effect, int amplifier, int ticks) {
        if (amplifier >= 0) {
            player.addEffect(new MobEffectInstance(effect, ticks, amplifier, false, true));
        }
    }

    public static boolean thornsBreakable() {
        return SenDimsConfig.TF_THORNS_BREAKABLE.get();
    }
}
