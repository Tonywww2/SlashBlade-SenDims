package com.tonywww.slashblade_sendims.network;

import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.client.zenith.ZenithRiftClient;
import com.tonywww.slashblade_sendims.sa.ZenithRiftArts;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/**
 * 真·次元斩的阶段包：START 时客户端按固定时间表播放整段画面（斩痕、定格、碎裂），
 * END 只在施放提前结束时发送，让客户端提前收尾。CUT 是 SE「裂空」的一次空间斩，只在世界里画几道斩痕。
 *
 * @param id     这次施放的编号
 * @param caster 施放者实体 ID
 * @param radius 水平半径（格）
 * @param seed   画面随机种子，所有观看者相同
 */
public record ZenithRiftPacket(ResourceLocation dimension, long id, Stage stage, int caster,
        double x, double y, double z, float radius, long seed) {
    public enum Stage { START, END, CUT }

    public boolean valid() {
        return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z)
                && Math.abs(x) < 3.0E7 && Math.abs(z) < 3.0E7 && Math.abs(y) < 4096
                && Float.isFinite(radius) && radius > 0.0F && radius <= 64.0F;
    }

    public static void encode(ZenithRiftPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.dimension);
        buffer.writeLong(packet.id);
        buffer.writeEnum(packet.stage);
        buffer.writeVarInt(packet.caster);
        buffer.writeDouble(packet.x);
        buffer.writeDouble(packet.y);
        buffer.writeDouble(packet.z);
        buffer.writeFloat(packet.radius);
        buffer.writeLong(packet.seed);
    }

    public static ZenithRiftPacket decode(FriendlyByteBuf buffer) {
        return new ZenithRiftPacket(buffer.readResourceLocation(), buffer.readLong(), buffer.readEnum(Stage.class),
                buffer.readVarInt(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                buffer.readFloat(), buffer.readLong());
    }

    public static void handle(ZenithRiftPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        if (packet.valid()) {
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> ZenithRiftClient.accept(packet)));
        }
        context.setPacketHandled(true);
    }

    /** 发给同一维度、距起点 64 格内的玩家。 */
    public static void send(ServerLevel level, ZenithRiftPacket packet) {
        double range = ZenithRiftArts.VIEW_DISTANCE * ZenithRiftArts.VIEW_DISTANCE;
        for (ServerPlayer observer : level.players()) {
            if (observer.distanceToSqr(packet.x, packet.y, packet.z) <= range) {
                SenDims.NETWORK.send(PacketDistributor.PLAYER.with(() -> observer), packet);
            }
        }
    }
}
