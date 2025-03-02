package com.mbrlabs.mundus.editor.ui.modules.dialogs

import com.badlogic.gdx.Input
import com.kotcrab.vis.ui.widget.VisTable
import com.mbrlabs.mundus.editor.Mundus
import com.mbrlabs.mundus.editor.core.keymap.KeymapKey
import com.mbrlabs.mundus.editor.core.keymap.KeymapManager

/**
 * @author JamesTKhan
 * @version July 26, 2022
 */
class KeyboardShortcutsDialog : BaseDialog("Keyboard Shortcuts") {

    private val keymapManager = Mundus.inject<KeymapManager>()
    private lateinit var root: VisTable

    init {
        setupUI()
    }

    private fun setupUI() {
        root = VisTable()
        root.defaults().pad(6f)

        val shortcutTableOne = VisTable()
        shortcutTableOne.defaults().pad(4f)

        addShortcut(keymapManager.getKey(KeymapKey.MOVE_FORWARD), "Move Forward", shortcutTableOne)
        addShortcut(keymapManager.getKey(KeymapKey.MOVE_BACK), "Move Back", shortcutTableOne)
        addShortcut(keymapManager.getKey(KeymapKey.STRAFE_LEFT), "Strafe Left", shortcutTableOne)
        addShortcut(keymapManager.getKey(KeymapKey.STRAFE_RIGHT), "Strafe Right", shortcutTableOne)
        addShortcut("Q", "Move Up", shortcutTableOne)
        addShortcut("E", "Move Down", shortcutTableOne)
        addShortcut("Hold Middle Click", "Look Around", shortcutTableOne)
        addShortcut("Scroll", "Zoom forward/backward", shortcutTableOne)
        addShortcut("Hold Shift", "Camera Panning", shortcutTableOne)
        addShortcut("F8", "Toggle Fullscreen 3d", shortcutTableOne)

        root.add(shortcutTableOne).top()
        root.addSeparator(true)

        val shortcutTableTwo = VisTable()
        shortcutTableTwo.defaults().pad(4f)

        addShortcut("CTRL+Z", "Undo", shortcutTableTwo)
        addShortcut("CTRL+Y", "Redo", shortcutTableTwo)
        addShortcut("CTRL+S", "Save Project", shortcutTableTwo)
        addShortcut("CTRL+T", "Translate Tool", shortcutTableTwo)
        addShortcut("CTRL+R", "Rotate Tool", shortcutTableTwo)
        addShortcut("CTRL+G", "Scale Tool", shortcutTableTwo)
        addShortcut("CTRL+F", "Select Tool", shortcutTableTwo)
        addShortcut("CTRL+F2", "Debug Render Mode", shortcutTableTwo)
        addShortcut("CTRL+F3", "Wireframe Mode", shortcutTableTwo)

        root.add(shortcutTableTwo).top()
        add(root)
    }

    private fun addShortcut(key: String, desc: String, table: VisTable) {
        table.add(key).left()
        table.addSeparator(true)
        table.add(desc).left().row()
        table.addSeparator().colspan(3)
    }

    private fun addShortcut(keycode: Int, desc: String, table: VisTable) {
        table.add(Input.Keys.toString(keycode)).left()
        table.addSeparator(true)
        table.add(desc).left().row()
        table.addSeparator().colspan(3)
    }
}