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

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Mesh
import com.badlogic.gdx.graphics.VertexAttribute
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.model.MeshPart
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.Pool
import com.mbrlabs.mundus.commons.scene3d.components.TerrainComponent
import com.mbrlabs.mundus.commons.terrain.Terrain

abstract class HelperLineShape(val width: Int,
                               val counterOffsetX: Int,
                               val counterOffsetY: Int,
                               val terrainComponent: TerrainComponent) : Disposable {

    companion object {
        val helperLineCenterObjectPool = object : Pool<HelperLineCenterObject>() {
            override fun newObject(): HelperLineCenterObject = HelperLineCenterObject()
        }

        val tmpV3 = Vector3()
    }

    val mesh: Mesh
    val modelInstance: ModelInstance

    val centerOfHelperObjects: Array<HelperLineCenterObject>

    init {
        val attribs = VertexAttributes(
                VertexAttribute.Position(),
                VertexAttribute.Normal(),
                VertexAttribute(VertexAttributes.Usage.Tangent, 4, ShaderProgram.TANGENT_ATTRIBUTE),
                VertexAttribute.TexCoords(0)
        )

        val terrain = terrainComponent.terrainAsset.terrain

        val numVertices = terrain.vertexResolution * terrain.vertexResolution
        val numIndices = calculateIndicesNum(width, terrain)

        mesh = Mesh(true, numVertices, numIndices, attribs)

        val indices = buildIndices(width, numIndices, terrain)

        val material = Material(ColorAttribute.createDiffuse(Color.RED))

        mesh.setIndices(indices)
        mesh.setVertices(terrain.vertices)

        val meshPart = MeshPart(null, mesh, 0, numIndices, GL20.GL_LINES)
        meshPart.update()

        val mb = ModelBuilder()
        mb.begin()
        mb.part(meshPart, material)
        val model = mb.end()
        modelInstance = ModelInstance(model)
        modelInstance.transform = terrainComponent.modelInstance.transform

        centerOfHelperObjects = calculateCenterOfHelperObjects()
    }

    abstract fun calculateIndicesNum(width: Int, terrain: Terrain): Int

    abstract fun fillIndices(width: Int, indices: ShortArray, vertexResolution: Int)

    abstract fun calculateCenterOfHelperObjects(): Array<HelperLineCenterObject>

    fun updateVertices() {
        mesh.setVertices(terrainComponent.terrainAsset.terrain.vertices)
    }

    fun findNearestCenterObject(pos: Vector3): HelperLineCenterObject {
        var nearest = centerOfHelperObjects.first()
        var nearestDistance = pos.dst(nearest.position.x, 0f, nearest.position.z)

        for (helperLineCenterObject in centerOfHelperObjects) {
            val distance = pos.dst(helperLineCenterObject.position.x, 0f, helperLineCenterObject.position.z)

            if (distance < nearestDistance) {
                nearest = helperLineCenterObject
                nearestDistance = distance
            }
        }

        return nearest
    }

    private fun buildIndices(width: Int, numIndices: Int, terrain: Terrain): ShortArray {
        val indices = ShortArray(numIndices)
        val vertexResolution = terrain.vertexResolution

        fillIndices(width, indices, vertexResolution)

        return indices
    }

    override fun dispose() {
        while (centerOfHelperObjects.notEmpty()) {
            helperLineCenterObjectPool.free(centerOfHelperObjects.removeIndex(0))
        }

        modelInstance.model!!.dispose()
    }

    protected fun calculateRightTerrainChunksVertexResolution() : Int {
        var rightTerrainComponent = terrainComponent.rightNeighbor
        var allVertexResolution = 0

        while (rightTerrainComponent != null) {
            allVertexResolution += rightTerrainComponent.terrainAsset.terrain.vertexResolution - 1
            rightTerrainComponent = rightTerrainComponent.rightNeighbor
        }

        return allVertexResolution
    }

    protected fun calculateBottomTerrainChunksVertexResolution() : Int {
        var bottomTerrainComponent = terrainComponent.bottomNeighbor
        var allVertexResolution = 0

        while (bottomTerrainComponent != null) {
            allVertexResolution += bottomTerrainComponent.terrainAsset.terrain.vertexResolution - 1
            bottomTerrainComponent = bottomTerrainComponent.bottomNeighbor
        }

        return allVertexResolution
    }

}
