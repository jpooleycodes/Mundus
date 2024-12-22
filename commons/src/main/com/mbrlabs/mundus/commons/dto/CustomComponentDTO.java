/*
 * Copyright (c) 2024. See AUTHORS file.
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
import com.badlogic.gdx.utils.OrderedMap;
import com.mbrlabs.mundus.commons.assets.Asset;

import java.util.Map;

public class CustomComponentDTO implements AssetUsageDTO {

    private String componentType;

    private OrderedMap<String, String> properties;

    private Array<String> assetIds;

    public String getComponentType() {
        return componentType;
    }

    public void setComponentType(final String componentType) {
        this.componentType = componentType;
    }

    public OrderedMap<String, String> getProperties() {
        return properties;
    }

    public void setProperties(final OrderedMap<String, String> properties) {
        this.properties = properties;
    }

    public Array<String> getAssetIds() {
        return assetIds;
    }

    public void setAssetIds(final Array<String> assetIds) {
        this.assetIds = assetIds;
    }

    @Override
    public boolean usesAsset(final Asset assetToCheck, final Map<String, Asset> assetMap) {
        if (assetIds == null) {
            return false;
        }

        for (int i = 0; i < assetIds.size; ++i) {
            if (assetToCheck.getID().equals(assetIds.get(i))) {
                return true;
            }
        }

        return false;
    }
}
