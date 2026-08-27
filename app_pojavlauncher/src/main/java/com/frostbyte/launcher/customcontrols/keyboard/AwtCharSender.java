package com.frostbyte.launcher.customcontrols.keyboard;

import com.frostbyte.launcher.AWTInputBridge;
import com.frostbyte.launcher.AWTInputEvent;

/** Send chars via the AWT Bridgee */
public class AwtCharSender implements CharacterSenderStrategy {
    @Override
    public void sendBackspace() {
        AWTInputBridge.sendKey(' ', AWTInputEvent.VK_BACK_SPACE);
    }

    @Override
    public void sendEnter() {
        AWTInputBridge.sendKey(' ', AWTInputEvent.VK_ENTER);
    }

    @Override
    public void sendChars(CharSequence chars) {
        for(int i = 0; i < chars.length(); i++) AWTInputBridge.sendChar(chars.charAt(i));
    }

}
