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

package com.mbrlabs.mundus.commons.assets;

/**
 * @author Marcus Brummer
 * @version 01-10-2016
 */
public enum AssetType {
    /** Texture type. Can be pretty much any type of image. */
    TEXTURE("Texture"),
    /** A Texture, backed by a pixmap. Can be pretty much any type of image. */
    PIXMAP_TEXTURE("Pixmap Texture"),
    /** 3D file. Can be g3db, g3dbj, dae, obj, fbx. */
    MODEL("Model"),
    /** Terra file. Contains height data for terrains. */
    TERRAIN("Terrain"),
    /** Material file. Mundus material file contains material information. */
    MATERIAL("Material"),
    /** Water file. Contains data for water. */
    WATER("Water"),
    /** Skybox file. Holds reference to skybox textures. **/
    SKYBOX("Skybox"),
    /** Holds the terrain textures to be used. **/
    TERRAIN_LAYER("Terrain Layer");

    private final String value;

    AssetType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static AssetType valueFromString(String string) {
        for (AssetType res : values()) {
            if (res.value.equals(string)) {
                return res;
            }
        }

        return null;
    }

}
