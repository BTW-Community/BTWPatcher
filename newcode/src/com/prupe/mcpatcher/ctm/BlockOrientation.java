package com.prupe.mcpatcher.ctm;

import com.prupe.mcpatcher.mal.block.BlockAPI;
import net.minecraft.src.Block;
import net.minecraft.src.IBlockAccess;

import static com.prupe.mcpatcher.ctm.TileOverride.*;

final class BlockOrientation {
    private static final int[][] ROTATE_UV_MAP = new int[][]{
        {WEST_FACE, EAST_FACE, NORTH_FACE, SOUTH_FACE, TOP_FACE, BOTTOM_FACE, 2, -2, 2, -2, 0, 0},
        {NORTH_FACE, SOUTH_FACE, TOP_FACE, BOTTOM_FACE, WEST_FACE, EAST_FACE, 0, 0, 0, 0, -2, 2},
        {WEST_FACE, EAST_FACE, NORTH_FACE, SOUTH_FACE, TOP_FACE, BOTTOM_FACE, 2, -2, -2, -2, 0, 0},
        {NORTH_FACE, SOUTH_FACE, TOP_FACE, BOTTOM_FACE, WEST_FACE, EAST_FACE, 0, 0, 0, 0, -2, -2},
    };

    Block block;
    IBlockAccess blockAccess;
    int i;
    int j;
    int k;
    int metadata;
    int altMetadata;
    int metadataBits;

    int cullFace;
    int uvFace;
    int iconFace;
    int rotateUV;
    boolean rotateTop;

    int di;
    int dj;
    int dk;

    void clear() {
        block = null;
        blockAccess = null;
        i = j = k = 0;
        metadata = 0;
        cullFace = uvFace = 0;
        rotateUV = 0;
        rotateTop = false;
        di = dj = dk = 0;
    }

    void setup(Block block, IBlockAccess blockAccess, int i, int j, int k, int cullFace, int uvFace) {
        this.block = block;
        this.blockAccess = blockAccess;
        this.i = i;
        this.j = j;
        this.k = k;
        this.cullFace = cullFace;
        iconFace = uvFace;
        if (cullFace < 0) {
            this.uvFace = cullFace;
        }
        metadata = altMetadata = BlockAPI.getMetadataAt(blockAccess, i, j, k);
        metadataBits = 1 << metadata;
        rotateUV = 0;
        rotateTop = false;
    }

    void setup(Block block, IBlockAccess blockAccess, int i, int j, int k, int face) {
        this.block = block;
        this.blockAccess = blockAccess;
        this.i = i;
        this.j = j;
        this.k = k;
        cullFace = iconFace = face;
        metadata = altMetadata = BlockAPI.getMetadataAt(blockAccess, i, j, k);
        rotateUV = 0;
        rotateTop = false;
        uvFace = cullFaceToUVFace(cullFace);
        metadataBits = (1 << metadata) | (1 << altMetadata);
    }

    void setup(Block block, int metadata, int face) {
        this.block = block;
        this.blockAccess = null;
        i = j = k = 0;
        cullFace = uvFace = face;
        this.metadata = metadata;
        metadataBits = 1 << metadata;
        di = dj = dk = 0;
        rotateUV = 0;
        rotateTop = false;
    }

    private int cullFaceToUVFace(int face) {
        switch (block.getRenderType()) {
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
                        rotateTop = true;
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
                        rotateTop = true;
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
        switch (block.getRenderType()) {
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

    int rotateUV(int neighbor) {
        return (neighbor + rotateUV) & 7;
    }

    int getFaceForHV() {
        int face = uvFace;
        if (face < 0) {
            face = NORTH_FACE;
        } else if (face <= TOP_FACE) {
            face = -1;
        } else {
            face = cullFace;
        }
        return face;
    }
}
