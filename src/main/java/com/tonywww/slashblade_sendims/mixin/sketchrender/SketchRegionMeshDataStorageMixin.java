package com.tonywww.slashblade_sendims.mixin.sketchrender;

import com.tonywww.slashblade_sendims.client.performance.SketchIndexCapacity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * SketchRender 0.1.0a：区块网格数据写入后（setMeshes、removeMeshes、replaceIndexBuffer 都经过 updateSectionMesh），
 * 读同一个区块数据指针，在 CPU 上记下它的索引数，见 {@link SketchIndexCapacity}。
 * 目标按类名指定，没装 SketchRender 时由 mixin 插件跳过，注入不上也不报错。
 */
@Pseudo
@Mixin(targets = "rogo.sketch.compat.sodium.RegionMeshDataStorage", remap = false)
public abstract class SketchRegionMeshDataStorageMixin {
    @ModifyArg(method = "updateSectionMesh",
            at = @At(value = "INVOKE",
                    target = "Lme/jellysquid/mods/sodium/client/render/chunk/data/SectionRenderDataUnsafe;getSliceMask(J)I"),
            index = 0, remap = false, require = 0)
    private long sendims$recordElementCount(long sectionData) {
        SketchIndexCapacity.record(sectionData);
        return sectionData;
    }
}
