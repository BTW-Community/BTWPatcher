package com.prupe.mcpatcher.ctm;

import com.prupe.mcpatcher.MCLogger;
import com.prupe.mcpatcher.MCPatcherUtils;
import net.minecraft.src.Icon;
import net.minecraft.src.ModelFace;
import net.minecraft.src.ModelFaceSprite;
import net.minecraft.src.TextureAtlasSprite;

import java.util.IdentityHashMap;
import java.util.Map;

public class CTMUtils18 {
    private static final MCLogger logger = MCLogger.getLogger(MCPatcherUtils.CONNECTED_TEXTURES);

    private static final Map<ModelFace, FaceInfo> faceInfoMap = new IdentityHashMap<ModelFace, FaceInfo>();

    public static void reset() {
        logger.info("CTMUtils18.reset");
        faceInfoMap.clear();
        CTMUtils.reset();
    }

    public static ModelFace registerModelFaceSprite(ModelFace face, TextureAtlasSprite sprite) {
        FaceInfo faceInfo = faceInfoMap.get(face);
        if (faceInfo == null) {
            faceInfo = new FaceInfo(face, sprite);
            faceInfoMap.put(face, faceInfo);
        }
        return face;
    }

    public static FaceInfo getFaceInfo(ModelFace face) {
        return faceInfoMap.get(face);
    }

    final public static class FaceInfo {
        private final ModelFace face;
        private final TextureAtlasSprite sprite;
        private final Map<Icon, ModelFace> altIcons = new IdentityHashMap<Icon, ModelFace>();
        private final int effectiveFace;
        private final int uvRotation;

        public FaceInfo(ModelFace face, TextureAtlasSprite sprite) {
            this.face = face;
            if (face instanceof ModelFaceSprite) {
                this.sprite = ((ModelFaceSprite) face).sprite;
            } else {
                this.sprite = sprite;
            }
            effectiveFace = computeEffectiveFace(face.getIntBuffer());
            uvRotation = computeRotation(face.getIntBuffer(), getEffectiveFace());
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
            for (int i = 0; i < RenderBlockState.NORMALS.length; i++) {
                int[] n = RenderBlockState.NORMALS[i];
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
                bigidx = RenderBlockState.NORTH_FACE;
            }
            return bigidx;
        }

        private static int computeRotation(int[] b, int face) {
            return computeXYZRotation(b, face) - computeUVRotation(b);
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
                case BlockOrientation.BOTTOM_FACE:
                    du = dx;
                    dv = -dz;
                    break;

                case BlockOrientation.TOP_FACE:
                    du = dx;
                    dv = dz;
                    break;

                case BlockOrientation.NORTH_FACE:
                    du = -dx;
                    dv = -dy;
                    break;

                case BlockOrientation.SOUTH_FACE:
                    du = dx;
                    dv = -dy;
                    break;

                case BlockOrientation.WEST_FACE:
                    du = dz;
                    dv = -dy;
                    break;

                case BlockOrientation.EAST_FACE:
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
    }
}
