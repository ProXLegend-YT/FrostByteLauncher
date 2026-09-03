package com.frostbyte.launcher.skins;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.TextureView;

/**
 * Renders the 3D player model into a TextureView instead of a GLSurfaceView. A GLSurfaceView
 * (backed by a raw SurfaceView) composites its own hardware layer outside the normal view
 * drawing pipeline, so it ignores the parent's rounded-corner clipping and can end up drawing
 * either fully opaque-black or bleeding outside its container depending on z-order flags.
 * TextureView draws into a real GPU texture that Android treats like any other view's content,
 * so clipToOutline, alpha blending with the wallpaper behind it, and rounded corners all work
 * exactly the way they do for a normal ImageView.
 */
public class SkinModelView extends TextureView implements TextureView.SurfaceTextureListener {

    private final SkinModelRenderer mRenderer = new SkinModelRenderer();
    private RenderThread mRenderThread;
    private float mLastTouchX, mLastTouchY;
    private static final float DRAG_SENSITIVITY = 0.5f;

    public SkinModelView(Context context) {
        this(context, null);
    }

    public SkinModelView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOpaque(false); // lets the wallpaper/panel behind this view show through
        setSurfaceTextureListener(this);
    }

    public void setSkin(Bitmap bitmap, boolean slimArms) {
        mRenderer.setSkin(bitmap, slimArms);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        mRenderThread = new RenderThread(surface, width, height);
        mRenderThread.start();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        if (mRenderThread != null) mRenderThread.onSizeChanged(width, height);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if (mRenderThread != null) {
            mRenderThread.shutdown();
            mRenderThread = null;
        }
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {}

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLastTouchX = event.getX();
                mLastTouchY = event.getY();
                mRenderer.autoRotate = false;
                return true;
            case MotionEvent.ACTION_MOVE:
                float dx = event.getX() - mLastTouchX;
                float dy = event.getY() - mLastTouchY;
                mRenderer.rotationYDegrees += dx * DRAG_SENSITIVITY;
                mRenderer.rotationXDegrees += dy * DRAG_SENSITIVITY;
                mRenderer.rotationXDegrees = Math.max(-80f, Math.min(80f, mRenderer.rotationXDegrees));
                mLastTouchX = event.getX();
                mLastTouchY = event.getY();
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                postDelayed(() -> mRenderer.autoRotate = true, 1500);
                return true;
        }
        return super.onTouchEvent(event);
    }

    /** Owns the EGL context and drives the render loop, since TextureView provides no built-in one. */
    private class RenderThread extends Thread {
        private final SurfaceTexture mSurfaceTexture;
        private volatile int mWidth, mHeight;
        private volatile boolean mRunning = true;

        private EGLDisplay mEglDisplay = EGL14.EGL_NO_DISPLAY;
        private EGLContext mEglContext = EGL14.EGL_NO_CONTEXT;
        private EGLSurface mEglSurface = EGL14.EGL_NO_SURFACE;

        RenderThread(SurfaceTexture surfaceTexture, int width, int height) {
            mSurfaceTexture = surfaceTexture;
            mWidth = width;
            mHeight = height;
        }

        void onSizeChanged(int width, int height) {
            mWidth = width;
            mHeight = height;
        }

        void shutdown() {
            mRunning = false;
        }

        @Override
        public void run() {
            if (!initEgl()) return;
            mRenderer.onSurfaceCreated();
            int lastWidth = -1, lastHeight = -1;

            while (mRunning) {
                if (mWidth != lastWidth || mHeight != lastHeight) {
                    mRenderer.onSurfaceChanged(mWidth, mHeight);
                    lastWidth = mWidth;
                    lastHeight = mHeight;
                }
                mRenderer.onDrawFrame();
                EGL14.eglSwapBuffers(mEglDisplay, mEglSurface);
                try {
                    Thread.sleep(16); // ~60fps cap
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
            releaseEgl();
        }

        private boolean initEgl() {
            mEglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
            if (mEglDisplay == EGL14.EGL_NO_DISPLAY) return false;

            int[] version = new int[2];
            if (!EGL14.eglInitialize(mEglDisplay, version, 0, version, 1)) return false;

            int[] configAttribs = {
                    EGL14.EGL_RED_SIZE, 8,
                    EGL14.EGL_GREEN_SIZE, 8,
                    EGL14.EGL_BLUE_SIZE, 8,
                    EGL14.EGL_ALPHA_SIZE, 8,
                    EGL14.EGL_DEPTH_SIZE, 16,
                    EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                    EGL14.EGL_NONE
            };
            EGLConfig[] configs = new EGLConfig[1];
            int[] numConfigs = new int[1];
            if (!EGL14.eglChooseConfig(mEglDisplay, configAttribs, 0, configs, 0, 1, numConfigs, 0)) {
                return false;
            }

            int[] contextAttribs = {EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE};
            mEglContext = EGL14.eglCreateContext(mEglDisplay, configs[0], EGL14.EGL_NO_CONTEXT, contextAttribs, 0);
            if (mEglContext == EGL14.EGL_NO_CONTEXT) return false;

            int[] surfaceAttribs = {EGL14.EGL_NONE};
            mEglSurface = EGL14.eglCreateWindowSurface(mEglDisplay, configs[0], mSurfaceTexture, surfaceAttribs, 0);
            if (mEglSurface == EGL14.EGL_NO_SURFACE) return false;

            return EGL14.eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mEglContext);
        }

        private void releaseEgl() {
            if (mEglDisplay != EGL14.EGL_NO_DISPLAY) {
                EGL14.eglMakeCurrent(mEglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
                EGL14.eglDestroySurface(mEglDisplay, mEglSurface);
                EGL14.eglDestroyContext(mEglDisplay, mEglContext);
                EGL14.eglTerminate(mEglDisplay);
            }
            mEglDisplay = EGL14.EGL_NO_DISPLAY;
            mEglContext = EGL14.EGL_NO_CONTEXT;
            mEglSurface = EGL14.EGL_NO_SURFACE;
        }
    }
}
