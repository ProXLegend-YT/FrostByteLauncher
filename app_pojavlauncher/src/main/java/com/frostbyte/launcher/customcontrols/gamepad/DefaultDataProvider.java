package com.frostbyte.launcher.customcontrols.gamepad;


import com.frostbyte.launcher.game.platform.input.PlatformGrabListener;
import com.frostbyte.launcher.game.platform.Platform;


public class DefaultDataProvider implements GamepadDataProvider {
    public static final DefaultDataProvider INSTANCE = new DefaultDataProvider();

    // Cannot instantiate this class publicly
    private DefaultDataProvider() {}

    @Override
    public GamepadMap getGameMap() {
        return GamepadMapStore.getGameMap();
    }


    @Override
    public GamepadMap getMenuMap() {
        return GamepadMapStore.getMenuMap();
    }

    @Override
    public boolean isGrabbing() {
        // Avoid going through the JNI each time.
        return Platform.isGrabbing();
    }

    @Override
    public void attachGrabListener(PlatformGrabListener grabListener) {
        Platform.addGrabListener(grabListener);
    }
}
