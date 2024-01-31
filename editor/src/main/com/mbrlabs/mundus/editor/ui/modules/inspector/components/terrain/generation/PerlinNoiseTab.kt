package com.mbrlabs.mundus.editor.ui.modules.inspector.components.terrain.generation

import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisTextButton
import com.kotcrab.vis.ui.widget.tabbedpane.Tab
import com.mbrlabs.mundus.commons.scene3d.components.TerrainComponent
import com.mbrlabs.mundus.editor.Mundus
import com.mbrlabs.mundus.editor.core.project.ProjectManager
import com.mbrlabs.mundus.editor.history.CommandHistory
import com.mbrlabs.mundus.editor.history.commands.TerrainHeightCommand
import com.mbrlabs.mundus.editor.terrain.Terraformer
import com.mbrlabs.mundus.editor.ui.widgets.FloatFieldWithLabel
import com.mbrlabs.mundus.editor.ui.widgets.IntegerFieldWithLabel

class PerlinNoiseTab(private val terrainComponent: TerrainComponent) : Tab(false, false) {

    private val root = VisTable()

    private val perlinNoiseBtn = VisTextButton("Generate Perlin noise")
    private val perlinNoiseSeed = IntegerFieldWithLabel("Seed", -1, false)
    private val perlinNoiseMinHeight = FloatFieldWithLabel("Min height", -1, true)
    private val perlinNoiseMaxHeight = FloatFieldWithLabel("Max height", -1, true)

    private val history: CommandHistory = Mundus.inject()
    private val projectManager: ProjectManager = Mundus.inject()

    init {
        root.align(Align.left)

        root.add(perlinNoiseSeed).pad(5f).left().fillX().expandX().row()
        root.add(perlinNoiseMinHeight).pad(5f).left().fillX().expandX().row()
        root.add(perlinNoiseMaxHeight).pad(5f).left().fillX().expandX().row()
        root.add(perlinNoiseBtn).pad(5f).right().row()

        setupListeners()
    }

    override fun getTabTitle(): String = "Perlin noise"

    override fun getContentTable(): Table = root

    private fun setupListeners() {
        perlinNoiseBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                val seed = perlinNoiseSeed.int
                val min = perlinNoiseMinHeight.float
                val max = perlinNoiseMaxHeight.float
                generatePerlinNoise(seed, min, max)
                projectManager.current().assetManager.addModifiedAsset(terrainComponent.terrainAsset)
            }
        })
    }

    private fun generatePerlinNoise(seed: Int, min: Float, max: Float) {
        val terrain = terrainComponent.terrainAsset.terrain
        val command = TerrainHeightCommand(terrainComponent)
        command.setHeightDataBefore(terrain.heightData)

        Terraformer.perlin(terrainComponent).minHeight(min).maxHeight(max).seed(seed.toLong()).terraform()

        command.setHeightDataAfter(terrain.heightData)
        history.add(command)
    }
}