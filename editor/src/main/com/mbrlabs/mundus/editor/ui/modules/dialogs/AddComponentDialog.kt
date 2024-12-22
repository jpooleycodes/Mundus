package com.mbrlabs.mundus.editor.ui.modules.dialogs

import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Array
import com.kotcrab.vis.ui.util.dialog.Dialogs
import com.kotcrab.vis.ui.widget.VisDialog
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisSelectBox
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisTextButton
import com.mbrlabs.mundus.commons.env.lights.LightType
import com.mbrlabs.mundus.commons.scene3d.GameObject
import com.mbrlabs.mundus.commons.scene3d.InvalidComponentException
import com.mbrlabs.mundus.commons.scene3d.components.Component
import com.mbrlabs.mundus.commons.scene3d.components.CustomPropertiesComponent
import com.mbrlabs.mundus.commons.utils.LightUtils
import com.mbrlabs.mundus.editor.Mundus
import com.mbrlabs.mundus.editor.core.plugin.PluginManagerProvider
import com.mbrlabs.mundus.editor.core.project.ProjectManager
import com.mbrlabs.mundus.editor.events.ComponentAddedEvent
import com.mbrlabs.mundus.editor.events.LogEvent
import com.mbrlabs.mundus.editor.events.LogType
import com.mbrlabs.mundus.editor.scene3d.components.PickableLightComponent
import com.mbrlabs.mundus.editor.ui.UI
import com.mbrlabs.mundus.pluginapi.ComponentExtension

class AddComponentDialog : BaseDialog("Add Component") {

    private abstract inner class DropdownComponent(val name: String) {

        abstract fun createComponent(gameObject: GameObject): Component?

        override fun toString(): String  = name
    }

    private lateinit var root: VisTable
    private lateinit var selectBox: VisSelectBox<DropdownComponent>
    private var addBtn = VisTextButton("Add Component")

    private val projectManager: ProjectManager = Mundus.inject()
    private val pluginManager = Mundus.inject<PluginManagerProvider>().pluginManager

    init {
        setupUI()
        setupListeners()
    }

    override fun show(stage: Stage?): VisDialog {
        // Load types into select box
        selectBox.items.clear()

        val addableTypes = Array<DropdownComponent>()
        addableTypes.add(object : DropdownComponent("Light"){
            override fun createComponent(gameObject: GameObject): Component? = getNewLightComponent(gameObject)
        })
        addableTypes.add(object : DropdownComponent("Custom properties"){
            override fun createComponent(gameObject: GameObject): Component? = getNewCustomPropertiesComponent(gameObject)
        })
        pluginManager.getExtensions(ComponentExtension::class.java).forEach {
            try {
                val supportedComponentTypes = it.supportedComponentTypes

                if (supportedComponentTypes == null || containsSupportedComponentType(supportedComponentTypes)) {
                    addableTypes.add(object : DropdownComponent(it.componentName) {
                        override fun createComponent(gameObject: GameObject): Component? =
                            it.createComponent(gameObject)
                    })
                }
            } catch (ex: Exception) {
                Mundus.postEvent(LogEvent(LogType.ERROR, "Exception during create component! $ex"))
            }
        }
        selectBox.items = addableTypes

        return super.show(stage)
    }

    private fun setupUI() {
        root = VisTable()
        root.defaults().pad(6f)

        // Component selector
        val selectorsTable = VisTable(true)
        selectorsTable.add(VisLabel("Component Type:"))
        selectBox = VisSelectBox<DropdownComponent>()
        selectorsTable.add(selectBox).left()
        root.add(selectorsTable).row()

        root.add(addBtn).left().growX()

        add(root)
    }

    private fun getSelectedGameObject(): GameObject {
        return UI.outline.getSelectedGameObject()!!
    }

    private fun containsSupportedComponentType(supportedComponentTypes: Array<Component.Type>): Boolean {
        val gameObject = getSelectedGameObject()

        for (supportedComponentType in supportedComponentTypes) {
            if (gameObject.findComponentByType<Component>(supportedComponentType) != null) {
                return true
            }
        }

        return false
    }

    private fun setupListeners() {
        addBtn.addListener(object : ClickListener () {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                // Add component to current game object
                val go = getSelectedGameObject()

                val component = selectBox.selected.createComponent(go)
                if (component != null) {
                    try {
                        go.addComponent(component)
                        Mundus.postEvent(ComponentAddedEvent(component))
                    } catch (ex: InvalidComponentException) {
                        Dialogs.showErrorDialog(stage, "Cannot add a duplicate component.", ex)
                    }

                    close()
                }
            }
        })
    }

    private fun getNewLightComponent(go: GameObject): Component? {
        val env = projectManager.current().currScene.environment

        // Create a point or spotlight based on maximum lights allowed
        if (LightUtils.canCreateLight(env, LightType.POINT_LIGHT)) {
            return PickableLightComponent(go, LightType.POINT_LIGHT)
        } else if (LightUtils.canCreateLight(env, LightType.SPOT_LIGHT)) {
            Dialogs.showOKDialog(UI, "Info", "Max point lights reached ("+LightUtils.MAX_POINT_LIGHTS+"), switching to spotlight.")
            return PickableLightComponent(go, LightType.SPOT_LIGHT)
        } else {
            val str = buildString {
                append("Max lighting reached, cannot add additional point or spot lights.\n")
                append("\nPoint Lights: " + LightUtils.MAX_POINT_LIGHTS)
                append("\nSpot Lights: " + LightUtils.MAX_SPOT_LIGHTS)
            }
            Dialogs.showOKDialog(UI, "Info", str)
            return null
        }
    }

    private fun getNewCustomPropertiesComponent(go: GameObject): Component? {
        return CustomPropertiesComponent(go)
    }

}