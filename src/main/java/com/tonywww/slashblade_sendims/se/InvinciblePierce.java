package com.tonywww.slashblade_sendims.se;

import com.tonywww.slashblade_sendims.SBSDValues;
import com.tonywww.slashblade_sendims.registeries.SBSDSpecialEffects;
import com.tonywww.slashblade_sendims.utils.TetraUtils;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.capability.slashblade.SlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import mods.flammpfeil.slashblade.util.AttackManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import vazkii.botania.api.mana.ManaItemHandler;
import vazkii.botania.common.handler.BotaniaSounds;
import vazkii.botania.common.item.BotaniaItems;

public class InvinciblePierce extends SpecialEffect {

    public static final String HIT_COUNT_PATH = "sbsd.sb.invincible_pierce_hits";
    public static final int MANA_COST_RATIO = 20;

    public InvinciblePierce() {
        super(60, true, false);
    }

    public static void onLivingHurt(ServerPlayer player, LivingEntity target, float originalDamage, boolean isMagic, int resonanceLevel) {
        CompoundTag targetData = target.getPersistentData();
        int hitCount = targetData.getInt(HIT_COUNT_PATH);
        float x = (float) (Math.min(1d + (resonanceLevel / 4d) + hitCount * 0.1d, 10d + (resonanceLevel / 2d)) / 100d);
        float extraDamage = originalDamage * x;

        if (extraDamage >= 0.5f) {
            int manaCost = (int) (extraDamage * MANA_COST_RATIO);
            if (ManaItemHandler.instance().requestManaExactForTool(BotaniaItems.terraSword.getDefaultInstance(), player, manaCost, true)) {
                Level level = target.level();
                if (isMagic) {
                    AttackManager.doAttackWith(player.damageSources().sonicBoom(player), extraDamage, target, true, false);
                    if (level instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.SOUL, target.getX(), target.getY() + target.getBbHeight() / 2.0, target.getZ(), 8, 0.5, 0.5, 0.5, 0.05);
                    }
                } else {
                    AttackManager.doAttackWith(player.damageSources().indirectMagic(player, player), extraDamage, target, true, false);
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), BotaniaSounds.terraBlade, SoundSource.PLAYERS, 1.5F, 1.0F);
                    if (level instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.DRAGON_BREATH, target.getX(), target.getY() + target.getBbHeight() / 2.0, target.getZ(), 8, 0.5, 0.5, 0.5, 0.05);
                    }
                }
            }
        }
        targetData.putInt(HIT_COUNT_PATH, Math.min(hitCount + 1, 400));
    }
}
