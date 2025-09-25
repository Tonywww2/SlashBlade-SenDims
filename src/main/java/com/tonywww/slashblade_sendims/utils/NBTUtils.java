package com.tonywww.slashblade_sendims.utils;

import net.minecraft.nbt.CompoundTag;

public class NBTUtils {
    public static int getSpecificField(CompoundTag tag, String field) {
        if (!tag.contains(field)) {
            tag.putInt(field, 0);
        }
        return tag.getInt(field);
    }
}
