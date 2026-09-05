package com.frostbyte.launcher;

import android.os.Bundle;

import com.frostbyte.launcher.R;

public class MissingStorageActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.storage_test_no_sdcard);
    }
}