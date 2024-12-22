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

package com.mbrlabs.mundus.commons.dto;

import com.badlogic.gdx.utils.Array;
import com.mbrlabs.mundus.commons.assets.Asset;

import java.util.Map;

/**
 * @author Tibor Zsuro
 * @version 21-08-2021
 */
public class GameObjectDTO implements AssetUsageDTO {

    private int id;
    private String name;
    private boolean active;

    private float[] transform = new float[10];

    private Array<String> tags;
    private Array<GameObjectDTO> childs;

    private ModelComponentDTO modelComponent;
    private TerrainComponentDTO terrainComponent;
    private WaterComponentDTO waterComponent;
    private LightComponentDTO lightComponent;
    private CustomPropertiesComponentDTO customPropertiesComponent;
    private Array<CustomComponentDTO> customComponents;

    public GameObjectDTO() {
        childs = new Array<>();
        tags = new Array<>();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public float[] getTransform() {
        return transform;
    }

    public Array<String> getTags() {
        return tags;
    }

    public void setTags(Array<String> tags) {
        this.tags = tags;
    }

    public Array<GameObjectDTO> getChilds() {
        return childs;
    }

    public void setChilds(Array<GameObjectDTO> childs) {
        this.childs = childs;
    }

    public ModelComponentDTO getModelComponent() {
        return modelComponent;
    }

    public void setModelComponent(ModelComponentDTO modelComponent) {
        this.modelComponent = modelComponent;
    }

    public TerrainComponentDTO getTerrainComponent() {
        return terrainComponent;
    }

    public void setTerrainComponent(TerrainComponentDTO terrainComponent) {
        this.terrainComponent = terrainComponent;
    }

    public WaterComponentDTO getWaterComponent() {
        return waterComponent;
    }

    public void setWaterComponent(WaterComponentDTO waterComponent) {
        this.waterComponent = waterComponent;
    }

    public LightComponentDTO getLightComponent() {
        return lightComponent;
    }

    public void setLightComponent(LightComponentDTO lightComponent) {
        this.lightComponent = lightComponent;
    }

    public CustomPropertiesComponentDTO getCustomPropertiesComponent() {
        return customPropertiesComponent;
    }

    public void setCustomPropertiesComponent(final CustomPropertiesComponentDTO customPropertiesComponent) {
        this.customPropertiesComponent = customPropertiesComponent;
    }

    public Array<CustomComponentDTO> getCustomComponents() {
        return customComponents;
    }

    public void setCustomComponents(final Array<CustomComponentDTO> customComponents) {
        this.customComponents = customComponents;
    }

    @Override
    public boolean usesAsset(Asset assetToCheck, Map<String, Asset> assetMap) {
        if (modelComponent != null && modelComponent.usesAsset(assetToCheck, assetMap)) {
            return true;
        }

        if (terrainComponent != null && terrainComponent.usesAsset(assetToCheck, assetMap)) {
            return true;
        }

        if (waterComponent != null && waterComponent.usesAsset(assetToCheck, assetMap)) {
            return true;
        }

        if (customComponents != null) {
            for (int i = 0; i < customComponents.size; ++i) {
                final CustomComponentDTO customComponent = customComponents.get(i);
                if (customComponent.usesAsset(assetToCheck, assetMap)) {
                    return true;
                }
            }
        }

        return false;
    }
}
