package com.mbrlabs.mundus.commons.terrain;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.mbrlabs.mundus.commons.assets.meta.MetaTerrain;
import com.mbrlabs.mundus.commons.dto.LevelOfDetailDTO;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;

/**
 * Loads Mundus Terrain objects via meta and .terra file data.
 * @author JamesTKhan
 * @version July 24, 2022
 */
public class TerrainLoader extends AsynchronousAssetLoader<Terrain, TerrainLoader.TerrainParameter> {

    private Terrain terrain = null;

    public TerrainLoader() {
        this(new InternalFileHandleResolver());
    }

    public TerrainLoader(FileHandleResolver resolver) {
        super(resolver);
    }

    @Override
    public Array<AssetDescriptor> getDependencies(String fileName, FileHandle file, TerrainParameter parameter) {
        if (parameter == null) {
            throw new GdxRuntimeException("TerrainLoader requires parameters");
        }

        if (parameter.metaTerrain == null) {
            throw new GdxRuntimeException("MetaTerrain is required for TerrainLoader");
        }
        return null;
    }

    @Override
    public void loadAsync(AssetManager manager, String fileName, FileHandle file, TerrainParameter parameter) {
        terrain = null;

        // load height data from terra file
        DataInputStream is;
        TerrainLoadResult result = null;
        try {
            is = new DataInputStream(new BufferedInputStream(file.read()));

            String tag = readHeader(is);
            if (tag.isEmpty()) {
                // first reopen the file as we already tried to read a string from it
                is.close();
                is = new DataInputStream(new BufferedInputStream(file.read()));
                result = readLegacyTerrainData(is);
            } else {
                result = readTerrainData(is, tag);
            }

            is.close();
        } catch (EOFException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
            throw new GdxRuntimeException("Error reading terra file: " + file.name());
        }
        terrain = new Terrain(parameter.metaTerrain.getSize(), result.heightData);
        terrain.updateUvScale(new Vector2(parameter.metaTerrain.getUv(), parameter.metaTerrain.getUv()));
        terrain.setLoDDTOs(result.loDDTOS);
    }

    private TerrainLoadResult readTerrainData(DataInputStream is, String startTag) throws IOException {
        TerrainLoadResult result = new TerrainLoadResult();
        while (is.available() > 0) {

            // If we have a start tag, use that instead of reading from the file
            String tag = startTag == null ? is.readUTF() : startTag;
            startTag = null;

            switch (tag) {
                case TerrainSaver.HEADER_HEIGHTMAP:
                    int heightDataLength = is.readInt();
                    float[] heightData = new float[heightDataLength];
                    for (int i = 0; i < heightDataLength; i++) {
                        heightData[i] = is.readFloat();
                    }
                    result.heightData = heightData;
                    break;
                case TerrainSaver.HEADER_LOD:
                    int lodCount = is.readInt();
                    result.loDDTOS = new LevelOfDetailDTO[lodCount];
                    for (int i = 0; i < lodCount; i++) {

                        int meshCount = is.readInt(); // Number of meshes in this LOD
                        float[][] lodVertices = new float[meshCount][];
                        short[][] lodIndices = new short[meshCount][];

                        for (int m = 0; m < meshCount; m++) {
                            int vertexCount = is.readInt();
                            lodVertices[m] = new float[vertexCount];
                            for (int j = 0; j < vertexCount; j++) {
                                lodVertices[m][j] = is.readFloat();
                            }

                            int indexCount = is.readInt();
                            lodIndices[m] = new short[indexCount];
                            for (int j = 0; j < indexCount; j++) {
                                lodIndices[m][j] = is.readShort();
                            }
                        }

                        // Cannot instantiate meshes here so we store the data in a DTO
                        result.loDDTOS[i] = new LevelOfDetailDTO(lodVertices, lodIndices);
                    }
                    break;
            }
        }
        return result;
    }

    private TerrainLoadResult readLegacyTerrainData(DataInputStream is) throws IOException {
        // Legacy format file (mundus version <= 0.6.0) that has no headers,
        // read in the entire file as height data
        final FloatArray floatArray = new FloatArray();
        while (is.available() > 0) {
            floatArray.add(is.readFloat());
        }
        TerrainLoadResult result = new TerrainLoadResult();
        result.heightData = floatArray.toArray();
        return result;
    }

    @Override
    public Terrain loadSync(AssetManager manager, String fileName, FileHandle file, TerrainParameter parameter) {
        terrain.init();
        terrain.update();

        Terrain terrain = this.terrain;
        this.terrain = null;
        return terrain;
    }

    static public class TerrainParameter extends AssetLoaderParameters<Terrain> {
        public TerrainParameter(MetaTerrain metaTerrain) {
            this.metaTerrain = metaTerrain;
        }

        /** Required to create the terrain properly **/
        public MetaTerrain metaTerrain = null;
    }

    static private class TerrainLoadResult {
        public float[] heightData;
        public LevelOfDetailDTO[] loDDTOS;
    }

    private String readHeader(DataInputStream is) {
        String header = "";
        try {
            header = is.readUTF();  // try to read the first bytes as a UTF string
        } catch (Exception e) {
            // If the file doesn't start with a string, it's an old format file.
        }
        return header;
    }
}
