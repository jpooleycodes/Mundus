/*
 * Copyright (c) 2023. See AUTHORS file.
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

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.PropertiesUtils;
import com.mbrlabs.mundus.commons.assets.meta.Meta;

import java.io.IOException;
import java.io.Reader;
import java.util.Map;

/**
 * Holds a reference to Textures for use by TerrainAssets
 */
public class TerrainLayerAsset extends Asset {
    private static final ObjectMap<String, String> MAP = new ObjectMap<>();

    public static final String PROP_SPLAT_BASE = "base";
    public static final String PROP_SPLAT_R = "r";
    public static final String PROP_SPLAT_G = "g";
    public static final String PROP_SPLAT_B = "b";
    public static final String PROP_SPLAT_A = "a";
    public static final String PROP_SPLAT_BASE_NORMAL = "baseNorm";
    public static final String PROP_SPLAT_R_NORMAL = "rNorm";
    public static final String PROP_SPLAT_G_NORMAL = "gNorm";
    public static final String PROP_SPLAT_B_NORMAL = "bNorm";
    public static final String PROP_SPLAT_A_NORMAL = "aNorm";

    private String splatBaseId;
    private String splatRId;
    private String splatGId;
    private String splatBId;
    private String splatAId;
    private String splatBaseNormalId;
    private String splatRNormalId;
    private String splatGNormalId;
    private String splatBNormalId;
    private String splatANormalId;

    private TextureAsset splatBase;
    private TextureAsset splatR;
    private TextureAsset splatG;
    private TextureAsset splatB;
    private TextureAsset splatA;
    private TextureAsset splatBaseNormal;
    private TextureAsset splatRNormal;
    private TextureAsset splatBNormal;
    private TextureAsset splatGNormal;
    private TextureAsset splatANormal;

    /**
     * @param meta the meta file
     * @param assetFile the asset file
     */
    public TerrainLayerAsset(Meta meta, FileHandle assetFile) {
        super(meta, assetFile);
    }

    @Override
    public void load() {
        MAP.clear();

        Reader reader = file.reader();
        try {
            PropertiesUtils.load(MAP, reader);
            reader.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        splatBaseId = MAP.get(PROP_SPLAT_BASE, null);
        splatRId = MAP.get(PROP_SPLAT_R, null);
        splatGId = MAP.get(PROP_SPLAT_G, null);
        splatBId = MAP.get(PROP_SPLAT_B, null);
        splatAId = MAP.get(PROP_SPLAT_A, null);
        splatBaseNormalId = MAP.get(PROP_SPLAT_BASE_NORMAL, null);
        splatRNormalId = MAP.get(PROP_SPLAT_R_NORMAL, null);
        splatGNormalId = MAP.get(PROP_SPLAT_G_NORMAL, null);
        splatBNormalId = MAP.get(PROP_SPLAT_B_NORMAL, null);
        splatANormalId = MAP.get(PROP_SPLAT_A_NORMAL, null);
    }

    @Override
    public void load(AssetManager assetManager) {
        // No async loading for terrain layers
        load();
    }

    @Override
    public void resolveDependencies(Map<String, Asset> assets) {
        if (splatBaseId != null && assets.containsKey(splatBaseId)) {
            setSplatBase((TextureAsset) assets.get(splatBaseId));
        }
        if (splatRId != null && assets.containsKey(splatRId)) {
            setSplatR((TextureAsset) assets.get(splatRId));
        }
        if (splatGId != null && assets.containsKey(splatGId)) {
            setSplatG((TextureAsset) assets.get(splatGId));
        }
        if (splatBId != null && assets.containsKey(splatBId)) {
            setSplatB((TextureAsset) assets.get(splatBId));
        }
        if (splatAId != null && assets.containsKey(splatAId)) {
            setSplatA((TextureAsset) assets.get(splatAId));
        }
        if (splatBaseNormalId != null && assets.containsKey(splatBaseNormalId)) {
            setSplatBaseNormal((TextureAsset) assets.get(splatBaseNormalId));
        }
        if (splatRNormalId != null && assets.containsKey(splatRNormalId)) {
            setSplatRNormal((TextureAsset) assets.get(splatRNormalId));
        }
        if (splatGNormalId != null && assets.containsKey(splatGNormalId)) {
            setSplatGNormal((TextureAsset) assets.get(splatGNormalId));
        }
        if (splatBNormalId != null && assets.containsKey(splatBNormalId)) {
            setSplatBNormal((TextureAsset) assets.get(splatBNormalId));
        }
        if (splatANormalId != null && assets.containsKey(splatANormalId)) {
            setSplatANormal((TextureAsset) assets.get(splatANormalId));
        }
    }

    @Override
    public void applyDependencies() {
        // Nothing to do here
    }

    @Override
    public void dispose() {

    }

    @Override
    public boolean usesAsset(Asset assetToCheck) {
        // Check normal maps
        return assetToCheck == splatBaseNormal || assetToCheck == splatRNormal ||
                assetToCheck == splatBNormal || assetToCheck == splatGNormal || assetToCheck == splatANormal;
    }

    public TextureAsset getSplatBase() {
        return splatBase;
    }

    public void setSplatBase(TextureAsset splatBase) {
        this.splatBase = splatBase;
        this.splatBaseId = splatBase == null ? null : splatBase.getID();
    }

    public TextureAsset getSplatR() {
        return splatR;
    }

    public void setSplatR(TextureAsset splatR) {
        this.splatR = splatR;
        this.splatRId = splatR == null ? null : splatR.getID();
    }

    public TextureAsset getSplatG() {
        return splatG;
    }

    public void setSplatG(TextureAsset splatG) {
        this.splatG = splatG;
        this.splatGId = splatG == null ? null : splatG.getID();
    }

    public TextureAsset getSplatB() {
        return splatB;
    }

    public void setSplatB(TextureAsset splatB) {
        this.splatB = splatB;
        this.splatBId = splatB == null ? null : splatB.getID();
    }

    public TextureAsset getSplatA() {
        return splatA;
    }

    public void setSplatA(TextureAsset splatA) {
        this.splatA = splatA;
        this.splatAId = splatA == null ? null : splatA.getID();
    }

    public TextureAsset getSplatBaseNormal() {
        return splatBaseNormal;
    }

    public void setSplatBaseNormal(TextureAsset splatBaseNormal) {
        this.splatBaseNormal = splatBaseNormal;
        this.splatBaseNormalId = splatBaseNormal == null ? null : splatBaseNormal.getID();
    }

    public TextureAsset getSplatRNormal() {
        return splatRNormal;
    }

    public void setSplatRNormal(TextureAsset splatRNormal) {
        this.splatRNormal = splatRNormal;
        this.splatRNormalId = splatRNormal == null ? null : splatRNormal.getID();
    }

    public TextureAsset getSplatBNormal() {
        return splatBNormal;
    }

    public void setSplatBNormal(TextureAsset splatBNormal) {
        this.splatBNormal = splatBNormal;
        this.splatBNormalId = splatBNormal == null ? null : splatBNormal.getID();
    }

    public TextureAsset getSplatGNormal() {
        return splatGNormal;
    }

    public void setSplatGNormal(TextureAsset splatGNormal) {
        this.splatGNormal = splatGNormal;
        this.splatGNormalId = splatGNormal == null ? null : splatGNormal.getID();
    }

    public TextureAsset getSplatANormal() {
        return splatANormal;
    }

    public void setSplatANormal(TextureAsset splatANormal) {
        this.splatANormal = splatANormal;
        this.splatANormalId = splatANormal == null ? null : splatANormal.getID();
    }

    /**
     * Returns the number of active splat layers
     */
    public int getActiveLayerCount() {
        int count = 0;
        if (splatR != null) {
            count++;
        }
        if (splatG != null) {
            count++;
        }
        if (splatB != null) {
            count++;
        }
        if (splatA != null) {
            count++;
        }
        return count;
    }

    /**
     * Duplicates the given layer asset into this one
     * @param assetToDupe The asset to duplicate
     */
    public void duplicateLayerAsset(TerrainLayerAsset assetToDupe) {
        if (assetToDupe == null) {
            return;
        }
        if (assetToDupe.getSplatBase() != null) setSplatBase(assetToDupe.getSplatBase());
        if (assetToDupe.getSplatR() != null) setSplatR(assetToDupe.getSplatR());
        if (assetToDupe.getSplatG() != null) setSplatG(assetToDupe.getSplatG());
        if (assetToDupe.getSplatB() != null) setSplatB(assetToDupe.getSplatB());
        if (assetToDupe.getSplatA() != null) setSplatA(assetToDupe.getSplatA());

        if (assetToDupe.getSplatBaseNormal() != null) setSplatBaseNormal(assetToDupe.getSplatBaseNormal());
        if (assetToDupe.getSplatRNormal() != null) setSplatRNormal(assetToDupe.getSplatRNormal());
        if (assetToDupe.getSplatGNormal() != null) setSplatGNormal(assetToDupe.getSplatGNormal());
        if (assetToDupe.getSplatBNormal() != null) setSplatBNormal(assetToDupe.getSplatBNormal());
        if (assetToDupe.getSplatANormal() != null) setSplatANormal(assetToDupe.getSplatANormal());
    }

}
