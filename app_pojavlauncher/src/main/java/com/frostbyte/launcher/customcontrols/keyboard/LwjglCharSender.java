package com.frostbyte.launcher.customcontrols.keyboard;

import static com.frostbyte.launcher.game.platform.Platform.PLATFORM;

import android.view.KeyEvent;

import com.frostbyte.launcher.CallbackBridge;

/** Sends keys via the CallBackBridge */
public class LwjglCharSender implements CharacterSenderStrategy {
    @Override
    public void sendBackspace() {
        CallbackBridge.sendKeyPress(KeyEvent.KEYCODE_DEL);
    }

    @Override
    public void sendEnter() {
        CallbackBridge.sendKeyPress(KeyEvent.KEYCODE_ENTER);
    }

    @Override
    public void sendChars(CharSequence chars) {
        PLATFORM.sendBulkUnicodeEvent(chars.toString(), CallbackBridge.getCurrentMods());
    }
}
