package com.mbrlabs.mundus.editor.terrain;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.mbrlabs.mundus.commons.scene3d.GameObject;
import com.mbrlabs.mundus.commons.scene3d.components.Component;
import com.mbrlabs.mundus.commons.scene3d.components.TerrainComponent;
import com.mbrlabs.mundus.commons.terrain.Terrain;
import com.mbrlabs.mundus.commons.utils.Pools;
import com.mbrlabs.mundus.editor.Mundus;
import com.mbrlabs.mundus.editor.core.project.ProjectContext;
import com.mbrlabs.mundus.editor.events.TerrainVerticesChangedEvent;
import com.mbrlabs.mundus.editor.history.commands.TerrainStitchCommand;
import com.mbrlabs.mundus.editor.ui.UI;

/**
 * Utility class to stitch all terrains within a scene together based on their neighbors.
 * Adapted from <a href="https://gist.github.com/andrew-raphael-lukasik/d2903247b3f9c94e6faa9c042d9f0460">...</a>
 *
 * @author JamesTKhan
 * @version June 26, 2023
 */
public class TerrainStitcher {

    /** The number of steps to take when stitching, lower = sharper transitions */
    public static int numSteps = 10;

    /** Whether to include the GameObject world height when stitching */
    public static boolean includeWorldHeight = false;

    /** Float comparison threshold */
    private static final float threshold = 0.001f;

    public static void stitch(ProjectContext projectContext) {
        // Get all the terrain components
        Array<GameObject> terrainGOs = projectContext.currScene.sceneGraph.findAllByComponent(Component.Type.TERRAIN);
        Array<TerrainComponent> terrainComponents = new Array<>();

        for (GameObject go : terrainGOs) {
            TerrainComponent terrainComponent = (TerrainComponent) go.findComponentByType(Component.Type.TERRAIN);

            int length = terrainComponent.getTerrainAsset().getTerrain().vertexResolution;
            if (numSteps > length) {
                throw new IllegalArgumentException("Number of Steps must be less than the vertex resolution of the terrain (" + length + ")");
            }

            terrainComponents.add(terrainComponent);
        }

        // Add command for undo/redo history
        TerrainStitchCommand command = new TerrainStitchCommand(terrainComponents);

        // Stitch them together
        int heightsStitched = stitch(terrainComponents);

        if (heightsStitched == 0) {
            // No changes were made, so don't add to history
            return;
        }

        // Execute command/apply changes to terrains
        Mundus.INSTANCE.getCommandHistory().add(command);
        command.setHeightDataAfter();
        command.execute();

        // Now add to the modified assets so they can be saved
        for (TerrainComponent terrainComponent : terrainComponents) {
            projectContext.assetManager.addModifiedAsset(terrainComponent.getTerrainAsset());
            Mundus.INSTANCE.postEvent(new TerrainVerticesChangedEvent(terrainComponent));
        }
    }

    public static int stitch(Array<TerrainComponent> terrainComponents) {
        int neighbors = 0;
        int heightsStitched = 0;
        for (TerrainComponent terrainComponent : terrainComponents) {
            heightsStitched = stitchTopBottomNeighbors(terrainComponent);
            if (terrainComponent.getTopNeighbor() != null) neighbors++;
            if (terrainComponent.getBottomNeighbor() != null) neighbors++;
        }

        for (TerrainComponent terrainComponent : terrainComponents) {
            heightsStitched += stitchLeftRightNeighbors(terrainComponent);
            if (terrainComponent.getLeftNeighbor() != null) neighbors++;
            if (terrainComponent.getRightNeighbor() != null) neighbors++;
        }

        if (neighbors == 0) {
            UI.INSTANCE.getToaster().error("No terrains had neighbors assigned.");
        } else if (heightsStitched == 0) {
            UI.INSTANCE.getToaster().info("No terrains needed stitching.");
        } else {
            UI.INSTANCE.getToaster().success("Stitched " + heightsStitched + " height values.");
        }
        return heightsStitched;
    }

    private static int stitchTopBottomNeighbors(TerrainComponent terrainComponent) {
        int length = terrainComponent.getTerrainAsset().getTerrain().vertexResolution;
        int last = length - 1;
        int heightsStitched = 0;

        float currWH = getWorldHeight(terrainComponent);

        TerrainComponent top = terrainComponent.getTopNeighbor();
        TerrainComponent bottom = terrainComponent.getBottomNeighbor();

        float[] heightMap = terrainComponent.getTerrainAsset().getTerrain().heightData;

        if (top != null) {
            float[] topHeightMap = top.getTerrainAsset().getTerrain().heightData;
            float topWH = getWorldHeight(top);

            for (int x = 0; x < length; x++) {
                float currHeight = heightMap[last * length + x] + (includeWorldHeight ? currWH : 0);
                float neighborHeight = topHeightMap[x] + (includeWorldHeight ? topWH : 0);

                if (currHeight + threshold >= neighborHeight) continue;

                float height = neighborHeight;
                for (int step = 0; step < numSteps; step++) {
                    int idx = (last - step) * length + x;
                    float targetHeight = heightMap[idx] + (includeWorldHeight ? currWH : 0);
                    height = MathUtils.lerp(height, targetHeight, (float) step / (float) numSteps);
                    heightMap[idx] = height - (includeWorldHeight ? currWH : 0);
                    heightsStitched++;
                }
            }
        }

        if (bottom != null) {
            float[] bottomHeightMap = bottom.getTerrainAsset().getTerrain().heightData;
            float bottomWH = getWorldHeight(bottom);

            for (int x = 0; x < length; x++) {
                float neighborHeight = bottomHeightMap[last * length + x] + (includeWorldHeight ? bottomWH : 0);
                float currHeight = heightMap[x] + (includeWorldHeight ? currWH : 0);

                if (currHeight + threshold >= neighborHeight) continue;

                float height = neighborHeight;
                for (int step = 0; step < numSteps; step++) {
                    int idx = step * length + x;
                    float targetHeight = heightMap[idx] + (includeWorldHeight ? currWH : 0);
                    height = MathUtils.lerp(height, targetHeight, (float) step / (float) numSteps);
                    heightMap[idx] = height - (includeWorldHeight ? currWH : 0);
                    heightsStitched++;
                }
            }
        }

        return heightsStitched;
    }

    private static int stitchLeftRightNeighbors(TerrainComponent terrainComponent) {
        int length = terrainComponent.getTerrainAsset().getTerrain().vertexResolution;
        int last = length - 1;
        int heightsStitched = 0;

        float currWH = getWorldHeight(terrainComponent);

        TerrainComponent left = terrainComponent.getLeftNeighbor();
        TerrainComponent right = terrainComponent.getRightNeighbor();

        float[] heightMap = terrainComponent.getTerrainAsset().getTerrain().heightData;

        if (left != null) {
            float[] leftHeightMap = left.getTerrainAsset().getTerrain().heightData;
            float leftWH = getWorldHeight(left);

            for (int z = 0; z < length; z++) {
                float currHeight = heightMap[z * length + last] + (includeWorldHeight ? currWH : 0);
                float neighborHeight = leftHeightMap[z * length] + (includeWorldHeight ? leftWH : 0);

                if (currHeight + threshold >= neighborHeight) continue;

                float height = neighborHeight;
                for (int step = 0; step < numSteps; step++) {
                    int idx = z * length + last - step;
                    float targetHeight = heightMap[idx] + (includeWorldHeight ? currWH : 0);
                    height = MathUtils.lerp(height, targetHeight, (float) step / (float) numSteps);
                    heightMap[idx] = height - (includeWorldHeight ? currWH : 0);
                    heightsStitched++;
                }
            }
        }

        if (right != null) {
            float[] rightHeightMap = right.getTerrainAsset().getTerrain().heightData;
            float rightWH = getWorldHeight(right);

            for (int z = 0; z < length; z++) {
                float neighborHeight = rightHeightMap[z * length + last] + (includeWorldHeight ? rightWH : 0);
                float currHeight = heightMap[z * length] + (includeWorldHeight ? currWH : 0);

                if (currHeight + threshold >= neighborHeight) continue;

                float height = neighborHeight;
                for (int step = 0; step < numSteps; step++) {
                    int idx = z * length + step;
                    float targetHeight = heightMap[idx] + (includeWorldHeight ? currWH : 0);
                    height = MathUtils.lerp(height, targetHeight, (float) step / (float) numSteps);
                    heightMap[idx] = height - (includeWorldHeight ? currWH : 0);
                    heightsStitched++;
                }
            }
        }

        return heightsStitched;
    }

    private static float getWorldHeight(TerrainComponent terrainComponent) {
        Vector3 tmp = Pools.vector3Pool.obtain();
        float worldHeight = terrainComponent.getGameObject().getTransform().getTranslation(tmp).y;
        Pools.vector3Pool.free(tmp);
        return worldHeight;
    }

}
