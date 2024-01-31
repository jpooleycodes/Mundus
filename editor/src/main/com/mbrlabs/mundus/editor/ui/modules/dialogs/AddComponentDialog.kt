package com.mbrlabs.mundus.editor.ui.modules.dialogs

import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Array
import com.kotcrab.vis.ui.util.dialog.Dialogs
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
import com.mbrlabs.mundus.editor.core.project.ProjectManager
import com.mbrlabs.mundus.editor.events.ComponentAddedEvent
import com.mbrlabs.mundus.editor.scene3d.components.PickableLightComponent
import com.mbrlabs.mundus.editor.ui.UI

class AddComponentDialog : BaseDialog("Add Component") {

    private enum class ComponentType(val type: Component.Type, val label: String) {
        LIGHT(Component.Type.LIGHT, "Light"),
        CUSTOM_PROPERTIES(Component.Type.CUSTOM_PROPERTIES, "Custom properties");

        override fun toString(): String = label
    }

    private lateinit var root: VisTable
    private lateinit var selectBox: VisSelectBox<ComponentType>
    private var addBtn = VisTextButton("Add Component")

    private val projectManager: ProjectManager = Mundus.inject()

    init {
        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        root = VisTable()
        root.defaults().pad(6f)

        // Component selector
        val selectorsTable = VisTable(true)
        selectorsTable.add(VisLabel("Component Type:"))
        selectBox = VisSelectBox<ComponentType>()
        selectorsTable.add(selectBox).left()
        root.add(selectorsTable).row()

        root.add(addBtn).left().growX()

        // Load types into select box
        val addableTypes = Array<ComponentType>()

        // At the moment, only light and custom properties components are supported for dynamically adding
        addableTypes.add(ComponentType.LIGHT)
        addableTypes.add(ComponentType.CUSTOM_PROPERTIES)

        selectBox.items = addableTypes

        add(root)
    }

    private fun setupListeners() {
        addBtn.addListener(object : ClickListener () {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                // Add component to current game object
                val go = UI.outline.getSelectedGameObject()!!

                val component = getNewComponent(selectBox.selected.type, go)
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

    /**
     * Retrieve a new component for the given type.
     */
    private fun getNewComponent(type: Component.Type, go: GameObject): Component? {
        when(type) {
            Component.Type.MODEL -> TODO()
            Component.Type.TERRAIN -> TODO()
            Component.Type.LIGHT -> return getNewLightComponent(go)
            Component.Type.PARTICLE_SYSTEM -> TODO()
            Component.Type.WATER -> TODO()
            Component.Type.CUSTOM_PROPERTIES -> return getNewCustomPropertiesComponent(go)
            Component.Type.TERRAIN_MANAGER -> TODO()
        }
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

    private fun getNewCustomPropertiesComponent(go: GameObject): Component {
        return CustomPropertiesComponent(go)
    }

}