package com.mbrlabs.mundus.editor.ui.modules.dialogs

import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.kotcrab.vis.ui.util.FloatDigitsOnlyFilter
import com.kotcrab.vis.ui.util.IntDigitsOnlyFilter
import com.kotcrab.vis.ui.util.dialog.Dialogs
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisSelectBox
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisTextButton
import com.kotcrab.vis.ui.widget.VisTextField
import com.mbrlabs.mundus.commons.shadows.MundusDirectionalShadowLight
import com.mbrlabs.mundus.commons.shadows.ShadowResolution
import com.mbrlabs.mundus.commons.utils.LightUtils
import com.mbrlabs.mundus.editor.Mundus
import com.mbrlabs.mundus.editor.core.project.ProjectManager
import com.mbrlabs.mundus.editor.ui.widgets.ToolTipLabel
import com.mbrlabs.mundus.editorcommons.events.ProjectChangedEvent
import com.mbrlabs.mundus.editorcommons.events.SceneChangedEvent

/**
 * @author JamesTKhan
 * @version July 18, 2022
 */
class ShadowSettingsDialog : BaseDialog("Shadow Settings"), ProjectChangedEvent.ProjectChangedListener,
    SceneChangedEvent.SceneChangedListener {

    private val root = VisTable()
    private val viewportSize = VisTextField("0")
    private val camNear = VisTextField("0")
    private val camFar = VisTextField("0")
    private val applyBtn = VisTextButton("Apply Settings")
    private val defaultBtn = VisTextButton("Reset Defaults")
    private lateinit var shadowResSelectBox: VisSelectBox<String>

    private val projectManager: ProjectManager = Mundus.inject()

    init {
        Mundus.registerEventListener(this)

        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        root.defaults().padBottom(5f).padTop(5f)
        add(root)

        root.add(VisLabel("Shadow settings must be applied once changed.\nShadow settings are set per scene, not globally."))
            .colspan(2).center().row()

        root.add(
            ToolTipLabel(
                "Viewport Width",
                "The size of the orthogonal viewport. \nLarge viewports will cover more distance but result " +
                        "in more pixelated shadows due to perspective aliasing. \nKeep this as small as possible."
            )
        ).left().padBottom(10f)
        root.add(viewportSize).left().padBottom(10f).row()

        root.add(ToolTipLabel("Camera Near Plane", "Near plane of Orthogonal camera.")).left().padBottom(10f)
        root.add(camNear).left().padBottom(10f).row()

        root.add(
            ToolTipLabel(
                "Camera Far Plane",
                "Far plane of Orthogonal camera.\nThe closer this is to the near plane, the more depth precision the shadows will have."
            )
        ).left().padBottom(10f)
        root.add(camFar).left().padBottom(10f).row()

        val resolutionLabel = ToolTipLabel(
            "Shadow Resolution", "Higher resolution results in better " +
                    "shadows at the cost of performance."
        )
        val selectorsTable = VisTable(true)
        shadowResSelectBox = VisSelectBox<String>()
        shadowResSelectBox.setItems(
            ShadowResolution._512.value,
            ShadowResolution._1024.value,
            ShadowResolution._2048.value,
            ShadowResolution._4096.value
        )
        selectorsTable.add(shadowResSelectBox)

        root.add(resolutionLabel).left().padBottom(10f)
        root.add(selectorsTable).left().padBottom(10f).row()

        root.add(applyBtn).pad(4f).growX()
        root.add(defaultBtn).pad(4f).growX()

        resetValues()
    }

    private fun setupListeners() {
        applyBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                // Validate fields first
                val errorMessage = validateFields()
                if (errorMessage != null) {
                    Dialogs.showErrorDialog(stage, errorMessage)
                    return
                }

                val directionalLightEx = LightUtils.getDirectionalLight(projectManager.current().currScene.environment)
                if (directionalLightEx is MundusDirectionalShadowLight) {
                    val res = ShadowResolution.valueFromString(shadowResSelectBox.selected)
                    directionalLightEx.set(
                        res,
                        viewportSize.text.toInt(),
                        viewportSize.text.toInt(),
                        camNear.text.toFloat(),
                        camFar.text.toFloat()
                    )
                }

                resetValues()
            }
        })

        defaultBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                val directionalLightEx = LightUtils.getDirectionalLight(projectManager.current().currScene.environment)
                if (directionalLightEx is MundusDirectionalShadowLight) {
                    directionalLightEx.set(
                        ShadowResolution.DEFAULT_SHADOW_RESOLUTION,
                        MundusDirectionalShadowLight.DEFAULT_VIEWPORT_SIZE,
                        MundusDirectionalShadowLight.DEFAULT_VIEWPORT_SIZE,
                        MundusDirectionalShadowLight.DEFAULT_CAM_NEAR,
                        MundusDirectionalShadowLight.DEFAULT_CAM_FAR
                    )

                    resetValues()
                }
            }
        })

        viewportSize.textFieldFilter = IntDigitsOnlyFilter(false)
        camNear.textFieldFilter = FloatDigitsOnlyFilter(false)
        camFar.textFieldFilter = FloatDigitsOnlyFilter(false)
    }

    private fun resetValues() {
        val directionalLightEx = LightUtils.getDirectionalLight(projectManager.current().currScene.environment)
        if (directionalLightEx is MundusDirectionalShadowLight) {
            viewportSize.text = directionalLightEx.camera.viewportWidth.toInt().toString()
            camNear.text = directionalLightEx.camera.near.toString()
            camFar.text = directionalLightEx.camera.far.toString()
            shadowResSelectBox.selected = directionalLightEx.shadowResolution.value
        }

    }

    private fun validateFields(): String? {
        if ((!viewportSize.isInputValid) || viewportSize.isEmpty) return "Viewport Size is not valid"
        if ((!camNear.isInputValid) || camNear.isEmpty) return "Camera Near is not valid"
        if ((!camFar.isInputValid) || camFar.isEmpty) return "Camera Far is not valid"

        return null
    }

    override fun onProjectChanged(event: ProjectChangedEvent) {
        resetValues()
    }

    override fun onSceneChanged(event: SceneChangedEvent) {
        resetValues()
    }

}