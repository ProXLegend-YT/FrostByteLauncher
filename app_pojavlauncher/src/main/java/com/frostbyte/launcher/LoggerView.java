package com.frostbyte.launcher;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Editable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.frostbyte.launcher.Logger;
import com.frostbyte.launcher.R;

/**
 * A class able to display logs to the user.
 * It has support for the Logger class
 */
public class LoggerView extends ConstraintLayout {
    private Logger.eventLogListener mLogListener;
    private ToggleButton mLogToggle;
    private DefocusableScrollView mScrollView;
    private TextView mLogTextView;


    public LoggerView(@NonNull Context context) {
        this(context, null);
    }

    public LoggerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    // Caps how much log text is kept on screen at once. Previously the log TextView had
    // setMaxLines(Integer.MAX_VALUE) and just appended forever for as long as the log view was
    // open — on a long play session with a lot of log output that's a real, unbounded memory
    // growth risk on a low-RAM device, not just a cosmetic scroll-length issue. This trims the
    // oldest text once the buffer gets too large, rather than never trimming at all.
    private static final int MAX_LOG_CHARS = 200_000;
    private static final int TRIM_TO_CHARS = 150_000;

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        // Triggers the log view shown state by default when viewing it
        mLogToggle.setChecked(visibility == VISIBLE);
    }

    /**
     * Inflate the layout, and add component behaviors
     */
    private void init(){
        inflate(getContext(), R.layout.view_logger, this);
        mLogTextView = findViewById(R.id.content_log_view);
        mLogTextView.setTypeface(Typeface.MONOSPACE);
        mLogTextView.setEllipsize(null);
        mLogTextView.setVisibility(GONE);

        // Toggle log visibility
        mLogToggle = findViewById(R.id.content_log_toggle_log);
        mLogToggle.setOnCheckedChangeListener(
                (compoundButton, isChecked) -> {
                    mLogTextView.setVisibility(isChecked ? VISIBLE : GONE);
                    if(isChecked) {
                        Logger.setLogListener(mLogListener);
                    }else{
                        mLogTextView.setText("");
                        Logger.setLogListener(null); // Makes the JNI code be able to skip expensive logger callbacks
                        // NOTE: was tested by rapidly smashing the log on/off button, no sync issues found :)
                    }
                });
        mLogToggle.setChecked(false);

        // Remove the loggerView from the user View
        ImageButton cancelButton = findViewById(R.id.log_view_cancel);
        cancelButton.setOnClickListener(view -> LoggerView.this.setVisibility(GONE));

        // Set the scroll view
        mScrollView = findViewById(R.id.content_log_scroll);
        mScrollView.setKeepFocusing(true);

        //Set up the autoscroll switch
        ToggleButton autoscrollToggle = findViewById(R.id.content_log_toggle_autoscroll);
        autoscrollToggle.setOnCheckedChangeListener(
                (compoundButton, isChecked) -> {
                    if(isChecked) mScrollView.fullScroll(View.FOCUS_DOWN);
                    mScrollView.setKeepFocusing(isChecked);
                }
        );
        autoscrollToggle.setChecked(true);

        // Listen to logs
        mLogListener = text -> {
            if(mLogTextView.getVisibility() != VISIBLE) return;
            post(() -> {
                mLogTextView.append(text + '\n');
                trimLogIfTooLarge();
                if(mScrollView.isKeepFocusing()) mScrollView.fullScroll(View.FOCUS_DOWN);
            });

        };
    }

    /**
     * Drops the oldest text once the log buffer exceeds MAX_LOG_CHARS, trimming back down to
     * TRIM_TO_CHARS rather than trimming by a tiny amount every single call — trimming to a
     * lower target and only checking the upper bound means this runs occasionally instead of on
     * every appended line.
     */
    private void trimLogIfTooLarge() {
        Editable text = mLogTextView.getEditableText();
        if (text == null || text.length() <= MAX_LOG_CHARS) return;
        int cutoff = text.length() - TRIM_TO_CHARS;
        // Avoid cutting in the middle of a line: trim to just after the next newline at or
        // after the raw cutoff point, so what remains still starts cleanly at a line boundary.
        int newlineAfterCutoff = text.toString().indexOf('\n', cutoff);
        int trimIndex = newlineAfterCutoff != -1 ? newlineAfterCutoff + 1 : cutoff;
        text.delete(0, trimIndex);
    }

}
