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

package com.mbrlabs.mundus.commons.mapper;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.OrderedMap;
import com.mbrlabs.mundus.commons.assets.Asset;
import com.mbrlabs.mundus.commons.scene3d.GameObject;
import com.mbrlabs.mundus.commons.scene3d.components.Component;

public interface CustomComponentConverter {

    /**
     * @return The component type of custom component.
     */
    Component.Type getComponentType();

    /**
     * Converts component into map for persisting.
     *
     * @param component The component.
     * @return The map.
     */
    OrderedMap<String, String> convert(Component component);

    /**
     * @param component The component.
     *
     * @return The ID of used assets.
     */
    default Array<String> getAssetIds(Component component) {
        return null;
    }

    /**
     * Converts map into custom component.
     *
     * @param gameObject The game object.
     * @param componentProperties The properties of custom component.
     * @param assets The assets of component.
     * @return The custom component.
     */
    Component convert(GameObject gameObject, OrderedMap<String, String> componentProperties, ObjectMap<String, Asset> assets);
}
