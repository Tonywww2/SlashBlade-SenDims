package com.tonywww.slashblade_sendims.compat.draconicevolution;

import com.brandon3055.draconicevolution.api.capability.DECapabilities;
import com.brandon3055.draconicevolution.api.capability.ModuleHost;
import com.brandon3055.draconicevolution.api.modules.ModuleTypes;
import com.brandon3055.draconicevolution.api.modules.entities.ShieldControlEntity;
import com.brandon3055.draconicevolution.init.DEDamage;
import com.brandon3055.draconicevolution.items.equipment.IModularArmor;
import com.tonywww.slashblade_sendims.mixin.draconicevolution.ShieldControlEntityAccessor;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDamageEvent;

/**
 * Draconic shield absorption on the final damage value, immediately before health is reduced.
 *
 * <p>DE's own {@code tryBlockDamage(LivingDamageEvent)} runs on an unposted event, so shield points,
 * boost, cooldown, hit animation and sound behave exactly as in DE. One correction: DE subtracts the
 * shield from the type-weighted cost (armor-bypassing ×3, magic ×2) and used to pass that weighted
 * remainder on as real damage. The remainder is converted back, so a nearly empty shield never makes
 * a hit hurt more than having no shield.</p>
 */
public final class DraconicShieldFinalStage {
    private DraconicShieldFinalStage() {}

    /**
     * {@code ForgeHooks.onLivingDamage} return value, right before {@code setHealth}. A hit the shield
     * takes whole marks the open {@code hurt} frame, which then drops the held armor durability damage
     * and ends {@code hurt} early (no knockback, cooldown, hurt animation or sound, attacker on-hit effects).
     */
    public static float finalDamage(LivingEntity entity, DamageSource source, float damage) {
        ShieldHurtFrame frame = entity instanceof ShieldHurtTracker tracker ? tracker.sendims$shieldFrame() : null;
        float left = damage > 0.0F ? absorb(entity, source, damage) : damage;
        if (frame != null) {
            if (damage > 0.0F && left <= 0.0F) {
                frame.blocked = true;
                frame.pendingArmor = null;
            } else {
                flushArmor(entity, frame);
            }
        }
        return left;
    }

    /** Opens a frame around {@code actuallyHurt}; values are the cooldown state before this hit. */
    public static ShieldHurtFrame open(ShieldHurtFrame outer, int priorInvulnerableTime, float priorLastHurt) {
        return new ShieldHurtFrame(outer, priorInvulnerableTime, priorLastHurt);
    }

    /** @return true when the shield took the whole hit and the rest of {@code hurt} must be skipped. */
    public static boolean close(LivingEntity entity, ShieldHurtFrame frame) {
        if (frame.blocked) return true;
        flushArmor(entity, frame);
        return false;
    }

    /** Armor durability: held inside {@code hurt} while a shield with points is worn. */
    public static boolean holdArmorDamage(LivingEntity entity, DamageSource source, float amount) {
        if (!(entity instanceof ShieldHurtTracker tracker)) return false;
        ShieldHurtFrame frame = tracker.sendims$shieldFrame();
        if (frame == null || frame.blocked) return false;
        if (frame.pendingArmor == null) {
            ShieldControlEntity shield = findShield(entity);
            if (shield == null || shield.getShieldPoints() <= 0) return false;
        }
        frame.hold(source, amount);
        return true;
    }

    private static void flushArmor(LivingEntity entity, ShieldHurtFrame frame) {
        var pending = frame.pendingArmor;
        if (pending == null) return;
        frame.pendingArmor = null;
        for (ShieldHurtFrame.PendingArmor hit : pending) {
            ((ShieldHurtTracker) entity).sendims$hurtArmorNow(hit.source(), hit.amount());
        }
    }

    public static ShieldControlEntity findShield(LivingEntity entity) {
        ItemStack armor = IModularArmor.getArmor(entity);
        if (armor.isEmpty()) return null;
        ModuleHost host = armor.getCapability(DECapabilities.MODULE_HOST_CAPABILITY).orElse(null);
        if (host == null) return null;
        return host.getEntitiesByType(ModuleTypes.SHIELD_CONTROLLER)
                .map(entry -> (ShieldControlEntity) entry).findAny().orElse(null);
    }

    public static float absorb(LivingEntity entity, DamageSource source, float damage) {
        if (entity.level().isClientSide() || source.is(DEDamage.KILL)) return damage;
        ShieldControlEntity shield = findShield(entity);
        return shield == null ? damage : absorb(shield, entity, source, damage);
    }

    public static float absorb(ShieldControlEntity shield, LivingEntity entity, DamageSource source, float damage) {
        float weighted = ((ShieldControlEntityAccessor) shield).sendims$applyDamageModifiers(source, damage);
        LivingDamageEvent probe = new LivingDamageEvent(entity, source, damage);
        shield.tryBlockDamage(probe);
        if (probe.isCanceled()) return 0.0F;
        float left = probe.getAmount();
        if (left == damage || weighted <= damage) return left;
        return Math.min(damage, left * damage / weighted);
    }
}
