/*
 * Copyright (c) 2023. See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mbrlabs.mundus.editor.core.helperlines

import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.math.Vector3
import com.mbrlabs.mundus.commons.scene3d.components.TerrainComponent
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Disposable
import com.mbrlabs.mundus.commons.scene3d.components.Component
import com.mbrlabs.mundus.editor.events.GameObjectModifiedEvent
import com.mbrlabs.mundus.editor.events.TerrainAddedEvent
import com.mbrlabs.mundus.editor.events.TerrainNewNeighborEvent
import com.mbrlabs.mundus.editor.events.TerrainRemovedEvent
import com.mbrlabs.mundus.editor.events.TerrainVerticesChangedEvent

class HelperLines : TerrainVerticesChangedEvent.TerrainVerticesChangedEventListener,
        TerrainAddedEvent.TerrainAddedEventListener,
        TerrainRemovedEvent.TerrainRemovedEventListener,
        TerrainNewNeighborEvent.TerrainNewNeighborEventListener,
        GameObjectModifiedEvent.GameObjectModifiedListener,
        Disposable {

    private val helperLineShapes = Array<HelperLineShape>()
    private var width = -1
    private var type: HelperLineType? = null
    private var counterOffsetX = 0
    private var counterOffsetY = 0

    fun build(type: HelperLineType, width: Int, counterOffsetX: Int, counterOffsetY: Int, terrainComponents: Array<TerrainComponent>) {
        this.type = type
        this.width = width
        this.counterOffsetX = counterOffsetX
        this.counterOffsetY = counterOffsetY

        for (terrainComponent in terrainComponents) {
            addNewHelperLineShape(terrainComponent)
        }
    }

    fun render(batch: ModelBatch) {
        for (helperLineObject in helperLineShapes) {
            batch.render(helperLineObject.modelInstance)
        }
    }

    fun hasHelperLines() = helperLineShapes.notEmpty()

    fun findHelperLineCenterObject(terrainComponent: TerrainComponent, pos: Vector3): HelperLineCenterObject? {
        for (helperLineShape in helperLineShapes) {
            if (helperLineShape.terrainComponent === terrainComponent) {
                return helperLineShape.findNearestCenterObject(pos)
            }
        }

        return null
    }

    override fun onTerrainVerticesChanged(event: TerrainVerticesChangedEvent) {
        helperLineShapes.filter { it.terrainComponent == event.terrainComponent }.forEach { it.updateVertices() }
    }

    override fun onTerrainAdded(event: TerrainAddedEvent) {
        if (type != null) {
            addNewHelperLineShape(event.terrainComponent)
        }
    }

    override fun onTerrainRemoved(event: TerrainRemovedEvent) {
        removeHelperLine(event.terrainComponent)
    }

    override fun onNewTerrainNeighbor(event: TerrainNewNeighborEvent) {
        val terrainComponent = event.terrainComponent
        removeHelperLine(terrainComponent)
        addNewHelperLineShape(terrainComponent)
    }

    override fun onGameObjectModified(event: GameObjectModifiedEvent) {
        val go = event.gameObject ?: return
        val terrainComponent: TerrainComponent = (go.findComponentByType(Component.Type.TERRAIN)?: return)

        if (go.active) {
            if (type != null && helperLineShapes.none { it.terrainComponent == terrainComponent }) {
                addNewHelperLineShape(terrainComponent)
            }
        } else {
            helperLineShapes.filter { it.terrainComponent == terrainComponent }.forEach {
                it.dispose()
                helperLineShapes.removeValue(it, true)
            }
        }
    }

    override fun dispose() {
        helperLineShapes.forEach { helperLineObject -> helperLineObject.dispose() }
        helperLineShapes.clear()
        type = null
    }

    private fun removeHelperLine(terrainComponent: TerrainComponent) {
        helperLineShapes.filter { it.terrainComponent == terrainComponent }.forEach {
            it.dispose()
            helperLineShapes.removeValue(it, true)
        }
    }

    private fun addNewHelperLineShape(terrainComponent: TerrainComponent) {
        val helperLineShape : HelperLineShape
        if (type == HelperLineType.RECTANGLE) {
            helperLineShape = RectangleHelperLineShape(width, counterOffsetX, counterOffsetY, terrainComponent)
        } else {
            helperLineShape = HexagonHelperLineShape(width, counterOffsetX, counterOffsetY, terrainComponent)
        }
        helperLineShapes.add(helperLineShape)
    }

}
