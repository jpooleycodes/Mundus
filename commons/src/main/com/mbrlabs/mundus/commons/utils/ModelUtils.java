/*
 * Copyright (c) 2022. See AUTHORS file.
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

package com.mbrlabs.mundus.commons.utils;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.OrientedBoundingBox;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.mbrlabs.mundus.commons.scene3d.GameObject;
import com.mbrlabs.mundus.commons.scene3d.components.Component;
import com.mbrlabs.mundus.commons.scene3d.components.ModelComponent;
import com.mbrlabs.mundus.commons.scene3d.components.TerrainComponent;

/**
 * @author JamesTKhan
 * @version July 13, 2022
 */
public class ModelUtils {
    private static final Array<Renderable> renderables = new Array<>();
    private static final Pool<Renderable> pool = new RenderablePool();
    private final static Vector3 tmpVec0 = new Vector3();

    private ModelUtils() {}

    /**
     * Gets the total bone count for the given model based on having
     * one renderable.
     *
     * @param model the model to count bones for
     * @return the bone count
     */
    public static int getBoneCount(Model model) {
        int numBones = 0;

        ModelInstance instance = new ModelInstance(model);
        instance.getRenderables(renderables, pool);

        // Bones appear to be copied to each NodePart
        // So we just count the first renderable that has bones
        // and break
        for (Renderable renderable : renderables) {
            if (renderable.bones != null) {
                numBones += renderable.bones.length;
                break;
            }
        }

        renderables.clear();
        pool.clear();

        return numBones;
    }

    /**
     * Applies materials on the provided GameObject and all of its children recursively
     *
     * @param rootGameObject the parent game object to apply
     */
    public static void applyGameObjectMaterials(GameObject rootGameObject) {
        ModelComponent mc = rootGameObject.findComponentByType(Component.Type.MODEL);
        if (mc != null) {
            mc.applyMaterials();
        }

        TerrainComponent tc = rootGameObject.findComponentByType(Component.Type.TERRAIN);
        if (tc != null) {
            tc.applyMaterial();
        }

        if (rootGameObject.getChildren() == null) return;

        // Update children recursively
        for (GameObject go : rootGameObject.getChildren()) {
            applyGameObjectMaterials(go);
        }
    }

    /**
     * Checks if visible to camera using OrientedBoundingBox
     */
    public static boolean isVisible(final Camera cam, OrientedBoundingBox box) {
        return cam.frustum.boundsInFrustum(box);
    }

    /**
     * Checks if visible to camera using sphereInFrustum and radius
     */
    public static boolean isVisible(final Camera cam, final ModelInstance modelInstance, Vector3 center, float radius) {
        tmpVec0.set(center).mul(modelInstance.transform);
        return cam.frustum.sphereInFrustum(tmpVec0, radius);
    }

    /**
     * Checks if visible to camera using boundsInFrustum and dimensions
     */
    public static boolean isVisible(final Camera cam, final ModelInstance modelInstance, Vector3 center, Vector3 dimensions) {
        modelInstance.transform.getTranslation(tmpVec0);
        tmpVec0.add(center);
        return cam.frustum.boundsInFrustum(tmpVec0, dimensions);
    }

    /**
     * Get Vertices count of a Model
     */
    public static int getVerticesCount(Model model) {
        int vertices = 0;
        for (Mesh mesh : model.meshes) {
            vertices += mesh.getNumVertices();
        }
        return vertices;
    }

    /**
     * Get Indices count of a Model
     */
    public static float getIndicesCount(Model model) {
        int indices = 0;
        for (Mesh mesh : model.meshes) {
            indices += mesh.getNumIndices();
        }
        return indices;
    }

}
