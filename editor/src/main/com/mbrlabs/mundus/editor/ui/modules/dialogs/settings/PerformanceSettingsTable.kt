package com.mbrlabs.mundus.editor.ui.modules.dialogs.settings

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.kotcrab.vis.ui.widget.VisCheckBox
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisTable
import com.mbrlabs.mundus.editor.Mundus
import com.mbrlabs.mundus.editor.core.project.ProjectManager
import com.mbrlabs.mundus.editor.core.scene.SceneManager
import com.mbrlabs.mundus.editor.events.ProjectChangedEvent
import com.mbrlabs.mundus.editor.events.SceneChangedEvent
import com.mbrlabs.mundus.editor.preferences.MundusPreferencesManager
import com.mbrlabs.mundus.editor.tools.brushes.TerrainBrush
import com.mbrlabs.mundus.editor.ui.UI
import com.mbrlabs.mundus.editor.ui.widgets.ToolTipLabel

/**
 * @author JamesTKhan
 * @version July 23, 2022
 */
class PerformanceSettingsTable : BaseSettingsTable(), ProjectChangedEvent.ProjectChangedListener, SceneChangedEvent.SceneChangedListener {

    private val projectManager: ProjectManager = Mundus.inject()
    private var globalPrefManager: MundusPreferencesManager = Mundus.inject()

    private val frustumCullingChkBox = VisCheckBox(null)
    private val optimizeTerrainUpdates = VisCheckBox(null)

    init {
        Mundus.registerEventListener(this)
        top().left()
        defaults().left().pad(4f)

        add(VisLabel("Performance Settings")).row()
        addSeparator().padBottom(10f).row()

        val settingsTable = VisTable()
        settingsTable.defaults().left().pad(4f)
        val frustumLabel = ToolTipLabel("Perform Frustum Culling (Per Scene)", "Frustum Culling increase performance by not rendering offscreen " +
            "objects.\nThis is done by calculating the bounds of GameObjects models and checking for intersections on the camera frustum.\n" +
                "If objects are being culled while still on screen make sure all transforms are applied to your model in your modeling application.\n" +
                "\nNote: If you are using shadows, then the shadow frustum is also used to test for visibility to allow for shadows to appear from offscreen objects.\n" +
            "This means that objects directly behind the player may not be culled until also out of the shadow frustum.")

        settingsTable.add(frustumLabel)
        settingsTable.add(frustumCullingChkBox).row()

        frustumCullingChkBox.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                projectManager.current().currScene.settings.useFrustumCulling = frustumCullingChkBox.isChecked
            }
        })

        val terrainUpdatesLabel = ToolTipLabel("Optimize Terrain Updates", "Depending on the vertex resolution of your terrain, " +
            "updating the terrain mesh can be very expensive.\nWith this option enabled, normals will only be calculated after releasing the mouse button while modifying terrains." +
                "\nIf you experience slowdowns when updating terrains, try enabling this option.")

        settingsTable.add(terrainUpdatesLabel)
        settingsTable.add(optimizeTerrainUpdates).row()
        optimizeTerrainUpdates.isChecked = globalPrefManager.getBoolean(MundusPreferencesManager.GLOB_OPTIMIZE_TERRAIN_UPDATES, false)
        TerrainBrush.setOptimizeTerrainUpdates(optimizeTerrainUpdates.isChecked)

        add(settingsTable)
    }

    private fun updateValues() {
        frustumCullingChkBox.isChecked = projectManager.current().currScene.settings.useFrustumCulling
    }

    override fun onSave() {
        SceneManager.saveScene(projectManager.current(), projectManager.current().currScene)

        // Save prefs
        globalPrefManager.set(MundusPreferencesManager.GLOB_OPTIMIZE_TERRAIN_UPDATES, optimizeTerrainUpdates.isChecked)
        TerrainBrush.setOptimizeTerrainUpdates(optimizeTerrainUpdates.isChecked)

        UI.toaster.success("Settings saved")
    }

    override fun onProjectChanged(event: ProjectChangedEvent) {
        updateValues()
    }

    override fun onSceneChanged(event: SceneChangedEvent) {
        updateValues()
    }
}