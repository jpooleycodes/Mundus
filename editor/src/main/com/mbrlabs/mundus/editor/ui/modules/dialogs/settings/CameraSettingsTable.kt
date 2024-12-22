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

package com.mbrlabs.mundus.editor.ui.modules.dialogs.settings

import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.kotcrab.vis.ui.util.FloatDigitsOnlyFilter
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisTextButton
import com.kotcrab.vis.ui.widget.VisTextField
import com.mbrlabs.mundus.commons.env.CameraSettings
import com.mbrlabs.mundus.editor.Mundus
import com.mbrlabs.mundus.editor.core.plugin.PluginManagerProvider
import com.mbrlabs.mundus.editor.core.project.ProjectManager
import com.mbrlabs.mundus.editor.core.scene.SceneManager
import com.mbrlabs.mundus.editor.events.LogEvent
import com.mbrlabs.mundus.editor.events.LogType
import com.mbrlabs.mundus.editor.ui.UI
import com.mbrlabs.mundus.editorcommons.events.ProjectChangedEvent
import com.mbrlabs.mundus.editorcommons.events.SceneChangedEvent
import org.pf4j.PluginManager

/**
 * @author JamesTKhan
 * @version July 10, 2022
 */
class CameraSettingsTable : BaseSettingsTable(), ProjectChangedEvent.ProjectChangedListener, SceneChangedEvent.SceneChangedListener {

    private val projectManager: ProjectManager = Mundus.inject()
    private val pluginManager: PluginManager = Mundus.inject<PluginManagerProvider>().pluginManager

    private val nearPlane = VisTextField("0")
    private val farPlane = VisTextField("0")
    private val fov = VisTextField("0")
    private val defaultBtn = VisTextButton("Reset Defaults")

    init {
        Mundus.registerEventListener(this)

        top().left()
        defaults().left().pad(4f)
        padRight(5f).padLeft(6f)

        add(VisLabel("Camera Settings")).row()
        addSeparator().padBottom(10f)

        val label = VisLabel()
        label.setText("Camera Settings are per scene, not global. You can change camera values at runtime " +
                "by accessing the scene.cam instance and modifying it in your runtime application as well.")
        label.wrap = true
        label.width = SettingsDialog.WIDTH * 0.7f
        add(label).expandX().prefWidth(SettingsDialog.WIDTH * 0.7f).row()

        add(VisLabel("Near Plane")).row()
        add(nearPlane).left().row()

        add(VisLabel("Far Plane")).row()
        add(farPlane).left().row()

        add(VisLabel("Field Of View")).row()
        add(fov).left().row()

        add(defaultBtn).left().row()

        registerListeners()
    }

    private fun updateValues() {
        nearPlane.text = projectManager.current().currScene.cam.near.toString()
        farPlane.text = projectManager.current().currScene.cam.far.toString()
        if (projectManager.current().currScene.cam is PerspectiveCamera) {
            fov.text = (projectManager.current().currScene.cam as PerspectiveCamera).fieldOfView.toString()
        }
    }

    override fun onProjectChanged(event: ProjectChangedEvent) {
        updateValues()
    }

    override fun onSceneChanged(event: SceneChangedEvent) {
        updateValues()
    }

    private fun registerListeners() {
        nearPlane.textFieldFilter = FloatDigitsOnlyFilter(true)
        nearPlane.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                if(nearPlane.isInputValid && !nearPlane.isEmpty) {
                    try {
                        projectManager.current().currScene.cam.near = nearPlane.text.toFloat()
                    }
                    catch (ex : NumberFormatException) {
                        Mundus.postEvent(LogEvent(LogType.ERROR,"Error parsing field " + nearPlane.name))
                    }
                }
            }
        })

        farPlane.textFieldFilter = FloatDigitsOnlyFilter(true)
        farPlane.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                if(farPlane.isInputValid && !farPlane.isEmpty) {
                    try {
                        projectManager.current().currScene.cam.far = farPlane.text.toFloat()
                    }
                    catch (ex : NumberFormatException) {
                        Mundus.postEvent(LogEvent(LogType.ERROR,"Error parsing field " + farPlane.name))
                    }
                }
            }
        })

        fov.textFieldFilter = FloatDigitsOnlyFilter(true)
        fov.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                if(fov.isInputValid && !fov.isEmpty) {
                    try {
                        if (projectManager.current().currScene.cam is PerspectiveCamera) {
                            (projectManager.current().currScene.cam as PerspectiveCamera).fieldOfView = fov.text.toFloat()
                        }
                    }
                    catch (ex : NumberFormatException) {
                        Mundus.postEvent(LogEvent(LogType.ERROR,"Error parsing field " + fov.name))
                    }
                }
            }
        })

        defaultBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                projectManager.current().currScene.cam.near = CameraSettings.DEFAULT_NEAR_PLANE
                projectManager.current().currScene.cam.far = CameraSettings.DEFAULT_FAR_PLANE
                if (projectManager.current().currScene.cam is PerspectiveCamera) {
                    (projectManager.current().currScene.cam as PerspectiveCamera).fieldOfView = CameraSettings.DEFAULT_FOV
                }
                updateValues()
            }
        })
    }

    override fun onSave() {
        SceneManager.saveScene(projectManager.current(), projectManager.current().currScene, pluginManager)
        UI.toaster.success("Settings saved")
    }

}