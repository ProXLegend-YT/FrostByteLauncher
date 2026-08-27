package com.frostbyte.launcher.customcontrols.gamepad;


import com.frostbyte.launcher.game.platform.input.PlatformGrabListener;

public interface GamepadDataProvider {
    GamepadMap getMenuMap();
    GamepadMap getGameMap();
    boolean isGrabbing();
    void attachGrabListener(PlatformGrabListener grabListener);
}
