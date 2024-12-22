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
package com.mbrlabs.mundus.editor.ui.modules.outline

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Tree
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Array
import com.kotcrab.vis.ui.widget.*
import com.mbrlabs.mundus.commons.scene3d.GameObject
import com.mbrlabs.mundus.commons.scene3d.SceneGraph
import com.mbrlabs.mundus.commons.scene3d.components.Component
import com.mbrlabs.mundus.commons.scene3d.components.LightComponent
import com.mbrlabs.mundus.commons.utils.LightUtils
import com.mbrlabs.mundus.editor.Mundus
import com.mbrlabs.mundus.editor.Mundus.postEvent
import com.mbrlabs.mundus.editor.core.project.ProjectManager
import com.mbrlabs.mundus.editor.events.*
import com.mbrlabs.mundus.editor.history.CommandHistory
import com.mbrlabs.mundus.editor.history.commands.DeleteCommand
import com.mbrlabs.mundus.editor.scene3d.components.PickableLightComponent
import com.mbrlabs.mundus.editor.tools.ToolManager
import com.mbrlabs.mundus.editor.ui.UI
import com.mbrlabs.mundus.editor.utils.Colors
import com.mbrlabs.mundus.editor.utils.Log
import com.mbrlabs.mundus.editorcommons.events.GameObjectModifiedEvent
import com.mbrlabs.mundus.editorcommons.events.ProjectChangedEvent
import com.mbrlabs.mundus.editorcommons.events.SceneChangedEvent

/**
 * Outline shows overview about all game objects in the scene
 *
 * @author Marcus Brummer, codenigma
 * @version 01-10-2016
 */
// TODO refactor...kind of messy spaghetti code!
class Outline : VisTable(),
    ProjectChangedEvent.ProjectChangedListener,
    SceneChangedEvent.SceneChangedListener,
    SceneGraphChangedEvent.SceneGraphChangedListener,
    GameObjectSelectedEvent.GameObjectSelectedListener,
    AssetSelectedEvent.AssetSelectedListener {

    companion object {
        private val TITLE = "Outline"
        private val TAG = Outline::class.java.simpleName

        val tmpPos = Vector3()
        val tmpScale = Vector3()
        val tmpQuat = Quaternion()
    }

    private val content: VisTable
    internal val tree: VisTree<OutlineNode, GameObject>
    private val scrollPane: ScrollPane
    private val dragAndDrop: DragAndDrop = DragAndDrop()
    private val rightClickMenu: OutlineRightClickMenu

    private val toolManager: ToolManager = Mundus.inject()
    private val projectManager: ProjectManager = Mundus.inject()
    private val history: CommandHistory = Mundus.inject()

    init {
        Mundus.registerEventListener(this)
        setBackground("window-bg")

        rightClickMenu = OutlineRightClickMenu(this)

        content = VisTable()
        content.align(Align.left or Align.top)

        tree = VisTree<OutlineNode, GameObject>()
        tree.selection.setProgrammaticChangeEvents(false)
        tree.indentSpacing = 10f
        scrollPane = VisScrollPane(tree)
        scrollPane.setFlickScroll(false)
        scrollPane.setFadeScrollBars(false)
        content.add(scrollPane).fill().expand()

        add(VisLabel(TITLE)).expandX().fillX().pad(3f).row()
        addSeparator().row()
        add(content).fill().expand()

        setupDragAndDrop()
        setupListeners()
    }

    fun getSelectedGameObject(): GameObject? = tree.selectedValue

    fun clearSelection() {
        tree.selection.clear()
        projectManager.current().currScene.currentSelection = null
    }

    override fun onProjectChanged(event: ProjectChangedEvent) {
        // update to new sceneGraph
        Log.trace(TAG, "Project changed. Building scene graph.")
        buildTree(projectManager.current().currScene.sceneGraph)
    }

    override fun onSceneChanged(event: SceneChangedEvent) {
        // update to new sceneGraph
        Log.trace(TAG, "Scene changed. Building scene graph.")
        buildTree(projectManager.current().currScene.sceneGraph)
    }

    override fun onSceneGraphChanged(event: SceneGraphChangedEvent) {
        Log.trace(TAG, "SceneGraph changed. Building scene graph.")
        buildTree(projectManager.current().currScene.sceneGraph)
    }

    private fun setupDragAndDrop() {
        // source
        dragAndDrop.addSource(object : DragAndDrop.Source(tree) {
            override fun dragStart(event: InputEvent, x: Float, y: Float, pointer: Int): DragAndDrop.Payload? {
                val payload = DragAndDrop.Payload()
                val node = tree.getNodeAt(y)
                if (node != null) {
                    payload.`object` = node
                    return payload
                }

                return null
            }
        })

        // target
        dragAndDrop.addTarget(object : DragAndDrop.Target(tree) {
            override fun drag(
                source: DragAndDrop.Source,
                payload: DragAndDrop.Payload,
                x: Float,
                y: Float,
                pointer: Int
            ): Boolean {
                // Select node under mouse if not over the selection.
                val overNode = tree.getNodeAt(y)
                if (overNode == null && tree.getSelection().isEmpty) {
                    return true
                }
                if (overNode != null && !tree.getSelection().contains(overNode)) {
                    tree.getSelection().set(overNode)
                }
                return true
            }

            override fun drop(
                source: DragAndDrop.Source,
                payload: DragAndDrop.Payload,
                x: Float,
                y: Float,
                pointer: Int
            ) {
                val context = projectManager.current()
                val newParent = tree.getNodeAt(y)

                @Suppress("UNCHECKED_CAST")
                val node: Tree.Node<OutlineNode, GameObject, VisTable> =
                    (payload.`object` as? Tree.Node<OutlineNode, GameObject, VisTable>) ?: return
                val draggedGo: GameObject = node.value

                // check if a go is dragged in one of its' children or
                // itself
                if (newParent != null) {
                    val parentGo = newParent.value
                    if (parentGo.isChildOf(draggedGo)) {
                        return
                    }
                }

                // remove child from old parent
                draggedGo.remove()

                // add to new parent
                if (newParent == null) {

                    // Get the current world transform of the GameObject
                    val worldPos = draggedGo.getPosition(tmpPos)
                    val worldRot = draggedGo.getRotation(tmpQuat)
                    val worldScale = draggedGo.getScale(tmpScale)

                    // Set the local transform to the current world transform
                    draggedGo.setLocalPosition(worldPos.x, worldPos.y, worldPos.z)
                    draggedGo.setLocalRotation(worldRot.x, worldRot.y, worldRot.z, worldRot.w)
                    draggedGo.setLocalScale(worldScale.x, worldScale.y, worldScale.z)

                    context.currScene.sceneGraph.root.addChild(draggedGo)

                } else {
                    val parentGo = newParent.value

                    // Get the current world transform of the GameObject
                    val childWorldTransform = draggedGo.transform

                    // Get the inverse world transform of the new parent
                    val invParentWorldTransform = parentGo.transform.cpy().inv()

                    // Multiply to get the new local transform
                    val localTransform = invParentWorldTransform.mul(childWorldTransform)

                    // Extract the new local position, rotation, and scale
                    localTransform.getTranslation(tmpPos)
                    localTransform.getRotation(tmpQuat, true)
                    localTransform.getScale(tmpScale)

                    // Set the new local transform
                    draggedGo.setLocalPosition(tmpPos.x, tmpPos.y, tmpPos.z)
                    draggedGo.setLocalRotation(tmpQuat.x, tmpQuat.y, tmpQuat.z, tmpQuat.w)
                    draggedGo.setLocalScale(tmpScale.x, tmpScale.y, tmpScale.z)

                    // add
                    parentGo.addChild(draggedGo)
                }

                // update tree
                buildTree(projectManager.current().currScene.sceneGraph)
                postEvent(GameObjectModifiedEvent(draggedGo))
            }

        })
    }

    private fun setupListeners() {

        scrollPane.addListener(object : InputListener() {
            override fun enter(event: InputEvent?, x: Float, y: Float, pointer: Int, fromActor: Actor?) {
                UI.scrollFocus = scrollPane
            }

            override fun exit(event: InputEvent?, x: Float, y: Float, pointer: Int, toActor: Actor?) {
                UI.scrollFocus = null
            }

        })

        tree.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                if (tapCount != 2)
                    return

                val go = tree.getNodeAt(y)?.value

                if (go != null) {
                    val context = projectManager.current()
                    val pos = Vector3()
                    go.transform.getTranslation(pos)

                    // just lerp in the direction of the object if certain distance away
                    if (pos.dst(context.currScene.cam.position) > 100)
                        context.currScene.cam.position.lerp(pos.cpy().add(0f, 40f, 0f), 0.5f)

                    context.currScene.cam.lookAt(pos)
                    context.currScene.cam.up.set(Vector3.Y)
                }

            }

            override fun touchDown(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int): Boolean {
                if (Input.Buttons.LEFT != button) {
                    return true
                }
                return super.touchDown(event, x, y, pointer, button)
            }

            // right click menu listener
            override fun touchUp(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int) {
                if (Input.Buttons.RIGHT != button) {
                    super.touchUp(event, x, y, pointer, button)
                    return
                }

                val node = tree.getNodeAt(y)
                var go: GameObject? = null
                if (node != null) {
                    go = node.value
                }
                rightClickMenu.show(node, go, Gdx.input.x.toFloat(), (Gdx.graphics.height - Gdx.input.y).toFloat())
            }

        })

        // select listener
        tree.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                val selection = tree.getSelection()
                if (selection != null && selection.size() > 0) {
                    val go = selection.first().value
                    Mundus.postEvent(GameObjectSelectedEvent(go))
                }
            }
        })

    }

    /**
     * Building tree from game objects in sceneGraph, clearing previous
     * sceneGraph
     * @param sceneGraph
     */
    internal fun buildTree(sceneGraph: SceneGraph) {
        // Get all expanded nodes so their expanded state can be preserved on rebuild of tree
        val expandedObjects = Array<GameObject>()
        getExpandedNodes(tree.rootNodes, expandedObjects)

        tree.clearChildren()

        var containsWater = false

        for (go in sceneGraph.gameObjects) {
            addGoToTree(null, go, expandedObjects)

            if (containsWater) continue

            val waterComponents = go.findComponentsByType(Array(), Component.Type.WATER, true)
            if (waterComponents.notEmpty()) {
                containsWater = true
            }
        }

        sceneGraph.isContainsWater = containsWater

        // After tree is rebuilt, we must set the selected GO again if there is one
        if (projectManager.current().currScene.currentSelection != null) {
            val node = tree.findNode(projectManager.current().currScene.currentSelection)
            tree.selection.clear()
            if (node != null)
                tree.selection.add(node)
        }
    }

    /**
     * Get all Outline nodes that are currently expanded and add to list
     */
    private fun getExpandedNodes(nodes: Array<OutlineNode>?, expandedObjects: Array<GameObject>) {
        if (nodes == null || nodes.isEmpty) return

        for (node in nodes) {
            if (node.isExpanded) {
                expandedObjects.add(node.value)
            }
            getExpandedNodes(node.children, expandedObjects)
        }
    }

    /**
     * Adding game object to outline

     * @param treeParentNode
     * *
     * @param gameObject
     */
    private fun addGoToTree(
        treeParentNode: Tree.Node<OutlineNode, GameObject, NodeTable>?,
        gameObject: GameObject,
        expandedList: Array<GameObject>?
    ) {
        val leaf = OutlineNode(NodeTable(gameObject), gameObject)
        if (treeParentNode == null) {
            tree.add(leaf)
        } else {
            treeParentNode.add(leaf)
        }
        // Only expand object if it's in the expanded list
        if (expandedList != null && expandedList.contains(gameObject, true)) {
            leaf.expandAll()
        }
        if (gameObject.children != null) {
            for (goChild in gameObject.children) {
                addGoToTree(leaf, goChild, expandedList)
            }
        }
    }

    internal fun addGoToTree(treeParentNode: Tree.Node<OutlineNode, GameObject, NodeTable>?, gameObject: GameObject) {
        addGoToTree(treeParentNode, gameObject, null)
    }

    /**
     * Removing game object from tree and outline

     * @param go
     */
    internal fun removeGo(go: GameObject) {
        // run delete command, updating sceneGraph and outline
        val deleteCommand = DeleteCommand(go, tree.findNode(go))
        history.add(deleteCommand)
        deleteCommand.execute() // run delete
    }

    /**
     * Deep copy of all game objects

     * @param go
     * *            the game object for cloning, with children
     * *
     * @param parent
     * *            game object on which clone will be added
     */
    internal fun duplicateGO(go: GameObject, parent: GameObject) {
        Log.trace(TAG, "Duplicate [{}] with parent [{}]", go, parent)
        val goCopy = GameObject(go, projectManager.current().obtainID())

        // Handle duplicated light components
        val lightComponent: LightComponent? = goCopy.findComponentByType(Component.Type.LIGHT)
        if (lightComponent != null) {
            // Remove the duplicated light component
            goCopy.removeComponent(lightComponent)
            lightComponent.remove()

            // This is a bit of a workaround, since we are in editor here, we replace the duplicated lightComponent
            // with a pickable version instead.
            val pickableLightComponent = PickableLightComponent(goCopy, lightComponent.lightType)
            LightUtils.copyLightSettings(lightComponent.light, pickableLightComponent.light)

            goCopy.addComponent(pickableLightComponent)
            Mundus.postEvent(ComponentAddedEvent(pickableLightComponent))
        }

        // add copy to tree
        val n = tree.findNode(parent)
        addGoToTree(n, goCopy)

        // add copy to scene graph
        parent.addChild(goCopy)

        // recursively clone child objects
        if (go.children != null) {
            for (child in go.children) {
                duplicateGO(child, goCopy)
            }
        }
    }

    override fun onGameObjectSelected(event: GameObjectSelectedEvent) {
        val node = tree.findNode(event.gameObject!!)
        Log.trace(TAG, "Select game object [{}].", node.value)
        tree.selection.clear()
        tree.selection.add(node)
        node.expandTo()
    }

    override fun onAssetSelected(event: AssetSelectedEvent) {
        clearSelection()
    }

    /**
     * TODO
     */
    inner class NodeTable(go: GameObject) : VisTable() {

        val nameLabel: VisLabel = VisLabel()
        private val childCountLabel: VisLabel = VisLabel()

        init {
            add(nameLabel).expand().fill()
            nameLabel.setText(go.name)
            if (go.children != null && !go.children.isEmpty) {
                add(childCountLabel).padLeft(4f)
                childCountLabel.setText("(${go.children.size})")
                childCountLabel.color = if (go.active) Colors.TEAL_LIGHT else Colors.GRAY_888
            }
            if (!go.active) nameLabel.color = Colors.GRAY_888
        }
    }

    inner class OutlineNode(table: NodeTable, gameObject: GameObject) :
        Tree.Node<OutlineNode, GameObject, NodeTable>(table) {

        init {
            value = gameObject
        }
    }

}
