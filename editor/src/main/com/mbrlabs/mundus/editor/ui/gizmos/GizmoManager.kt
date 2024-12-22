package com.mbrlabs.mundus.editor.ui.gizmos

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g3d.decals.CameraGroupStrategy
import com.badlogic.gdx.graphics.g3d.decals.DecalBatch
import com.badlogic.gdx.utils.Array
import com.mbrlabs.mundus.commons.scene3d.components.Component
import com.mbrlabs.mundus.commons.scene3d.components.LightComponent
import com.mbrlabs.mundus.editor.Mundus
import com.mbrlabs.mundus.editor.events.ComponentAddedEvent
import com.mbrlabs.mundus.editor.events.ComponentRemovedEvent
import com.mbrlabs.mundus.editor.events.SceneGraphChangedEvent
import com.mbrlabs.mundus.editorcommons.events.ProjectChangedEvent
import com.mbrlabs.mundus.editorcommons.events.SceneChangedEvent

/**
 * Responsible for rendering Gizmos in the scene and listens for when new components are added.
 *
 * @author JamesTKhan
 * @version June 01, 2022
 */
class GizmoManager : ComponentAddedEvent.ComponentAddedListener,
        ComponentRemovedEvent.ComponentRemovedListener,
        SceneGraphChangedEvent.SceneGraphChangedListener,
        SceneChangedEvent.SceneChangedListener,
        ProjectChangedEvent.ProjectChangedListener {


    private lateinit var camera: Camera
    private lateinit var decalBatch: DecalBatch

    private var gizmos: Array<Gizmo>
    private var renderEnabled = true

    init {
        Mundus.registerEventListener(this)
        gizmos = Array<Gizmo>()
    }

    fun render() {
        if (!renderEnabled) return

        for (gizmo in gizmos) {
            gizmo.update()
            // Billboard behavior
            gizmo.decal.lookAt(camera.position, camera.up)
            // Add to batch for rendering
            decalBatch.add(gizmo.decal)
        }

        decalBatch.flush()
    }

    fun setCamera(camera: Camera) {
        this.camera = camera
        decalBatch = DecalBatch(CameraGroupStrategy(camera))
    }

    fun toggleRendering() {
        renderEnabled = !renderEnabled
    }

    fun isRenderEnabled(): Boolean {
        return renderEnabled
    }

    private fun removeObsoleteGizmos() {
        val iterator = gizmos.iterator()
        while (iterator.hasNext()) {
            val gizmo = iterator.next()
            if (gizmo.shouldRemove()) {
                gizmo.dispose()
                iterator.remove()
            }
        }
    }

    override fun onComponentAdded(event: ComponentAddedEvent) {
        if (event.component.type == Component.Type.LIGHT)
            gizmos.add(LightGizmo(event.component as LightComponent))
    }

    override fun onComponentRemoved(event: ComponentRemovedEvent) {
        removeObsoleteGizmos()
    }

    override fun onSceneGraphChanged(event: SceneGraphChangedEvent) {
        removeObsoleteGizmos()
    }

    override fun onSceneChanged(event: SceneChangedEvent) {
        clearGizmos()
    }

    override fun onProjectChanged(event: ProjectChangedEvent) {
        clearGizmos()
    }

    private fun clearGizmos() {
        for (gizmo in gizmos) {
            gizmo.dispose()
        }
        gizmos.clear()
    }
}