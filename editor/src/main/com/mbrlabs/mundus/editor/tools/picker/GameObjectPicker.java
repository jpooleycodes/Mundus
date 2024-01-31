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

package com.mbrlabs.mundus.editor.tools.picker;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.mbrlabs.mundus.commons.scene3d.GameObject;
import com.mbrlabs.mundus.commons.scene3d.SceneGraph;
import com.mbrlabs.mundus.commons.scene3d.components.Component;
import com.mbrlabs.mundus.editor.core.EditorScene;
import com.mbrlabs.mundus.editor.scene3d.components.PickableComponent;

/**
 * Renders a scene graph to an offscreen FBO, encodes the game object's id in
 * the game object's render color (see GameObjectPickerShader) and does mouse
 * picking by decoding the picked color.
 *
 * See also:
 * http://www.opengl-tutorial.org/miscellaneous/clicking-on-objects/picking-with-an-opengl-hack/
 *
 * @author Marcus Brummer
 * @version 20-02-2016
 */
public class GameObjectPicker extends BasePicker {

    /**
     * Filter to ignore certain components when picking.
     */
    public interface ComponentIgnoreFilter {
        /**
         * Whether to ignore the given component or not.
         * @param component component to check
         * @return true if component should be ignored
         */
        boolean ignore(Component component);
    }

    /**
     * Optional filter which can be used to ignore certain components when picking.
     */
    public ComponentIgnoreFilter ignoreFilter;

    public GameObjectPicker() {
        super();
    }

    public GameObject pick(EditorScene scene, int screenX, int screenY) {
        // Scene not initialized yet
        if (scene.viewport == null) return null;

        begin(scene.viewport);
        renderPickableScene(scene.sceneGraph);
        end();
        Pixmap pm = getFrameBufferPixmap(scene.viewport);

        int x = screenX - scene.viewport.getScreenX();
        int y = screenY - (Gdx.graphics.getHeight() - (scene.viewport.getScreenY() + scene.viewport.getScreenHeight()));

        int id = PickerColorEncoder.decode(pm.getPixel(x, y));
        pm.dispose();

        for (GameObject go : scene.sceneGraph.getGameObjects()) {
            if (id == go.id) return go;
            for (GameObject child : go) {
                if (id == child.id) return child;
            }
        }

        return null;
    }

    private void renderPickableScene(SceneGraph sceneGraph) {
        sceneGraph.scene.batch.begin(sceneGraph.scene.cam);
        renderPickableGameObject(sceneGraph.getRoot());
        sceneGraph.scene.batch.end();
    }

    private void renderPickableGameObject(GameObject go) {
        for (Component c : go.getComponents()) {
            if (c instanceof PickableComponent) {
                if (ignoreFilter != null && ignoreFilter.ignore(c)) continue;
                ((PickableComponent) c).renderPick();
            }
        }

        if (go.getChildren() != null) {
            for (GameObject child : go.getChildren()) {
                if (!child.active) continue;
                renderPickableGameObject(child);
            }
        }
    }

    /**
     * Sets a filter which can be used to ignore certain components when rendering for picking.
     * @param filter the filter to set
     */
    public void setIgnoreFilter(ComponentIgnoreFilter filter) {
        this.ignoreFilter = filter;
    }

    /**
     * Clears the ignore filter.
     */
    public void clearIgnoreFilter() {
        this.ignoreFilter = null;
    }

}
