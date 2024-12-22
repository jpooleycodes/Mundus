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

package com.mbrlabs.mundus.commons.utils;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.mbrlabs.mundus.commons.assets.Asset;

import java.util.Map;

/**
 * Utils for assets.
 */
public class AssetUtils {

    public static ObjectMap<String, Asset> getAssetsById(final Array<String> assetIds, final Map<String, Asset> assetMap) {
        final ObjectMap<String, Asset> retMap = new ObjectMap<>();
        if (assetIds != null) {
            for (int i = 0; i < assetIds.size; ++i) {
                final String assetId = assetIds.get(i);
                if (assetId != null) {
                    final Asset asset = assetMap.get(assetId);

                    if (asset != null) {
                        retMap.put(assetId, asset);
                    }
                }
            }
        }

        return retMap;
    }
}
