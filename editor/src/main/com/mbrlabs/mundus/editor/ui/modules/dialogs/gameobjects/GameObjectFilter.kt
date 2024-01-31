package com.mbrlabs.mundus.editor.ui.modules.dialogs.gameobjects

import com.mbrlabs.mundus.commons.scene3d.GameObject

/**
 * Interface for filtering GameObjects.
 * Implementations of this interface can be used to filter GameObjects in the GameObjectPickerDialog.
 * @author JamesTKhan
 * @version July 02, 2023
 */
interface GameObjectFilter {
    fun ignore(go: GameObject): Boolean
}