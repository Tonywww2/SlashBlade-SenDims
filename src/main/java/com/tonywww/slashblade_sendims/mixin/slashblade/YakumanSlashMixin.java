package com.tonywww.slashblade_sendims.mixin.slashblade;

import com.tonywww.slashblade_sendims.utils.PositionedSlashes;
import com.tonywww.slashblade_sendims.yakuman.YakumanArts;
import mods.flammpfeil.slashblade.entity.EntitySlashEffect;
import mods.flammpfeil.slashblade.util.AttackManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.List;
import java.util.function.Consumer;

/**
 * Yakuman slashes sync their lifetime to the client. Slashes from {@link PositionedSlashes}
 * (all pack SA slash damage) attack around themselves; other slashes keep the native pipeline.
 */
@Mixin(EntitySlashEffect.class)
public abstract class YakumanSlashMixin {
    @Unique private static final EntityDataAccessor<Integer> yakumanLifetime=
            SynchedEntityData.defineId(EntitySlashEffect.class,EntityDataSerializers.INT);
    @Shadow(remap=false) private int lifetime;
    @Inject(method="defineSynchedData",at=@At("TAIL"))
    private void yakumanDefineLifetime(CallbackInfo ci) {
        ((EntitySlashEffect)(Object)this).getEntityData().define(yakumanLifetime,10);
    }
    @Inject(method="setLifetime",at=@At("TAIL"),remap=false)
    private void yakumanSetLifetime(int value,CallbackInfo ci) {
        var slash=(EntitySlashEffect)(Object)this;
        if((slash.getColor()&0xff000000)==YakumanArts.NATIVE_COLOR_MARKER)
            slash.getEntityData().set(yakumanLifetime,value);
    }
    @Inject(method="tick",at=@At("HEAD"))
    private void yakumanReadLifetime(CallbackInfo ci) {
        var slash=(EntitySlashEffect)(Object)this;
        if(slash.level().isClientSide()&&(slash.getColor()&0xff000000)==YakumanArts.NATIVE_COLOR_MARKER)
            lifetime=slash.getEntityData().get(yakumanLifetime);
    }
    @Redirect(method="tick",at=@At(value="INVOKE",target="Lmods/flammpfeil/slashblade/util/AttackManager;areaAttack(Lnet/minecraft/world/entity/Entity;Ljava/util/function/Consumer;DZZLjava/util/List;)Ljava/util/List;",remap=false))
    private List<Entity> positionedDamage(Entity entity,Consumer<LivingEntity> beforeHit,
                                       double reach,boolean force,boolean reset,List<Entity> excluded) {
        var slash=(EntitySlashEffect)entity;
        if(PositionedSlashes.isPositioned(slash))
            return PositionedSlashes.damage(slash,beforeHit,force,reset,excluded);
        return AttackManager.areaAttack(slash,beforeHit,reach,force,reset,excluded);
    }
}
