package com.frostbyte.launcher.core.controls

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeControlsStore : ControlsStore {
    private val layoutFlow = MutableStateFlow(TouchControlLayout.default())
    private val overridesFlow = MutableStateFlow<Map<MinecraftAction, String>>(emptyMap())

    override val touchLayout: Flow<TouchControlLayout> = layoutFlow

    override val keyBindings: Flow<Map<MinecraftAction, String>> = overridesFlow.map { overrides ->
        MinecraftAction.entries.associateWith { action -> overrides[action] ?: action.defaultKey }
    }

    override suspend fun saveTouchLayout(layout: TouchControlLayout) {
        layoutFlow.value = layout
    }

    override suspend fun resetTouchLayoutToDefault() {
        layoutFlow.value = TouchControlLayout.default()
    }

    override suspend fun setKeyBinding(action: MinecraftAction, key: String) {
        overridesFlow.value = overridesFlow.value + (action to key)
    }

    override suspend fun resetKeyBinding(action: MinecraftAction) {
        overridesFlow.value = overridesFlow.value - action
    }
}

class FakeGamepadProvider(private val gamepads: List<ConnectedGamepad> = emptyList()) : GamepadProvider {
    override fun listConnectedGamepads(): List<ConnectedGamepad> = gamepads
}
