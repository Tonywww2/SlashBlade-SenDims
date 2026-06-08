package com.tonywww.slashblade_sendims.utils;

import se.mickelus.tetra.module.data.MaterialData;

public interface IMaterialData {
    static IMaterialData cast(MaterialData data) {
        return (IMaterialData) data;
    }
    float slashBlade_SenDims$getCountFactor();
    void slashBlade_SenDims$setCountFactor(float factor);
}
