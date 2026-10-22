package com.tonywww.slashblade_sendims.client;

import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.network.SukunaVisualPacket;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.UUID;

@Mod.EventBusSubscriber(modid=SenDims.MOD_ID,value=Dist.CLIENT)
public final class SukunaVisualClient {
    private record Visual(SukunaVisualPacket packet,double began) {}
    private static final LinkedHashMap<UUID,Visual> DOMAINS=new LinkedHashMap<>();
    private static final ArrayDeque<Visual> CUTS=new ArrayDeque<>();
    private static final MultiBufferSource.BufferSource BUFFERS=MultiBufferSource.immediate(
            new com.mojang.blaze3d.vertex.BufferBuilder(262144));
    private static final ResourceLocation TEXTURE=SenDims.prefix("textures/effect/holy_sword_light.png");
    private static final RenderType BODY=RenderType.entityTranslucent(TEXTURE);
    private static final RenderType LIGHT=RenderType.entityTranslucentEmissive(TEXTURE,false);
    private static Object world;
    private SukunaVisualClient() {}
    private static void world() {
        var level=Minecraft.getInstance().level;
        if(level!=world) {world=level;DOMAINS.clear();CUTS.clear();}
    }
    public static void accept(SukunaVisualPacket p) {
        world();var level=Minecraft.getInstance().level;
        if(level==null || !p.valid() || !level.dimension().location().equals(p.dimension()))return;
        double now=level.getGameTime();
        if(p.stage()==SukunaVisualPacket.Stage.END) {
            var old=DOMAINS.get(p.owner());
            if(old!=null && old.packet.id()==p.id())DOMAINS.remove(p.owner());
        } else if(p.stage()==SukunaVisualPacket.Stage.DOMAIN) {
            DOMAINS.put(p.owner(),new Visual(p,now-p.age()));
            while(DOMAINS.size()>8)DOMAINS.remove(DOMAINS.keySet().iterator().next());
        } else {CUTS.addLast(new Visual(p,now));while(CUTS.size()>128)CUTS.removeFirst();}
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e) {
        if(e.phase!=TickEvent.Phase.END)return;world();
        var level=Minecraft.getInstance().level;if(level==null)return;
        double now=level.getGameTime();
        DOMAINS.values().removeIf(v->now-v.began>=150);
        CUTS.removeIf(v->now-v.began>=(v.packet.stage()==SukunaVisualPacket.Stage.WHEEL?16:6));
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent e) {
        if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES)return;world();
        var mc=Minecraft.getInstance();if(mc.level==null || DOMAINS.isEmpty() && CUTS.isEmpty())return;
        Vec3 camera=e.getCamera().getPosition();double now=mc.level.getGameTime()+e.getPartialTick();
        for(Visual v:DOMAINS.values())draw(v,now,camera,e);
        for(Visual v:CUTS)draw(v,now,camera,e);
        BUFFERS.endBatch(BODY);BUFFERS.endBatch(LIGHT);
    }
    private static void draw(Visual v,double now,Vec3 camera,RenderLevelStageEvent e) {
        var p=v.packet;double dist=p.origin().distanceToSqr(camera);
        if(dist>128*128 || !e.getFrustum().isVisible(new AABB(p.origin(),p.origin()).inflate(21,12,21)))return;
        float age=(float)(now-v.began);
        SukunaGeometry.draw(p,age,dist<48*48,(a,b,c,d,rgb,alpha,glow)->{
            if(alpha<=.005F)return;
            var buffer=BUFFERS.getBuffer(glow?LIGHT:BODY);
            Vec3 normal=b.subtract(a).cross(c.subtract(a)).normalize();
            vertex(buffer,e.getPoseStack().last(),a.subtract(camera),normal,rgb,alpha,0,0);
            vertex(buffer,e.getPoseStack().last(),b.subtract(camera),normal,rgb,alpha,1,0);
            vertex(buffer,e.getPoseStack().last(),c.subtract(camera),normal,rgb,alpha,1,1);
            vertex(buffer,e.getPoseStack().last(),d.subtract(camera),normal,rgb,alpha,0,1);
        });
    }
    private static void vertex(VertexConsumer b,PoseStack.Pose pose,Vec3 p,Vec3 n,int rgb,float alpha,float u,float v) {
        b.vertex(pose.pose(),(float)p.x,(float)p.y,(float)p.z)
                .color((rgb>>16)&255,(rgb>>8)&255,rgb&255,(int)(Math.max(0,Math.min(1,alpha))*255))
                .uv(u,v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(pose.normal(),(float)n.x,(float)n.y,(float)n.z).endVertex();
    }
}
