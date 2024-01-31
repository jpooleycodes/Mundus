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

package com.mbrlabs.mundus.editor.ui.modules.menu

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.kotcrab.vis.ui.widget.Menu
import com.kotcrab.vis.ui.widget.MenuItem
import com.mbrlabs.mundus.editor.Mundus
import com.mbrlabs.mundus.editor.core.project.ProjectManager
import com.mbrlabs.mundus.editor.ui.UI
import com.mbrlabs.mundus.editor.ui.modules.dialogs.tools.AssetCleanUpDialog

/**
 * @author JamesTKhan
 * @version July 28, 2022
 */
class ToolsMenu : Menu("Tools") {

    private val findUnusedAssets = MenuItem("Asset Clean Up")
    private val debugRendering = MenuItem("Debug Render Options")
    private val fixTerrainSeams = MenuItem("Fix Terrain Seams")

    val projectManager: ProjectManager = Mundus.inject()

    init {
        addItem(findUnusedAssets)
        addItem(debugRendering)
        addItem(fixTerrainSeams)

        findUnusedAssets.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                val dialog = AssetCleanUpDialog()
                UI.showDialog(dialog)
                Thread {
                    val unusedAssets = projectManager.current().assetManager.findUnusedAssets(projectManager)
                    Gdx.app.postRunnable {
                        dialog.setAssetsToDelete(unusedAssets)
                    }
                }.start()
            }
        })

        debugRendering.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                UI.showDialog(UI.debugRenderDialog)
            }
        })

        fixTerrainSeams.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                UI.showDialog(UI.terrainStitcherDialog)
            }
        })
    }

}
