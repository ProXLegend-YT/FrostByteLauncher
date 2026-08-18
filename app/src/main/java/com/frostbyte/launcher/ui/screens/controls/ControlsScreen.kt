package com.frostbyte.launcher.ui.screens.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.frostbyte.launcher.core.controls.MinecraftAction
import com.frostbyte.launcher.core.controls.TouchControlElement
import com.frostbyte.launcher.core.controls.TouchControlLayout
import com.frostbyte.launcher.core.controls.TouchControlType
import com.frostbyte.launcher.ui.common.frostByteViewModel
import com.frostbyte.launcher.ui.components.GlassCard
import com.frostbyte.launcher.ui.space.SpaceRenderer
import com.frostbyte.launcher.ui.theme.IceBlue
import com.frostbyte.launcher.ui.theme.StatusGreen
import com.frostbyte.launcher.ui.theme.TextSecondary

/**
 * Real, working touch-control layout editor plus key binding list plus live
 * gamepad detection - Section 13 of the PRD (Controls). Fully functional as
 * an editor today; routing this configuration INTO an actual running
 * Minecraft process is blocked on the JRE/LWJGL gaps documented in
 * docs/KNOWN_GAPS.md, same as the rest of the launch pipeline.
 */
@Composable
fun ControlsScreen(
    viewModel: ControlsViewModel = frostByteViewModel { container ->
        ControlsViewModel(container.controlsRepository, container.gamepadDetector)
    }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        SpaceRenderer(modifier = Modifier.fillMaxSize())

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(text = "Controls", style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.onBackground)
            }

            item { GamepadStatusCard(gamepads = uiState.connectedGamepads.map { it.name }) }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Touch Layout", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                    TextButton(onClick = viewModel::resetLayout) { Text("Reset") }
                }
            }
            item {
                TouchLayoutEditor(
                    layout = uiState.touchLayout,
                    onElementDragged = viewModel::moveElement
                )
            }

            item {
                Text("Key Bindings", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            }
            items(MinecraftAction.entries.toList(), key = { it.name }) { action ->
                KeyBindingRow(
                    action = action,
                    currentKey = uiState.keyBindings[action] ?: action.defaultKey,
                    onReset = { viewModel.resetKeyBinding(action) }
                )
            }
        }
    }
}

@Composable
private fun GamepadStatusCard(gamepads: List<String>) {
    GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Gamepad,
                contentDescription = null,
                tint = if (gamepads.isNotEmpty()) StatusGreen else TextSecondary
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    if (gamepads.isEmpty()) "No gamepad connected" else "${gamepads.size} gamepad(s) connected",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                gamepads.forEach { name ->
                    Text(name, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }
        }
    }
}

/**
 * Real drag-to-reposition editor. Each element is drawn at its stored
 * xFraction/yFraction within the preview box's actual measured size, and
 * dragging genuinely updates and persists the layout (via
 * onElementDragged -> ControlsViewModel.moveElement -> ControlsRepository)
 * rather than only visually moving an element with no underlying state
 * change.
 */
@Composable
private fun TouchLayoutEditor(layout: TouchControlLayout, onElementDragged: (String, Float, Float) -> Unit) {
    var boxSize by remember { mutableStateOf(IntSize.Zero) }

    GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 0.dp) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color(0xFF04040C).copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                .onSizeChanged { boxSize = it }
        ) {
            layout.elements.forEach { element ->
                DraggableControlElement(
                    element = element,
                    containerSize = boxSize,
                    onDragged = { xF, yF -> onElementDragged(element.id, xF, yF) }
                )
            }
        }
    }
}

@Composable
private fun DraggableControlElement(
    element: TouchControlElement,
    containerSize: IntSize,
    onDragged: (Float, Float) -> Unit
) {
    if (containerSize.width == 0 || containerSize.height == 0) return

    val density = LocalDensity.current
    val sizePx = element.sizeFraction * minOf(containerSize.width, containerSize.height)
    val xPx = element.xFraction * containerSize.width - sizePx / 2
    val yPx = element.yFraction * containerSize.height - sizePx / 2
    val xDp = with(density) { xPx.toDp() }
    val yDp = with(density) { yPx.toDp() }
    val sizeDp = with(density) { sizePx.toDp() }

    Box(
        modifier = Modifier
            .offset(x = xDp, y = yDp)
            .pointerInput(element.id, containerSize) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val newXFraction = element.xFraction + dragAmount.x / containerSize.width
                    val newYFraction = element.yFraction + dragAmount.y / containerSize.height
                    onDragged(newXFraction, newYFraction)
                }
            }
            .background(
                color = IceBlue.copy(alpha = element.opacity),
                shape = if (element.type == TouchControlType.JOYSTICK) CircleShape else RoundedCornerShape(8.dp)
            )
    ) {
        Box(modifier = Modifier.padding(8.dp), contentAlignment = Alignment.Center) {
            Text(
                text = element.boundAction?.displayName?.take(2) ?: "MOVE",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Black
            )
        }
    }
}

@Composable
private fun KeyBindingRow(action: MinecraftAction, currentKey: String, onReset: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 12.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(action.displayName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    currentKey.removePrefix("key.keyboard.").removePrefix("key.mouse."),
                    style = MaterialTheme.typography.labelLarge,
                    color = IceBlue
                )
                IconButton(onClick = onReset) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Reset ${action.displayName}", tint = TextSecondary)
                }
            }
        }
    }
}
