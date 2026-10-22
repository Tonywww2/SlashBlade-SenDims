package com.tonywww.slashblade_sendims.client;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.yakuman.YakumanVisualPacket;
import com.tonywww.slashblade_sendims.yakuman.YakumanVisualPacket.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.phys.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Quaternionf;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Cached meshes/materials, batched draw calls and a bounded visual-only budget. Uses vanilla shaders. */
@Mod.EventBusSubscriber(modid=SenDims.MOD_ID,value=Dist.CLIENT)
public final class YakumanVisualClient {
    /** Shared opacity for all yakuman effect meshes; lifetime fades multiply this value. */
    public static final float EFFECT_OPACITY=.65F;
    private record Lease(Visual v,double began,double born,Vec3 correction) {
        Vec3 point(double now) {
            if(!moving(v))return v.pos();
            double age=Math.max(0,now-began);
            return v.pos().add(v.motion().scale(age))
                    .add(correction.scale(Math.max(0,1-age/3D)));
        }
    }
    private static boolean moving(Visual v) {
        return v.shape()==Shape.FALL_STACK || v.life()==6&&switch(v.shape()) {
            case NEEDLE,PHANTOM,TILE,STACK,DRAGON,SHADOW,GOLD,SLASH,FACING_TILE -> true;
            default -> false;
        };
    }
    private static final LinkedHashMap<Long,Lease> VISUALS=new LinkedHashMap<>();
    private static final Map<String,float[]> MESHES=new HashMap<>();
    private static final float[][] TILES=new float[35][];
    private static final ResourceLocation ATLAS=SenDims.prefix("model/yakuman/tiles.png");
    private static final ResourceLocation EFFECT=SenDims.prefix("model/yakuman/effects.png");
    private static final ResourceLocation SLASH_TEX=ResourceLocation.fromNamespaceAndPath("slashblade","model/util/slash.png");
    private static final RenderType TILE_TYPE=RenderType.entityTranslucent(ATLAS);
    private static final RenderType GLOW=RenderType.entityTranslucentEmissive(EFFECT,false);
    private static final RenderType DARK=RenderType.entityTranslucent(EFFECT);
    private static final RenderType SLASH_TYPE=RenderType.entityTranslucentEmissive(SLASH_TEX,false);
    private static final MultiBufferSource.BufferSource BUFFERS=MultiBufferSource.immediate(new BufferBuilder(262144));
    private static Object world;
    private static void world() {
        var level=Minecraft.getInstance().level;
        if(world!=level){world=level;VISUALS.clear();}
    }
    public static void accept(YakumanVisualPacket packet) {
        world();var level=Minecraft.getInstance().level;
        if(level==null||!level.dimension().location().equals(packet.dimension()))return;
        double now=level.getGameTime()+Minecraft.getInstance().getFrameTime();
        for(var v:packet.visuals()) {
            if(v.shape()==Shape.END)VISUALS.remove(v.id());
            else {
                var previous=VISUALS.get(v.id());
                boolean continuous=moving(v)&&previous!=null&&moving(previous.v);
                Vec3 correction=continuous?previous.point(now).subtract(v.pos()):Vec3.ZERO;
                VISUALS.put(v.id(),new Lease(v,now,continuous?previous.born:now,correction));
            }
        }
        // Dropping distant/old render descriptors never changes server-side damage.
        while(VISUALS.size()>768)VISUALS.remove(VISUALS.keySet().iterator().next());
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e) {
        if(e.phase!=TickEvent.Phase.END)return;world();
        var level=Minecraft.getInstance().level;
        if(level!=null)VISUALS.values().removeIf(l->level.getGameTime()-l.began>=l.v.life());
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent e) {
        if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES)return;world();
        var mc=Minecraft.getInstance();if(mc.level==null||VISUALS.isEmpty())return;
        double now=mc.level.getGameTime()+e.getPartialTick();Vec3 camera=e.getCamera().getPosition();
        for(var lease:VISUALS.values()) {
            var v=lease.v;double age=now-lease.began;
            if(age<0||age>=v.life())continue;
            Vec3 point=lease.point(now);
            double distance=point.distanceToSqr(camera);
            if(distance>96*96||!e.getFrustum().isVisible(new AABB(point,point).inflate(v.scale()+6)))continue;
            draw(v,(float)(moving(v)?now-lease.born:age),(float)age,point,camera,e.getPoseStack(),distance<40*40);
        }
        BUFFERS.endBatch(TILE_TYPE);BUFFERS.endBatch(GLOW);BUFFERS.endBatch(DARK);BUFFERS.endBatch(SLASH_TYPE);
    }
    private static void draw(Visual v,float age,float snapshotAge,Vec3 point,Vec3 camera,PoseStack pose,boolean detail) {
        Vec3 motion=v.motion();
        double orbit=motion.x+age*.11+(v.tile()-27)*Math.PI*2/7;
        if(v.shape()==Shape.ORBIT)point=point.add(Math.cos(orbit)*1.8,.3,Math.sin(orbit)*1.8);
        pose.pushPose();pose.translate(point.x-camera.x,point.y-camera.y,point.z-camera.z);
        float alpha=EFFECT_OPACITY*(moving(v)&&v.life()==6?1:Math.min(1,(v.life()-snapshotAge)/3F));
        float scale=v.scale();
        if(v.shape()==Shape.RING) {
            ring(pose,scale*Math.min(1,age/10F),alpha,v.color(),detail?48:24);
        } else if(v.shape()==Shape.SLASH) {
            double yaw=Math.atan2(motion.x,motion.z);
            pose.mulPose(Axis.YP.rotation((float)yaw));pose.mulPose(Axis.ZP.rotationDegrees(12));
            pose.scale(scale/85,scale/85,scale/85);
            mesh("native_slash",pose,SLASH_TYPE,alpha,v.color(),0);
        } else {
            Vec3 facing=facing(v,point);
            if(v.shape()!=Shape.ORBIT && facing.lengthSqr()>.0001)pose.mulPose(new Quaternionf().rotationTo(0,0,1,(float)facing.x,(float)facing.y,(float)facing.z));
            pose.scale(scale,scale,scale);
            switch(v.shape()) {
                case TILE,ORBIT,FACING_TILE -> {
                    if(v.shape()==Shape.ORBIT)pose.mulPose(Axis.YP.rotation((float)(Math.PI/2-orbit)));
                    mesh("tile",pose,TILE_TYPE,alpha,0xffffff,v.tile());
                }
                case STACK,FALL_STACK -> {
                    for(int i=0;i<4;i++) {
                        pose.pushPose();pose.translate(0,0,i*(motion.y<0?-.54:.54));
                        mesh("tile",pose,TILE_TYPE,alpha,0xffffff,v.tile());pose.popPose();
                    }
                }
                case GATE -> mesh("gate",pose,GLOW,alpha,v.color(),0);
                case PHANTOM -> {
                    mesh("phantom",pose,GLOW,alpha,v.color(),0);
                    if(detail)ribbon(pose,0,2F,.1F,age,alpha,v.color(),false);
                }
                case NEEDLE,GOLD -> {
                    // The mesh tip is +X; the common flight frame and trail use +Z.
                    pose.pushPose();pose.mulPose(Axis.YP.rotationDegrees(-90));
                    pose.scale(2F,.7F,v.shape()==Shape.GOLD?.7F:.25F);
                    mesh("needle",pose,GLOW,alpha,v.color(),0);
                    pose.popPose();
                    if(detail)ribbon(pose,0,1.8F,.12F,age,alpha,v.color(),false);
                }
                case DRAGON -> dragon(pose,age,alpha,v.color(),detail);
                case SHADOW -> {
                    for(int i=0;i<(detail?3:1);i++)ribbon(pose,(i-1)*.07F,3F,.18F,age+i,alpha,0x08070c,true);
                }
                default -> {}
            }
        }
        pose.popPose();
    }
    private static Vec3 facing(Visual v,Vec3 point) {
        if(v.shape()==Shape.FACING_TILE) {
            // Shape-specific color field carries the caster id, never the observing camera.
            var caster=Minecraft.getInstance().level.getEntity(v.color());
            if(caster!=null)return caster.getEyePosition().subtract(point).normalize();
        }
        return v.motion().normalize();
    }
    private static void dragon(PoseStack pose,float age,float alpha,int rgb,boolean detail) {
        ribbon(pose,0,3.6F,.32F,age,alpha,rgb,false);
        if(detail) {
            for(int sign:new int[]{-1,1}) {
                pose.pushPose();pose.translate(sign*.16,.07,.6);
                pose.mulPose(Axis.ZP.rotationDegrees(sign*30));pose.mulPose(Axis.XP.rotationDegrees(-20));
                ribbon(pose,0,.9F,.12F,age,alpha,rgb,false);pose.popPose();
                pose.pushPose();pose.translate(sign*.24,-.04,.7);pose.mulPose(Axis.YP.rotationDegrees(sign*40));
                ribbon(pose,0,1.1F,.05F,age+2,alpha,rgb,false);pose.popPose();
            }
        }
    }
    private static void ribbon(PoseStack pose,float offset,float length,float width,float age,float alpha,int rgb,boolean dark) {
        var b=BUFFERS.getBuffer(dark?DARK:GLOW);int segments=12;
        for(int i=0;i<segments;i++) {
            float a=(float)i/segments,d=(float)(i+1)/segments;
            float za=-a*length,zd=-d*length;
            float xa=(float)Math.sin(a*7-age*.5)*.08F+offset,xd=(float)Math.sin(d*7-age*.5)*.08F+offset;
            float wa=width*(1-a),wd=width*(1-d);
            // Upper-right atlas cell is a feathered beam; black ribbons use alpha blending.
            beamQuad(b,pose,xa-wa,0,za,xa+wa,0,za,xd+wd,0,zd,xd-wd,0,zd,rgb,alpha,.5F+a*.5F,.5F+d*.5F);
            beamQuad(b,pose,0,xa-wa,za,0,xa+wa,za,0,xd+wd,zd,0,xd-wd,zd,rgb,alpha,.5F+a*.5F,.5F+d*.5F);
        }
    }
    private static void ring(PoseStack pose,float radius,float alpha,int rgb,int segments) {
        var b=BUFFERS.getBuffer(GLOW);
        for(int i=0;i<segments;i++) {
            double a=i*Math.PI*2/segments,d=(i+1)*Math.PI*2/segments;
            float inner=Math.max(0,radius-.22F),outer=radius+.22F;
            beamQuad(b,pose,(float)Math.cos(a)*inner,.1F,(float)Math.sin(a)*inner,
                    (float)Math.cos(a)*outer,.1F,(float)Math.sin(a)*outer,
                    (float)Math.cos(d)*outer,.1F,(float)Math.sin(d)*outer,
                    (float)Math.cos(d)*inner,.1F,(float)Math.sin(d)*inner,rgb,alpha,.65F,.85F);
        }
    }
    private static void beamQuad(VertexConsumer b,PoseStack pose,
                                 float ax,float ay,float az,float bx,float by,float bz,
                                 float cx,float cy,float cz,float dx,float dy,float dz,
                                 int rgb,float alpha,float u0,float u1) {
        vertex(b,pose,ax,ay,az,u0,.2F,0,1,0,alpha,rgb);
        vertex(b,pose,bx,by,bz,u0,.3F,0,1,0,alpha,rgb);
        vertex(b,pose,cx,cy,cz,u1,.3F,0,1,0,alpha,rgb);
        vertex(b,pose,dx,dy,dz,u1,.2F,0,1,0,alpha,rgb);
    }
    private static void mesh(String name,PoseStack pose,RenderType type,float alpha,int rgb,int tile) {
        float[] data=MESHES.computeIfAbsent(name,YakumanVisualClient::load);
        if(name.equals("tile")) {
            if(TILES[tile]==null) {
                var copy=data.clone();float du=tile%8*32/256F,dv=(tile/8)*48/256F;
                for(int i=0;i<copy.length;i+=8)if(copy[i+3]<32/256F&&copy[i+4]<48/256F) {
                    copy[i+3]+=du;copy[i+4]+=dv;
                }
                TILES[tile]=copy;
            }
            data=TILES[tile];
        }
        var b=BUFFERS.getBuffer(type);
        for(int i=0;i<data.length;i+=24) {
            for(int j=0;j<4;j++) {
                int k=i+Math.min(j,2)*8;
                vertex(b,pose,data[k],data[k+1],data[k+2],data[k+3],data[k+4],data[k+5],data[k+6],data[k+7],alpha,rgb);
            }
        }
    }
    private static void vertex(VertexConsumer b,PoseStack pose,float x,float y,float z,float u,float v,float nx,float ny,float nz,float alpha,int rgb) {
        b.vertex(pose.last().pose(),x,y,z).color((rgb>>16)&255,(rgb>>8)&255,rgb&255,(int)(alpha*255))
                .uv(u,v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(pose.last().normal(),nx,ny,nz).endVertex();
    }
    private static float[] load(String name) {
        var id=name.equals("native_slash")?ResourceLocation.fromNamespaceAndPath("slashblade","model/util/slash.obj")
                :SenDims.prefix("model/yakuman/fx_"+name+".obj");
        try(var stream=Minecraft.getInstance().getResourceManager().open(id);
            var reader=new BufferedReader(new InputStreamReader(stream,StandardCharsets.UTF_8))) {
            List<float[]> positions=new ArrayList<>(),uvs=new ArrayList<>(),normals=new ArrayList<>();
            List<Float> out=new ArrayList<>();String line;boolean group=!name.equals("native_slash");
            while((line=reader.readLine())!=null) {
                String[] a=line.trim().split("\\s+");if(a.length==0)continue;
                switch(a[0]) {
                    case "g" -> group=!name.equals("native_slash")||a.length>1&&a[1].equals("base");
                    case "v" -> positions.add(new float[]{Float.parseFloat(a[1]),Float.parseFloat(a[2]),Float.parseFloat(a[3])});
                    case "vt" -> uvs.add(new float[]{Float.parseFloat(a[1]),1-Float.parseFloat(a[2])});
                    case "vn" -> normals.add(new float[]{Float.parseFloat(a[1]),Float.parseFloat(a[2]),Float.parseFloat(a[3])});
                    case "f" -> {
                        if(!group)continue;
                        for(int t=2;t<a.length-1;t++)for(int idx:new int[]{1,t,t+1}) {
                            String[] ref=a[idx].split("/");
                            float[] p=positions.get(index(ref[0],positions.size()));
                            float[] uv=ref.length>1&&!ref[1].isEmpty()?uvs.get(index(ref[1],uvs.size())):new float[]{0,0};
                            // Native slash UVs occupy an animation strip. Cache the middle native frame.
                            float textureV=uv[1]-(name.equals("native_slash")?.425F:0);
                            float[] n=ref.length>2?normals.get(index(ref[2],normals.size())):new float[]{0,1,0};
                            boolean orient=name.equals("needle")||name.equals("phantom");
                            float[] vertex={orient?-p[2]:p[0],p[1],orient?p[0]:p[2],uv[0],textureV,
                                    orient?-n[2]:n[0],n[1],orient?n[0]:n[2]};
                            for(float f:vertex)out.add(f);
                        }
                    }
                    default -> {}
                }
            }
            float[] result=new float[out.size()];for(int i=0;i<result.length;i++)result[i]=out.get(i);
            return result;
        } catch(Exception error) {SenDims.LOGGER.error("Unable to load yakuman effect mesh {}",id,error);return new float[0];}
    }
    private static int index(String raw,int size){int n=Integer.parseInt(raw);return n<0?size+n:n-1;}
    @Mod.EventBusSubscriber(modid=SenDims.MOD_ID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static final class Reload {
        @SubscribeEvent public static void register(RegisterClientReloadListenersEvent e) {
            e.registerReloadListener((ResourceManagerReloadListener)manager->{
                MESHES.clear();Arrays.fill(TILES,null);VISUALS.clear();
            });
        }
    }
}
