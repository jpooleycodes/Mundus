package com.mbrlabs.mundus.editor.ui.widgets

import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisTable
import com.mbrlabs.mundus.commons.terrain.LodLevel

/**
 * @author JamesTKhan
 * @version October 04, 2023
 */
class LevelOfDetailWidget : BaseWidget() {
    private val lodLevelCount = VisLabel()
    private val lodTable = VisTable()

    init {
        add(VisLabel("Level of Detail")).padTop(5f).growX().row()
        addSeparator().padBottom(5f).row()
        add(lodLevelCount).growX().row()
        add(lodTable).growX().row()
    }

    fun setLodLevels(lodLevels: Array<LodLevel>?) {
        if (lodLevels == null) {
            lodLevelCount.setText("Lod Levels: None")
            lodTable.clear()
            return
        }

        lodLevelCount.setText("Lod Levels: " + lodLevels.size)
        lodTable.clear()
        lodTable.add(lodLevelCount).growX().row()
        for (i in lodLevels.indices) {

            var verticesCount = 0
            var indicesCount = 0
            for (j in lodLevels[i].lodMesh.indices) {
                val lodMesh = lodLevels[i].lodMesh[j]
                verticesCount += lodMesh.numVertices
                indicesCount += lodMesh.numIndices
            }

            val lodLabel = VisLabel()
            lodLabel.setText("LOD $i: $verticesCount vertices, $indicesCount indices")
            lodTable.add(lodLabel).growX().row()
        }
    }
}