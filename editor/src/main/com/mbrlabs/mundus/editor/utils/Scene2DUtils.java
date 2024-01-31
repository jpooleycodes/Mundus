package com.mbrlabs.mundus.editor.utils;

import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Button;

/**
 * @author JamesTKhan
 * @version October 05, 2023
 */
public class Scene2DUtils {

    public static void setButtonState(Button button, boolean enable) {
        if (enable) {
            button.setTouchable(Touchable.enabled);
            button.setDisabled(false);
        } else {
            button.setTouchable(Touchable.disabled);
            button.setDisabled(true);
        }
    }
}
