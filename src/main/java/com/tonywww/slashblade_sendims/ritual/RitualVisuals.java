package com.tonywww.slashblade_sendims.ritual;

import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.network.RitualVisualPacket;
import com.tonywww.slashblade_sendims.network.RitualVisualPacket.Kind;
import com.tonywww.slashblade_sendims.network.RitualVisualPacket.Stage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;

import java.util.Locale;

/**
 * 仪式着色器特效的服务端接口，供整合包 KubeJS 仪式脚本调用（kubejs/server_scripts/custom_spawn/ritual_fx.js）。
 * <p>
 * 全部是静态方法，参数只用基本类型和字符串，并且不重载：KubeJS 对重载方法的解析不可靠。
 * 每个方法只发一个阶段包，画面在客户端生成；返回 false 表示没有发出，脚本据此退回原版表现。
 * 颜色用主题里的尘埃色字符串（"r g b"，0–1 浮点）。
 */
public final class RitualVisuals {
    /** 阶段包的接收范围。 */
    public static final double RANGE = 96.0D;

    private RitualVisuals() {
    }

    /** 蓄能开始（基岩破坏者为每一下敲击）。kind：summon、portal、breach。param 见 {@link RitualVisualPacket}。 */
    public static boolean charge(Level level, int x, int y, int z, String kind, String style,
            String colorA, String colorB, int ticks, double param) {
        if (!(level instanceof ServerLevel server)) return false;
        return broadcast(server, x + 0.5D, y + 0.5D, z + 0.5D, new RitualVisualPacket(server.dimension().location(),
                BlockPos.asLong(x, y, z), Stage.CHARGE, kind(kind), style(style), color(colorA), color(colorB),
                x, y, z, Mth.clamp(ticks, 0, RitualVisualPacket.MAX_DURATION), -1, (float) param));
    }

    /** 蓄能被打断。 */
    public static boolean interrupt(Level level, int x, int y, int z) {
        if (!(level instanceof ServerLevel server)) return false;
        return broadcast(server, x + 0.5D, y + 0.5D, z + 0.5D, new RitualVisualPacket(server.dimension().location(),
                BlockPos.asLong(x, y, z), Stage.INTERRUPT, Kind.SUMMON, "", 0, 0, x, y, z, 0, -1, Float.NaN));
    }

    /** 释放：召唤成功、发起传送或基岩碎裂。travelerId 为传送者实体 ID，没有时传 -1。 */
    public static boolean release(Level level, int x, int y, int z, String kind, String style,
            String colorA, String colorB, int travelerId) {
        if (!(level instanceof ServerLevel server)) return false;
        return broadcast(server, x + 0.5D, y + 0.5D, z + 0.5D, new RitualVisualPacket(server.dimension().location(),
                BlockPos.asLong(x, y, z), Stage.RELEASE, kind(kind), style(style), color(colorA), color(colorB),
                x, y, z, 0, travelerId, Float.NaN));
    }

    /** 传送前的屏幕旋涡，只发给传送者。kind 为 breach 时是向下坠落的旋涡。 */
    public static boolean warp(ServerPlayer player, String kind, String style, String colorA, String colorB, int ticks) {
        if (player == null || player.hasDisconnected()) return false;
        RitualVisualPacket packet = new RitualVisualPacket(player.level().dimension().location(), 0L, Stage.WARP,
                kind(kind), style(style), color(colorA), color(colorB), player.getX(), player.getY(), player.getZ(),
                Mth.clamp(ticks, 1, RitualVisualPacket.MAX_DURATION), player.getId(), Float.NaN);
        SenDims.NETWORK.send(PacketDistributor.PLAYER.with(() -> player), packet);
        return true;
    }

    /** 抵达：在抵达点（传送者脚下）画抵达符印；travelerId 对应的本地玩家还会看到闪光淡出。 */
    public static boolean arrival(Level level, double x, double y, double z, String kind, String style,
            String colorA, String colorB, int travelerId) {
        if (!(level instanceof ServerLevel server)) return false;
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) return false;
        return broadcast(server, x, y, z, new RitualVisualPacket(server.dimension().location(),
                BlockPos.containing(x, y, z).asLong(), Stage.ARRIVAL, kind(kind), style(style),
                color(colorA), color(colorB), x, y, z, 0, travelerId, Float.NaN));
    }

    private static boolean broadcast(ServerLevel level, double x, double y, double z, RitualVisualPacket packet) {
        if (!packet.valid()) return false;
        double range = RANGE * RANGE;
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(x, y, z) <= range) {
                SenDims.NETWORK.send(PacketDistributor.PLAYER.with(() -> player), packet);
            }
        }
        return true;
    }

    private static Kind kind(String value) {
        if (value == null) return Kind.SUMMON;
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "portal" -> Kind.PORTAL;
            case "breach" -> Kind.BREACH;
            default -> Kind.SUMMON;
        };
    }

    private static String style(String value) {
        if (value == null) return "default";
        String trimmed = value.trim();
        return trimmed.length() > RitualVisualPacket.MAX_STYLE_LENGTH
                ? trimmed.substring(0, RitualVisualPacket.MAX_STYLE_LENGTH) : trimmed;
    }

    /** "r g b"（0–1）→ 0xRRGGBB；解析失败时为白色。 */
    static int color(String value) {
        if (value == null) return 0xFFFFFF;
        String[] parts = value.trim().split("\\s+");
        if (parts.length < 3) return 0xFFFFFF;
        int rgb = 0;
        try {
            for (int i = 0; i < 3; i++) {
                rgb = rgb << 8 | Mth.clamp(Math.round(Float.parseFloat(parts[i]) * 255.0F), 0, 255);
            }
        } catch (NumberFormatException error) {
            return 0xFFFFFF;
        }
        return rgb;
    }
}
