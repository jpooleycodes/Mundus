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

package com.mbrlabs.mundus.editor.ui.modules.dialogs

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.color.ColorPickerAdapter
import com.mbrlabs.mundus.editor.Mundus
import com.mbrlabs.mundus.editor.core.project.ProjectManager
import com.mbrlabs.mundus.editor.ui.widgets.ColorPickerField
import com.mbrlabs.mundus.editorcommons.events.ProjectChangedEvent
import com.mbrlabs.mundus.editorcommons.events.SceneChangedEvent

/**
 * @author Marcus Brummer
 * *
 * @version 04-03-2016
 */
class AmbientLightDialog : BaseDialog("Ambient Light"), ProjectChangedEvent.ProjectChangedListener,
        SceneChangedEvent.SceneChangedListener {

    private val colorPickerField = ColorPickerField()

    private val projectManager: ProjectManager = Mundus.inject()

    init {
        Mundus.registerEventListener(this)

        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        val root = Table()
        root.padTop(6f).padRight(6f).padBottom(22f)
        add(root)
        root.add(VisLabel("Color")).growX().row()
        root.add(colorPickerField).left().fillX().expandX().colspan(2).row()
        resetValues()
    }

    private fun setupListeners() {

        // color
        colorPickerField.colorAdapter = object: ColorPickerAdapter() {
            override fun finished(newColor: Color) {
                val projectContext = projectManager.current()
                val ambientLight: ColorAttribute = projectContext.currScene.environment.get(
                    ColorAttribute::class.java, ColorAttribute.AmbientLight
                )
                ambientLight.color.set(newColor)
            }

            override fun changed(newColor: Color?) {
                val projectContext = projectManager.current()
                val ambientLight: ColorAttribute = projectContext.currScene.environment.get<ColorAttribute>(
                    ColorAttribute::class.java, ColorAttribute.AmbientLight
                )
                ambientLight.color.set(newColor)
            }

            override fun canceled(oldColor: Color?) {
                val projectContext = projectManager.current()
                val ambientLight: ColorAttribute = projectContext.currScene.environment.get<ColorAttribute>(
                    ColorAttribute::class.java, ColorAttribute.AmbientLight
                )
                ambientLight.color.set(oldColor)
            }
        }

    }

    private fun resetValues() {
        val light = projectManager.current().currScene.environment.ambientLight
        colorPickerField.selectedColor = light.color
    }

    override fun onProjectChanged(event: ProjectChangedEvent) {
        resetValues()
    }

    override fun onSceneChanged(event: SceneChangedEvent) {
        resetValues()
    }

}
