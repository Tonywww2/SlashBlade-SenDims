package com.tonywww.slashblade_sendims.utils;

import net.minecraft.nbt.CompoundTag;

public class NBTUtils {
    public static int getSpecificIntField(CompoundTag tag, String field) {
        if (!tag.contains(field)) {
            tag.putInt(field, 0);
        }
        return tag.getInt(field);
    }
    public static boolean getSpecificBoolField(CompoundTag tag, String field) {
        if (!tag.contains(field)) {
            tag.putBoolean(field, false);
        }
        return tag.getBoolean(field);
    }
}
