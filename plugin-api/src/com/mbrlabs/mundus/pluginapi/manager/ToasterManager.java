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

package com.mbrlabs.mundus.pluginapi.manager;

import com.mbrlabs.mundus.editorcommons.types.ToastType;

public interface ToasterManager {

    /**
     * Shows toaster message as info status for some seconds.
     *
     * @param text The text.
     */
    void info(String text);

    /**
     * Shows toaster message as error status for some seconds.
     *
     * @param text The text.
     */
    void error(String text);

    /**
     * Shows toaster message as success status for some seconds.
     *
     * @param text The text.
     */
    void success(String text);

    /**
     * Shows toaster message until the user close it.
     *
     * @param type The type of text.
     * @param text The text.
     */
    void sticky(ToastType type, String text);

}
