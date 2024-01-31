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
package com.mbrlabs.mundus.editor.tools

import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Disposable
import com.mbrlabs.mundus.commons.scene3d.GameObject
import com.mbrlabs.mundus.editor.Mundus
import com.mbrlabs.mundus.editor.core.project.ProjectManager
import com.mbrlabs.mundus.editor.events.GameObjectSelectedEvent
import com.mbrlabs.mundus.editor.events.ToolActivatedEvent
import com.mbrlabs.mundus.editor.events.ToolDeactivatedEvent
import com.mbrlabs.mundus.editor.history.CommandHistory
import com.mbrlabs.mundus.editor.input.InputManager
import com.mbrlabs.mundus.editor.preferences.MundusPreferencesManager
import com.mbrlabs.mundus.editor.tools.brushes.*
import com.mbrlabs.mundus.editor.tools.picker.GameObjectPicker
import com.mbrlabs.mundus.editor.tools.picker.ToolHandlePicker
import com.mbrlabs.mundus.editor.ui.UI

/**
 * @author Marcus Brummer
 * @version 25-12-2015
 */
class ToolManager(private val inputManager: InputManager,
                  projectManager: ProjectManager,
                  goPicker: GameObjectPicker,
                  toolHandlePicker: ToolHandlePicker,
                  shapeRenderer: ShapeRenderer,
                  history: CommandHistory,
                  globalPreferencesManager: MundusPreferencesManager)
    : InputAdapter(), Disposable {
    var activeTool: Tool? = null
        private set
    var terrainBrushes: Array<TerrainBrush>
    var modelPlacementTool: ModelPlacementTool
    var selectionTool: SelectionTool
    var translateTool: TranslateTool
    var rotateTool: RotateTool
    var scaleTool: ScaleTool

    init {
        terrainBrushes = Array()
        terrainBrushes.add(SmoothCircleBrush(projectManager, history, goPicker))
        terrainBrushes.add(CircleBrush(projectManager, history, goPicker))
        terrainBrushes.add(StarBrush(projectManager, history, goPicker))
        terrainBrushes.add(ConfettiBrush(projectManager, history, goPicker))
        modelPlacementTool = ModelPlacementTool(projectManager, history)
        selectionTool = SelectionTool(projectManager, goPicker, history, globalPreferencesManager)
        translateTool = TranslateTool(projectManager, goPicker, toolHandlePicker, history, globalPreferencesManager)
        rotateTool = RotateTool(projectManager, goPicker, toolHandlePicker, shapeRenderer, history, globalPreferencesManager)
        scaleTool = ScaleTool(projectManager, goPicker, toolHandlePicker, shapeRenderer, history, globalPreferencesManager)
    }

    fun activateTool(tool: Tool?) {
        val shouldKeepSelection = activeTool != null && activeTool is SelectionTool && tool is SelectionTool
        val selected = getSelectedObject()
        deactivateTool()
        activeTool = tool
        inputManager.addProcessor(activeTool)
        activeTool!!.onActivated()
        if (shouldKeepSelection && selected != null) {
            (activeTool as SelectionTool?)!!.gameObjectSelected(selected)
        }
        Mundus.postEvent(ToolActivatedEvent())
    }

    fun deactivateTool() {
        if (activeTool != null) {
            activeTool!!.onDisabled()
            inputManager.removeProcessor(activeTool)
            activeTool = null
        }
    }

    fun setDefaultTool() {
        if (activeTool == null || activeTool === modelPlacementTool || activeTool is TerrainBrush) {
            UI.statusBar.clearMousePos()
            val selectedGO = UI.outline.getSelectedGameObject()
            activateTool(translateTool)
            if (selectedGO != null) {
                activateTool(translateTool)
                Mundus.postEvent(GameObjectSelectedEvent(UI.outline.getSelectedGameObject()))
            }
        } else {
            activeTool!!.onDisabled()
            UI.outline.clearSelection()
            UI.inspector.clearWidgets()
        }
    }

    fun render() {
        if (activeTool != null) {
            activeTool!!.render()
        }
    }

    fun act() {
        if (activeTool != null) {
            activeTool!!.act()
        }
    }

    fun isSelected(go: GameObject): Boolean {
        return go == getSelectedObject()
    }

    override fun keyUp(keycode: Int): Boolean {
        if (keycode == KEY_DEACTIVATE) {
            if (activeTool != null) {
                activeTool!!.onDisabled()
                Mundus.postEvent(ToolDeactivatedEvent())
            }
            setDefaultTool()
            UI.docker.assetsDock.clearSelection()
            return true
        }
        return false
    }

    override fun dispose() {
        for (brush in terrainBrushes) {
            brush.dispose()
        }
        translateTool.dispose()
        modelPlacementTool.dispose()
        selectionTool.dispose()
        rotateTool.dispose()
        scaleTool.dispose()
    }

    private fun getSelectedObject() : GameObject? {
        if (activeTool == null) {
            return null
        }
        val scene = activeTool!!.projectManager.current().currScene ?: return null
        return scene.currentSelection
    }

    companion object {
        private const val KEY_DEACTIVATE = Input.Keys.ESCAPE
    }
}