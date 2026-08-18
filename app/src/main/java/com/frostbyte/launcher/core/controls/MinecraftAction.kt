package com.frostbyte.launcher.core.controls

/**
 * Real Minecraft key binding identifiers and their default LWJGL key codes,
 * matching what Minecraft itself writes/reads in options.txt (e.g.
 * "key_key.forward:key.keyboard.w"). Modeled directly from Minecraft's own
 * binding names so that whenever a real game process exists (blocked on
 * the JRE/LWJGL gaps - see docs/KNOWN_GAPS.md), this configuration is
 * already wire-compatible with options.txt rather than needing translation.
 *
 * LWJGL key names use the "key.keyboard.<name>" format Minecraft expects,
 * e.g. "key.keyboard.w", "key.keyboard.space", "key.keyboard.left.shift".
 */
enum class MinecraftAction(val optionsKeyName: String, val displayName: String, val defaultKey: String) {
    FORWARD("key_key.forward", "Walk Forward", "key.keyboard.w"),
    LEFT("key_key.left", "Strafe Left", "key.keyboard.a"),
    BACK("key_key.back", "Walk Backward", "key.keyboard.s"),
    RIGHT("key_key.right", "Strafe Right", "key.keyboard.d"),
    JUMP("key_key.jump", "Jump", "key.keyboard.space"),
    SNEAK("key_key.sneak", "Sneak", "key.keyboard.left.shift"),
    SPRINT("key_key.sprint", "Sprint", "key.keyboard.left.control"),
    INVENTORY("key_key.inventory", "Open/Close Inventory", "key.keyboard.e"),
    DROP("key_key.drop", "Drop Selected Item", "key.keyboard.q"),
    CHAT("key_key.chat", "Open Chat", "key.keyboard.t"),
    ATTACK("key_key.attack", "Attack/Destroy", "key.mouse.left"),
    USE("key_key.use", "Use Item/Place Block", "key.mouse.right"),
    PICK_ITEM("key_key.pickItem", "Pick Block", "key.mouse.middle"),
    SWAP_HANDS("key_key.swapOffhand", "Swap Item With Offhand", "key.keyboard.f"),
    HOTBAR_1("key_key.hotbar.1", "Hotbar Slot 1", "key.keyboard.1"),
    HOTBAR_2("key_key.hotbar.2", "Hotbar Slot 2", "key.keyboard.2"),
    HOTBAR_3("key_key.hotbar.3", "Hotbar Slot 3", "key.keyboard.3"),
    HOTBAR_4("key_key.hotbar.4", "Hotbar Slot 4", "key.keyboard.4"),
    HOTBAR_5("key_key.hotbar.5", "Hotbar Slot 5", "key.keyboard.5"),
    HOTBAR_6("key_key.hotbar.6", "Hotbar Slot 6", "key.keyboard.6"),
    HOTBAR_7("key_key.hotbar.7", "Hotbar Slot 7", "key.keyboard.7"),
    HOTBAR_8("key_key.hotbar.8", "Hotbar Slot 8", "key.keyboard.8"),
    HOTBAR_9("key_key.hotbar.9", "Hotbar Slot 9", "key.keyboard.9"),
    PERSPECTIVE("key_key.togglePerspective", "Toggle Perspective", "key.keyboard.f5"),
    SCREENSHOT("key_key.screenshot", "Take Screenshot", "key.keyboard.f2"),
    FULLSCREEN("key_key.fullscreen", "Toggle Fullscreen", "key.keyboard.f11")
}
