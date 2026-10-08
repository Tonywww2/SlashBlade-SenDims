package com.tonywww.slashblade_sendims.worldgen;

import java.nio.file.*;
import java.util.Locale;

/** Fixed coordinates and checksums make speed measurements comparable without changing terrain. */
public final class WorldTreePerformanceProbe {
    private static volatile long sink;
    private static long hash(long h,int value){return (h^value)*0x100000001b3L;}
    private static long full(WorldTreeField f){
        long h=0xcbf29ce484222325L;
        for(int i=0;i<64;i++){
            int cx=-32+(i%8)*12,cz=-32+(i/8)*12;
            var nearby=f.nearby(cx*16+8,cz*16+8);
            for(int z=0;z<16;z++)for(int x=0;x<16;x++){
                var c=f.column(cx*16+x,cz*16+z,nearby);h=hash(h,c.floor);
                for(int y=-64;y<=319;y++)h=hash(h,c.kind(y).ordinal());
            }
        }
        sink=h;return h;
    }
    public static void main(String[] args)throws Exception {
        WorldTreeField f=new WorldTreeField(20261004,64,360,289);
        long start=System.nanoTime(),floorHash=0;
        for(int i=0;i<64;i++)for(int z=0;z<16;z++)for(int x=0;x<16;x++)
            floorHash=hash(floorHash,f.floorAt((-32+i%8*12)*16+x,(-32+i/8*12)*16+z));
        double floor=(System.nanoTime()-start)/1e6;
        start=System.nanoTime();long cold=full(f);double coldMs=(System.nanoTime()-start)/1e6;
        start=System.nanoTime();long warm=full(f);double warmMs=(System.nanoTime()-start)/1e6;
        if(cold!=warm)throw new AssertionError("cache changes terrain");
        String report=String.format(Locale.ROOT,"64 chunks / 16384 columns / 6291456 block samples\nfloor_cold_ms=%.3f\ngeometry_cold_ms=%.3f\ngeometry_warm_ms=%.3f\nfloor_hash=%016x\nblock_hash=%016x\n",floor,coldMs,warmMs,floorHash,cold);
        Path out=Path.of(args[0]);Files.createDirectories(out);Files.writeString(out.resolve("performance.txt"),report);System.out.print(report);
    }
}
