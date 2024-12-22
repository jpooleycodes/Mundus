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

package com.mbrlabs.mundus.editor.ui.modules.dialogs.importer

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Cubemap
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.UBJsonReader
import com.kotcrab.vis.ui.util.dialog.Dialogs
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisTextButton
import com.mbrlabs.mundus.commons.assets.ModelAsset
import com.mbrlabs.mundus.commons.assets.meta.MetaModel
import com.mbrlabs.mundus.commons.g3d.MG3dModelLoader
import com.mbrlabs.mundus.commons.shaders.MundusPBRShaderProvider
import com.mbrlabs.mundus.commons.utils.LightUtils
import com.mbrlabs.mundus.commons.utils.ModelUtils
import com.mbrlabs.mundus.commons.utils.MundusShaderParser
import com.mbrlabs.mundus.commons.utils.ShaderUtils
import com.mbrlabs.mundus.editor.Mundus
import com.mbrlabs.mundus.editor.assets.FileHandleWithDependencies
import com.mbrlabs.mundus.editor.assets.MetaSaver
import com.mbrlabs.mundus.editor.assets.ModelImporter
import com.mbrlabs.mundus.editor.core.project.ProjectManager
import com.mbrlabs.mundus.editor.events.AssetImportEvent
import com.mbrlabs.mundus.editor.events.LogEvent
import com.mbrlabs.mundus.editor.events.LogType
import com.mbrlabs.mundus.editor.ui.UI
import com.mbrlabs.mundus.editor.ui.modules.dialogs.BaseDialog
import com.mbrlabs.mundus.editor.ui.widgets.FileChooserField
import com.mbrlabs.mundus.editor.ui.widgets.RenderWidget
import com.mbrlabs.mundus.editor.utils.Log
import com.mbrlabs.mundus.editor.utils.isCollada
import com.mbrlabs.mundus.editor.utils.isFBX
import com.mbrlabs.mundus.editor.utils.isG3DB
import com.mbrlabs.mundus.editor.utils.isGLB
import com.mbrlabs.mundus.editor.utils.isGLTF
import com.mbrlabs.mundus.editor.utils.isWavefont
import com.mbrlabs.mundus.editorcommons.exceptions.AssetAlreadyExistsException
import net.mgsx.gltf.loaders.glb.GLBLoader
import net.mgsx.gltf.loaders.gltf.GLTFLoader
import net.mgsx.gltf.scene3d.attributes.PBRCubemapAttribute
import net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute
import net.mgsx.gltf.scene3d.lights.DirectionalLightEx
import net.mgsx.gltf.scene3d.scene.SceneRenderableSorter
import net.mgsx.gltf.scene3d.shaders.PBRDepthShaderProvider
import net.mgsx.gltf.scene3d.shaders.PBRShaderConfig
import net.mgsx.gltf.scene3d.shaders.PBRShaderProvider
import net.mgsx.gltf.scene3d.utils.IBLBuilder
import java.io.IOException
import java.nio.file.Paths

/**
 * @author Marcus Brummer
 * @version 07-06-2016
 */
class ImportModelDialog : BaseDialog("Import Mesh"), Disposable {

    companion object {
        private val TAG = ImportModelDialog::class.java.simpleName
    }

    private val importMeshTable: ImportModelTable

    private val modelImporter: ModelImporter = Mundus.inject()
    private val projectManager: ProjectManager = Mundus.inject()

    init {
        isModal = true
        isMovable = true

        val root = VisTable()
        add<Table>(root).expand().fill()
        importMeshTable = ImportModelTable()

        root.add(importMeshTable).minWidth(600f).expand().fill().left().top()
    }

    override fun dispose() {
        importMeshTable.dispose()
    }

    override fun close() {
        importMeshTable.deleteTempModel()
        importMeshTable.resetDialog()
        super.close()
    }

    /**
     */
    private inner class ImportModelTable : VisTable(), Disposable {
        // UI elements
        private var renderWidget: RenderWidget? = null
        private val importBtn = VisTextButton("IMPORT")
        private val modelInput = FileChooserField(300)

        // preview model + instance
        private var previewModel: Model? = null
        private var previewInstance: ModelInstance? = null

        private var importedModel: FileHandleWithDependencies? = null
        private var maxBones = 0

        private var modelBatch: ModelBatch? = null
        private val cam: PerspectiveCamera = PerspectiveCamera()
        private val env: Environment

        private var brdfLUT: Texture? = null
        private var diffuseCubemap: Cubemap? = null
        private var environmentCubemap: Cubemap? = null
        private var specularCubemap: Cubemap? = null

        init {

            cam.position.set(0f, 5f, 5f)
            cam.lookAt(0f, 0f, 0f)
            cam.near = 0.1f
            cam.far = 100f
            cam.update()

            env = Environment()

            val directionalLightEx = DirectionalLightEx()
            directionalLightEx.intensity = LightUtils.DEFAULT_INTENSITY
            directionalLightEx.setColor(LightUtils.DEFAULT_COLOR)
            directionalLightEx.direction.set(-1f, -0.8f, -0.2f)

            val iblBuilder = IBLBuilder.createOutdoor(directionalLightEx)
            environmentCubemap = iblBuilder.buildEnvMap(1024)
            diffuseCubemap = iblBuilder.buildIrradianceMap(256)
            specularCubemap = iblBuilder.buildRadianceMap(10)
            iblBuilder.dispose()

            brdfLUT = Texture(Gdx.files.classpath("net/mgsx/gltf/shaders/brdfLUT.png"))
            env.set(ColorAttribute(ColorAttribute.AmbientLight, 1f, 1f, 1f, 1f))
            env.set(PBRTextureAttribute(PBRTextureAttribute.BRDFLUTTexture, brdfLUT))
            env.set(PBRCubemapAttribute.createSpecularEnv(specularCubemap))
            env.set(PBRCubemapAttribute.createDiffuseEnv(diffuseCubemap))

            this.setupUI()
            this.setupListener()
        }

        private fun setupUI() {
            val root = VisTable()
            // root.debugAll();
            root.padTop(6f).padRight(6f).padBottom(22f)
            add(root)

            val inputTable = VisTable()
            renderWidget = RenderWidget(cam)
            renderWidget!!.setRenderer { camera ->
                if (previewInstance != null) {
                    try {
                        Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT)
                        previewInstance!!.transform.rotate(0f, 1f, 0f, -1f)
                        modelBatch?.begin(camera)
                        modelBatch?.render(previewInstance!!, env)
                        modelBatch?.end()
                    } catch (ex: GdxRuntimeException) {
                        Dialogs.showErrorDialog(stage, ex.message)
                        Mundus.postEvent(LogEvent(LogType.ERROR, ex.toString()))
                        previewInstance = null
                    }
                }
            }

            root.add(inputTable).width(300f).height(300f).padRight(10f)
            root.addSeparator(true)
            root.add<RenderWidget>(renderWidget).width(300f).height(300f).expand().fill()

            inputTable.left().top()

            val label = VisLabel()
            label.setText("The recommended format is '.gltf' separate (bin file, gltf file, textures). Mundus relies on textures being external image files," +
                    " so using binary files like .glb or embedded .gltf where the files are compressed and packed into the binary is " +
                    "not recommended. Automatic importing of material attributes only works with separate .gltf files currently.")
            label.wrap = true
            label.width = 300f
            inputTable.add(label).expandX().prefWidth(300f).padBottom(10f).row()

            inputTable.add(VisLabel("Model File")).left().padBottom(5f).row()
            inputTable.add(modelInput).fillX().expandX().padBottom(10f).row()
            inputTable.add(importBtn).fillX().expand().bottom()

            modelInput.setEditable(false)
        }

        private fun setupListener() {

            // model chooser
            modelInput.setCallback { fileHandle ->
                if (fileHandle.exists()) {
                    loadAndShowPreview(modelInput.file)
                }
            }

            // import btn
            importBtn.addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    if (previewModel != null && previewInstance != null) {
                        try {
                            val modelAsset = importModel()
                            val modelBoneCount = modelAsset.meta.model.numBones

                            // If the imported model has more bones than current shaders numBones, create new
                            // shader provider with new max bones.
                            if (modelBoneCount > projectManager.current().assetManager.maxNumBones) {
                                val config = ShaderUtils.buildPBRShaderConfig(modelBoneCount)
                                projectManager.modelBatch = ModelBatch(MundusPBRShaderProvider(config), SceneRenderableSorter())

                                val depthConfig = ShaderUtils.buildPBRShaderDepthConfig(modelBoneCount)
                                projectManager.setDepthBatch((ModelBatch(PBRDepthShaderProvider(depthConfig))))

                                projectManager.current().assetManager.maxNumBones = modelBoneCount
                                Mundus.postEvent(LogEvent(LogType.INFO, "Max Bone count increased to $modelBoneCount"))
                            }

                            Mundus.postEvent(AssetImportEvent(modelAsset))
                            UI.toaster.success("Mesh imported")
                        } catch (e: IOException) {
                            e.printStackTrace()
                            UI.toaster.error("Error while creating a ModelAsset")
                        } catch (ee: AssetAlreadyExistsException) {
                            Log.exception(TAG, ee)
                            UI.toaster.error("Error: There already exists a model with the same name")
                        }

                        dispose()
                        close()
                    } else {
                        UI.toaster.error("There is nothing to import")
                    }
                }
            })
        }

        @Throws(IOException::class, AssetAlreadyExistsException::class)
        private fun importModel(): ModelAsset {

            // create model asset
            val assetManager = projectManager.current().assetManager
            val modelAsset = assetManager.createModelAsset(importedModel!!)

            // create materials
            modelAsset.meta.model = MetaModel()
            for (mat in modelAsset.model.materials) {
                val materialAsset = assetManager.createMaterialAsset(modelAsset.id.substring(0, 4) + "_" + mat.id)

                modelImporter.populateMaterialAsset(importedModel!!, projectManager.current().assetManager, mat, materialAsset)
                projectManager.current().assetManager.saveMaterialAsset(materialAsset)

                modelAsset.meta.model.defaultMaterials.put(mat.id, materialAsset.id)
                modelAsset.defaultMaterials.put(mat.id, materialAsset)
            }

            modelAsset.meta.model.numBones = maxBones

            // save meta file
            val saver = MetaSaver()
            saver.save(modelAsset.meta)

            modelAsset.applyDependencies()

            return modelAsset
        }

        private fun loadAndShowPreview(model: FileHandle) {
            this.importedModel = modelImporter.importToTempFolder(model)

            if (importedModel == null) {
                if (isCollada(model) || isFBX(model)
                        || isWavefont(model)) {
                    Dialogs.showErrorDialog(stage, "Import error\nPlease make sure you specified the right "
                            + "files & have set the correct fbc-conv binary in the settings menu.")
                } else {
                    Dialogs.showErrorDialog(stage, "Import error\nPlease make sure you specified the right files")
                }
            }

            // load and show preview
            if (importedModel != null) {
                try {
                    if (isG3DB(importedModel!!.file)) {
                        val modelData = MG3dModelLoader(UBJsonReader()).loadModelData(importedModel!!.file)
                        previewModel = Model(modelData)
                    } else if (isGLTF(importedModel!!.file)) {
                        previewModel = GLTFLoader().load(importedModel!!.file).scene.model
                    } else if (isGLB(importedModel!!.file)) {
                        previewModel = GLBLoader().load(importedModel!!.file).scene.model
                    } else {
                        throw GdxRuntimeException("Unsupported 3D format")
                    }

                    maxBones = ModelUtils.getBoneCount(previewModel)

                    previewInstance = ModelInstance(previewModel!!)
                    showPreview()
                } catch (e: GdxRuntimeException) {
                    Dialogs.showErrorDialog(stage, e.message)
                }

            }
        }

        private fun showPreview() {
            previewInstance = ModelInstance(previewModel!!)

            val config = PBRShaderConfig()
            config.numDirectionalLights = 1
            config.numBones = maxBones
            config.vertexShader = MundusShaderParser.parse(Gdx.files.classpath("com/mbrlabs/mundus/commons/shaders/pbr/pbr.vs.glsl"))
            config.fragmentShader = MundusShaderParser.parse(Gdx.files.classpath("com/mbrlabs/mundus/commons/shaders/pbr/pbr.fs.glsl"))

            modelBatch = ModelBatch(PBRShaderProvider(config))

            // scale to 2 open gl units
            val boundingBox = previewInstance!!.calculateBoundingBox(BoundingBox())
            val max = boundingBox.getMax(Vector3())
            var maxDim = 0f
            if (max.x > maxDim) maxDim = max.x
            if (max.y > maxDim) maxDim = max.y
            if (max.z > maxDim) maxDim = max.z
            previewInstance!!.transform.scl(2f / maxDim)
        }

        override fun dispose() {
            if (previewModel != null) {
                previewModel!!.dispose()
                previewModel = null
                previewInstance = null
            }
            modelBatch?.dispose()
            modelInput.clear()
        }

        fun deleteTempModel() {
            if (importedModel == null) return

            val parentDirectory = importedModel?.file?.parent()
            val isDir = parentDirectory?.isDirectory
            if (isDir == true) {
                val path = Paths.get("mundus", "temp")
                val tempModelDirPath = parentDirectory.file().absolutePath
                // A defensive check, just to make sure the directory we are deleting is in the temp directory
                if (tempModelDirPath.contains(path.toString())) {
                    parentDirectory.deleteDirectory()
                    Mundus.postEvent(LogEvent("Deleted temporary model directory at $tempModelDirPath"))
                }
            }

            importedModel = null
        }

        fun resetDialog() {
            previewInstance = null
            modelInput.clear()
        }
    }

}
