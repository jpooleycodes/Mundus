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
package com.mbrlabs.mundus.editor.tools;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.mbrlabs.mundus.commons.scene3d.GameObject;
import com.mbrlabs.mundus.commons.scene3d.components.Component;
import com.mbrlabs.mundus.commons.scene3d.components.ModelComponent;
import com.mbrlabs.mundus.commons.scene3d.components.TerrainComponent;
import com.mbrlabs.mundus.commons.scene3d.components.WaterComponent;
import com.mbrlabs.mundus.commons.utils.DebugRenderer;
import com.mbrlabs.mundus.editor.Mundus;
import com.mbrlabs.mundus.editor.core.project.ProjectManager;
import com.mbrlabs.mundus.editor.events.GameObjectSelectedEvent;
import com.mbrlabs.mundus.editor.history.CommandHistory;
import com.mbrlabs.mundus.editor.preferences.MundusPreferencesManager;
import com.mbrlabs.mundus.editor.tools.picker.GameObjectPicker;
import com.mbrlabs.mundus.editor.utils.Fa;

/**
 * @author Marcus Brummer
 * @version 26-12-2015
 */
public class SelectionTool extends Tool {

    public static final String NAME = "Selection Tool";

    private final GameObjectPicker goPicker;
    private final MundusPreferencesManager globalPreferencesManager;

    public SelectionTool(final ProjectManager projectManager,
                         final GameObjectPicker goPicker,
                         final CommandHistory history,
                         final MundusPreferencesManager globalPreferencesManager) {
        super(projectManager, history);
        this.goPicker = goPicker;
        this.globalPreferencesManager = globalPreferencesManager;
    }

    public void gameObjectSelected(GameObject selection) {
        getProjectManager().current().currScene.currentSelection = selection;
        DebugRenderer.setSelectedGameObject(selection);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Drawable getIcon() {
        return null;
    }

    @Override
    public String getIconFont() {
        return Fa.Companion.getMOUSE_POINTER();
    }

    @Override
    public void render() {
        if (getProjectManager().current().currScene.currentSelection != null) {
            Gdx.gl.glLineWidth(globalPreferencesManager.getFloat(MundusPreferencesManager.GLOB_LINE_WIDTH_SELECTION, MundusPreferencesManager.GLOB_LINE_WIDTH_DEFAULT_VALUE));
            getProjectManager().getModelBatch().begin(getProjectManager().current().currScene.cam);
            for (GameObject go : getProjectManager().current().currScene.currentSelection) {
                // model component
                ModelComponent mc = go.findComponentByType(Component.Type.MODEL);
                if (mc != null) {
                    getProjectManager().getModelBatch().render(mc.getModelInstance(), getShader());
                }

                // terrainAsset component
                TerrainComponent tc = go.findComponentByType(Component.Type.TERRAIN);
                if (tc != null) {
                    getProjectManager().getModelBatch().render(tc.getModelInstance(), getShader());
                }

                // waterAsset component
                WaterComponent wc = go.findComponentByType(Component.Type.WATER);
                if (wc != null) {
                    getProjectManager().getModelBatch().render(wc.getWaterAsset().water, getShader());
                }
            }
            getProjectManager().getModelBatch().end();
            Gdx.gl.glLineWidth(MundusPreferencesManager.GLOB_LINE_WIDTH_DEFAULT_VALUE);
        }
    }

    @Override
    public void act() {

    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (button == getSelectButtonId()) {
            GameObject selection = goPicker.pick(getProjectManager().current().currScene, screenX, screenY);
            if (selection != null && !selection.equals(getProjectManager().current().currScene.currentSelection)) {
                gameObjectSelected(selection);
                Mundus.INSTANCE.postEvent(new GameObjectSelectedEvent(selection, true));
            }
        }

        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public void dispose() {

    }

    @Override
    public void onActivated() {

    }

    @Override
    public void onDisabled() {
        getProjectManager().current().currScene.currentSelection = null;
    }

    protected boolean isSelectWithRightButton() {
        return globalPreferencesManager.getBoolean(MundusPreferencesManager.GLOB_RIGHT_BUTTON_SELECT, MundusPreferencesManager.GLOB_RIGHT_SELECT_BUTTON_DEFAULT_VALUE);
    }

    private int getSelectButtonId() {
        return isSelectWithRightButton() ? Input.Buttons.RIGHT : Input.Buttons.LEFT;
    }

}
