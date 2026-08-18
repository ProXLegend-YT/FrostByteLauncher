package com.frostbyte.launcher.ui.screens.controls

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frostbyte.launcher.core.controls.ConnectedGamepad
import com.frostbyte.launcher.core.controls.ControlsStore
import com.frostbyte.launcher.core.controls.GamepadProvider
import com.frostbyte.launcher.core.controls.MinecraftAction
import com.frostbyte.launcher.core.controls.TouchControlLayout
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ControlsUiState(
    val touchLayout: TouchControlLayout = TouchControlLayout.default(),
    val keyBindings: Map<MinecraftAction, String> = emptyMap(),
    val connectedGamepads: List<ConnectedGamepad> = emptyList()
)

/**
 * Gamepad list is recomputed on every touchLayout/keyBindings emission,
 * which covers "the Controls screen shows current gamepads whenever it's
 * opened/recomposed." A real hotplug listener
 * (InputManager.InputDeviceListener, wired to push updates into this
 * StateFlow live while the screen is open) is a reasonable real follow-up,
 * not something to fake with a no-op "refresh" button here.
 */
class ControlsViewModel(
    private val repository: ControlsStore,
    private val gamepadDetector: GamepadProvider
) : ViewModel() {

    val uiState: StateFlow<ControlsUiState> = combine(
        repository.touchLayout,
        repository.keyBindings
    ) { layout, bindings ->
        ControlsUiState(
            touchLayout = layout,
            keyBindings = bindings,
            connectedGamepads = gamepadDetector.listConnectedGamepads()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ControlsUiState()
    )

    fun moveElement(elementId: String, xFraction: Float, yFraction: Float) {
        viewModelScope.launch {
            val current = uiState.value.touchLayout
            val updated = current.copy(
                elements = current.elements.map { element ->
                    if (element.id == elementId) {
                        element.copy(
                            xFraction = xFraction.coerceIn(0f, 1f),
                            yFraction = yFraction.coerceIn(0f, 1f)
                        )
                    } else {
                        element
                    }
                }
            )
            repository.saveTouchLayout(updated)
        }
    }

    fun resetLayout() {
        viewModelScope.launch { repository.resetTouchLayoutToDefault() }
    }

    fun setKeyBinding(action: MinecraftAction, key: String) {
        viewModelScope.launch { repository.setKeyBinding(action, key) }
    }

    fun resetKeyBinding(action: MinecraftAction) {
        viewModelScope.launch { repository.resetKeyBinding(action) }
    }
}
