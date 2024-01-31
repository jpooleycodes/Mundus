package com.mbrlabs.mundus.editor.history.commands

import com.badlogic.gdx.utils.Array
import com.mbrlabs.mundus.commons.scene3d.components.TerrainComponent
import com.mbrlabs.mundus.editor.history.Command

/**
 * Wrapper for multiple TerrainHeightCommands since Terrain Stitching modifies multiple terrains.
 *
 * On construction, setHeightDataBefore is called immediately.
 * Implementations must call setHeightDataAfter after modifying terrains but before executing.
 *
 * @author JamesTKhan
 * @version June 26, 2023
 */
class TerrainStitchCommand(private var terrains: Array<TerrainComponent>) : Command {
    private var terrainHeightCommands = HashMap<TerrainComponent, TerrainHeightCommand>()

    init {
        for (terrain in terrains) {
            val command = TerrainHeightCommand(terrain)
            command.setHeightDataBefore(terrain.terrainAsset.terrain.heightData)
            terrainHeightCommands[terrain] = command
        }
    }

    fun setHeightDataAfter() {
        for (terrain in terrains) {
            val command = terrainHeightCommands[terrain]
            command!!.setHeightDataAfter(terrain.terrainAsset.terrain.heightData)
        }
    }

    override fun execute() {
        for (terrain in terrains) {
            val command = terrainHeightCommands[terrain]
            command!!.execute()
        }
    }

    override fun undo() {
        for (terrain in terrains) {
            val command = terrainHeightCommands[terrain]
            command!!.undo()
        }
    }

}