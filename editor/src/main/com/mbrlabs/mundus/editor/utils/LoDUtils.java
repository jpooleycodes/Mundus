package com.mbrlabs.mundus.editor.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.mbrlabs.mundus.commons.scene3d.components.TerrainComponent;
import com.mbrlabs.mundus.commons.terrain.LodLevel;
import com.mbrlabs.mundus.commons.terrain.Terrain;

import java.util.HashMap;
import java.util.concurrent.Callable;

/**
 * @author JamesTKhan
 * @version September 30, 2023
 */
public class LoDUtils {

    public interface TerrainLodCallback {
        void onComplete();
    }

    /**
     * Returns a Callable that builds the LoD levels for the given terrain components on a background thread.
     * @param components The terrain components to build LoD levels for
     * @param callback (optional) The callback to call when the LoD levels are built
     */
    public static Callable<Void> createTerrainLodProcessingTask(Iterable<TerrainComponent> components, TerrainLodCallback callback) {
        Array<TerrainComponent> tcs = new Array<>();

        // Copy the iterable to an array so we can access it from another thread without
        // worrying about what happens to it on the main thread
        for (TerrainComponent tc : components) {
            tcs.add(tc);
        }

        Callable<Void> callable = () -> {
            HashMap<TerrainComponent, MeshUtils.SimplifyResult[]> lodData = new HashMap<>();

            // find max height difference between all terrains
            float maxHeightDiff = 0;
            for (TerrainComponent terrain : tcs) {
                float maxHeight = terrain.getOrientedBoundingBox().getBounds().max.y;
                float minHeight = terrain.getOrientedBoundingBox().getBounds().min.y;
                float heightDiff = Math.abs(maxHeight - minHeight);
                maxHeightDiff = Math.max(maxHeightDiff, heightDiff);
            }

            for (TerrainComponent terrain : tcs) {
                lodData.put(terrain, buildTerrainLod(terrain, Terrain.LOD_SIMPLIFICATION_FACTORS, maxHeightDiff));
            }

            Gdx.app.postRunnable(() -> {
                // Now we have all the lod data, convert it to libGDX meshes on main thread
                for (TerrainComponent terrain : lodData.keySet()) {
                    MeshUtils.SimplifyResult[] results = lodData.get(terrain);
                    LodLevel[] levels = new LodLevel[Terrain.LOD_SIMPLIFICATION_FACTORS.length + 1]; // +1 for base lod

                    // Set base lod to the original meshes
                    levels[0] = new LodLevel(terrain.getTerrainAsset().getTerrain().getModel().meshes.toArray(Mesh.class));

                    // Convert the rest of the lod levels
                    for (int i = 0; i < results.length; i++) {
                        levels[i+1] = convertToLodLevel(terrain.getTerrainAsset().getTerrain().getModel(), results[i]);
                    }

                    terrain.getTerrainAsset().setLodLevels(levels);
                    // We mark dirty so it knows to update meshes
                    terrain.getLodManager().markDirty();
                    terrain.getLodManager().enable();
                }

                if (callback != null) {
                    callback.onComplete();
                }
            });
            return null;
        };
        return callable;
    }

    /**
     * Simplify the given terrain based on multipliers. 0.5 = target 50% of the original indices.
     * @param terrain The terrain to simplify
     * @param simplificationFactors The simplification factors to use, these are applied to target indice count
     * @param maxHeightDiff The maximum height difference between the highest and lowest points on all terrains
     */
    public static MeshUtils.SimplifyResult[] buildTerrainLod(TerrainComponent terrain, float[] simplificationFactors, float maxHeightDiff) {
        // prevent divide by 0
        if (maxHeightDiff == 0) {
            maxHeightDiff = 0.0001f;
        }

        Model model = terrain.getTerrainAsset().getTerrain().getModel();
        MeshUtils.SimplifyResult[] results = new MeshUtils.SimplifyResult[simplificationFactors.length];

        // calculate scale factor based on height difference of this terrain and the max height difference of all terrains
        // lower height difference indicates a flatter terrain, so we can use a higher simplification factor
        float maxHeight = terrain.getOrientedBoundingBox().getBounds().max.y;
        float minHeight = terrain.getOrientedBoundingBox().getBounds().min.y;
        float heightDifference = Math.abs(maxHeight - minHeight); // take the absolute to ensure it's positive
        float scaleFactor = 1.0f - (heightDifference / maxHeightDiff);

        scaleFactor = MathUtils.clamp(scaleFactor, 0.1f, 1.0f);

        for (int i = 0; i < simplificationFactors.length; i++) {
            float factor = simplificationFactors[i] * scaleFactor;
            float targetError = 2f;
            MeshUtils.SimplifyResult result = MeshUtils.simplify(model, factor, targetError);
            results[i] = result;
        }
        return results;
    }

    /**
     * Converts the given SimplifyResult objects into to LoDLevels by instantiating new meshes.
     * @param model The original model
     * @param results The results of the simplification for the model
     * @return The LoDLevel array
     */
    public static LodLevel[] convertToLodLevels(Model model, MeshUtils.SimplifyResult[] results) {
        LodLevel[] lodLevels = new LodLevel[results.length + 1];
        lodLevels[0] = new LodLevel(model.meshes.toArray(Mesh.class));

        for (int i = 1; i < lodLevels.length; i++) {
            lodLevels[i] = convertToLodLevel(model, results[i-1]);
        }
        return lodLevels;
    }

    /**
     * Converts the given SimplifyResult object into a LoDLevel by instantiating new meshes.
     * @param model The original model
     * @param result The result of the simplification for the model
     * @return
     */
    public static LodLevel convertToLodLevel(Model model, MeshUtils.SimplifyResult result) {
        Mesh[] meshes = new Mesh[model.meshes.size];
        for (int i = 0; i < model.meshes.size; i++) {
            Mesh mesh = new Mesh(true, result.getVertices()[i].length, result.getIndices()[i].length, model.meshes.get(i).getVertexAttributes());
            mesh.setVertices(result.getVertices()[i]);
            mesh.setIndices(result.getIndices()[i]);
            meshes[i] = mesh;
        }
        return new LodLevel(meshes);
    }
}
