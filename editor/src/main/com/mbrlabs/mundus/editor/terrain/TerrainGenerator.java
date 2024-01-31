package com.mbrlabs.mundus.editor.terrain;

import com.badlogic.gdx.math.MathUtils;
import com.mbrlabs.mundus.editor.terrain.noise.modifiers.TerrainModifier;
import com.mbrlabs.mundus.editor.terrain.noise.TerrainNoiseData;

import java.util.ArrayList;

/**
 * Iterates through the modifiers and applies them based on given X and Z
 * @author JamesTKhan
 * @version October 31, 2022
 */
public class TerrainGenerator {
    private final ArrayList<TerrainModifier> modifiers;

    public TerrainGenerator(ArrayList<TerrainModifier> modifiers) {
        this.modifiers = modifiers;
    }

    public float getNoise(TerrainNoiseData terrainNoiseData, float x, float z) {
        if (modifiers.isEmpty()) return 0;

        for (TerrainModifier modifier : modifiers) {
            modifier.modify(terrainNoiseData, x, z);
        }

        return MathUtils.clamp(terrainNoiseData.elevation, -1.0f, 1.0f);
    }

}
