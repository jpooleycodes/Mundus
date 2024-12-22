package com.mbrlabs.mundus.editor.ui.widgets

import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.kotcrab.vis.ui.util.dialog.Dialogs
import com.kotcrab.vis.ui.util.dialog.InputDialogAdapter
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisTextButton
import com.mbrlabs.mundus.commons.assets.Asset
import com.mbrlabs.mundus.commons.assets.MaterialAsset
import com.mbrlabs.mundus.editor.Mundus
import com.mbrlabs.mundus.editor.assets.AssetMaterialFilter
import com.mbrlabs.mundus.editor.core.project.ProjectManager
import com.mbrlabs.mundus.editor.events.AssetSelectedEvent
import com.mbrlabs.mundus.editor.events.MaterialDuplicatedEvent
import com.mbrlabs.mundus.editor.ui.UI
import com.mbrlabs.mundus.editor.ui.modules.dialogs.assets.AssetPickerDialog
import com.mbrlabs.mundus.editor.utils.Colors
import com.mbrlabs.mundus.editorcommons.exceptions.AssetAlreadyExistsException
import java.io.FileNotFoundException

/**
 * Displays a materials name and allows the user to change/edit/duplicate it.
 * Use the [matChangedListener] to get notified when the material has changed.
 *
 * @author JamesTKhan
 * @version June 02, 2023
 */
class MaterialSelectWidget(var material: MaterialAsset?) : VisTable() {
    private val matFilter: AssetMaterialFilter = AssetMaterialFilter()
    private val matEditBtn: VisTextButton = VisTextButton("Edit")
    private val matDuplicatedBtn: VisTextButton = VisTextButton("Duplicate")
    private val matChangedBtn: VisTextButton = VisTextButton("Change")
    private val matPickerListener: AssetPickerDialog.AssetPickerListener
    private val matNameLabel: VisLabel = VisLabel()
    private val projectManager: ProjectManager = Mundus.inject()
    private val root: VisTable = VisTable()

    /**
     * An optional listener for changing the material. If the property is null
     * the user will not be able to change the material.
     */
    var matChangedListener: MaterialWidget.MaterialChangedListener? = null
        set(value) {
            field = value
            matChangedBtn.touchable = if(value == null) Touchable.disabled else Touchable.enabled
        }

    init {
        setupUI()
        matPickerListener = object: AssetPickerDialog.AssetPickerListener {
            override fun onSelected(asset: Asset?) {
                material = (asset as? MaterialAsset)!!
                matChangedListener?.materialChanged(material!!)
                updateUI()
            }
        }

        matDuplicatedBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                Dialogs.showInputDialog(UI, "Name:", "", object : InputDialogAdapter() {
                    override fun finished(input: String?) {
                        if (input != null) {
                            try {
                                val newMaterial = projectManager.current().assetManager.createMaterialAsset(input)
                                if (material != null) {
                                    newMaterial.duplicateMaterialAsset(material)
                                }
                                material = newMaterial
                                updateUI()
                                matChangedListener?.materialChanged(material!!)
                                Mundus.postEvent(MaterialDuplicatedEvent())
                            } catch (e: AssetAlreadyExistsException) {
                                Dialogs.showErrorDialog(UI, "That material already exists. Try a different name.")
                            } catch (e: FileNotFoundException) {
                                Dialogs.showErrorDialog(UI, "Invalid material name.")
                            }
                        }
                    }
                })
            }
        })

        matChangedBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                UI.assetSelectionDialog.show(false, matFilter, matPickerListener)
            }
        })

        matEditBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                Mundus.postEvent(AssetSelectedEvent(material!!))
                UI.docker.assetsDock.setSelected(material!!)
            }
        })
    }

    private fun setupUI() {
        matNameLabel.color = Colors.TEAL
        matNameLabel.wrap = true

        add(root).grow().row()
        root.add(matNameLabel).grow()
        root.add(matEditBtn)
        root.add(matDuplicatedBtn).padLeft(4f).right()
        root.add(matChangedBtn).padLeft(4f).right().row()
        updateUI()
    }

    private fun updateUI() {
        matNameLabel.setText(if (material == null) "None Selected" else material?.name)
        matEditBtn.touchable = if (material == null) Touchable.disabled else Touchable.enabled
        matDuplicatedBtn.touchable = if (material == null) Touchable.disabled else Touchable.enabled
        matEditBtn.isDisabled = material == null
        matDuplicatedBtn.isDisabled = material == null
    }
}