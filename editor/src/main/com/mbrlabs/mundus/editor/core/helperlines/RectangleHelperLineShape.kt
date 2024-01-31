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

import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Array
import com.mbrlabs.mundus.commons.scene3d.components.TerrainComponent
import com.mbrlabs.mundus.commons.terrain.Terrain

class RectangleHelperLineShape(width: Int,
                               counterOffsetX: Int,
                               counterOffsetY: Int,
                               terrainComponent: TerrainComponent) : HelperLineShape(width, counterOffsetX, counterOffsetY, terrainComponent) {

    override fun calculateIndicesNum(width: Int, terrain: Terrain): Int {
        var i = 0
        calculate(width, terrain.vertexResolution) { ++i }
        return i
    }

    override fun fillIndices(width: Int, indices: ShortArray, vertexResolution: Int) {
        var i = 0
        calculate(width, vertexResolution) { pos -> indices[i++] = pos }
    }

    private fun calculate(width: Int, vertexResolution: Int, method: (pos: Short) -> Unit) {
        val yInit = 0 - (calculateBottomTerrainChunksVertexResolution() % width)
        val xInit = 0 - (calculateRightTerrainChunksVertexResolution() % width)

        for (y in yInit until vertexResolution step width) {
            if (y >= 0) {
                for (x in 0 until vertexResolution - 1) {
                    val current = y * vertexResolution + x
                    val next = current + 1

                    method.invoke(current.toShort())
                    method.invoke(next.toShort())
                }
            }
        }
        for (y in xInit until vertexResolution step width) {
            if (y >= 0) {
                for (x in 0 until vertexResolution - 1) {
                    val current = y + vertexResolution * x
                    val next = current + vertexResolution

                    method.invoke(current.toShort())
                    method.invoke(next.toShort())
                }
            }
        }
    }

    override fun calculateCenterOfHelperObjects(): Array<HelperLineCenterObject> {
        val centerOfHelperObjects = Array<HelperLineCenterObject>()
        val terrain = terrainComponent.terrainAsset.terrain
        val vertexResolution = terrain.vertexResolution

        val widthOffset = terrain.terrainWidth.toFloat() / (vertexResolution - 1).toFloat()
        val depthOffset = terrain.terrainDepth.toFloat() / (vertexResolution - 1).toFloat()

        var terrainY = 0f
        var y = 0
        while (terrainY + 1 < terrain.terrainDepth) {
            var terrainX = 0f
            var x = 0

            while (terrainX + 1 < terrain.terrainWidth) {
                val fullCell = terrainX + width * widthOffset <= terrain.terrainWidth && terrainY + width * depthOffset <= terrain.terrainDepth
                val pos = Vector3(terrainX + width * widthOffset / 2, 0f, terrainY + width * depthOffset / 2)

                // Convert to world position
                pos.mul(terrainComponent.modelInstance.transform)

                val helperLineCenterObject = helperLineCenterObjectPool.obtain().initialize(x + counterOffsetX, y + counterOffsetY, pos, fullCell)
                centerOfHelperObjects.add(helperLineCenterObject)

                ++x
                terrainX += width * widthOffset
            }

            ++y
            terrainY += width * depthOffset
        }

        return centerOfHelperObjects
    }
}