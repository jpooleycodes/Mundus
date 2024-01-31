package com.mbrlabs.mundus.editor.history.commands

import com.kotcrab.vis.ui.widget.VisTextField
import com.mbrlabs.mundus.commons.scene3d.components.TerrainComponent
import com.mbrlabs.mundus.editor.Mundus
import com.mbrlabs.mundus.editor.events.LogEvent
import com.mbrlabs.mundus.editor.events.TerrainNewNeighborEvent
import com.mbrlabs.mundus.editor.history.Command

/**
 * This command is used to undo and redo changes to the neighbors of a terrain component.
 * @author JamesTKhan
 * @version July 03, 2023
 */
class TerrainNeighborCommand(var terrainComponent: TerrainComponent, var neighborString: String) : Command {
    private var previousNeighbor: TerrainComponent? = null
    private var newNeighbor: TerrainComponent? = null
    private var textField: VisTextField? = null

    init {
        previousNeighbor =
            when (neighborString) {
                "Left" -> terrainComponent.leftNeighbor
                "Right" -> terrainComponent.rightNeighbor
                "Top" -> terrainComponent.topNeighbor
                "Bottom" -> terrainComponent.bottomNeighbor
                else -> null
            }
    }

    override fun execute() {
        setNeighbor(newNeighbor)
        Mundus.postEvent(LogEvent("$neighborString Terrain neighbor of ${terrainComponent.gameObject?.name} changed to ${newNeighbor?.gameObject?.name}"))
        Mundus.postEvent(TerrainNewNeighborEvent(terrainComponent))
    }

    override fun undo() {
        setNeighbor(previousNeighbor)
        Mundus.postEvent(LogEvent("$neighborString Terrain neighbor of ${terrainComponent.gameObject?.name} undone back to ${previousNeighbor?.gameObject?.name}"))
        Mundus.postEvent(TerrainNewNeighborEvent(terrainComponent))
    }

    private fun setNeighbor(neighbor: TerrainComponent?) {
        if (neighborString == "Left") terrainComponent.leftNeighbor = neighbor
        if (neighborString == "Right") terrainComponent.rightNeighbor = neighbor
        if (neighborString == "Top") terrainComponent.topNeighbor = neighbor
        if (neighborString == "Bottom") terrainComponent.bottomNeighbor = neighbor
        textField?.text = neighbor?.gameObject?.name ?: "None"
    }

    fun setNewNeighbor(newNeighbor: TerrainComponent?) {
        this.newNeighbor = newNeighbor
    }

    fun setTextField(textField: VisTextField?) {
        this.textField = textField
    }

}