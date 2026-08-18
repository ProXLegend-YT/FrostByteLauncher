package com.frostbyte.launcher.ui.screens.controls

import com.frostbyte.launcher.core.controls.ConnectedGamepad
import com.frostbyte.launcher.core.controls.FakeControlsStore
import com.frostbyte.launcher.core.controls.FakeGamepadProvider
import com.frostbyte.launcher.core.controls.MinecraftAction
import com.frostbyte.launcher.core.controls.TouchControlLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ControlsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var store: FakeControlsStore

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        store = FakeControlsStore()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(gamepads: List<ConnectedGamepad> = emptyList()) =
        ControlsViewModel(store, FakeGamepadProvider(gamepads))

    @Test
    fun `initial state reflects the default layout and default key bindings`() = runTest(testDispatcher) {
        val vm = viewModel()
        val state = vm.uiState.first()

        assertEquals(TouchControlLayout.default(), state.touchLayout)
        assertEquals(MinecraftAction.FORWARD.defaultKey, state.keyBindings[MinecraftAction.FORWARD])
    }

    @Test
    fun `moveElement updates the target element's position and leaves others untouched`() = runTest(testDispatcher) {
        val vm = viewModel()
        vm.uiState.first() // subscribe

        val targetId = TouchControlLayout.MOVEMENT_JOYSTICK_ID
        vm.moveElement(targetId, xFraction = 0.5f, yFraction = 0.5f)

        val updated = vm.uiState.first { layout ->
            layout.touchLayout.elements.first { it.id == targetId }.xFraction == 0.5f
        }
        val movedElement = updated.touchLayout.elements.first { it.id == targetId }
        assertEquals(0.5f, movedElement.xFraction)
        assertEquals(0.5f, movedElement.yFraction)

        // A different element must be unaffected.
        val otherOriginal = TouchControlLayout.default().elements.first { it.id != targetId }
        val otherUpdated = updated.touchLayout.elements.first { it.id == otherOriginal.id }
        assertEquals(otherOriginal.xFraction, otherUpdated.xFraction)
    }

    @Test
    fun `moveElement clamps positions to the 0 to 1 range`() = runTest(testDispatcher) {
        val vm = viewModel()
        vm.uiState.first()

        val targetId = TouchControlLayout.MOVEMENT_JOYSTICK_ID
        vm.moveElement(targetId, xFraction = 1.5f, yFraction = -0.3f)

        val updated = vm.uiState.first { layout ->
            layout.touchLayout.elements.first { it.id == targetId }.xFraction == 1f
        }
        val movedElement = updated.touchLayout.elements.first { it.id == targetId }
        assertEquals(1f, movedElement.xFraction)
        assertEquals(0f, movedElement.yFraction)
    }

    @Test
    fun `resetLayout restores the default layout after a move`() = runTest(testDispatcher) {
        val vm = viewModel()
        vm.uiState.first()
        vm.moveElement(TouchControlLayout.MOVEMENT_JOYSTICK_ID, 0.9f, 0.9f)
        vm.uiState.first { it.touchLayout != TouchControlLayout.default() }

        vm.resetLayout()

        val state = vm.uiState.first { it.touchLayout == TouchControlLayout.default() }
        assertEquals(TouchControlLayout.default(), state.touchLayout)
    }

    @Test
    fun `setKeyBinding overrides only the targeted action`() = runTest(testDispatcher) {
        val vm = viewModel()
        vm.uiState.first()

        vm.setKeyBinding(MinecraftAction.FORWARD, "key.keyboard.up")

        val state = vm.uiState.first { it.keyBindings[MinecraftAction.FORWARD] == "key.keyboard.up" }
        assertEquals("key.keyboard.up", state.keyBindings[MinecraftAction.FORWARD])
        // An untouched action must still report its default.
        assertEquals(MinecraftAction.JUMP.defaultKey, state.keyBindings[MinecraftAction.JUMP])
    }

    @Test
    fun `resetKeyBinding reverts a previously overridden action back to default`() = runTest(testDispatcher) {
        val vm = viewModel()
        vm.uiState.first()
        vm.setKeyBinding(MinecraftAction.FORWARD, "key.keyboard.up")
        vm.uiState.first { it.keyBindings[MinecraftAction.FORWARD] == "key.keyboard.up" }

        vm.resetKeyBinding(MinecraftAction.FORWARD)

        val state = vm.uiState.first { it.keyBindings[MinecraftAction.FORWARD] == MinecraftAction.FORWARD.defaultKey }
        assertEquals(MinecraftAction.FORWARD.defaultKey, state.keyBindings[MinecraftAction.FORWARD])
    }

    @Test
    fun `connectedGamepads reflects what the gamepad provider reports`() = runTest(testDispatcher) {
        val vm = viewModel(gamepads = listOf(ConnectedGamepad(deviceId = 1, name = "Test Controller")))

        val state = vm.uiState.first { it.connectedGamepads.isNotEmpty() }

        assertEquals(1, state.connectedGamepads.size)
        assertEquals("Test Controller", state.connectedGamepads.first().name)
    }
}
