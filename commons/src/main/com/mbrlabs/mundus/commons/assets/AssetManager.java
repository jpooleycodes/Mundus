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

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.loaders.TextureLoader;
import com.badlogic.gdx.assets.loaders.resolvers.AbsoluteFileHandleResolver;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.UBJsonReader;
import com.mbrlabs.mundus.commons.assets.meta.Meta;
import com.mbrlabs.mundus.commons.assets.meta.MetaFileParseException;
import com.mbrlabs.mundus.commons.assets.meta.MetaLoader;
import com.mbrlabs.mundus.commons.assets.meta.MetaModel;
import com.mbrlabs.mundus.commons.g3d.MG3dModelLoader;
import com.mbrlabs.mundus.commons.terrain.Terrain;
import com.mbrlabs.mundus.commons.terrain.TerrainLoader;
import com.mbrlabs.mundus.commons.utils.FileFormatUtils;
import net.mgsx.gltf.loaders.glb.GLBAssetLoader;
import net.mgsx.gltf.loaders.gltf.GLTFAssetLoader;
import net.mgsx.gltf.scene3d.scene.SceneAsset;

import java.io.File;
import java.io.FileFilter;
import java.util.HashMap;
import java.util.Map;

/**
 * Read-only asset manager.
 *
 * @author Marcus Brummer
 * @version 06-10-2016
 */
@SuppressWarnings("NonJREEmulationClassesInClientCode")
public class AssetManager implements Disposable {

    private static final String TAG = AssetManager.class.getSimpleName();

    protected FileHandle rootFolder;
    protected FileHandle[] metaFiles;
    protected final MetaLoader metaLoader = new MetaLoader();

    protected Array<Asset> assets;
    protected Map<String, Asset> assetIndex;
    protected com.badlogic.gdx.assets.AssetManager gdxAssetManager;

    // Tracks the highest bone count out of all loaded model assets
    public int maxNumBones = 0;

    /**
     * Asset manager constructor.
     *
     * @param assetsFolder
     *            root directory of assets
     */
    public AssetManager(FileHandle assetsFolder) {
        this.rootFolder = assetsFolder;
        this.assets = new Array<>();
        this.assetIndex = new HashMap<>();
    }

    /**
     * The Mundus AssetManager class encapsulates the libGDX AssetManager, mostly
     * for async loading
     */
    public com.badlogic.gdx.assets.AssetManager getGdxAssetManager() {
        return gdxAssetManager;
    }

    /**
     * Returns an asset by id.
     *
     * @param id
     *            id of asset
     * @return matching asset or null
     */
    public Asset findAssetByID(String id) {
        if (id == null) return null;
        return assetIndex.get(id);
    }

    /**
     * Returns an asset by filename, else null if not found.
     * @param fileName the filename to search
     * @return matching asset or null
     */
    public Asset findAssetByFileName(String fileName) {
        for (Asset asset : assets) {
            if (asset.file.name().equals(fileName))
                return asset;
        }
        return null;
    }

    public Map<String, Asset> getAssetMap() {
        return assetIndex;
    }

    public void addAsset(Asset asset) {
        if (asset == null) return;
        if (assetIndex.get(asset.getID()) == null) {
            assets.add(asset);
            assetIndex.put(asset.getID(), asset);
        }
    }

    /**
     * Returns all assets.
     *
     * @return all assets
     */
    public Array<Asset> getAssets() {
        return assets;
    }

    /**
     * Returns all assets of type MODEL.
     *
     * @return all model assets
     */
    public Array<ModelAsset> getModelAssets() {
        Array<ModelAsset> models = new Array<>();
        for (Asset asset : assets) {
            if (asset instanceof ModelAsset) {
                models.add((ModelAsset) asset);
            }
        }

        return models;
    }

    /**
     * Returns all assets of type TERRAIN.
     *
     * @return all model assets
     */
    public Array<TerrainAsset> getTerrainAssets() {
        Array<TerrainAsset> terrains = new Array<>();
        for (Asset asset : assets) {
            if (asset instanceof TerrainAsset) {
                terrains.add((TerrainAsset) asset);
            }
        }

        return terrains;
    }

    /**
     * Returns all assets of type MATERIAL.
     *
     * @return all model assets
     */
    public Array<MaterialAsset> getMaterialAssets() {
        Array<MaterialAsset> materials = new Array<>();
        for (Asset asset : assets) {
            if (asset instanceof MaterialAsset) {
                materials.add((MaterialAsset) asset);
            }
        }

        return materials;
    }

    /**
     * Queues all assets in the project's asset folder for loading later.
     *
     * Should be called before {@link #continueLoading()} and {@link #finalizeLoad()}
     *
     * @param isRuntime
     *            is this called by the runtime or editor (runtime requires different file logic)
     * @throws MetaFileParseException
     *             if a meta file can't be parsed
     */
    public void queueAssetsForLoading(boolean isRuntime) throws MetaFileParseException {

        String[] files;
        FileHandle fileList;

        if (isRuntime) {
            // assets.txt has relative/internal paths
            gdxAssetManager = new com.badlogic.gdx.assets.AssetManager(new InternalFileHandleResolver());
        } else {
            // For the editor, we get all files in the assets directory and absolute paths via rootFolder.list()
            // which has the added benefit of detecting assets not in the assets.txt file
            gdxAssetManager = new com.badlogic.gdx.assets.AssetManager(new AbsoluteFileHandleResolver());
        }

        if (isRuntime) {
            // Desktop applications cannot use .list() for internal jar files.
            // Application will need to provide an assets.txt file listing all Mundus assets
            // in the Mundus root directory.
            // https://lyze.dev/2021/04/29/libGDX-Internal-Assets-List/
            fileList = rootFolder.child("assets.txt");

            // Normalize line endings before reading
            files = fileList.readString().replaceAll("\\r\\n?", "\n").split("\\n");
            metaFiles = getMetaFiles(files);
        } else {
            // Editor uses this block to load meta files
            FileFilter metaFileFilter = new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return file.getName().endsWith(Meta.META_EXTENSION);
                }
            };
            metaFiles = rootFolder.list(metaFileFilter);
        }

        // Set loaders
        gdxAssetManager.setLoader(Terrain.class, ".terra", new TerrainLoader());
        gdxAssetManager.setLoader(SceneAsset.class, ".gltf", new GLTFAssetLoader());
        gdxAssetManager.setLoader(SceneAsset.class, ".glb", new GLBAssetLoader());
        gdxAssetManager.setLoader(Model.class, ".g3db", new MG3dModelLoader(new UBJsonReader(), gdxAssetManager.getFileHandleResolver()));

        // Queue files for async loading into LibGDX's assetManager
        for (FileHandle meta : metaFiles) {
            Meta m = metaLoader.load(meta);
            queueAssetForLoading(m);
        }
    }

    protected void queueAssetForLoading(Meta m) {
        String filePath = m.getFile().pathWithoutExtension();
        switch (m.getType()) {
            case TEXTURE:
                // These are hard coded for now
                TextureLoader.TextureParameter param = new TextureLoader.TextureParameter();
                param.genMipMaps = true;
                param.minFilter = Texture.TextureFilter.MipMapLinearLinear;
                param.magFilter = Texture.TextureFilter.Linear;

                gdxAssetManager.load(filePath, Texture.class, param);
                break;
            case PIXMAP_TEXTURE:
                gdxAssetManager.load(filePath, Pixmap.class);
                break;
            case MODEL:
                if (FileFormatUtils.isG3DB(filePath)) {
                    gdxAssetManager.load(filePath, Model.class);
                } else if (FileFormatUtils.isGLTF(filePath)) {
                    gdxAssetManager.load(filePath, SceneAsset.class);
                } else if (FileFormatUtils.isGLB(filePath)) {
                    gdxAssetManager.load(filePath, SceneAsset.class);
                } else {
                    throw new GdxRuntimeException("Unsupported 3D model");
                }
                break;
            case TERRAIN:
                TerrainLoader.TerrainParameter terrainParameter = new TerrainLoader.TerrainParameter(m.getTerrain());
                gdxAssetManager.load(filePath, Terrain.class, terrainParameter);
            case MATERIAL:
                // loads synchronously
                break;
            case WATER:
                // loads synchronously
                break;
            case SKYBOX:
                // loads synchronously
                break;
        }
    }

    /**
     * Should be called each frame until it returns true
     * which indicates that assets are loaded.
     *
     * @return boolean indicating if asynchronous loading is complete
     */
    public boolean continueLoading() throws MetaFileParseException, AssetNotFoundException {
        boolean complete = gdxAssetManager.update(17);
        if (complete) {
            finalizeLoad();
        }
        return complete;
    }

    /**
     * Returns a progress value between 0.0 and 1.0 representing the percentage loaded.
     * @return progress percentage
     */
    public float getProgress() {
        return gdxAssetManager.getProgress();
    }

    /**
     * Call to finalize the loading process.
     */
    public void finalizeLoad() throws AssetNotFoundException, MetaFileParseException {
        // Ensure loading is complete before continuing
        gdxAssetManager.finishLoading();

        // finalize loading of Mundus assets
        for (FileHandle meta : metaFiles) {
            loadAsset(metaLoader.load(meta));
        }

        // resolve material assets
        for (Asset asset : assets) {
            if (asset instanceof MaterialAsset) {
                asset.resolveDependencies(assetIndex);
                asset.applyDependencies();
            }
        }

        // resolve other assets
        for (Asset asset : assets) {
            if (asset instanceof MaterialAsset) continue;
            if (asset instanceof ModelAsset) {
                MetaModel model = asset.getMeta().getModel();
                if (model != null) {
                    int modelBones = model.getNumBones();
                    maxNumBones = Math.max(modelBones, maxNumBones);
                }
            }
            asset.resolveDependencies(assetIndex);
            asset.applyDependencies();
        }
    }

    /**
     * Get an array of Meta FileHandles for the given String array of file names.
     *
     * @param files the array of file names to retrieve meta filehandles from
     * @return FileHandle array of meta files.
     */
    private FileHandle[] getMetaFiles(String[] files) {
        // Get meta file extension file names
        Array<String> metalFileNames = new Array<>();
        for (String filename: files) {
            if (filename.endsWith(Meta.META_EXTENSION)) {
                metalFileNames.add(filename);
            }
        }

        FileHandle[] metaFiles = new FileHandle[metalFileNames.size];
        for (int i = 0; i < metaFiles.length; i++) {
            metaFiles[i] = rootFolder.child(metalFileNames.get(i));
        }

        return metaFiles;
    }

    /**
     * Loads an asset, given it's meta file.
     *
     * @param meta
     *            meta file of asset
     * @return asset or null
     * @throws AssetNotFoundException
     *             if a meta file points to a non existing asset
     */
    public Asset loadAsset(Meta meta) throws AssetNotFoundException {
        FileHandle assetFile = meta.getFile().sibling(meta.getFile().nameWithoutExtension());

        // check if asset exists
        if (!assetFile.exists()) {
            throw new AssetNotFoundException("Meta file found, but asset does not exist: " + meta.getFile().path());
        }

        // load actual asset
        Asset asset;
        switch (meta.getType()) {
            case TEXTURE:
                asset = loadTextureAsset(meta, assetFile);
                break;
            case PIXMAP_TEXTURE:
                asset = loadPixmapTextureAsset(meta, assetFile);
                break;
            case TERRAIN:
                asset = loadTerrainAsset(meta, assetFile);
                break;
            case MODEL:
                asset = loadModelAsset(meta, assetFile);
                break;
            case MATERIAL:
                asset = loadMaterialAsset(meta, assetFile);
                break;
            case WATER:
                asset = loadWaterAsset(meta, assetFile);
                break;
            case SKYBOX:
                asset = loadSkyboxAsset(meta, assetFile);
                break;
            default:
                return null;
        }

        addAsset(asset);
        return asset;
    }

    private Asset loadSkyboxAsset(Meta meta, FileHandle assetFile) {
        SkyboxAsset asset = new SkyboxAsset(meta, assetFile);
        asset.load(gdxAssetManager);
        return asset;
    }

    private MaterialAsset loadMaterialAsset(Meta meta, FileHandle assetFile) {
        MaterialAsset asset = new MaterialAsset(meta, assetFile);
        asset.load(gdxAssetManager);
        return asset;
    }

    private TextureAsset loadTextureAsset(Meta meta, FileHandle assetFile) {
        TextureAsset asset = new TextureAsset(meta, assetFile);
        // TODO parse special texture instead of always setting them
        asset.setTileable(true);
        asset.generateMipmaps(true);
        asset.load(gdxAssetManager);
        return asset;
    }

    private TerrainAsset loadTerrainAsset(Meta meta, FileHandle assetFile) {
        TerrainAsset asset = new TerrainAsset(meta, assetFile);
        asset.load(gdxAssetManager);
        return asset;
    }

    private PixmapTextureAsset loadPixmapTextureAsset(Meta meta, FileHandle assetFile) {
        PixmapTextureAsset asset = new PixmapTextureAsset(meta, assetFile);
        asset.load(gdxAssetManager);
        return asset;
    }

    protected ModelAsset loadModelAsset(Meta meta, FileHandle assetFile) {
        ModelAsset asset = new ModelAsset(meta, assetFile);
        asset.load(gdxAssetManager);
        return asset;
    }

    private WaterAsset loadWaterAsset(Meta meta, FileHandle assetFile) {
        WaterAsset asset = new WaterAsset(meta, assetFile);
        asset.load(gdxAssetManager);
        return asset;
    }

    @Override
    public void dispose() {
        Gdx.app.log(TAG, "Disposing assets...");
        for (Asset asset : assets) {
            asset.dispose();
        }
        Gdx.app.log(TAG, "Assets disposed");
        assets.clear();
        assetIndex.clear();
    }

    /**
     * Native JavaScript string split method for GWT support
     *
     * No longer needed: dead code
     */
    public static final native String[] split(String string, String separator) /*-{
        return string.split(separator);
    }-*/;

    /**
     * Used to inform users about the current loading status.
     */
    public interface AssetLoadingListener {
        /**
         * Called if an asset loaded
         * 
         * @param asset
         *            loaded asset
         * @param progress
         *            number of already loaded assets
         * @param assetCount
         *            total number of assets
         */
        void onLoad(Asset asset, int progress, int assetCount);

        /**
         * Called if all assets loaded.
         * 
         * @param assetCount
         *            total number of loaded assets
         */
        void onFinish(int assetCount);
    }

}
