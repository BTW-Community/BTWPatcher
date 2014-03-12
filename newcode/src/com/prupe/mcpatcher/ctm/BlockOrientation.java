package com.prupe.mcpatcher.ctm;

import com.prupe.mcpatcher.MCLogger;
import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.mal.block.BlockAPI;
import net.minecraft.src.Block;
import net.minecraft.src.IBlockAccess;

import java.util.IdentityHashMap;
import java.util.Map;

final class BlockOrientation {
    private static final MCLogger logger = MCLogger.getLogger(MCPatcherUtils.CONNECTED_TEXTURES, "CTM");

    static final int BOTTOM_FACE = 0; // 0, -1, 0
    static final int TOP_FACE = 1; // 0, 1, 0
    static final int NORTH_FACE = 2; // 0, 0, -1
    static final int SOUTH_FACE = 3; // 0, 0, 1
    static final int WEST_FACE = 4; // -1, 0, 0
    static final int EAST_FACE = 5; // 1, 0, 0

    static final int[] GO_DOWN = new int[]{0, -1, 0};
    static final int[] GO_UP = new int[]{0, 1, 0};
    static final int[] GO_NORTH = new int[]{0, 0, -1};
    static final int[] GO_SOUTH = new int[]{0, 0, 1};
    static final int[] GO_WEST = new int[]{-1, 0, 0};
    static final int[] GO_EAST = new int[]{1, 0, 0};

    static final int REL_L = 0;
    static final int REL_DL = 1;
    static final int REL_D = 2;
    static final int REL_DR = 3;
    static final int REL_R = 4;
    static final int REL_UR = 5;
    static final int REL_U = 6;
    static final int REL_UL = 7;

    private static final int[][] ROTATE_UV_MAP = new int[][]{
        {WEST_FACE, EAST_FACE, NORTH_FACE, SOUTH_FACE, TOP_FACE, BOTTOM_FACE, 2, -2, 2, -2, 0, 0},
        {NORTH_FACE, SOUTH_FACE, TOP_FACE, BOTTOM_FACE, WEST_FACE, EAST_FACE, 0, 0, 0, 0, -2, 2},
        {WEST_FACE, EAST_FACE, NORTH_FACE, SOUTH_FACE, TOP_FACE, BOTTOM_FACE, 2, -2, -2, -2, 0, 0},
        {NORTH_FACE, SOUTH_FACE, TOP_FACE, BOTTOM_FACE, WEST_FACE, EAST_FACE, 0, 0, 0, 0, -2, -2},
    };

    private static final Map<Block, Integer> fakeRenderTypes = new IdentityHashMap<Block, Integer>();

    // NEIGHBOR_OFFSETS[a][b][c] = offset from starting block
    // a: face 0-5
    // b: neighbor 0-7
    //    7   6   5
    //    0   *   4
    //    1   2   3
    // c: coordinate (x,y,z) 0-2
    private static final int[][][] NEIGHBOR_OFFSET = new int[][][]{
        // BOTTOM_FACE
        {
            GO_WEST,
            add(GO_WEST, GO_SOUTH),
            GO_SOUTH,
            add(GO_EAST, GO_SOUTH),
            GO_EAST,
            add(GO_EAST, GO_NORTH),
            GO_NORTH,
            add(GO_WEST, GO_NORTH),
        },
        // TOP_FACE
        {
            GO_WEST,
            add(GO_WEST, GO_SOUTH),
            GO_SOUTH,
            add(GO_EAST, GO_SOUTH),
            GO_EAST,
            add(GO_EAST, GO_NORTH),
            GO_NORTH,
            add(GO_WEST, GO_NORTH),
        },
        // NORTH_FACE
        {
            GO_EAST,
            add(GO_EAST, GO_DOWN),
            GO_DOWN,
            add(GO_WEST, GO_DOWN),
            GO_WEST,
            add(GO_WEST, GO_UP),
            GO_UP,
            add(GO_EAST, GO_UP),
        },
        // SOUTH_FACE
        {
            GO_WEST,
            add(GO_WEST, GO_DOWN),
            GO_DOWN,
            add(GO_EAST, GO_DOWN),
            GO_EAST,
            add(GO_EAST, GO_UP),
            GO_UP,
            add(GO_WEST, GO_UP),
        },
        // WEST_FACE
        {
            GO_NORTH,
            add(GO_NORTH, GO_DOWN),
            GO_DOWN,
            add(GO_SOUTH, GO_DOWN),
            GO_SOUTH,
            add(GO_SOUTH, GO_UP),
            GO_UP,
            add(GO_NORTH, GO_UP),
        },
        // EAST_FACE
        {
            GO_SOUTH,
            add(GO_SOUTH, GO_DOWN),
            GO_DOWN,
            add(GO_NORTH, GO_DOWN),
            GO_NORTH,
            add(GO_NORTH, GO_UP),
            GO_UP,
            add(GO_SOUTH, GO_UP),
        },
    };

    Block block;
    IBlockAccess blockAccess;
    int i;
    int j;
    int k;
    int metadata;
    private int altMetadata;
    int metadataBits;
    private int renderType;

    int blockFace;
    int textureFace;
    int textureFaceOrig;
    int rotateUV;

    int di;
    int dj;
    int dk;

    static void reset() {
        fakeRenderTypes.clear();
        registerFakeRenderType("wooden_door", 7);
        registerFakeRenderType("iron_door", 7);
        registerFakeRenderType("ladder", 8);
        registerFakeRenderType("bed", 14);
        registerFakeRenderType("vine", 20);
        registerFakeRenderType("log", 31);
        registerFakeRenderType("log2", 31);
        registerFakeRenderType("hay_block", 31);
        registerFakeRenderType("quartz_block", 39);
        registerFakeRenderType("double_plant", 40);
    }

    private static void registerFakeRenderType(String name, int renderType) {
        Block block = BlockAPI.parseBlockName("minecraft:" + name);
        if (block != null && block.getRenderType() == 43) {
            fakeRenderTypes.put(block, renderType);
        }
    }

    private static int[] add(int[] a, int[] b) {
        if (a.length != b.length) {
            throw new RuntimeException("arrays to add are not same length");
        }
        int[] c = new int[a.length];
        for (int i = 0; i < c.length; i++) {
            c[i] = a[i] + b[i];
        }
        return c;
    }

    void clear() {
        block = null;
        blockAccess = null;
        i = j = k = 0;
        renderType = -1;
        metadata = 0;
        blockFace = textureFace = 0;
        rotateUV = 0;
        di = dj = dk = 0;
    }

    void setBlock(Block block, IBlockAccess blockAccess, int i, int j, int k) {
        this.block = block;
        this.blockAccess = blockAccess;
        this.i = i;
        this.j = j;
        this.k = k;
        renderType = block.getRenderType();
        metadata = altMetadata = BlockAPI.getMetadataAt(blockAccess, i, j, k);
    }

    void setFakeRenderType() {
        Integer fakeRenderType = fakeRenderTypes.get(block);
        if (fakeRenderType != null) {
            renderType = fakeRenderType;
        }
    }

    void setFace(int blockFace, int textureFace, int rotation) {
        this.blockFace = blockFace;
        this.textureFace = textureFaceOrig = textureFace;
        if (blockFace < 0) {
            this.textureFace = blockFace;
        }
        blockFaceToTextureFace(blockFace);
        rotateUV = rotation;
        metadataBits = (1 << metadata) | (1 << altMetadata);
    }

    void setFace(int face) {
        blockFace = textureFaceOrig = face;
        rotateUV = 0;
        textureFace = blockFaceToTextureFace(blockFace);
        metadataBits = (1 << metadata) | (1 << altMetadata);
    }

    void setup(Block block, int metadata, int face) {
        this.block = block;
        this.blockAccess = null;
        i = j = k = 0;
        renderType = block.getRenderType();
        blockFace = textureFace = textureFaceOrig = face;
        this.metadata = metadata;
        metadataBits = 1 << metadata;
        di = dj = dk = 0;
        rotateUV = 0;
    }

    private int blockFaceToTextureFace(int face) {
        switch (renderType) {
            case 1: // renderCrossedSquares
                return -1;

            case 8: // renderBlockLadder
                switch (metadata) {
                    case 2:
                    case 3:
                    case 4:
                    case 5:
                        return metadata;

                    default:
                        break;
                }
                break;

            case 20: // renderBlockVine
                switch (metadata) {
                    case 1:
                        return NORTH_FACE;

                    case 2:
                        return EAST_FACE;

                    case 4:
                        return SOUTH_FACE;

                    case 8:
                        return WEST_FACE;

                    default:
                        break;
                }
                break;

            case 31: // renderBlockLog (also applies to hay)
                switch (metadata & 0xc) {
                    case 4: // west-east
                        altMetadata &= ~0xc;
                        rotateUV = ROTATE_UV_MAP[0][face + 6];
                        return ROTATE_UV_MAP[0][face];

                    case 8: // north-south
                        altMetadata &= ~0xc;
                        rotateUV = ROTATE_UV_MAP[1][face + 6];
                        return ROTATE_UV_MAP[1][face];

                    default:
                        break;
                }
                break;

            case 39: // renderBlockQuartz
                switch (metadata) {
                    case 3: // north-south
                        altMetadata = 2;
                        rotateUV = ROTATE_UV_MAP[2][face + 6];
                        return ROTATE_UV_MAP[2][face];

                    case 4: // west-east
                        altMetadata = 2;
                        rotateUV = ROTATE_UV_MAP[3][face + 6];
                        return ROTATE_UV_MAP[3][face];

                    default:
                        break;
                }
                break;

            default:
                break;
        }
        return face;
    }

    boolean setCoordOffsetsForRenderType() {
        di = dj = dk = 0;
        boolean changed = false;
        switch (renderType) {
            case 1: // renderCrossedSquares
                while (j > 0 && block == BlockAPI.getBlockAt(blockAccess, i, j - 1, k)) {
                    j--;
                    dj--;
                    changed = true;
                }
                break;

            case 7: // renderBlockDoor
            case 40: // renderBlockDoublePlant
                if ((metadata & 0x8) != 0 && block == BlockAPI.getBlockAt(blockAccess, i, j - 1, k)) {
                    dj--;
                    changed = true;
                }
                break;

            case 14: // renderBlockBed
                metadata = BlockAPI.getMetadataAt(blockAccess, i, j, k);
                switch (metadata) {
                    case 0:
                    case 4:
                        dk = 1; // head is one block south
                        break;

                    case 1:
                    case 5:
                        di = -1; // head is one block west
                        break;

                    case 2:
                    case 6:
                        dk = -1; // head is one block north
                        break;

                    case 3:
                    case 7:
                        di = 1; // head is one block east
                        break;

                    default:
                        return false; // head itself, no offset
                }
                changed = block == BlockAPI.getBlockAt(blockAccess, i + di, j, k + dk);
                break;

            default:
                break;
        }
        return changed;
    }

    private int rotateUV(int neighbor) {
        return (neighbor + rotateUV) & 7;
    }

    int[] getOffset(int relativeDirection) {
        return getOffset(blockFace, relativeDirection);
    }

    int[] getOffset(int blockFace, int relativeDirection) {
        return NEIGHBOR_OFFSET[blockFace][rotateUV(relativeDirection)];
    }

    int getFaceForHV() {
        return blockFace;
    }

    void logIt(String format, Object... params) {
        if (i == -63 && j == 72 && k == 392) {
            logger.info(format, params);
        }

    }
}
