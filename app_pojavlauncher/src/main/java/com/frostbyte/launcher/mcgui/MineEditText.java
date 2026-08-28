package com.frostbyte.launcher.mcgui;

import android.content.*;
import android.util.*;
import android.graphics.*;
import android.widget.EditText;

public class MineEditText extends androidx.appcompat.widget.AppCompatEditText {
	public MineEditText(Context ctx) {
		super(ctx);
		init();
	}

	public MineEditText(Context ctx, AttributeSet attrs) {
		super(ctx, attrs);
		init();
	}

	public void init() {
		setBackground(androidx.core.content.res.ResourcesCompat.getDrawable(getResources(), com.frostbyte.launcher.R.drawable.frostbyte_input_bg, null));
		setTextColor(getResources().getColor(com.frostbyte.launcher.R.color.frostbyte_text_primary));
		setHintTextColor(getResources().getColor(com.frostbyte.launcher.R.color.frostbyte_text_secondary));
		int hPad = (int) (16 * getResources().getDisplayMetrics().density);
		int vPad = (int) (10 * getResources().getDisplayMetrics().density);
		setPadding(hPad, vPad, hPad, vPad);
	}
}
