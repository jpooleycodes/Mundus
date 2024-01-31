package com.mbrlabs.mundus.editor.ui.modules.inspector.components.terrain

import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Array
import com.kotcrab.vis.ui.util.dialog.Dialogs
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisTextButton
import com.mbrlabs.mundus.commons.assets.Asset
import com.mbrlabs.mundus.commons.assets.TerrainLayerAsset
import com.mbrlabs.mundus.commons.scene3d.GameObject
import com.mbrlabs.mundus.commons.scene3d.components.Component
import com.mbrlabs.mundus.commons.scene3d.components.TerrainComponent
import com.mbrlabs.mundus.commons.scene3d.components.TerrainManagerComponent
import com.mbrlabs.mundus.editor.LevelOfDetailScheduler
import com.mbrlabs.mundus.editor.Mundus
import com.mbrlabs.mundus.editor.assets.AssetTerrainLayerFilter
import com.mbrlabs.mundus.editor.core.project.ProjectManager
import com.mbrlabs.mundus.editor.ui.UI
import com.mbrlabs.mundus.editor.ui.modules.dialogs.assets.AssetPickerDialog
import com.mbrlabs.mundus.editor.ui.modules.inspector.components.ComponentWidget

/**
 * @author JamesTKhan
 * @version July 17, 2023
 */
class TerrainManagerComponentWidget(terrainManagerComponent: TerrainManagerComponent) :
    ComponentWidget<TerrainManagerComponent>("Terrain Manager Component", terrainManagerComponent) {

    private val filter: AssetTerrainLayerFilter = AssetTerrainLayerFilter()
    private lateinit var assetPickerListener: AssetPickerDialog.AssetPickerListener

    var root = VisTable()
    private var projectManager : ProjectManager = Mundus.inject()
    private var lodScheduler : LevelOfDetailScheduler = Mundus.inject()

    private val updateBtn: VisTextButton = VisTextButton("Change Layers")
    private val triplanarOnBtn: VisTextButton = VisTextButton("Triplanar Toggle On")
    private val triplanarOffBtn: VisTextButton = VisTextButton("Triplanar Toggle Off")
    private val generationBtn: VisTextButton = VisTextButton("Generation")
    private val levelOfDetailBtn: VisTextButton = VisTextButton("Level of Detail")

    init {
        setupUI()
        setupListeners()
    }

    override fun setValues(go: GameObject) {
        val c : TerrainManagerComponent = go.findComponentByType(Component.Type.TERRAIN_MANAGER) ?: return
        component = c
    }

    private fun setupUI() {
        root.defaults()

        val label = VisLabel()
        label.wrap = true
        label.setText("This component applies changes to all child terrains.")
        collapsibleContent.add(label).grow().padBottom(10f).row()

        val buttonTable = VisTable()
        buttonTable.defaults().fill().pad(5f)
        buttonTable.add(updateBtn).row()
        buttonTable.add(triplanarOnBtn).row()
        buttonTable.add(triplanarOffBtn).row()
        buttonTable.add(generationBtn).row()
        buttonTable.add(levelOfDetailBtn).row()
        root.add(buttonTable).left().row()

        collapsibleContent.add(root).left().growX().row()
    }

    private fun setupListeners() {
        assetPickerListener = object : AssetPickerDialog.AssetPickerListener {
            override fun onSelected(asset: Asset?) {
                val layer = (asset as? TerrainLayerAsset)!!

                Dialogs.showConfirmDialog(
                    UI,
                    "Change Layer",
                    "If the selected layer has less texture channels assigned than existing terrains, Splat map paint data for the exceeding channels will be lost." +
                            "\nDo you want to continue?",
                    arrayOf("Cancel", "Yes"),
                    arrayOf(0, 1)
                ) { r: Int ->
                    if (r == 1) {
                        setTerrainLayerAsset(layer)
                    }
                }.padBottom(20f).pack()
            }
        }

        updateBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                UI.assetSelectionDialog.show(false, filter, assetPickerListener)
            }
        })

        triplanarOnBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                setTriplanar(true)
            }
        })

        triplanarOffBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                setTriplanar(false)
            }
        })

        generationBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                UI.showDialog(UI.terrainSystemGenerationDialog)
            }
        })

        levelOfDetailBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                UI.levelOfDetailDialog.setTerrainComponentManager(component)
                UI.showDialog(UI.levelOfDetailDialog)
            }
        })
    }

    private fun setTriplanar(value: Boolean) {
        val modifiedTerrains = Array<TerrainComponent>()
        component.setTriplanar(value, modifiedTerrains)
        for (terrain in modifiedTerrains) {
            projectManager.current().assetManager.addModifiedAsset(terrain.terrainAsset)
        }
    }

    private fun setTerrainLayerAsset(layer: TerrainLayerAsset) {
        val modifiedTerrains = Array<TerrainComponent>()
        component.setTerrainLayerAsset(layer, modifiedTerrains)

        // If the switched layer uses splat textures, we need to create splatmaps for all terrains missing them
        val createSplatMaps = layer.activeLayerCount > 0
        for (terrain in modifiedTerrains) {
            projectManager.current().assetManager.addModifiedAsset(terrain.terrainAsset)

            if (createSplatMaps) {
                projectManager.current().assetManager.createSplatmapForTerrain(terrain)
            }
        }
    }

}