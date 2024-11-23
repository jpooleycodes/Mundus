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
import com.badlogic.gdx.graphics.g3d.Attribute;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.UBJsonReader;
import com.mbrlabs.mundus.commons.assets.meta.Meta;
import com.mbrlabs.mundus.commons.assets.meta.MetaModel;
import com.mbrlabs.mundus.commons.g3d.MG3dModelLoader;
import com.mbrlabs.mundus.commons.utils.FileFormatUtils;
import com.mbrlabs.mundus.commons.utils.ModelUtils;
import net.mgsx.gltf.loaders.glb.GLBLoader;
import net.mgsx.gltf.loaders.gltf.GLTFLoader;
import net.mgsx.gltf.scene3d.scene.SceneAsset;
import org.w3c.dom.Attr;

import java.util.HashMap;
import java.util.Map;

import static net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute.*;

/**
 * @author Marcus Brummer
 * @version 01-10-2016
 */
public class ModelAsset extends Asset {
    protected static long TextureAttributeMask = Diffuse | Specular | Bump | Normal | Ambient | Emissive | Reflection | MetallicRoughnessTexture
            | OcclusionTexture | BaseColorTexture | NormalTexture | EmissiveTexture | BRDFLUTTexture;

    private Model model;

    private final Map<String, MaterialAsset> defaultMaterials;
    private final Array<Material> initialModelMaterials; // The initial materials for the model, before mundus modifies them

    public ModelAsset(Meta meta, FileHandle assetFile) {
        super(meta, assetFile);
        defaultMaterials = new HashMap<>();
        initialModelMaterials = new Array<>();
    }

    public Model getModel() {
        return model;
    }

    public Map<String, MaterialAsset> getDefaultMaterials() {
        return defaultMaterials;
    }

    @Override
    public void load() {
        // TODO don't create a new loader each time
        if (FileFormatUtils.isG3DB(file)) {
            MG3dModelLoader loader = new MG3dModelLoader(new UBJsonReader());
            model = loader.loadModel(file);
        } else if (FileFormatUtils.isGLTF(file)) {
            GLTFLoader loader = new GLTFLoader();
            model = loader.load(file).scene.model;
        } else if (FileFormatUtils.isGLB(file)) {
            GLBLoader loader = new GLBLoader();
            model = loader.load(file).scene.model;
        } else {
            throw new GdxRuntimeException("Unsupported 3D model");
        }

        copyMaterials();
        updateBoneCount();
     }

    @Override
    public void load(AssetManager assetManager) {
        Object modelObj = assetManager.get(meta.getFile().pathWithoutExtension());
        if (modelObj instanceof SceneAsset) {
            model = ((SceneAsset) modelObj).scene.model;
        } else if (modelObj instanceof Model) {
            model = (Model) modelObj;
        } else {
            throw new GdxRuntimeException("Unsupported 3D model");
        }

        copyMaterials();
        updateBoneCount();
    }

    @Override
    public void resolveDependencies(Map<String, Asset> assets) {
        // materials
        MetaModel metaModel = meta.getModel();
        if (metaModel == null) {
            return;
        }
        for (String g3dbMatID : metaModel.getDefaultMaterials().keys()) {
            String uuid = metaModel.getDefaultMaterials().get(g3dbMatID);
            defaultMaterials.put(g3dbMatID, (MaterialAsset) assets.get(uuid));
        }
    }

    @Override
    public void applyDependencies() {
        if (model == null) return;

        // materials
        for (Material mat : model.materials) {
            MaterialAsset materialAsset = defaultMaterials.get(mat.id);
            if (materialAsset == null) continue;
            materialAsset.applyToMaterial(mat);
        }
    }

    @Override
    public void dispose() {
        if (model != null) {
            model.dispose();
        }
    }

    @Override
    public boolean usesAsset(Asset assetToCheck) {
        // if it's a MaterialAsset compare to the models materials
        if (assetToCheck instanceof MaterialAsset) {
            return defaultMaterials.containsValue(assetToCheck);
        }

        // check if the materials use the asset, like a texture asset
        for (Map.Entry<String, MaterialAsset> stringMaterialAssetEntry : defaultMaterials.entrySet()) {
            if (stringMaterialAssetEntry.getValue().usesAsset(assetToCheck)) {
                return true;
            }
        }

        // This looks painful but all we are doing is checking if the texture asset being deleted is used
        // by the original model file. Why? because even if it's not in use by the active mundus material(s)
        // we still need it to load the model file properly since it's a dependency. This may also be used as
        // a way to "rollback" the mundus material to the models defaults
        if (assetToCheck instanceof TextureAsset) {
            for (Material material : initialModelMaterials) {
                Array<Attribute> attrs = material.get(new Array<Attribute>(), TextureAttributeMask);
                for (Attribute attr : attrs) {
                    if (attr instanceof TextureAttribute) {
                        TextureAttribute textureAttribute = (TextureAttribute) attr;
                        if (textureAttribute.textureDescription.texture == ((TextureAsset) assetToCheck).getTexture()) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    /**
     * Copy materials of the model before mundus has modified them
     */
    private void copyMaterials() {
        // Store a copy of the original unmodified model materials
        for (Material material : model.materials) {
            initialModelMaterials.add(new Material(material));
        }
    }

    private void updateBoneCount() {
        // Update bone count for model
        if (meta != null && meta.getModel() != null) {
            //This is to support models armatures being updated after initial import
            // as well as backwards compatability for projects existing prior to getting bone counts on import.
            meta.getModel().setNumBones(ModelUtils.getBoneCount(model));
        }
    }
}
