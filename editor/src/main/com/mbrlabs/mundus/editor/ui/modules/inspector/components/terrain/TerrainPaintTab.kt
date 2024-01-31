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

package com.mbrlabs.mundus.editor.ui.modules.inspector.components.terrain

import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.kotcrab.vis.ui.widget.VisTable
import com.mbrlabs.mundus.commons.assets.TerrainLayerAsset
import com.mbrlabs.mundus.commons.terrain.SplatTexture
import com.mbrlabs.mundus.editor.Mundus
import com.mbrlabs.mundus.editor.assets.MetaSaver
import com.mbrlabs.mundus.editor.core.project.ProjectManager
import com.mbrlabs.mundus.editor.tools.brushes.TerrainBrush
import com.mbrlabs.mundus.editor.ui.widgets.TerrainTextureLayerWidget

/**
 * @author Marcus Brummer
 * @version 30-01-2016
 */
class TerrainPaintTab(private val parentWidget: TerrainComponentWidget) : BaseBrushTab(parentWidget, TerrainBrush.BrushMode.PAINT) {

    companion object {
        private val TAG = TerrainPaintTab::class.java.simpleName
    }

    private val root = VisTable()
    private val terrainTextureLayerWidget = TerrainTextureLayerWidget(this.parentWidget.component.terrainAsset.terrainLayerAsset)

    private val projectManager: ProjectManager = Mundus.inject()
    private val metaSaver: MetaSaver = Mundus.inject()

    init {
        root.align(Align.left)

        // brushes
        root.add(terrainBrushGrid).expand().fill().padBottom(5f).row()
        root.add(terrainTextureLayerWidget).expand().fill().padBottom(5f).row()

        terrainTextureLayerWidget.layerChangedListener = object : TerrainTextureLayerWidget.LayerChangedListener {
            override fun layerChanged(terrainLayerAsset: TerrainLayerAsset) {
                // Assign the new layer asset to the terrain
                parentWidget.component.terrainAsset.terrainLayerAsset = terrainLayerAsset
                parentWidget.component.applyMaterial()
                metaSaver.save(parentWidget.component.terrainAsset.meta)
                projectManager.current().assetManager.addModifiedAsset(parentWidget.component.terrainAsset)
            }
        }

        terrainTextureLayerWidget.layerUpdatedListener = object : TerrainTextureLayerWidget.LayerUpdatedListener {
            override fun layerUpdated(terrainLayerAsset: TerrainLayerAsset) {
                parentWidget.component.terrainAsset.updateTerrainMaterial()
                parentWidget.component.applyMaterial()
                projectManager.current().assetManager.addModifiedAsset(parentWidget.component.terrainAsset)
            }
        }

    }

    override fun onShow() {
        super.onShow()

        // At tab open the first (base) texture will be selected
        TerrainBrush.setPaintChannel(SplatTexture.Channel.BASE)
        terrainTextureLayerWidget.textureGrid.highlightFirst()
    }

    override fun getTabTitle(): String {
        return "Paint"
    }

    override fun getContentTable(): Table {
        return root
    }
}
