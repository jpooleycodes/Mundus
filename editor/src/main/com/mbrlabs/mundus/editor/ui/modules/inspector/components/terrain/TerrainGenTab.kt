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
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.tabbedpane.Tab
import com.kotcrab.vis.ui.widget.tabbedpane.TabbedPane
import com.kotcrab.vis.ui.widget.tabbedpane.TabbedPaneListener
import com.mbrlabs.mundus.editor.ui.modules.inspector.components.terrain.generation.HeightmapTab
import com.mbrlabs.mundus.editor.ui.modules.inspector.components.terrain.generation.PerlinNoiseTab

/**
 * @Deprecated
 * @author Marcus Brummer
 * @version 04-03-2016
 */
class TerrainGenTab(parent: TerrainComponentWidget) : Tab(false, false), TabbedPaneListener {
    private val root = VisTable()

    private val tabbedPane = TabbedPane()
    private val tabContainer = VisTable()

    private val heightmapTab = HeightmapTab(parent.component)
    private val perlinNoiseTab = PerlinNoiseTab(parent.component)

    init {
        tabbedPane.addListener(this)

        tabbedPane.add(heightmapTab)
        tabbedPane.add(perlinNoiseTab)

        root.add(tabbedPane.table).growX().row()
        root.add(tabContainer).expand().fill().row()
        tabbedPane.switchTab(0)
    }


    override fun getTabTitle(): String = "Gen"

    override fun getContentTable(): Table = root

    override fun switchedTab(tab: Tab) {
        tabContainer.clearChildren()
        tabContainer.add(tab.contentTable).expand().fill()
    }

    override fun removedTab(tab: Tab) {
        // no
    }

    override fun removedAllTabs() {
        // noop
    }

}
