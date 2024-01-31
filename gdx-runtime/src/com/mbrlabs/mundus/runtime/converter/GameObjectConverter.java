/*
 * Copyright (c) 2021. See AUTHORS file.
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

package com.mbrlabs.mundus.runtime.converter;

import com.mbrlabs.mundus.commons.assets.AssetManager;
import com.mbrlabs.mundus.commons.dto.GameObjectDTO;
import com.mbrlabs.mundus.commons.dto.TerrainComponentDTO;
import com.mbrlabs.mundus.commons.mapper.CustomPropertiesComponentConverter;
import com.mbrlabs.mundus.commons.scene3d.GameObject;
import com.mbrlabs.mundus.commons.scene3d.SceneGraph;
import com.mbrlabs.mundus.commons.scene3d.components.Component;
import com.mbrlabs.mundus.commons.scene3d.components.TerrainComponent;
import com.mbrlabs.mundus.runtime.Shaders;

/**
 * Converter for game object.
 */
public class GameObjectConverter {

    /**
     * Converts {@link GameObjectDTO} to {@link GameObject}.
     */
    public static GameObject convert(GameObjectDTO dto, SceneGraph sceneGraph, Shaders shaders, AssetManager assetManager) {
        final GameObject go = new GameObject(sceneGraph, dto.getName(), dto.getId());
        go.active = dto.isActive();

        // transformation
        final float[] transform = dto.getTransform();
        go.translate(transform[0], transform[1], transform[2]);
        go.rotate(transform[3], transform[4], transform[5], transform[6]);
        go.scale(transform[7], transform[8], transform[9]);

        // convert tags
        if (dto.getTags() != null || !dto.getTags().isEmpty()) {
            for (String tag : dto.getTags()) {
                go.addTag(tag);
            }
        }

        // convert components
        if (dto.getModelComponent() != null) {
            go.getComponents().add(ModelComponentConverter.convert(dto.getModelComponent(), go, assetManager));
        } else if (dto.getTerrainComponent() != null) {
            go.getComponents().add(TerrainComponentConverter.convert(dto.getTerrainComponent(), go, assetManager));
        } else if (dto.getWaterComponent() != null) {
            go.getComponents().add(WaterComponentConverter.convert(dto.getWaterComponent(), go, shaders, assetManager));
        }

        if (dto.getLightComponent() != null) {
            go.getComponents().add(LightComponentConverter.convert(dto.getLightComponent(), go));
        }

        if (dto.getCustomPropertiesComponent() != null) {
            go.getComponents().add(CustomPropertiesComponentConverter.convert(dto.getCustomPropertiesComponent(), go));
        }

        // recursively convert children
        if (dto.getChilds() != null) {
            for (GameObjectDTO c : dto.getChilds()) {
                go.addChild(convert(c, sceneGraph, shaders, assetManager));
            }

            setupNeighborTerrains(dto, go);
        }

        return go;
    }

    /**
     * Setups neighbor terrains for all children if child has {@link TerrainComponent} component and it has set neighbor.
     *
     * @param goDto The DTO of game object.
     * @param go The game object.
     */
    private static void setupNeighborTerrains(final GameObjectDTO goDto, final GameObject go) {
        for (final GameObjectDTO childDto : goDto.getChilds()) {
            final GameObject child = go.findChildById(childDto.getId());

            final TerrainComponentDTO terrainComponentDTO = childDto.getTerrainComponent();
            final TerrainComponent terrainComponent = (TerrainComponent) child.findComponentByType(Component.Type.TERRAIN);

            if (terrainComponentDTO != null) {
                final Integer topNeighborId = terrainComponentDTO.getTopNeighborID();
                if (topNeighborId != null) {
                    final GameObject topNeighbor = go.findChildById(topNeighborId);
                    terrainComponent.setTopNeighbor((TerrainComponent) topNeighbor.findComponentByType(Component.Type.TERRAIN));
                }

                final Integer rightNeighborId = terrainComponentDTO.getRightNeighborID();
                if (rightNeighborId != null) {
                    final GameObject rightNeighbor = go.findChildById(rightNeighborId);
                    terrainComponent.setRightNeighbor((TerrainComponent) rightNeighbor.findComponentByType(Component.Type.TERRAIN));
                }

                final Integer bottomNeighborId = terrainComponentDTO.getBottomNeighborID();
                if (bottomNeighborId != null) {
                    final GameObject bottomNeighbor = go.findChildById(bottomNeighborId);
                    terrainComponent.setBottomNeighbor((TerrainComponent) bottomNeighbor.findComponentByType(Component.Type.TERRAIN));
                }

                final Integer leftNeighborId = terrainComponentDTO.getLeftNeighborID();
                if (leftNeighborId != null) {
                    final GameObject leftNeighbor = go.findChildById(leftNeighborId);
                    terrainComponent.setLeftNeighbor((TerrainComponent) leftNeighbor.findComponentByType(Component.Type.TERRAIN));
                }
            }
        }
    }
}
