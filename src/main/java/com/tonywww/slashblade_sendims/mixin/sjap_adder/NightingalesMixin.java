package com.tonywww.slashblade_sendims.mixin.sjap_adder;

import cn.mmf.slashblade_addon.entity.BlisteringSwordsEntity;
import cn.mmf.slashblade_addon.entity.LightingSwordEntity;
import cn.mmf.slashblade_addon.registry.SBAEntitiesRegistry;
import com.dinzeer.legendreliclib.lib.util.slashblade.AbstractSpecialEffect;
import com.dinzeer.sjapadder.se.kamuy.Nightingales;
import mods.flammpfeil.slashblade.capability.concentrationrank.ConcentrationRankCapabilityProvider;
import mods.flammpfeil.slashblade.capability.concentrationrank.IConcentrationRank;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Nightingales.class, remap = false)
public abstract class NightingalesMixin extends AbstractSpecialEffect {

    public NightingalesMixin(int requestLevel) {
        super(requestLevel);
    }

    @Inject(method = "handleDoSlash", at = @At("HEAD"), cancellable = true)
    private void onHandleDoSlash(SlashBladeEvent.DoSlashEvent event, LivingEntity attacker, CallbackInfo ci) {
        ci.cancel();
        if (attacker.level().isClientSide() || !isEffective(attacker)) return;
        if (attacker.getRandom().nextFloat() >= 0.5F) return;

        var state = event.getSlashBladeState();
        int rank = attacker.getCapability(ConcentrationRankCapabilityProvider.RANK_POINT)
                .map(value -> value.getRank(attacker.level().getGameTime()).level).orElse(0);
        int count = rank >= IConcentrationRank.ConcentrationRanks.S.level ? 2 : 1;
        double damage = 4.0D + state.getRefine() * 0.05D;
        for (int index = 0; index < count; index++) {
            sendims$spawnSword(attacker, false, state.getColorCode(), damage, 20 + index);
        }
        if (rank >= IConcentrationRank.ConcentrationRanks.SS.level) {
            sendims$spawnSword(attacker, true, state.getColorCode(), damage, 21 + count);
        }
        attacker.playSound(SoundEvents.CHORUS_FRUIT_TELEPORT, 0.2F, 1.45F);
    }

    @Unique
    private static void sendims$spawnSword(LivingEntity attacker, boolean lightning,
                                           int color, double damage, int delay) {
        BlisteringSwordsEntity sword = lightning
                ? new LightingSwordEntity(SBAEntitiesRegistry.LightingSwords, attacker.level())
                : new BlisteringSwordsEntity(SBAEntitiesRegistry.BlisteringSwords, attacker.level());
        sword.setSpeed(2.5F);
        sword.setIsCritical(false);
        sword.setOwner(attacker);
        sword.setColor(lightning ? 16766720 : color);
        sword.setRoll(0.0F);
        sword.setDamage(damage);
        sword.setDelay(delay);

        RandomSource random = attacker.getRandom();
        Vec3 offset = new Vec3(random.nextDouble() * 2.5D * (delay % 2 == 0 ? 1 : -1),
                random.nextFloat() * 2.0F, random.nextFloat() * 0.5D);
        sword.setPos(attacker.position().add(offset));
        sword.setOffset(offset);
        attacker.level().addFreshEntity(sword);
        sword.startRiding(attacker, true);
    }
}
