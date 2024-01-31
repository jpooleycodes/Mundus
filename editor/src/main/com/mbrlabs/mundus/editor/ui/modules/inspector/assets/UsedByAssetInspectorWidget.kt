package com.mbrlabs.mundus.editor.ui.modules.inspector.assets

import com.badlogic.gdx.utils.Align
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisTable
import com.mbrlabs.mundus.commons.assets.Asset
import com.mbrlabs.mundus.commons.scene3d.GameObject
import com.mbrlabs.mundus.editor.core.project.ProjectManager
import com.mbrlabs.mundus.editor.ui.modules.inspector.BaseInspectorWidget
import com.mbrlabs.mundus.editor.ui.widgets.AutoFocusScrollPane


/**
 * Shows which assets and scenes an Asset is used by.
 *
 * @author JamesTKhan
 * @version July 29, 2022
 */
class UsedByAssetInspectorWidget  : BaseInspectorWidget(TITLE) {

    companion object {
        private val TITLE = "Usages"
    }

    val root = VisTable()
    private val scrollPane = AutoFocusScrollPane(root)

    init {
        isDeletable = false
        defaults().padBottom(4f)
        collapsibleContent.add(scrollPane).top().left().growX( ).row()
    }

    fun setAsset(asset: Asset?, projectManager: ProjectManager) {

        root.clearChildren()
        if (asset == null) return
        val usagesInAssets = projectManager.current().assetManager.findAssetUsagesInAssets(asset)
        val usagesInScenes = projectManager.current().assetManager.findAssetUsagesInScenes(projectManager, asset)

        root.defaults().align(Align.left)

        root.add(VisLabel("Used by Assets")).row()
        root.addSeparator()
        if (usagesInAssets.isNotEmpty()) {
            for (assetUsage in usagesInAssets) {
                root.add(assetUsage.name).row()
            }
        } else {
            root.add("None").row()
        }

        root.add(VisLabel("Used in Scenes")).padTop(10f).row()
        root.addSeparator()
        if (usagesInScenes.isNotEmpty()) {
            for (assetUsage in usagesInScenes) {
                root.add(assetUsage.value + ": " + assetUsage.key.name).row()
            }
        } else {
            root.add("None").row()
        }
    }

    override fun onDelete() {
        // can't be deleted
    }

    override fun setValues(go: GameObject) {
        // nope
    }

}