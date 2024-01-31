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

import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.tabbedpane.Tab
import com.kotcrab.vis.ui.widget.tabbedpane.TabbedPane
import com.kotcrab.vis.ui.widget.tabbedpane.TabbedPaneListener
import com.mbrlabs.mundus.commons.assets.MaterialAsset
import com.mbrlabs.mundus.commons.scene3d.GameObject
import com.mbrlabs.mundus.commons.scene3d.components.Component
import com.mbrlabs.mundus.commons.scene3d.components.TerrainComponent
import com.mbrlabs.mundus.editor.Mundus
import com.mbrlabs.mundus.editor.core.project.ProjectManager
import com.mbrlabs.mundus.editor.tools.ToolManager
import com.mbrlabs.mundus.editor.tools.brushes.TerrainBrush
import com.mbrlabs.mundus.editor.ui.UI
import com.mbrlabs.mundus.editor.ui.modules.inspector.components.ComponentWidget
import com.mbrlabs.mundus.editor.ui.widgets.MaterialSelectWidget
import com.mbrlabs.mundus.editor.ui.widgets.MaterialWidget

/**
 * @author Marcus Brummer
 * @version 29-01-2016
 */
class TerrainComponentWidget(terrainComponent: TerrainComponent) :
        ComponentWidget<TerrainComponent>("Terrain Component", terrainComponent), TabbedPaneListener {

    private val tabbedPane = TabbedPane()
    private val tabContainer = VisTable()
    private val materialContainer = VisTable()
    private val projectManager: ProjectManager = Mundus.inject()
    private val toolManager: ToolManager = Mundus.inject()

    private val raiseLowerTab = TerrainUpDownTab(this)
    private val flattenTab = TerrainFlattenTab(this)
    private val smoothTab = TerrainSmoothTab(this)
    private val rampTab = TerrainRampTab(this)
    private val paintTab = TerrainPaintTab(this)
    private val settingsTab = TerrainSettingsTab(this)
    private var initializing = true

    init {
        tabbedPane.addListener(this)

        tabbedPane.add(raiseLowerTab)
        tabbedPane.add(flattenTab)
        tabbedPane.add(smoothTab)
        tabbedPane.add(rampTab)
        tabbedPane.add(paintTab)
        tabbedPane.add(settingsTab)

        collapsibleContent.add(tabbedPane.table).growX().row()
        collapsibleContent.add(VisLabel("Use CTRL+Scroll Wheel to adjust brush size")).center().padBottom(4f).row()
        collapsibleContent.add(tabContainer).expand().fill().row()
        collapsibleContent.add(VisLabel("Material")).expandX().fillX().left().padBottom(4f).padTop(4f).row()
        collapsibleContent.addSeparator().row()
        collapsibleContent.add(materialContainer).grow().row()
        buildMaterials()
        tabbedPane.switchTab(0)
        initializing = false
    }

    private fun buildMaterials() {
        materialContainer.clear()
        val selectWidget = MaterialSelectWidget(component.terrainAsset.materialAsset)
        materialContainer.add(selectWidget).grow().padBottom(20f).row()
        selectWidget.matChangedListener = object: MaterialWidget.MaterialChangedListener {
            override fun materialChanged(materialAsset: MaterialAsset) {
                component.terrainAsset.materialAsset = materialAsset
                component.applyMaterial()
                val assetManager = projectManager.current().assetManager
                assetManager.addModifiedAsset(component.terrainAsset)
            }
        }
    }

    override fun setValues(go: GameObject) {
        val c: TerrainComponent? = go.findComponentByType(Component.Type.TERRAIN)
        if (c != null) {
            this.component = c
        }
    }

    override fun switchedTab(tab: Tab) {
        tabContainer.clearChildren()
        tabContainer.add(tab.contentTable).expand().fill()

        // Don't toggle tool if the listener is called during init
        if (initializing) return

        // Set the active tool to default translate tool if changing tab
        val projectManager: ProjectManager = Mundus.inject()
        if (toolManager.activeTool is TerrainBrush) {
            toolManager.activateTool(toolManager.translateTool)
            projectManager.current().currScene.currentSelection = UI.outline.getSelectedGameObject()
            UI.statusBar.clearMousePos()
        }
    }

    override fun removedTab(tab: Tab) {
        // no
    }

    override fun removedAllTabs() {
        // nope
    }

}
