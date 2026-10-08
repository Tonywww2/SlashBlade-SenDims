package com.tonywww.slashblade_sendims.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import java.util.*;

/** A garden is planned once, then each generating chunk only reads its own immutable slice. */
public final class AsteroidBeltGarden {
    public record Voxel(BlockPos pos,BlockState state){}
    public record Plan(Map<Long,List<Voxel>> chunks,int trees){}
    public static Plan create(AsteroidBeltField.Body b){
        Map<BlockPos,BlockState> blocks=new HashMap<>();int trees=0;
        Block[] woods={Blocks.OAK_LOG,Blocks.BIRCH_LOG,Blocks.SPRUCE_LOG,Blocks.CHERRY_LOG};
        Block[] foliage={Blocks.OAK_LEAVES,Blocks.BIRCH_LEAVES,Blocks.SPRUCE_LEAVES,Blocks.CHERRY_LEAVES};
        for(int i=0;i<24;i++){
            long h=AsteroidBeltField.mix(b.id+i*413L);double a=AsteroidBeltField.unit(h)*Math.PI*2,r=Math.sqrt(AsteroidBeltField.unit(h+1))*.58;
            int x=(int)Math.round(b.x+Math.cos(a)*b.rx*r),z=(int)Math.round(b.z+Math.sin(a)*b.rz*r),floor=b.floor(x,z);
            if(b.water(x,z)||b.sample(x,floor,z)!=AsteroidBeltField.Material.GRASS)continue;
            boolean near=false;for(var e:blocks.entrySet())if(e.getValue().is(woods[0])||e.getValue().is(woods[1])||e.getValue().is(woods[2])||e.getValue().is(woods[3])){
                if(e.getKey().getY()==floor+1&&e.getKey().distSqr(new BlockPos(x,floor+1,z))<64){near=true;break;}
            }if(near)continue;
            int variant=(int)(AsteroidBeltField.unit(h+2)*4),height=7+(int)(AsteroidBeltField.unit(h+3)*6);
            // Keep a natural tree when the reduced garden cannot accommodate its original height.
            while(height>5&&(!b.inGarden(x,floor+height+4,z)||b.sample(x,floor+height+4,z)!=AsteroidBeltField.Material.AIR))height--;
            if(!b.inGarden(x,floor+height+4,z)||b.sample(x,floor+height+4,z)!=AsteroidBeltField.Material.AIR)continue;
            int radius=variant==2||height<=6?3:4,reach=Math.min(3,height/2);
            BlockState wood=woods[variant].defaultBlockState(),leaves=foliage[variant].defaultBlockState().setValue(BlockStateProperties.PERSISTENT,true);
            for(int dy=1;dy<=height;dy++)put(blocks,b,x,floor+dy,z,wood,true);
            if(variant==2){
                for(int dy=height-6;dy<=height+1;dy++){
                    int spread=Math.max(1,3-(dy-height+6)/3);
                    for(int dz=-spread;dz<=spread;dz++)for(int dx=-spread;dx<=spread;dx++)if(Math.abs(dx)+Math.abs(dz)<=spread+1)put(blocks,b,x+dx,floor+dy,z+dz,leaves,false);
                }
            }else{
                for(int branch=0;branch<3;branch++){
                    double angle=branch*Math.PI*2/3+AsteroidBeltField.unit(h+4)*6;
                    int ex=x+(int)Math.round(Math.cos(angle)*reach),ez=z+(int)Math.round(Math.sin(angle)*reach),ey=floor+height-1+branch%2;
                    for(int s=1;s<=4;s++)put(blocks,b,x+(ex-x)*s/4,floor+height-4+(ey-floor-height+4)*s/4,z+(ez-z)*s/4,wood,true);
                    for(int dz=-radius;dz<=radius;dz++)for(int dx=-radius;dx<=radius;dx++)for(int dy=-2;dy<=2;dy++){
                        double q=(dx*dx+dz*dz)/(double)(radius*radius)+dy*dy/5.0;
                        if(q<1+AsteroidBeltField.noise(h+9,(ex+dx)*.45,(ey+dy)*.45,(ez+dz)*.45)*.24)put(blocks,b,ex+dx,ey+dy,ez+dz,leaves,false);
                    }
                }
            }
            trees++;
        }
        for(int z=(int)(b.z-b.rz*.72);z<=b.z+b.rz*.72;z++)for(int x=(int)(b.x-b.rx*.72);x<=b.x+b.rx*.72;x++){
            int y=b.floor(x,z);var ground=b.sample(x,y,z);long h=AsteroidBeltField.hash(b.id+801,x,0,z);
            if(ground==AsteroidBeltField.Material.GLOWSTONE){if(AsteroidBeltField.unit(h)<.65)put(blocks,b,x,y+1,z,Blocks.MOSS_CARPET.defaultBlockState(),false);continue;}
            if(ground!=AsteroidBeltField.Material.GRASS||b.water(x,z)||AsteroidBeltField.unit(h)>.36)continue;
            Block plant=AsteroidBeltField.unit(h+1)<.70?Blocks.GRASS:AsteroidBeltField.unit(h+2)<.45?Blocks.FERN:
                switch((int)(AsteroidBeltField.unit(h+3)*5)){case 0->Blocks.AZURE_BLUET;case 1->Blocks.OXEYE_DAISY;case 2->Blocks.ALLIUM;case 3->Blocks.LILY_OF_THE_VALLEY;default->Blocks.DANDELION;};
            put(blocks,b,x,y+1,z,plant.defaultBlockState(),false);
        }
        Map<Long,List<Voxel>> chunks=new HashMap<>();
        blocks.entrySet().stream().sorted(Comparator.comparingLong(e->e.getKey().asLong())).forEach(e->chunks.computeIfAbsent(AsteroidBeltField.key(e.getKey().getX()>>4,e.getKey().getZ()>>4),k->new ArrayList<>()).add(new Voxel(e.getKey(),e.getValue())));
        chunks.replaceAll((k,v)->List.copyOf(v));return new Plan(Map.copyOf(chunks),trees);
    }
    private static void put(Map<BlockPos,BlockState> out,AsteroidBeltField.Body b,int x,int y,int z,BlockState state,boolean replace){
        if(!b.inGarden(x,y,z)||b.sample(x,y,z)!=AsteroidBeltField.Material.AIR||y<b.floor(x,z)+1)return;
        BlockPos p=new BlockPos(x,y,z);if(replace)out.put(p,state);else out.putIfAbsent(p,state);
    }
}
