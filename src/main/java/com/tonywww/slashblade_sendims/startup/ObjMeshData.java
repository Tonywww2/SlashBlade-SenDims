package com.tonywww.slashblade_sendims.startup;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Immutable, index-based OBJ input. No GL objects, registry objects or shared parser state. */
public final class ObjMeshData {
    public record Group(String name, int mode, int begin, int end) {}
    final float[] vertices;
    final float[] normals;
    final float[] textures;
    final int[] faces;
    final List<Group> groups;

    ObjMeshData(float[] vertices, float[] normals, float[] textures, int[] faces, List<Group> groups) {
        this.vertices = vertices;
        this.normals = normals;
        this.textures = textures;
        this.faces = faces;
        this.groups = List.copyOf(groups);
    }

    public int vertexCount() { return vertices.length / 3; }
    public int normalCount() { return normals.length / 3; }
    public int textureCount() { return textures.length / 3; }
    public float vertexComponent(int index) { return vertices[index]; }
    public float normalComponent(int index) { return normals[index]; }
    public float textureComponent(int index) { return textures[index]; }
    public int faceValue(int index) { return faces[index]; }
    public List<Group> groups() { return groups; }

    public long estimatedBytes() {
        long bytes = 4L * (vertices.length + normals.length + textures.length + faces.length);
        for (Group group : groups) bytes += 32L + 2L * group.name().length();
        return bytes;
    }

    public void validate() throws IOException {
        if (vertices.length % 3 != 0 || normals.length % 3 != 0 || textures.length % 3 != 0) {
            throw new IOException("Invalid OBJ coordinate count");
        }
        int previous = 0;
        for (Group group : groups) {
            if (group.begin() != previous || group.end() < previous || group.end() > faces.length) {
                throw new IOException("Invalid OBJ group span");
            }
            int at = group.begin();
            while (at < group.end()) {
                int header = faces[at++], count = header >>> 2;
                if (count != 3 && count != 4) throw new IOException("Invalid OBJ face size");
                if (group.mode() != (count == 3 ? 4 : 7)) throw new IOException("Invalid OBJ drawing mode");
                int stride = 1 + (header & 1) + ((header >>> 1) & 1);
                if (count * stride > group.end() - at) throw new IOException("Truncated OBJ face");
                for (int i = 0; i < count; i++) {
                    checkIndex(faces[at++], vertices.length / 3);
                    if ((header & 1) != 0) checkIndex(faces[at++], textures.length / 3);
                    if ((header & 2) != 0) checkIndex(faces[at++], normals.length / 3);
                }
            }
            if (group.begin() == group.end() && group.mode() != -1) throw new IOException("Invalid empty OBJ group");
            previous = at;
        }
        if (previous != faces.length) throw new IOException("Ungrouped OBJ faces");
    }

    private static void checkIndex(int index, int count) throws IOException {
        if (index < 0 || index >= count) throw new IOException("Invalid OBJ index");
    }

    public static ObjMeshData parse(Reader input) throws IOException, UnsupportedSyntax {
        FloatBuffer vertices = new FloatBuffer(), normals = new FloatBuffer(), textures = new FloatBuffer();
        IntBuffer faces = new IntBuffer();
        List<Group> groups = new ArrayList<>();
        String name = null;
        int begin = 0, mode = -1;
        Tokens tokens = new Tokens();
        BufferedReader reader = input instanceof BufferedReader buffered ? buffered : new BufferedReader(input, 32768);
        for (String line; (line = reader.readLine()) != null;) {
            int count = tokens.scan(line);
            if (count == 0 || tokens.startsWith('#')) continue;
            if (tokens.is("v") || tokens.is("vn") || tokens.is("vt")) {
                if (count == 1) continue; // Upstream ignores a bare directive after trim().
                boolean uv = tokens.is("vt"), normal = tokens.is("vn");
                int numbers = count - 1;
                if (numbers < (uv ? 2 : 3) || numbers > (uv ? 3 : 4)) throw new UnsupportedSyntax();
                for (int i = 1; i < count; i++) if (!tokens.decimal(i)) throw new UnsupportedSyntax();
                if (!uv && numbers == 4) continue; // Upstream validates but does not append homogeneous vertices.
                float x = tokens.floating(1), y = tokens.floating(2), z = numbers == 3 ? tokens.floating(3) : 0F;
                (uv ? textures : normal ? normals : vertices).add(x, uv ? 1F - y : y, z);
            } else if (tokens.is("g") || tokens.is("o")) {
                if (count == 1) continue;
                if (count != 2 || !tokens.groupName(1)) throw new UnsupportedSyntax();
                if (name != null) groups.add(new Group(name, mode, begin, faces.size));
                name = tokens.text(1); begin = faces.size; mode = -1;
            } else if (tokens.is("f")) {
                if (count == 1) continue;
                if (count != 4 && count != 5) throw new UnsupportedSyntax();
                int points = count - 1, nextMode = points == 3 ? 4 : 7;
                if (mode != -1 && mode != nextMode) throw new UnsupportedSyntax();
                mode = nextMode;
                if (name == null) name = "Default";
                int shape = tokens.faceShape(1);
                if (shape < 0) throw new UnsupportedSyntax();
                faces.add((points << 2) | shape);
                for (int i = 1; i < count; i++) {
                    if (tokens.faceShape(i) != shape) throw new UnsupportedSyntax();
                    tokens.indices(i, shape, faces, vertices.size / 3, textures.size / 3, normals.size / 3);
                }
            }
            // Material directives, comments and unknown directives are ignored by upstream.
        }
        if (name != null) groups.add(new Group(name, mode, begin, faces.size));
        return new ObjMeshData(vertices.finish(), normals.finish(), textures.finish(), faces.finish(), groups);
    }

    /** Signals exact upstream fallback, including its original line numbers and exception messages. */
    public static final class UnsupportedSyntax extends Exception {
        private static final long serialVersionUID = 1L;
        public UnsupportedSyntax() { super(null, null, false, false); }
    }

    private static final class FloatBuffer {
        private float[] values = new float[96];
        private int size;
        void add(float x, float y, float z) {
            if (size + 3 > values.length) values = Arrays.copyOf(values, values.length * 2);
            values[size++] = x; values[size++] = y; values[size++] = z;
        }
        float[] finish() { return Arrays.copyOf(values, size); }
    }

    private static final class IntBuffer {
        private int[] values = new int[128];
        private int size;
        void add(int value) {
            if (size == values.length) values = Arrays.copyOf(values, values.length * 2);
            values[size++] = value;
        }
        int[] finish() { return Arrays.copyOf(values, size); }
    }

    private static final class Tokens {
        private final int[] starts = new int[6], ends = new int[6];
        private String line;
        int scan(String value) throws UnsupportedSyntax {
            line = value;
            int begin = 0, end = line.length(), count = 0;
            while (begin < end && line.charAt(begin) <= 32) begin++;
            while (end > begin && line.charAt(end - 1) <= 32) end--;
            if (begin == end || line.charAt(begin) == '#') return 0;
            while (begin < end) {
                int start = begin;
                while (begin < end && !space(line.charAt(begin))) begin++;
                if (count == starts.length) {
                    // Long ignored material/comment lines must remain ignored.
                    if (!is("v") && !is("vn") && !is("vt") && !is("f") && !is("g") && !is("o")) return 0;
                    throw new UnsupportedSyntax();
                }
                starts[count] = start; ends[count++] = begin;
                while (begin < end && space(line.charAt(begin))) begin++;
            }
            return count;
        }
        boolean is(String expected) { return ends[0] - starts[0] == expected.length() && line.regionMatches(starts[0], expected, 0, expected.length()); }
        boolean startsWith(char value) { return line.charAt(starts[0]) == value; }
        String text(int token) { return line.substring(starts[token], ends[token]); }
        float floating(int token) { return Float.parseFloat(text(token)); }
        boolean decimal(int token) {
            int at = starts[token], end = ends[token];
            if (at < end && line.charAt(at) == '-') at++;
            int first = at;
            while (at < end && digit(line.charAt(at))) at++;
            if (at == first) return false;
            if (at < end && line.charAt(at) == '.') {
                first = ++at;
                while (at < end && digit(line.charAt(at))) at++;
                if (at == first) return false;
            }
            return at == end;
        }
        boolean groupName(int token) {
            for (int at = starts[token]; at < ends[token]; at++) {
                char c = line.charAt(at);
                if (!digit(c) && !(c >= 'a' && c <= 'z') && !(c >= 'A' && c <= 'Z') && c != '_' && c != '.') return false;
            }
            return true;
        }
        int faceShape(int token) {
            int at = starts[token], end = ends[token], first = at;
            while (at < end && digit(line.charAt(at))) at++;
            if (at == first) return -1;
            if (at == end) return 0;
            if (line.charAt(at++) != '/') return -1;
            if (at < end && line.charAt(at) == '/') {
                first = ++at;
                while (at < end && digit(line.charAt(at))) at++;
                return at > first && at == end ? 2 : -1;
            }
            first = at;
            while (at < end && digit(line.charAt(at))) at++;
            if (at == first) return -1;
            if (at == end) return 1;
            if (line.charAt(at++) != '/') return -1;
            first = at;
            while (at < end && digit(line.charAt(at))) at++;
            return at > first && at == end ? 3 : -1;
        }
        void indices(int token, int shape, IntBuffer output, int vertices, int textures, int normals) throws UnsupportedSyntax {
            int at = starts[token], end = ends[token];
            int component = 0;
            while (at < end) {
                int start = at;
                while (at < end && digit(line.charAt(at))) at++;
                if (start == at) throw new UnsupportedSyntax();
                int value;
                try { value = Integer.parseInt(line, start, at, 10) - 1; }
                catch (NumberFormatException invalid) { throw new UnsupportedSyntax(); }
                int limit = component == 0 ? vertices : component == 1 && (shape & 1) != 0 ? textures : normals;
                if (value < 0 || value >= limit) throw new UnsupportedSyntax();
                output.add(value); component++;
                while (at < end && line.charAt(at) == '/') at++;
            }
        }
        private static boolean digit(char c) { return c >= '0' && c <= '9'; }
        private static boolean space(char c) { return c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f' || c == '\u000b'; }
    }
}
