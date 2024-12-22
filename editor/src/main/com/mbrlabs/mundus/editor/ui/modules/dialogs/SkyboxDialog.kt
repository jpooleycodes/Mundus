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

package com.mbrlabs.mundus.editor.ui.modules.dialogs

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Array
import com.kotcrab.vis.ui.util.FloatDigitsOnlyFilter
import com.kotcrab.vis.ui.util.dialog.Dialogs
import com.kotcrab.vis.ui.widget.VisCheckBox
import com.kotcrab.vis.ui.widget.VisDialog
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisSelectBox
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisTextButton
import com.kotcrab.vis.ui.widget.VisTextField
import com.mbrlabs.mundus.commons.assets.Asset
import com.mbrlabs.mundus.commons.assets.SkyboxAsset
import com.mbrlabs.mundus.commons.skybox.Skybox
import com.mbrlabs.mundus.editor.Mundus
import com.mbrlabs.mundus.editor.core.project.ProjectManager
import com.mbrlabs.mundus.editor.events.LogEvent
import com.mbrlabs.mundus.editor.events.LogType
import com.mbrlabs.mundus.editor.shader.Shaders
import com.mbrlabs.mundus.editor.ui.widgets.ImageChooserField
import com.mbrlabs.mundus.editor.utils.createDefaultSkybox
import com.mbrlabs.mundus.editorcommons.events.ProjectChangedEvent
import com.mbrlabs.mundus.editorcommons.events.SceneChangedEvent
import com.mbrlabs.mundus.editorcommons.exceptions.AssetAlreadyExistsException

/**
 * @author Marcus Brummer
 * @version 10-01-2016
 */
class SkyboxDialog : BaseDialog("Skybox"), ProjectChangedEvent.ProjectChangedListener,
        SceneChangedEvent.SceneChangedListener, ImageChooserField.ImageChosenListener {

    private lateinit var root: VisTable
    private lateinit var selectBox: VisSelectBox<SkyboxAsset>
    private lateinit var rotateEnabled: VisCheckBox
    private var rotateSpeed = VisTextField()

    private val positiveX: ImageChooserField = ImageChooserField(100, false, this)
    private var negativeX: ImageChooserField = ImageChooserField(100, false, this)
    private var positiveY: ImageChooserField = ImageChooserField(100, false, this)
    private var negativeY: ImageChooserField = ImageChooserField(100, false, this)
    private var positiveZ: ImageChooserField = ImageChooserField(100, false, this)
    private var negativeZ: ImageChooserField = ImageChooserField(100, false, this)

    private var createBtn = VisTextButton("Create skybox")
    private var defaultBtn = VisTextButton("Create default skybox")
    private var deletBtn = VisTextButton("Remove Skybox")

    private var skyboxName = VisTextField()

    private val projectManager: ProjectManager = Mundus.inject()

    init {
        Mundus.registerEventListener(this)

        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        positiveX.setButtonText("Right (+X)")
        negativeX.setButtonText("Left (-X)")
        positiveY.setButtonText("Top (+Y)")
        negativeY.setButtonText("Bottom (-Y)")
        positiveZ.setButtonText("Front (+Z)")
        negativeZ.setButtonText("Back (-Z)")

        positiveX.isRequireSquareImage = true
        negativeX.isRequireSquareImage = true
        positiveY.isRequireSquareImage = true
        negativeY.isRequireSquareImage = true
        positiveZ.isRequireSquareImage = true
        negativeZ.isRequireSquareImage = true

        positiveX.isRequirePowerOfTwo = true
        negativeX.isRequirePowerOfTwo = true
        positiveY.isRequirePowerOfTwo = true
        negativeY.isRequirePowerOfTwo = true
        positiveZ.isRequirePowerOfTwo = true
        negativeZ.isRequirePowerOfTwo = true

        rotateEnabled = VisCheckBox(null)

        root = VisTable()
        add(root).left().top()

        root.add(VisLabel("Current Skybox")).left().row()
        root.addSeparator().row()

        // Skybox selector
        selectBox = VisSelectBox<SkyboxAsset>()
        val settingsTable = VisTable()
        settingsTable.defaults().padTop(10f).padLeft(10f).padRight(6f)
        settingsTable.add(VisLabel("Active Skybox:"))
        settingsTable.add(selectBox).left().row()

        settingsTable.defaults().padTop(10f).padLeft(10f).padRight(6f)
        settingsTable.add(VisLabel("Rotate: ")).left()
        settingsTable.add(rotateEnabled).left().row()
        settingsTable.add(VisLabel("Rotate Speed: "))
        settingsTable.add(rotateSpeed).row()
        root.add(settingsTable).left().padBottom(10f).row()

        // Image pickers
        root.add(VisLabel("Create a Skybox")).left().padTop(10f).row()
        val imageChooserTable = VisTable()
        imageChooserTable.addSeparator().colspan(3).row()
        imageChooserTable.add(VisLabel("The 6 images must be square and of equal size")).colspan(3).row()
        imageChooserTable.padTop(6f).padRight(6f)
        imageChooserTable.add(positiveX)
        imageChooserTable.add(negativeX)
        imageChooserTable.add(positiveY).row()
        imageChooserTable.add(negativeY)
        imageChooserTable.add(positiveZ)
        imageChooserTable.add(negativeZ).row()
        root.add(imageChooserTable).left().top().row()

        // Create skybox buttons
        val createTable = VisTable()
        createTable.defaults().padTop(15f).padLeft(6f).padRight(6f)
        createTable.add(VisLabel("Skybox Name: ")).left()
        createTable.add(skyboxName).row()
        createTable.add(createBtn).colspan(2).center()
        root.add(createTable).row()

        // Options
        root.add(VisLabel("Options")).left().padTop(10f).row()
        root.addSeparator().row()

        val tab = VisTable()
        tab.defaults().padTop(15f).padLeft(6f).padRight(6f).padBottom(15f)
        tab.add(defaultBtn).expandX().fillX()
        tab.add(deletBtn).expandX().fillX().row()
        root.add(tab).fillX().expandX().row()

        // Disable by default until name field is populated
        createBtn.isDisabled = true
    }

    private fun setupListeners() {
        var projectContext = projectManager.current()

        // skybox select
        selectBox.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                projectContext.currScene.skybox?.dispose()

                projectManager.current().currScene.setSkybox(selectBox.selected, Shaders.skyboxShader)
                resetImages()
                resetFields()
            }
        })

        rotateEnabled.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                projectManager.current().currScene.skybox.isRotateEnabled = rotateEnabled.isChecked
                addModifiedAsset()
            }
        })

        rotateSpeed.textFieldFilter = FloatDigitsOnlyFilter(true)
        rotateSpeed.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                if (rotateSpeed.isInputValid && !rotateSpeed.isEmpty) {
                    try {
                        projectManager.current().currScene.skybox.rotateSpeed = rotateSpeed.text.toFloat()
                        addModifiedAsset()
                    } catch (ex : NumberFormatException) {
                        Mundus.postEvent(LogEvent(LogType.ERROR,"Error parsing field " + rotateSpeed.name))
                    }
                }
            }
        })

        // Name field listener for enabling create button
        skyboxName.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                validateFields()
            }
        })

        // create btn
        createBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                if (createBtn.isDisabled)
                    return

                // check if skybox asset with same name exists
                for (asset in projectManager.current().assetManager.getSkyboxAssets()) {
                    // drop the .sky file extension
                    val name = asset.name.dropLast(4)

                    if (name == skyboxName.text) {
                        skyboxName.isInputValid = false
                        Dialogs.showErrorDialog(stage, "A skybox with the same name already exists.")
                        return
                    }
                }

                val oldSkybox = projectContext.currScene.skybox
                oldSkybox?.dispose()

                // Set actual skybox
                projectManager.current().currScene.skybox = Skybox(positiveX.file, negativeX.file,
                        positiveY.file, negativeY.file, positiveZ.file, negativeZ.file, Shaders.skyboxShader)

                val files = ArrayList<FileHandle>()
                files.add(positiveX.file)
                files.add(negativeX.file)
                files.add(positiveY.file)
                files.add(negativeY.file)
                files.add(positiveZ.file)
                files.add(negativeZ.file)

                val textureAssets = ArrayList<String>()

                // Create texture assets for each skybox image
                for (file in files) {
                    var asset: Asset

                    try {
                        asset = projectManager.current().assetManager.createTextureAsset(file)
                    } catch (e: AssetAlreadyExistsException) {
                        Mundus.postEvent(LogEvent(LogType.ERROR, "Skybox texture already exists. " + file.name()))
                        asset = projectManager.current().assetManager.findAssetByFileName(file.name())
                    }

                    textureAssets.add(asset.id)
                }

                // Create the skybox asset
                val skyboxAsset = projectManager.current().assetManager.createSkyBoxAsset(skyboxName.text, textureAssets[0], textureAssets[1],
                        textureAssets[2], textureAssets[3], textureAssets[4], textureAssets[5])

                projectManager.current().currScene.skyboxAssetId = skyboxAsset.id
                resetImages()
                refreshSelectBox()
                resetFields()
            }
        })

        // default skybox btn
        defaultBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                projectContext = projectManager.current()
                projectContext.currScene.skybox?.dispose()

                projectContext.currScene.skybox = createDefaultSkybox(Shaders.skyboxShader)
                val defaultSkybox = projectManager.getDefaultSkyboxAsset(projectContext, true)

                projectManager.current().currScene.skyboxAssetId = defaultSkybox.id
                refreshSelectBox()
                resetImages()
                resetFields()
            }
        })

        // delete skybox btn
        deletBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                projectContext = projectManager.current()
                projectContext.currScene.skybox?.dispose()

                projectContext.currScene.skybox = null
                projectContext.currScene.skyboxAssetId = null
                resetImages()
            }
        })

    }

    /**
     * Updates skybox assets based on fields and adds the asset to modified assets.
     */
    private fun addModifiedAsset() {
        val skyboxAsset = projectManager.current().assetManager.findAssetByID(projectManager.current().currScene.skyboxAssetId) as SkyboxAsset
        skyboxAsset.rotateEnabled = rotateEnabled.isChecked

        if (!rotateSpeed.isEmpty)
            skyboxAsset.rotateSpeed = rotateSpeed.text.toFloat()

        projectManager.current().assetManager.addModifiedAsset(skyboxAsset)
    }

    private fun resetFields() {
        rotateEnabled.isChecked = projectManager.current().currScene.skybox.isRotateEnabled
        rotateSpeed.text = projectManager.current().currScene.skybox.rotateSpeed.toString()
    }

    /**
     * Validate fields for Skybox creation. Checks the Name field
     * and image fields than disables/enables the create button accordingly.
     */
    private fun validateFields() {
        skyboxName.isInputValid = !skyboxName.isEmpty

        if (skyboxName.isEmpty || !imagesValid()) {
            createBtn.isDisabled = true
            return
        }

        createBtn.isDisabled = false
    }

    /**
     * Validates images in the Image Choosers. If any image is null
     * then it returns false.
     */
    private fun imagesValid(): Boolean {
        if (null === positiveX.file || null === negativeX.file ||
            null === positiveY.file || null === negativeY.file ||
            null === positiveZ.file || null === negativeZ.file) {

            return false
        }
        return true
    }

    override fun show(stage: Stage?): VisDialog {
        // Update select box on dialog show
        refreshSelectBox()
        resetFields()

        return super.show(stage)
    }

    private fun refreshSelectBox() {
        val skyboxes = projectManager.current().assetManager.getSkyboxAssets()
        val currentId = projectManager.current().currScene.skyboxAssetId

        // Refresh skyboxes
        selectBox.items.clear()
        selectBox.items = skyboxes
        selectBox.selected = projectManager.current().assetManager.findAssetByID(currentId) as SkyboxAsset?
    }

    private fun resetImages() {
        val skybox = projectManager.current().currScene.skybox
        if (skybox != null) {
            positiveX.setImage(skybox.positiveX)
            negativeX.setImage(skybox.negativeX)
            positiveY.setImage(skybox.positiveY)
            negativeY.setImage(skybox.negativeY)
            positiveZ.setImage(skybox.positiveZ)
            negativeZ.setImage(skybox.negativeZ)
        } else {
            positiveX.setImage(null)
            negativeX.setImage(null)
            positiveY.setImage(null)
            negativeY.setImage(null)
            positiveZ.setImage(null)
            negativeZ.setImage(null)

            // Cannot create if no images set
            createBtn.isDisabled = true
        }
    }

    override fun onProjectChanged(event: ProjectChangedEvent) {
        resetImages()
    }

    override fun onSceneChanged(event: SceneChangedEvent) {
        resetImages()
    }

    override fun onImageChosen() {
        validateFields()
    }

    override fun onImagesChosen(
        images: Array<FileHandle>?,
        failedFiles: HashMap<FileHandle, String>
    ) {
        // Unused
    }

}
