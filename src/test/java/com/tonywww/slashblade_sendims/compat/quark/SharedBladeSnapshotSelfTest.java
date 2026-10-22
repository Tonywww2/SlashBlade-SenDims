package com.tonywww.slashblade_sendims.compat.quark;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.TagParser;

/** Regression checks for binary packet NBT and JSON hover NBT, without a game window. */
public final class SharedBladeSnapshotSelfTest {
    private static int checks;

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }

    public static void main(String[] args) throws Exception {
        String id = "slashblade:slashblade";
        CompoundTag saved = TagParser.parseTag("""
                {id:"slashblade:slashblade",Count:1b,
                 tag:{Damage:7,Unbreakable:1b,display:{Name:'{"text":"Shared blade"}'},
                      Enchantments:[{id:"minecraft:sharpness",lvl:10s}],
                      custom:{nested:[I;1,2,3]},bladeState:{refine:100,killCount:12345}},
                 ForgeCaps:{"slashblade:blade_state":{refine:100,killCount:12345,
                     proudSoul:987654,baseAttackModifier:90.0f,slashArtsKey:"test:art",
                     model:"test:model/blade.obj",texture:"test:model/blade.png",
                     SpecialEffects:{"test:effect":3}},"test:extra_capability":{energy:123456L}}}
                """);
        CompoundTag envelope = saved.getCompound("tag").copy();
        envelope.put(SharedBladeSnapshot.SNAPSHOT_TAG, saved.copy());
        CompoundTag originalEnvelope = envelope.copy();

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        NbtIo.write(envelope, new DataOutputStream(bytes));
        CompoundTag packetNbt = NbtIo.read(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
        // HoverEvent serializes its tag as SNBT inside the component's JSON.
        CompoundTag hoverNbt = TagParser.parseTag(packetNbt.toString());
        CompoundTag restored = SharedBladeSnapshot.readSnapshot(hoverNbt, id, 1);
        check(saved.equals(restored), "all capability and ordinary NBT survives packet + hover roundtrip");
        check(saved.getCompound("ForgeCaps").equals(restored.getCompound("ForgeCaps")), "all attached capabilities retained");
        check(originalEnvelope.equals(envelope), "source envelope unchanged");
        check(originalEnvelope.equals(hoverNbt), "decoded hover envelope unchanged");
        restored.getCompound("ForgeCaps").putInt("mutation", 1);
        check(!hoverNbt.getCompound(SharedBladeSnapshot.SNAPSHOT_TAG).getCompound("ForgeCaps").contains("mutation"), "restored data has no shared mutable compounds");

        CompoundTag capped = SharedBladeSnapshot.readSnapshot(hoverNbt, id, 64);
        check(capped.getByte("Count") == 64, "Quark hover count honored");
        capped.putByte("Count", (byte) 1);
        check(saved.equals(capped), "count adjustment preserves remaining data");
        check(SharedBladeSnapshot.readSnapshot(hoverNbt, "minecraft:diamond", 1) == null, "different item id rejected");
        check(SharedBladeSnapshot.readSnapshot(null, id, 1) == null, "null tag retains vanilla behavior");
        check(SharedBladeSnapshot.readSnapshot(new CompoundTag(), id, 1) == null, "unmarked items retain vanilla behavior");
        check(SharedBladeSnapshot.readSnapshot(hoverNbt, id, 0) == null, "empty count rejected");
        check(SharedBladeSnapshot.readSnapshot(hoverNbt, id, 128) == null, "out-of-range count rejected");
        CompoundTag malformed = new CompoundTag();
        malformed.putString(SharedBladeSnapshot.SNAPSHOT_TAG, "not a compound");
        check(SharedBladeSnapshot.readSnapshot(malformed, id, 1) == null, "invalid envelope type rejected");
        CompoundTag nested = saved.copy();
        nested.getCompound("tag").put(SharedBladeSnapshot.SNAPSHOT_TAG, saved.copy());
        CompoundTag nestedEnvelope = new CompoundTag();
        nestedEnvelope.put(SharedBladeSnapshot.SNAPSHOT_TAG, nested);
        check(!SharedBladeSnapshot.readSnapshot(nestedEnvelope, id, 1).getCompound("tag").contains(SharedBladeSnapshot.SNAPSHOT_TAG), "transport marker removed from display stack");
        System.out.println("Quark shared blade snapshot checks passed: " + checks);
    }
}
