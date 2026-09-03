package com.frostbyte.launcher.skins;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds the vertex/UV data for a real 3D Minecraft player model, box by box, using the
 * standard 64x64 skin texture layout (the same layout every Minecraft-compatible skin
 * viewer uses — unchanged since the 1.8 skin format).
 *
 * Each body part is an axis-aligned box. A box becomes 6 quads (2 triangles each), and each
 * quad face is mapped to its own UV rectangle read from the skin texture. Coordinates are in
 * "Minecraft model units" where 1 unit = 1 pixel of a 64px-tall skin, so the whole model is
 * built at a consistent, correct relative scale (head is 8x8x8, torso 8x12x4, etc).
 */
public class SkinModelGeometry {

    /**
     * One renderable box: its 3D extent, position offset, and where each face samples from the
     * texture. The GL-ready direct buffers are built ONCE here (not per draw call/frame) — the
     * geometry never changes after construction, so re-wrapping the same arrays into brand-new
     * off-heap direct ByteBuffers 60 times a second for every box was pure wasted allocation,
     * and on real hardware (as opposed to a one-shot static render) that churn is exactly what
     * shows up as stutter/glitching during the model's auto-rotate.
     */
    public static class Box {
        public final float[] vertices; // x,y,z per vertex, 24 vertices (4 per face x 6 faces)
        public final float[] uvs;      // u,v per vertex, matching the vertex order
        public final short[] indices;  // 2 triangles per face x 6 faces = 36 indices

        public final FloatBuffer vertexBuffer;
        public final FloatBuffer uvBuffer;
        public final ShortBuffer indexBuffer;

        Box(float[] vertices, float[] uvs, short[] indices) {
            this.vertices = vertices;
            this.uvs = uvs;
            this.indices = indices;

            this.vertexBuffer = ByteBuffer.allocateDirect(vertices.length * 4)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer();
            this.vertexBuffer.put(vertices).position(0);

            this.uvBuffer = ByteBuffer.allocateDirect(uvs.length * 4)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer();
            this.uvBuffer.put(uvs).position(0);

            this.indexBuffer = ByteBuffer.allocateDirect(indices.length * 2)
                    .order(ByteOrder.nativeOrder()).asShortBuffer();
            this.indexBuffer.put(indices).position(0);
        }
    }

    /** The full model: base layer boxes, then overlay (jacket/sleeve/hat) boxes drawn slightly larger on top. */
    public static class Model {
        public final List<Box> baseBoxes = new ArrayList<>();
        public final List<Box> overlayBoxes = new ArrayList<>();
    }

    private static final int TEX_SIZE = 64;


    public static Model build(boolean slimArms) {
        Model model = new Model();
        float armWidth = slimArms ? 3f : 4f;

        // Positions are in pixels, origin at model center on X/Z, feet at Y=0, matching how
        // Minecraft itself lays the model out. Y grows upward.

        // Head: 8x8x8, centered on X/Z, sits from y=24 to y=32 (on a 32px-tall model)
        model.baseBoxes.add(box(8, 8, 8, -4, 24, -4, 0, 0, true));
        model.overlayBoxes.add(box(8.5f, 8.5f, 8.5f, -4.25f, 23.75f, -4.25f, 32, 0, true));

        // Torso: 8 wide, 12 tall, 4 deep, sits from y=12 to y=24
        model.baseBoxes.add(box(8, 12, 4, -4, 12, -2, 16, 16, false));
        model.overlayBoxes.add(box(8.5f, 12.5f, 4.5f, -4.25f, 11.75f, -2.25f, 16, 32, false));

        // Right arm (player's right = model's left side visually, matches Minecraft's own convention)
        model.baseBoxes.add(box(armWidth, 12, 4, -4 - armWidth, 12, -2, 40, 16, false));
        model.overlayBoxes.add(box(armWidth + 0.5f, 12.5f, 4.5f, -4.25f - armWidth, 11.75f, -2.25f, 40, 32, false));

        // Left arm — 64x64 format gives it independent UVs instead of mirroring the right arm
        model.baseBoxes.add(box(armWidth, 12, 4, 4, 12, -2, 32, 48, false));
        model.overlayBoxes.add(box(armWidth + 0.5f, 12.5f, 4.5f, 3.75f, 11.75f, -2.25f, 48, 48, false));

        // Right leg: 4x12x4, sits from y=0 to y=12
        model.baseBoxes.add(box(4, 12, 4, -4, 0, -2, 0, 16, false));
        model.overlayBoxes.add(box(4.5f, 12.5f, 4.5f, -4.25f, -0.25f, -2.25f, 0, 32, false));

        // Left leg — independent UV region in the 64x64 format
        model.baseBoxes.add(box(4, 12, 4, 0, 0, -2, 16, 48, false));
        model.overlayBoxes.add(box(4.5f, 12.5f, 4.5f, -0.25f, -0.25f, -2.25f, 0, 48, false));

        return model;
    }

    /**
     * Builds one box's geometry.
     * @param w,h,d box dimensions in pixels
     * @param ox,oy,oz box origin (minimum corner) in model space
     * @param u,v UV origin for this box's face layout on the texture sheet
     * @param isHead head UV layout differs slightly in row order from limb/torso boxes
     */
    private static Box box(float w, float h, float d, float ox, float oy, float oz, int u, int v, boolean isHead) {
        // 8 corners of the box
        float x0 = ox, x1 = ox + w;
        float y0 = oy, y1 = oy + h;
        float z0 = oz, z1 = oz + d;

        // 6 faces x 4 vertices = 24 vertices. Order per face: matches a standard box-UV unwrap
        // (top, bottom, right, front, left, back), which is the conventional order used by
        // Minecraft's own model format and every compatible renderer.
        float[] vertices = new float[]{
                // Top face (y1) - reversed winding so its normal faces +Y
                x0, y1, z1,  x1, y1, z1,  x1, y1, z0,  x0, y1, z0,
                // Bottom face (y0)
                x0, y0, z0,  x1, y0, z0,  x1, y0, z1,  x0, y0, z1,
                // Right face (x1) - facing +X
                x1, y1, z0,  x1, y1, z1,  x1, y0, z1,  x1, y0, z0,
                // Front face (z1) - reversed winding so its normal faces +Z
                x0, y0, z1,  x1, y0, z1,  x1, y1, z1,  x0, y1, z1,
                // Left face (x0) - facing -X
                x0, y1, z1,  x0, y1, z0,  x0, y0, z0,  x0, y0, z1,
                // Back face (z0) - reversed winding so its normal faces -Z
                x1, y0, z0,  x0, y0, z0,  x0, y1, z0,  x1, y1, z0,
        };

        int dU = Math.round(d), wU = Math.round(w), hU = Math.round(h);

        // Each face lists its 4 UV corners in the SAME order as that face's 4 vertices above.
        // Standard Minecraft box UV unwrap strip: (top)(bottom)(right)(front)(left)(back).
        float[] topUv = uvCorners(u + dU, v, wU, dU, "BL", "BR", "TR", "TL");
        float[] bottomUv = uvCorners(u + dU + wU, v, wU, dU, "TL", "TR", "BR", "BL");
        float[] rightUv = uvCorners(u, v + dU, dU, hU, "TL", "TR", "BR", "BL");
        float[] frontUv = uvCorners(u + dU, v + dU, wU, hU, "BL", "BR", "TR", "TL");
        float[] leftUv = uvCorners(u + dU + wU, v + dU, dU, hU, "TL", "TR", "BR", "BL");
        float[] backUv = uvCorners(u + dU + wU + dU, v + dU, wU, hU, "BR", "BL", "TL", "TR");

        float[] uvs = new float[48];
        System.arraycopy(topUv, 0, uvs, 0, 8);
        System.arraycopy(bottomUv, 0, uvs, 8, 8);
        System.arraycopy(rightUv, 0, uvs, 16, 8);
        System.arraycopy(frontUv, 0, uvs, 24, 8);
        System.arraycopy(leftUv, 0, uvs, 32, 8);
        System.arraycopy(backUv, 0, uvs, 40, 8);

        short[] indices = new short[36];
        for (int face = 0; face < 6; face++) {
            short base = (short) (face * 4);
            int i = face * 6;
            indices[i] = base;     indices[i + 1] = (short) (base + 1); indices[i + 2] = (short) (base + 2);
            indices[i + 3] = base; indices[i + 4] = (short) (base + 2); indices[i + 5] = (short) (base + 3);
        }

        return new Box(vertices, uvs, indices);
    }

    /**
     * Returns 4 UV coordinate pairs (8 floats) for a face's texture rectangle, normalized 0-1,
     * in the exact corner order requested. Each corner is "TL","TR","BR", or "BL" (top-left,
     * top-right, bottom-right, bottom-left of the texture rectangle) so callers can match
     * their own vertex traversal order explicitly instead of assuming a fixed shape.
     */
    private static float[] uvCorners(int u, int v, int w, int h, String c0, String c1, String c2, String c3) {
        float u0 = u / (float) TEX_SIZE;
        float u1 = (u + w) / (float) TEX_SIZE;
        float v0 = v / (float) TEX_SIZE;
        float v1 = (v + h) / (float) TEX_SIZE;
        float[] result = new float[8];
        String[] corners = {c0, c1, c2, c3};
        for (int i = 0; i < 4; i++) {
            switch (corners[i]) {
                case "TL": result[i * 2] = u0; result[i * 2 + 1] = v0; break;
                case "TR": result[i * 2] = u1; result[i * 2 + 1] = v0; break;
                case "BR": result[i * 2] = u1; result[i * 2 + 1] = v1; break;
                case "BL": result[i * 2] = u0; result[i * 2 + 1] = v1; break;
            }
        }
        return result;
    }
}
