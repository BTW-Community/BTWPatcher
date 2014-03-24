package com.prupe.mcpatcher.ctm;

import com.prupe.mcpatcher.mal.block.BlockAPI;
import net.minecraft.src.Block;
import net.minecraft.src.IBlockAccess;

import java.util.IdentityHashMap;
import java.util.Map;

final class BlockOrientation {
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

    static final int[][] NORMALS = new int[][]{
        GO_DOWN,
        GO_UP,
        GO_NORTH,
        GO_SOUTH,
        GO_WEST,
        GO_EAST,
    };

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

    // NEIGHBOR_OFFSETS[a][b][c] = offset from starting block
    // a: face 0-5
    // b: neighbor 0-7
    //    7   6   5
    //    0   *   4
    //    1   2   3
    // c: coordinate (x,y,z) 0-2
    private static final int[][][] NEIGHBOR_OFFSET = new int[][][]{
        makeNeighborOffset(WEST_FACE, SOUTH_FACE, EAST_FACE, NORTH_FACE), // BOTTOM_FACE
        makeNeighborOffset(WEST_FACE, SOUTH_FACE, EAST_FACE, NORTH_FACE), // TOP_FACE
        makeNeighborOffset(EAST_FACE, BOTTOM_FACE, WEST_FACE, TOP_FACE), // NORTH_FACE
        makeNeighborOffset(WEST_FACE, BOTTOM_FACE, EAST_FACE, TOP_FACE), // SOUTH_FACE
        makeNeighborOffset(NORTH_FACE, BOTTOM_FACE, SOUTH_FACE, TOP_FACE), // WEST_FACE
        makeNeighborOffset(SOUTH_FACE, BOTTOM_FACE, NORTH_FACE, TOP_FACE), // EAST_FACE
    };

    private static final int[][][] NEIGHBOR_OFFSET_43 = new int[][][]{
        makeNeighborOffset(WEST_FACE, NORTH_FACE, EAST_FACE, SOUTH_FACE), // BOTTOM_FACE - flipped n-s in 1.8 custom models
        makeNeighborOffset(WEST_FACE, SOUTH_FACE, EAST_FACE, NORTH_FACE), // TOP_FACE
        makeNeighborOffset(EAST_FACE, BOTTOM_FACE, WEST_FACE, TOP_FACE), // NORTH_FACE
        makeNeighborOffset(WEST_FACE, BOTTOM_FACE, EAST_FACE, TOP_FACE), // SOUTH_FACE
        makeNeighborOffset(NORTH_FACE, BOTTOM_FACE, SOUTH_FACE, TOP_FACE), // WEST_FACE
        makeNeighborOffset(SOUTH_FACE, BOTTOM_FACE, NORTH_FACE, TOP_FACE), // EAST_FACE
    };

    private static final Map<Block, Integer> fakeRenderTypes = new IdentityHashMap<Block, Integer>();

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
    private int[][][] neighborOffsets;
    private int rotateUV;

    boolean offsetsComputed;
    boolean haveOffsets;
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

    private static int[][] makeNeighborOffset(int left, int down, int right, int up) {
        int[] l = NORMALS[left];
        int[] d = NORMALS[down];
        int[] r = NORMALS[right];
        int[] u = NORMALS[up];
        return new int[][]{
            l,
            add(l, d),
            d,
            add(d, r),
            r,
            add(r, u),
            u,
            add(u, l),
        };
    }

    void clear() {
        block = null;
        blockAccess = null;
        i = j = k = 0;
        renderType = -1;
        metadata = 0;
        blockFace = textureFace = 0;
        neighborOffsets = null;
        rotateUV = 0;
        offsetsComputed = false;
        haveOffsets = false;
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
        neighborOffsets = NEIGHBOR_OFFSET;
        offsetsComputed = false;
    }

    void setFakeRenderType() {
        Integer fakeRenderType = fakeRenderTypes.get(block);
        if (fakeRenderType != null) {
            renderType = fakeRenderType;
        }
        neighborOffsets = NEIGHBOR_OFFSET_43;
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
        blockFace = getBlockFaceByRenderType(face);
        textureFaceOrig = face;
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
        neighborOffsets = NEIGHBOR_OFFSET_43;
        rotateUV = 0;
    }

    private int getBlockFaceByRenderType(int face) {
        switch (renderType) {
            case 1: // renderCrossedSquares
                return NORTH_FACE;

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

            default:
                break;
        }
        return face;
    }

    private int blockFaceToTextureFace(int face) {
        switch (renderType) {
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
        if (offsetsComputed) {
            return haveOffsets;
        }
        offsetsComputed = true;
        haveOffsets = false;
        di = dj = dk = 0;
        switch (renderType) {
            case 1: // renderCrossedSquares
                while (j + dj > 0 && block == BlockAPI.getBlockAt(blockAccess, i, j + dj - 1, k)) {
                    dj--;
                    haveOffsets = true;
                }
                break;

            case 7: // renderBlockDoor
            case 40: // renderBlockDoublePlant
                if ((metadata & 0x8) != 0 && block == BlockAPI.getBlockAt(blockAccess, i, j - 1, k)) {
                    dj--;
                    haveOffsets = true;
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
                haveOffsets = block == BlockAPI.getBlockAt(blockAccess, i + di, j, k + dk);
                break;

            default:
                break;
        }
        return haveOffsets;
    }

    private int rotateUV(int neighbor) {
        return (neighbor + rotateUV) & 7;
    }

    int[] getOffset(int relativeDirection) {
        return getOffset(blockFace, relativeDirection);
    }

    int[] getOffset(int blockFace, int relativeDirection) {
        return neighborOffsets[blockFace][rotateUV(relativeDirection)];
    }

    int getFaceForHV() {
        return blockFace;
    }

    boolean logIt() {
        //return i == -31 && j == 72 && (k == 412 || k == 413);
        //return j == 72 && (metadata == 7 || metadata == 11) && blockFace == 1 && BlockAPI.getBlockName(block).equals("minecraft:log");
        return false;
    }
}
