package com.tonywww.slashblade_sendims.worldgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Pure, coordinate-addressed geometry. No world access, chunk-order state or registry dependencies. */
public final class WorldTreeField {
    public static final int MIN_Y = -64, HEIGHT = 384, MAX_Y = MIN_Y + HEIGHT - 1;
    public enum Zone { ROOTS, SHALLOWS, DEPTHS }
    public enum Kind { AIR, WATER, BEDROCK, STONE, DIRT, SOIL, MUD, GRAVEL, WOOD, LEAVES,
        MANGROVE_LOG, MANGROVE_ROOTS, MANGROVE_LEAVES, MANGROVE_PROPAGULE }
    public final long seed;
    public final int seaLevel, spacing, crownRadius;
    private final ConcurrentHashMap<Long, Tree> trees = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Column> columns = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Optional<Mangrove>> mangroves = new ConcurrentHashMap<>();
    private static final Segment[] NO_SEGMENTS=new Segment[0];
    private static long coordinateKey(int x,int z){return mix(((long)x<<32)^(z&0xffffffffL));}
    private static <T> void trim(ConcurrentHashMap<Long,T> cache,int limit,int batch){
        if(cache.size()<=limit)return;
        int removed=0;
        for(Long key:cache.keySet()){cache.remove(key);if(++removed>=batch)break;}
    }

    public WorldTreeField(long seed, int seaLevel, int spacing, int crownRadius) {
        if (seaLevel < 16 || seaLevel > 96 || spacing < 280 || spacing > 600 || crownRadius < 120 || crownRadius > 400)
            throw new IllegalArgumentException("Invalid world-tree layout parameters");
        this.seed = seed; this.seaLevel = seaLevel; this.spacing = spacing; this.crownRadius = crownRadius;
    }

    public static long mix(long v) {
        v = (v ^ (v >>> 30)) * 0xbf58476d1ce4e5b9L;
        v = (v ^ (v >>> 27)) * 0x94d049bb133111ebL;
        return v ^ (v >>> 31);
    }
    private long cellSeed(int x, int z) { return mix(seed ^ ((long)x * 0x632be59bd9b4e019L) ^ ((long)z * 0x9e3779b97f4a7c15L)); }
    public long pointSeed(int x, int y, int z) { return mix(cellSeed(x, z) ^ ((long)y * 0xd1b54a32d192ed03L)); }
    public int materialAt(int x,int z,int count){return Math.min(count-1,Math.max(0,(int)((noise(x*.035,z*.035)+1)*.5*count)));}
    public boolean mineralPatchAt(int x,int z){return noise(x*.012+101,z*.012-77)>.05;}
    public double vegetationAt(int x,int z){return (noise(x*.024+63,z*.024-29)+1)*.5;}
    /** Broad ridges, smaller dunes, and fine gravel relief; deep basins remain ~100 blocks deep. */
    public double seabedAt(double x,double z){
        return -43+11*noise(x*.007-53,z*.007+71)+5*noise(x*.026+19,z*.026-37)
            +1.7*noise(x*.085-8,z*.085+5);
    }
    private static double smooth(double v) { v = Math.max(0, Math.min(1, v)); return v*v*(3-2*v); }
    private static double lerp(double t, double a, double b) { return a + t*(b-a); }
    private double lattice(int x, int z) { return (cellSeed(x,z) >>> 11) * 0x1.0p-53 * 2 - 1; }
    private double noise(double x, double z) {
        int ix=(int)Math.floor(x), iz=(int)Math.floor(z);
        double tx=smooth(x-ix), tz=smooth(z-iz);
        return lerp(tz,lerp(tx,lattice(ix,iz),lattice(ix+1,iz)),lerp(tx,lattice(ix,iz+1),lattice(ix+1,iz+1)));
    }
    public Tree tree(int cx, int cz) {
        long key = coordinateKey(cx,cz);
        Tree existing=trees.get(key);
        if (existing!=null) return existing;
        Tree result=trees.computeIfAbsent(key,k->new Tree(cx,cz));
        trim(trees,512,64);
        return result;
    }
    public List<Tree> nearby(int x, int z) {
        int cx=Math.floorDiv(x,spacing), cz=Math.floorDiv(z,spacing);
        // Enlarged, winding crowns can reach past the immediately adjacent cell.
        int reach=Math.max(1,(int)Math.ceil(Math.max(crownRadius*1.9+70,710)/spacing));
        List<Tree> result=new ArrayList<>((reach*2+1)*(reach*2+1));
        for(int dz=-reach;dz<=reach;dz++) for(int dx=-reach;dx<=reach;dx++) {
            int tx=cx+dx,tz=cz+dz;
            // Cluster the same 12.5% open-water allowance into 2x2-cell basins.
            // Larger, merging islands then have real sea channels between archipelagos.
            long basin=mix(cellSeed(Math.floorDiv(tx,4),Math.floorDiv(tz,4))^0x71d47318ce25bca9L)&7;
            int quadrant=Math.floorMod(tx,4)/2+2*(Math.floorMod(tz,4)/2);
            if(basin>=4 || basin!=quadrant)result.add(tree(tx,tz));
        }
        return result;
    }
    private double distance(Tree t, double x, double z) {
        // Domain warping creates bays and promontories rather than a regular ellipse.
        double wx=x+42*noise(x*.006+17,z*.006-31)+14*noise(x*.022-9,z*.022+12);
        double wz=z+42*noise(x*.006-43,z*.006+11)+14*noise(x*.022+7,z*.022-6);
        double dx=wx-t.x, dz=wz-t.z, a=Math.atan2(dz,dx);
        double lobes=1+.22*Math.sin(a*3+t.phase)+.13*Math.sin(a*5-t.phase)+.05*Math.sin(a*9+t.phase*2);
        return Math.hypot(dx/t.stretch,dz*t.stretch)/lobes
            +8*noise(x*.037,z*.037)+3*noise(x*.12+3,z*.12-5)+noise(x*.28-11,z*.28+9);
    }
    public Zone zoneAt(int x, int z) {
        int floor=floorAt(x,z);
        return floor>=seaLevel ? Zone.ROOTS : floor>=seaLevel-11 ? Zone.SHALLOWS : Zone.DEPTHS;
    }
    public int floorAt(int x, int z) { return floorAt(x,z,nearby(x,z)); }
    private int floorAt(int x, int z, List<Tree> candidates) {
        double top=seabedAt(x,z);
        for(Tree t:candidates) {
            // Distant crowns still contribute leaves, but cannot contribute island terrain.
            if(Math.abs(x-t.x)>t.shore*t.stretch*1.5+70 || Math.abs(z-t.z)>t.shore/t.stretch*1.5+70)continue;
            top=Math.max(top,terrain(t,x,z));
        }
        return Math.max(-61,(int)Math.floor(top));
    }
    private double terrain(Tree t,double x,double z) {
        double d=distance(t,x,z);
        double bottom=seabedAt(x,z);
        if(d>=t.shore)return bottom;
        double land=seaLevel+2+10*smooth(1-d/t.rootIsland)
            +2.4*noise(x*.037,z*.037)+.8*noise(x*.16-8,z*.16+5);
        double shelf=seaLevel-2-5*smooth((d-t.rootIsland)/(t.shore-t.rootIsland))
            +1.7*noise(x*.045+6,z*.045-9)+.6*noise(x*.19,z*.19);
        // Both the dry shore and the ocean slope are continuous. There is no Y jump
        // at rootIsland and no narrow vertical trench around shore.
        double surface=lerp(smooth((d-(t.rootIsland-19))/38),land,shelf);
        double width=t.slopeWidth+6*noise(x*.011+33,z*.011-18);
        double descent=smooth((d-(t.shore-width))/width);
        return lerp(descent,surface,bottom);
    }
    private double woodNoise(double x,double y,double z,double phase) {
        return .65*noise(x*.19+y*.087+phase,z*.17-y*.093)
            +.35*noise(x*.38-y*.13,z*.34+y*.17+phase);
    }
    public Column column(int x, int z) {
        Column existing=columns.get(coordinateKey(x,z));
        return existing!=null?existing:cachedColumn(x,z,nearby(x,z));
    }
    public Column cachedColumn(int x,int z,List<Tree> candidates){
        long key=coordinateKey(x,z);Column existing=columns.get(key);
        if(existing!=null)return existing;
        Column result=columns.computeIfAbsent(key,k->new Column(x,z,candidates));
        trim(columns,16384,2048);
        return result;
    }
    // This uncached overload also lets regression tests compare independently selected candidates.
    public Column column(int x, int z, List<Tree> candidates) { return new Column(x,z,candidates); }
    public final class Column {
        public final int x,z,floor;
        public final Zone zone;
        private final List<Tree> trunks=new ArrayList<>(2);
        private final List<Segment> wood=new ArrayList<>();
        private final List<Lobe> leaves=new ArrayList<>();
        private final List<Mangrove> smallTrees=new ArrayList<>(1);
        Column(int x,int z,List<Tree> candidates) {
            this.x=x;this.z=z;floor=floorAt(x,z,candidates);
            zone=floor>=seaLevel ? Zone.ROOTS : floor>=seaLevel-11 ? Zone.SHALLOWS : Zone.DEPTHS;
            for(Tree t:candidates) {
                if(Math.abs(x-t.x)<t.radius*1.7+Math.abs(t.leanX)+35 && Math.abs(z-t.z)<t.radius*1.7+Math.abs(t.leanZ)+35) trunks.add(t);
                double reach=Math.max(350,crownRadius*1.6+90);
                if(Math.abs(x-t.x)>reach||Math.abs(z-t.z)>reach)continue;
                for(Segment s:t.geometry().at(x,z)) if(s.containsXZ(x,z)) wood.add(s);
                for(Lobe l:t.leaves) if(l.containsXZ(x,z)) leaves.add(l);
            }
            if(floor>=seaLevel-32&&floor<=seaLevel+22){
                int mx=Math.floorDiv(x,48),mz=Math.floorDiv(z,48);
                for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++){
                    int tx=mx+dx,tz=mz+dz;
                    // Centres stay ten blocks inside each cell; most neighbouring cells cannot reach this column.
                    if(x<tx*48-4||x>tx*48+52||z<tz*48-4||z>tz*48+52)continue;
                    Optional<Mangrove> m=mangroves.computeIfAbsent(coordinateKey(tx,tz),key->makeMangrove(tx,tz));
                    if(m.isPresent()&&m.get().containsXZ(x,z))smallTrees.add(m.get());
                }
                trim(mangroves,1024,128);
            }
        }
        public boolean woodAt(int y) {
            for(Tree t:trunks) if(t.trunkContains(x,y,z)) return true;
            for(Segment s:wood) if(s.contains(x,y,z)) return true;
            return false;
        }
        public boolean leavesAt(int y) { for(Lobe l:leaves) if(l.contains(x,y,z)) return true; return false; }
        /** Bounded attachment search using this column's spatially indexed wood segments. */
        int ecologySurface(int low,int high,boolean underside) {
            int found=Integer.MIN_VALUE;
            for(Segment s:wood) {
                int lo=Math.max(low,(int)Math.floor(s.minY)),hi=Math.min(high,(int)Math.ceil(s.maxY));
                for(int y=underside?lo:hi;underside?y<=hi:y>=lo;y+=underside?1:-1) {
                    if(!s.contains(x,y,z))continue;
                    int next=y+(underside?-1:1);Kind k=kind(next);
                    if(y>floor && (k==Kind.AIR||k==Kind.WATER) && y>found)found=y;
                    break;
                }
            }
            return found;
        }
        /** Resin occurs mainly on hollow inner walls (1/24), with rare exterior logs (1/3072). N/E/S/W. */
        public int filledTrunkFace(int y){
            if(y<seaLevel+10||y>230)return -1;
            long sample=Math.floorMod(pointSeed(x,y,z),3072);
            if(sample%24!=0)return -1;
            int[] dx={0,1,0,-1},dz={-1,0,1,0};
            // Prioritise a face directed into the central hollow, not the outside bark or doorway edge.
            for(Tree t:trunks)if(t.hollow&&t.trunkContains(x,y,z))
                for(int i=0;i<4;i++)if(t.hollowInterior(x+dx[i],y,z+dz[i])
                    &&WorldTreeField.this.column(x+dx[i],z+dz[i]).kind(y)==Kind.AIR)return i;
            if(sample!=0)return -1;
            for(Tree t:trunks)if(t.trunkContains(x,y,z))
                for(int i=0;i<4;i++)if(!t.trunkContains(x+dx[i],y,z+dz[i])
                    &&WorldTreeField.this.column(x+dx[i],z+dz[i]).kind(y)==Kind.AIR)return i;
            return -1;
        }
        public boolean hollowAt(int y){
            for(Tree t:trunks)if(t.hollowInterior(x,y,z))return true;
            return false;
        }
        public Kind kind(int y) {
            if(y<MIN_Y || y>MAX_Y) return Kind.AIR;
            if(y==MIN_Y) return Kind.BEDROCK;
            if(y<=floor) {
                if(y<floor-3) return zone==Zone.ROOTS && y>seaLevel-16 ? Kind.DIRT : Kind.STONE;
                if(zone==Zone.ROOTS) return y==floor ? Kind.SOIL : Kind.DIRT;
                return zone==Zone.SHALLOWS ? Kind.MUD : Kind.GRAVEL;
            }
            if(woodAt(y)) return Kind.WOOD;
            if(leavesAt(y)) return Kind.LEAVES;
            for(Mangrove m:smallTrees){Kind k=m.kind(x,y,z);if(k!=Kind.AIR)return k;}
            return y<seaLevel ? Kind.WATER : Kind.AIR;
        }
        public int highestSolid(boolean includeLeaves) {
            for(int y=MAX_Y;y>floor;y--) if(woodAt(y) || includeLeaves && leavesAt(y)) return y;
            return floor;
        }
    }
    private Optional<Mangrove> makeMangrove(int cx,int cz){
        Random r=new Random(mix(cellSeed(cx,cz)^0x198fe742832ead41L));
        if(r.nextDouble()>.65)return Optional.empty();
        int x=cx*48+10+r.nextInt(29),z=cz*48+10+r.nextInt(29),floor=floorAt(x,z);
        if(floor<seaLevel-8||floor>seaLevel-2||vegetationAt(x,z)<.34)return Optional.empty();
        for(int dz=-4;dz<=4;dz+=4)for(int dx=-4;dx<=4;dx+=4)
            if(Math.abs(floorAt(x+dx,z+dz)-floor)>3)return Optional.empty();
        return Optional.of(new Mangrove(x,z,floor,r));
    }
    /** Small coordinate-addressed mangroves stitch across chunks without neighbouring-chunk writes. */
    private final class Mangrove {
        final int x,z,minY,maxY;
        final List<Segment> logs=new ArrayList<>(),roots=new ArrayList<>();
        final List<Vec> crowns=new ArrayList<>();
        final double phase;
        Mangrove(int x,int z,int floor,Random r){
            this.x=x;this.z=z;phase=r.nextDouble()*20;int low=floor+1;
            int top=seaLevel+8+r.nextInt(6);maxY=top+5;
            Vec bottom=new Vec(x,floor+1,z),fork=new Vec(x+(r.nextDouble()-.5)*4,seaLevel+5,z+(r.nextDouble()-.5)*4);
            Vec tip=fork.add((r.nextDouble()-.5)*4,top-fork.y,(r.nextDouble()-.5)*4);
            logs.add(new Segment(WorldTreeField.this,bottom,fork,1.4,1.1,phase));
            logs.add(new Segment(WorldTreeField.this,fork,tip,1.1,.7,phase));crowns.add(tip);
            for(int i=0;i<3;i++){
                double a=phase+i*Math.PI*2/3+r.nextDouble()*.7;
                Vec end=tip.add(Math.cos(a)*(4+r.nextDouble()*2),-1-r.nextDouble()*2,Math.sin(a)*(4+r.nextDouble()*2));
                logs.add(new Segment(WorldTreeField.this,fork.add(0,1,0),end,.9,.55,phase+i));crowns.add(end);
            }
            for(int i=0;i<7;i++){
                double a=phase+i*Math.PI*2/7+r.nextDouble()*.5,len=5+r.nextDouble()*5;
                int ex=(int)Math.round(x+Math.cos(a)*len),ez=(int)Math.round(z+Math.sin(a)*len);
                Vec start=fork.add(0,-1-r.nextDouble()*2,0);
                Vec knee=new Vec(x+Math.cos(a)*len*.62,seaLevel+1+r.nextDouble()*2,z+Math.sin(a)*len*.62);
                Vec foot=new Vec(ex,floorAt(ex,ez)+1,ez);
                low=Math.min(low,(int)foot.y-1);
                roots.add(new Segment(WorldTreeField.this,start,knee,.9,.85,phase+i));
                roots.add(new Segment(WorldTreeField.this,knee,foot,.85,.6,phase+i));
            }
            minY=low;
        }
        boolean containsXZ(int px,int pz){return Math.abs(px-x)<=14&&Math.abs(pz-z)<=14;}
        boolean leaf(int px,int y,int pz){
            if(y<seaLevel+3||y>maxY)return false;
            for(Vec c:crowns){
                double dx=(px-c.x)/4.5,dy=(y-c.y)/3.5,dz=(pz-c.z)/4.5,q=dx*dx+dy*dy+dz*dz;
                if(q<.65||q<1.15+(lattice(px,pz)*.15)&&((pointSeed(px,y,pz)&7)!=0))return true;
            }
            return false;
        }
        Kind kind(int px,int y,int pz){
            if(y<minY||y>maxY)return Kind.AIR;
            for(Segment s:logs)if(s.contains(px,y,pz))return Kind.MANGROVE_LOG;
            for(Segment s:roots)if(s.contains(px,y,pz))return Kind.MANGROVE_ROOTS;
            if(leaf(px,y,pz))return Kind.MANGROVE_LEAVES;
            if(y>seaLevel+2&&(pointSeed(px,0,pz)&15)==0&&leaf(px,y+1,pz))return Kind.MANGROVE_PROPAGULE;
            return Kind.AIR;
        }
    }
    public final class Tree {
        public final int cellX,cellZ;
        public final double x,z,phase,radius,rootIsland,shore,slopeWidth,stretch,leanX,leanZ,base,fork,top;
        public final boolean hollow;
        private final double[] burlY=new double[3],burlSize=new double[3];
        private final Random geometryRandom;
        private volatile Geometry geometry;
        public final List<Segment> wood=new ArrayList<>();
        public final List<Lobe> leaves=new ArrayList<>();
        Tree(int cx,int cz) {
            cellX=cx;cellZ=cz;
            Random r=new Random(cellSeed(cx,cz));
            x=(cx+.25+r.nextDouble()*.5)*spacing;
            z=(cz+.25+r.nextDouble()*.5)*spacing;
            phase=r.nextDouble()*Math.PI*2;
            radius=28+r.nextDouble()*16; rootIsland=112+r.nextDouble()*50;
            slopeWidth=88+r.nextDouble()*24;
            shore=rootIsland+slopeWidth+38+r.nextDouble()*12;
            stretch=.70+r.nextDouble()*.72;
            leanX=(r.nextDouble()-.5)*54;leanZ=(r.nextDouble()-.5)*54;
            base=seaLevel+6;fork=177+r.nextDouble()*38;top=284+r.nextDouble()*17;
            hollow=r.nextDouble()<.42;
            for(int i=0;i<3;i++){burlY[i]=base+28+r.nextDouble()*155;burlSize[i]=4+r.nextDouble()*7;}
            geometryRandom=r;
        }
        public void prepareGeometry(){geometry();}
        private Geometry geometry(){
            Geometry result=geometry;
            if(result!=null)return result;
            synchronized(this){
                if(geometry==null){buildGeometry(geometryRandom);geometry=new Geometry(wood);}
                return geometry;
            }
        }
        private void buildGeometry(Random r){
            int roots=8+r.nextInt(4);
            for(int i=0;i<roots;i++) {
                double a=phase+i*Math.PI*2/roots+(r.nextDouble()-.5)*.65;
                double turn=(r.nextDouble()-.5)*1.1;
                double len=110+r.nextDouble()*120, ex=x+Math.cos(a+turn)*len,ez=z+Math.sin(a+turn)*len;
                double endY=ownFloor(ex,ez)+2;
                Vec center=trunkCenter(base+11);
                Vec rootStart=center.add(Math.cos(a)*radius*.38,0,Math.sin(a)*radius*.38);
                Vec rootControl1=new Vec(x+Math.cos(a-turn*.6)*len*.34,seaLevel+7+r.nextDouble()*9,z+Math.sin(a-turn*.6)*len*.34);
                Vec rootControl2=new Vec(x+Math.cos(a+turn*1.3)*len*.73,lerp(.6,seaLevel,endY),z+Math.sin(a+turn*1.3)*len*.73);
                Vec rootEnd=new Vec(ex,endY,ez);
                Curve root=curve(rootStart,rootControl1,rootControl2,rootEnd,radius*.68+r.nextDouble()*7,1.3,true,r);
                for(int j=0;j<2+r.nextInt(2);j++) {
                    double at=.38+r.nextDouble()*.43,side=a+turn+(j%2==0?1:-1)*(.5+r.nextDouble()*.55);
                    Vec origin=root.point(at);
                    double length=28+r.nextDouble()*44;
                    double tx=origin.x+Math.cos(side)*length,tz=origin.z+Math.sin(side)*length;
                    Vec tip=new Vec(tx,ownFloor(tx,tz)+1,tz);
                    curve(origin,origin.add(Math.cos(side-.5)*length*.38,3,Math.sin(side-.5)*length*.38),
                        tip.add(-Math.cos(side+.4)*length*.3,3,-Math.sin(side+.4)*length*.3),tip,
                        2.5+r.nextDouble()*2,.65,true,r);
                }
            }
            double scale=crownRadius/165.0;
            Vec crown=trunkCenter(top-4);
            leaves.add(new Lobe(crown.x,top-1,crown.z,72*scale,17,65*scale,phase));
            int branches=6+r.nextInt(3);
            for(int i=0;i<branches;i++) {
                double a=phase+i*Math.PI*2/branches+(r.nextDouble()-.5)*.75;
                double turn=(r.nextDouble()-.5)*1.0;
                double len=(85+r.nextDouble()*45)*scale;
                Vec begin=trunkCenter(fork+r.nextDouble()*35);
                Vec end=new Vec(crown.x+Math.cos(a+turn)*len,Math.min(296,top-26+r.nextDouble()*30),crown.z+Math.sin(a+turn)*len);
                Vec control1=begin.add(Math.cos(a-turn*.65)*len*.36,6+r.nextDouble()*30,Math.sin(a-turn*.65)*len*.36);
                Vec control2=end.add(-Math.cos(a+turn*1.5)*len*.4,-8-r.nextDouble()*16,-Math.sin(a+turn*1.5)*len*.4);
                Curve main=curve(begin,control1,control2,end,11+r.nextDouble()*9,1.4,false,r);
                leaves.add(new Lobe(end.x,end.y+5,end.z,(43+r.nextDouble()*20)*scale,17+r.nextDouble()*9,(40+r.nextDouble()*23)*scale,a));
                for(int j=0;j<3;j++) {
                    double at=.36+r.nextDouble()*.5;
                    Vec s=main.point(at);
                    double aa=a+turn+(j%2==0?1:-1)*(.5+r.nextDouble()*.65), extension=(32+r.nextDouble()*30)*scale;
                    Vec rawTip=s.add(Math.cos(aa)*extension,8+r.nextDouble()*29,Math.sin(aa)*extension);
                    Vec tip=new Vec(rawTip.x,Math.min(297,rawTip.y),rawTip.z);
                    Curve secondary=curve(s,s.add(Math.cos(aa-.35)*extension*.45,-5+r.nextDouble()*18,Math.sin(aa-.35)*extension*.45),
                        tip.add(-Math.cos(aa+.35)*extension*.3,-8,-Math.sin(aa+.35)*extension*.3),tip,4+r.nextDouble()*2,.7,false,r);
                    leaves.add(new Lobe(tip.x,tip.y+5,tip.z,(26+r.nextDouble()*16)*scale,13+r.nextDouble()*9,(27+r.nextDouble()*16)*scale,aa));
                    Vec twigStart=secondary.point(.66);
                    Vec twigEnd=twigStart.add(Math.cos(aa+.9)*20*scale,7,Math.sin(aa+.9)*20*scale);
                    curve(twigStart,twigStart.add(4,2,5),twigEnd.add(-3,-2,-4),twigEnd,1.7,.5,false,r);
                }
            }
        }
        private double ownFloor(double px,double pz) {
            return Math.max(-61,terrain(this,px,pz));
        }
        private Vec trunkCenter(double y) {
            double t=Math.max(0,(y-base)/(top-base));
            double bend=Math.sin(Math.PI*t);
            return new Vec(x+leanX*t+bend*(15*Math.sin(t*7+phase)+9*noise(t*7+phase,phase+2)),y,
                z+leanZ*t+bend*(15*Math.cos(t*6-phase)+9*noise(phase-3,t*7+phase)));
        }
        Vec ecologyTrunkSurface(int y,double angle){
            Vec center=trunkCenter(y);
            for(int d=(int)(radius*2.2);d>0;d--){
                int px=(int)Math.round(center.x+Math.cos(angle)*d),pz=(int)Math.round(center.z+Math.sin(angle)*d);
                if(trunkContains(px,y,pz))return new Vec(px,y,pz);
            }
            return null;
        }
        private boolean hollowInterior(double px,double y,double pz){
            if(!hollow||y<=base+3||y>=fork+5)return false;
            Vec c=trunkCenter(y);double t=Math.max(0,(y-base)/(top-base));
            double taper=Math.max(14,radius*(1-.70*Math.pow(t,.83)));
            return Math.hypot(px-c.x,pz-c.z)<taper*(.37+.06*Math.sin(y*.12+phase));
        }
        private boolean trunkContains(double px,double y,double pz) {
            if(y<base-6 || y>top-2) return false;
            Vec c=trunkCenter(y);
            double t=Math.max(0,(y-base)/(top-base));
            double angle=Math.atan2(pz-c.z,px-c.x);
            double taper=Math.max(14,radius*(1-.70*Math.pow(t,.83)));
            double burls=0;
            for(int i=0;i<burlY.length;i++){double q=(y-burlY[i])/13;burls+=burlSize[i]*Math.exp(-q*q);}
            double buttress=radius*.48*Math.exp(-Math.pow((y-base)/30,2));
            double rr=(taper+burls+buttress)*(1+.18*Math.sin(angle*5+phase+y*.037)+.08*Math.cos(angle*11-y*.071))
                +2.5*woodNoise(px,y,pz,phase);
            double d=Math.hypot(px-c.x,pz-c.z);
            if(d>rr) return false;
            if(hollow && y>base+3 && y<fork+5) {
                if(d<taper*(.37+.06*Math.sin(y*.12+phase))) return false;
                double opening=Math.atan2(Math.sin(angle-phase-.12*Math.sin(y*.08)),Math.cos(angle-phase-.12*Math.sin(y*.08)));
                if(y>base+7 && y<base+51+5*Math.sin(angle*3+y*.09) && Math.abs(opening)<.25+.06*Math.sin(y*.19)) return false;
            }
            return true;
        }
        private Curve curve(Vec a,Vec b,Vec c,Vec d,double r0,double r1,boolean root,Random random) {
            Curve path=new Curve(this,a,b,c,d,root,random.nextDouble()*1000,Math.min(root?8:13,a.distance(d)*.09));
            int steps=Math.max(12,(int)Math.ceil((a.distance(b)+b.distance(c)+c.distance(d))/3.5));
            Vec previous=path.point(0);double previousRadius=r0;
            for(int i=1;i<=steps;i++) {
                double t=(double)i/steps;Vec next=path.point(t);
                double size=lerp(Math.pow(t,.72),r0,r1)*(1+.13*Math.sin(Math.PI*t)*Math.sin(t*19+path.phase));
                wood.add(new Segment(WorldTreeField.this,previous,next,previousRadius,size,path.phase));
                previous=next;previousRadius=size;
            }
            return path;
        }
    }
    private final class Curve {
        private final Tree owner;
        private final Vec a,b,c,d;
        private final boolean root;
        private final double phase,amplitude;
        private Curve(Tree owner,Vec a,Vec b,Vec c,Vec d,boolean root,double phase,double amplitude){
            this.owner=owner;this.a=a;this.b=b;this.c=c;this.d=d;this.root=root;this.phase=phase;this.amplitude=amplitude;
        }
        private Vec point(double t){
            Vec p=bezier(a,b,c,d,t);double envelope=Math.sin(Math.PI*t);
            p=p.add(amplitude*envelope*noise(t*8+phase,phase+5),
                amplitude*.55*envelope*noise(phase-3,t*7+phase),
                amplitude*envelope*noise(t*8-phase,phase-7));
            if(root) {
                double ground=owner.ownFloor(p.x,p.z)+2+4*Math.sin(Math.PI*t);
                p=new Vec(p.x,lerp(.84*smooth(t*2),p.y,ground),p.z);
            }
            return p;
        }
    }
    public record Vec(double x,double y,double z) {
        Vec add(double dx,double dy,double dz){return new Vec(x+dx,y+dy,z+dz);}
        double distance(Vec other){return Math.sqrt((x-other.x)*(x-other.x)+(y-other.y)*(y-other.y)+(z-other.z)*(z-other.z));}
    }
    private static Vec bezier(Vec a,Vec b,Vec c,Vec d,double t) {
        double u=1-t;return new Vec(u*u*u*a.x+3*u*u*t*b.x+3*u*t*t*c.x+t*t*t*d.x,
            u*u*u*a.y+3*u*u*t*b.y+3*u*t*t*c.y+t*t*t*d.y,
            u*u*u*a.z+3*u*u*t*b.z+3*u*t*t*c.z+t*t*t*d.z);
    }
    /** Immutable 16x16 spatial bins; segment order and exact containment math are preserved. */
    private static final class Geometry {
        final int minX,minZ,width,depth;
        final Segment[][] bins;
        @SuppressWarnings("unchecked") Geometry(List<Segment> segments){
            int x0=Integer.MAX_VALUE,z0=Integer.MAX_VALUE,x1=Integer.MIN_VALUE,z1=Integer.MIN_VALUE;
            for(Segment s:segments){
                x0=Math.min(x0,(int)Math.floor(s.minX/16));z0=Math.min(z0,(int)Math.floor(s.minZ/16));
                x1=Math.max(x1,(int)Math.floor(s.maxX/16));z1=Math.max(z1,(int)Math.floor(s.maxZ/16));
            }
            minX=x0;minZ=z0;width=x1-x0+1;depth=z1-z0+1;
            List<Segment>[] building=(List<Segment>[])new List<?>[width*depth];
            for(Segment s:segments)for(int z=(int)Math.floor(s.minZ/16);z<=(int)Math.floor(s.maxZ/16);z++)
                for(int x=(int)Math.floor(s.minX/16);x<=(int)Math.floor(s.maxX/16);x++){
                    int i=(z-minZ)*width+x-minX;
                    if(building[i]==null)building[i]=new ArrayList<>();building[i].add(s);
                }
            bins=new Segment[building.length][];
            for(int i=0;i<bins.length;i++)bins[i]=building[i]==null?NO_SEGMENTS:building[i].toArray(Segment[]::new);
        }
        Segment[] at(int x,int z){
            int bx=Math.floorDiv(x,16)-minX,bz=Math.floorDiv(z,16)-minZ;
            return bx<0||bz<0||bx>=width||bz>=depth?NO_SEGMENTS:bins[bz*width+bx];
        }
    }
    public static final class Segment {
        private final WorldTreeField field;
        final Vec a,b;final double ra,rb,dx,dy,dz,length2,r,phase;
        final double minX,maxX,minZ,maxZ,minY,maxY;
        Segment(WorldTreeField field,Vec a,Vec b,double ra,double rb,double phase){
            this.field=field;this.a=a;this.b=b;this.ra=ra;this.rb=rb;this.phase=phase;
            dx=b.x-a.x;dy=b.y-a.y;dz=b.z-a.z;length2=Math.max(1e-8,dx*dx+dy*dy+dz*dz);r=Math.max(ra,rb)*1.35+2;
            minX=Math.min(a.x,b.x)-r;maxX=Math.max(a.x,b.x)+r;
            minZ=Math.min(a.z,b.z)-r;maxZ=Math.max(a.z,b.z)+r;
            minY=Math.min(a.y,b.y)-r;maxY=Math.max(a.y,b.y)+r;
        }
        boolean containsXZ(double x,double z){return x>=minX&&x<=maxX&&z>=minZ&&z<=maxZ;}
        boolean contains(double x,double y,double z){
            if(y<minY||y>maxY)return false;
            double t=Math.max(0,Math.min(1,((x-a.x)*dx+(y-a.y)*dy+(z-a.z)*dz)/length2));
            double xx=x-(a.x+t*dx), yy=y-(a.y+t*dy),zz=z-(a.z+t*dz),radius=lerp(t,ra,rb);
            if(xx*xx+yy*yy+zz*zz>r*r)return false;
            // Growth ridges, knots and correlated block-scale irregularity break the tube silhouette.
            radius*=1+.14*Math.sin(x*.17+y*.11+z*.13+phase)+.075*Math.cos(x*.31-y*.19+z*.23-phase);
            radius+=Math.min(2.1,radius*.19)*field.woodNoise(x,y,z,phase);
            return xx*xx+yy*yy+zz*zz<=radius*radius;
        }
    }
    public final class Lobe {
        final double x,y,z,rx,ry,rz,phase;
        private final long fringeSeed;
        private final LeafPatch[] patches;
        Lobe(double x,double y,double z,double rx,double ry,double rz,double phase){
            this.x=x;this.y=Math.min(y,MAX_Y-2-ry*1.25);this.z=z;this.rx=rx;this.ry=ry;this.rz=rz;this.phase=phase;
            fringeSeed=mix(Double.doubleToLongBits(phase));
            Random random=new Random(mix(seed^Double.doubleToLongBits(x)^Long.rotateLeft(Double.doubleToLongBits(z),23)^Double.doubleToLongBits(phase)));
            patches=new LeafPatch[7];
            patches[0]=new LeafPatch(x,this.y-ry*.1,z,rx*.60,ry*.86,rz*.60);
            for(int i=1;i<patches.length;i++){
                double angle=phase+(i-1)*Math.PI/3+(random.nextDouble()-.5)*.4;
                patches[i]=new LeafPatch(x+Math.cos(angle)*rx*.53,this.y+(random.nextDouble()-.5)*ry*.5,z+Math.sin(angle)*rz*.53,
                    rx*(.38+random.nextDouble()*.14),ry*(.55+random.nextDouble()*.27),rz*(.38+random.nextDouble()*.14));
            }
        }
        boolean containsXZ(double px,double pz){return Math.abs(px-x)<rx*1.18&&Math.abs(pz-z)<rz*1.18;}
        boolean contains(double px,double py,double pz){
            if(Math.abs(py-y)>ry*1.25)return false;
            double perturb=Double.NaN;
            for(LeafPatch patch:patches){
                double dx=(px-patch.x)/patch.rx,dy=(py-patch.y)/patch.ry,dz=(pz-patch.z)/patch.rz;
                double q=dx*dx+dy*dy+dz*dz;
                if(q>1.42)continue;
                if(q<.56)return true;
                if(Double.isNaN(perturb))perturb=.16*noise(px*.055+phase,pz*.055-py*.04)
                    +.14*woodNoise(px*.62,py*.62,pz*.62,phase)
                    +.12*((mix(pointSeed(Math.floorDiv((int)Math.floor(px),3),Math.floorDiv((int)Math.floor(py),2),Math.floorDiv((int)Math.floor(pz),3))^fringeSeed)>>>11)*0x1.0p-53*2-1);
                if(q<1+perturb)return true;
            }
            return false;
        }
    }
    private record LeafPatch(double x,double y,double z,double rx,double ry,double rz){}
}
