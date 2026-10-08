package com.tonywww.slashblade_sendims.worldgen;

import java.nio.file.*;
import java.io.*;
import java.util.*;

/** Geometry regression and sampled cross-section; intentionally independent of Minecraft. */
public final class WorldTreeFieldSelfTest {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception {
        long start=System.nanoTime();
        WorldTreeField f=new WorldTreeField(20261004,64,360,289);
        Random r=new Random(90210);
        int[] zones=new int[3],covered=new int[3],slopes=new int[256];int bottomMin=999,bottomMax=-999,maxLeaf=-999,maxWood=-999,slopeSamples=0;
        int mangroveLogs=0,mangroveLeaves=0,mangroveRoots=0;
        for(int i=0;i<9000;i++) {
            int x=r.nextInt(6000)-3000,z=r.nextInt(6000)-3000;
            var c=f.column(x,z);zones[c.zone.ordinal()]++;
            boolean canopy=false;
            for(int y=190;y<=319;y++) {
                if(c.leavesAt(y)){canopy=true;maxLeaf=Math.max(maxLeaf,y);}
                if(c.woodAt(y))maxWood=Math.max(maxWood,y);
            }
            if(canopy)covered[c.zone.ordinal()]++;
            for(int y=50;y<=85;y++)switch(c.kind(y)){
                case MANGROVE_LOG->mangroveLogs++;
                case MANGROVE_LEAVES->mangroveLeaves++;
                case MANGROVE_ROOTS->mangroveRoots++;
                default->{}
            }
            if(c.floor==(int)Math.floor(f.seabedAt(x,z))){bottomMin=Math.min(bottomMin,c.floor);bottomMax=Math.max(bottomMax,c.floor);}
            if(c.floor>-56&&c.floor<52){
                int delta=Math.max(Math.abs(c.floor-f.floorAt(x+1,z)),Math.abs(c.floor-f.floorAt(x,z+1)));
                slopes[Math.min(255,delta)]++;slopeSamples++;
            }
            check(c.kind(-64)==WorldTreeField.Kind.BEDROCK,"missing bedrock");
            check(c.kind(320)==WorldTreeField.Kind.AIR,"out of dimension range");
            check(!c.leavesAt(319)&&!c.woodAt(319),"tree hits build ceiling at "+x+","+z);
            check(c.floor>=-61,"seabed below two-block substrate");
            // Chunk-center candidate selection must equal coordinate selection at grid boundaries.
            var chunk=f.column(x,z,f.nearby(Math.floorDiv(x,16)*16+8,Math.floorDiv(z,16)*16+8));
            for(int y=-64;y<=319;y+=7)check(c.kind(y)==chunk.kind(y),"chunk seam at "+x+","+z+","+y);
        }
        // Rebuilding the cache in a different order, including negatives, must not change geometry.
        WorldTreeField other=new WorldTreeField(20261004,64,360,289);
        for(int i=2000;i>=0;i--)other.tree(i%65-32,i/65-16);
        r=new Random(90210);
        for(int i=0;i<256;i++) {
            int x=r.nextInt(6000)-3000,z=r.nextInt(6000)-3000;
            var a=f.column(x,z);var b=other.column(x,z);
            for(int y=-64;y<=319;y++)check(a.kind(y)==b.kind(y),"order-dependent generation");
        }
        WorldTreeField concurrent=new WorldTreeField(20261004,64,360,289);
        var pool=java.util.concurrent.Executors.newFixedThreadPool(4);
        try{
            List<java.util.concurrent.Future<?>> tasks=new ArrayList<>();
            for(int worker=0;worker<4;worker++)tasks.add(pool.submit(()->{
                for(int i=0;i<128;i++){
                    int x=9400+i%16*5,z=9400+i/16*7;
                    var cached=concurrent.column(x,z);var fresh=concurrent.column(x,z,concurrent.nearby(x,z));
                    for(int y=-64;y<=319;y++)check(cached.kind(y)==fresh.kind(y),"concurrent cache mismatch");
                }
            }));
            for(var task:tasks)task.get();
        }finally{pool.shutdown();}
        for(int i=0;i<3;i++)check(zones[i]>400,"missing biome zone "+i);
        check(bottomMin>=-61&&bottomMin<=-53&&bottomMax>=-33&&bottomMax-bottomMin>=22,"deep ridges and hollows missing: "+bottomMin+".."+bottomMax);
        check(mangroveLogs>0&&mangroveLeaves>0&&mangroveRoots>0,"mangrove geometry missing");
        check((double)covered[0]/zones[0]>.85,"root canopy too sparse");
        check((double)covered[1]/zones[1]>.65,"shallow canopy too sparse");
        check((double)covered[2]/zones[2]>.08&&(double)covered[2]/zones[2]<.95,"deep canopy must have gaps: "+covered[2]+" / "+zones[2]);
        check(maxLeaf>=310&&maxLeaf<=317,"unexpected crown height "+maxLeaf);
        var t=f.tree(0,0);int centerX=(int)Math.round(t.x),centerZ=(int)Math.round(t.z);
        Path out=Path.of(args.length>0?args[0]:".");Files.createDirectories(out);
        try(var w=Files.newBufferedWriter(out.resolve("section.ppm"))) {
            int width=1080;w.write("P3\n"+width+" 384\n255\n");
            List<WorldTreeField.Column> cols=new ArrayList<>();
            for(int x=0;x<width;x++)cols.add(f.column(centerX+x-width/2,centerZ));
            for(int y=319;y>=-64;y--)for(var c:cols){
                int[] rgb=switch(c.kind(y)){
                    case AIR->new int[]{160,193,207};case WATER->new int[]{24,77,98};
                    case BEDROCK->new int[]{20,24,30};case WOOD->new int[]{111, 70,37};
                    case LEAVES->new int[]{47,100,59};case MANGROVE_LOG->new int[]{90,62,44};case MANGROVE_ROOTS->new int[]{96,79,58};case MANGROVE_LEAVES,MANGROVE_PROPAGULE->new int[]{66,131,51};case SOIL->new int[]{111,113,53};
                    case MUD->new int[]{85,72,62};case GRAVEL->new int[]{100,110,118};
                    case DIRT->new int[]{104,82,57};case STONE->new int[]{58,63,74};};
                w.write(rgb[0]+" "+rgb[1]+" "+rgb[2]+"\n");
            }
        }
        try(var w=Files.newBufferedWriter(out.resolve("map.csv"))){
            w.write("x,z,zone,canopy,floor\n");
            for(int z=-900;z<900;z+=6)for(int x=-900;x<900;x+=6){
                var c=f.column(x,z);boolean canopy=false;
                for(int y=220;y<=317&&!canopy;y+=3)canopy=c.leavesAt(y);
                w.write(x+","+z+","+c.zone.ordinal()+","+(canopy?1:0)+","+c.floor+"\n");
            }
        }
        String report="Tree center="+centerX+","+centerZ+" hollow="+t.hollow+"\n";
        for(int i=0;i<3;i++)report+=WorldTreeField.Zone.values()[i]+": samples="+zones[i]+", canopy="+String.format(Locale.ROOT,"%.1f%%",100.0*covered[i]/zones[i])+"\n";
        report+="deep floor="+bottomMin+".."+bottomMax+", crown max="+maxLeaf+", wood max="+maxWood+"\n";
        report+="mangrove samples: logs="+mangroveLogs+", roots="+mangroveRoots+", leaves="+mangroveLeaves+"\n";
        int sum=0,p99=0,maxSlope=0;
        for(int i=0;i<slopes.length;i++){sum+=slopes[i];if(sum<slopeSamples*.99)p99=i+1;if(slopes[i]>0)maxSlope=i;}
        check(slopeSamples>1000,"insufficient ocean-slope samples");
        check(p99<=9&&maxSlope<=14,"ocean slope still has abrupt cliffs: "+p99+" / "+maxSlope);
        report+="ocean slope: "+slopeSamples+" samples, adjacent-column delta p99="+p99+", max="+maxSlope+"\n";
        report+="9000 columns + seams + cache-order assertions passed; seconds="+(System.nanoTime()-start)/1e9+"\n";
        report+="4 workers: cold geometry publication and cached/fresh columns match\n";
        Files.writeString(out.resolve("geometry-report.txt"),report);System.out.print(report);
    }
}
