package com.tonywww.slashblade_sendims.client.zenith;

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
 * 天顶·裂空的自写着色器。它们只在世界绘制之外运行（刀身画布在每帧开头烘焙，真·次元斩屏幕层在 renderLevel 之后），
 * 开着光影包时 Iris 不会接管。某个着色器加载失败时只关掉对应的部分。
 */
@Mod.EventBusSubscriber(modid = SenDims.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ZenithRiftShaders {
    private static final Logger LOGGER = LogUtils.getLogger();
    /** 真·次元斩屏幕层。 */
    static ShaderInstance screen;
    /** 天顶·裂空刀身、刀镡、刀鞘的发光画布。 */
    static ShaderInstance blade;

    private ZenithRiftShaders() {
    }

    @SubscribeEvent
    public static void register(RegisterShadersEvent event) {
        screen = blade = null;
        load(event, "zenith_rift_screen", shader -> screen = shader);
        load(event, "zenith_rift_blade", shader -> {
            blade = shader;
            ZenithRiftBlade.shadersReloaded();
        });
    }

    private static void load(RegisterShadersEvent event, String name, Consumer<ShaderInstance> sink) {
        try {
            event.registerShader(new ShaderInstance(event.getResourceProvider(), SenDims.prefix(name),
                    DefaultVertexFormat.POSITION), sink);
        } catch (Exception | LinkageError error) {
            LOGGER.warn("Zenith Rift shader {} failed to load; that layer is disabled", name, error);
        }
    }
}
