package com.tonywww.slashblade_sendims.client.startup;

import com.tonywww.slashblade_sendims.startup.ObjMeshData;
import java.util.ArrayList;
import mods.flammpfeil.slashblade.client.renderer.model.obj.Face;
import mods.flammpfeil.slashblade.client.renderer.model.obj.GroupObject;
import mods.flammpfeil.slashblade.client.renderer.model.obj.TextureCoordinate;
import mods.flammpfeil.slashblade.client.renderer.model.obj.Vertex;
import mods.flammpfeil.slashblade.client.renderer.model.obj.WavefrontObject;

/** Materializes fresh mutable objects for each model; disk entries never contain live render state. */
public final class ObjModelAdapter {
    private ObjModelAdapter() {}

    public static void apply(ObjMeshData data, WavefrontObject target) {
        ArrayList<Vertex> vertices = vertices(data, false), normals = vertices(data, true);
        ArrayList<TextureCoordinate> textures = new ArrayList<>(data.textureCount());
        for (int i = 0; i < data.textureCount() * 3; i += 3) {
            textures.add(new TextureCoordinate(data.textureComponent(i), data.textureComponent(i + 1), data.textureComponent(i + 2)));
        }
        ArrayList<GroupObject> groups = new ArrayList<>(data.groups().size());
        for (ObjMeshData.Group span : data.groups()) {
            GroupObject group = new GroupObject(span.name(), span.mode());
            for (int at = span.begin(); at < span.end();) {
                int header = data.faceValue(at++), count = header >>> 2;
                Face face = new Face();
                face.vertices = new Vertex[count];
                if ((header & 1) != 0) face.textureCoordinates = new TextureCoordinate[count];
                if ((header & 2) != 0) face.vertexNormals = new Vertex[count];
                for (int i = 0; i < count; i++) {
                    face.vertices[i] = vertices.get(data.faceValue(at++));
                    if (face.textureCoordinates != null) face.textureCoordinates[i] = textures.get(data.faceValue(at++));
                    if (face.vertexNormals != null) face.vertexNormals[i] = normals.get(data.faceValue(at++));
                }
                face.faceNormal = face.calculateFaceNormal();
                group.faces.add(face);
            }
            groups.add(group);
        }
        target.vertices = vertices;
        target.vertexNormals = normals;
        target.textureCoordinates = textures;
        target.groupObjects = groups;
    }

    private static ArrayList<Vertex> vertices(ObjMeshData data, boolean normals) {
        int count = normals ? data.normalCount() : data.vertexCount();
        ArrayList<Vertex> result = new ArrayList<>(count);
        for (int i = 0; i < count * 3; i += 3) {
            result.add(normals ? new Vertex(data.normalComponent(i), data.normalComponent(i + 1), data.normalComponent(i + 2))
                    : new Vertex(data.vertexComponent(i), data.vertexComponent(i + 1), data.vertexComponent(i + 2)));
        }
        return result;
    }
}
