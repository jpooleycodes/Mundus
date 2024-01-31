package com.mbrlabs.mundus.editor.ui.modules.outline

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Array
import com.kotcrab.vis.ui.util.dialog.Dialogs
import com.kotcrab.vis.ui.util.dialog.InputDialogAdapter
import com.kotcrab.vis.ui.widget.MenuItem
import com.kotcrab.vis.ui.widget.PopupMenu
import com.mbrlabs.mundus.commons.assets.ModelAsset
import com.mbrlabs.mundus.commons.assets.meta.MetaModel
import com.mbrlabs.mundus.commons.scene3d.GameObject
import com.mbrlabs.mundus.commons.scene3d.SceneGraph
import com.mbrlabs.mundus.commons.scene3d.components.Component
import com.mbrlabs.mundus.commons.scene3d.components.TerrainComponent
import com.mbrlabs.mundus.commons.scene3d.components.WaterComponent
import com.mbrlabs.mundus.commons.utils.Pools
import com.mbrlabs.mundus.editor.Mundus
import com.mbrlabs.mundus.editor.assets.MetaSaver
import com.mbrlabs.mundus.editor.assets.ModelImporter
import com.mbrlabs.mundus.editor.core.project.ProjectManager
import com.mbrlabs.mundus.editor.events.AssetImportEvent
import com.mbrlabs.mundus.editor.events.GameObjectSelectedEvent
import com.mbrlabs.mundus.editor.events.SceneGraphChangedEvent
import com.mbrlabs.mundus.editor.events.TerrainRemovedEvent
import com.mbrlabs.mundus.editor.history.CommandHistory
import com.mbrlabs.mundus.editor.history.commands.MultiCommand
import com.mbrlabs.mundus.editor.history.commands.RotateCommand
import com.mbrlabs.mundus.editor.history.commands.TranslateCommand
import com.mbrlabs.mundus.editor.history.commands.GameObjectActiveCommand
import com.mbrlabs.mundus.editor.history.commands.SortChildrenCommand
import com.mbrlabs.mundus.editor.scene3d.components.PickableModelComponent
import com.mbrlabs.mundus.editor.tools.ToolManager
import com.mbrlabs.mundus.editor.ui.UI
import com.mbrlabs.mundus.editor.utils.Log
import com.mbrlabs.mundus.editor.utils.UsefulMeshs
import net.mgsx.gltf.scene3d.attributes.PBRColorAttribute
import net.mgsx.gltf.scene3d.attributes.PBRFloatAttribute

/**
 * Holds code for Outlines right click menu. Separated from Outline class
 * due to its complexity
 *
 * @author JamesTKhan
 * @version August 03, 2022
 */
class OutlineRightClickMenu(outline: Outline) : PopupMenu() {
    companion object {
        private val TAG = OutlineRightClickMenu::class.java.simpleName
    }

    private val outline: Outline
    private val projectManager: ProjectManager = Mundus.inject()
    private val toolManager: ToolManager = Mundus.inject()
    private val modelImporter: ModelImporter = Mundus.inject()
    private val history: CommandHistory = Mundus.inject()


    init {
        this.outline = outline
    }

    //region Main menu
    private val duplicate: MenuItem = MenuItem("Duplicate")
    private val rename: MenuItem = MenuItem("Rename")
    private val delete: MenuItem = MenuItem("Delete")
    private val moveMenuItem: MenuItem = MenuItem("Move")
    private val moveMenu: MoveSubMenu = MoveSubMenu()
    private val addMenuItem: MenuItem = MenuItem("Add")
    private val addMenu: AddSubMenu = AddSubMenu()
    private val actionsMenuItem: MenuItem = MenuItem("Actions")
    private val actionsMenu: ActionSubMenu = ActionSubMenu()

    private var selectedGO: GameObject? = null
    private var currentNode: Outline.OutlineNode? = null

    init {
        moveMenuItem.subMenu = moveMenu
        addMenuItem.subMenu = addMenu
        actionsMenuItem.subMenu = actionsMenu

        rename.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                if (selectedGO != null) {
                    showRenameDialog()
                }
            }
        })

        // duplicate node
        duplicate.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                if (selectedGO != null && !duplicate.isDisabled) {
                    outline.duplicateGO(selectedGO!!, selectedGO!!.parent)
                    Mundus.postEvent(SceneGraphChangedEvent())
                }
            }
        })

        // delete game object
        delete.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                if (selectedGO != null) {
                    outline.removeGo(selectedGO!!)
                    if (toolManager.isSelected(selectedGO!!)) {
                        toolManager.setDefaultTool()
                    }

                    val terrainComponent: TerrainComponent? = selectedGO!!.findComponentByType(Component.Type.TERRAIN)
                    if (terrainComponent != null) {
                        projectManager.current().currScene.terrains.removeValue(terrainComponent, true)
                        Mundus.postEvent(TerrainRemovedEvent(terrainComponent))
                    }
                }
            }
        })

        addItem(moveMenuItem)
        addItem(addMenuItem)
        addItem(actionsMenuItem)
        addItem(rename)
        addItem(duplicate)
        addItem(delete)
    }

    /**
     * Right click event opens menu and enables more options if selected
     * game object is active.
     *
     * @param go
     * @param x
     * @param y
     */
    fun show(node: Outline.OutlineNode?, go: GameObject?, x: Float, y: Float) {
        selectedGO = go
        currentNode = node
        showMenu(UI, x, y)
        moveMenu.show()
        actionsMenu.show()

        // check if game object is selected
        if (selectedGO != null) {
            // Activate menu options for selected game objects
            rename.isDisabled = false
            delete.isDisabled = false
        } else {
            // disable MenuItems which only works with selected item
            rename.isDisabled = true
            delete.isDisabled = true
        }


        // some assets can not be duplicated
        duplicate.isDisabled = selectedGO == null
                || selectedGO!!.findComponentByType<TerrainComponent?>(Component.Type.TERRAIN) != null
                || selectedGO!!.findComponentByType<WaterComponent?>(Component.Type.WATER) != null
    }

    fun showRenameDialog() {
        val node = outline.tree.findNode(selectedGO!!)
        val goNode = node.actor as Outline.NodeTable

        val renameDialog = Dialogs.showInputDialog(
            UI, "Rename", "",
            object : InputDialogAdapter() {
                override fun finished(input: String?) {
                    Log.trace(TAG, "Rename game object [{}] to [{}].", selectedGO, input)
                    // update sceneGraph
                    selectedGO!!.name = input
                    // update Outline
                    //goNode.name.setText(input + " [" + selectedGO.id + "]");
                    goNode.nameLabel.setText(input)

                    Mundus.postEvent(SceneGraphChangedEvent())
                    Mundus.postEvent(GameObjectSelectedEvent(selectedGO!!))
                }
            })
        // set position of dialog to menuItem position
        val nodePosX = node.actor.x
        val nodePosY = node.actor.y
        renameDialog.setPosition(nodePosX, nodePosY)
        renameDialog.setText(selectedGO.toString())
    }
    //endregion Main Menu

    //region Move Sub Menu
    /**
     * A submenu to allow moving nodes up and down within the outline tree
     */
    private inner class MoveSubMenu : PopupMenu() {
        private val moveUp: MenuItem = MenuItem("Move Up")
        private val moveDown: MenuItem = MenuItem("Move Down")
        private val moveToTop: MenuItem = MenuItem("Move To Top")
        private val moveToBottom: MenuItem = MenuItem("Move To Bottom")

        init {
            addItem(moveUp)
            addItem(moveDown)
            addItem(moveToTop)
            addItem(moveToBottom)

            moveUp.addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    val parentChildArray = getParentChildArray()
                    val currentNodeIndex = parentChildArray.indexOf(currentNode!!.value)
                    // If the current Node is NOT the first one...
                    if (currentNodeIndex > 0) {
                        parentChildArray.swap(currentNodeIndex, currentNodeIndex - 1)
                        updateChildren()
                    }
                }
            })

            moveDown.addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    val parentChildArray = getParentChildArray()
                    val currentNodeIndex = parentChildArray.indexOf(currentNode!!.value)
                    // If the current Node is NOT the last one...
                    if (currentNodeIndex < parentChildArray.size - 1) {
                        parentChildArray.swap(currentNodeIndex, currentNodeIndex + 1)
                        updateChildren()
                    }
                }
            })

            moveToTop.addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    val parentChildArray = getParentChildArray()
                    val currentNodeIndex = parentChildArray.indexOf(currentNode!!.value)

                    // If already at top, return
                    if (currentNodeIndex == 0) return

                    parentChildArray.removeValue(currentNode!!.value, true)
                    parentChildArray.insert(0, currentNode!!.value)
                    updateChildren()
                }
            })

            moveToBottom.addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    val parentChildArray = getParentChildArray()
                    val currentNodeIndex = parentChildArray.indexOf(currentNode!!.value)

                    // If already at bottom, return
                    if (currentNodeIndex == parentChildArray.size - 1) return

                    parentChildArray.removeValue(currentNode!!.value, true)
                    parentChildArray.insert(parentChildArray.size, currentNode!!.value)
                    updateChildren()
                }
            })
        }

        fun show() {
            moveMenuItem.isDisabled = currentNode == null
            if (currentNode == null) return

            val parentChildArray: Array<GameObject> = getParentChildArray()

            // Disable move menu items depending on nodes current index
            val nodeIndex = parentChildArray.indexOf(currentNode!!.value, true)
            moveUp.isDisabled = nodeIndex == 0
            moveToTop.isDisabled = nodeIndex == 0
            moveDown.isDisabled = nodeIndex == parentChildArray.size - 1
            moveToBottom.isDisabled = nodeIndex == parentChildArray.size - 1
        }

        /**
         * Get the parents child array of current node, for root nodes .parent is null
         */
        fun getParentChildArray(): Array<GameObject> {
            return currentNode!!.value.parent.children
        }

        private fun updateChildren() {
            outline.buildTree(currentNode!!.value.sceneGraph)
        }
    }
    //endregion Move Sub Menu

    //region Add Sub Menu
    /**
     * A submenu to allow to add GameObjects to the scene
     */
    private inner class AddSubMenu : PopupMenu() {
        private val addEmpty: MenuItem = MenuItem("Add Empty")
        private val addTerrain: MenuItem = MenuItem("Add Terrain")
        private val addWater: MenuItem = MenuItem("Add Water")
        private val addPlane: MenuItem = MenuItem("Add Plane")
        private val addCube: MenuItem = MenuItem("Add Cube")

        init {
            addItem(addEmpty)
            addItem(addTerrain)
            addItem(addWater)
            addItem(addPlane)
            addItem(addCube)

            // add empty
            addEmpty.addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    val sceneGraph = projectManager.current().currScene.sceneGraph
                    val go = createGameObject(sceneGraph)

                    // update outline
                    updateOutline(sceneGraph, go)
                }
            })

            addPlane.addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    val fileName = "standard_plane.gltf"
                    val assetManager = projectManager.current().assetManager
                    var modelAsset = assetManager.findAssetByFileName(fileName) as ModelAsset?

                    val sceneGraph = projectManager.current().currScene.sceneGraph
                    val go = createGameObject(sceneGraph)

                    if (modelAsset == null) {
                        // Create new material
                        val material = Material("plane_material")
                        setDefaultValues(material)
                        modelAsset = createModelAsset(fileName, UsefulMeshs.createPlane(material, 5f))
                    }

                    // Create model component
                    val modelComponent = PickableModelComponent(go)

                    // Set model and add to game object
                    modelComponent.setModel(modelAsset, true)
                    go.addComponent(modelComponent)
                    modelComponent.encodeRaypickColorId()

                    Mundus.postEvent(AssetImportEvent(modelAsset))

                    // update outline
                    updateOutline(sceneGraph, go)
                }
            })

            addCube.addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    val fileName = "standard_cube.gltf"
                    val assetManager = projectManager.current().assetManager
                    var modelAsset = assetManager.findAssetByFileName(fileName) as ModelAsset?

                    val sceneGraph = projectManager.current().currScene.sceneGraph
                    val go = createGameObject(sceneGraph)

                    if (modelAsset == null) {
                        // Create new material
                        val material = Material("cube_material")
                        setDefaultValues(material)
                        modelAsset = createModelAsset(fileName, UsefulMeshs.createCube(material, 5f))
                    }

                    // Create model component
                    val modelComponent = PickableModelComponent(go)

                    // Set model and add to game object
                    modelComponent.setModel(modelAsset, true)
                    go.addComponent(modelComponent)
                    modelComponent.encodeRaypickColorId()

                    Mundus.postEvent(AssetImportEvent(modelAsset!!))

                    // update outline
                    updateOutline(sceneGraph, go)
                }
            })

            addTerrain.addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    UI.showDialog(UI.addTerrainChunksDialog)
                }
            })

            // add waterAsset
            addWater.addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    UI.addWaterDialog.show(selectedGO)
                }
            })
        }

        private fun createModelAsset(fileName: String, model: Model): ModelAsset {
            val assetManager = projectManager.current().assetManager
            val modelAsset = assetManager.createModelAsset(fileName, model)
            modelAsset.meta.model = MetaModel()

            for (mat in modelAsset.model.materials) {
                val materialAsset = assetManager.createMaterialAsset(modelAsset.id.substring(0, 4) + "_" + mat.id)

                modelImporter.populateMaterialAsset(null, projectManager.current().assetManager, mat, materialAsset)
                projectManager.current().assetManager.saveMaterialAsset(materialAsset)

                modelAsset.meta.model.defaultMaterials.put(mat.id, materialAsset.id)
                modelAsset.defaultMaterials.put(mat.id, materialAsset)
            }

            // save meta file
            val saver = MetaSaver()
            saver.save(modelAsset.meta)

            modelAsset.applyDependencies()
            return modelAsset
        }

        private fun setDefaultValues(material: Material) {
            material.set(PBRColorAttribute.createBaseColorFactor(Color.GRAY))
            material.set(PBRFloatAttribute.createMetallic(0f))
            material.set(PBRFloatAttribute.createRoughness(1.0f))
            material.set(IntAttribute.createCullFace(GL20.GL_BACK))
        }

        private fun updateOutline(sceneGraph: SceneGraph, go: GameObject) {
            // update outline
            if (selectedGO == null) {
                // update sceneGraph
                Log.trace(TAG, "Add empty game object [{}] in root node.", go)
                sceneGraph.addGameObject(go)
                // update outline
                outline.addGoToTree(null, go)
            } else {
                Log.trace(TAG, "Add empty game object [{}] child in node [{}].", go, selectedGO)
                // update sceneGraph
                selectedGO!!.addChild(go)
                // update outline
                val n = outline.tree.findNode(selectedGO!!)
                outline.addGoToTree(n, go)
            }
            Mundus.postEvent(SceneGraphChangedEvent())
        }

        fun createGameObject(sceneGraph: SceneGraph): GameObject {
            val id = projectManager.current().obtainID()
            // the new game object
            return GameObject(sceneGraph, GameObject.DEFAULT_NAME, id)
        }

    }
    //endregion Add Sub Menu

    //region Action Sub Menu
    /**
     * A submenu to allow adding GameObjects to the scene
     */
    private inner class ActionSubMenu : PopupMenu() {
        private val toggleActive = MenuItem("Toggle active")
        private val alignCameraToObject = MenuItem("Align Camera to Object")
        private val alignObjectToCamera = MenuItem("Align Object to Camera")
        private val sortChildren = MenuItem("Sort children")

        init {
            addItem(toggleActive)
            addItem(alignCameraToObject)
            addItem(alignObjectToCamera)
            addItem(sortChildren)

            toggleActive.addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    val command = GameObjectActiveCommand(currentNode!!.value, !currentNode!!.value.active)
                    command.execute()
                    history.add(command)
                }
            })

            alignCameraToObject.addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    val camera = projectManager.current().currScene.cam
                    val go = currentNode!!.value

                    val pos = Pools.vector3Pool.obtain()
                    val dir = Pools.vector3Pool.obtain()

                    go.transform.getTranslation(pos)
                    go.getForwardDirection(dir)

                    camera.position.set(pos)
                    camera.direction.set(dir)
                    camera.update()

                    Pools.vector3Pool.free(pos)
                    Pools.vector3Pool.free(dir)
                }
            })

            alignObjectToCamera.addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    val camera = projectManager.current().currScene.cam
                    val go = currentNode!!.value

                    val pos = Pools.vector3Pool.obtain()
                    val dir = Pools.vector3Pool.obtain()
                    val up = Pools.vector3Pool.obtain()
                    val right = Pools.vector3Pool.obtain()
                    val q = Pools.quaternionPool.obtain()
                    val q2 = Pools.quaternionPool.obtain()
                    val m = Pools.matrix4Pool.obtain()

                    // Use commands for undo/redo
                    val translateCommand = TranslateCommand(go)
                    val rotateCommand = RotateCommand(go)
                    val multiCommand = MultiCommand()
                    multiCommand.addCommand(translateCommand)
                    multiCommand.addCommand(rotateCommand)

                    // set before values
                    go.getLocalPosition(pos)
                    translateCommand.setBefore(pos)
                    go.getLocalRotation(q)
                    rotateCommand.setBefore(q)

                    // set camera values for calculations
                    pos.set(camera.position)
                    dir.set(camera.direction)
                    right.set(camera.direction).crs(camera.up).nor()

                    // set 'up' to ensure it's orthogonal to dir and right
                    up.set(right).crs(dir).nor()
                    right.scl(-1f)

                    // Convert camera world pos to local space
                    m.set(go.parent.transform).inv()
                    val localPos = pos.mul(m)
                    go.setLocalPosition(localPos.x, localPos.y, localPos.z)

                    // Calc camera's world rotation
                    val cameraWorldRot =
                        q2.setFromAxes(right.x, up.x, dir.x, right.y, up.y, dir.y, right.z, up.z, dir.z)

                    // Calc parent's world rotation
                    val parentWorldRot = go.parent.getRotation(q)

                    // Calc object's local rotation
                    val localRot = parentWorldRot.conjugate().mul(cameraWorldRot)
                    go.setLocalRotation(localRot.x, localRot.y, localRot.z, localRot.w)

                    // set after values for commands
                    translateCommand.setAfter(localPos)
                    rotateCommand.setAfter(localRot)

                    history.add(multiCommand)

                    Pools.vector3Pool.free(pos)
                    Pools.vector3Pool.free(dir)
                    Pools.vector3Pool.free(up)
                    Pools.vector3Pool.free(right)
                    Pools.quaternionPool.free(q)
                    Pools.quaternionPool.free(q2)
                    Pools.matrix4Pool.free(m)
                }
            })

            sortChildren.addListener(object: ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    val childArray = getChildArray()

                    val command = SortChildrenCommand(childArray)
                    command.execute()

                    history.add(command)
                }
            })
        }

        fun show() {
            if (currentNode == null) {
                actionsMenuItem.isDisabled = true
                return
            } else {
                actionsMenuItem.isDisabled = false
            }

            if (currentNode!!.value.active) {
                toggleActive.text = "Deactivate"
            } else {
                toggleActive.text = "Activate"
            }

            sortChildren.isDisabled = getChildrenNum() == 0
        }

        fun getChildrenNum(): Int = currentNode!!.value.children?.size ?: 0

        fun getChildArray(): Array<GameObject> {
            return currentNode!!.value.children
        }

    }
    //endregion Action Sub Menu

}
