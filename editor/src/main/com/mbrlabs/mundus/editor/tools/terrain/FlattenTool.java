package com.mbrlabs.mundus.editor.tools.terrain;

import com.mbrlabs.mundus.commons.terrain.Terrain;
import com.mbrlabs.mundus.editor.tools.brushes.TerrainBrush;

/**
 * @author JamesTKhan
 * @version June 28, 2023
 */
public class FlattenTool extends RadiusTerrainTool {

    private static final TerrainBrush.TerrainModifyAction modifier = (brush, terrainComponent, x, z, tVec2, vertexPos) -> {
        Terrain terrain = terrainComponent.getTerrainAsset().getTerrain();

        final int index = z * terrain.vertexResolution + x;
        final float diff = Math.abs(terrain.heightData[index] - TerrainBrush.getHeightSample());
        if (diff <= 1f) {
            terrain.heightData[index] = TerrainBrush.getHeightSample();
        } else if (diff > 1f) {
            final float elevation = brush.getValueOfBrushPixmap(tVec2.x, tVec2.z, vertexPos.x, vertexPos.z, brush.getScaledRadius(terrainComponent));
            // current height is lower than sample
            if (TerrainBrush.getHeightSample() > terrain.heightData[index]) {
                terrain.heightData[index] += elevation * TerrainBrush.getStrength();
            } else {
                float newHeight = terrain.heightData[index] - elevation * TerrainBrush.getStrength();
                if (diff > Math.abs(newHeight) || terrain.heightData[index] > TerrainBrush.getHeightSample()) {
                    terrain.heightData[index] = newHeight;
                }

            }
        }
    };

    @Override
    public void act(TerrainBrush brush) {
        brush.modifyTerrain(modifier, radiusDistanceComparison, true);
    }
}
