package com.frostbyte.launcher.core.controls

import android.hardware.input.InputManager
import android.view.InputDevice

data class ConnectedGamepad(
    val deviceId: Int,
    val name: String
)

/**
 * Narrow interface for listing connected gamepads. Lets ControlsViewModel
 * be unit-tested with a fake instead of needing a real Android InputManager.
 */
interface GamepadProvider {
    fun listConnectedGamepads(): List<ConnectedGamepad>
}

/**
 * Detects connected game controllers via Android's real InputDevice API -
 * genuinely functional today, independent of whether a live Minecraft
 * process exists yet (unlike actually routing gamepad input INTO the game,
 * which needs LWJGL/GLFW's controller support - a separate, larger gap
 * tracked in docs/KNOWN_GAPS.md alongside the rest of the rendering-layer
 * work). This class only answers "is a gamepad plugged in and what is it,"
 * which is real, useful information on its own (e.g. for the Controls
 * screen to show gamepad status).
 */
class GamepadDetector(private val inputManager: InputManager) : GamepadProvider {

    override fun listConnectedGamepads(): List<ConnectedGamepad> {
        return inputManager.inputDeviceIds
            .mapNotNull { id: Int -> inputManager.getInputDevice(id) }
            .filter { device -> device.isGamepad() }
            .map { device -> ConnectedGamepad(deviceId = device.id, name = device.name) }
    }

    private fun InputDevice.isGamepad(): Boolean {
        val sources = sources
        return (sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
            (sources and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
    }
}
