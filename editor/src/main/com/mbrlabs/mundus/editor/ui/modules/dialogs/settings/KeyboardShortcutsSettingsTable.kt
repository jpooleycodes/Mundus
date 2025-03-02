/*
 * Copyright (c) 2025. See AUTHORS file.
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

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.ObjectIntMap
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisTextButton
import com.mbrlabs.mundus.editor.Mundus
import com.mbrlabs.mundus.editor.core.io.IOManager
import com.mbrlabs.mundus.editor.core.io.IOManagerProvider
import com.mbrlabs.mundus.editor.core.keymap.KeymapKey
import com.mbrlabs.mundus.editor.core.keymap.KeymapManager
import com.mbrlabs.mundus.editor.core.registry.Registry
import com.mbrlabs.mundus.editor.events.SettingsChangedEvent
import com.mbrlabs.mundus.editor.ui.UI

class KeyboardShortcutsSettingsTable : BaseSettingsTable() {

    private val keymapManager = Mundus.inject<KeymapManager>()
    private val registry: Registry = Mundus.inject()
    private val ioManager: IOManager = Mundus.inject<IOManagerProvider>().ioManager
    private val changeInputListener = ChangeInputListener()
    private val keyboardShortcutsTable = VisTable()

    private val changedKeyboardShortcuts = ObjectIntMap<KeymapKey>()

    init {
        top().left()
        padRight(UI.PAD_SIDE).padLeft(UI.PAD_SIDE)

        add(VisLabel("Keyboard Shortcuts Settings")).left().row()
        addSeparator().padBottom(UI.PAD_SIDE*2)

        keyboardShortcutsTable.defaults().pad(4f)
        add(keyboardShortcutsTable).growX()
    }

    override fun onInit() {
        changedKeyboardShortcuts.clear()
        initKeymapTable()
    }

    override fun onSave() {
        // Save changed shortcuts
        for (changedKeyboardShortcut in changedKeyboardShortcuts) {
            keymapManager.setKey(changedKeyboardShortcut.key, changedKeyboardShortcut.value)
        }
        changedKeyboardShortcuts.clear()

        // Save into registry
        val customKeyboardShortcuts = registry.settings.customKeyboardShortcuts
        customKeyboardShortcuts.clear()

        if (keymapManager.getKey(KeymapKey.MOVE_FORWARD) != KeymapManager.MOVE_FORWARD_DEFAULT_KEY) {
            customKeyboardShortcuts.put(KeymapKey.MOVE_FORWARD.name, Input.Keys.toString(keymapManager.getKey(KeymapKey.MOVE_FORWARD)))
        }
        if (keymapManager.getKey(KeymapKey.MOVE_BACK) != KeymapManager.MOVE_BACK_DEFAULT_KEY) {
            customKeyboardShortcuts.put(KeymapKey.MOVE_BACK.name, Input.Keys.toString(keymapManager.getKey(KeymapKey.MOVE_BACK)))
        }
        if (keymapManager.getKey(KeymapKey.STRAFE_LEFT) != KeymapManager.STRAFE_LEFT_DEFAULT_KEY) {
            customKeyboardShortcuts.put(KeymapKey.STRAFE_LEFT.name, Input.Keys.toString(keymapManager.getKey(KeymapKey.STRAFE_LEFT)))
        }
        if (keymapManager.getKey(KeymapKey.STRAFE_RIGHT) != KeymapManager.STRAFE_RIGHT_DEFAULT_KEY) {
            customKeyboardShortcuts.put(KeymapKey.STRAFE_RIGHT.name, Input.Keys.toString(keymapManager.getKey(KeymapKey.STRAFE_RIGHT)))
        }

        ioManager.saveRegistry(registry)
        Mundus.postEvent(SettingsChangedEvent(registry.settings))
        UI.toaster.success("Settings saved")
    }

    private fun initKeymapTable() {
        keyboardShortcutsTable.clear()

        addShortcut(KeymapKey.MOVE_FORWARD, "Move Forward", keyboardShortcutsTable)
        addShortcut(KeymapKey.MOVE_BACK, "Move Back", keyboardShortcutsTable)
        addShortcut(KeymapKey.STRAFE_LEFT, "Strafe Left", keyboardShortcutsTable)
        addShortcut(KeymapKey.STRAFE_RIGHT, "Strafe Right", keyboardShortcutsTable)
    }

    private fun addShortcut(keymapKey: KeymapKey, desc: String, table: VisTable) {
        val keyLabel = VisLabel(Input.Keys.toString(keymapManager.getKey(keymapKey)))

        val changeButton = VisTextButton("Change")
        changeButton.addListener(ChangeButtonListener(keymapKey, keyLabel))

        val resetButton = VisTextButton("Reset")
        resetButton.addListener(ResetButtonListener())

        table.add(keyLabel).left()
        table.add(changeButton).left()
        table.add(resetButton).left()
        table.addSeparator(true)
        table.add(desc).left().row()
        table.addSeparator().colspan(5)
    }

    private inner class ChangeButtonListener(val keymapKey: KeymapKey, val keyLabel: VisLabel) : ClickListener() {
        override fun clicked(event: InputEvent, x: Float, y: Float) {
            changeInputListener.originalInputListeners = Gdx.input.inputProcessor
            changeInputListener.keymapKey = keymapKey
            changeInputListener.keyLabel = keyLabel
            Gdx.input.inputProcessor = changeInputListener

            keyLabel.setText("---")

        }
    }

    private class ResetButtonListener : ClickListener() {
        override fun clicked(event: InputEvent, x: Float, y: Float) {
            // TODO
        }
    }

    private inner class ChangeInputListener : InputAdapter() {

        var originalInputListeners: InputProcessor? = null
        lateinit var keymapKey: KeymapKey
        lateinit var keyLabel: VisLabel

        override fun keyUp(keycode: Int): Boolean {
            if (keycode == Input.Keys.ESCAPE) {
                keyLabel.setText(Input.Keys.toString(keymapManager.getKey(keymapKey)))
            } else {
                changedKeyboardShortcuts.put(keymapKey, keycode)
                keyLabel.setText(Input.Keys.toString(keycode))
            }

            Gdx.input.inputProcessor = originalInputListeners
            return true
        }
    }
}
