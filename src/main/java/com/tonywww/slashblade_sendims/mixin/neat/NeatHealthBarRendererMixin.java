package com.tonywww.slashblade_sendims.mixin.neat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tonywww.slashblade_sendims.client.performance.IrisShadowPass;
import com.tonywww.slashblade_sendims.client.performance.NeatRenderCache;
import com.tonywww.slashblade_sendims.config.ClientPerformanceConfig;
import net.minecraft.ChatFormatting;
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
import net.minecraft.world.level.ItemLike;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vazkii.neat.HealthBarRenderer;
import vazkii.neat.NeatConfig;

import java.text.DecimalFormat;

@Mixin(HealthBarRenderer.class)
public abstract class NeatHealthBarRendererMixin {
    @Inject(method = "hookRender", at = @At("HEAD"), cancellable = true, remap = false)
    private static void sdbf$filter(Entity entity, PoseStack poses, MultiBufferSource buffers, Camera camera,
                                    EntityRenderer<? super Entity> renderer, float partialTicks,
                                    double x, double y, double z, CallbackInfo callback) {
        // HEAD has not pushed a pose yet. Cancel only the UI; entity/shadow rendering continues upstream.
        if (IrisShadowPass.isActive()) {
            callback.cancel();
            return;
        }
        if (entity instanceof LivingEntity living) {
            boolean targeted = Minecraft.getInstance().crosshairPickEntity == entity;
            boolean hidden = switch (ClientPerformanceConfig.HEALTH_BARS.get()) {
                case ALL -> false;
                case DAMAGED_OR_TARGETED -> !targeted && living.getHealth() >= living.getMaxHealth();
                case TARGETED -> !targeted;
            };
            if (hidden) {
                callback.cancel();
            }
        }
    }

    @Redirect(method = "hookRender", remap = false,
            at = @At(value = "NEW", target = "(Ljava/lang/String;)Ljava/text/DecimalFormat;", remap = false))
    private static DecimalFormat sdbf$formatter(String pattern) {
        return NeatRenderCache.current().formatter(pattern);
    }

    @Redirect(method = "hookRender", remap = false,
            at = @At(value = "INVOKE", target = "Ljava/text/DecimalFormat;format(D)Ljava/lang/String;", remap = false))
    private static String sdbf$format(DecimalFormat formatter, double value) {
        return NeatRenderCache.current().format(formatter, value);
    }

    @Redirect(method = "hookRender",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;width(Ljava/lang/String;)I"))
    private static int sdbf$width(Font font, String text) {
        return NeatRenderCache.current().width(font, text);
    }

    @Redirect(method = "hookRender", remap = false,
            at = @At(value = "INVOKE", target = "Ljava/util/HexFormat;fromHexDigits(Ljava/lang/CharSequence;)I", remap = false))
    private static int sdbf$color(CharSequence color) {
        return NeatRenderCache.current().color(color);
    }

    @Redirect(method = {"hookRender", "getIcon"}, remap = false,
            at = @At(value = "NEW", target = "(Lnet/minecraft/world/level/ItemLike;)Lnet/minecraft/world/item/ItemStack;", remap = false))
    private static ItemStack sdbf$icon(ItemLike item) {
        return NeatRenderCache.current().icon(item);
    }

    @Redirect(method = "hookRender", remap = false,
            at = @At(value = "INVOKE", target = "Lvazkii/neat/NeatConfig$ConfigAccess;showAttributes()Z", remap = false))
    private static boolean sdbf$attributes(NeatConfig.ConfigAccess config) {
        return config.showAttributes() && ClientPerformanceConfig.ATTRIBUTE_ICONS.get();
    }

    @Redirect(method = "hookRender", remap = false,
            at = @At(value = "INVOKE", target = "Lvazkii/neat/NeatConfig$ConfigAccess;showMaxHP()Z", remap = false))
    private static boolean sdbf$maxHealth(NeatConfig.ConfigAccess config) {
        return config.showMaxHP() && !ClientPerformanceConfig.COMPACT_HEALTH_TEXT.get();
    }

    @Redirect(method = "hookRender", remap = false,
            at = @At(value = "INVOKE", target = "Lvazkii/neat/NeatConfig$ConfigAccess;showPercentage()Z", remap = false))
    private static boolean sdbf$percentage(NeatConfig.ConfigAccess config) {
        return config.showPercentage() && !ClientPerformanceConfig.COMPACT_HEALTH_TEXT.get();
    }

    @Inject(method = "hookRender", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;getArmorValue()I", shift = At.Shift.BEFORE), cancellable = true)
    private static void sdbf$armor(Entity entity, PoseStack poses, MultiBufferSource buffers, Camera camera,
                                   EntityRenderer<? super Entity> renderer, float partialTicks,
                                   double x, double y, double z, CallbackInfo callback) {
        if (!(entity instanceof LivingEntity living)) {
            return;
        }
        // Neat 41 has two outstanding pushes at this exact site. The replaced tail owns both pops.
        try {
            int armor = living.getArmorValue();
            NeatConfig.ConfigAccess config = NeatConfig.instance;
            if (armor > 0 && config.showArmor()) {
                Minecraft mc = Minecraft.getInstance();
                NeatRenderCache cache = NeatRenderCache.current();
                String name = living.hasCustomName()
                        ? ChatFormatting.ITALIC + living.getCustomName().getString() : living.getDisplayName().getString();
                float halfSize = Math.max((float) config.plateSize(), cache.width(mc.font, name) * 0.25f + 10.0f);
                boolean attributes = config.showAttributes() && ClientPerformanceConfig.ATTRIBUTE_ICONS.get();
                float iconOffset = 12.85f + (attributes ? 5.0f : 0.0f);
                float scale = 0.0267f;
                double dy = 3.0f * scale + config.iconOffsetY();
                double dz = (attributes ? -0.1f : 0.0f) * scale;

                poses.pushPose();
                try {
                    poses.translate(-((halfSize - iconOffset) * scale + config.iconOffsetX()), dy, dz);
                    poses.scale(0.12f, 0.12f, 0.12f);
                    poses.mulPose(Axis.YP.rotationDegrees(180.0f));
                    mc.getItemRenderer().renderStatic(cache.icon(Items.IRON_CHESTPLATE), ItemDisplayContext.NONE,
                            0xF000F0, OverlayTexture.NO_OVERLAY, poses, buffers, living.level(), 0);
                } finally {
                    poses.popPose();
                }

                poses.pushPose();
                try {
                    poses.translate(-((halfSize - iconOffset - 4.5f + 6.5f) * scale + config.iconOffsetX()), dy, dz);
                    poses.scale(-scale, -scale, scale);
                    poses.scale(0.375f, 0.375f, 0.375f);
                    mc.font.drawInBatch(cache.armorText(armor), 0.0f, -4.0f, cache.color(config.textColor()), false,
                            poses.last().pose(), buffers, Font.DisplayMode.NORMAL, 0, 0xF000F0);
                } finally {
                    poses.popPose();
                }
            }
        } finally {
            poses.popPose();
            poses.popPose();
        }
        callback.cancel();
    }
}
