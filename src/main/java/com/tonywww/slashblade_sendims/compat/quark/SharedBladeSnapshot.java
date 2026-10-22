package com.tonywww.slashblade_sendims.compat.quark;

import javax.annotation.Nullable;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

/** Full Forge stack data carried only inside Quark's disposable chat snapshot. */
public final class SharedBladeSnapshot {
    public static final String SNAPSHOT_TAG = "slashblade_sendims:shared_blade";

    private SharedBladeSnapshot() {
    }

    public static ItemStack forSharing(ItemStack source) {
        if (!(source.getItem() instanceof ItemSlashBlade)) {
            return source;
        }

        // getShareTag can update a blade's tag. Never let packet encoding touch
        // the inventory stack, and preserve capabilities omitted by that tag.
        ItemStack shared = source.copy();
        CompoundTag saved = shared.save(new CompoundTag());
        if (saved.contains("tag", Tag.TAG_COMPOUND)) {
            saved.getCompound("tag").remove(SNAPSHOT_TAG);
        }
        shared.getOrCreateTag().put(SNAPSHOT_TAG, saved);
        return shared;
    }

    @Nullable
    public static CompoundTag readSnapshot(@Nullable CompoundTag hoverTag, String expectedItemId, int count) {
        if (hoverTag == null || !hoverTag.contains(SNAPSHOT_TAG, Tag.TAG_COMPOUND)
                || count <= 0 || count > Byte.MAX_VALUE) {
            return null;
        }
        CompoundTag saved = hoverTag.getCompound(SNAPSHOT_TAG);
        if (!saved.contains("id", Tag.TAG_STRING) || !expectedItemId.equals(saved.getString("id"))) {
            return null;
        }

        CompoundTag restored = saved.copy();
        // Respect the outer hover count, including Quark's 64-item display cap.
        restored.putByte("Count", (byte) count);
        if (restored.contains("tag", Tag.TAG_COMPOUND)) {
            restored.getCompound("tag").remove(SNAPSHOT_TAG);
        }
        return restored;
    }
}
