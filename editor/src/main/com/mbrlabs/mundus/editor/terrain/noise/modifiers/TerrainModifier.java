package com.mbrlabs.mundus.editor.terrain.noise.modifiers;

import com.mbrlabs.mundus.editor.terrain.noise.TerrainNoiseData;

/**
 * @author JamesTKhan
 * @version October 31, 2022
 */
public interface TerrainModifier {
    String getName();
    void modify(TerrainNoiseData noiseData, float x, float y);
}
