package com.mbrlabs.mundus.editor.ui.modules.dialogs.terrain

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.kotcrab.vis.ui.util.dialog.Dialogs
import com.kotcrab.vis.ui.widget.VisCheckBox
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisSelectBox
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisTextButton
import com.kotcrab.vis.ui.widget.VisTextField
import com.kotcrab.vis.ui.widget.tabbedpane.Tab
import com.mbrlabs.mundus.commons.scene3d.components.TerrainComponent
import com.mbrlabs.mundus.commons.terrain.SplatMapResolution
import com.mbrlabs.mundus.editor.terrain.HeightMapGenerator
import com.mbrlabs.mundus.editor.terrain.Terraformer
import com.mbrlabs.mundus.editor.ui.UI
import com.mbrlabs.mundus.editor.ui.modules.dialogs.AddTerrainChunksDialog
import com.mbrlabs.mundus.editor.ui.widgets.FileChooserField
import com.mbrlabs.mundus.editor.ui.widgets.FloatField
import com.mbrlabs.mundus.editor.ui.widgets.ImprovedSlider
import com.mbrlabs.mundus.editor.ui.widgets.IntegerField
import com.mbrlabs.mundus.editor.ui.widgets.ToolTipLabel
import com.mbrlabs.mundus.editor.utils.isImage


/**
 * @author JamesTKhan
 * @version July 03, 2023
 */
class HeightMapTerrainTab(var dialog: AddTerrainChunksDialog) : Tab(false, false) {
    private val root = VisTable()

    private val hmInput = FileChooserField()
    private val loadHeightMapBtn = VisTextButton("Load heightmap")

    private val smoothingSlider: ImprovedSlider = ImprovedSlider(0f, 1f, 0.05f, 2)
    private val smoothingPasses: IntegerField = IntegerField(false)
    private val applySmoothing: VisCheckBox = VisCheckBox("Apply smoothing")
    private val flipHeightMap: VisCheckBox = VisCheckBox("Flip heightmap")
    private val genLoD: VisCheckBox = VisCheckBox("Generate Levels of Detail")

    private val name = VisTextField("Terrain")
    private val maxHeightField = FloatField(true)
    private val minHeightField = FloatField(true)
    private val terrainWidth = IntegerField(false)
    private val splatMapSelectBox: VisSelectBox<String> = VisSelectBox()

    private lateinit var heightMapData: FloatArray
    private var imageWidth: Int = 0
    private var imageHeight: Int = 0

    private val description = """
            Generate terrain using a heightmap image.
            
            Terrain height range is from 0 to the maximum height.
            Maximum height must be a positive value.
            Maximum height must be greater than 0.
            
        """.trimIndent()

    init {
        root.align(Align.center)
        root.add(VisLabel(description)).pad(5f).left().fillX().row()

        // set defaults
        smoothingSlider.value = 1f
        smoothingPasses.text = "1"
        applySmoothing.isChecked = true
        flipHeightMap.isChecked = true
        genLoD.isChecked = true
        minHeightField.text = "0"
        maxHeightField.text = "300"
        terrainWidth.text = "1200"

        val leftTable = VisTable()
        leftTable.defaults().pad(5f).top()

        leftTable.add(genLoD).left().row()
        leftTable.add(flipHeightMap).left()
        leftTable.add(applySmoothing).left().row()

        leftTable.add(ToolTipLabel("Smoothing Passes: ", "The amount of smoothing passes to perform.")).left()
        leftTable.add(smoothingPasses).left().row()

        leftTable.add(ToolTipLabel("Smoothing Strength: ", "The strength of the smoothing pass.")).left()
        leftTable.add(smoothingSlider).left().row()

        val centerTable = VisTable()
        centerTable.defaults().pad(5f).top()

        centerTable.add(VisLabel("Name: ")).left()
        centerTable.add(name).row()

        centerTable.add(VisLabel("Width: ")).left()
        centerTable.add(terrainWidth).left().row()

        centerTable.add(VisLabel("Min Height: ")).left()
        centerTable.add(minHeightField).left().row()

        centerTable.add(VisLabel("Max Height: ")).left()
        centerTable.add(maxHeightField).left().row()

        val rightTable = VisTable()
        rightTable.defaults().pad(5f).top()

        splatMapSelectBox.setItems(
                SplatMapResolution._512.value,
                SplatMapResolution._1024.value,
                SplatMapResolution._2048.value,
        )

        rightTable.add(ToolTipLabel("SplatMap Resolution: ", "The resolution of the splatmap for texture painting on the terrain.\n" +
                "Higher resolution results in smoother texture painting at the cost of more memory usage and performance slowdowns when painting.\n" +
                "If you are targeting HTML, 512 is recommended")).left()
        rightTable.add(splatMapSelectBox).left().row()

        val container = VisTable()
        container.add(leftTable).left().top()
        container.add(centerTable).left().top()
        container.add(rightTable).left().top()

        root.add(container).left().row()

        root.add(VisLabel("\n\nSelect heightmap image:")).pad(5f).left().fillX().row()

        val chooserTable = VisTable()
        chooserTable.add(hmInput).pad(5f).left().expandX().fillX().row()
        chooserTable.add(loadHeightMapBtn).pad(5f).right().row()
        root.add(chooserTable).left().fill().expand().row()

        loadHeightMapBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                val hm = hmInput.file
                val max = maxHeightField.float

                if (max == 0f) maxHeightField.text = "100" // if max height left blank or zero, set to 100
                if (max < 0f) maxHeightField.text =
                    "" + -max // if max is negative, then set to max to positive value

                if (hm != null && hm.exists() && isImage(hm)) {
                    loadHeightMap(hm)
                } else {
                    Dialogs.showErrorDialog(UI, "Please select a heightmap image")
                }
            }
        })
    }

    private fun loadHeightMap(heightMap: FileHandle) {
        val originalMap = Pixmap(heightMap)
        if (originalMap.width % 2 != 0) {
            Dialogs.showErrorDialog(UI, "HeightMap dimensions must be divisible by two!")
            return
        }

        imageWidth = originalMap.width
        imageHeight = originalMap.height

        try {
            heightMapData = HeightMapGenerator.heightColorsToMap(
                originalMap.getPixels(), originalMap.getFormat(), imageWidth,
                imageHeight
            )
        } catch (e: Exception) {
            Dialogs.showErrorDialog(UI, "Failed to load heightmap. " + e.message)
            return
        }

        if (flipHeightMap.isChecked) {
            // Bottom left corner of the image is 0,0 on terrain
            heightMapData = flipHeightMap(heightMapData, imageWidth, imageHeight)
        }

        if (applySmoothing.isChecked) {
            val strength = smoothingSlider.value
            for (i in 0 until smoothingPasses.int) {
                heightMapData = HeightMapGenerator.smoothHeightmap(heightMapData, imageWidth, imageHeight, strength)
            }
        }

        // Calculate the number of chunks such that each chunk's resolution is <= 180
        val maxResolution = 180
        var divisor = 1
        while (originalMap.width / divisor > maxResolution + 1) {
            divisor++
        }

        val width = terrainWidth.int
        // Number of chunks along one axis
        val chunks = divisor

        // Now, calculate the vertex resolution
        val resolution = originalMap.width / chunks - 1
        // Create terrain
        dialog.createTerrainChunk(resolution, width, true, chunks, chunks, name.text, SplatMapResolution.valueFromString(splatMapSelectBox.selected).resolutionValues, genLoD.isChecked)

        originalMap.dispose()
    }

    override fun getTabTitle(): String {
        return "Heightmap"
    }

    override fun getContentTable(): Table {
        return root
    }

    fun terraform(xOffset: Int, yOffset: Int, terrain: TerrainComponent) {
        Terraformer
            .heightMap(terrain)
            .maxHeight(maxHeightField.float)
            .minHeight(minHeightField.float)
            .map(heightMapData)
            .imageWidth(imageWidth)
            .imageHeight(imageHeight)
            .offset(yOffset, xOffset)
            .terraform()
    }

    /**
     * Flips the heightmap data
     */
    private fun flipHeightMap(heightMapData: FloatArray, width: Int, height: Int): FloatArray {
        val flippedHeightMapData = FloatArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val srcIndex = y * width + x
                val destIndex = (height - y - 1) * width + x
                flippedHeightMapData[destIndex] = heightMapData[srcIndex]
            }
        }
        return flippedHeightMapData
    }

    fun getMinHeightValue() : Float = minHeightField.float
    fun getMaxHeightValue() : Float = maxHeightField.float

}