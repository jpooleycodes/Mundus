/*
 * Copyright (c) 2022. See AUTHORS file.
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

package com.mbrlabs.mundus.commons.physics.enums;

/**
 * @author James Pooley
 * @version July 03, 2022
 */
public enum PhysicsShape {
    // Dynamic/Kinematic
    BOX("Box"),
    SPHERE("Sphere"),
    CAPSULE("Capsule"),
    CYLINDER("Cylinder"),
    CONE("Cone"),
    CONVEX_HULL("Convex Hull"),
    G_IMPACT_TRIANGLE_MESH("Gimpact Mesh"),
    // Static
    TERRAIN("Terrain Heightfield"),
    SCALED_BVH_TRIANGLE("Scaled BVH Triangle"),
    BVH_TRIANGLE("BVH Triangle");

    private final String value;

    PhysicsShape(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static PhysicsShape valueFromString(String value) {
        for (PhysicsShape preset : values()) {
            if (preset.getValue().equals(value)) {
                return preset;
            }
        }

        return null;
    }

}
