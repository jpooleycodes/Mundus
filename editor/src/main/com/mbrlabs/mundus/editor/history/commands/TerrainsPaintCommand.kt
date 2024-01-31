package com.mbrlabs.mundus.editor.history.commands

import com.mbrlabs.mundus.commons.terrain.SplatMap
import com.mbrlabs.mundus.commons.terrain.Terrain
import com.mbrlabs.mundus.editor.history.DisposableCommand

/**
 * A wrapper for modifying the splat map paint of multiple terrains.
 * @author JamesTKhan
 * @version June 30, 2023
 */
class TerrainsPaintCommand : DisposableCommand {
    private var terrainPaintCommands = HashMap<Terrain, TerrainPaintCommand>()

    /**
     * Add a terrain to be modified and set the height data before.
     */
    fun addTerrain(terrain: Terrain) {
        val sm: SplatMap = terrain.terrainTexture.splatmap
        val command = TerrainPaintCommand(terrain)
        command.setBefore(sm.pixmap)
        terrainPaintCommands[terrain] = command
    }

    fun setAfter() {
        for (terrain in terrainPaintCommands.keys) {
            val command = terrainPaintCommands[terrain]
            command!!.setAfter(terrain.terrainTexture.splatmap.pixmap)
        }
    }

    override fun execute() {
        for (terrain in terrainPaintCommands.keys) {
            val command = terrainPaintCommands[terrain]
            command!!.execute()
        }
    }

    override fun undo() {
        for (terrain in terrainPaintCommands.keys) {
            val command = terrainPaintCommands[terrain]
            command!!.undo()
        }
    }

    override fun dispose() {
        for (terrain in terrainPaintCommands.keys) {
            val command = terrainPaintCommands[terrain]
            command!!.dispose()
        }
        terrainPaintCommands.clear()
    }
}