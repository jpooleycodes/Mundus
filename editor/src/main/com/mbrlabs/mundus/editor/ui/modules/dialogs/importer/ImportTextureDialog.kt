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

package com.mbrlabs.mundus.editor.ui.modules.dialogs.importer

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Disposable
import com.kotcrab.vis.ui.util.dialog.Dialogs
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisTextButton
import com.mbrlabs.mundus.editor.Mundus
import com.mbrlabs.mundus.editor.core.project.ProjectManager
import com.mbrlabs.mundus.editor.events.AssetImportEvent
import com.mbrlabs.mundus.editor.events.FilesDroppedEvent
import com.mbrlabs.mundus.editor.ui.UI
import com.mbrlabs.mundus.editor.ui.modules.dialogs.BaseDialog
import com.mbrlabs.mundus.editor.ui.widgets.ImageChooserField
import com.mbrlabs.mundus.editor.utils.ImageUtils
import com.mbrlabs.mundus.editor.utils.Log
import com.mbrlabs.mundus.editor.utils.isImage
import com.mbrlabs.mundus.editorcommons.exceptions.AssetAlreadyExistsException
import java.io.File
import java.io.IOException

/**
 * @author Marcus Brummer
 * @version 07-06-2016
 */
class ImportTextureDialog : BaseDialog("Import Texture"), FilesDroppedEvent.FilesDroppedListener, ImageChooserField.ImageChosenListener, Disposable {

    companion object {
        private val TAG = ImportTextureDialog::class.java.simpleName
    }

    private val importTextureTable: ImportTextureTable

    private val projectManager: ProjectManager = Mundus.inject()

    init {
        Mundus.registerEventListener(this)

        isModal = true
        isMovable = true

        val root = VisTable()
        add<Table>(root).expand().fill()
        importTextureTable = ImportTextureTable(this)

        root.add("Drag and drop import is supported.").align(Align.center).row()
        root.add("Preview Mode only supported for single texture selection.").align(Align.center).row()
        root.add(importTextureTable).minWidth(300f).expand().fill().left().top()
    }

    override fun dispose() {
        importTextureTable.dispose()
    }

    override fun close() {
        super.close()
        importTextureTable.removeTexture()
    }

    /**

     */
    private inner class ImportTextureTable(listener: ImageChooserField.ImageChosenListener) : VisTable(), Disposable {
        // UI elements
        private val importBtn = VisTextButton("IMPORT")
        private val imageChooserField = ImageChooserField(300, true, listener)

        init {
            this.setupUI()
            this.setupListener()

            align(Align.topLeft)
        }

        private fun setupUI() {
            padTop(6f).padRight(6f).padBottom(22f)
            add(imageChooserField).grow().row()
            add(importBtn).grow().row()
        }

        private fun setupListener() {
            importBtn.addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    try {
                        val texture = imageChooserField.file
                        if (texture != null && texture.exists() && isImage(texture)) {
                            val assetManager = projectManager.current().assetManager
                            val asset = assetManager.createTextureAsset(texture)
                            Mundus.postEvent(AssetImportEvent(asset))
                            close()
                            UI.toaster.success("Texture imported")
                        } else {
                            UI.toaster.error("There is nothing to import")
                        }
                    } catch (e: IOException) {
                        Log.exception(TAG, e)
                        UI.toaster.error("IO error")
                    } catch (ee: AssetAlreadyExistsException) {
                        Log.exception(TAG, ee)
                        UI.toaster.error("Error: There already exists a texture with the same name")
                    }

                }
            })
        }

        fun getImageChooserField(): ImageChooserField {
            return imageChooserField
        }

        fun removeTexture() {
            imageChooserField.removeImage()
        }

        override fun dispose() {

        }
    }

    override fun onFilesDropped(event: FilesDroppedEvent) {
        if (!dialogOpen) return
        if (event.files == null || event.files.isEmpty()) return

        if (event.files.size == 1) {
            val fileHandle = FileHandle(File(event.files[0]))
            val errorMessage = ImageUtils.validateImageFile(fileHandle)

            if (errorMessage == null) {
                importTextureTable.getImageChooserField().setImage(fileHandle)
            } else {
                Dialogs.showErrorDialog(stage, errorMessage)
            }
            return
        }

        // <File Name, Error Message>
        val failedFiles = HashMap<FileHandle, String>()
        val files = Array<FileHandle>()

        for (filePath in event.files) {
            val fileHandle = FileHandle(File(filePath))
            val errorMessage = ImageUtils.validateImageFile(fileHandle)
            if (errorMessage == null) {
                files.add(fileHandle)
            } else {
                failedFiles[fileHandle] = errorMessage
            }
        }

        importFiles(files, failedFiles)
    }

    private fun importFiles(files: Array<FileHandle>, failedFiles: HashMap<FileHandle, String>) {
        for (file in files) {
            try {
                val assetManager = projectManager.current().assetManager
                val asset = assetManager.createTextureAsset(file)
                Mundus.postEvent(AssetImportEvent(asset))
            } catch (ee: AssetAlreadyExistsException) {
                Log.exception(TAG, ee)
                failedFiles[file] = "There already exists a texture with the same name"
            }
        }

        if (failedFiles.isEmpty()) {
            close()
            UI.toaster.success("Textures imported")
            return
        }

        val dialogMessage = buildString {
            append("The following could not be imported\n\n")
            for (file in failedFiles) {
                append(file.key.name())
                append(" : ")
                append(file.value)
                append("\n")
            }
        }

        Dialogs.showErrorDialog(stage, dialogMessage)
        close()
    }

    override fun onImageChosen() {
        // Unused
    }

    override fun onImagesChosen(
        images: Array<FileHandle>,
        failedFiles: HashMap<FileHandle, String>
    ) {
        importFiles(images, failedFiles)
    }

}
