package com.tonywww.slashblade_sendims.startup;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/** Content-addressed, versioned, checksummed cache. Failures are ordinary cache misses. */
public final class ObjDiskCache {
    private static final int MAGIC = 0x534F424A;
    private static final int VERSION = 1;
    public static final int MAX_ENTRY_BYTES = 32 * 1024 * 1024;
    private final Path directory;
    private final long budget;
    private long knownBytes = -1; // Updated only by the serialized writer; avoids rescanning per entry.

    public ObjDiskCache(Path directory, long budget) {
        this.directory = directory;
        this.budget = Math.max(0, budget);
    }

    public ObjMeshData get(String key) {
        if (!validKey(key) || budget == 0 || Files.isSymbolicLink(directory)) return null;
        Path file = directory.resolve(key + ".bin");
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) return null;
        try {
            if (Files.size(file) > MAX_ENTRY_BYTES) return null;
            try (DataInputStream input = new DataInputStream(new BufferedInputStream(
                    new GZIPInputStream(new BufferedInputStream(Files.newInputStream(file))), 65536))) {
                if (input.readInt() != MAGIC || input.readInt() != VERSION || !input.readUTF().equals(key)) return null;
                long[] remaining = {MAX_ENTRY_BYTES};
                float[] vertices = floats(input, remaining), normals = floats(input, remaining), textures = floats(input, remaining);
                int[] faces = integers(input, remaining);
                int count = length(input, remaining, 32);
                List<ObjMeshData.Group> groups = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    String name = input.readUTF();
                    remaining[0] -= 2L * name.length();
                    if (remaining[0] < 0) throw new IOException("OBJ group names exceed cache entry budget");
                    groups.add(new ObjMeshData.Group(name, input.readInt(), input.readInt(), input.readInt()));
                }
                if (input.read() != -1) throw new IOException("Unexpected OBJ cache payload"); // Also verifies GZIP CRC.
                ObjMeshData result = new ObjMeshData(vertices, normals, textures, faces, groups);
                result.validate();
                return result;
            }
        } catch (IOException | RuntimeException invalid) {
            return null;
        }
    }

    public synchronized void put(String key, ObjMeshData data) throws IOException {
        if (!validKey(key) || budget == 0 || data.estimatedBytes() > Math.min(budget, MAX_ENTRY_BYTES)) return;
        Files.createDirectories(directory);
        if (Files.isSymbolicLink(directory)) return;
        Path file = directory.resolve(key + ".bin");
        if (Files.isSymbolicLink(file)) return;
        long previous = Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) ? Files.size(file) : 0;
        Path temporary = Files.createTempFile(directory, "obj-", ".tmp");
        try {
            try (DataOutputStream output = new DataOutputStream(new BufferedOutputStream(
                    new GZIPOutputStream(new BufferedOutputStream(Files.newOutputStream(temporary))), 65536))) {
                output.writeInt(MAGIC); output.writeInt(VERSION); output.writeUTF(key);
                write(output, data.vertices); write(output, data.normals); write(output, data.textures);
                output.writeInt(data.faces.length);
                for (int value : data.faces) output.writeInt(value);
                output.writeInt(data.groups.size());
                for (ObjMeshData.Group group : data.groups) {
                    output.writeUTF(group.name()); output.writeInt(group.mode());
                    output.writeInt(group.begin()); output.writeInt(group.end());
                }
            }
            long written = Files.size(temporary);
            if (written > budget) return;
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
            if (knownBytes >= 0) knownBytes += written - previous;
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    /** Run only on the cache writer, after a preparation batch, never on the render thread. */
    public synchronized void prune() throws IOException {
        if (knownBytes >= 0 && knownBytes <= budget) return;
        if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) return;
        record Entry(Path path, long bytes, FileTime modified) {}
        List<Entry> entries = new ArrayList<>();
        long total = 0;
        try (var files = Files.newDirectoryStream(directory, "*.bin")) {
            for (Path path : files) {
                if (!validKey(path.getFileName().toString().replace(".bin", ""))
                        || !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) continue;
                BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
                long bytes = attributes.size();
                entries.add(new Entry(path, bytes, attributes.lastModifiedTime()));
                total += bytes;
            }
        }
        entries.sort(Comparator.comparing(Entry::modified));
        for (Entry entry : entries) {
            if (total <= budget) break;
            Files.deleteIfExists(entry.path()); total -= entry.bytes();
        }
        knownBytes = total;
    }

    private static boolean validKey(String key) {
        if (key == null || key.length() != 64) return false;
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            if (!(c >= '0' && c <= '9') && !(c >= 'a' && c <= 'f')) return false;
        }
        return true;
    }
    private static int length(DataInputStream input, long[] remaining, int unit) throws IOException {
        int count = input.readInt();
        if (count < 0 || (long) count * unit > remaining[0]) throw new IOException("OBJ cache entry exceeds bounds");
        remaining[0] -= (long) count * unit;
        return count;
    }
    private static float[] floats(DataInputStream input, long[] remaining) throws IOException {
        float[] values = new float[length(input, remaining, 4)];
        for (int i = 0; i < values.length; i++) values[i] = Float.intBitsToFloat(input.readInt());
        return values;
    }
    private static int[] integers(DataInputStream input, long[] remaining) throws IOException {
        int[] values = new int[length(input, remaining, 4)];
        for (int i = 0; i < values.length; i++) values[i] = input.readInt();
        return values;
    }
    private static void write(DataOutputStream output, float[] values) throws IOException {
        output.writeInt(values.length);
        for (float value : values) output.writeInt(Float.floatToRawIntBits(value));
    }
}
