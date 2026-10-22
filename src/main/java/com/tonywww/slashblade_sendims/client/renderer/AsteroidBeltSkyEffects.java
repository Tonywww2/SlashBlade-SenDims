package com.tonywww.slashblade_sendims.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.tonywww.slashblade_sendims.SenDims;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import java.util.Random;

/** Four native-resolution pixel-art sky sectors behind the real asteroid terrain. */
@Mod.EventBusSubscriber(modid=SenDims.MOD_ID, value=Dist.CLIENT, bus=Mod.EventBusSubscriber.Bus.MOD)
public final class AsteroidBeltSkyEffects extends DimensionSpecialEffects {
    public static final ResourceLocation EFFECTS=new ResourceLocation(SenDims.MOD_ID,"asteroid_belt");
    private static final ResourceLocation[] PANORAMAS={
        new ResourceLocation(SenDims.MOD_ID,"textures/environment/asteroid_cosmos_0.png"),
        new ResourceLocation(SenDims.MOD_ID,"textures/environment/asteroid_cosmos_1.png"),
        new ResourceLocation(SenDims.MOD_ID,"textures/environment/asteroid_cosmos_2.png"),
        new ResourceLocation(SenDims.MOD_ID,"textures/environment/asteroid_cosmos_3.png")
    };
    private static final ResourceLocation DIMENSION=new ResourceLocation("sdbf","asteroid_belt");
    private static final int LONGITUDES=24, LATITUDES=48;
    // Dim the artwork independently of the terrain lightmap; keep stars legible.
    private static final float PANORAMA_BRIGHTNESS=.30f, POLAR_STAR_BRIGHTNESS=.55f;
    private VertexBuffer[] domes;
    private VertexBuffer polarStars;

    public AsteroidBeltSkyEffects(){
        // Retain the previous End-style lightmap; sky art does not change gameplay lighting.
        super(Float.NaN,false,SkyType.NONE,true,false);
    }
    @SubscribeEvent public static void register(RegisterDimensionSpecialEffectsEvent event){
        event.register(EFFECTS,new AsteroidBeltSkyEffects());
    }
    @Override public Vec3 getBrightnessDependentFogColor(Vec3 color,float daylight){return color.scale(.15);}
    @Override public boolean isFoggyAt(int x,int z){return false;}
    @Override public float[] getSunriseColor(float time,float partialTick){return null;}

    @Override public boolean renderSky(ClientLevel level,int ticks,float partialTick,PoseStack poses,Camera camera,Matrix4f projection,boolean foggy,Runnable setupFog){
        if(!level.dimension().location().equals(DIMENSION))return false;
        setupFog.run();
        var fluid=camera.getFluidInCamera();
        if(foggy||fluid==FogType.LAVA||fluid==FogType.POWDER_SNOW)return true;
        if(camera.getEntity() instanceof LivingEntity entity&&(entity.hasEffect(MobEffects.BLINDNESS)||entity.hasEffect(MobEffects.DARKNESS)))return true;
        if(domes==null)createDome();
        RenderSystem.disableBlend();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShaderColor(1,1,1,1);
        try{
            // Texture reloads use the same location, while immutable geometry stays on the GPU.
            for(int sector=0;sector<PANORAMAS.length;sector++){
                var texture=Minecraft.getInstance().getTextureManager().getTexture(PANORAMAS[sector]);
                texture.setFilter(false,false);
                RenderSystem.setShaderTexture(0,PANORAMAS[sector]);
                domes[sector].bind();
                domes[sector].drawWithShader(poses.last().pose(),projection,GameRenderer.getPositionTexColorShader());
            }
            polarStars.bind();
            polarStars.drawWithShader(poses.last().pose(),projection,GameRenderer.getPositionColorShader());
        }finally{
            VertexBuffer.unbind();
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            RenderSystem.setShaderColor(1,1,1,1);
        }
        return true;
    }
    private void createDome(){
        var builder=Tesselator.getInstance().getBuilder();
        domes=new VertexBuffer[PANORAMAS.length];
        for(int sector=0;sector<domes.length;sector++){
            builder.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_TEX_COLOR);
            for(int y=0;y<LATITUDES;y++)for(int x=0;x<LONGITUDES;x++){
                vertex(builder,sector,x,y);vertex(builder,sector,x+1,y);vertex(builder,sector,x+1,y+1);vertex(builder,sector,x,y+1);
            }
            domes[sector]=new VertexBuffer(VertexBuffer.Usage.STATIC);
            domes[sector].bind();domes[sector].upload(builder.end());VertexBuffer.unbind();
        }
        createPolarStars(builder);
    }
    private void createPolarStars(BufferBuilder builder){
        // Geometric square stars have no spherical UV singularity. They only
        // occupy the quiet polar caps of the generated artwork, and never tick.
        builder.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR);
        Random random=new Random(0xA57E201DL);
        for(int i=0;i<640;i++){
            double y=random.nextDouble()*2-1,angle=random.nextDouble()*Math.PI*2;
            double radial=Math.sqrt(1-y*y),x=radial*Math.cos(angle),z=radial*Math.sin(angle);
            if(Math.abs(y)<.55)continue;
            double rx=z/radial,rz=-x/radial,ux=y*rz,uy=z*rx-x*rz,uz=-y*rx;
            double size=.065+random.nextDouble()*.09;
            float brightness=POLAR_STAR_BRIGHTNESS*(float)((.12+random.nextDouble()*.62)*Math.min(1,(Math.abs(y)-.55)/.30));
            for(int v=0;v<4;v++){
                int side=(v==0||v==3)?-1:1,up=v<2?-1:1;
                builder.vertex(x*95+size*(side*rx+up*ux),y*95+size*up*uy,z*95+size*(side*rz+up*uz))
                    .color(brightness*.86f,brightness*.93f,brightness,1).endVertex();
            }
        }
        polarStars=new VertexBuffer(VertexBuffer.Usage.STATIC);
        polarStars.bind();polarStars.upload(builder.end());VertexBuffer.unbind();
    }
    private static void vertex(BufferBuilder builder,int sector,int x,int y){
        float u=(float)x/LONGITUDES,v=(float)y/LATITUDES;
        double longitude=(sector+u)*Math.PI*.5-Math.PI*.25,latitude=(.5-v)*Math.PI;
        float horizontal=(float)(100*Math.cos(latitude));
        // Quiet dark edges converge to black at the wrap and poles. This removes
        // discontinuities without resampling or blurring the generated pixel art.
        float seam=Math.min(1,Math.min(u,1-u)*96);
        // An equirectangular image compresses star pixels near the poles. Fade the
        // sparsely painted polar caps before they turn into radial streaks.
        float polarDistance=Math.min(v,1-v);
        float pole=Math.max(0,Math.min(1,(polarDistance-.16f)/.16f));
        pole=pole*pole*(3-2*pole);
        float shade=PANORAMA_BRIGHTNESS*seam*pole;
        builder.vertex(horizontal*Math.sin(longitude),100*Math.sin(latitude),horizontal*Math.cos(longitude))
            .uv(u,v).color(shade,shade,shade,1).endVertex();
    }
}
