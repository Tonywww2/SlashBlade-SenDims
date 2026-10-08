package com.tonywww.slashblade_sendims.worldgen;

import java.util.*;

/** Seeded fractured bodies, clipped candidate searches and no per-tick terrain work. */
public final class AsteroidBeltField {
    public static final int HEIGHT=320, CELL=192, SWARM_CELL=64;
    public enum Type { ROCK, MINERAL, HOLLOW, ICE, ICE_ROCK, ICE_ORE, GARDEN }
    public enum Material { AIR, ROCK, CARBON, SILICA, ORE, MINERAL_INNER, MINERAL_ALTERNATE, MINERAL_MIDDLE, MINERAL_OUTER, PACKED_ICE, BLUE_ICE, ICE, DIRT, GRASS, WATER, GLOWSTONE }
    public final long seed;
    private final Map<Long,Body> bodies=Collections.synchronizedMap(new LinkedHashMap<>(512,.75f,true){
        @Override protected boolean removeEldestEntry(Map.Entry<Long,Body> e){return size()>512;}
    });
    private final Map<Long,List<Body>> companions=Collections.synchronizedMap(new LinkedHashMap<>(512,.75f,true){
        @Override protected boolean removeEldestEntry(Map.Entry<Long,List<Body>> e){return size()>512;}
    });
    public AsteroidBeltField(long seed){this.seed=seed;}
    public static long mix(long v){v=(v^(v>>>30))*0xbf58476d1ce4e5b9L;v=(v^(v>>>27))*0x94d049bb133111ebL;return v^(v>>>31);}
    public static double unit(long v){return (mix(v)>>>11)*0x1.0p-53;}
    public static long key(int x,int z){return ((long)x<<32)^(z&0xffffffffL);}
    public static long hash(long seed,int x,int y,int z){return mix(seed^mix(x*0x632be59bd9b4e019L)^Long.rotateLeft(mix(y*0x9e3779b97f4a7c15L),21)^Long.rotateLeft(mix(z*0x94d049bb133111ebL),42));}
    public static double noise(long seed,double x,double y,double z){
        int ix=(int)Math.floor(x),iy=(int)Math.floor(y),iz=(int)Math.floor(z);double u=smooth(x-ix),v=smooth(y-iy),w=smooth(z-iz),out=0;
        for(int dz=0;dz<2;dz++)for(int dy=0;dy<2;dy++)for(int dx=0;dx<2;dx++)out+=(unit(hash(seed,ix+dx,iy+dy,iz+dz))*2-1)*(dx==0?1-u:u)*(dy==0?1-v:v)*(dz==0?1-w:w);
        return out;
    }
    private static double smooth(double v){return v*v*(3-2*v);}
    private static Type type(long h,boolean major){
        double r=unit(h+1);
        if(major)return r<.54?Type.ROCK:r<.60?Type.MINERAL:r<.70?Type.HOLLOW:r<.81?Type.ICE:r<.88?Type.ICE_ROCK:r<.94?Type.ICE_ORE:Type.GARDEN;
        return r<.73?Type.ROCK:r<.76?Type.MINERAL:r<.84?Type.HOLLOW:r<.95?Type.ICE:r<.98?Type.ICE_ROCK:Type.ICE_ORE;
    }
    public Body major(int cx,int cz){
        long k=key(cx,cz);Body old=bodies.get(k);if(old!=null)return old;long h=hash(seed,cx,0,cz);Type t=type(h,true);
        // A continuous distribution closes the previous 13.5..28 and 54..62 size gaps.
        double radius=t==Type.GARDEN?23.5+unit(h+2)*16.5:6+74*Math.pow(unit(h+2),1.28);
        Body b=create(h,t,cx*CELL+CELL*.5+(unit(h+9)-.5)*68,48+unit(h+8)*230,cz*CELL+CELL*.5+(unit(h+10)-.5)*68,radius,true);
        bodies.put(k,b);return b;
    }
    private Body create(long h,Type t,double x,double y,double z,double radius,boolean major){
        boolean garden=t==Type.GARDEN;
        return new Body(h,t,x,y,z,radius*(garden?.92+unit(h+5)*.20:.68+unit(h+5)*.44),radius*(garden?.70+unit(h+7)*.16:.42+unit(h+7)*.43),radius*(garden?.87+unit(h+6)*.25:.56+unit(h+6)*.54),major);
    }
    private List<Body> companion(int cx,int cz){
        long k=key(cx,cz);var old=companions.get(k);if(old!=null)return old;List<Body> out=new ArrayList<>();
        // All heights and radii vary continuously; the overlapping bands only bound searches.
        for(int layer=0;layer<3;layer++){
            long h=hash(seed^0x51acdaL,cx,layer,cz);if(unit(h)>.68)continue;
            double radius=2.5+39*Math.pow(unit(h+4),1.85);Type t=type(h,false);
            Body b=create(h,t,cx*SWARM_CELL+4+unit(h+11)*(SWARM_CELL-8),25+layer*96+unit(h+12)*78,cz*SWARM_CELL+4+unit(h+13)*(SWARM_CELL-8),radius,false);
            // Keep cavities pristine, while allowing broken companions to cluster around solid rocks.
            boolean clear=true;
            int mx=Math.floorDiv((int)Math.floor(b.x),CELL),mz=Math.floorDiv((int)Math.floor(b.z),CELL);
            for(int dz=-1;dz<=1&&clear;dz++)for(int dx=-1;dx<=1;dx++){
                Body p=major(mx+dx,mz+dz);double nx=(b.x-p.x)/(p.rx+b.bound*.75+4),ny=(b.y-p.y)/(p.ry+b.bound*.75+4),nz=(b.z-p.z)/(p.rz+b.bound*.75+4);
                double limit=p.type==Type.GARDEN||p.type==Type.HOLLOW?2.8:.42;
                if(nx*nx+ny*ny+nz*nz<limit){clear=false;break;}
            }
            if(clear)out.add(b);
        }
        old=List.copyOf(out);companions.put(k,old);return old;
    }
    public List<Body> intersecting(int minX,int minZ,int maxX,int maxZ){
        List<Body> out=new ArrayList<>();
        for(int z=Math.floorDiv(minZ,CELL)-1;z<=Math.floorDiv(maxZ,CELL)+1;z++)for(int x=Math.floorDiv(minX,CELL)-1;x<=Math.floorDiv(maxX,CELL)+1;x++){
            var b=major(x,z);if(b.intersects(minX,minZ,maxX,maxZ))out.add(b);
        }
        // Largest companion envelope is below 86 blocks, so two neighboring swarm cells suffice.
        for(int z=Math.floorDiv(minZ-86,SWARM_CELL);z<=Math.floorDiv(maxZ+86,SWARM_CELL);z++)for(int x=Math.floorDiv(minX-86,SWARM_CELL);x<=Math.floorDiv(maxX+86,SWARM_CELL);x++)
            for(var b:companion(x,z))if(b.intersects(minX,minZ,maxX,maxZ))out.add(b);
        out.sort(Comparator.comparingLong(b->b.id));return List.copyOf(out);
    }
    public final class Body {
        public final long id;public final Type type;public final double x,y,z,rx,ry,rz,bound,radius;
        public final boolean major,river,pond;public final int ore,waterY;
        private final double cos,sin,tilt,verticalBound,coarseScale,fineScale,coreX,coreY,coreZ;
        private final double[][] faces,lobes,craters,voids;
        Body(long id,Type type,double x,double y,double z,double rx,double ry,double rz,boolean major){
            this.id=id;this.type=type;this.x=x;this.z=z;this.rx=rx;this.ry=ry;this.rz=rz;this.major=major;radius=Math.max(rx,rz);
            bound=radius*1.75+3;double angle=unit(id+20)*Math.PI*2;cos=Math.cos(angle);sin=Math.sin(angle);tilt=(unit(id+21)-.5)*.20;
            verticalBound=ry*1.75+Math.abs(tilt)*bound+3;this.y=Math.max(verticalBound+3,Math.min(y,HEIGHT-4-verticalBound));
            coarseScale=2.4/radius;fineScale=9.5/radius;faces=new double[14][4];
            // Unpaired, slanted faces give every body an angular, asymmetric skeleton.
            for(int i=0;i<6;i++){
                long h=id+100+i*7;int axis=i/2;double[] f=faces[i];
                for(int a=0;a<3;a++)f[a]=a==axis?(i%2==0?1:-1):(unit(h+a)-.5)*.30;
                f[3]=type==Type.GARDEN?.94+unit(h+4)*.14:.46+unit(h+4)*.46;
            }
            for(int i=0;i<8;i++){
                long h=id+160+i*5;double[] f=faces[i+6];double norm=0;
                for(int a=0;a<3;a++){f[a]=((i>>a&1)==0?1:-1)*(.72+unit(h+a)*.55);norm+=f[a]*f[a];}
                norm=Math.sqrt(norm);for(int a=0;a<3;a++)f[a]/=norm;f[3]=type==Type.GARDEN?1.0+unit(h+3)*.12:.68+unit(h+3)*.32;
            }
            lobes=new double[(major?2:1)+(int)(unit(id+82)*(major?4:3))][6];
            for(int i=0;i<lobes.length;i++){
                long h=id+230+i*9;double a=unit(h)*Math.PI*2,e=(unit(h+1)-.5)*1.7,lateral=Math.sqrt(1-e*e),offset=.38+unit(h+2)*.42,r=.50+unit(h+3)*.30;
                lobes[i]=new double[]{Math.cos(a)*lateral*offset,e*offset,Math.sin(a)*lateral*offset,1/(r*(.66+unit(h+4)*.34)),1/(r*(.55+unit(h+5)*.45)),1/(r*(.64+unit(h+6)*.36))};
            }
            voids=new double[1+(int)(unit(id+83)*3)][6];
            for(int i=0;i<voids.length;i++){
                long h=id+310+i*7;voids[i]=new double[]{(unit(h)-.5)*.75,(unit(h+1)-.5)*.60,(unit(h+2)-.5)*.75,
                    1/(.16+unit(h+3)*.28),1/(.15+unit(h+4)*.26),1/(.17+unit(h+5)*.26)};
            }
            coreX=(unit(id+51)-.5)*.32;coreY=(unit(id+52)-.5)*.32;coreZ=(unit(id+53)-.5)*.32;
            ore=(int)(unit(id+22)*100000);waterY=(int)Math.floor(this.y-ry*.28);river=unit(id+23)<.52;pond=unit(id+24)<.64;
            craters=new double[major?2+(int)(unit(id+84)*3):1][4];
            for(int i=0;i<craters.length;i++){
                long h=id+400+i*5;double a=unit(h)*Math.PI*2,e=(unit(h+1)-.5)*1.65,lateral=Math.sqrt(1-e*e),u=Math.cos(a)*lateral,v=Math.sin(a)*lateral;
                craters[i]=new double[]{x+u*rx*cos-v*rz*sin,this.y+e*ry,z+u*rx*sin+v*rz*cos,Math.min(rx,rz)*(.13+unit(h+2)*.34)};
            }
        }
        public boolean ice(){return type==Type.ICE||type==Type.ICE_ROCK||type==Type.ICE_ORE||type==Type.GARDEN;}
        public int minY(){return Math.max(2,(int)Math.floor(y-verticalBound));}public int maxY(){return Math.min(317,(int)Math.ceil(y+verticalBound));}
        public boolean intersects(int a,int b,int c,int d){return x+bound>=a&&x-bound<=c&&z+bound>=b&&z-bound<=d;}
        public double radial(int px,int pz){double dx=px-x,dz=pz-z,u=(dx*cos+dz*sin)/rx,v=(dz*cos-dx*sin)/rz;return u*u+v*v;}
        private double poly(double u,double w,double v){
            double out=-100;
            for(double[] f:faces){
                double plane=(u*f[0]+w*f[1]+v*f[2])/f[3],gap=Math.abs(out-plane);
                out=Math.max(out,plane);
                // A narrow polynomial smooth maximum rounds meeting faces without
                // erasing their asymmetric shape. Single faces remain unchanged;
                // two equal faces receive only a 0.05 normalized corner inset.
                if(gap<.20){double blend=.20-gap;out+=blend*blend/ .80;}
            }
            return out;
        }
        private double wrinkle(int px,int py,int pz){return noise(id+66,px*coarseScale,py*coarseScale,pz*coarseScale)*.11+noise(id+67,px*fineScale,py*fineScale,pz*fineScale)*.025;}
        private double cavity(int px,int py,int pz){double dx=px-x,dz=pz-z,u=(dx*cos+dz*sin)/rx,v=(dz*cos-dx*sin)/rz;return poly(u/.70,(py-y)/(ry*.72),v/.70)+wrinkle(px,py,pz);}
        public boolean inGarden(int px,int py,int pz){return type==Type.GARDEN&&cavity(px,py,pz)<1;}
        /** Conservative convex shell interval; used once per column to reserve garden space. */
        public int[] gardenInterval(int px,int pz){
            if(type!=Type.GARDEN)return null;double dx=px-x,dz=pz-z,u=(dx*cos+dz*sin)/(rx*.70),v=(dz*cos-dx*sin)/(rz*.70),lo=minY(),hi=maxY();
            for(double[] f:faces){double side=u*f[0]+v*f[2],a=f[1]/(ry*.72),c=1.56*f[3]-side;
                if(Math.abs(a)<1e-10){if(c<0)return null;}else if(a>0)hi=Math.min(hi,y+c/a);else lo=Math.max(lo,y+c/a);
            }
            return lo<=hi?new int[]{(int)Math.floor(lo),(int)Math.ceil(hi)}:null;
        }
        public int floor(int px,int pz){double relief=noise(id,px*.071,0,pz*.071)*2.7+noise(id+1,px*.18,0,pz*.18)*.85;return waterY+2+Math.max(-1,(int)Math.round(relief));}
        public boolean water(int px,int pz){
            double dx=px-x,dz=pz-z,u=dx*cos+dz*sin,v=dz*cos-dx*sin;if(radial(px,pz)>.21||!inGarden(px,waterY-1,pz))return false;
            boolean stream=river&&Math.abs(v-Math.sin(u*.12+unit(id+45)*6)*rz*.12)<.9+unit(id+46)*1.5&&Math.abs(u)<rx*.40;
            double pu=(u+rx*.16)/(rx*.16),pv=(v-rz*.07)/(rz*.13);return stream||(pond&&pu*pu+pv*pv<1+noise(id+47,px*.19,0,pz*.19)*.30);
        }
        public double[] mineralCenter(){double dx=coreX*rx*cos-coreZ*rz*sin,dz=coreX*rx*sin+coreZ*rz*cos;return new double[]{x+dx,y+coreY*ry+tilt*(dx*.65+dz*.35),z+dz};}
        public Material sample(int px,int py,int pz){
            double dx=px-x,dz=pz-z,u=(dx*cos+dz*sin)/rx,v=(dz*cos-dx*sin)/rz,w=(py-y-tilt*(dx*.65+dz*.35))/ry,q=u*u+v*v+w*w;
            double c=type==Type.GARDEN?cavity(px,py,pz):0;boolean protectedGarden=type==Type.GARDEN&&c<=1.39;
            double density=poly(u,w,v);
            if(!protectedGarden){
                if(q>3.0625)return Material.AIR;
                for(double[] l:lobes)density=Math.min(density,poly((u-l[0])*l[3],(w-l[1])*l[4],(v-l[2])*l[5]));
                if(density>1.32)return Material.AIR;
                double rough=noise(id,px*coarseScale,py*coarseScale,pz*coarseScale)*.25+noise(id+2,px*fineScale,py*fineScale,pz*fineScale)*.09;
                if(density>1+rough)return Material.AIR;
                for(double[] crater:craters){double a=(px-crater[0])/crater[3],b=(py-crater[1])/crater[3],d=(pz-crater[2])/crater[3];if(a*a+b*b+d*d<.85+rough*.45)return Material.AIR;}
            }
            long h=hash(id,px,py,pz);
            if(type==Type.GARDEN){
                if(c<1){int fy=floor(px,pz);if(water(px,pz)){if(py<=waterY-2)return Material.DIRT;if(py<=waterY)return Material.WATER;return Material.AIR;}
                    if(py<fy)return Material.DIRT;if(py==fy){int gx=Math.floorDiv(px,7),gz=Math.floorDiv(pz,7);long lh=hash(id+71,gx,0,gz);
                        if(Math.floorMod(px,7)==1+(int)(unit(lh)*4)&&Math.floorMod(pz,7)==1+(int)(unit(lh+1)*4))return Material.GLOWSTONE;return Material.GRASS;}
                    return Material.AIR;
                }
                if(c<1.075&&py>y&&noise(id+73,px*.24,py*.24,pz*.24)>.22)return Material.GLOWSTONE;
                if(c<1.15+noise(id+74,px*.13,py*.13,pz*.13)*.018)return rock(px,py,pz);
            }
            double porous=noise(id+12,px*coarseScale,py*coarseScale,pz*coarseScale);
            if(type==Type.HOLLOW){
                double hollow=poly((u-coreX)/.69,(w-coreY)/.66,(v-coreZ)/.70)+porous*.23;
                if(hollow<1)return Material.AIR;
            }else if(type==Type.ROCK&&major&&unit(id+55)<.37){
                for(double[] a:voids)if(poly((u-a[0])*a[3],(w-a[1])*a[4],(v-a[2])*a[5])+porous*.2<1)return Material.AIR;
            }
            double core=poly((u-coreX)/.40,(w-coreY)/.39,(v-coreZ)/.38)+porous*.28;
            if(type==Type.MINERAL){
                if(core<1&&unit(h)<.80)return Material.ORE;
                if(core<1.22)return unit(h+1)<.16?Material.MINERAL_ALTERNATE:Material.MINERAL_INNER;
                if(core<1.65)return Material.MINERAL_MIDDLE;
                return noise(id+59,px*.17,py*.17,pz*.17)>.15?rock(px,py,pz):Material.MINERAL_OUTER;
            }
            if(ice()){
                if((type==Type.ICE_ROCK||type==Type.ICE_ORE)&&core<1.6){if(type==Type.ICE_ORE&&core<.90&&unit(h)<.73)return Material.ORE;return rock(px,py,pz);}
                double vein=noise(id+53,px*coarseScale*2.0,py*coarseScale*2.0,pz*coarseScale*2.0);
                if(vein>.12)return Material.BLUE_ICE;
                if(vein<-.17&&unit(h+54)<.35)return Material.ICE;return Material.PACKED_ICE;
            }
            if(type==Type.ROCK&&core<.65&&unit(id+56)<.46&&unit(h)<.58)return Material.ORE;
            return rock(px,py,pz);
        }
        private Material rock(int px,int py,int pz){
            // Twisted lenses, breccia patches and veins replace the former repeated seven-layer bands.
            double large=noise(id+59,px*coarseScale*1.6,py*coarseScale*1.3,pz*coarseScale*1.6),small=noise(id+60,px*.29,py*.31,pz*.29);
            double vein=noise(id+61,px*coarseScale*2.5,py*coarseScale*1.8,pz*coarseScale*2.5);
            if(large+small*.40>.15+(unit(id+62)-.5)*.20)return Material.CARBON;
            if(Math.abs(vein)<.06+unit(id+63)*.055||large+small*.32<-.27)return Material.SILICA;
            return Material.ROCK;
        }
    }
}
