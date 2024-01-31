/*
 * Copyright (c) 2016. See AUTHORS file.
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

package com.mbrlabs.mundus.editor.history.commands

import com.mbrlabs.mundus.commons.scene3d.components.TerrainComponent
import com.mbrlabs.mundus.editor.Mundus
import com.mbrlabs.mundus.editor.events.TerrainVerticesChangedEvent
import com.mbrlabs.mundus.editor.history.Command

/**
 * @author Marcus Brummer
 * @version 07-02-2016
 */
class TerrainHeightCommand(private var terrain: TerrainComponent?) : Command {

    private var heightDataBefore: FloatArray? = null
    private var heightDataAfter: FloatArray? = null

    fun setHeightDataBefore(data: FloatArray) {
        heightDataBefore = FloatArray(data.size)
        System.arraycopy(data, 0, heightDataBefore!!, 0, data.size)
    }

    fun setHeightDataAfter(data: FloatArray) {
        heightDataAfter = FloatArray(data.size)
        System.arraycopy(data, 0, heightDataAfter!!, 0, data.size)
    }

    override fun execute() {
        copyHeightData(heightDataAfter!!)
        terrain!!.terrainAsset.terrain.update()
        terrain!!.lodManager.disable()
        Mundus.postEvent(TerrainVerticesChangedEvent(terrain!!))
    }

    override fun undo() {
        copyHeightData(heightDataBefore!!)
        terrain!!.terrainAsset.terrain.update()
        terrain!!.lodManager.disable()
        Mundus.postEvent(TerrainVerticesChangedEvent(terrain!!))
    }

    private fun copyHeightData(data: FloatArray) {
        for (i in data.indices) {
            terrain!!.terrainAsset.terrain.heightData!![i] = data[i]
        }
    }

}
