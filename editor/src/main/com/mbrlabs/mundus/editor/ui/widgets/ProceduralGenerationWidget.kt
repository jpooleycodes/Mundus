package com.mbrlabs.mundus.editor.ui.widgets

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Array
import com.kotcrab.vis.ui.widget.VisCheckBox
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisSelectBox
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisTextButton
import com.kotcrab.vis.ui.widget.VisTextField
import com.mbrlabs.mundus.commons.scene3d.components.TerrainComponent
import com.mbrlabs.mundus.commons.scene3d.components.TerrainManagerComponent.ProceduralGeneration
import com.mbrlabs.mundus.commons.terrain.SplatMapResolution
import com.mbrlabs.mundus.commons.terrain.Terrain
import com.mbrlabs.mundus.editor.Mundus
import com.mbrlabs.mundus.editor.events.UpdateNoiseTextureEvent
import com.mbrlabs.mundus.editor.terrain.noise.modifiers.ElevationModifier
import com.mbrlabs.mundus.editor.terrain.noise.modifiers.NoiseModifier
import com.mbrlabs.mundus.editor.terrain.noise.modifiers.TerrainModifier
import com.mbrlabs.mundus.editor.ui.UI
import com.mbrlabs.mundus.editor.ui.modules.dialogs.NoiseModifierDialog
import com.mbrlabs.mundus.editor.utils.FastNoiseLite


class ProceduralGenerationWidget(private val nameFieldVisible: Boolean,
                                 private val vertexResolutionFieldVisible: Boolean,
                                 private val terrainWidthFieldVisible: Boolean,
                                 private val splatmapResolutionFieldVisible: Boolean,
                                 private val iterationFieldsVisible: Boolean) : Table() {

    private val root = VisTable()

    private val name = VisTextField("Terrain")
    private val vertexResolution = IntegerFieldWithLabel("", -1, false)
    private val terrainWidth = IntegerFieldWithLabel("", -1, false)
    private val minHeight = FloatFieldWithLabel("", -1, true)
    private val maxHeight = FloatFieldWithLabel("", -1, true)
    private val splatMapSelectBox: VisSelectBox<String> = VisSelectBox()
    private val genLoD = VisCheckBox("")
    private val multipleTerrain = VisCheckBox("")
    private val gridFieldsTable = VisTable()
    private val gridX = IntegerFieldWithLabel("", -1, false)
    private val gridZ = IntegerFieldWithLabel("", -1, false)
    private val generateBtn = VisTextButton("Generate Terrain")

    private val noiseGeneratorWidget : NoiseGeneratorWidget = NoiseGeneratorWidget()

    private val modifierTable = VisTable()

    var generateButtonListener: GenerateButtonListener? = null

    init {
        setupUI()
        setupListeners()
    }

    fun init() {
        vertexResolution.text = 64.toString()
        terrainWidth.text = Terrain.DEFAULT_SIZE.toString()

        minHeight.text = (-50f).toString()
        maxHeight.text = 50f.toString()

        gridX.text = "2"
        gridZ.text = "2"

        noiseGeneratorWidget.generator.modifiers.clear()
        noiseGeneratorWidget.generator.modifiers.add(ElevationModifier())
        buildModifierTable()
        Mundus.postEvent(UpdateNoiseTextureEvent())
    }

    fun init(proceduralGeneration: ProceduralGeneration) {
        init()

        minHeight.text = proceduralGeneration.minHeight.toString()
        maxHeight.text = proceduralGeneration.maxHeight.toString()

        if (proceduralGeneration.noiseModifiers.notEmpty()) {
            noiseGeneratorWidget.generator.modifiers.clear()
            for (elevation in proceduralGeneration.noiseModifiers) {
                val modifier = ElevationModifier()
                modifier.type = FastNoiseLite.NoiseType.valueOf(elevation.noiseType)
                modifier.fractalType = FastNoiseLite.FractalType.valueOf(elevation.fractalType)
                modifier.domainType = FastNoiseLite.DomainWarpType.valueOf(elevation.domainType)
                modifier.frequency = elevation.frequency
                modifier.domainWarpFrequency = elevation.domainWarpFrequency
                modifier.domainWarpAmps = elevation.domainWarpAmps
                modifier.noiseGenerator.SetFractalLacunarity(elevation.fractalLacunarity)
                modifier.fractalGain = elevation.fractalGain
                modifier.fractalLacunarity = elevation.fractalLacunarity
                modifier.noiseAdditive = elevation.additive
                noiseGeneratorWidget.generator.modifiers.add(modifier)
            }
        } else {
            noiseGeneratorWidget.generator.modifiers.add(ElevationModifier())
        }

        buildModifierTable()
        Mundus.postEvent(UpdateNoiseTextureEvent())
    }

    fun terraform(xOffset: Int, yOffset: Int, terrain: TerrainComponent) {
        noiseGeneratorWidget.generator.offset(xOffset, yOffset).setTerrain(terrain.terrainAsset.terrain).terraform()
    }

    fun getTerrainName(): String = name.text
    fun getVertexResolution(): Int = vertexResolution.int
    fun getTerrainWidth(): Int = terrainWidth.int
    fun getMinHeightValue(): Float = minHeight.float
    fun getMaxHeightValue(): Float = maxHeight.float
    fun getSplatMapResolution(): Int = SplatMapResolution.valueFromString(splatMapSelectBox.selected).resolutionValues
    fun isGenerateLoD(): Boolean = genLoD.isChecked
    fun isMultipleTerrain(): Boolean = multipleTerrain.isChecked
    fun getGridX(): Int = gridX.int
    fun getGridZ(): Int = gridZ.int

    fun uploadNoiseModifiers(noiseModifierList: Array<ProceduralGeneration.ProceduralNoiseModifier>) {
        val modifiers = noiseGeneratorWidget.generator.modifiers

        for (modifier in modifiers) {
            if (modifier is NoiseModifier) {
                val proceduralNoiseModifier = ProceduralGeneration.ProceduralNoiseModifier()
                proceduralNoiseModifier.noiseType = modifier.type.name
                proceduralNoiseModifier.fractalType = modifier.fractalType.name
                proceduralNoiseModifier.domainType = modifier.domainType.name
                proceduralNoiseModifier.frequency = modifier.frequency
                proceduralNoiseModifier.domainWarpFrequency = modifier.domainWarpFrequency
                proceduralNoiseModifier.domainWarpAmps = modifier.domainWarpAmps
                proceduralNoiseModifier.fractalLacunarity = modifier.fractalLacunarity
                proceduralNoiseModifier.fractalGain = modifier.fractalGain
                proceduralNoiseModifier.additive = modifier.noiseAdditive

                noiseModifierList.add(proceduralNoiseModifier)
            }
        }
    }

    private fun setupUI() {
        // root table
        root.padTop(6f).padRight(6f).padBottom(22f)
        root.setFillParent(true)
        add(root)

        // left table
        val leftTable = VisTable()
        leftTable.defaults().pad(4f)
        leftTable.left().top()

        root.add(VisLabel("Terrain Settings")).row()

        if (nameFieldVisible) {
            leftTable.add(VisLabel("Name: ")).left().padBottom(10f)
            leftTable.add(name).fillX().expandX().row()
        }

        if (vertexResolutionFieldVisible) {
            leftTable.add(
                    ToolTipLabel("Vertex resolution: ", "This will determine the vertices count when squared. 180 = 32,400 vertices. \n" +
                            "The default value (or lower) is recommended for performance. \nSettings this over 180 may cause issues on some devices.")
            ).left()
            leftTable.add(vertexResolution).fillX().expandX().row()
        }

        if (terrainWidthFieldVisible) {
            leftTable.add(ToolTipLabel("Terrain width: ", "Size of the terrain, in meters.")).left()
            leftTable.add(terrainWidth).fillX().expandX().row()
        }

        leftTable.add(ToolTipLabel("Min height", "The minimum height any point on the generated terrain will have. Can be negative")).left()
        leftTable.add(minHeight).left().row()
        leftTable.add(ToolTipLabel("Max height", "The maximum height any point on the generated terrain will have.")).left()
        leftTable.add(maxHeight).left().row()

        if (splatmapResolutionFieldVisible) {
            splatMapSelectBox.setItems(
                    SplatMapResolution._512.value,
                    SplatMapResolution._1024.value,
                    SplatMapResolution._2048.value,
            )

            leftTable.add(ToolTipLabel("SplatMap Resolution: ", "The resolution of the splatmap for texture painting on the terrain.\n" +
                    "Higher resolution results in smoother texture painting at the cost of more memory usage and performance slowdowns when painting.\n" +
                    "If you are targeting HTML, 512 is recommended")).left()
            leftTable.add(splatMapSelectBox).left().row()
        }

        if (iterationFieldsVisible) {
            // Only show if we are generating multiple terrains and it's the first time we are generating
            leftTable.add(ToolTipLabel("Generate Level Of Detail", "Generate lower level detail meshes for better rendering performance.")).left()
            leftTable.add(genLoD).left().row()

            leftTable.add(ToolTipLabel("Multiple Terrain", "Multiple terrain means terrain system that contains more then 1 terrains under a parent game object.")).left()
            leftTable.add(multipleTerrain).left().row()
            gridFieldsTable.defaults().pad(4f)
            gridFieldsTable.left().top()
            gridFieldsTable.isVisible = false

            gridFieldsTable.add(ToolTipLabel("X Iterations", "The number of Terrain Chunks to create on the X axis")).left()
            gridFieldsTable.add(gridX).left().row()
            gridFieldsTable.add(ToolTipLabel("Z Iterations", "The number of Terrain Chunks to create on the Z axis")).left()
            gridFieldsTable.add(gridZ).left().row()
            leftTable.add(gridFieldsTable).left().row()
        }

        leftTable.add(generateBtn).expandY().fillX().bottom()

        root.add(leftTable).top().fillX().expandX().fillY().expandY()
        root.addSeparator(true)

        // Center table
        root.add(noiseGeneratorWidget).pad(4f).fillX().expandX()

        // Right table
        buildModifierTable()

        root.addSeparator(true)
        root.add(modifierTable).top()
    }

    private fun setupListeners() {
        generateBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent, x: Float, y: Float) {
                super.clicked(event, x, y)
                noiseGeneratorWidget.generator
                        .minHeight(minHeight.float)
                        .maxHeight(maxHeight.float)

                generateButtonListener?.generate()
            }
        })

        vertexResolution.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                noiseGeneratorWidget.setNoiseTextureWidth(vertexResolution.int)
            }
        })

        multipleTerrain.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                gridFieldsTable.isVisible = multipleTerrain.isChecked
            }
        })
    }

    private fun buildModifierTable() {
        modifierTable.clear()

        val addModifierBtn = VisTextButton("Add Modifier")

        modifierTable.defaults().pad(4f)
        modifierTable.left().top()
        modifierTable.add(addModifierBtn).left().row()
        modifierTable.addSeparator().row()

        addModifierBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                val mod = ElevationModifier()
                addModifierToList(mod)
                noiseGeneratorWidget.generator.modifiers.add(mod)
            }
        })

        for (mod in noiseGeneratorWidget.generator.modifiers) {
            addModifierToList(mod)
        }
    }

    private fun addModifierToList(mod: TerrainModifier) {
        val button = VisTextButton(mod.name)
        button.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                if (mod is NoiseModifier) {
                    val dialog = NoiseModifierDialog(mod)
                    dialog.show(UI)
                }
                super.clicked(event, x, y)
            }
        })
        modifierTable.add(button).left().row()
    }

    interface GenerateButtonListener {
        fun generate()
    }

}
