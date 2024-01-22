/*
 * Copyright (c) 2023. See AUTHORS file.
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

package com.mbrlabs.mundus.commons.terrain;

import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.RenderableProvider;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.model.NodePart;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.Pool;
import com.mbrlabs.mundus.commons.assets.ModelAsset;
import com.mbrlabs.mundus.commons.assets.TerrainObject;
import com.mbrlabs.mundus.commons.assets.TerrainObjectLayerAsset;
import com.mbrlabs.mundus.commons.assets.TerrainObjectsAsset;

import java.nio.FloatBuffer;

public class TerrainObjectManager implements RenderableProvider {

    private static final String INSTANCED_ALIAS = "i_worldTrans";

    private final Array<TerrainObjectModelInstance> modelInstances;

    public TerrainObjectManager() {
        modelInstances = new Array<>(2);
    }

    @Override
    public void getRenderables(final Array<Renderable> renderables, final Pool<Renderable> pool) {
        for (int i = 0; i < modelInstances.size; ++i) {
            modelInstances.get(i).modelInstance.getRenderables(renderables, pool);
        }
    }

    /**
     * Apply terrain object changes.
     * If there is removed terrain object in asset then removes it from rendering.
     * If there is new terrain object in asset then adds it to rendering.
     * Updates position of terrain objects.
     *
     * @param recreateAllObjects If true then removes all terrain objects and recreates again.
     */
    public void apply(final boolean recreateAllObjects, final TerrainObjectsAsset terrainObjectsAsset, final TerrainObjectLayerAsset terrainObjectLayerAsset, final Matrix4 parentTransform) {
        final ObjectMap<ModelAsset, Array<TerrainObject>> map = asd(terrainObjectsAsset, terrainObjectLayerAsset);
        removeModelInstances(recreateAllObjects, map);
        addModelInstances(map);
        updatePositions(map, parentTransform);
    }

    private ObjectMap<ModelAsset, Array<TerrainObject>> asd(final TerrainObjectsAsset terrainObjectsAsset, final TerrainObjectLayerAsset terrainObjectLayerAsset) {
        final ObjectMap<ModelAsset, Array<TerrainObject>> map = new ObjectMap<>();

        for (int i = 0; i < terrainObjectsAsset.getTerrainObjectNum(); ++i) {
            final TerrainObject terrainObject = terrainObjectsAsset.getTerrainObject(i);
            final int layerPos = terrainObject.getLayerPos();
            final ModelAsset modelAsset = terrainObjectLayerAsset.getModels().get(layerPos);

            if (!map.containsKey(modelAsset)) {
                map.put(modelAsset, new Array<TerrainObject>());
            }

            map.get(modelAsset).add(terrainObject);
        }

        return map;
    }

    private void updatePositions(final ObjectMap<ModelAsset, Array<TerrainObject>> terrainObjectMap, final Matrix4 parentTransform) {
        final ObjectMap.Entries<ModelAsset, Array<TerrainObject>> entries = terrainObjectMap.entries();
        while (entries.hasNext) {
            final ObjectMap.Entry<ModelAsset, Array<TerrainObject>> next = entries.next();

            final TerrainObjectModelInstance terrainObjectModelInstance = findById(next.key.getID());
            final ModelInstance modelInstance = terrainObjectModelInstance.modelInstance;

            modelInstance.transform.set(parentTransform);
            enableInstancedMeshesIfNecessary(terrainObjectModelInstance, next.value.size);
            setupPositionScaleAndRotation(modelInstance, next.value);
        }
    }

    private void addModelInstances(final ObjectMap<ModelAsset, Array<TerrainObject>> terrainObjectMap) {
        ObjectMap.Keys<ModelAsset> keys = terrainObjectMap.keys();
        while (keys.hasNext) {
            final ModelAsset modelAsset = keys.next();

            if (!containsModelInstance(modelAsset.getID())) {
                modelInstances.add(new TerrainObjectModelInstance(modelAsset));
            }
        }
    }

//    private void setupPositionScaleAndRotation(final ModelInstance modelInstance, final TerrainObject terrainObject, final Matrix4 parentTransform) {
//        final Vector3 localPosition = terrainObject.getPosition();
//        final Vector3 rotate = terrainObject.getRotation();
//        final Vector3 scale = terrainObject.getScale();
//
//        modelInstance.transform.translate(localPosition);
//
//        if (!rotate.isZero()) {
//            final Quaternion rot = modelInstance.transform.getRotation(Pools.quaternionPool.obtain());
//            rot.setEulerAngles(rotate.y, rotate.x, rotate.z);
//            modelInstance.transform.rotate(rot);
//
//            Pools.quaternionPool.free(rot);
//        }
//
//        if (!scale.isUnit()) {
//            modelInstance.transform.scale(scale.x, scale.y, scale.z);
//        }
//
//        modelInstance.transform.mulLeft(parentTransform);
//    }

    private void setupPositionScaleAndRotation(final ModelInstance modelInstance, final Array<TerrainObject> terrainObjects) {
        final FloatBuffer offsets = BufferUtils.newFloatBuffer(terrainObjects.size * 16); // 16 floats for mat4
        final Matrix4 tmpMatrix4 = new Matrix4();

        for (int i = 0 ; i < terrainObjects.size; ++i) {
            final TerrainObject terrainObject = terrainObjects.get(i);

            tmpMatrix4.idt();
            tmpMatrix4.trn(terrainObject.getPosition());

            offsets.put(tmpMatrix4.getValues());
        }

        offsets.position(0);

        for(int i = 0 ; i < modelInstance.nodes.size; i++) {
            final Node node = modelInstance.nodes.get(i);
            for (int ii = 0; ii < node.parts.size; ++ii) {
                final NodePart nodePart = node.parts.get(ii);
                final Mesh mesh = nodePart.meshPart.mesh;

                mesh.setInstanceData(offsets);
            }
        }
    }

    private void removeModelInstances(final boolean recreateAllObjects, final ObjectMap<ModelAsset, Array<TerrainObject>> terrainObjectMap) {
        for (int i = modelInstances.size - 1; i >= 0; --i) {

            if (recreateAllObjects || !terrainObjectMap.containsKey(modelInstances.get(i).modelAsset)) {
                modelInstances.removeIndex(i);
            }
        }
    }

    private boolean containsModelInstance(final String id) {
        return findById(id) != null;
    }

    private boolean containsTerrainObject(final String id, final TerrainObjectsAsset terrainObjectsAsset) {
        for (int i = 0; i < terrainObjectsAsset.getTerrainObjectNum(); ++i) {
            if (id.equals(terrainObjectsAsset.getTerrainObject(i).getId())) {
                return true;
            }
        }

        return false;
    }

    private TerrainObjectModelInstance findById(final String id) {
        for (int i = 0; i < modelInstances.size; ++i) {
            final TerrainObjectModelInstance modelInstance = modelInstances.get(i);

            if (id.equals(modelInstance.modelAsset.getID())) {
                return modelInstance;
            }
        }

        return null;
    }

    private void enableInstancedMeshesIfNecessary(final TerrainObjectModelInstance terrainObjectModelInstance, final int maxInstances) {
        final ModelInstance modelInstance = terrainObjectModelInstance.modelInstance;
        final int previousMaxInstances = terrainObjectModelInstance.instancedCount;

        for(int i = 0 ; i < modelInstance.nodes.size; i++) {
            final Node node = modelInstance.nodes.get(i);
            for (int ii = 0; ii < node.parts.size; ++ii) {
                final NodePart nodePart = node.parts.get(ii);
                final Mesh mesh = nodePart.meshPart.mesh;

                if (previousMaxInstances != TerrainObjectModelInstance.DISABLED_INSTANCES_COUNT && previousMaxInstances != maxInstances) {
                    mesh.disableInstancedRendering();
                }

                if (!mesh.isInstanced()) {
                    mesh.enableInstancedRendering(true, maxInstances,
                            new VertexAttribute(VertexAttributes.Usage.Generic, 4, INSTANCED_ALIAS, 0),
                            new VertexAttribute(VertexAttributes.Usage.Generic, 4, INSTANCED_ALIAS, 1),
                            new VertexAttribute(VertexAttributes.Usage.Generic, 4, INSTANCED_ALIAS, 2),
                            new VertexAttribute(VertexAttributes.Usage.Generic, 4, INSTANCED_ALIAS, 3));
                }
            }
        }

        terrainObjectModelInstance.instancedCount = maxInstances;
    }

    private class TerrainObjectModelInstance {
        private static final int DISABLED_INSTANCES_COUNT = -1;

        private final ModelAsset modelAsset;
        private final ModelInstance modelInstance;
        private int instancedCount = DISABLED_INSTANCES_COUNT;

        public TerrainObjectModelInstance(final ModelAsset modelAsset) {
            this.modelAsset = modelAsset;
            modelInstance = new ModelInstance(modelAsset.getModel());
            // TODO copy meshes
        }
    }
}
