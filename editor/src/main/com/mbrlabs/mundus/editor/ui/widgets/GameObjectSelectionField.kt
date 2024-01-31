package com.mbrlabs.mundus.editor.ui.widgets

import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisTextButton
import com.kotcrab.vis.ui.widget.VisTextField
import com.mbrlabs.mundus.commons.scene3d.GameObject
import com.mbrlabs.mundus.editor.ui.UI
import com.mbrlabs.mundus.editor.ui.modules.dialogs.gameobjects.GameObjectFilter
import com.mbrlabs.mundus.editor.ui.modules.dialogs.gameobjects.GameObjectPickerDialog

/**
 * A UI field for selecting a GameObject. Filters can be applied to the list of GameObjects.
 * @author JamesTKhan
 * @version July 02, 2023
 */
class GameObjectSelectionField : VisTable() {

    val textField: VisTextField = VisTextField()
    private val btn: VisTextButton

    var pickerListener: GameObjectPickerDialog.GameObjectPickerListener? = null
    var gameObjectFilter: GameObjectFilter? = null

    private val internalListener: GameObjectPickerDialog.GameObjectPickerListener

    init {
        textField.isDisabled = true
        btn = VisTextButton("Select")

        add(textField).grow()
        add(btn).padLeft(5f).row()

        internalListener = object: GameObjectPickerDialog.GameObjectPickerListener {
            override fun onSelected(go: GameObject?) {
                setGameObject(go)
                pickerListener?.onSelected(go)
            }
        }

        btn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                UI.gameObjectSelectionDialog.show(true, gameObjectFilter, internalListener)
            }
        })
    }

    fun setGameObject(go: GameObject?) {
        textField.text = if (go == null) "None" else go.name
    }

}