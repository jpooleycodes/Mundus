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

package com.mbrlabs.mundus.editor.assets

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute
import com.mbrlabs.mundus.commons.assets.MaterialAsset
import com.mbrlabs.mundus.commons.assets.TextureAsset
import com.mbrlabs.mundus.commons.assets.TexCoordInfo
import com.mbrlabs.mundus.editor.Mundus
import com.mbrlabs.mundus.editor.core.registry.Registry
import com.mbrlabs.mundus.editor.events.SettingsChangedEvent
import com.mbrlabs.mundus.editor.utils.*
import net.mgsx.gltf.scene3d.attributes.PBRColorAttribute
import net.mgsx.gltf.scene3d.attributes.PBRFloatAttribute
import net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute
import org.apache.commons.io.FilenameUtils
import java.util.HashMap

/**
 * @author Marcus Brummer
 * @version 12-12-2015
 */
class ModelImporter(private val registry: Registry) : SettingsChangedEvent.SettingsChangedListener {

    private val fbxConv: FbxConv

    init {
        Mundus.registerEventListener(this)
        this.fbxConv = FbxConv(registry.settings.fbxConvBinary)
    }

    override fun onSettingsChanged(event: SettingsChangedEvent) {
        fbxConv.setFbxBinary(event.settings.fbxConvBinary)
    }

    fun importToTempFolder(modelFile: FileHandle?): FileHandleWithDependencies? {
        if (modelFile == null || !modelFile.exists()) {
            return null
        }

        val modelFileWithDependencies = FileHandleWithDependencies(modelFile)

        var retFile: FileHandleWithDependencies? = null
        val tempModelCache = registry.createTempFolder()

        // copy model file
        modelFileWithDependencies.copyTo(tempModelCache)
        val rawModelFile = Gdx.files.absolute(FilenameUtils.concat(tempModelCache.path(), modelFile.name()))
        if (!rawModelFile.exists()) {
            return null
        }

        // convert copied importer
        val convert = isFBX(rawModelFile) || isCollada(rawModelFile)
                || isWavefont(rawModelFile)

        if (convert) {
            fbxConv.clear()
            val convResult =
                fbxConv.input(rawModelFile.path()).output(tempModelCache.file().absolutePath).flipTexture(true)
                    .execute()

            if (convResult.isSuccess) {
                retFile = FileHandleWithDependencies(Gdx.files.absolute(convResult.outputFile))
            }
        } else if (isG3DB(rawModelFile) || isGLTF(rawModelFile) || isGLB(rawModelFile)) {
            retFile = FileHandleWithDependencies(rawModelFile)
        }

        // check if converted file exists
        return if (retFile != null && retFile.exists()) retFile else null
    }

    /**
     * Populate a material asset based on the given material. The imported model is
     * required for resolving image/texture files.
     */
    fun populateMaterialAsset(
        importedModel: FileHandleWithDependencies?,
        assetManager: EditorAssetManager,
        materialToUse: Material,
        materialAssetToPopulate: MaterialAsset,
    ) {
        // Import supported attributes from the imported models materials into Mundus material assets

        if (materialToUse.has(BlendingAttribute.Type)) {
            val attr = materialToUse.get(BlendingAttribute.Type) as BlendingAttribute
            materialAssetToPopulate.opacity = attr.opacity
        }

        if (materialToUse.has(IntAttribute.CullFace)) {
            val attr = materialToUse.get(IntAttribute.CullFace) as IntAttribute
            materialAssetToPopulate.cullFace = attr.value
        }

        // Color Attributes
        if (materialToUse.has(PBRColorAttribute.BaseColorFactor)) {
            val attr = materialToUse.get(PBRColorAttribute.BaseColorFactor) as PBRColorAttribute
            materialAssetToPopulate.diffuseColor.set(attr.color)
        }

        if (materialToUse.has(ColorAttribute.Emissive)) {
            val attr = materialToUse.get(ColorAttribute.Emissive) as ColorAttribute
            materialAssetToPopulate.emissiveColor.set(attr.color)
        }

        // Texture Attributes
        if (materialToUse.has(PBRTextureAttribute.BaseColorTexture)) {
            materialAssetToPopulate.diffuseTexture = getTextureAssetForMaterial(
                importedModel!!,
                assetManager,
                materialToUse.id,
                PBRTextureAttribute.BaseColorTexture)
            populateTextureCoordInfo(PBRTextureAttribute.BaseColorTexture, materialToUse, materialAssetToPopulate.diffuseTexCoord)
        }

        if (materialToUse.has(PBRTextureAttribute.NormalTexture)) {
            materialAssetToPopulate.normalMap = getTextureAssetForMaterial(
                importedModel!!,
                assetManager,
                materialToUse.id,
                PBRTextureAttribute.NormalTexture
            )
            populateTextureCoordInfo(PBRTextureAttribute.NormalTexture, materialToUse, materialAssetToPopulate.normalTexCoord)
        }

        if (materialToUse.has(PBRTextureAttribute.EmissiveTexture)) {
            materialAssetToPopulate.emissiveTexture = getTextureAssetForMaterial(
                importedModel!!,
                assetManager,
                materialToUse.id,
                PBRTextureAttribute.EmissiveTexture
            )
            populateTextureCoordInfo(PBRTextureAttribute.EmissiveTexture, materialToUse, materialAssetToPopulate.emissiveTexCoord)
        }

        if (materialToUse.has(PBRTextureAttribute.MetallicRoughnessTexture)) {
            materialAssetToPopulate.metallicRoughnessTexture = getTextureAssetForMaterial(
                importedModel!!,
                assetManager,
                materialToUse.id,
                PBRTextureAttribute.MetallicRoughnessTexture
            )
            populateTextureCoordInfo(PBRTextureAttribute.MetallicRoughnessTexture, materialToUse, materialAssetToPopulate.metallicRoughnessTexCoord)
        }

        if (materialToUse.has(PBRTextureAttribute.OcclusionTexture)) {
            materialAssetToPopulate.occlusionTexture = getTextureAssetForMaterial(
                importedModel!!,
                assetManager,
                materialToUse.id,
                PBRTextureAttribute.OcclusionTexture
            )
            populateTextureCoordInfo(PBRTextureAttribute.OcclusionTexture, materialToUse, materialAssetToPopulate.occlusionTexCoord)
        }

        // Float attributes
        if (materialToUse.has(PBRFloatAttribute.Metallic)) {
            val attr = materialToUse.get(PBRFloatAttribute.Metallic) as PBRFloatAttribute
            materialAssetToPopulate.metallic = attr.value
        }

        if (materialToUse.has(PBRFloatAttribute.Roughness)) {
            val attr = materialToUse.get(PBRFloatAttribute.Roughness) as PBRFloatAttribute
            materialAssetToPopulate.roughness = attr.value
        }

        if (materialToUse.has(FloatAttribute.AlphaTest)) {
            val attr = materialToUse.get(FloatAttribute.AlphaTest) as FloatAttribute
            materialAssetToPopulate.alphaTest = attr.value
        }

        if (materialToUse.has(PBRFloatAttribute.NormalScale)) {
            val attr = materialToUse.get(PBRFloatAttribute.NormalScale) as PBRFloatAttribute
            materialAssetToPopulate.normalScale = attr.value
        }
    }

    /**
     * Populates the TexCoordInfo POJO with UV data from the attribute
     */
    private fun populateTextureCoordInfo(type: Long, materialToUse: Material, texCoordInfo: TexCoordInfo) {
        val attr = materialToUse.get(type) as PBRTextureAttribute
        texCoordInfo.uvIndex = attr.uvIndex
        texCoordInfo.offsetU = attr.offsetU
        texCoordInfo.offsetV = attr.offsetV
        texCoordInfo.scaleU = attr.scaleU
        texCoordInfo.scaleV = attr.scaleV
        texCoordInfo.rotationUV = attr.rotationUV
    }

    /**
     * Create or retrieve a TextureAsset for the given TextureAttribute
     */
    private fun getTextureAssetForMaterial(
        importedModel: FileHandleWithDependencies,
        assetManager: EditorAssetManager,
        materialName: String,
        textureAttribute: Long
    ): TextureAsset? {

        // If the parsed GLTF materials do not contain a material with this name, cannot continue
        if (!importedModel.materialImageMap.contains(materialName)) return null

        // Map that contains Attribute, Int. The Int is the images array index
        val imageIndexMap: HashMap<Long, Int>? = importedModel.materialImageMap[materialName]

        // If the map contains an image for the given texture attribute...
        if (imageIndexMap!!.contains(textureAttribute)) {
            // Retrieve the image index for this texture attribute and create a texture asset for it.
            val index = imageIndexMap[textureAttribute]!!

            // For GLTF embedded files this will allow them to import albeit the packed textures will not be usable.
            if (importedModel.images.isEmpty) {
                return null
            }

            return assetManager.getOrCreateTextureAsset(importedModel.images.get(index))
        }

        return null
    }

}
