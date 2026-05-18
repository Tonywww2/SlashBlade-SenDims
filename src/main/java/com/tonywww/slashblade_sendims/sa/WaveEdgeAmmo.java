package com.tonywww.slashblade_sendims.sa;

import com.tonywww.slashblade_sendims.SenDims;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.slasharts.WaveEdge;
import mods.flammpfeil.slashblade.util.KnockBacks;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.Triple;

public class WaveEdgeAmmo extends AmmoSA {

    public static int maxAmmo = 8;
    public static int cdAfterSlash = 2;

    public static void onInit(LivingEntity attacker) {
        Triple<ServerPlayer, ItemStack, ISlashBladeState> triple = AmmoSA.saInit(attacker);
        if (triple != null) {
            AmmoSA.onInit(triple.getLeft(), triple.getMiddle(), triple.getRight(), maxAmmo);
        }
    }

    public static boolean onSlashEffects(ServerPlayer attacker, ItemStack stack, ISlashBladeState state) {
        CompoundTag tag = stack.getOrCreateTag();
        int currentAmmo = tag.contains(AmmoSA.AMMO_PATH) ? tag.getInt(AmmoSA.AMMO_PATH) : maxAmmo;

        if (AmmoSA.onSlashEffects(attacker, stack, state, maxAmmo, cdAfterSlash)) {
            int timesUsed = maxAmmo - currentAmmo;

            double damage = state.getDamage() * 0.3d;
            float minSpeed = 0.2f + 0.1f * timesUsed;
            float maxSpeed = 1f + 0.1f * timesUsed;
            int lifetime = 20 + 2 * timesUsed;

            WaveEdge.doSlash(
                    attacker,
                    90f,
                    lifetime,
                    Vec3.ZERO,
                    false,
                    damage,
                    KnockBacks.cancel,
                    minSpeed,
                    maxSpeed,
                    timesUsed
            );

            return true;
        }
        return false;
    }
}

