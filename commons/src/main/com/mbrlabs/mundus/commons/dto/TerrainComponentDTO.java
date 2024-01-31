/*
 * Copyright (c) 2021. See AUTHORS file.
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

package com.mbrlabs.mundus.commons.dto;

import com.mbrlabs.mundus.commons.assets.Asset;
import com.mbrlabs.mundus.commons.assets.TerrainAsset;

import java.util.Map;

/**
 * @author Tibor Zsuro
 * @version 12-08-2021
 */
public class TerrainComponentDTO implements AssetUsageDTO {

    private String terrainID;

    // Neighbor terrain's game object IDs
    private Integer topNeighborID;
    private Integer rightNeighborID;
    private Integer bottomNeighborID;
    private Integer leftNeighborID;


    public String getTerrainID() {
        return terrainID;
    }

    public void setTerrainID(String id) {
        this.terrainID = id;
    }

    public Integer getTopNeighborID() {
        return topNeighborID;
    }

    public void setTopNeighborID(Integer topNeighborID) {
        this.topNeighborID = topNeighborID;
    }

    public Integer getRightNeighborID() {
        return rightNeighborID;
    }

    public void setRightNeighborID(Integer rightNeighborID) {
        this.rightNeighborID = rightNeighborID;
    }

    public Integer getBottomNeighborID() {
        return bottomNeighborID;
    }

    public void setBottomNeighborID(Integer bottomNeighborID) {
        this.bottomNeighborID = bottomNeighborID;
    }

    public Integer getLeftNeighborID() {
        return leftNeighborID;
    }

    public void setLeftNeighborID(Integer leftNeighborID) {
        this.leftNeighborID = leftNeighborID;
    }

    @Override
    public boolean usesAsset(Asset assetToCheck, Map<String, Asset> assetMap) {
        if (assetToCheck.getID().equals(terrainID)) {
            return true;
        }

        TerrainAsset terrainAsset = (TerrainAsset) assetMap.get(terrainID);
        if (assetToCheck == terrainAsset.getMaterialAsset()){
            return true;
        }

        return terrainAsset.usesAsset(assetToCheck);
    }
}
