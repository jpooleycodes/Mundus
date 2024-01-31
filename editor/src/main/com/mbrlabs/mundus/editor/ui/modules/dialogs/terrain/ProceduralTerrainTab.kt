package com.mbrlabs.mundus.editor.ui.modules.dialogs.terrain

import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Array
import com.kotcrab.vis.ui.widget.tabbedpane.Tab
import com.mbrlabs.mundus.commons.scene3d.components.TerrainComponent
import com.mbrlabs.mundus.commons.scene3d.components.TerrainManagerComponent
import com.mbrlabs.mundus.editor.ui.modules.dialogs.AddTerrainChunksDialog
import com.mbrlabs.mundus.editor.ui.widgets.ProceduralGenerationWidget

/**
 * @author JamesTKhan
 * @version July 03, 2023
 */
class ProceduralTerrainTab(var dialog: AddTerrainChunksDialog) : Tab(false, false) {

    private val root = ProceduralGenerationWidget(true, true, true,  true, true)

    init {
        root.generateButtonListener = object : ProceduralGenerationWidget.GenerateButtonListener {
            override fun generate() {
                generateTerrain()
            }
        }
    }

    override fun onShow() {
        super.onShow()
        root.init()
    }

    fun terraform(xOffset: Int, yOffset: Int, terrain: TerrainComponent) {
        root.terraform(xOffset, yOffset, terrain)
    }

    fun getMinHeightValue() : Float = root.getMinHeightValue()
    fun getMaxHeightValue() : Float = root.getMaxHeightValue()

    fun uploadNoiseModifiers(noiseModifierList: Array<TerrainManagerComponent.ProceduralGeneration.ProceduralNoiseModifier>) = root.uploadNoiseModifiers(noiseModifierList)

    override fun getTabTitle(): String {
        return "Procedural Terrain"
    }

    override fun getContentTable(): Table {
        return root
    }

    private fun generateTerrain() {
        dialog.createTerrainChunk(root.getVertexResolution(), root.getTerrainWidth(), root.isMultipleTerrain(), root.getGridX(), root.getGridZ(), root.getTerrainName(), root.getSplatMapResolution(), root.isGenerateLoD())
    }
}