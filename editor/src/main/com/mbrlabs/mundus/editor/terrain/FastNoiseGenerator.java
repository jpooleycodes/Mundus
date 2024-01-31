package com.mbrlabs.mundus.editor.terrain;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.mbrlabs.mundus.commons.terrain.Terrain;
import com.mbrlabs.mundus.editor.terrain.noise.modifiers.TerrainModifier;
import com.mbrlabs.mundus.editor.terrain.noise.TerrainNoiseData;

import java.util.ArrayList;

/**
 * @author JamesTKhan
 * @version October 24, 2022
 */
public class FastNoiseGenerator extends Generator<FastNoiseGenerator> {

    // The list of modifiers to apply to the noise
    private final ArrayList<TerrainModifier> modifiers = new ArrayList<>();

    FastNoiseGenerator(Terrain terrain) {
        super(terrain);
    }

    public ArrayList<TerrainModifier> getModifiers() {
        return modifiers;
    }

    @Override
    public void terraform() {
        TerrainGenerator terrainGenerator = new TerrainGenerator(modifiers);
        TerrainNoiseData terrainNoiseData = new TerrainNoiseData();

        for (int i = 0; i < terrain.heightData.length; i++) {
            terrainNoiseData.elevation = 0;

            int x = i % terrain.vertexResolution;
            int z = (int) Math.floor((double) i / terrain.vertexResolution);

            float modX = offsetX * (terrain.vertexResolution - 1);
            float modY = offsetZ * (terrain.vertexResolution - 1);

            terrainGenerator.getNoise(terrainNoiseData, x + modX, z + modY);

            // Major Elevation
            float height = Interpolation.linear.apply(minHeight, maxHeight, terrainNoiseData.elevation);

            terrain.heightData[z * terrain.vertexResolution + x] = height;
        }
    }

    public Pixmap generateNoise(int width, int height) {
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.BLACK);
        pixmap.fill();

        float maxN = 0;
        float minN = 0;

        TerrainGenerator terrainGenerator = new TerrainGenerator(modifiers);
        TerrainNoiseData[][] noiseData = new TerrainNoiseData[width][height];

        for (int x = 0; x < width; x++) {
            for (int z = 0; z < height; z++) {

                TerrainNoiseData terrainNoiseData = new TerrainNoiseData();
                float noiseValue = terrainGenerator.getNoise(terrainNoiseData, x, z);
                noiseData[x][z] = terrainNoiseData;

                maxN = Math.max(maxN, noiseValue);
                minN = Math.min(minN, noiseValue);
            }
        }

        float scale = 255f / (maxN - minN);

        for (int x = 0; x < width; x++) {
            for (int z = 0; z < height; z++) {
                int value = Math.round(MathUtils.clamp((noiseData[x][z].elevation - minN) * scale, 0, 255));

                value |= value << 8;
                value |= value << 16;

                pixmap.drawPixel(x, z, value);
            }
        }

        //PixmapIO.writePNG(Gdx.files.local("test.png"), pixmap);
        return pixmap;
    }
}
