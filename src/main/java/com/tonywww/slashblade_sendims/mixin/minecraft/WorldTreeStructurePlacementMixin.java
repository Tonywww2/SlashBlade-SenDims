package com.tonywww.slashblade_sendims.mixin.minecraft;

import com.tonywww.slashblade_sendims.worldgen.WorldTreeSeaChunkGenerator;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.function.Predicate;

@Mixin(Structure.class)
public abstract class WorldTreeStructurePlacementMixin {
    @Inject(method="generate",at=@At("RETURN"),cancellable=true)
    private void worldTree$checkFootprint(RegistryAccess registries,ChunkGenerator generator,BiomeSource source,RandomState state,
            StructureTemplateManager templates,long seed,ChunkPos chunk,int references,LevelHeightAccessor heights,
            Predicate<Holder<Biome>> biomes,CallbackInfoReturnable<StructureStart> cir){
        if(!(generator instanceof WorldTreeSeaChunkGenerator trees))return;
        StructureStart start=cir.getReturnValue();if(start==null||!start.isValid())return;
        ResourceLocation id=registries.registryOrThrow(Registries.STRUCTURE).getKey((Structure)(Object)this);
        if(id==null)return;
        String path=id.getPath();
        boolean city=id.toString().equals("cataclysm:sunken_city");
        boolean ship=path.equals("illager_corsair")||path.equals("illager_galley")||path.equals("undead_pirate_ship")||path.equals("kraken_ship");
        if(city) {
            BoundingBox box=start.getBoundingBox();
            // Align the whole city to the highest sampled support under its actual pieces.
            // Rolling seabeds must not bury lower rooms on the uphill side.
            int ground=trees.getMinY()+1;
            for(StructurePiece piece:start.getPieces()){
                BoundingBox footprint=piece.getBoundingBox();
                for(int z=footprint.minZ();z<=footprint.maxZ();z+=12)for(int x=footprint.minX();x<=footprint.maxX();x+=12)
                    ground=Math.max(ground,trees.field().floorAt(x,z));
            }
            int dy=ground+1-box.minY();
            for(StructurePiece piece:start.getPieces())piece.move(0,dy,0);
            start=new StructureStart((Structure)(Object)this,chunk,references,new PiecesContainer(start.getPieces()));
            cir.setReturnValue(start);
        }
        if(id.toString().equals("cataclysm:amethyst_nest")) {
            BoundingBox box=start.getBoundingBox();BlockPos center=box.getCenter();
            int ground=trees.field().floorAt(center.getX(),center.getZ());
            if(ground<trees.getSeaLevel()){cir.setReturnValue(StructureStart.INVALID_START);return;}
            int dy=ground-box.minY();
            for(StructurePiece piece:start.getPieces())piece.move(0,dy,0);
            // Rebuild the start so the bounding-box cache includes the terrain adjustment.
            start=new StructureStart((Structure)(Object)this,chunk,references,new PiecesContainer(start.getPieces()));
            box=start.getBoundingBox();
            for(int z=box.minZ();z<=box.maxZ();z+=3)for(int x=box.minX();x<=box.maxX();x+=3){
                var column=trees.field().column(x,z);
                if(Math.abs(column.floor-ground)>4){cir.setReturnValue(StructureStart.INVALID_START);return;}
                for(int y=ground+2;y<=box.maxY();y+=2)if(column.woodAt(y)){
                    cir.setReturnValue(StructureStart.INVALID_START);return;
                }
            }
            cir.setReturnValue(start);
        }
        if(city||ship)for(StructurePiece piece:start.getPieces())
            if(!trees.acceptsWaterStructure(piece.getBoundingBox(),city)){
                cir.setReturnValue(StructureStart.INVALID_START);return;
            }
    }
}
