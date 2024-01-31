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

package com.mbrlabs.mundus.editor.ui.modules.inspector.assets

import com.mbrlabs.mundus.commons.assets.TerrainLayerAsset
import com.mbrlabs.mundus.commons.scene3d.GameObject
import com.mbrlabs.mundus.editor.ui.modules.inspector.BaseInspectorWidget
import com.mbrlabs.mundus.editor.ui.widgets.TerrainTextureLayerWidget

/**
 * @author JamesTKhan
 * @version July 12, 2023
 */
class TerrainLayerAssetInspectorWidget() : BaseInspectorWidget(TITLE)  {
    companion object {
        private val TITLE = "Terrain Layer Asset"
    }

    private var terrainTextureLayerWidget : TerrainTextureLayerWidget? = null
    private var layerAsset: TerrainLayerAsset? = null

    init {
        isDeletable = false
    }

    fun setTerrainLayerAsset(layerAsset: TerrainLayerAsset) {
        this.layerAsset = layerAsset
        if (terrainTextureLayerWidget == null) {
            terrainTextureLayerWidget = TerrainTextureLayerWidget(layerAsset, false)
            collapsibleContent.add(terrainTextureLayerWidget).growX().row()
        } else {
            terrainTextureLayerWidget!!.setTerrainLayerAsset(layerAsset)
        }
    }

    override fun onDelete() {

    }

    override fun setValues(go: GameObject) {

    }

}