package com.tonywww.slashblade_sendims.performance;

import codechicken.lib.render.CCModel;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.buffer.TransformingVertexConsumer;
import codechicken.lib.vec.Matrix4;
import codechicken.lib.vec.Rotation;
import codechicken.lib.vec.Transformation;
import codechicken.lib.vec.Translation;
import codechicken.lib.vec.Vector3;
import codechicken.lib.vec.Vertex5;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tonywww.slashblade_sendims.client.performance.ClientPerformanceOptions;
import com.tonywww.slashblade_sendims.client.performance.GrinderSwordGeometry;
import com.tonywww.slashblade_sendims.client.performance.ObjFaceEmitter;
import com.tonywww.slashblade_sendims.client.performance.RenderResourceEpoch;
import mods.flammpfeil.slashblade.client.renderer.model.obj.Face;
import mods.flammpfeil.slashblade.client.renderer.model.obj.TextureCoordinate;
import mods.flammpfeil.slashblade.client.renderer.model.obj.Vertex;
import org.joml.Matrix4f;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** Calls both upstream and production optimized emitters. No window, world, server or GL context. */
public final class ClientRenderingParitySelfTest {
    private static int checks;

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static final class Capture implements VertexConsumer {
        final List<double[]> vertices = new ArrayList<>();
        final double[] current = new double[14];
        public VertexConsumer vertex(double x, double y, double z) { current[0]=x; current[1]=y; current[2]=z; return this; }
        public VertexConsumer color(int r, int g, int b, int a) { current[3]=r; current[4]=g; current[5]=b; current[6]=a; return this; }
        public VertexConsumer uv(float u, float v) { current[7]=u; current[8]=v; return this; }
        public VertexConsumer overlayCoords(int u, int v) { current[9]=u | v << 16; return this; }
        public VertexConsumer uv2(int u, int v) { current[10]=u | v << 16; return this; }
        public VertexConsumer normal(float x, float y, float z) { current[11]=x; current[12]=y; current[13]=z; return this; }
        public void endVertex() { vertices.add(current.clone()); }
        public void defaultColor(int r, int g, int b, int a) { throw new AssertionError("default color changed"); }
        public void unsetDefaultColor() { throw new AssertionError("default color changed"); }
    }

    private static void same(Capture upstream, Capture optimized, double epsilon, String path) {
        check(upstream.vertices.size() == optimized.vertices.size(), path+" vertex count");
        for (int i=0; i<upstream.vertices.size(); i++) {
            for (int j=0; j<14; j++) {
                double expected=upstream.vertices.get(i)[j], actual=optimized.vertices.get(i)[j];
                check(Double.doubleToLongBits(expected)==Double.doubleToLongBits(actual)
                                || Math.abs(expected-actual)<=epsilon,
                        path+" vertex="+i+" component="+j+" expected="+expected+" actual="+actual);
            }
        }
    }

    private static void caches() {
        TextResultCache text = new TextResultCache();
        check(text.get("title", 40)==null, "trim miss");
        text.put("title", 40, "tit...");
        check("tit...".equals(text.get(new String("title"),40)), "equal immutable text");
        check(text.get("title",41)==null, "width change");
        text.put("Aa",50,"A"); text.put("BB",50,"B");
        check(text.get("Aa",50)==null && "B".equals(text.get("BB",50)), "hash collision evicts without stale result");
        text.put("x".repeat(513),40,"x");
        check(text.get("x".repeat(513),40)==null, "large text not retained");
        text.clear(); check(text.get("title",40)==null, "resource/logout clear");
        TextureFilterMemo filter = new TextureFilterMemo();
        check(filter.needsWrite(7,TextureFilterMemo.MIN_FILTER,9728), "initial filter write");
        filter.record(7,TextureFilterMemo.MIN_FILTER,9728);
        filter.record(7,TextureFilterMemo.MAG_FILTER,9728);
        check(!filter.needsWrite(7,TextureFilterMemo.MIN_FILTER,9728), "same min reused");
        check(!filter.needsWrite(7,TextureFilterMemo.MAG_FILTER,9728), "same mag reused");
        check(filter.needsWrite(8,TextureFilterMemo.MIN_FILTER,9728), "new texture");
        check(filter.needsWrite(7,TextureFilterMemo.MAG_FILTER,9729), "zoom changes filter");
        filter.clear(); check(filter.needsWrite(7,TextureFilterMemo.MIN_FILTER,9728), "foreign write invalidation");
        FixedBufferViewCache<Object> views = new FixedBufferViewCache<>();
        Object buffer = new Object(); Set<Object> one = views.view(buffer);
        check(one == views.view(buffer) && one.contains(buffer), "fixed view reuse");
        try { one.clear(); throw new AssertionError("mutable singleton"); } catch (UnsupportedOperationException expected) { checks++; }
        for(int i=0; i<1000; i++) views.view(new Object());
        check(views.size()==64,"bounded views");
        views.clear(); check(views.size()==0 && one!=views.view(buffer),"close releases views");
    }

    private static Face face() {
        Face result=new Face();
        result.vertices=new Vertex[]{new Vertex(0,0,0),new Vertex(1,0,0),new Vertex(1,1,.2F),new Vertex(0,1,.2F)};
        result.textureCoordinates=new TextureCoordinate[]{new TextureCoordinate(0,0),new TextureCoordinate(1,0),new TextureCoordinate(1,1),new TextureCoordinate(0,1)};
        return result;
    }

    private static void obj() {
        ObjFaceEmitter emitter=new ObjFaceEmitter(); Random random=new Random(9321);
        for(int i=0; i<120; i++) {
            Face face=face();
            if(i%3==0) face.textureCoordinates=null;
            if(i%4==0) face.vertexNormals=new Vertex[]{new Vertex(0,0,1),new Vertex(0,.2F,1),new Vertex(.2F,0,1),new Vertex(0,0,1)};
            Face.setUvOperator(i%2==0?-1F:1F,.75F,.25F,-.125F);
            Matrix4f matrix=new Matrix4f().translate(random.nextFloat(),random.nextFloat(),random.nextFloat())
                    .rotateXYZ(random.nextFloat(),random.nextFloat(),random.nextFloat()).scale(.5F,1.5F,2F);
            Capture upstream=new Capture(), optimized=new Capture(); int color=random.nextInt();
            face.addFaceForRender(upstream,.0005F,matrix,15728688,color);
            check(emitter.render(face,optimized,.0005F,matrix,15728688,color),"OBJ optimized path");
            same(upstream,optimized,0,"OBJ");
            face.vertices[0].x += .3F;
            upstream=new Capture(); optimized=new Capture();
            face.addFaceForRender(upstream,0,matrix,32,color);
            check(emitter.render(face,optimized,0,matrix,32,color),"live OBJ vertices");
            same(upstream,optimized,0,"OBJ live");
        }
        Face custom=face();
        Face.setAlphaOverride((position,alpha)->alpha/2);
        check(!emitter.render(custom,new Capture(),0,new Matrix4f(),0,0),"custom alpha callback falls back");
        Face.resetAlphaOverride(); Face.resetUvOperator();
    }

    private static CCModel model() {
        CCModel model=CCModel.quadModel(4);
        model.verts[0]=new Vertex5(0,0,0,0,0);
        model.verts[1]=new Vertex5(1,0,0,1,0);
        model.verts[2]=new Vertex5(1,1,0,1,1);
        model.verts[3]=new Vertex5(0,1,0,0,1);
        return model.computeNormals();
    }

    private static CCRenderState state(Capture capture) {
        CCRenderState state=CCRenderState.instance(); state.reset();
        state.brightness=15728688; state.overlay=655360;
        Matrix4 view=new Matrix4().translate(.2,.3,.4).apply(new Rotation(.31,0,1,0));
        state.bind(new TransformingVertexConsumer(capture,view),DefaultVertexFormat.NEW_ENTITY);
        return state;
    }

    private static Transformation pose(double angle,double side,Vector3 tile,Vector3 target,double animation) {
        animation*=2; double yoyo=animation>1?2-animation:animation;
        side*=1-yoyo;
        Transformation rotation=new Rotation(yoyo*Math.PI*-2.8,1,0,0).at(new Vector3(0,-.2,0));
        Vector3 hand=new Vector3(.4975+side,.5,.125+yoyo*.375);
        hand.apply(new Rotation(angle,0,1,0).at(new Vector3(.5,0,.5)));
        var transforms=rotation.with(new Translation(hand));
        if(target==null) return transforms.with(new Rotation(angle,0,1,0).at(hand));
        Vector3 vector=target.copy().subtract(tile);
        return transforms.with(new Rotation(Math.atan2(vector.x,vector.z)+Math.PI,0,1,0).at(hand))
                .with(new Translation(vector.multiply(yoyo)));
    }

    private static void grinder() {
        CCModel model=model(); GrinderSwordGeometry geometry=new GrinderSwordGeometry();
        double[] degrees={180,0,270,90}; Vector3 tile=new Vector3(7,310,-4);
        for(int direction=0;direction<4;direction++) for(double side:new double[]{.34,-.34}) {
            double angle=degrees[direction]*.017453292519943;
            Capture upstream=new Capture(), optimized=new Capture();
            model.render(state(upstream),pose(angle,side,tile,null,0));
            check(geometry.idle(model,state(optimized),direction,side,angle),"idle geometry path");
            same(upstream,optimized,1E-6,"idle grinder");
            upstream=new Capture();
            model.render(state(upstream),pose(angle,side,tile,null,1));
            same(upstream,optimized,1E-6,"completed animation idle grinder");
            for(int frame=0;frame<=10;frame++) {
                double animation=frame/10.0;
                Vector3 target=frame%2==0?null:new Vector3(9,311,-8);
                Transformation transform=pose(angle,side,tile,target,animation);
                upstream=new Capture(); optimized=new Capture();
                model.render(state(upstream),transform);
                check(geometry.animated(model,state(optimized),new Transformation[]{transform}),"animated geometry path");
                same(upstream,optimized,2E-6,"animated grinder");
            }
        }
        check(geometry.cachedBytes()<=ClientPerformanceOptions.grinderCacheBytes,"geometry hard budget");
        model.verts[0].vec.x+=.3; RenderResourceEpoch.invalidate();
        Capture upstream=new Capture(), optimized=new Capture();
        Transformation transform=pose(0,.34,tile,null,0);
        model.render(state(upstream),transform);
        check(geometry.idle(model,state(optimized),0,.34,0),"resource invalidation rebuild");
        same(upstream,optimized,1E-6,"reloaded geometry");
        ClientPerformanceOptions.grinderCacheBytes=0;
        check(!geometry.idle(model,state(new Capture()),0,.34,0),"zero-budget fallback");
        check(geometry.cachedBytes()==0,"zero budget releases geometry");
        ClientPerformanceOptions.grinderCacheBytes=2*1024*1024;
    }

    public static void main(String[] args) {
        caches(); obj(); grinder();
        System.out.println("Client rendering parity checks passed: "+checks);
    }
}
