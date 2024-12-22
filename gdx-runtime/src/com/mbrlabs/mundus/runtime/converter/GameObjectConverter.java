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

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.mbrlabs.mundus.commons.assets.Asset;
import com.mbrlabs.mundus.commons.assets.AssetManager;
import com.mbrlabs.mundus.commons.dto.CustomComponentDTO;
import com.mbrlabs.mundus.commons.dto.GameObjectDTO;
import com.mbrlabs.mundus.commons.mapper.CustomComponentConverter;
import com.mbrlabs.mundus.commons.mapper.CustomPropertiesComponentConverter;
import com.mbrlabs.mundus.commons.scene3d.GameObject;
import com.mbrlabs.mundus.commons.scene3d.SceneGraph;
import com.mbrlabs.mundus.commons.scene3d.components.Component;
import com.mbrlabs.mundus.runtime.Shaders;
import com.mbrlabs.mundus.commons.utils.AssetUtils;

/**
 * Converter for game object.
 */
public class GameObjectConverter {

    /**
     * Converts {@link GameObjectDTO} to {@link GameObject}.
     */
    public static GameObject convert(
            GameObjectDTO dto,
            SceneGraph sceneGraph,
            Shaders shaders,
            AssetManager assetManager,
            CustomComponentConverter[] customComponentConverters
    ) {
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

        if (dto.getCustomComponents() != null && customComponentConverters != null) {
            for (int i = 0; i < dto.getCustomComponents().size; ++i) {
                final CustomComponentDTO customComponentDTO = dto.getCustomComponents().get(i);

                for (int ii = 0; ii < customComponentConverters.length; ++ii) {
                    final CustomComponentConverter converter = customComponentConverters[ii];

                    if (customComponentDTO.getComponentType().equals(converter.getComponentType().name())) {
                        final Array<String> assetIds = customComponentDTO.getAssetIds();
                        final ObjectMap<String, Asset> assetMap = AssetUtils.getAssetsById(assetIds, assetManager.getAssetMap());
                        final Component component = converter.convert(go, customComponentDTO.getProperties(), assetMap);

                        if (component != null) {
                            go.getComponents().add(component);
                        }
                    }
                }
            }
        }

        // recursively convert children
        if (dto.getChilds() != null) {
            for (GameObjectDTO c : dto.getChilds()) {
                go.addChild(convert(c, sceneGraph, shaders, assetManager, customComponentConverters));
            }
        }

        return go;
    }
}
