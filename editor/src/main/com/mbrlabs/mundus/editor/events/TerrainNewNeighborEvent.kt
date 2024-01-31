package com.mbrlabs.mundus.editor.events

import com.mbrlabs.mundus.commons.scene3d.components.TerrainComponent

class TerrainNewNeighborEvent(val terrainComponent: TerrainComponent) {

    interface TerrainNewNeighborEventListener {
        @Subscribe
        fun onNewTerrainNeighbor(event: TerrainNewNeighborEvent)
    }
}
