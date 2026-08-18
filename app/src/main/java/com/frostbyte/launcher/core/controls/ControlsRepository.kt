package com.frostbyte.launcher.core.controls

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.controlsDataStore by preferencesDataStore(name = "frostbyte_controls")

/**
 * Narrow interface for reading/writing touch layout and key bindings. Lets
 * ControlsViewModel be unit-tested with an in-memory fake instead of
 * needing a real Android Context/DataStore.
 */
interface ControlsStore {
    val touchLayout: Flow<TouchControlLayout>
    val keyBindings: Flow<Map<MinecraftAction, String>>
    suspend fun saveTouchLayout(layout: TouchControlLayout)
    suspend fun resetTouchLayoutToDefault()
    suspend fun setKeyBinding(action: MinecraftAction, key: String)
    suspend fun resetKeyBinding(action: MinecraftAction)
}

/**
 * Persists the touch control layout and keyboard/mouse key bindings.
 * Serializes real, structured data (not placeholders) via Gson - both are
 * genuinely usable today independent of whether a real game process exists
 * yet, and key bindings are stored in exactly the shape needed to write a
 * real options.txt once LauncherEngine can hand off to a live game (see
 * OptionsTxtWriter).
 */
class ControlsRepository(context: Context) : ControlsStore {

    private val dataStore = context.controlsDataStore
    private val gson = Gson()

    private object Keys {
        val TOUCH_LAYOUT = stringPreferencesKey("touch_layout_json")
        val KEY_BINDINGS = stringPreferencesKey("key_bindings_json")
        val TOUCH_CONTROLS_ENABLED = stringPreferencesKey("touch_controls_enabled")
    }

    override val touchLayout: Flow<TouchControlLayout> = dataStore.data.map { prefs ->
        val json = prefs[Keys.TOUCH_LAYOUT]
        if (json == null) {
            TouchControlLayout.default()
        } else {
            try {
                gson.fromJson(json, TouchControlLayout::class.java)
            } catch (e: Exception) {
                TouchControlLayout.default() // corrupt/old-format data falls back to a known-good default rather than crashing the Controls screen
            }
        }
    }

    override val keyBindings: Flow<Map<MinecraftAction, String>> = dataStore.data.map { prefs ->
        val json = prefs[Keys.KEY_BINDINGS]
        val overrides: Map<String, String> = if (json == null) {
            emptyMap()
        } else {
            try {
                gson.fromJson(json, Map::class.java) as? Map<String, String> ?: emptyMap()
            } catch (e: Exception) {
                emptyMap()
            }
        }
        MinecraftAction.entries.associateWith { action -> overrides[action.name] ?: action.defaultKey }
    }

    override suspend fun saveTouchLayout(layout: TouchControlLayout) {
        dataStore.edit { prefs -> prefs[Keys.TOUCH_LAYOUT] = gson.toJson(layout) }
    }

    override suspend fun resetTouchLayoutToDefault() {
        saveTouchLayout(TouchControlLayout.default())
    }

    override suspend fun setKeyBinding(action: MinecraftAction, key: String) {
        dataStore.edit { prefs ->
            val current = prefs[Keys.KEY_BINDINGS]
            val overrides: MutableMap<String, String> = if (current == null) {
                mutableMapOf()
            } else {
                @Suppress("UNCHECKED_CAST")
                (gson.fromJson(current, Map::class.java) as? Map<String, String>)?.toMutableMap() ?: mutableMapOf()
            }
            overrides[action.name] = key
            prefs[Keys.KEY_BINDINGS] = gson.toJson(overrides)
        }
    }

    override suspend fun resetKeyBinding(action: MinecraftAction) {
        dataStore.edit { prefs ->
            val current = prefs[Keys.KEY_BINDINGS] ?: return@edit
            @Suppress("UNCHECKED_CAST")
            val overrides = (gson.fromJson(current, Map::class.java) as? Map<String, String>)?.toMutableMap() ?: return@edit
            overrides.remove(action.name)
            prefs[Keys.KEY_BINDINGS] = gson.toJson(overrides)
        }
    }
}
