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

package com.mbrlabs.mundus.editor.ui.modules.dock.assets

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Array
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.layout.GridGroup
import com.kotcrab.vis.ui.widget.*
import com.kotcrab.vis.ui.widget.tabbedpane.Tab
import com.mbrlabs.mundus.commons.assets.*
import com.mbrlabs.mundus.commons.utils.TextureProvider
import com.mbrlabs.mundus.editor.Mundus
import com.mbrlabs.mundus.editor.assets.EditorModelAsset
import com.mbrlabs.mundus.editor.core.project.ProjectManager
import com.mbrlabs.mundus.editor.events.*
import com.mbrlabs.mundus.editor.ui.UI
import com.mbrlabs.mundus.editor.ui.widgets.AutoFocusScrollPane
import com.mbrlabs.mundus.editor.utils.ObjExporter
import com.mbrlabs.mundus.editorcommons.events.ProjectChangedEvent
import java.awt.Desktop
import java.lang.RuntimeException


/**
 * @author Marcus Brummer
 * @version 08-12-2015
 */
class AssetsDock : Tab(false, false),
        ProjectChangedEvent.ProjectChangedListener,
        AssetImportEvent.AssetImportListener,
        AssetDeletedEvent.AssetDeletedListener,
        GameObjectSelectedEvent.GameObjectSelectedListener,
        FullScreenEvent.FullScreenEventListener,
        MaterialDuplicatedEvent.MaterialDuplicatedEventListener {

    private val root = VisTable()
    private val filesViewContextContainer = VisTable(false)
    private val filesView = GridGroup(80f, 4f)

    private val filterAssets = VisSelectBox<String>()
    private var currentFilter: AssetType? = null

    private val assetItems = Array<AssetItem>()

    private val assetOpsMenu = PopupMenu()
    private val renameAsset = MenuItem("Rename Asset")
    private val openDirectoryAsset = MenuItem("Open Directory")
    private val deleteAsset = MenuItem("Delete Asset")
    private val exportTerrainAsset = MenuItem("Export to OBJ")

    private var currentSelection: AssetItem? = null
    private val projectManager: ProjectManager = Mundus.inject()

    private val thumbnailOverlay: TextureRegionDrawable
    private var selectedOverlay: Image

    init {
        Mundus.registerEventListener(this)
        initUi()

        // Darkening overlay to darken texture thumbnails slightly so text is more visible
        val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
        pixmap.setColor(0.0f,0.0f,0.0f,0.5f)
        pixmap.fill()
        thumbnailOverlay = TextureRegionDrawable(TextureRegion(Texture(pixmap)))

        selectedOverlay = Image(VisUI.getSkin().getDrawable("default-select-selection"))
        selectedOverlay.color.a = 0.6f
    }

    fun initUi() {
        val values = Array<String>()
        values.add("All")
        for (value in AssetType.values())
            values.add(value.value)

        filterAssets.items = values
        filesView.touchable = Touchable.enabled

        val contentTable = VisTable(false)
        val bar = VisTable()
        bar.defaults().pad(2f)
        bar.add(VisLabel("Assets "))
        bar.addSeparator(true)
        bar.add(filterAssets)
        contentTable.add(bar).left().pad(2f).row()
        contentTable.add(Separator()).expandX().fillX()
        contentTable.row()
        contentTable.add<VisTable>(filesViewContextContainer).expandX().fillX()
        contentTable.row()
        contentTable.add(createScrollPane(filesView, true)).expand().fill()

        val splitPane = VisSplitPane(VisLabel("file tree here"), contentTable, false)
        splitPane.setSplitAmount(0.2f)

        root.setBackground("window-bg")
        root.add(splitPane).expand().fill()

        // asset ops right click menu
        assetOpsMenu.addItem(renameAsset)
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            assetOpsMenu.addItem(openDirectoryAsset)
        }
        assetOpsMenu.addItem(deleteAsset)

        registerListeners()
    }

    fun clearSelection() {
        setSelected(null)
    }

    private fun registerListeners() {
        deleteAsset.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent, x: Float, y: Float) {
                currentSelection?.asset?.let {
                    projectManager.current().assetManager.deleteAssetSafe(it, projectManager)
                    reloadAssets()
                }
            }
        })

        openDirectoryAsset.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent, x: Float, y: Float) {
                currentSelection?.asset?.let {
                    Desktop.getDesktop().open(it.file.file().parentFile)
                }
            }
        })

        exportTerrainAsset.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent, x: Float, y: Float) {
                currentSelection?.asset?.let {
                    it as TerrainAsset
                    try {
                        ObjExporter.exportToObj(it.name, it.terrain)
                        UI.toaster.success("Terrain export successful")
                    } catch (ex: RuntimeException) {
                        UI.toaster.error("Error during export. ${ex.message}")
                    }

                }
            }
        })

        filterAssets.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                currentFilter = AssetType.valueFromString(filterAssets.selected)
                reloadAssets()
            }
        })

    }

    private fun setSelected(assetItem: AssetItem?) {
        currentSelection = assetItem
        for (item in assetItems) {
            if (currentSelection != null && currentSelection == item) {
                item.toggleSelectOverlay(true)
            } else {
                item.toggleSelectOverlay(false)
            }
        }
    }

    /**
     * Highlights the selected asset item in the dock view.
     * @param asset
     */
    fun setSelected(asset: Asset) {
        for (item in assetItems) {
            if (item.asset == asset) {
                item.toggleSelectOverlay(true)
            } else {
                item.toggleSelectOverlay(false)
            }
        }
    }

    fun reloadAssets() {
        filesView.clearChildren()
        val projectContext = projectManager.current()
        for (asset in projectContext.assetManager.assets) {
            if (currentFilter != null && asset.meta.type != currentFilter) continue
            val assetItem = AssetItem(asset)
            filesView.addActor(assetItem)
            assetItems.add(assetItem)
            assetItem.layout()
        }
    }

    private fun createScrollPane(actor: Actor, disableX: Boolean): VisScrollPane {
        val scrollPane = AutoFocusScrollPane(actor)
        scrollPane.setFadeScrollBars(false)
        scrollPane.setScrollingDisabled(disableX, false)
        return scrollPane
    }

    override fun getTabTitle(): String {
        return "Assets"
    }

    override fun getContentTable(): Table {
        return root
    }

    override fun onProjectChanged(event: ProjectChangedEvent) {
        reloadAssets()
    }

    override fun onAssetImported(event: AssetImportEvent) {
        reloadAssets()
    }

    override fun onAssetDeleted(event: AssetDeletedEvent) {
        reloadAssets()
    }

    override fun onGameObjectSelected(event: GameObjectSelectedEvent) {
        clearSelection()
    }

    override fun onFullScreenEvent(event: FullScreenEvent) {
        if (!event.isFullScreen)
            reloadAssets()
    }

    override fun onMaterialDuplicated(event: MaterialDuplicatedEvent) {
        reloadAssets()
    }

    /**
     * Asset item in the grid.
     */
    private inner class AssetItem(val asset: Asset) : VisTable() {

        private val nameLabel: VisLabel
        private var nameTable: VisTable
        val stack = Stack()

        init {
            setBackground("menu-bg")
            align(Align.center)
            nameLabel = VisLabel(asset.toString(), "tiny")
            nameLabel.wrap = true

            nameTable = VisTable()
            nameTable.add(nameLabel).grow().top().row()

            loadBackground()

            stack.add(nameTable)
            add(stack).grow().top().row()

            addListener(object : InputListener() {
                override fun touchDown(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int): Boolean {
                    return true
                }

                override fun touchUp(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int) {
                    if (event!!.button == Input.Buttons.RIGHT) {
                        setSelected()

                        if (asset is TerrainAsset) {
                            if ( !exportTerrainAsset.hasParent())
                                assetOpsMenu.addItem(exportTerrainAsset)
                        } else {
                            exportTerrainAsset.remove()
                            assetOpsMenu.pack()
                        }

                        assetOpsMenu.showMenu(UI, Gdx.input.x.toFloat(),
                                (Gdx.graphics.height - Gdx.input.y).toFloat())
                    } else if (event.button == Input.Buttons.LEFT) {
                        setSelected()
                    }
                }

            })
        }

        fun setSelected() {
            this@AssetsDock.setSelected(this@AssetItem)
            Mundus.postEvent(AssetSelectedEvent(asset))
        }

        private fun loadBackground() {
            if (asset is TextureProvider) {
                nameTable.background = thumbnailOverlay
                stack.add(Image(asset.texture))
            }
        }

        fun toggleSelectOverlay(selected: Boolean) {
            if (selected) {
                // Remove the name table from stack, put the selected overlay on, then put the name table back on
                // the stack, over top of the select overlay
                stack.removeActor(nameTable)
                stack.add(selectedOverlay)
                stack.add(nameTable)
            } else {
                stack.removeActor(selectedOverlay)
            }
        }
    }
}
