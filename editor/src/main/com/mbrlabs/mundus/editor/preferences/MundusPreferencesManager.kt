package com.mbrlabs.mundus.editor.preferences

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences

/**
 * Manages preferences using the given key value
 *
 * @author JamesTKhan
 * @version July 14, 2022
 */
class MundusPreferencesManager(preferencesKey: String) : PreferencesManager {
    companion object {
        private val TAG = MundusPreferencesManager::class.java.simpleName

        /** Keys for global prefs **/
        const val GLOB_MUNDUS_VERSION = "version"
        const val GLOB_RIGHT_BUTTON_SELECT = "right-button-select"
        const val GLOB_OPTIMIZE_TERRAIN_UPDATES = "optimize-terrain-updates"
        const val GLOB_LINE_WIDTH_SELECTION = "line-width-selection"
        const val GLOB_LINE_WIDTH_WIREFRAME = "line-width-wireframe"
        const val GLOB_LINE_WIDTH_HELPER_LINE = "line-width-helper-line"

        // Debug renderer settings
        const val GLOB_BOOL_DEBUG_RENDERER_ON = "debug-renderer-on"
        const val GLOB_BOOL_DEBUG_RENDERER_DEPTH_OFF = "debug-renderer-depth-off"
        const val GLOB_BOOL_DEBUG_FACING_ARROW = "debug-renderer-facingarrow-off"

        // Default values for global prefs
        const val GLOB_RIGHT_SELECT_BUTTON_DEFAULT_VALUE = true
        const val GLOB_LINE_WIDTH_DEFAULT_VALUE = 1.0f

        /** Keys for project specific prefs **/
        const val PROJ_LAST_DIR = "lastDirectoryOpened"
    }

    private var preferences: Preferences

    init {
        preferences = Gdx.app.getPreferences("mundus.$preferencesKey")
    }

    /**
     * Set a value in preferences
     * Supported types: Boolean, String, Int, Long, Float
     */
    override fun set(key: String, value: Any) {
        when(value) {
            is Boolean -> preferences.putBoolean(key, value)
            is String -> preferences.putString(key, value)
            is Int -> preferences.putInteger(key, value)
            is Long -> preferences.putLong(key, value)
            is Float -> preferences.putFloat(key, value)
            else -> {
                Gdx.app.error(TAG, "Invalid object type given for preferences.")
                return
            }
        }

        preferences.flush()
    }

    fun set(value: MutableMap<String, *>) {
        preferences.put(value)
    }

    override fun get(key: String, type: Class<*>): Any? {
        if (type == Boolean::class.java) {
            return preferences.getBoolean(key, false)
        }
        return null
    }

    override fun getBoolean(key: String): Boolean {
        return preferences.getBoolean(key)
    }

    override fun getBoolean(key: String, defValue: Boolean): Boolean {
        return preferences.getBoolean(key, defValue)
    }

    override fun getInteger(key: String): Int {
        return preferences.getInteger(key)
    }

    override fun getInteger(key: String, defValue: Int): Int {
        return preferences.getInteger(key, defValue)
    }

    override fun getLong(key: String): Long {
        return preferences.getLong(key)
    }

    override fun getLong(key: String, defValue: Long): Long {
        return preferences.getLong(key, defValue)
    }

    override fun getFloat(key: String): Float {
        return preferences.getFloat(key)
    }

    override fun getFloat(key: String, defValue: Float): Float {
        return preferences.getFloat(key, defValue)
    }

    override fun getString(key: String): String {
        return preferences.getString(key)
    }

    override fun getString(key: String?, defValue: String?): String {
        preferences.get()
        return preferences.getString(key, defValue)
    }

    override fun contains(key: String): Boolean {
        return preferences.contains(key)
    }

    override fun remove(key: String) {
        preferences.remove(key)
    }

    override fun clear() {
        preferences.clear()
    }

}