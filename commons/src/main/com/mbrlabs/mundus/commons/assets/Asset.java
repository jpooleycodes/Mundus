/*
 * Copyright (c) 2016. See AUTHORS file.
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
import com.badlogic.gdx.utils.Disposable;
import com.mbrlabs.mundus.commons.assets.meta.Meta;
import com.mbrlabs.mundus.commons.scene3d.components.AssetUsage;

import java.util.Map;

/**
 * A generic asset type.
 *
 * Assets hold a file handle to the asset file. They also have a meta file,
 * which contains meta information about the asset. Assets can have dependencies
 * to other assets by specifying the asset id in the meta file or in the asset
 * file.
 * 
 * @author Marcus Brummer
 * @version 01-10-2016
 */
public abstract class Asset implements Disposable, AssetUsage {

    protected FileHandle file;
    protected Meta meta;

    /**
     *
     * @param meta
     * @param assetFile
     */
    public Asset(Meta meta, FileHandle assetFile) {
        this.meta = meta;
        this.file = assetFile;
    }

    public Meta getMeta() {
        return meta;
    }

    public String getName() {
        return file.name();
    }

    public FileHandle getFile() {
        return file;
    }

    public String getID() {
        return meta.getUuid();
    }

    @Override
    public String toString() {
        return "[" + getMeta().getType().toString() + "] " + file.name();
    }

    /**
     * Loads the asset.
     *
     * Loads the asset from disk synchronously and creates it.
     */
    public abstract void load();

    /**
     * Loads the asset using the given asset manager, usually after asynchronous project load.
     */
    public abstract void load(AssetManager assetManager);

    /**
     * Resolves all dependencies of this asset.
     *
     * Before calling this method all assets must have been loaded using the
     * load() method.
     * 
     * @param assets
     *            map of loaded assets with asset id as key
     */
    public abstract void resolveDependencies(Map<String, Asset> assets);

    /**
     * Applies dependent assets to this one.
     *
     * If dependencies have been set, this method applies them to the asset.
     * Note, that the asset and all it's set dependencies must have called
     * load() before calling this method.
     */
    public abstract void applyDependencies();

    /**
     * Returns the loading priority of this asset. Some assets need to be loaded
     * before others, e.g. a material asset needs to be loaded before a Model
     *
     * @return the loading priority of this asset
     */
    public int getLoadingPriority() {
        if (this instanceof MaterialAsset || this instanceof TerrainLayerAsset) {
            return 1;
        }
        return 2;
    }

}
