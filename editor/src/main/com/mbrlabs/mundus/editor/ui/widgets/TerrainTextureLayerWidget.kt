/*
 * Copyright (c) 2023. See AUTHORS file.
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

package com.mbrlabs.mundus.editor.ui.widgets

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.util.dialog.Dialogs
import com.kotcrab.vis.ui.util.dialog.InputDialogAdapter
import com.kotcrab.vis.ui.widget.MenuItem
import com.kotcrab.vis.ui.widget.PopupMenu
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisTextButton
import com.mbrlabs.mundus.commons.assets.Asset
import com.mbrlabs.mundus.commons.assets.TerrainLayerAsset
import com.mbrlabs.mundus.commons.assets.TextureAsset
import com.mbrlabs.mundus.commons.scene3d.components.Component
import com.mbrlabs.mundus.commons.scene3d.components.TerrainComponent
import com.mbrlabs.mundus.commons.terrain.SplatTexture
import com.mbrlabs.mundus.editor.Mundus
import com.mbrlabs.mundus.editor.assets.AssetAlreadyExistsException
import com.mbrlabs.mundus.editor.assets.AssetTerrainLayerFilter
import com.mbrlabs.mundus.editor.assets.AssetTextureFilter
import com.mbrlabs.mundus.editor.core.project.ProjectManager
import com.mbrlabs.mundus.editor.events.AssetImportEvent
import com.mbrlabs.mundus.editor.events.AssetSelectedEvent
import com.mbrlabs.mundus.editor.tools.brushes.TerrainBrush
import com.mbrlabs.mundus.editor.ui.UI
import com.mbrlabs.mundus.editor.ui.modules.dialogs.assets.AssetPickerDialog
import com.mbrlabs.mundus.editor.utils.Colors
import java.io.FileNotFoundException
import java.io.IOException

/**
 * Widget for displaying and updating a TerrainLayer via a TextureGrid
 * @author JamesTKhan
 * @version July 12, 2023
 */
class TerrainTextureLayerWidget(var asset: TerrainLayerAsset, var allowEditAndChange: Boolean = true) : VisTable() {

    interface LayerChangedListener {
        fun layerChanged(terrainLayerAsset: TerrainLayerAsset)
    }

    interface LayerUpdatedListener {
        fun layerUpdated(terrainLayerAsset: TerrainLayerAsset)
    }

    /**
     * An optional listener for when the layer is updated (texture removed, added, etc..).
     */
    var layerUpdatedListener: LayerUpdatedListener? = null

    /**
     * An optional listener for when the layer is changed.
     */
    var layerChangedListener: LayerChangedListener? = null

    private val filter: AssetTerrainLayerFilter = AssetTerrainLayerFilter()
    private lateinit var assetPickerListener: AssetPickerDialog.AssetPickerListener

    private val addTextureBtn = VisTextButton("Add Texture")
    internal val textureGrid = TextureGrid<SplatTexture>(40, 5)

    private val layerNameLabel: VisLabel = VisLabel()
    private val editBtn: VisTextButton = VisTextButton("Edit")
    private val duplicatedBtn: VisTextButton = VisTextButton("Duplicate")
    private val changedBtn: VisTextButton = VisTextButton("Change")

    private val projectManager: ProjectManager = Mundus.inject()
    private val root = VisTable()
    private val rightClickMenu = TextureRightClickMenu()

    init {
        layerNameLabel.color = Colors.TEAL
        layerNameLabel.wrap = true

        val description = "Terrain layers determine what textures a terrain uses.\n" +
                "They can be shared with multiple terrains.\n" +
                "Changing a texture here will update all terrains using the layer."
        val descLabel = ToolTipLabel("Terrain Layer", description)
        root.add(descLabel).expandX().fillX().row()
        root.addSeparator()

        val layerTable = VisTable()
        layerTable.add(layerNameLabel).grow()
        if (allowEditAndChange)
            layerTable.add(editBtn).padLeft(4f).right()
        layerTable.add(duplicatedBtn).padLeft(4f).right()
        if (allowEditAndChange)
            layerTable.add(changedBtn).padLeft(4f).right().row()
        root.add(layerTable).expandX().fillX().row()

        layerNameLabel.setText(asset.name)

        // textures
        root.add(VisLabel("Textures:")).padLeft(5f).left().row()
        textureGrid.background = VisUI.getSkin().getDrawable("menu-bg")
        root.add(textureGrid).expand().fill().pad(5f).row()

        // add texture
        root.add(addTextureBtn).padRight(5f).right().row()
        add(root).expand().fill()

        setupListeners()
        setupTextureGrid()
    }

    private fun setupListeners() {
        if (allowEditAndChange) {
            assetPickerListener = object : AssetPickerDialog.AssetPickerListener {
                override fun onSelected(asset: Asset?) {
                    val layer = (asset as? TerrainLayerAsset)!!

                    val oldLayerCount = this@TerrainTextureLayerWidget.asset.activeLayerCount
                    val newLayerCount = layer.activeLayerCount

                    if (oldLayerCount > newLayerCount) {
                        Dialogs.showConfirmDialog(
                            UI,
                            "Change Layer",
                            "The new layer has less texture channels assigned than the current one. Splat map paint data for the exceeding channels will be lost." +
                                    "\nDo you want to continue?",
                            arrayOf("Cancel", "Yes"),
                            arrayOf(0, 1)
                        ) { r: Int ->
                            if (r == 1) {
                                setTerrainLayerAsset(layer)
                            }
                        }.padBottom(20f).pack()
                    } else {
                        setTerrainLayerAsset(layer)
                    }
                }
            }

            changedBtn.addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    UI.assetSelectionDialog.show(false, filter, assetPickerListener)
                }
            })
        }

        addTextureBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                UI.assetSelectionDialog.show(
                    false,
                    AssetTextureFilter(),
                    object : AssetPickerDialog.AssetPickerListener {
                        override fun onSelected(asset: Asset?) {
                            try {
                                addTexture(asset as TextureAsset)
                            } catch (e: IOException) {
                                e.printStackTrace()
                                UI.toaster.error("Error while creating the splatmap.")
                            }
                        }
                    })
            }
        })

        duplicatedBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                Dialogs.showInputDialog(UI, "Name:", "", object : InputDialogAdapter() {
                    override fun finished(input: String?) {
                        if (input != null) {
                            try {
                                val newLayer = projectManager.current().assetManager.createTerrainLayerAsset(input)
                                newLayer.duplicateLayerAsset(asset)
                                Mundus.postEvent(AssetImportEvent(asset))
                                if (allowEditAndChange) {
                                    asset = newLayer
                                    layerNameLabel.setText(asset.name)
                                    layerChangedListener?.layerChanged(asset)
                                    setTexturesInUiGrid()
                                }
                            } catch (e: AssetAlreadyExistsException) {
                                Dialogs.showErrorDialog(UI, "That asset already exists. Try a different name.")
                            } catch (e: FileNotFoundException) {
                                Dialogs.showErrorDialog(UI, "Invalid asset name.")
                            }
                        }
                    }
                })
            }
        })

        if (allowEditAndChange) {
            editBtn.addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    Mundus.postEvent(AssetSelectedEvent(asset))
                    UI.docker.assetsDock.setSelected(asset)
                }
            })
        }
    }

    private fun setupTextureGrid() {
        textureGrid.setListener { texture, leftClick ->
            val tex = texture as SplatTexture
            if (leftClick) {
                TerrainBrush.setPaintChannel(tex.channel)
            } else {
                rightClickMenu.setChannel(tex.channel)
                rightClickMenu.show()
            }
        }

        setTexturesInUiGrid()
    }

    private fun setTexturesInUiGrid() {
        textureGrid.removeTextures()
        if (asset.splatBase != null) {
            textureGrid.addTexture(SplatTexture(SplatTexture.Channel.BASE, asset.splatBase))
        }
        if (asset.splatR != null) {
            textureGrid.addTexture(SplatTexture(SplatTexture.Channel.R, asset.splatR))
        }
        if (asset.splatG != null) {
            textureGrid.addTexture(SplatTexture(SplatTexture.Channel.G, asset.splatG))
        }
        if (asset.splatB != null) {
            textureGrid.addTexture(SplatTexture(SplatTexture.Channel.B, asset.splatB))
        }
        if (asset.splatA != null) {
            textureGrid.addTexture(SplatTexture(SplatTexture.Channel.A, asset.splatA))
        }
    }

    @Throws(IOException::class)
    private fun addTexture(textureAsset: TextureAsset) {
        val assetManager = projectManager.current().assetManager

        val terrainLayerAsset = asset
        assetManager.addModifiedAsset(terrainLayerAsset)

        // channel base
        if (terrainLayerAsset.splatBase == null) {
            terrainLayerAsset.splatBase = textureAsset
            applyDependencies()
            textureGrid.addTexture(SplatTexture(SplatTexture.Channel.BASE, terrainLayerAsset.splatBase))
            return
        }

        // create splatmaps
        createSplatmapsIfNotExists()

        // channel r
        if (terrainLayerAsset.splatR == null) {
            terrainLayerAsset.splatR = textureAsset
            applyDependencies()
            textureGrid.addTexture(SplatTexture(SplatTexture.Channel.R, terrainLayerAsset.splatR))
            return
        }

        // channel g
        if (terrainLayerAsset.splatG == null) {
            terrainLayerAsset.splatG = textureAsset
            applyDependencies()
            textureGrid.addTexture(SplatTexture(SplatTexture.Channel.G, terrainLayerAsset.splatG))
            return
        }

        // channel b
        if (terrainLayerAsset.splatB == null) {
            terrainLayerAsset.splatB = textureAsset
            applyDependencies()
            textureGrid.addTexture(SplatTexture(SplatTexture.Channel.B, terrainLayerAsset.splatB))
            return
        }

        // channel a
        if (terrainLayerAsset.splatA == null) {
            terrainLayerAsset.splatA = textureAsset
            applyDependencies()
            textureGrid.addTexture(SplatTexture(SplatTexture.Channel.A, terrainLayerAsset.splatA))
            return
        }

        Dialogs.showErrorDialog(UI, "Not more than 5 textures per terrainAsset please :)")
    }

    private fun applyDependencies() {
        projectManager.current().currScene.sceneGraph.findAllByComponent(Component.Type.TERRAIN).forEach {
            val component = it.findComponentByType(Component.Type.TERRAIN) as TerrainComponent

            if (component.terrainAsset.terrainLayerAsset == asset) {
                component.terrainAsset.updateTerrainMaterial()
                component.applyMaterial()
                projectManager.current().assetManager.addModifiedAsset(component.terrainAsset)
            }
        }
    }

    /**
     * Creates the splatmaps for each terrainAsset using this layer
     * if they don't exist yet.
     */
    private fun createSplatmapsIfNotExists() {
        projectManager.current().currScene.sceneGraph.findAllByComponent(Component.Type.TERRAIN).forEach {
            val component = it.findComponentByType(Component.Type.TERRAIN) as TerrainComponent

            if (component.terrainAsset.terrainLayerAsset == asset) {
                projectManager.current().assetManager.createSplatmapForTerrain(component)
            }
        }

    }

    fun setTerrainLayerAsset(layerAsset: TerrainLayerAsset) {
        layerNameLabel.setText(layerAsset.name)
        layerChangedListener?.layerChanged(layerAsset)
        this@TerrainTextureLayerWidget.asset = layerAsset
        setupTextureGrid()
    }

    /**

     */
    private inner class TextureRightClickMenu : PopupMenu() {

        private val removeTexture = MenuItem("Remove texture")
        private val changeTexture = MenuItem("Change texture")
        private val addNormalMap = MenuItem("Add Normal Map")
        private val removeNormalMap = MenuItem("Remove Normal Map")

        private var channel: SplatTexture.Channel? = null

        init {
            addItem(removeTexture)
            addItem(changeTexture)
            addItem(addNormalMap)

            removeTexture.addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    if (channel != null) {
                        val terrainLayerAsset = asset
                        if (channel == SplatTexture.Channel.R) {
                            terrainLayerAsset.splatR = null
                            terrainLayerAsset.splatRNormal = null
                        } else if (channel == SplatTexture.Channel.G) {
                            terrainLayerAsset.splatG = null
                            terrainLayerAsset.splatGNormal = null
                        } else if (channel == SplatTexture.Channel.B) {
                            terrainLayerAsset.splatB = null
                            terrainLayerAsset.splatBNormal = null
                        } else if (channel == SplatTexture.Channel.A) {
                            terrainLayerAsset.splatA = null
                            terrainLayerAsset.splatANormal = null
                        } else {
                            UI.toaster.error("Can't remove the base texture")
                            return
                        }

                        applyDependencies()
                        setTexturesInUiGrid()
                        layerUpdatedListener?.layerUpdated(terrainLayerAsset)
                        projectManager.current().assetManager.addModifiedAsset(terrainLayerAsset)
                    }
                }
            })

            changeTexture.addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    if (channel != null) {

                        UI.assetSelectionDialog.show(
                            false,
                            AssetTextureFilter(),
                            object : AssetPickerDialog.AssetPickerListener {
                                override fun onSelected(asset: Asset?) {
                                    if (channel != null) {
                                        val terrainLayerAsset = this@TerrainTextureLayerWidget.asset
                                        if (channel == SplatTexture.Channel.BASE) {
                                            terrainLayerAsset.splatBase = asset as TextureAsset
                                        } else if (channel == SplatTexture.Channel.R) {
                                            terrainLayerAsset.splatR = asset as TextureAsset
                                        } else if (channel == SplatTexture.Channel.G) {
                                            terrainLayerAsset.splatG = asset as TextureAsset
                                        } else if (channel == SplatTexture.Channel.B) {
                                            terrainLayerAsset.splatB = asset as TextureAsset
                                        } else if (channel == SplatTexture.Channel.A) {
                                            terrainLayerAsset.splatA = asset as TextureAsset
                                        }
                                        applyDependencies()
                                        setTexturesInUiGrid()
                                        layerUpdatedListener?.layerUpdated(terrainLayerAsset)
                                        projectManager.current().assetManager.addModifiedAsset(terrainLayerAsset)
                                    }
                                }
                            })

                    }
                }
            })

            addNormalMap.addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    if (channel != null) {

                        UI.assetSelectionDialog.show(
                            false,
                            AssetTextureFilter(),
                            object : AssetPickerDialog.AssetPickerListener {
                                override fun onSelected(asset: Asset?) {
                                    if (channel != null) {
                                        val terrainLayerAsset = this@TerrainTextureLayerWidget.asset
                                        if (channel == SplatTexture.Channel.BASE) {
                                            terrainLayerAsset.splatBaseNormal = asset as TextureAsset
                                        } else if (channel == SplatTexture.Channel.R) {
                                            terrainLayerAsset.splatRNormal = asset as TextureAsset
                                        } else if (channel == SplatTexture.Channel.G) {
                                            terrainLayerAsset.splatGNormal = asset as TextureAsset
                                        } else if (channel == SplatTexture.Channel.B) {
                                            terrainLayerAsset.splatBNormal = asset as TextureAsset
                                        } else if (channel == SplatTexture.Channel.A) {
                                            terrainLayerAsset.splatANormal = asset as TextureAsset
                                        }

                                        applyDependencies()
                                        layerUpdatedListener?.layerUpdated(terrainLayerAsset)
                                        projectManager.current().assetManager.addModifiedAsset(terrainLayerAsset)
                                    }
                                }
                            })

                    }
                }
            })

            removeNormalMap.addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    if (channel != null) {
                        val terrainLayerAsset = this@TerrainTextureLayerWidget.asset
                        if (channel == SplatTexture.Channel.BASE) {
                            terrainLayerAsset.splatBaseNormal = null
                        }
                        if (channel == SplatTexture.Channel.R) {
                            terrainLayerAsset.splatRNormal = null
                        } else if (channel == SplatTexture.Channel.G) {
                            terrainLayerAsset.splatGNormal = null
                        } else if (channel == SplatTexture.Channel.B) {
                            terrainLayerAsset.splatBNormal = null
                        } else if (channel == SplatTexture.Channel.A) {
                            terrainLayerAsset.splatANormal = null
                        }

                        applyDependencies()
                        layerUpdatedListener?.layerUpdated(terrainLayerAsset)
                        projectManager.current().assetManager.addModifiedAsset(terrainLayerAsset)
                    }
                }
            })

        }

        fun setChannel(channel: SplatTexture.Channel) {
            this.channel = channel
        }

        fun show() {
            updateMenuVisibility()
            showMenu(UI, Gdx.input.x.toFloat(), (Gdx.graphics.height - Gdx.input.y).toFloat())
        }

        private fun updateMenuVisibility() {
            // Show/Hide remove normal map button conditionally
            var normalMapRemoveVisible = false
            val terrainLayerAsset = this@TerrainTextureLayerWidget.asset
            if (channel == SplatTexture.Channel.BASE && terrainLayerAsset.splatBaseNormal != null) {
                normalMapRemoveVisible = true
            } else if (channel == SplatTexture.Channel.R && terrainLayerAsset.splatRNormal != null) {
                normalMapRemoveVisible = true
            } else if (channel == SplatTexture.Channel.G && terrainLayerAsset.splatGNormal != null) {
                normalMapRemoveVisible = true
            } else if (channel == SplatTexture.Channel.B && terrainLayerAsset.splatBNormal != null) {
                normalMapRemoveVisible = true
            } else if (channel == SplatTexture.Channel.A && terrainLayerAsset.splatANormal != null) {
                normalMapRemoveVisible = true
            }

            if (normalMapRemoveVisible && !removeNormalMap.hasParent()) {
                addItem(removeNormalMap)
            } else if (!normalMapRemoveVisible) {
                removeNormalMap.remove()
            }
            pack()
        }

    }
}