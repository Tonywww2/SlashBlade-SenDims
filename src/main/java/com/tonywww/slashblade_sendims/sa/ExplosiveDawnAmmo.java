package com.tonywww.slashblade_sendims.sa;

import com.dinzeer.sjapadder.register.SjaSpecialEffectRegsitry;
import com.dinzeer.sjapadder.sa.ExplosiveDawn;
import com.dinzeer.sjapadder.se.common.ShadowOfRevenant;
import com.tonywww.slashblade_sendims.SenDims;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.tuple.Triple;

public class ExplosiveDawnAmmo extends AmmoSA {

    public static int maxAmmo = 5;
    public static int cdAfterSlash = 35;

    public static void onInit(LivingEntity attacker) {
        Triple<ServerPlayer, ItemStack, ISlashBladeState> triple = AmmoSA.saInit(attacker);
        if (triple != null) {
            onInit(triple.getLeft(), triple.getMiddle(), triple.getRight());

        }
    }

    public static void onInit(ServerPlayer attacker, ItemStack stack, ISlashBladeState state) {
        AmmoSA.onInit(attacker, stack, state, maxAmmo);

    }

    public static boolean onSlashEffects(ServerPlayer attacker, ItemStack stack, ISlashBladeState state) {
        if (AmmoSA.onSlashEffects(attacker, stack, state, maxAmmo, cdAfterSlash)) {
            ServerLevel serverLevel = attacker.serverLevel();
            Entity target = state.getTargetEntity(serverLevel);

            if (target != null) {
                attacker.playNotifySound(SoundEvents.AMETHYST_BLOCK_PLACE, net.minecraft.sounds.SoundSource.PLAYERS, 10.0F, 0.5F);
                attacker.playNotifySound(SoundEvents.AMETHYST_BLOCK_FALL, net.minecraft.sounds.SoundSource.PLAYERS, 5.0F, 2.0F);
                attacker.playNotifySound(SoundEvents.AMETHYST_BLOCK_RESONATE, net.minecraft.sounds.SoundSource.PLAYERS, 3.0F, 1.0F);

                SenDims.serverScheduler.schedule(10, () -> {

                    boolean hasRevenantSE = SpecialEffect.isEffective(SjaSpecialEffectRegsitry.SHADOW_OF_REVENANT.get(), attacker.experienceLevel) &
                            state.hasSpecialEffect(SjaSpecialEffectRegsitry.SHADOW_OF_REVENANT.getId());

                    if (hasRevenantSE) {
                        CompoundTag nbt = stack.getOrCreateTag();
                        if (!nbt.contains("slashblade_shadow_of_revenant")) {
                            nbt.put("slashblade_shadow_of_revenant", new CompoundTag());
                        }

                        CompoundTag shadowOfRevenantTag = nbt.getCompound("slashblade_shadow_of_revenant");

                        for(int i = 0; i < 3; ++i) {
                            shadowOfRevenantTag.putInt(ShadowOfRevenant.REVENANT_KEYS[i], 5);
                        }
                    }

                    ExplosiveDawn.doExplosion(serverLevel, attacker, state, target.blockPosition(), hasRevenantSE);
                });
                return true;

            } else {
                CompoundTag tag = stack.getOrCreateTag();
                tag.putInt(AmmoSA.AMMO_PATH, tag.getInt(AmmoSA.AMMO_PATH) + 1);
                tag.putInt(AmmoSA.MAX_AMMO_PATH, maxAmmo);
                return false;
            }

        }
        return false;
    }
}
