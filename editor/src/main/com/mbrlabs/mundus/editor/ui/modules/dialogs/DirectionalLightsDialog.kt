package com.mbrlabs.mundus.editor.ui.modules.dialogs

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.kotcrab.vis.ui.util.FloatDigitsOnlyFilter
import com.kotcrab.vis.ui.widget.VisCheckBox
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisTextButton
import com.kotcrab.vis.ui.widget.VisTextField
import com.kotcrab.vis.ui.widget.color.ColorPickerAdapter
import com.mbrlabs.mundus.commons.utils.LightUtils
import com.mbrlabs.mundus.editor.Mundus
import com.mbrlabs.mundus.editor.core.project.ProjectManager
import com.mbrlabs.mundus.editor.events.LogEvent
import com.mbrlabs.mundus.editor.events.LogType
import com.mbrlabs.mundus.editor.ui.UI
import com.mbrlabs.mundus.editor.ui.widgets.ColorPickerField
import com.mbrlabs.mundus.editor.ui.widgets.ImprovedSlider
import com.mbrlabs.mundus.editor.ui.widgets.ToolTipLabel
import com.mbrlabs.mundus.editorcommons.events.ProjectChangedEvent
import com.mbrlabs.mundus.editorcommons.events.SceneChangedEvent

/**
 * @author JamesTKhan
 * *
 * @version 25-05-2022
 */
class  DirectionalLightsDialog : BaseDialog("Directional Light"), ProjectChangedEvent.ProjectChangedListener,
        SceneChangedEvent.SceneChangedListener {

    private val root = VisTable()

    private val intensity = VisTextField("0")
    private val shadowSettings = VisTextButton("Shadow Settings")
    private val colorPickerField = ColorPickerField()
    private val castShadows = VisCheckBox(null)

    private val dirXSlider = ImprovedSlider(-1.0f, 1.0f, .1f)
    private val dirYSlider = ImprovedSlider(-1.0f, 1.0f, .1f)
    private val dirZSlider = ImprovedSlider(-1.0f, 1.0f, .1f)

    private var defaultBtn = VisTextButton("Reset Defaults")

    private val projectManager: ProjectManager = Mundus.inject()

    init {
        Mundus.registerEventListener(this)

        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        root.padTop(6f).padRight(6f).padBottom(22f)
        root.defaults().padBottom(5f).padTop(5f)
        add(root)

        root.add(VisLabel("Light Settings")).colspan(2).left().row()
        root.addSeparator().colspan(2).row()

        // Settings
        val settingsTable = VisTable()
        settingsTable.defaults().padBottom(5f).padLeft(6f).padRight(6f)
        settingsTable.add(VisLabel("Intensity")).left().padBottom(10f)
        settingsTable.add(intensity).left().padBottom(10f).row()
        settingsTable.add(VisLabel("Color")).growX()
        settingsTable.add(colorPickerField).left().fillX().expandX().colspan(2).row()

        settingsTable.add(VisLabel("Direction")).left()

        // Light direction sliders
        val directionTable = VisTable()
        directionTable.defaults().padBottom(5f).padLeft(6f).padRight(6f)
        directionTable.add(VisLabel("X:")).left().padRight(2f)
        directionTable.add(dirXSlider).row()
        directionTable.add(VisLabel("Y:")).left().padRight(2f)
        directionTable.add(dirYSlider).row()
        directionTable.add(VisLabel("Z:")).left().padRight(2f)
        directionTable.add(dirZSlider).row()
        settingsTable.add(directionTable).colspan(2).left().growX().row()

        root.add(settingsTable).row()

        addShadowSection()

        // Options
        root.add(VisLabel("Options")).colspan(2).left().padTop(10f).row()
        root.addSeparator().colspan(2).row()

        val tab = VisTable()
        tab.defaults().padTop(15f).padLeft(6f).padRight(6f).padBottom(15f)
        tab.add(defaultBtn).expandX().fillX()
        root.add(tab).fillX().expandX().row()

        resetValues()
    }

    private fun addShadowSection() {
        val shadowLabel = ToolTipLabel("Shadows", "Experimental for now." +
                " Only a single pass shadow map with limited range.")
        root.add(shadowLabel).colspan(2).left().row()
        root.addSeparator().colspan(2).row()

        // Settings
        val shadowSettingsTable = VisTable()
        val enableLabel = ToolTipLabel("Cast Dynamic Shadows: ", "Enables shadow mapping.")
        shadowSettingsTable.defaults().padBottom(5f).padLeft(6f).padRight(6f)
        shadowSettingsTable.add(enableLabel).left().padBottom(10f)
        shadowSettingsTable.add(castShadows).left().padBottom(10f).left().row()

        shadowSettingsTable.add(shadowSettings).left().padBottom(10f).row()

        root.add(shadowSettingsTable).left().row()
    }

    private fun setupListeners() {

        // intensity
        intensity.textFieldFilter = FloatDigitsOnlyFilter(false)
        intensity.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                if (intensity.isInputValid && !intensity.isEmpty) {
                    val d = convert(intensity.text) ?: return
                    try {
                        val light = LightUtils.getDirectionalLight(projectManager.current().currScene.environment)
                        light?.intensity = d
                        light?.updateColor()
                        projectManager.current().currScene.initPBR()
                    } catch (ex : NumberFormatException) {
                        Mundus.postEvent(LogEvent(LogType.ERROR,"Error parsing field " + intensity.name))
                    }
                }
            }
        })

        // color
        colorPickerField.colorAdapter = object: ColorPickerAdapter() {
            override fun finished(newColor: Color) {
                val light = LightUtils.getDirectionalLight(projectManager.current().currScene.environment)
                light?.baseColor?.set(newColor)
                light?.updateColor()
                projectManager.current().currScene.initPBR()
            }

            override fun changed(newColor: Color?) {
                val light = LightUtils.getDirectionalLight(projectManager.current().currScene.environment)
                light?.baseColor?.set(newColor)
                light?.updateColor()
                projectManager.current().currScene.initPBR()
            }

            override fun canceled(oldColor: Color?) {
                val light = LightUtils.getDirectionalLight(projectManager.current().currScene.environment)
                light?.baseColor?.set(oldColor)
                light?.updateColor()
                projectManager.current().currScene.initPBR()
            }
        }

        dirXSlider.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                LightUtils.getDirectionalLight(projectManager.current().currScene.environment)?.direction?.x = dirXSlider.value
                projectManager.current().currScene.initPBR()
            }
        })

        dirYSlider.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                LightUtils.getDirectionalLight(projectManager.current().currScene.environment)?.direction?.y = dirYSlider.value
                projectManager.current().currScene.initPBR()
            }
        })

        dirZSlider.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                LightUtils.getDirectionalLight(projectManager.current().currScene.environment).direction?.z = dirZSlider.value
                projectManager.current().currScene.initPBR()
            }
        })

        // Shadows
        castShadows.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                val light = LightUtils.getDirectionalLight(projectManager.current().currScene.environment)
                light.isCastsShadows = castShadows.isChecked
                shadowSettings.isDisabled = !castShadows.isChecked
            }
        })

        shadowSettings.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                if (shadowSettings.isDisabled) return
                UI.showDialog(UI.shadowSettingsDialog)
            }
        })

        defaultBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                val light = LightUtils.getDirectionalLight(projectManager.current().currScene.environment)
                light?.color?.set(Color.WHITE)
                light?.intensity = 1f
                light?.direction?.set(.5f, -.5f, -.7f)

                light?.isCastsShadows = false
                projectManager.current().currScene.initPBR()
                resetValues()
            }
        })

    }

    private fun resetValues() {
        val light = LightUtils.getDirectionalLight(projectManager.current().currScene.environment)

        intensity.text = light!!.intensity.toString()
        colorPickerField.selectedColor = light.color
        castShadows.isChecked = light.isCastsShadows
        shadowSettings.isDisabled = !light.isCastsShadows

        dirXSlider.value = light.direction!!.x
        dirYSlider.value = light.direction!!.y
        dirZSlider.value = light.direction!!.z
    }

    private fun convert(input: String): Float? {
        try {
            if (input.isEmpty()) return null
            return java.lang.Float.valueOf(input)
        } catch (e: Exception) {
            return null
        }
    }

    override fun onProjectChanged(event: ProjectChangedEvent) {
        resetValues()
    }

    override fun onSceneChanged(event: SceneChangedEvent) {
        resetValues()
    }

}