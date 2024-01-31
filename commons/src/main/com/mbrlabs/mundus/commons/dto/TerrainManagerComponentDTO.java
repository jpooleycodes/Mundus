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

package com.mbrlabs.mundus.commons.dto;

import com.mbrlabs.mundus.commons.scene3d.components.TerrainManagerComponent;

/**
 * @author JamesTKhan
 * @version July 17, 2023
 */
public class TerrainManagerComponentDTO {

    private TerrainManagerComponent.ProceduralGeneration proceduralGeneration;

    public TerrainManagerComponentDTO() {
    }

    public TerrainManagerComponentDTO(final TerrainManagerComponent.ProceduralGeneration proceduralGeneration) {
        this.proceduralGeneration = proceduralGeneration;
    }

    public TerrainManagerComponent.ProceduralGeneration getProceduralGeneration() {
        return proceduralGeneration;
    }

    public void setProceduralGeneration(final TerrainManagerComponent.ProceduralGeneration proceduralGeneration) {
        this.proceduralGeneration = proceduralGeneration;
    }
}
