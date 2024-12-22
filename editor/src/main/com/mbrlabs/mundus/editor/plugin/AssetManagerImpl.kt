/*
 * Copyright (c) 2024. See AUTHORS file.
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

package com.mbrlabs.mundus.editor.plugin

import com.badlogic.gdx.files.FileHandle
import com.mbrlabs.mundus.commons.assets.Asset
import com.mbrlabs.mundus.commons.assets.CustomAsset
import com.mbrlabs.mundus.editor.Mundus
import com.mbrlabs.mundus.editor.core.project.ProjectManager
import com.mbrlabs.mundus.editor.events.AssetDeletedEvent
import com.mbrlabs.mundus.editor.events.AssetImportEvent
import com.mbrlabs.mundus.editorcommons.exceptions.AssetAlreadyExistsException
import com.mbrlabs.mundus.pluginapi.manager.AssetManager

class AssetManagerImpl : AssetManager {

    private val projectManager = Mundus.inject<ProjectManager>()

    @Throws(AssetAlreadyExistsException::class)
    override fun createNewAsset(file: FileHandle): CustomAsset {
        val assetManager = projectManager.current().assetManager
        val customAsset = assetManager.createCustomAsset(file)
        Mundus.postEvent(AssetImportEvent(customAsset))
        return customAsset
    }

    override fun markAsModifiedAsset(asset: Asset) {
        projectManager.current().assetManager.addModifiedAsset(asset)
    }

    override fun deleteAsset(asset: CustomAsset) {
        projectManager.current().assetManager.deleteAsset(asset)
        Mundus.postEvent(AssetDeletedEvent())
    }
}
