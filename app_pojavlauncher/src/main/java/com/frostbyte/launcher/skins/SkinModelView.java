package com.frostbyte.launcher.skins;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Shows a real, rotatable 3D Minecraft player model built from actual cube geometry and the
 * skin's own texture (not a flat image or a fake pseudo-3D projection). Drag left/right or
 * up/down to rotate manually; it also auto-rotates on its own when not being touched.
 */
public class SkinModelView extends GLSurfaceView {

    private final SkinModelRenderer mRenderer;
    private float mLastTouchX, mLastTouchY;
    private static final float DRAG_SENSITIVITY = 0.5f;

    public SkinModelView(Context context) {
        this(context, null);
    }

    public SkinModelView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setEGLContextClientVersion(2);
        setEGLConfigChooser(8, 8, 8, 8, 16, 0); // request an alpha channel so the background shows through
        getHolder().setFormat(android.graphics.PixelFormat.TRANSLUCENT);
        mRenderer = new SkinModelRenderer();
        setRenderer(mRenderer);
        setRenderMode(RENDERMODE_CONTINUOUSLY);
    }

    /** Loads a new skin bitmap into the model. Safe to call repeatedly (e.g. when previewing different skins). */
    public void setSkin(Bitmap bitmap, boolean slimArms) {
        mRenderer.setSkin(bitmap, slimArms);
    }

    public void setAutoRotate(boolean autoRotate) {
        mRenderer.autoRotate = autoRotate;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLastTouchX = event.getX();
                mLastTouchY = event.getY();
                mRenderer.autoRotate = false; // dragging takes over from auto-spin
                return true;
            case MotionEvent.ACTION_MOVE:
                float dx = event.getX() - mLastTouchX;
                float dy = event.getY() - mLastTouchY;
                mRenderer.rotationYDegrees += dx * DRAG_SENSITIVITY;
                mRenderer.rotationXDegrees += dy * DRAG_SENSITIVITY;
                // Keep the model from being flipped upside down by an overly enthusiastic vertical drag
                mRenderer.rotationXDegrees = Math.max(-80f, Math.min(80f, mRenderer.rotationXDegrees));
                mLastTouchX = event.getX();
                mLastTouchY = event.getY();
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                // Resume auto-rotate a moment after the user lets go
                postDelayed(() -> mRenderer.autoRotate = true, 1500);
                return true;
        }
        return super.onTouchEvent(event);
    }
}
