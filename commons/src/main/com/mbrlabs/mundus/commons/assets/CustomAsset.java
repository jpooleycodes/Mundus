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

package com.mbrlabs.mundus.commons.assets;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.ObjectMap;
import com.mbrlabs.mundus.commons.assets.meta.Meta;

import java.util.Map;

/**
 * Custom asset for plugins.
 */
public class CustomAsset extends Asset {

    /**
     * @param meta The meta file.
     * @param assetFile The asset file.
     */
    public CustomAsset(Meta meta, FileHandle assetFile) {
        super(meta, assetFile);
    }

    @Override
    public void load() {

    }

    @Override
    public void load(AssetManager assetManager) {

    }

    @Override
    public void resolveDependencies(Map<String, Asset> assets) {

    }

    @Override
    public void applyDependencies() {

    }

    @Override
    public void dispose() {

    }

    @Override
    public boolean usesAsset(Asset assetToCheck) {
        return false;
    }

    public ObjectMap<String, String> getProperties() {
        return meta.getCustom().getProperties();
    }
}
