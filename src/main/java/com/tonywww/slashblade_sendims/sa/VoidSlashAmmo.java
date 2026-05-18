package com.tonywww.slashblade_sendims.sa;

import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.entity.EntitySlashEffect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.tuple.Triple;

public class VoidSlashAmmo extends AmmoSA {

    public static int maxAmmo = 3;
    public static int cdAfterSlash = 10;

    public static void onInit(LivingEntity attacker) {
        Triple<ServerPlayer, ItemStack, ISlashBladeState> triple = AmmoSA.saInit(attacker);
        if (triple != null) {
            AmmoSA.onInit(triple.getLeft(), triple.getMiddle(), triple.getRight(), maxAmmo);
        }
    }

    public static boolean onSlashEffects(ServerPlayer attacker, ItemStack stack, ISlashBladeState state) {
        if (AmmoSA.onSlashEffects(attacker, stack, state, maxAmmo, cdAfterSlash)) {
            ServerLevel level = attacker.serverLevel();
            EntitySlashEffect slash = new EntitySlashEffect(SlashBlade.RegistryEvents.SlashEffect, level);

            slash.setPos(
                    attacker.getX(),
                    attacker.getY() + attacker.getEyeHeight() / 2.0,
                    attacker.getZ()
            );

            CompoundTag tag = stack.getOrCreateTag();
            int currentAmmo = tag.contains(AmmoSA.AMMO_PATH) ? tag.getInt(AmmoSA.AMMO_PATH) : maxAmmo;
            int timesUsed = maxAmmo - currentAmmo;

            slash.setDamage(4f);
            slash.setRank(7);
            slash.setBaseSize(1.5f);

            slash.setYRot(attacker.getYRot());
            slash.setXRot(attacker.getXRot());
            slash.setRotationRoll((30 + (timesUsed * 90)) % 360);

            int colorCode = state.getColorCode();
            slash.setColor(colorCode);

            slash.setOwner(attacker);

            level.addFreshEntity(slash);

            return true;
        }
        return false;
    }
}

