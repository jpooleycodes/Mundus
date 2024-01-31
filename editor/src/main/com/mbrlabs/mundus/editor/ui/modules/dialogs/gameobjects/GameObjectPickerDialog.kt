package com.mbrlabs.mundus.editor.ui.modules.dialogs.gameobjects

import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Array
import com.kotcrab.vis.ui.util.adapter.SimpleListAdapter
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisTextButton
import com.mbrlabs.mundus.commons.scene3d.GameObject
import com.mbrlabs.mundus.editor.Mundus
import com.mbrlabs.mundus.editor.core.project.ProjectManager
import com.mbrlabs.mundus.editor.events.ProjectChangedEvent
import com.mbrlabs.mundus.editor.events.SceneChangedEvent
import com.mbrlabs.mundus.editor.events.SceneGraphChangedEvent
import com.mbrlabs.mundus.editor.ui.UI
import com.mbrlabs.mundus.editor.ui.modules.dialogs.BaseDialog
import com.mbrlabs.mundus.editor.ui.widgets.AutoFocusListView

/**
 * A filterable list of GameObjects.
 *
 * The user can pick one or no GameObject. The list of GameObjects can be filtered before
 * showing it to the user via GameObjectFilter.
 *
 * @author JamesTKhan
 * @version July 02, 2023
 */
class GameObjectPickerDialog : BaseDialog(TITLE),
    SceneGraphChangedEvent.SceneGraphChangedListener,
    SceneChangedEvent.SceneChangedListener,
    ProjectChangedEvent.ProjectChangedListener {

    private companion object {
        private val TITLE = "Select a GameObject"
    }

    private val root = VisTable()
    private val listAdapter = SimpleListAdapter(Array<GameObject>())
    private val list = AutoFocusListView(listAdapter)
    private val noneBtn = VisTextButton("None / Remove")

    private var filter: GameObjectFilter? = null
    private var listener: GameObjectPickerListener? = null

    private val projectManager: ProjectManager = Mundus.inject()

    init {
        Mundus.registerEventListener(this)

        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        root.add(list.mainTable).grow().size(350f, 450f).row()
        root.add<VisTextButton>(noneBtn).padTop(10f).grow().row()
        add<VisTable>(root).padRight(5f).padBottom(5f).grow().row()
    }

    private fun setupListeners() {
        list.setItemClickListener { item ->
            if (listener != null) {
                listener!!.onSelected(item)
                close()
            }
        }

        noneBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                if (listener != null) {
                    listener!!.onSelected(null)
                    close()
                }
            }
        })
    }

    override fun onProjectChanged(event: ProjectChangedEvent) {
        reloadData()
    }

    override fun onSceneChanged(event: SceneChangedEvent) {
        reloadData()
    }

    override fun onSceneGraphChanged(event: SceneGraphChangedEvent) {
        reloadData()
    }

    private fun reloadData() {
        val gos = Array<GameObject>()
        projectManager.current().currScene.sceneGraph.getAllGameObjects(gos)

        listAdapter.clear()

        // filter GameObjects
        for (go in gos) {
            if (filter != null) {
                if (filter!!.ignore(go)) {
                    continue
                }
            }
            listAdapter.add(go)
        }

        listAdapter.itemsDataChanged()
    }

    /**
     * Shows the dialog.
     *
     * @param showNoneOption if true the user will be able to select a NONE GameObject
     * @param filter optional GameObject type filter
     * @listener picker listener
     */
    fun show(showNoneOption: Boolean, filter: GameObjectFilter?, listener: GameObjectPickerListener) {
        this.listener = listener
        this.filter = filter
        if (showNoneOption) {
            noneBtn.isDisabled = false
            noneBtn.touchable = Touchable.enabled
        } else {
            noneBtn.isDisabled = true
            noneBtn.touchable = Touchable.disabled
        }
        reloadData()
        UI.showDialog(this)
    }

    /**
     */
    interface GameObjectPickerListener {
        fun onSelected(go: GameObject?)
    }

}