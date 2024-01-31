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

package com.mbrlabs.mundus.commons.assets.meta;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.mbrlabs.mundus.commons.assets.AssetType;
import com.mbrlabs.mundus.commons.terrain.SplatMapResolution;
import com.mbrlabs.mundus.commons.terrain.Terrain;

/**
 *
 * @author Marcus Brummer
 * @version 26-10-2016
 */
public class MetaLoader {

    private final JsonReader reader = new JsonReader();

    public Meta load(FileHandle file) throws MetaFileParseException {
        Meta meta = new Meta(file);

        JsonValue json = reader.parse(file);
        parseBasics(meta, json);

        if(meta.getType() == AssetType.TERRAIN) {
            parseTerrain(meta, json.get(Meta.JSON_TERRAIN));
        } else if(meta.getType() == AssetType.MODEL) {
            parseModel(meta, json.get(Meta.JSON_MODEL));
        }

        return meta;
    }

    private void parseBasics(Meta meta, JsonValue jsonRoot) {
        meta.setVersion(jsonRoot.getInt(Meta.JSON_VERSION));
        meta.setLastModified(jsonRoot.getLong(Meta.JSON_LAST_MOD));
        meta.setUuid(jsonRoot.getString(Meta.JSON_UUID));
        meta.setType(AssetType.valueOf(jsonRoot.getString(Meta.JSON_TYPE)));
    }

    private void parseTerrain(Meta meta, JsonValue jsonTerrain) {
        if(jsonTerrain == null) return;

        final MetaTerrain terrain = new MetaTerrain();
        terrain.setSize(jsonTerrain.getInt(MetaTerrain.JSON_SIZE));
        terrain.setSplatMapResolution(jsonTerrain.getInt(MetaTerrain.JSON_SPLATMAP_RESOLUTION, SplatMapResolution.DEFAULT_RESOLUTION.getResolutionValues()));
        terrain.setUv(jsonTerrain.getFloat(MetaTerrain.JSON_UV_SCALE, Terrain.DEFAULT_UV_SCALE));
        terrain.setTriplanar(jsonTerrain.getBoolean(MetaTerrain.JSON_TRIPLANAR, false));
        terrain.setSplatmap(jsonTerrain.getString(MetaTerrain.JSON_SPLATMAP, null));
        terrain.setSplatBase64(jsonTerrain.getString(MetaTerrain.JSON_SPLAT_BASE64, null));
        terrain.setTerrainLayerAssetId(jsonTerrain.getString(MetaTerrain.JSON_LAYER, null));

        // Deprecated with TerrainLayer feature, left for Backward compatibility for the time being
        terrain.setSplatBase(jsonTerrain.getString(MetaTerrain.JSON_SPLAT_BASE, null));
        terrain.setSplatR(jsonTerrain.getString(MetaTerrain.JSON_SPLAT_R, null));
        terrain.setSplatG(jsonTerrain.getString(MetaTerrain.JSON_SPLAT_G, null));
        terrain.setSplatB(jsonTerrain.getString(MetaTerrain.JSON_SPLAT_B, null));
        terrain.setSplatA(jsonTerrain.getString(MetaTerrain.JSON_SPLAT_A, null));
        terrain.setSplatBaseNormal(jsonTerrain.getString(MetaTerrain.JSON_SPLAT_BASE_NORMAL, null));
        terrain.setSplatRNormal(jsonTerrain.getString(MetaTerrain.JSON_SPLAT_R_NORMAL, null));
        terrain.setSplatGNormal(jsonTerrain.getString(MetaTerrain.JSON_SPLAT_G_NORMAL, null));
        terrain.setSplatBNormal(jsonTerrain.getString(MetaTerrain.JSON_SPLAT_B_NORMAL, null));
        terrain.setSplatANormal(jsonTerrain.getString(MetaTerrain.JSON_SPLAT_A_NORMAL, null));

        meta.setTerrain(terrain);
    }

    private void parseModel(Meta meta, JsonValue jsonModel) {
        if(jsonModel == null) return;

        final MetaModel model = new MetaModel();

        int numBones = readWithDefault(jsonModel, MetaModel.JSON_NUM_BONES, 0);
        model.setNumBones(numBones);

        final JsonValue materials = jsonModel.get(MetaModel.JSON_DEFAULT_MATERIALS);

        for(final JsonValue mat : materials) {
            final String g3dbID = mat.name;
            final String assetUUID = materials.getString(g3dbID);
            model.getDefaultMaterials().put(g3dbID, assetUUID);
        }

        meta.setModel(model);
    }

    /**
     * When new values are added and cannot be found on jsonValue.getXXX(),
     * an IllegalArgumentException is thrown.
     *
     * To try and maintain backwards compatibility between meta changes,
     * if we have a default value, that can be used with this method to
     * default to it when it could not be found in the meta during parsing.
     *
     * @param jsonValue the JsonValue instance
     * @param jsonKey the jsonKey value to try and read
     * @param defaultValue the value to default to if jsonKey not found
     * @return float from meta file, or default if not found
     */
    private float readWithDefault(JsonValue jsonValue, String jsonKey, float defaultValue) {
        try {
            return jsonValue.getFloat(jsonKey);
        } catch (IllegalArgumentException ex) {
            return defaultValue;
        }
    }

    private int readWithDefault(JsonValue jsonValue, String jsonKey, int defaultValue) {
        try {
            return jsonValue.getInt(jsonKey);
        } catch (IllegalArgumentException ex) {
            return defaultValue;
        }
    }

}
