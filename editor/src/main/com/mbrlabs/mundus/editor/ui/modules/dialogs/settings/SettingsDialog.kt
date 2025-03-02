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

package com.mbrlabs.mundus.editor.ui.modules.dialogs.settings

import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Tree
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisTextButton
import com.kotcrab.vis.ui.widget.VisTree
import com.mbrlabs.mundus.editor.ui.UI
import com.mbrlabs.mundus.editor.ui.modules.dialogs.BaseDialog

/**
 * @author Marcus Brummer
 * @version 24-11-2015
 */
class SettingsDialog : BaseDialog("Settings") {
    companion object {
        const val WIDTH = 700f
        const val HEIGHT = 400f
    }

    private val settingsTree = VisTree<SettingsNode, BaseSettingsTable>()
    private val content = VisTable()
    private val saveBtn = VisTextButton("Save")
    private var listener: ClickListener? = null

    private val generalSettings = GeneralSettingsTable()
    private val keyboardShortcutsSettings = KeyboardShortcutsSettingsTable()
    private val exportSettings = ExportSettingsTable()
    private val appearenceSettings = AppearanceSettingsTable()
    private val cameraSettings = CameraSettingsTable()
    private val performanceSettings = PerformanceSettingsTable()

    init {
        val root = VisTable()
        content.padRight(UI.PAD_SIDE)
        add(root).width(WIDTH).height(HEIGHT).row()

        root.add(settingsTree).width(WIDTH*0.3f).padRight(UI.PAD_SIDE).grow()
        root.addSeparator(true).padLeft(5f).padRight(5f)
        root.add(content).width(WIDTH*0.7f).grow().row()

        // general
        val generalSettingsNode = SettingsNode(VisLabel("General"))
        generalSettingsNode.value = generalSettings
        settingsTree.add(generalSettingsNode)

        // Keyboard shortcuts
        val keyboardShortcutsSettingsNode = SettingsNode(VisLabel("Keyboard Shortcuts"))
        keyboardShortcutsSettingsNode.value = keyboardShortcutsSettings
        settingsTree.add(keyboardShortcutsSettingsNode)

        // export
        val exportSettingsNode = SettingsNode(VisLabel("Export"))
        exportSettingsNode.value = exportSettings
        settingsTree.add(exportSettingsNode)

        // appearance
        val appearenceNode = SettingsNode(VisLabel("Appearance"))
        appearenceNode.value = appearenceSettings
        settingsTree.add(appearenceNode)

        val cameraNode = SettingsNode(VisLabel("Camera"))
        cameraNode.value = cameraSettings
        settingsTree.add(cameraNode)

        val perfNode = SettingsNode(VisLabel("Performance"))
        perfNode.value = performanceSettings
        settingsTree.add(perfNode)

        // listener
        settingsTree.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                val node = settingsTree.getNodeAt(y)
                replaceContent(node?.value)
            }
        })

        // set initial content
        settingsTree.selection.add(generalSettingsNode)
        replaceContent(generalSettings)
    }

    private fun replaceContent(table: BaseSettingsTable?) {
        if(table == null) return
        content.clear()
        content.add(table).grow().row()
        content.add(saveBtn).growX().bottom().pad(10f).row()

        if(listener != null) {
            saveBtn.removeListener(listener!!)
        }
        listener = object: ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                table.onSave()
            }
        }
        saveBtn.addListener(listener)

        table.onInit()
    }

    inner class SettingsNode(label: VisLabel) : Tree.Node<SettingsNode, BaseSettingsTable, VisLabel>(label) {

    }

}
