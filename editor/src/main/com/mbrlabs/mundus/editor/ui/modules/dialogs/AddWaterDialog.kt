package com.mbrlabs.mundus.editor.ui.modules.dialogs

import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.kotcrab.vis.ui.util.dialog.Dialogs
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisTextButton
import com.kotcrab.vis.ui.widget.VisTextField
import com.mbrlabs.mundus.commons.assets.WaterAsset
import com.mbrlabs.mundus.commons.scene3d.GameObject
import com.mbrlabs.mundus.commons.water.Water
import com.mbrlabs.mundus.editor.Mundus
import com.mbrlabs.mundus.editor.core.io.IOManager
import com.mbrlabs.mundus.editor.core.io.IOManagerProvider
import com.mbrlabs.mundus.editor.core.project.ProjectManager
import com.mbrlabs.mundus.editor.events.AssetImportEvent
import com.mbrlabs.mundus.editor.events.SceneGraphChangedEvent
import com.mbrlabs.mundus.editor.ui.UI
import com.mbrlabs.mundus.editor.ui.widgets.FloatFieldWithLabel
import com.mbrlabs.mundus.editor.ui.widgets.IntegerFieldWithLabel
import com.mbrlabs.mundus.editor.ui.widgets.ToolTipLabel
import com.mbrlabs.mundus.editor.utils.Log
import com.mbrlabs.mundus.editor.utils.createWaterGO
import com.mbrlabs.mundus.editorcommons.exceptions.AssetAlreadyExistsException

class AddWaterDialog : BaseDialog("Add Water") {

    companion object {
        private val TAG = AddWaterDialog::class.java.simpleName
    }

    private val name = VisTextField("Water")
    private val waterWidth = IntegerFieldWithLabel("", -1, false)
    private val positionX = FloatFieldWithLabel("", -1, true)
    private val positionY = FloatFieldWithLabel("", -1, true)
    private val positionZ = FloatFieldWithLabel("", -1, true)

    private val generateBtn = VisTextButton("Generate Water")

    private var projectManager : ProjectManager
    private var ioManager : IOManager

    private var selectedGO : GameObject? = null

    init {
        isResizable = true

        projectManager = Mundus.inject()
        ioManager = Mundus.inject<IOManagerProvider>().ioManager

        setupUI()
        setDefaults()
        setupListeners()
    }

    fun show(selectedGO: GameObject?) {
        this.selectedGO = selectedGO
        UI.showDialog(this)
    }

    private fun setupUI() {
        val root = Table()
        // root.debugAll();
        root.padTop(6f).padRight(6f).padBottom(22f)
        add(root)

        // left table
        val content = VisTable()
        content.defaults().pad(4f)
        content.left().top()
        content.add(VisLabel("Name: ")).left().padBottom(10f)
        content.add(name).fillX().expandX().row()
        content.add(ToolTipLabel("Water width: ", "Size of the water, in meters.")).left().padBottom(10f)
        content.add(waterWidth).fillX().expandX().row()
        content.add(VisLabel("Position on x-axis:")).left().padBottom(10f)
        content.add(positionX).fillX().expandX().row()
        content.add(VisLabel("Position on y-axis:")).left().padBottom(10f)
        content.add(positionY).fillX().expandX().row()
        content.add(VisLabel("Position on z-axis: ")).left().padBottom(10f)
        content.add(positionZ).fillX().expandX().row()

        content.add(generateBtn).fillX().expand().colspan(2).bottom()
        root.add(content)
    }

    private fun setDefaults() {
        waterWidth.text = Water.DEFAULT_SIZE.toString()
        positionX.text = "0"
        positionY.text = "0"
        positionZ.text = "0"
    }

    private fun setupListeners() {
        generateBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent, x: Float, y: Float) {
                super.clicked(event, x, y)
                createWater()
            }
        })
    }

    private fun createWater() {
        try {
            val waterName: String = name.text
            val width: Int = waterWidth.int
            val posX: Float = positionX.float
            val posY: Float = positionY.float
            val posZ: Float = positionZ.float

            try {
                Log.trace(TAG, "Add water game object in root node.")
                val context = projectManager.current()
                val sceneGraph = context.currScene.sceneGraph
                val goID = projectManager.current().obtainID()

                // Save context here so that the ID above is persisted in .pro file
                ioManager.saveProjectContext(projectManager.current())

                val asset: WaterAsset
                try {
                    // create asset
                    asset = context.assetManager.createWaterAsset(waterName, width)
                } catch (ex: AssetAlreadyExistsException) {
                    Dialogs.showErrorDialog(stage, "An asset with that name already exists.")
                    return
                }
                asset.load()
                asset.applyDependencies()

                val waterGO = createWaterGO(sceneGraph,
                        null, goID, waterName, asset)
                // update sceneGraph
                if (selectedGO == null) {
                    sceneGraph.addGameObject(waterGO)
                } else {
                    sceneGraph.addGameObject(selectedGO, waterGO)
                }
                waterGO.setLocalPosition(posX, posY, posZ)

                Mundus.postEvent(SceneGraphChangedEvent())

                projectManager.current().assetManager.addNewAsset(asset)
                Mundus.postEvent(AssetImportEvent(asset))
                Mundus.postEvent(SceneGraphChangedEvent())

                close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } catch (nfe: NumberFormatException) {
            Log.error(TAG, nfe.message)
        }
    }
}