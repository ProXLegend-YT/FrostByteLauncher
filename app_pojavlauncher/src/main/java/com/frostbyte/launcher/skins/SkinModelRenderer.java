package com.frostbyte.launcher.skins;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class SkinModelRenderer implements GLSurfaceView.Renderer {

    private static final String VERTEX_SHADER =
            "uniform mat4 uMVPMatrix;" +
            "attribute vec4 aPosition;" +
            "attribute vec2 aTexCoord;" +
            "varying vec2 vTexCoord;" +
            "void main() {" +
            "  gl_Position = uMVPMatrix * aPosition;" +
            "  vTexCoord = aTexCoord;" +
            "}";

    private static final String FRAGMENT_SHADER =
            "precision mediump float;" +
            "uniform sampler2D uTexture;" +
            "varying vec2 vTexCoord;" +
            "void main() {" +
            "  vec4 color = texture2D(uTexture, vTexCoord);" +
            "  if (color.a < 0.1) discard;" +
            "  gl_FragColor = color;" +
            "}";

    private int mProgram;
    private int mPositionHandle, mTexCoordHandle, mMvpHandle, mTextureHandle;
    private int mTextureId = -1;

    private final float[] mProjectionMatrix = new float[16];
    private final float[] mViewMatrix = new float[16];
    private final float[] mModelMatrix = new float[16];
    private final float[] mMvpMatrix = new float[16];

    private volatile Bitmap mPendingSkinBitmap;
    private volatile boolean mSlimArms;
    private volatile boolean mGeometryDirty = true;

    /** Current rotation, in degrees, driven by drag gestures and/or auto-spin. */
    public volatile float rotationYDegrees = 20f;
    public volatile float rotationXDegrees = -10f;
    public volatile boolean autoRotate = true;
    private static final float AUTO_ROTATE_SPEED_DEG_PER_FRAME = 0.4f;

    private List<SkinModelGeometry.Box> mBaseBoxes;
    private List<SkinModelGeometry.Box> mOverlayBoxes;

    public void setSkin(Bitmap bitmap, boolean slimArms) {
        mPendingSkinBitmap = bitmap;
        mSlimArms = slimArms;
        mGeometryDirty = true;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0f, 0f, 0f, 0f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glCullFace(GLES20.GL_BACK);

        int vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);
        mProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(mProgram, vertexShader);
        GLES20.glAttachShader(mProgram, fragmentShader);
        GLES20.glLinkProgram(mProgram);

        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "aPosition");
        mTexCoordHandle = GLES20.glGetAttribLocation(mProgram, "aTexCoord");
        mMvpHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        mTextureHandle = GLES20.glGetUniformLocation(mProgram, "uTexture");
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        float aspect = (float) width / height;
        Matrix.perspectiveM(mProjectionMatrix, 0, 35f, aspect, 1f, 200f);
        Matrix.setLookAtM(mViewMatrix, 0, 0f, 16f, 55f, 0f, 16f, 0f, 0f, 1f, 0f);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        if (mGeometryDirty && mPendingSkinBitmap != null) {
            uploadTexture(mPendingSkinBitmap);
            SkinModelGeometry.Model model = SkinModelGeometry.build(mSlimArms);
            mBaseBoxes = model.baseBoxes;
            mOverlayBoxes = model.overlayBoxes;
            mGeometryDirty = false;
        }

        if (mBaseBoxes == null || mTextureId == -1) return;

        if (autoRotate) {
            rotationYDegrees += AUTO_ROTATE_SPEED_DEG_PER_FRAME;
        }

        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.rotateM(mModelMatrix, 0, rotationXDegrees, 1f, 0f, 0f);
        Matrix.rotateM(mModelMatrix, 0, rotationYDegrees, 0f, 1f, 0f);

        float[] vpMatrix = new float[16];
        Matrix.multiplyMM(vpMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);
        Matrix.multiplyMM(mMvpMatrix, 0, vpMatrix, 0, mModelMatrix, 0);

        GLES20.glUseProgram(mProgram);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId);
        GLES20.glUniform1i(mTextureHandle, 0);
        GLES20.glUniformMatrix4fv(mMvpHandle, 1, false, mMvpMatrix, 0);

        for (SkinModelGeometry.Box b : mBaseBoxes) drawBox(b);
        for (SkinModelGeometry.Box b : mOverlayBoxes) drawBox(b);
    }

    private void drawBox(SkinModelGeometry.Box box) {
        FloatBuffer vertexBuffer = toFloatBuffer(box.vertices);
        FloatBuffer uvBuffer = toFloatBuffer(box.uvs);
        ShortBuffer indexBuffer = toShortBuffer(box.indices);

        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glVertexAttribPointer(mPositionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        GLES20.glEnableVertexAttribArray(mTexCoordHandle);
        GLES20.glVertexAttribPointer(mTexCoordHandle, 2, GLES20.GL_FLOAT, false, 0, uvBuffer);

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, box.indices.length, GLES20.GL_UNSIGNED_SHORT, indexBuffer);

        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLES20.glDisableVertexAttribArray(mTexCoordHandle);
    }

    private void uploadTexture(Bitmap bitmap) {
        if (mTextureId != -1) {
            GLES20.glDeleteTextures(1, new int[]{mTextureId}, 0);
        }
        int[] textureIds = new int[1];
        GLES20.glGenTextures(1, textureIds, 0);
        mTextureId = textureIds[0];
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
    }

    private static int compileShader(int type, String source) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);
        return shader;
    }

    private static FloatBuffer toFloatBuffer(float[] data) {
        FloatBuffer buffer = ByteBuffer.allocateDirect(data.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        buffer.put(data).position(0);
        return buffer;
    }

    private static ShortBuffer toShortBuffer(short[] data) {
        ShortBuffer buffer = ByteBuffer.allocateDirect(data.length * 2)
                .order(ByteOrder.nativeOrder()).asShortBuffer();
        buffer.put(data).position(0);
        return buffer;
    }
}
