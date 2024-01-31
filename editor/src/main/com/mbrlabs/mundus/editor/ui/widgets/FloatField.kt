package com.mbrlabs.mundus.editor.ui.widgets

import com.kotcrab.vis.ui.util.FloatDigitsOnlyFilter
import com.kotcrab.vis.ui.widget.VisTextField

/**
 * @author JamesTKhan
 * @version July 09, 2023
 */
class FloatField : VisTextField {

    constructor(allowNegative: Boolean) {
        textFieldFilter = FloatDigitsOnlyFilter(allowNegative)
    }

    constructor() : super() {
        textFieldFilter = FloatDigitsOnlyFilter(true)
    }

    fun isValid(): Boolean {
        // A failsafe catchall to see if its valid
        try {
            java.lang.Float.parseFloat(text)
            isInputValid = true
            return true
        } catch (ex: NumberFormatException) {
            isInputValid = false
            return false
        }
    }

    val float: Float
        get() {
            if (isValid()) {
                return java.lang.Float.parseFloat(text)
            }
            return 0f
        }
}