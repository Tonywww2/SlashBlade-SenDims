package com.tonywww.slashblade_sendims.network;

import com.tonywww.slashblade_sendims.client.ritual.RitualVisualClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 仪式特效的阶段包：每个阶段一个，画面全在客户端生成（见 docs/ritual_visuals.md）。
 *
 * @param id       同一处仪式各阶段的关联键（核心方块坐标 asLong；抵达用抵达点方块坐标）
 * @param x        核心方块坐标或抵达点（CHARGE/INTERRUPT/RELEASE 为方块坐标，ARRIVAL 为玩家脚下位置）
 * @param duration 蓄能或旋涡时长（tick）
 * @param entity   发起者实体 ID，没有时为 -1
 * @param param    按 kind 解释：地图传送为罗盘方位（度，世界 XZ 平面上 atan2(dz, dx)），基岩破坏者为第几下；没有时为 NaN
 */
public record RitualVisualPacket(ResourceLocation dimension, long id, Stage stage, Kind kind, String style,
        int colorA, int colorB, double x, double y, double z, int duration, int entity, float param) {
    public enum Stage { CHARGE, INTERRUPT, RELEASE, WARP, ARRIVAL }

    public enum Kind { SUMMON, PORTAL, BREACH }

    public static final int MAX_STYLE_LENGTH = 32;
    public static final int MAX_DURATION = 1200;

    public boolean valid() {
        return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z)
                && Math.abs(x) < 3.0E7 && Math.abs(z) < 3.0E7 && Math.abs(y) < 4096
                && duration >= 0 && duration <= MAX_DURATION
                && style != null && style.length() <= MAX_STYLE_LENGTH;
    }

    public static void encode(RitualVisualPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.dimension);
        buffer.writeLong(packet.id);
        buffer.writeEnum(packet.stage);
        buffer.writeEnum(packet.kind);
        buffer.writeUtf(packet.style, MAX_STYLE_LENGTH);
        buffer.writeInt(packet.colorA);
        buffer.writeInt(packet.colorB);
        buffer.writeDouble(packet.x);
        buffer.writeDouble(packet.y);
        buffer.writeDouble(packet.z);
        buffer.writeVarInt(packet.duration);
        buffer.writeVarInt(packet.entity);
        buffer.writeFloat(packet.param);
    }

    public static RitualVisualPacket decode(FriendlyByteBuf buffer) {
        return new RitualVisualPacket(buffer.readResourceLocation(), buffer.readLong(),
                buffer.readEnum(Stage.class), buffer.readEnum(Kind.class), buffer.readUtf(MAX_STYLE_LENGTH),
                buffer.readInt(), buffer.readInt(),
                buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                buffer.readVarInt(), buffer.readVarInt(), buffer.readFloat());
    }

    public static void handle(RitualVisualPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        if (packet.valid()) {
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> RitualVisualClient.accept(packet)));
        }
        context.setPacketHandled(true);
    }
}
