package com.mbrlabs.mundus.editor.ui.widgets

import com.mbrlabs.mundus.commons.scene3d.GameObject
import com.mbrlabs.mundus.commons.scene3d.components.Component
import com.mbrlabs.mundus.commons.scene3d.components.TerrainComponent
import com.mbrlabs.mundus.editor.Mundus
import com.mbrlabs.mundus.editor.history.commands.TerrainNeighborCommand
import com.mbrlabs.mundus.editor.ui.modules.dialogs.gameobjects.GameObjectFilter
import com.mbrlabs.mundus.editor.ui.modules.dialogs.gameobjects.GameObjectPickerDialog

/**
 * A widget to display and update terrain neighbors.
 * Neighbors must be children of the same parent GameObject.
 * @author JamesTKhan
 * @version July 03, 2023
 */
class TerrainNeighborWidget(val terrainComponent: TerrainComponent) : BaseWidget() {

    private val leftNeighborSelectionField = GameObjectSelectionField()
    private val rightNeighborSelectionField = GameObjectSelectionField()
    private val topNeighborSelectionField = GameObjectSelectionField()
    private val bottomNeighborSelectionField = GameObjectSelectionField()
    private var filter: GameObjectFilter? = null

    /**
     * Custom filter that ignores all GameObjects that are not terrains or are not children of the same parent GameObject.
     */
    class TerrainGameObjectFilter(var parentGo: GameObject, var currentGo: GameObject) : GameObjectFilter {
        override fun ignore(go: GameObject): Boolean {
            return go.findComponentByType<TerrainComponent>(Component.Type.TERRAIN) == null || !go.isChildOf(parentGo) || go == currentGo
        }
    }

    init {
        filter = TerrainGameObjectFilter(terrainComponent.gameObject.parent, terrainComponent.gameObject)
        add(ToolTipLabel("Neighbors: ", "Assigned neighbor terrains.\nNeighbors must all be children of the same parent GameObject."))
            .left().padBottom(5f).row()
        setNeighbors()
        addField(leftNeighborSelectionField, "Left")
        addField(rightNeighborSelectionField, "Right")
        addField(topNeighborSelectionField, "Top")
        addField(bottomNeighborSelectionField, "Bottom")
    }

    private fun addField(field: GameObjectSelectionField, neighborString: String) {
        field.gameObjectFilter = filter
        field.pickerListener = object : GameObjectPickerDialog.GameObjectPickerListener {
            override fun onSelected(go: GameObject?) {
                val newNeighbor = go?.findComponentByType(Component.Type.TERRAIN) as TerrainComponent?
                val command = TerrainNeighborCommand(terrainComponent, neighborString)
                command.setNewNeighbor(newNeighbor)
                command.setTextField(field.textField)

                Mundus.getCommandHistory().add(command)
                command.execute()

                field.setGameObject(go)
            }
        }

        add(
            ToolTipLabel(
                "$neighborString Neighbor",
                "The terrain to the ${neighborString.lowercase()} of this terrain."
            )
        ).left().row()
        add(field).growX().row()
    }

    private fun setNeighbors() {
        terrainComponent.leftNeighbor?.let { leftNeighborSelectionField.setGameObject(it.gameObject) }
        terrainComponent.rightNeighbor?.let { rightNeighborSelectionField.setGameObject(it.gameObject) }
        terrainComponent.topNeighbor?.let { topNeighborSelectionField.setGameObject(it.gameObject) }
        terrainComponent.bottomNeighbor?.let { bottomNeighborSelectionField.setGameObject(it.gameObject) }
    }

}