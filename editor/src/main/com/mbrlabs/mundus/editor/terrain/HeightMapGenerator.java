/*
 * Copyright (c) 2016. See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mbrlabs.mundus.editor.terrain;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.mbrlabs.mundus.commons.scene3d.components.TerrainComponent;
import com.mbrlabs.mundus.editor.Mundus;
import com.mbrlabs.mundus.editor.events.TerrainVerticesChangedEvent;

import java.nio.ByteBuffer;

/**
 *
 * @author Marcus Brummer
 * @version 20-06-2016
 */
public class HeightMapGenerator extends Generator<HeightMapGenerator> {

    private final TerrainComponent terrainComponent;

    private float[] heightMapData;
    private int imageWidth;
    private int imageHeight;

    HeightMapGenerator(final TerrainComponent terrainComponent) {
        super(terrainComponent.getTerrainAsset().getTerrain());
        this.terrainComponent = terrainComponent;
    }

    /**
     * The entire height map data from the texture.
     * @param map the height map data
     */
    public HeightMapGenerator map(float[] map) {
        this.heightMapData = map;
        return this;
    }

    public HeightMapGenerator imageWidth(int imageWidth) {
        this.imageWidth = imageWidth;
        return this;
    }

    public HeightMapGenerator imageHeight(int imageHeight) {
        this.imageHeight = imageHeight;
        return this;
    }

    @Override
    public void terraform() {
        int idx = 0;

        // Copy only a part of the height map data to this terrain using the offsets
        for (int i = 0; i < terrain.vertexResolution; i++) {
            for (int j = 0; j < terrain.vertexResolution; j++) {
                int xi = offsetX * (terrain.vertexResolution - 1) + i;
                int zj = offsetZ * (terrain.vertexResolution - 1) + j;
                int heightDataIndex = xi + zj * imageWidth;
                float height = heightMapData[heightDataIndex];

                terrain.heightData[idx] = (height * (maxHeight - minHeight)) + minHeight;  // map it to the desired range
                idx++;
            }
        }

        Mundus.INSTANCE.postEvent(new TerrainVerticesChangedEvent(terrainComponent));
    }

    // Simply creates an array containing only all the red components of the
    // heightData.
    public static float[] heightColorsToMap(final ByteBuffer data, final Pixmap.Format format, int width, int height) {
        final int bytesPerColor = (format == Pixmap.Format.RGB888 ? 3 : (format == Pixmap.Format.RGBA8888 ? 4 : 0));
        if (bytesPerColor == 0) throw new GdxRuntimeException("Unsupported format, should be either RGB8 or RGBA8");
        if (data.remaining() < (width * height * bytesPerColor)) throw new GdxRuntimeException("Incorrect map size");

        final int startPos = data.position();
        byte[] source = null;
        int sourceOffset = 0;
        if (data.hasArray() && !data.isReadOnly()) {
            source = data.array();
            sourceOffset = data.arrayOffset() + startPos;
        } else {
            source = new byte[width * height * bytesPerColor];
            data.get(source);
            data.position(startPos);
        }

        float[] dest = new float[width * height];
        for (int i = 0; i < dest.length; ++i) {
            int v = source[sourceOffset + i * bytesPerColor];
            v = v < 0 ? 256 + v : v;
            dest[i] = ((float) v / 255f); // normalize to 0..1
        }
        return dest;
    }

    /**
     * Smooths a heightmap by averaging the heights of each vertex and its neighbors.
     * @param heightmap the heightmap to smooth
     * @param width the width of the heightmap
     * @param height the height of the heightmap
     * @param smoothingStrength should be a number between 0 (no smoothing) and 1 (maximum smoothing)
     * @return the smoothed heightmap
     */
    public static float[] smoothHeightmap(float[] heightmap, int width, int height, float smoothingStrength) {
        float[] result = new float[width * height];
        for(int y = 0; y < height; y++) {
            for(int x = 0; x < width; x++) {
                float totalHeight = 0;
                int count = 0;

                // sum heights of current vertex and its neighbors
                for(int dy = -1; dy <= 1; dy++) {
                    for(int dx = -1; dx <= 1; dx++) {
                        int nx = x + dx;
                        int ny = y + dy;
                        if(nx >= 0 && nx < width && ny >= 0 && ny < height) {
                            totalHeight += heightmap[ny * width + nx];
                            count++;
                        }
                    }
                }

                // calculate the average height of the neighborhood
                float avgHeight = totalHeight / count;

                // get the original height of the point
                float origHeight = heightmap[y * width + x];

                // set smoothed height using a weighted average of the original height and the neighborhood average
                result[y * width + x] = origHeight * (1 - smoothingStrength) + avgHeight * smoothingStrength;
            }
        }
        return result;
    }
}
