package com.tonywww.slashblade_sendims.client;

import com.tonywww.slashblade_sendims.network.SukunaVisualPacket;
import net.minecraft.world.phys.Vec3;

/** Bounded procedural shrine and planar cuts, using standard entity materials. */
public final class SukunaGeometry {
    private static final Vec3 UP=new Vec3(0,1,0);
    private static final int BONE=0xADA18C,RED=0x661C23,BLACK=0x151017,WHITE=0xFFF6EB;
    @FunctionalInterface public interface Sink {
        void quad(Vec3 a,Vec3 b,Vec3 c,Vec3 d,int rgb,float alpha,boolean glow);
    }
    private SukunaGeometry() {}
    private static double clamp(double n) {return Math.max(0,Math.min(1,n));}
    private static double noise(long seed,int index) {return HolySwordGeometry.noise(seed,index);}
    public static void draw(SukunaVisualPacket p,float age,boolean detail,Sink s) {
        Vec3 x=p.forward().cross(UP).normalize(),z=p.forward(),o=p.origin();
        switch(p.stage()) {
            case DOMAIN -> {
                float alpha=(float)(clamp(age/6)*clamp((150-age)/8));
                if(alpha<=0)return;
                Vec3 c=o.subtract(z.scale(4));
                box(s,c,x,z,3.5,.3,2.5,BLACK,alpha,false);
                box(s,c.add(0,.6,0),x,z,3,.3,2.1,BONE,alpha,false);
                for(int side:new int[]{-1,1})for(int back:new int[]{-1,1}) {
                    Vec3 pillar=c.add(x.scale(side*2.4)).add(z.scale(back*1.5));
                    box(s,pillar.add(0,3.1,0),x,z,.25,2.5,.28,BONE,alpha,false);
                    box(s,pillar.add(0,1.1,0),x,z,.48,.25,.48,RED,alpha,false);
                    box(s,pillar.add(0,5.5,0),x,z,.5,.23,.5,BONE,alpha,false);
                }
                box(s,c.add(0,2.6,0),x,z,1.5,1.8,.8,BLACK,alpha,false);
                box(s,c.add(0,4.5,0),x,z,1.65,.22,1,RED,alpha,false);
                roof(s,c.add(0,5.7,0),x,z,3.9,2.7,1.1,RED,alpha);
                roof(s,c.add(0,7.0,0),x,z,2.7,1.9,.8,BLACK,alpha);
                for(int side:new int[]{-1,1}) {
                    line(s,c.add(x.scale(side*3.8)).add(0,5.7,0),
                            c.add(x.scale(side*4.6)).add(0,6.7,0),z,UP,.14,BONE,alpha,false);
                    line(s,c.add(x.scale(side*2.6)).add(0,7,0),
                            c.add(x.scale(side*3.2)).add(0,7.8,0),z,UP,.12,BONE,alpha,false);
                }
                // Skull-like reliquaries and a toothed mouth, built as closed solids.
                int skulls=detail?5:3;
                for(int i=0;i<skulls;i++) {
                    Vec3 skull=c.add(x.scale((i-(skulls-1)*.5)*1.1)).add(z.scale(2.15)).add(0,1.1,0);
                    box(s,skull,x,z,.48,.42,.3,BONE,alpha,false);
                    for(int sign:new int[]{-1,1})box(s,skull.add(x.scale(sign*.2)).add(z.scale(.31)),
                            x,z,.12,.13,.025,BLACK,alpha,false);
                }
                Vec3 mouth=c.add(z.scale(.85)).add(0,2.5,0);
                for(int i=0;i<7;i++)for(int sign:new int[]{-1,1})
                    box(s,mouth.add(x.scale((i-3)*.37)).add(0,sign*.8,0),x,z,.1,.27,.12,BONE,alpha,false);
                ring(s,o.add(0,.07,0),16,.09,RED,alpha*.75F,detail?64:40);
                ring(s,o.add(0,.08,0),15.7,.025,WHITE,alpha*.45F,detail?64:40);
                domainBoundary(s,o,alpha,detail);
                int phase=(int)(age/10);double beat=Math.exp(-(age%10)*.42);
                int cuts=detail?18:8;
                for(int i=0;i<cuts;i++) {
                    long seed=p.id()+phase*137;
                    double a=noise(seed,i*6)*Math.PI*2, r=Math.sqrt(noise(seed,i*6+1))*12;
                    Vec3 mid=new Vec3(Math.cos(a)*r,-5+noise(seed,i*6+2)*10,Math.sin(a)*r);
                    double direction=noise(seed,i*6+3)*Math.PI*2;
                    Vec3 axis=new Vec3(Math.cos(direction),(noise(seed,i*6+4)-.5)*.6,Math.sin(direction)).normalize();
                    double length=8+noise(seed,i*6+5)*6;
                    // Clip each long stroke to the actual cylinder, instead of shortening
                    // every stroke by its midpoint's distance from the outside edge.
                    double horizontal=axis.x*axis.x+axis.z*axis.z;
                    double dot=mid.x*axis.x+mid.z*axis.z;
                    double root=Math.sqrt(dot*dot+horizontal*(16*16-mid.x*mid.x-mid.z*mid.z));
                    double low=Math.max(-length,(-dot-root)/horizontal);
                    double high=Math.min(length,(-dot+root)/horizontal);
                    if(Math.abs(axis.y)>1e-6) {
                        double below=(-6-mid.y)/axis.y,above=(6-mid.y)/axis.y;
                        low=Math.max(low,Math.min(below,above));high=Math.min(high,Math.max(below,above));
                    }
                    Vec3 start=o.add(mid).add(axis.scale(low)),end=o.add(mid).add(axis.scale(high));
                    Vec3 side=axis.cross(UP).normalize(),normal=axis.cross(side).normalize();
                    line(s,start,end,side,normal,
                            .035,WHITE,(float)(alpha*beat*.8),true);
                    if(detail)line(s,start,end,side,normal,
                            .1,RED,(float)(alpha*beat*.35),true);
                }
            }
            case SPACE -> {
                float alpha=(float)clamp(1-age/6);if(alpha<=0)return;
                Vec3 axis=x.add(UP.scale(.12)).normalize();
                line(s,o.subtract(axis.scale(5)),o.add(axis.scale(5)),z,UP,.045,BLACK,alpha,true);
                line(s,o.subtract(axis.scale(5)),o.add(axis.scale(5)),z,UP,.009,WHITE,alpha,true);
                for(int side:new int[]{-1,1}) {
                    Vec3 edge=o.add(0,side*(.045+age*.012),0);
                    line(s,edge.subtract(axis.scale(5)),edge.add(axis.scale(5)),z,UP,.009,RED,alpha*.7F,true);
                    if(detail)line(s,o.add(0,side*1.3,0).subtract(axis.scale(4)),
                            o.add(0,side*1.3,0).add(axis.scale(4)),z,UP,.006,WHITE,alpha*.35F,true);
                }
            }
            case WHEEL -> {
                float alpha=(float)clamp(1-age/16);if(alpha<=0)return;
                ring(s,o,.7,.08,0xD2BF86,alpha,32);
                for(int i=0;i<8;i++) {
                    double a=i*Math.PI/4+Math.min(age/10,1)*Math.PI/4;
                    Vec3 axis=new Vec3(Math.cos(a),0,Math.sin(a));
                    line(s,o.add(axis.scale(.15)),o.add(axis.scale(1.0)),UP,axis.cross(UP),.035,0xD2BF86,alpha,true);
                }
            }
            case END -> {}
        }
    }
    private static void domainBoundary(Sink s,Vec3 o,float alpha,boolean detail) {
        int segments=detail?40:24;
        for(int sign:new int[]{-1,1}) {
            ring(s,o.add(0,sign*6,0),16,.065,RED,alpha*.55F,segments);
            ring(s,o.add(0,sign*6,0),15.9,.02,WHITE,alpha*.25F,segments);
        }
        // Low-opacity walls give the ground ring height without a dense particle cloud.
        for(int i=0;i<segments;i++) {
            double a=i*Math.PI*2/segments,b=(i+1)*Math.PI*2/segments;
            Vec3 p=o.add(Math.cos(a)*16,0,Math.sin(a)*16),q=o.add(Math.cos(b)*16,0,Math.sin(b)*16);
            s.quad(p.add(0,-6,0),q.add(0,-6,0),q.add(0,6,0),p.add(0,6,0),RED,alpha*.045F,true);
            if(i%(detail?5:4)==0) {
                Vec3 radial=new Vec3(Math.cos(a),0,Math.sin(a));
                line(s,p.add(0,-6,0),p.add(0,6,0),radial,radial.cross(UP),.018,RED,alpha*.35F,true);
            }
        }
    }
    private static void box(Sink s,Vec3 o,Vec3 right,Vec3 forward,double w,double h,double d,int color,float a,boolean glow) {
        Vec3 x=right.scale(w),y=UP.scale(h),z=forward.scale(d);
        Vec3 p=o.subtract(x).subtract(y).subtract(z),q=o.add(x).subtract(y).subtract(z);
        Vec3 r=o.add(x).add(y).subtract(z),t=o.subtract(x).add(y).subtract(z);
        Vec3 u=o.subtract(x).subtract(y).add(z),v=o.add(x).subtract(y).add(z);
        Vec3 j=o.add(x).add(y).add(z),k=o.subtract(x).add(y).add(z);
        s.quad(p,q,r,t,color,a,glow);s.quad(v,u,k,j,color,a,glow);
        s.quad(u,p,t,k,color,a,glow);s.quad(q,v,j,r,color,a,glow);
        s.quad(t,r,j,k,color,a,glow);s.quad(u,v,q,p,color,a,glow);
    }
    private static void roof(Sink s,Vec3 c,Vec3 x,Vec3 z,double w,double d,double h,int color,float alpha) {
        Vec3[] a=new Vec3[4],b=new Vec3[4];
        double[] dz={-d,-d*.15,d*.15,d},dy={0,h,h,0};
        for(int i=0;i<4;i++) {a[i]=c.subtract(x.scale(w)).add(z.scale(dz[i])).add(0,dy[i],0);
            b[i]=c.add(x.scale(w)).add(z.scale(dz[i])).add(0,dy[i],0);}
        s.quad(a[3],a[2],a[1],a[0],color,alpha,false);s.quad(b[0],b[1],b[2],b[3],color,alpha,false);
        for(int i=0;i<4;i++) {int j=(i+1)%4;s.quad(a[i],a[j],b[j],b[i],color,alpha,false);}
    }
    private static void line(Sink s,Vec3 a,Vec3 b,Vec3 x,Vec3 y,double w,int color,float alpha,boolean glow) {
        for(Vec3 axis:new Vec3[]{x,y}) {Vec3 d=axis.scale(w);
            s.quad(a.subtract(d),a.add(d),b.add(d),b.subtract(d),color,alpha,glow);}
    }
    private static void ring(Sink s,Vec3 o,double radius,double width,int color,float alpha,int count) {
        for(int i=0;i<count;i++) {
            double a=i*Math.PI*2/count,b=(i+1)*Math.PI*2/count;
            s.quad(o.add(Math.cos(a)*radius,0,Math.sin(a)*radius),
                    o.add(Math.cos(a)*(radius+width),0,Math.sin(a)*(radius+width)),
                    o.add(Math.cos(b)*(radius+width),0,Math.sin(b)*(radius+width)),
                    o.add(Math.cos(b)*radius,0,Math.sin(b)*radius),color,alpha,true);
        }
    }
}
