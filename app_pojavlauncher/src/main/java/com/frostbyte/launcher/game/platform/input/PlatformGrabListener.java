package com.frostbyte.launcher.game.platform.input;

/**
 * Platform grab change listener. Accepts grab and ungrab events
 */
public interface PlatformGrabListener {
    void onGrabState(boolean grabbing);
}
