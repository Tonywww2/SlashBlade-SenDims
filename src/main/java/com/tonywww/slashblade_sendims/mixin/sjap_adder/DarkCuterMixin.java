package com.tonywww.slashblade_sendims.mixin.sjap_adder;

import com.dinzeer.sjapadder.se.kamuy.DarkCuter;
import mods.flammpfeil.slashblade.entity.EntityJudgementCut;
import mods.flammpfeil.slashblade.slasharts.JudgementCut;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = DarkCuter.class, remap = false)
public class DarkCuterMixin {
    @Unique
    private static final String SEN_DIMS$DARK_CUT_READY_AT = "sbsd.se.dark_cuter.ready_at";

    @Redirect(method = "handleDoSlash", at = @At(value = "INVOKE", target = "Lmods/flammpfeil/slashblade/slasharts/JudgementCut;doJudgementCut(Lnet/minecraft/world/entity/LivingEntity;)Lmods/flammpfeil/slashblade/entity/EntityJudgementCut;"))
    private EntityJudgementCut onDoJudgementCut(LivingEntity user) {
        if (user.level().isClientSide) return null;
        long now = user.level().getGameTime();
        CompoundTag data = user.getPersistentData();
        if (data.contains(SEN_DIMS$DARK_CUT_READY_AT) && now < data.getLong(SEN_DIMS$DARK_CUT_READY_AT)) return null;
        if (user.getRandom().nextFloat() >= 0.3F) return null;

        EntityJudgementCut cut = JudgementCut.doJudgementCut(user);
        data.putLong(SEN_DIMS$DARK_CUT_READY_AT, now + 20L);
        return cut;
    }
}
