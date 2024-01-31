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

package com.mbrlabs.mundus.editor.ui.modules.inspector.assets

import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisTextButton
import com.mbrlabs.mundus.commons.assets.TerrainAsset
import com.mbrlabs.mundus.commons.scene3d.GameObject
import com.mbrlabs.mundus.commons.utils.ModelUtils
import com.mbrlabs.mundus.editor.Mundus
import com.mbrlabs.mundus.editor.Mundus.postEvent
import com.mbrlabs.mundus.editor.core.project.ProjectManager
import com.mbrlabs.mundus.editor.events.SceneGraphChangedEvent
import com.mbrlabs.mundus.editor.scene3d.components.PickableTerrainComponent
import com.mbrlabs.mundus.editor.ui.modules.inspector.BaseInspectorWidget
import com.mbrlabs.mundus.editor.ui.widgets.LevelOfDetailWidget

/**
 * @author Marcus Brummer
 * @version 15-10-2016
 */
class TerrainAssetInspectorWidget : BaseInspectorWidget(TITLE) {

    companion object {
        private val TITLE = "Terrain Asset"
    }

    private val projectManager: ProjectManager = Mundus.inject()

    private val name = VisLabel()
    private var terrainAsset: TerrainAsset? = null
    private val vertexCount = VisLabel()
    private val indexCount = VisLabel()
    private val splatMapSize = VisLabel()
    private val lodWidget = LevelOfDetailWidget()

    // actions
    private val terrainPlacement = VisTextButton("Add Terrain to Scene")

    init {
        collapsibleContent.add(name).growX().row()
        collapsibleContent.add(vertexCount).growX().row()
        collapsibleContent.add(indexCount).growX().row()
        collapsibleContent.add(splatMapSize).growX().row()
        collapsibleContent.add(lodWidget).padTop(5f).growX().row()

        // actions
        collapsibleContent.add(VisLabel("Actions")).padTop(5f).growX().row()
        collapsibleContent.addSeparator().padBottom(5f).row()
        val placementLabel = VisLabel("Note: Places Terrain at 0,0,0. Modifying any placed instance of an existing Terrain Asset updates all instances.")
        placementLabel.wrap = true
        collapsibleContent.add(placementLabel).padBottom(5f).grow().row()
        collapsibleContent.add(terrainPlacement).growX().padBottom(15f).row()

        // model placement action
        terrainPlacement.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                if (terrainAsset == null) return

                val id = projectManager.current().obtainID()
                val modelGo = GameObject(projectManager.current().currScene.sceneGraph, terrainAsset!!.name, id)

                projectManager.current().currScene.sceneGraph.addGameObject(modelGo)

                val newComponent = PickableTerrainComponent(modelGo)
                newComponent.terrainAsset = terrainAsset
                newComponent.encodeRaypickColorId()

                projectManager.current().currScene.terrains.add(newComponent)

                modelGo.addComponent(newComponent)

                postEvent(SceneGraphChangedEvent())
            }
        })
    }

    fun setTerrainAsset(asset: TerrainAsset) {
        this.terrainAsset = asset
        updateUI()
    }

    private fun updateUI() {
        val model = terrainAsset!!.terrain.model

        name.setText("Name: " + terrainAsset!!.name)
        vertexCount.setText("Vertices: " + ModelUtils.getVerticesCount(model))
        indexCount.setText("Indices: " + ModelUtils.getIndicesCount(model))
        splatMapSize.setText("SplatMap Resolution: " + terrainAsset!!.meta.terrain.splatMapResolution)
        lodWidget.setLodLevels(terrainAsset!!.lodLevels)
    }

    override fun onDelete() {

    }

    override fun setValues(go: GameObject) {

    }

}
