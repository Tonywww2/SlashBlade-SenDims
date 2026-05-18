package com.tonywww.slashblade_sendims.sa;

import com.tonywww.slashblade_sendims.registeries.SBSDSlashArtRegistry;
import com.tonywww.slashblade_sendims.utils.SlashBladeUtil;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.registry.ComboStateRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.commons.lang3.tuple.Triple;

import java.util.HashSet;

@Mod.EventBusSubscriber
public class AmmoSA {

    public static final String AMMO_PATH = "sdbf.ammo";
    public static final String MAX_AMMO_PATH = "sdbf.max_ammo";
    public static int maxAmmo;
    public static int cdAfterSlash;

    public static final HashSet<ResourceLocation> BANNED_COMBO = new HashSet<>();

    static {
        BANNED_COMBO.add(ComboStateRegistry.RAPID_SLASH.getId());
    }

    public static Triple<ServerPlayer, ItemStack, ISlashBladeState> saInit(LivingEntity attacker) {
        if (!(attacker instanceof ServerPlayer serverPlayer)) return null;
        ItemStack stack = attacker.getMainHandItem();
        ISlashBladeState state = SlashBladeUtil.getState(stack);

        return Triple.of(serverPlayer, stack, state);
    }

    public static void onInit(ServerPlayer attacker, ItemStack stack, ISlashBladeState state, int maxAmmo) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt(AMMO_PATH, maxAmmo);
        tag.putInt(MAX_AMMO_PATH, maxAmmo);

    }

    public static boolean onSlashEffects(ServerPlayer attacker, ItemStack stack, ISlashBladeState state, int maxAmmoO, int cdAfterSlashO) {
        CompoundTag tag = stack.getOrCreateTag();
        int ammo = tag.getInt(AMMO_PATH);
        if (ammo > 0 && ammo <= maxAmmoO) {
            int newAmmo = ammo - 1;
            tag.putInt(AMMO_PATH, ammo - 1);
            if (cdAfterSlashO > 0) {
                attacker.getCooldowns().addCooldown(stack.getItem(), cdAfterSlashO);
            }
            if (newAmmo == 0) {
                tag.remove(AMMO_PATH);
                tag.remove(MAX_AMMO_PATH);
            }
            return true;
        }
        return false;

    }

    @SubscribeEvent
    public static void onDoSlash(SlashBladeEvent.DoSlashEvent event) {
        LivingEntity livingEntity = event.getUser();
        if (livingEntity.level().isClientSide()) return;
        if (!(livingEntity instanceof ServerPlayer serverPlayer)) return;

        ISlashBladeState state = event.getSlashBladeState();
        if (state == null) return;

        if (BANNED_COMBO.contains(state.getComboSeq())) return;

        if (state.getSlashArts() == SBSDSlashArtRegistry.EXPLOSIVE_DAWN_AMMO.get()) {
            ExplosiveDawnAmmo.onSlashEffects(serverPlayer, event.getBlade(), state);
        } else if (state.getSlashArts() == SBSDSlashArtRegistry.WAVE_EDGE_AMMO.get()) {
            WaveEdgeAmmo.onSlashEffects(serverPlayer, event.getBlade(), state);
        } else if (state.getSlashArts() == SBSDSlashArtRegistry.VOID_SLASH_AMMO.get()) {
            VoidSlashAmmo.onSlashEffects(serverPlayer, event.getBlade(), state);
        } else if (state.getSlashArts() == SBSDSlashArtRegistry.SAKURA_END_AMMO.get()) {
            SakuraEndAmmo.onSlashEffects(serverPlayer, event.getBlade(), state);
        }

    }

}
