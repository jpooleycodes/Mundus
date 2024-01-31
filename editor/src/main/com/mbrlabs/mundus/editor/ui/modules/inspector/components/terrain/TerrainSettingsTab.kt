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

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.utils.Align
import com.kotcrab.vis.ui.widget.VisCheckBox
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.tabbedpane.Tab
import com.mbrlabs.mundus.editor.Mundus
import com.mbrlabs.mundus.editor.core.project.ProjectManager
import com.mbrlabs.mundus.editor.ui.widgets.TerrainNeighborWidget
import com.mbrlabs.mundus.editor.ui.widgets.ToolTipLabel

/**
 * @author Marcus Brummer
 * @version 30-01-2016
 */
class TerrainSettingsTab(private val parentWidget: TerrainComponentWidget) : Tab(false, false) {

    private val table = VisTable()
    private val triplanar = VisCheckBox(null)
    private var neighborWidget: TerrainNeighborWidget? = null

    private val projectManager: ProjectManager = Mundus.inject()

    init {
        table.defaults().padLeft(10f).padBottom(5f).align(Align.left)
        table.add(VisLabel("Shader: ")).row()

        val triplanarTable = VisTable()
        triplanarTable.add(ToolTipLabel("Triplanar", "Enables Triplanar texturing which calculates UV coordinates\n" +
                "in the shader instead of using Vertex coordinates.\n Reduces texture stretching on steep terrain.")).left()
        triplanarTable.add(triplanar).left().row()
        table.add(triplanarTable).expandX().left().row()
        triplanar.isChecked = parentWidget.component.terrainAsset.terrain.terrainTexture.isTriplanar
        triplanar.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                val assetManager = projectManager.current().assetManager
                assetManager.addModifiedAsset(parentWidget.component.terrainAsset)
                parentWidget.component.terrainAsset.setTriplanar(triplanar.isChecked)
            }
        })

        table.addSeparator()
        neighborWidget = TerrainNeighborWidget(parentWidget.component)
        table.add(neighborWidget).expand().fill().row()
    }

    override fun getTabTitle(): String {
        return "Settings"
    }

    override fun getContentTable(): Table {
        return table
    }

}
