package com.mbrlabs.mundus.editor.ui.modules.dialogs

import com.badlogic.gdx.scenes.scene2d.Action
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.kotcrab.vis.ui.util.dialog.Dialogs
import com.kotcrab.vis.ui.widget.VisCheckBox
import com.kotcrab.vis.ui.widget.VisDialog
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisTextButton
import com.mbrlabs.mundus.editor.Mundus
import com.mbrlabs.mundus.editor.core.project.ProjectManager
import com.mbrlabs.mundus.editor.terrain.TerrainStitcher
import com.mbrlabs.mundus.editor.ui.UI
import com.mbrlabs.mundus.editor.ui.widgets.IntegerFieldWithLabel
import com.mbrlabs.mundus.editor.ui.widgets.ToolTipLabel

/**
 * @author JamesTKhan
 * @version June 26, 2023
 */
class TerrainStitcherDialog : BaseDialog(TITLE) {

    private val projectManager: ProjectManager = Mundus.inject()

    companion object {
        private const val TITLE = "Terrain Stitching Dialog"
    }

    private val executeBtn = VisTextButton("Execute")
    private val numStepsField = IntegerFieldWithLabel("", -1, false)
    private val includeWorldHeight = VisCheckBox("")

    init {
        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        numStepsField.text = TerrainStitcher.numSteps.toString()

        val table = VisTable()

        val description = VisLabel("Stitches/fixes broken seams on all terrains in the current scene\n that have terrain neighbors assigned.")
        table.add(description).left().padBottom(10f).colspan(2).row()

        table.addSeparator()

        table.add(ToolTipLabel("Include World Height", "If checked, the world height will be included in the stitching process, otherwise only vertex height is considered." +
                "\nIdeally all terrains are at 0 world height. If they aren't, you should check this.")).left().padBottom(10f)
        table.add(includeWorldHeight).left().padBottom(10f).colspan(2).row()

        table.add(ToolTipLabel("Number of Steps", "How many steps around the broken seams will be interpolated." +
                "\nThe larger the value, the smoother the transitions (but more alteration to the terrain)." +
                "\nCannot exceed terrains vertex resolution.")).left().padBottom(10f)
        table.add(numStepsField).fillX().expandX().row()
        table.add(executeBtn).colspan(2).growX()

        add(table).pad(6f)
    }

    override fun show(stage: Stage?, action: Action?): VisDialog {
        numStepsField.text = TerrainStitcher.numSteps.toString()
        includeWorldHeight.isChecked = TerrainStitcher.includeWorldHeight
        return super.show(stage, action)
    }



    private fun setupListeners() {
        numStepsField.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                TerrainStitcher.numSteps = numStepsField.int
            }
        })

        executeBtn.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                try {
                    TerrainStitcher.stitch(projectManager.current())
                } catch (e: Exception) {
                    Dialogs.showErrorDialog(UI, e.message)
                }
            }
        })

        includeWorldHeight.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                TerrainStitcher.includeWorldHeight = includeWorldHeight.isChecked
            }
        })
    }
}