package com.mbrlabs.mundus.editor.ui.modules.dialogs

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Array
import com.kotcrab.vis.ui.widget.VisCheckBox
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisSelectBox
import com.kotcrab.vis.ui.widget.VisTable
import com.mbrlabs.mundus.editor.Mundus
import com.mbrlabs.mundus.editor.events.UpdateNoiseTextureEvent
import com.mbrlabs.mundus.editor.terrain.noise.modifiers.NoiseModifier
import com.mbrlabs.mundus.editor.ui.widgets.ImprovedSlider
import com.mbrlabs.mundus.editor.ui.widgets.ToolTipLabel
import com.mbrlabs.mundus.editor.utils.FastNoiseLite

/**
 * @author JamesTKhan
 * @version November 02, 2022
 */
class NoiseModifierDialog(var modifier: NoiseModifier) : BaseDialog(modifier.name) {

    var noiseTypeSelect = VisSelectBox<FastNoiseLite.NoiseType>()
    var fractalTypeSelect = VisSelectBox<FastNoiseLite.FractalType>()
    var domainTypeSelect = VisSelectBox<FastNoiseLite.DomainWarpType>()
    val frequency = ImprovedSlider(0.000f, 0.1f, 0.001f, 3)
    val domainWarpFrequencySlider = ImprovedSlider(0.000f, 0.1f, 0.001f, 3)
    val domainWarpAmpsSlider = ImprovedSlider(0f, 60f, 2f, 3)
    val lacunaritySlider = ImprovedSlider(0f, 4f, 0.1f, 2)
    val gainSlider = ImprovedSlider(0f, 4f, 0.1f, 2)
    val additiveCheckBox = VisCheckBox("")

    init {
        setupUI()
    }

    private fun setupUI() {
        //add(VisLabel(modifier.name)).row()

        val root = VisTable()
        root.defaults().pad(5f).left()
        addNoiseSelect(root)
        addFractalSelect(root)
        addDomainSelect(root)

        val sliderTable = VisTable()
        sliderTable.defaults().pad(5f).left()
        root.add(sliderTable).colspan(2)

        sliderTable.add(VisLabel("Frequency: ")).left()
        sliderTable.add(frequency).row()
        frequency.value = modifier.frequency
        frequency.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                modifier.noiseGenerator.SetFrequency(frequency.value)
                modifier.frequency = frequency.value
                Mundus.postEvent(UpdateNoiseTextureEvent())
            }
        })

        sliderTable.add(VisLabel("Domain Warp Frequency: ")).left()
        sliderTable.add(domainWarpFrequencySlider).row()
        domainWarpFrequencySlider.value = modifier.domainWarpFrequency
        domainWarpFrequencySlider.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                modifier.domainWarpFrequency = domainWarpFrequencySlider.value
                Mundus.postEvent(UpdateNoiseTextureEvent())
            }
        })

        sliderTable.add(VisLabel("Domain Warp Amps: ")).left()
        sliderTable.add(domainWarpAmpsSlider).row()
        domainWarpAmpsSlider.value = 0f
        domainWarpAmpsSlider.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                modifier.domainWarpAmps = domainWarpAmpsSlider.value
                Mundus.postEvent(UpdateNoiseTextureEvent())
            }
        })

        sliderTable.add(VisLabel("Lacunarity: ")).left()
        sliderTable.add(lacunaritySlider).row()
        lacunaritySlider.value = modifier.fractalLacunarity
        lacunaritySlider.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                modifier.fractalLacunarity = lacunaritySlider.value
                Mundus.postEvent(UpdateNoiseTextureEvent())
            }
        })

        sliderTable.add(VisLabel("Gain: ")).left()
        sliderTable.add(gainSlider).row()
        gainSlider.value = modifier.fractalGain
        gainSlider.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                modifier.fractalGain = gainSlider.value
                Mundus.postEvent(UpdateNoiseTextureEvent())
            }
        })

        sliderTable.add(ToolTipLabel("Additive Noise?", "When checked, the noise modifier will be added to the current heightmap instead of multiplied by.").left())
        sliderTable.add(additiveCheckBox)
        additiveCheckBox.isChecked = modifier.noiseAdditive;
        additiveCheckBox.addListener( object : ClickListener(){
            override fun touchUp(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int) {
                super.touchUp(event, x, y, pointer, button)
                modifier.noiseAdditive  = !modifier.noiseAdditive
                Mundus.postEvent(UpdateNoiseTextureEvent())
            }
        })

        add(root).expand().fill()
    }

    private fun addDomainSelect(selectTable: VisTable) {
        val noiseTypes = Array<FastNoiseLite.DomainWarpType>()
        for (type in FastNoiseLite.DomainWarpType.values())
            noiseTypes.add(type)
        domainTypeSelect.items = noiseTypes
        domainTypeSelect.selected = modifier.domainType

        selectTable.add(VisLabel("Domain Type: "))
        selectTable.add(domainTypeSelect).row()
        domainTypeSelect.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                modifier.domainType = domainTypeSelect.selected
                Mundus.postEvent(UpdateNoiseTextureEvent())
            }
        })
    }

    private fun addFractalSelect(selectTable: VisTable) {
        val noiseTypes = Array<FastNoiseLite.FractalType>()
        for (type in FastNoiseLite.FractalType.values())
            noiseTypes.add(type)
        fractalTypeSelect.items = noiseTypes
        fractalTypeSelect.selected = modifier.fractalType

        selectTable.add(VisLabel("Fractal Type: "))
        selectTable.add(fractalTypeSelect).row()
        fractalTypeSelect.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                modifier.fractalType = fractalTypeSelect.selected
                Mundus.postEvent(UpdateNoiseTextureEvent())
            }
        })
    }

    private fun addNoiseSelect(selectTable: VisTable) {
        val noiseTypes = Array<FastNoiseLite.NoiseType>()
        for (type in FastNoiseLite.NoiseType.values())
            noiseTypes.add(type)
        noiseTypeSelect.items = noiseTypes
        noiseTypeSelect.selected = modifier.type

        selectTable.add(VisLabel("Noise Type: "))
        selectTable.add(noiseTypeSelect).row()
        noiseTypeSelect.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                modifier.type = noiseTypeSelect.selected
                Mundus.postEvent(UpdateNoiseTextureEvent())
            }
        })
    }
}