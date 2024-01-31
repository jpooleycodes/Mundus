package com.mbrlabs.mundus.editor.events

import com.mbrlabs.mundus.commons.scene3d.components.TerrainComponent

/**
 * An event to indicate that a terrain's LoD needs to be built/rebuilt.
 * @author JamesTKhan
 * @version October 04, 2023
 * @param terrainComponent the terrain component
 * @param immediate if true, the LoD will be built immediately, otherwise it will be scheduled
 */
class TerrainLoDRebuildEvent(val terrainComponent: TerrainComponent, val immediate: Boolean? = null) {
    interface TerrainLoDRebuildEventListener {
        @Subscribe
        fun onTerrainLoDRebuild(event: TerrainLoDRebuildEvent)
    }
}