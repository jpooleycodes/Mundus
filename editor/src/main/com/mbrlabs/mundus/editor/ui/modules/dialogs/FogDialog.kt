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
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.kotcrab.vis.ui.widget.VisCheckBox
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisTextField
import com.kotcrab.vis.ui.widget.color.ColorPickerAdapter
import com.mbrlabs.mundus.editor.Mundus
import com.mbrlabs.mundus.editor.core.project.ProjectManager
import com.mbrlabs.mundus.editor.ui.widgets.ColorPickerField
import com.mbrlabs.mundus.editorcommons.events.ProjectChangedEvent
import com.mbrlabs.mundus.editorcommons.events.SceneChangedEvent
import net.mgsx.gltf.scene3d.attributes.FogAttribute

/**
 * @author Marcus Brummer
 * @version 06-01-2016
 */
class FogDialog : BaseDialog("Fog"), ProjectChangedEvent.ProjectChangedListener, SceneChangedEvent.SceneChangedListener {

    private val useFog = VisCheckBox("Use fog")
    private val nearPlane = VisTextField("0")
    private val farPlane = VisTextField("0")
    private val gradient = VisTextField("0")
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

        root.add(useFog).left().padBottom(10f).colspan(2).row()
        root.add(VisLabel("Near Plane: ")).left().padBottom(10f)
        root.add(nearPlane).growX().padBottom(10f).row()
        root.add(VisLabel("Far Plane: ")).left().padBottom(10f)
        root.add(farPlane).growX().padBottom(10f).row()
        root.add(VisLabel("Gradient: ")).left().padBottom(10f)
        root.add(gradient).growX().padBottom(10f).row()
        root.add(VisLabel("Color")).growX().row()
        root.add(colorPickerField).left().growX().colspan(2).row()
        resetValues()
    }

    private fun setupListeners() {

        // use fog checkbox
        useFog.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                val projectContext = projectManager.current()
                if (useFog.isChecked) {
                    val fogEquation: FogAttribute? = getFogEquationAttribute()

                    if (fogEquation == null) {
                        nearPlane.text = projectContext.currScene.cam.near.toString()
                        farPlane.text = (projectContext.currScene.cam.far / 4f).toString()
                        gradient.text = 1.5f.toString()
                        setFogAttributes()
                    }

                    nearPlane.isDisabled = false
                    farPlane.isDisabled = false
                    gradient.isDisabled = false
                    colorPickerField.disable(false)
                } else {
                    nearPlane.isDisabled = true
                    farPlane.isDisabled = true
                    gradient.isDisabled = true
                    colorPickerField.disable(true)
                    projectContext.currScene.environment.remove(FogAttribute.FogEquation)
                    projectContext.currScene.environment.remove(ColorAttribute.Fog)
                }
            }
        })

        // gradient
        gradient.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                setFogAttributes()
            }
        })

        nearPlane.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                setFogAttributes()
            }
        })

        farPlane.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                setFogAttributes()
            }
        })

        // color
        colorPickerField.colorAdapter = object: ColorPickerAdapter() {
            override fun finished(newColor: Color) {
                setFogAttributes()
            }
        }

    }

    private fun setFogAttributes() {
        val n = convert(nearPlane.text)
        val f = convert(farPlane.text)
        val g = convert(gradient.text)
        if (g == null || f == null || n == null) return
        projectManager.current().currScene.environment.set(FogAttribute(FogAttribute.FogEquation).set(n, f, g))
        projectManager.current().currScene.environment.set(ColorAttribute(ColorAttribute.Fog, colorPickerField.selectedColor))
    }

    private fun resetValues() {
        val fogEquation = getFogEquationAttribute()
        if (fogEquation == null) {
            nearPlane.isDisabled = true
            farPlane.isDisabled = true
            gradient.isDisabled = true
        } else {
            val fogColorAttrib = getFogColorAttribute()
            useFog.isChecked = true
            nearPlane.text = fogEquation.value.x.toString()
            farPlane.text = fogEquation.value.y.toString()
            gradient.text = fogEquation.value.z.toString()
            colorPickerField.selectedColor = if (fogColorAttrib == null) Color.WHITE else fogColorAttrib.color
        }
    }

    private fun convert(input: String): Float? {
        try {
            if (input.isEmpty()) return null
            return java.lang.Float.valueOf(input)
        } catch (e: Exception) {
            return null
        }

    }

    private fun getFogColorAttribute(): ColorAttribute? {
        return projectManager.current().currScene.environment.get(ColorAttribute::class.java, ColorAttribute.Fog)
    }

    private fun getFogEquationAttribute(): FogAttribute? {
        return projectManager.current().currScene.environment.get(FogAttribute::class.java, FogAttribute.FogEquation)
    }

    override fun onProjectChanged(event: ProjectChangedEvent) {
        resetValues()
    }

    override fun onSceneChanged(event: SceneChangedEvent) {
        resetValues()
    }

}
