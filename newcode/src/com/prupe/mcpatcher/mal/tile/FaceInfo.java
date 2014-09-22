package com.prupe.mcpatcher.mal.tile;

import com.prupe.mcpatcher.MCLogger;
import net.minecraft.src.*;

import java.util.IdentityHashMap;
import java.util.Map;

final public class FaceInfo {
    private static final MCLogger logger = MCLogger.getLogger("Tilesheet");

    // duplicated from RenderBlockState
    private static final int BOTTOM_FACE = 0; // 0, -1, 0
    private static final int TOP_FACE = 1; // 0, 1, 0
    private static final int NORTH_FACE = 2; // 0, 0, -1
    private static final int SOUTH_FACE = 3; // 0, 0, 1
    private static final int WEST_FACE = 4; // -1, 0, 0
    private static final int EAST_FACE = 5; // 1, 0, 0

    private static final int[] GO_DOWN = new int[]{0, -1, 0};
    private static final int[] GO_UP = new int[]{0, 1, 0};
    private static final int[] GO_NORTH = new int[]{0, 0, -1};
    private static final int[] GO_SOUTH = new int[]{0, 0, 1};
    private static final int[] GO_WEST = new int[]{-1, 0, 0};
    private static final int[] GO_EAST = new int[]{1, 0, 0};

    private static final int[][] NORMALS = new int[][]{
        GO_DOWN,
        GO_UP,
        GO_NORTH,
        GO_SOUTH,
        GO_WEST,
        GO_EAST,
    };

    private static final Map<ModelFace, FaceInfo> faceInfoMap = new IdentityHashMap<ModelFace, FaceInfo>();

    private final ModelFace face;
    private final TextureAtlasSprite sprite;
    private final Map<Icon, ModelFace> altIcons = new IdentityHashMap<Icon, ModelFace>();
    private final int effectiveFace;
    private final int uvRotation;
    private final String textureName;
    private final int textureFacingBits;

    static void clear() {
        faceInfoMap.clear();
    }

    public static ModelFace registerModelFaceSprite(ModelFace face, TextureAtlasSprite sprite, String textureName) {
        FaceInfo faceInfo = faceInfoMap.get(face);
        if (faceInfo == null) {
            faceInfo = new FaceInfo(face, sprite, textureName);
            faceInfoMap.put(face, faceInfo);
        }
        return face;
    }

    public static FaceInfo getFaceInfo(ModelFace face) {
        return faceInfoMap.get(face);
    }

    public FaceInfo(ModelFace face, TextureAtlasSprite sprite, String textureName) {
        this.face = face;
        if (face instanceof ModelFaceSprite) {
            this.sprite = ((ModelFaceSprite) face).sprite;
        } else {
            this.sprite = sprite;
        }
        effectiveFace = computeEffectiveFace(face.getIntBuffer());
        uvRotation = computeRotation(face.getIntBuffer(), getEffectiveFace());
        if (textureName.startsWith("#")) {
            textureName = textureName.substring(1);
        }
        textureName = textureName.toLowerCase().trim();
        this.textureName = textureName;
        textureFacingBits = computeTextureFacing(textureName);
    }

    @Override
    public String toString() {
        return String.format("FaceInfo{%s,%s/%s -> %s R%+d}",
            face, sprite.getIconName(), textureName, Direction.values()[effectiveFace], uvRotation * 45
        );
    }

    public TextureAtlasSprite getSprite() {
        return sprite;
    }

    public synchronized ModelFace getAltFace(TextureAtlasSprite altSprite) {
        if (altSprite == sprite) {
            return face;
        }
        ModelFace newFace = altIcons.get(altSprite);
        if (newFace == null) {
            newFace = new ModelFaceSprite(face, altSprite);
            recalculateUV(sprite, face.getIntBuffer(), altSprite, newFace.getIntBuffer());
            altIcons.put(altSprite, newFace);
        }
        return newFace;
    }

    public int getEffectiveFace() {
        return effectiveFace;
    }

    public int getUVRotation() {
        return uvRotation;
    }

    public String getTextureName() {
        return textureName;
    }

    public boolean isTextureFacing(int bits) {
        return (textureFacingBits & bits) != 0;
    }

    private static int computeEffectiveFace(int[] b) {
        float ax = Float.intBitsToFloat(b[0]) - Float.intBitsToFloat(b[7]); // x0 - x1
        float ay = Float.intBitsToFloat(b[1]) - Float.intBitsToFloat(b[8]); // y0 - y1
        float az = Float.intBitsToFloat(b[2]) - Float.intBitsToFloat(b[9]); // z0 - z1
        float bx = Float.intBitsToFloat(b[0]) - Float.intBitsToFloat(b[21]); // x0 - x3
        float by = Float.intBitsToFloat(b[1]) - Float.intBitsToFloat(b[22]); // y0 - y3
        float bz = Float.intBitsToFloat(b[2]) - Float.intBitsToFloat(b[23]); // z0 - z3
        float cx = ay * bz - by * az; // cross product
        float cy = bx * az - ax * bz;
        float cz = ax * by - bx * ay;
        int bigidx = -1;
        float[] dot = new float[6];
        float bigdot = 0.0f;
        for (int i = 0; i < NORMALS.length; i++) {
            int[] n = NORMALS[i];
            dot[i] = (float) n[0] * cx + (float) n[1] * cy + (float) n[2] * cz;
            if (dot[i] > bigdot) {
                bigdot = dot[i];
                bigidx = i;
            }
        }
        if (bigidx < 0) {
            logger.warning("a: %f %f %f", ax, ay, az);
            logger.warning("b: %f %f %f", bx, by, bz);
            logger.warning("c: %f %f %f", cx, cy, cz);
            logger.warning("dot: %f %f %f %f %f %f", dot[0], dot[1], dot[2], dot[3], dot[4], dot[5]);
            bigidx = NORTH_FACE;
        }
        return bigidx;
    }

    private static int computeRotation(int[] b, int face) {
        return (computeXYZRotation(b, face) - computeUVRotation(b)) & 0x7;
    }

    private static int computeUVRotation(int[] b) {
        float du = Float.intBitsToFloat(b[4]) - Float.intBitsToFloat(b[18]); // u0 - u2
        float dv = Float.intBitsToFloat(b[5]) - Float.intBitsToFloat(b[19]); // v0 - v2
        return computeRotation(du, dv);
    }

    private static int computeXYZRotation(int[] b, int face) {
        float dx = Float.intBitsToFloat(b[0]) - Float.intBitsToFloat(b[14]); // x0 - x2
        float dy = Float.intBitsToFloat(b[1]) - Float.intBitsToFloat(b[15]); // y0 - y2
        float dz = Float.intBitsToFloat(b[2]) - Float.intBitsToFloat(b[16]); // z0 - z2
        float du;
        float dv;
        switch (face) {
            case BOTTOM_FACE:
                du = dx;
                dv = -dz;
                break;

            case TOP_FACE:
                du = dx;
                dv = dz;
                break;

            case NORTH_FACE:
                du = -dx;
                dv = -dy;
                break;

            case SOUTH_FACE:
                du = dx;
                dv = -dy;
                break;

            case WEST_FACE:
                du = dz;
                dv = -dy;
                break;

            case EAST_FACE:
                du = -dz;
                dv = -dy;
                break;

            default:
                return 0;
        }
        return computeRotation(du, dv);
    }

    private static int computeRotation(float s, float t) {
        if (s <= 0) {
            if (t <= 0) {
                return 0; // no rotation
            } else {
                return 2; // rotate 90 ccw
            }
        } else {
            if (t <= 0) {
                return 6; // rotate 90 cw
            } else {
                return 4; // rotate 180
            }
        }
    }

    private static void logRotation(int[] b, int face) {
        int x0 = (int) (Float.intBitsToFloat(b[0]) * 16.0f);
        int y0 = (int) (Float.intBitsToFloat(b[1]) * 16.0f);
        int z0 = (int) (Float.intBitsToFloat(b[2]) * 16.0f);
        int u0 = (int) Float.intBitsToFloat(b[4]);
        int v0 = (int) Float.intBitsToFloat(b[5]);

        int x1 = (int) (Float.intBitsToFloat(b[7]) * 16.0f);
        int y1 = (int) (Float.intBitsToFloat(b[8]) * 16.0f);
        int z1 = (int) (Float.intBitsToFloat(b[9]) * 16.0f);
        int u1 = (int) Float.intBitsToFloat(b[11]);
        int v1 = (int) Float.intBitsToFloat(b[12]);

        int x2 = (int) (Float.intBitsToFloat(b[14]) * 16.0f);
        int y2 = (int) (Float.intBitsToFloat(b[15]) * 16.0f);
        int z2 = (int) (Float.intBitsToFloat(b[16]) * 16.0f);
        int u2 = (int) Float.intBitsToFloat(b[18]);
        int v2 = (int) Float.intBitsToFloat(b[19]);

        int x3 = (int) (Float.intBitsToFloat(b[21]) * 16.0f);
        int y3 = (int) (Float.intBitsToFloat(b[22]) * 16.0f);
        int z3 = (int) (Float.intBitsToFloat(b[23]) * 16.0f);
        int u3 = (int) Float.intBitsToFloat(b[25]);
        int v3 = (int) Float.intBitsToFloat(b[26]);

        logger.info("x0,y0,z0=%d %d %d, x1,y1,z1=%d %d %d, x2,y2,z2=%d %d %d, x3,y3,z3=%d %d %d, rotation %d",
            x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3, computeXYZRotation(b, face)
        );
        logger.info("u0,v0=%d %d, u1,v1=%d %d, u2,v2=%d %d, u3,v3=%d %d, rotation %d",
            u0, v0, u1, v1, u2, v2, u3, v3, computeUVRotation(b)
        );
    }

    private static int computeTextureFacing(String name) {
        int bits = ~0x3f;
        for (Direction direction : Direction.values()) {
            if (name.equalsIgnoreCase(direction.toString())) {
                bits |= (1 << direction.ordinal());
            }
        }
        if (name.equals("bottom")) {
            bits |= (1 << BOTTOM_FACE);
        } else if (name.equals("top")) {
            bits |= (1 << TOP_FACE);
        } else {
            if (name.equals("side") || name.equals("sides") || name.equals("all")) {
                bits |= (1 << NORTH_FACE) | (1 << SOUTH_FACE) | (1 << WEST_FACE) | (1 << EAST_FACE);
            }
            if (name.equals("end") || name.equals("ends") || name.equals("all")) {
                bits |= (1 << BOTTOM_FACE) | (1 << TOP_FACE);
            }
        }
        return bits;
    }

    private static void recalculateUV(TextureAtlasSprite origIcon, int[] a, TextureAtlasSprite newIcon, int[] b) {
        for (int i = 0; i < 28; i += 7) {
            float u = 16.0f * (Float.intBitsToFloat(a[i + 4]) - origIcon.getMinU()) / (origIcon.getMaxU() - origIcon.getMinU());
            float v = 16.0f * (Float.intBitsToFloat(a[i + 5]) - origIcon.getMinV()) / (origIcon.getMaxV() - origIcon.getMinV());
            b[i + 4] = Float.floatToIntBits(newIcon.getInterpolatedU(u));
            b[i + 5] = Float.floatToIntBits(newIcon.getInterpolatedV(v));
        }
    }
}
