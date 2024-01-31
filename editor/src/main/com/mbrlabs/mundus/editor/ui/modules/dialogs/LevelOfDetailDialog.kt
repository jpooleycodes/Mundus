package com.mbrlabs.mundus.editor.ui.modules.dialogs

import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Array
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisTextButton
import com.mbrlabs.mundus.commons.scene3d.components.Component
import com.mbrlabs.mundus.commons.scene3d.components.TerrainComponent
import com.mbrlabs.mundus.commons.scene3d.components.TerrainManagerComponent
import com.mbrlabs.mundus.editor.LevelOfDetailScheduler
import com.mbrlabs.mundus.editor.Mundus
import com.mbrlabs.mundus.editor.core.project.ProjectManager
import com.mbrlabs.mundus.editor.events.TerrainLoDRebuildEvent
import com.mbrlabs.mundus.editor.ui.UI
import com.mbrlabs.mundus.editor.utils.Scene2DUtils

/**
 * @author JamesTKhan
 * @version October 06, 2023
 */
class LevelOfDetailDialog : BaseDialog("Level of Detail") {
    private val generateLoDBtn = VisTextButton("Generate")
    private val disableLoDBtn = VisTextButton("Disable")
    private var manager: TerrainManagerComponent? = null

    private var projectManager : ProjectManager = Mundus.inject()
    private var lodScheduler : LevelOfDetailScheduler = Mundus.inject()
    private val lodSchedulerListener = object : LevelOfDetailScheduler.LodSchedulerListener {
        override fun onTerrainLoDRebuild(state: LevelOfDetailScheduler.State) {
            Scene2DUtils.setButtonState(generateLoDBtn, state == LevelOfDetailScheduler.State.COMPLETE)
        }
    }

    init {
        setupUI()
        lodScheduler.addListener(lodSchedulerListener)
    }

    private fun setupUI() {
        val width = UI.width * 0.3f

        val descriptionOne = VisLabel()
        descriptionOne.setText("Level of Detail (LoD) is a technique that reduces the number of triangles in a mesh to improve" +
                " performance. This is done by generating a series of meshes with decreasing triangle counts. The " +
                "meshes are then swapped out depending on the distance from the camera.")
        descriptionOne.wrap = true

        val descriptionTwo = VisLabel()
        descriptionTwo.setText("The Generate button will build LoD meshes for all terrain components under the manager. If they already " +
                "have LoD meshes, they will be rebuilt. LoD's are rebuilt automatically as needed so manual regeneration should not be needed." +
                " The Disable button will remove all LoD meshes from the terrain components. After updating LoD" +
                " meshes, you will need to save the project to keep the changes.")
        descriptionTwo.wrap = true

        contentTable.add(descriptionOne).expandX().prefWidth(width).padBottom(10f).row()
        contentTable.add(descriptionTwo).expandX().prefWidth(width).padBottom(10f).row()

        disableLoDBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                disableLods()
            }
        })

        generateLoDBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                buildLoDs()
            }
        })

        contentTable.add(generateLoDBtn).pad(5f).growX().row()
        contentTable.add(disableLoDBtn).pad(5f).growX().row()
    }

    private fun disableLods() {
        if (manager == null) return

        val components = Array<Component>()
        manager!!.gameObject.findComponentsByType(components, Component.Type.TERRAIN, true)

        for (i in 0 until components.size) {
            val c = components[i] as TerrainComponent
            c.lodManager.disable()
            c.terrainAsset.lodLevels = null
            projectManager.current().assetManager.addModifiedAsset(c.terrainAsset)
        }
    }

    private fun buildLoDs() {
        if (manager == null) return

        Scene2DUtils.setButtonState(generateLoDBtn, false)
        val components = Array<Component>()
        manager!!.gameObject.findComponentsByType(components, Component.Type.TERRAIN, true)

        for (i in 0 until components.size) {
            val c = components[i]
            val immediate = i == components.size - 1
            Mundus.postEvent(TerrainLoDRebuildEvent(c as TerrainComponent, immediate))
            projectManager.current().assetManager.addModifiedAsset(c.terrainAsset)
        }
    }

    fun setTerrainComponentManager(manager: TerrainManagerComponent) {
        this.manager = manager
    }
}