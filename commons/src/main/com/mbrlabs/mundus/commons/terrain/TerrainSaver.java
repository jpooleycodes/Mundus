package com.mbrlabs.mundus.commons.terrain;

import com.badlogic.gdx.graphics.Mesh;
import com.mbrlabs.mundus.commons.assets.TerrainAsset;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Saves a terrain to a .terra file.
 *
 * @author JamesTKhan
 * @version September 29, 2023
 */
public class TerrainSaver {
    public static final String HEADER_VERSION = "1.1";
    public static final String HEADER_HEIGHTMAP = "HEIGHTMAP";
    public static final String HEADER_LOD = "LOD";

    public static void save(TerrainAsset terrain) throws IOException {
        OutputStream fileOutputStream = terrain.getFile().write(false, 8192);
        DataOutputStream outputStream = new DataOutputStream(fileOutputStream);

        outputStream.writeUTF(HEADER_VERSION);

        writeHeightmapData(terrain, outputStream);
        writeLoDData(terrain, outputStream);

        outputStream.flush();
        outputStream.close();
    }

    private static void writeHeightmapData(TerrainAsset terrain, DataOutputStream outputStream) throws IOException {
        // Write height data
        outputStream.writeUTF(HEADER_HEIGHTMAP);
        outputStream.writeInt(terrain.getTerrain().heightData.length);
        for (float f : terrain.getTerrain().heightData) {
            outputStream.writeFloat(f);
        }
    }

    private static void writeLoDData(TerrainAsset terrain, DataOutputStream outputStream) throws IOException {
        LodLevel[] levels = terrain.getLodLevels();
        if (levels == null || levels.length == 0) return;

        outputStream.writeUTF(HEADER_LOD);
        outputStream.writeInt(levels.length - 1);

        for (int i = 0; i < levels.length; i++) {
            if (i == 0) continue; // lod0 is base level, aka heightmap
            LodLevel level = levels[i];

            // At time of writing this, terrains only have 1 mesh per LoD but want to keep it flexible
            int meshCount = level.getLodMesh().length;
            outputStream.writeInt(meshCount); // Number of meshes in this LOD

            for (int m = 0; m < meshCount; m++) {
                Mesh mesh = level.getLodMesh()[m];

                float[] vertices = new float[mesh.getNumVertices() * mesh.getVertexSize() / 4];
                short[] indices = new short[mesh.getNumIndices()];
                mesh.getVertices(vertices);
                mesh.getIndices(indices);

                outputStream.writeInt(vertices.length);
                for (float v : vertices) {
                    outputStream.writeFloat(v);
                }

                outputStream.writeInt(indices.length);
                for (short index : indices) {
                    outputStream.writeShort(index);
                }

            }
        }
    }
}
