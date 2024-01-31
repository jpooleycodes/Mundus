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

/**
 *
 * @author Marcus Brummer
 * @version 26-10-2016
 */
public class MetaTerrain {

    public static final String JSON_SIZE = "size";
    public static final String JSON_SPLATMAP = "map";
    public static final String JSON_SPLATMAP_RESOLUTION = "mapRes";
    public static final String JSON_SPLAT_BASE = "base";
    public static final String JSON_SPLAT_BASE64 = "base64";
    public static final String JSON_SPLAT_R = "r";
    public static final String JSON_SPLAT_G = "g";
    public static final String JSON_SPLAT_B = "b";
    public static final String JSON_SPLAT_A = "a";
    public static final String JSON_SPLAT_BASE_NORMAL = "baseNorm";
    public static final String JSON_SPLAT_R_NORMAL = "rNorm";
    public static final String JSON_SPLAT_G_NORMAL = "gNorm";
    public static final String JSON_SPLAT_B_NORMAL = "bNorm";
    public static final String JSON_SPLAT_A_NORMAL = "aNorm";
    public static final String JSON_UV_SCALE= "uv";
    public static final String JSON_TRIPLANAR = "triplanar";
    public static final String JSON_MATERIAL = "material";
    public static final String JSON_LAYER = "layer";

    private int size;
    private int splatMapResolution;
    private float uv;
    private boolean triplanar;
    private String materialId;
    private String splatmap;
    private String splatBase;
    private String splatBase64;
    private String splatR;
    private String splatG;
    private String splatB;
    private String splatA;
    private String splatBaseNormal;
    private String splatRNormal;
    private String splatGNormal;
    private String splatBNormal;
    private String splatANormal;
    private String terrainLayerAssetId;

    public String getSplatmap() {
        return splatmap;
    }

    public String getTerrainLayerAssetId() {
        return terrainLayerAssetId;
    }

    public void setSplatmap(String splatmap) {
        this.splatmap = splatmap;
    }

    /**
     * @deprecated use {@link MetaTerrainLayer)} instead
     */
    @Deprecated
    public String getSplatBase() {
        return splatBase;
    }

    /**
     * @deprecated use {@link MetaTerrainLayer)} instead
     */
    @Deprecated
    public void setSplatBase(String splatBase) {
        this.splatBase = splatBase;
    }

    public String getSplatBase64() {
        return splatBase64;
    }

    public void setSplatBase64(String splatBase64) {
        this.splatBase64 = splatBase64;
    }

    /**
     * @deprecated use {@link MetaTerrainLayer)} instead
     */
    @Deprecated
    public String getSplatR() {
        return splatR;
    }

    /**
     * @deprecated use {@link MetaTerrainLayer)} instead
     */
    public void setSplatR(String splatR) {
        this.splatR = splatR;
    }

    /**
     * @deprecated use {@link MetaTerrainLayer} ()} instead
     */
    public String getSplatG() {
        return splatG;
    }

    /**
     * @deprecated use {@link MetaTerrainLayer)} ()} instead
     */
    public void setSplatG(String splatG) {
        this.splatG = splatG;
    }

    /**
     * @deprecated use {@link MetaTerrainLayer)} instead
     */
    public String getSplatB() {
        return splatB;
    }

    /**
     * @deprecated use {@link MetaTerrainLayer)} instead
     */
    public void setSplatB(String splatB) {
        this.splatB = splatB;
    }

    /**
     * @deprecated use {@link MetaTerrainLayer)} instead
     */
    public String getSplatA() {
        return splatA;
    }

    /**
     * @deprecated use {@link MetaTerrainLayer)} instead
     */
    public void setSplatA(String splatA) {
        this.splatA = splatA;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getSplatMapResolution() {
        return splatMapResolution;
    }

    public void setSplatMapResolution(int splatMapResolution) {
        this.splatMapResolution = splatMapResolution;
    }

    public float getUv() {
        return uv;
    }

    public void setTerrainLayerAssetId(String terrainLayerAssetId) {
        this.terrainLayerAssetId = terrainLayerAssetId;
    }

    public void setUv(float uv) {
        this.uv = uv;
    }

    public boolean isTriplanar() {
        return triplanar;
    }

    public void setTriplanar(boolean triplanar) {
        this.triplanar = triplanar;
    }

    /**
     * @deprecated use {@link MetaTerrainLayer)} instead
     */
    public String getSplatBaseNormal() {
        return splatBaseNormal;
    }

    /**
     * @deprecated use {@link MetaTerrainLayer)} instead
     */
    public void setSplatBaseNormal(String splatBaseNormal) {
        this.splatBaseNormal = splatBaseNormal;
    }

    /**
     * @deprecated use {@link MetaTerrainLayer)} instead
     */
    public String getSplatRNormal() {
        return splatRNormal;
    }

    /**
     * @deprecated use {@link MetaTerrainLayer)} instead
     */
    public void setSplatRNormal(String splatRNormal) {
        this.splatRNormal = splatRNormal;
    }

    /**
     * @deprecated use {@link MetaTerrainLayer)} instead
     */
    public String getSplatGNormal() {
        return splatGNormal;
    }

    /**
     * @deprecated use {@link MetaTerrainLayer)} instead
     */
    public void setSplatGNormal(String splatGNormal) {
        this.splatGNormal = splatGNormal;
    }

    /**
     * @deprecated use {@link MetaTerrainLayer)} instead
     */
    public String getSplatBNormal() {
        return splatBNormal;
    }

    /**
     * @deprecated use {@link MetaTerrainLayer)} instead
     */
    public void setSplatBNormal(String splatBNormal) {
        this.splatBNormal = splatBNormal;
    }

    /**
     * @deprecated use {@link MetaTerrainLayer)} instead
     */
    public String getSplatANormal() {
        return splatANormal;
    }

    /**
     * @deprecated use {@link MetaTerrainLayer)} instead
     */
    public void setSplatANormal(String splatANormal) {
        this.splatANormal = splatANormal;
    }

    public String getMaterialId() {
        return materialId;
    }

    public void setMaterialId(String materialId) {
        this.materialId = materialId;
    }

    @Override
    public String toString() {
        return "MetaTerrain{" +
                "size=" + size +
                ", uv=" + uv +
                ", triplanar=" + triplanar +
                ", material='" + materialId + '\'' +
                ", splatmap='" + splatmap + '\'' +
                ", splatBase='" + splatBase + '\'' +
                ", splatBase64='" + splatBase64 + '\'' +
                ", splatR='" + splatR + '\'' +
                ", splatG='" + splatG + '\'' +
                ", splatB='" + splatB + '\'' +
                ", splatA='" + splatA + '\'' +
                ", splatBaseNormal='" + splatBaseNormal + '\'' +
                ", splatRNormal='" + splatRNormal + '\'' +
                ", splatGNormal='" + splatGNormal + '\'' +
                ", splatBNormal='" + splatBNormal + '\'' +
                ", splatANormal='" + splatANormal + '\'' +
                '}';
    }
}
