package com.tonywww.slashblade_sendims.sa;

import com.tonywww.slashblade_sendims.registeries.SBSDSlashArtRegistry;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.slasharts.SakuraEnd;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.tracen.umapyoi.api.UmapyoiAPI;
import net.tracen.umapyoi.utils.UmaSoulUtils;
import org.apache.commons.lang3.tuple.Triple;

@Mod.EventBusSubscriber
public class SakuraEndAmmo extends AmmoSA {

    public static int maxAmmo = 36;
    public static int cdAfterSlash = 0;

    public static final String HIT_COUNT_PATH = "sdbf.sea.hit";

    public static void onInit(LivingEntity attacker) {
        Triple<ServerPlayer, ItemStack, ISlashBladeState> triple = AmmoSA.saInit(attacker);
        if (triple != null) {
            AmmoSA.onInit(triple.getLeft(), triple.getMiddle(), triple.getRight(), maxAmmo);
            CompoundTag tag = triple.getMiddle().getOrCreateTag();
            tag.putInt(HIT_COUNT_PATH, 0);
        }
    }

    public static boolean onSlashEffects(ServerPlayer attacker, ItemStack stack, ISlashBladeState state) {
        CompoundTag tag = stack.getOrCreateTag();
        int currentAmmo = tag.contains(AmmoSA.AMMO_PATH) ? tag.getInt(AmmoSA.AMMO_PATH) : maxAmmo;

        if (AmmoSA.onSlashEffects(attacker, stack, state, maxAmmo, cdAfterSlash)) {
            int newAmmo = currentAmmo - 1;
            int timesUsed = maxAmmo - newAmmo;

            if (timesUsed % 6 == 0) {

                // 袈裟斩
                SakuraEnd.doSlash(attacker, 45f, Vec3.ZERO, false, false, 0.4f);
                // 逆袈裟斩
                SakuraEnd.doSlash(attacker, 315f, Vec3.ZERO, false, false, 0.4f);
            }

            if (newAmmo <= 0) {
                int hitCount = tag.getInt(HIT_COUNT_PATH);
                if (hitCount > 0) {
                    float ratio = Math.min(hitCount / 5, 18) * 0.01f;
                    attacker.heal(attacker.getMaxHealth() * ratio);

                    ItemStack soul = UmapyoiAPI.getUmaSoul(attacker);
                    UmaSoulUtils.addActionPoint(soul, (int) (UmaSoulUtils.getMaxActionPoint(soul) * ratio));

                }
                tag.remove(HIT_COUNT_PATH);
            }

            return true;
        }
        return false;
    }

    @SubscribeEvent
    public static void onHit(SlashBladeEvent.HitEvent event) {
        LivingEntity livingEntity = event.getUser();
        if (livingEntity == null || livingEntity.level().isClientSide()) return;
        if (!(livingEntity instanceof ServerPlayer serverPlayer)) return;

        ISlashBladeState state = event.getSlashBladeState();
        if (state == null) return;

        if (state.getSlashArts() == SBSDSlashArtRegistry.SAKURA_END_AMMO.get()) {
            ItemStack stack = event.getBlade();
            CompoundTag tag = stack.getOrCreateTag();
            if (tag.contains(AmmoSA.AMMO_PATH)) {
                int ammo = tag.getInt(AmmoSA.AMMO_PATH);
                if (ammo > 0) {
                    int hc = tag.getInt(HIT_COUNT_PATH);
                    tag.putInt(HIT_COUNT_PATH, hc + 1);
                }
            }
        }
    }
}
