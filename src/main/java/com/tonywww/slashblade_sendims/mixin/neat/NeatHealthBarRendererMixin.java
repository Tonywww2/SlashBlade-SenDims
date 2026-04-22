package com.tonywww.slashblade_sendims.mixin.neat;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vazkii.neat.HealthBarRenderer;
import vazkii.neat.NeatConfig;

import java.util.HexFormat;

@Mixin(HealthBarRenderer.class)
public class NeatHealthBarRendererMixin {

    @Inject(method = "hookRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getArmorValue()I", shift = At.Shift.BEFORE), cancellable = true)
    private static void onArmorRender(Entity entity, PoseStack poseStack, MultiBufferSource buffers, Camera camera, EntityRenderer<? super Entity> entityRenderer, float partialTicks, double x, double y, double z, CallbackInfo ci) {
        if (!(entity instanceof LivingEntity living)) return;

        int armor = living.getArmorValue();
        if (armor <= 0 || !NeatConfig.instance.showArmor()) {
            poseStack.popPose();
            poseStack.popPose();
            ci.cancel();
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        boolean boss = HealthBarRenderer.isBoss(living);
        String name = living.hasCustomName() ? net.minecraft.ChatFormatting.ITALIC + living.getCustomName().getString() : living.getDisplayName().getString();
        float nameLen = (float) mc.font.width(name) * 0.5f;
        float halfSize = Math.max((float) NeatConfig.instance.plateSize(), nameLen / 2.0f + 10.0f);

        float iconOffset = 12.85f;
        float zShift = 0.0f;
        if (NeatConfig.instance.showAttributes()) {
            iconOffset += 5.0f;
            zShift -= 0.1f;
        }
        float globalScale = 0.0267f;

        ItemStack iron = new ItemStack(Items.IRON_CHESTPLATE);
        poseStack.pushPose();
        double dx = (double) ((halfSize - iconOffset) * globalScale) + NeatConfig.instance.iconOffsetX();
        double dy = 3.0f * globalScale;
        double dz = zShift * globalScale;
        poseStack.translate(-dx, dy + NeatConfig.instance.iconOffsetY(), dz);
        poseStack.scale(0.12f, 0.12f, 0.12f);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0f));
        mc.getItemRenderer().renderStatic(iron, ItemDisplayContext.NONE, 0xF000F0, OverlayTexture.NO_OVERLAY, poseStack, buffers, living.level(), 0);
        poseStack.popPose();

        iconOffset += 4.5f; // Gives sufficient space to clear the item icon
        zShift -= 0.1f;

        poseStack.pushPose();
        double textDx = (double) ((halfSize - iconOffset + 6.5) * globalScale) + NeatConfig.instance.iconOffsetX();
        poseStack.translate(-textDx, dy + NeatConfig.instance.iconOffsetY(), dz);

        // Reverse globalScale to make text upright, then apply Neat's text scale
        poseStack.scale(-globalScale, -globalScale, globalScale);
        poseStack.scale(0.375f, 0.375f, 0.375f);

        String armorText = "×" + armor;
        int textColor = HexFormat.fromHexDigits(NeatConfig.instance.textColor());
        // Draw the string vertically centered relative to the icon center
        mc.font.drawInBatch(armorText, 0.0f, -4.0f, textColor, false, poseStack.last().pose(), buffers, Font.DisplayMode.NORMAL, 0, 0xF000F0);
        poseStack.popPose();

        poseStack.popPose();
        poseStack.popPose();

        ci.cancel();
    }
}
