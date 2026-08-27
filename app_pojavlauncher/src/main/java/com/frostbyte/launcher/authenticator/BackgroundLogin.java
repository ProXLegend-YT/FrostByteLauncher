package com.frostbyte.launcher.authenticator;

import androidx.annotation.NonNull;

import com.frostbyte.launcher.authenticator.listener.LoginListener;
import com.frostbyte.launcher.authenticator.accounts.Account;

public interface BackgroundLogin {
    void createAccount(@NonNull LoginListener loginListener, String code);
    void refreshAccount(@NonNull LoginListener loginListener, Account account);
    interface Creator {
        BackgroundLogin create();
    }
}
