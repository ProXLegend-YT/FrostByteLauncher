package com.frostbyte.launcher.core.controls

/**
 * A single virtual touch control element - a joystick or a button bound to
 * a MinecraftAction, positioned as a fraction of screen width/height (0f-1f)
 * so layouts remain valid across different screen sizes/orientations rather
 * than storing raw pixel coordinates.
 */
data class TouchControlElement(
    val id: String,
    val type: TouchControlType,
    val boundAction: MinecraftAction?, // null for the movement joystick, which maps to 4 actions at once (see MOVEMENT_JOYSTICK_ID)
    val xFraction: Float,
    val yFraction: Float,
    val sizeFraction: Float, // diameter/side length as a fraction of the shorter screen dimension
    val opacity: Float = 0.6f
)

enum class TouchControlType {
    JOYSTICK, BUTTON
}

data class TouchControlLayout(
    val elements: List<TouchControlElement>
) {
    companion object {
        const val MOVEMENT_JOYSTICK_ID = "movement_joystick"
        const val CAMERA_LOOK_AREA_ID = "camera_look_area"

        /**
         * A reasonable default layout: movement joystick bottom-left, jump/
         * sneak/inventory/attack/use as buttons bottom-right, matching the
         * general shape of every real mobile Minecraft touch UI (Pocket
         * Edition, PojavLauncher, etc.) rather than an arbitrary invention.
         */
        fun default(): TouchControlLayout = TouchControlLayout(
            elements = listOf(
                TouchControlElement(MOVEMENT_JOYSTICK_ID, TouchControlType.JOYSTICK, null, xFraction = 0.15f, yFraction = 0.78f, sizeFraction = 0.22f),
                TouchControlElement("btn_jump", TouchControlType.BUTTON, MinecraftAction.JUMP, xFraction = 0.88f, yFraction = 0.68f, sizeFraction = 0.11f),
                TouchControlElement("btn_sneak", TouchControlType.BUTTON, MinecraftAction.SNEAK, xFraction = 0.76f, yFraction = 0.82f, sizeFraction = 0.09f),
                TouchControlElement("btn_attack", TouchControlType.BUTTON, MinecraftAction.ATTACK, xFraction = 0.92f, yFraction = 0.45f, sizeFraction = 0.10f),
                TouchControlElement("btn_use", TouchControlType.BUTTON, MinecraftAction.USE, xFraction = 0.78f, yFraction = 0.45f, sizeFraction = 0.10f),
                TouchControlElement("btn_inventory", TouchControlType.BUTTON, MinecraftAction.INVENTORY, xFraction = 0.92f, yFraction = 0.88f, sizeFraction = 0.08f)
            )
        )
    }
}
