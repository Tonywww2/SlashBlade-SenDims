package com.tonywww.slashblade_sendims.se;

import com.tonywww.slashblade_sendims.SBSDValues;
import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.registeries.SBSDSpecialEffects;
import com.tonywww.slashblade_sendims.utils.TetraUtils;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import vazkii.botania.api.mana.ManaItemHandler;
import vazkii.botania.common.handler.BotaniaSounds;
import vazkii.botania.common.item.BotaniaItems;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import vazkii.botania.client.fx.WispParticleData;

import java.util.List;

import mods.flammpfeil.slashblade.util.AttackManager;

@Mod.EventBusSubscriber
public class ManaDetonation extends SpecialEffect {

    public static final String STORED_DAMAGE_PATH = "sbsd.sb.mana_detonation_damage";

    public static double MANA_DETONATION_RANGE = 8.0D;

    public ManaDetonation() {
        super(45, true, false);
        MinecraftForge.EVENT_BUS.addListener(this::onSlashBladeUpdate);
    }

    public void onSlashBladeUpdate(SlashBladeEvent.UpdateEvent event) {
        ISlashBladeState state = event.getSlashBladeState();
        if (state.hasSpecialEffect(SBSDSpecialEffects.MANA_DETONATION.getId())) {
            if (!(event.getEntity() instanceof Player player)) {
                return;
            }

            if (!event.isSelected()) {
                return;
            }

            // Mana repair
            Level level = player.level();
            if (level.getGameTime() % 20 == 0 && event.getBlade().getDamageValue() > 0) {
                if (ManaItemHandler.instance().
                        requestManaExactForTool(BotaniaItems.terraSword.getDefaultInstance(), player, 100, true)) {
                    event.getBlade().setDamageValue(event.getBlade().getDamageValue() - 1);
                }
            }

            // Burst logic
            if (level.getGameTime() % 100 == 0) {
                CompoundTag tag = event.getBlade().getOrCreateTag();
                float storedDamage = tag.getFloat(STORED_DAMAGE_PATH);
                double attackDamage = player.getAttributeValue(Attributes.ATTACK_DAMAGE);

                if (storedDamage >= attackDamage * 2.0) {
                    int a = TetraUtils.getEffectLvlTotal(player, SBSDValues.MANA_RESONANCE);
                    float burstDamage = storedDamage * (0.8f + (a / 100f));

                    if (!level.isClientSide) {
                        tag.putFloat(STORED_DAMAGE_PATH, 0);

                        AABB aabb = player.getBoundingBox().inflate(MANA_DETONATION_RANGE);
                        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, aabb,
                                e -> e != player && e.isAlive()
                        );

                        level.playSound(null, player.getX(), player.getY(), player.getZ(), BotaniaSounds.holyCloak, SoundSource.PLAYERS, 1.0F, 0.6F);

                        for (LivingEntity target : targets) {
                            AttackManager.doAttackWith(player.damageSources().indirectMagic(player, player), burstDamage, target, false, true);
                        }
                    } else {
                        spawnManaParticles(player);
                    }
                }
            }
        }
    }

    private void spawnManaParticles(Player player) {
        float r = 0.2f;
        float g = 0.8f;
        float b = 1.0f;

        for (int i = 0; i < 36; i++) {
            double angle = i * 10 * (Math.PI / 180);
            double px = player.getX() + Math.cos(angle) * MANA_DETONATION_RANGE;
            double pz = player.getZ() + Math.sin(angle) * MANA_DETONATION_RANGE;
            WispParticleData data = WispParticleData.wisp(0.5f, r, g, b);
            player.level().addParticle(data, px, player.getY() + 1.0, pz, 0, 0, 0);
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ItemStack bladeStack = player.getMainHandItem();
        if (!(bladeStack.getItem() instanceof ItemSlashBlade)) return;

        ISlashBladeState state = bladeStack.getCapability(ItemSlashBlade.BLADESTATE).orElse(null);
        if (state == null || !state.hasSpecialEffect(SBSDSpecialEffects.MANA_DETONATION.getId())) return;

        if (!event.getSource().is(DamageTypeTags.WITCH_RESISTANT_TO)) {
            float originalDamage = event.getAmount();
            int a = TetraUtils.getEffectLvlTotal(player, SBSDValues.MANA_RESONANCE);
            SenDims.LOGGER.debug("Mana detonation: originalDamage {}", originalDamage);
            SenDims.LOGGER.debug("a value {}", a);
            SenDims.LOGGER.debug("New amount {}", originalDamage * (0.5f + (a / 2f)));

            event.setAmount(originalDamage * Math.min(1f, 0.5f + (a / 200f)));

            CompoundTag tag = bladeStack.getOrCreateTag();
            float newDamage = tag.getFloat(STORED_DAMAGE_PATH) + originalDamage * 0.5f;
            tag.putFloat(STORED_DAMAGE_PATH, newDamage);
        }
    }
}
