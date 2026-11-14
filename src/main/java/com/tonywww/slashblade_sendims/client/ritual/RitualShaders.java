package com.tonywww.slashblade_sendims.client.ritual;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.logging.LogUtils;
import com.tonywww.slashblade_sendims.SenDims;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.function.Consumer;

/**
 * 仪式特效的自写着色器。它们只在世界绘制之外运行：画布烘焙在每帧开头，屏幕效果在整帧世界画完之后，
 * 所以开着光影包时 Iris 不会接管它们；世界里的面片用原版材质贴画布纹理，由光影包接管。
 * 某个着色器加载失败时只关闭对应的层，不影响游戏。
 */
@Mod.EventBusSubscriber(modid = SenDims.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class RitualShaders {
    private static final Logger LOGGER = LogUtils.getLogger();
    static ShaderInstance circle;
    static ShaderInstance runes;
    static ShaderInstance beam;
    static ShaderInstance screen;

    private RitualShaders() {
    }

    @SubscribeEvent
    public static void register(RegisterShadersEvent event) {
        circle = runes = beam = screen = null;
        load(event, "ritual_circle", shader -> circle = shader);
        load(event, "ritual_runes", shader -> runes = shader);
        load(event, "ritual_beam", shader -> beam = shader);
        load(event, "ritual_screen", shader -> screen = shader);
    }

    private static void load(RegisterShadersEvent event, String name, Consumer<ShaderInstance> sink) {
        try {
            event.registerShader(new ShaderInstance(event.getResourceProvider(), SenDims.prefix(name),
                    DefaultVertexFormat.POSITION), sink);
        } catch (Exception | LinkageError error) {
            LOGGER.warn("Ritual shader {} failed to load; that ritual layer is disabled", name, error);
        }
    }

    static boolean bakeReady() {
        return circle != null && runes != null && beam != null;
    }
}
