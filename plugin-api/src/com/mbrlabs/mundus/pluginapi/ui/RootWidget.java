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

package com.mbrlabs.mundus.pluginapi.ui;

import com.badlogic.gdx.utils.Array;

/**
 * The root wigdet for plugin.
 */
public interface RootWidget {

    /**
     * Adds label.
     * @param text The text of label.
     * @return The created widget.
     */
    LabelCell addLabel(String text);

    /**
     * Adds text field.
     * @param listener The listener for text field.
     * @return The created text field.
     */
    Cell addTextField(TextFieldChangeListener listener);

    /**
     * Adds text button.
     *
     * @param text The ext of button.
     * @param listener The listener for button.
     * @return The created widget.
     */
    Cell addTextButton(String text, ButtonListener listener);

    /**
     * Adds radio buttons. The first button will be selected.
     *
     * @param button1Text The text for first radio button.
     * @param button2Text The text for second radio button.
     * @param listener The listener for radio buttons.
     * @return The created widget.
     */
    Cell addRadioButtons(String button1Text, String button2Text, RadioButtonListener listener);

    /**
     * Adds radio buttons.
     *
     * @param button1Text The text for first radio button.
     * @param button2Text The text for second radio button.
     * @param selectedFirst If true then the first button will be selected otherwise second button will be selected.
     * @param listener The listener for radio buttons.
     * @return The created widget.
     */
    Cell addRadioButtons(String button1Text, String button2Text, boolean selectedFirst, RadioButtonListener listener);

    /**
     * Adds integer spinner with 1 step value.
     *
     * @param text The text of spinner.
     * @param min The min value.
     * @param max The max value.
     * @param initValue The initial value.
     * @param listener The listener.
     * @return The created widget.
     */
    Cell addSpinner(String text, int min, int max, int initValue, IntSpinnerListener listener);

    /**
     * Adds integer spinner.
     *
     * @param text The text of spinner.
     * @param min The min value.
     * @param max The max value.
     * @param initValue The initial value.
     * @param step The step value
     * @param listener The listener.
     * @return The created widget.
     */
    Cell addSpinner(String text, int min, int max, int initValue, int step, IntSpinnerListener listener);

    /**
     * Adds float spinner with 1 step value.
     *
     * @param text The text of spinner.
     * @param min The min value.
     * @param max The max value.
     * @param initValue The initial value.
     * @param listener The listener.
     * @return The created widget.
     */
    Cell addSpinner(String text, float min, float max, float initValue, FloatSpinnerListener listener);

    /**
     * Adds float spinner.
     *
     * @param text The text of spinner.
     * @param min The min value.
     * @param max The max value.
     * @param initValue The initial value.
     * @param step The step value
     * @param listener The listener.
     * @return The created widget.
     */
    Cell addSpinner(String text, float min, float max, float initValue, float step, FloatSpinnerListener listener);

    /**
     * Adds checkbox with unchecked status.
     *
     * @param text The text of checkbox.
     * @param listener The listener.
     * @return The created widget.
     */
    Cell addCheckbox(String text, CheckboxListener listener);

    /**
     * Adds checkbox.
     *
     * @param text The text of checkbox.
     * @param checked If true then it will checked otherwese it will unchecked.
     * @param listener The listener.
     * @return The created widget.
     */
    Cell addCheckbox(String text, boolean checked, CheckboxListener listener);

    /**
     * Adds select box.
     *
     * @param selectList The list options.
     * @param listener The listener.
     * @return The created widget.
     * @param <T> The type of select box.
     */
    <T> Cell addSelectBox(Array<T> selectList, SelectBoxListener<T> listener);

    /**
     * Adds select box with specified default value.
     *
     * @param selectList The list options.
     * @param defaultValue The default value.
     * @param listener The listener.
     * @return The created widget.
     * @param <T> The type of select box.
     */
    <T> Cell addSelectBox(Array<T> selectList, T defaultValue, SelectBoxListener<T> listener);

    /**
     * Adds row.
     */
    void addRow();

    /**
     * Adds empty widget.
     * @return The created empty widget.
     */
    RootWidgetCell addEmptyWidget();

    /**
     * Clears the widget.
     */
    void clearWidgets();
}
