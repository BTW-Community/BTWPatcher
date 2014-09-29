package com.prupe.mcpatcher.ctm;

import com.prupe.mcpatcher.mal.block.BlockAPI;
import net.minecraft.src.Block;
import net.minecraft.src.IBlockAccess;
import net.minecraft.src.Icon;

final class BlockOrientation extends RenderBlockState {
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

    private int i;
    private int j;
    private int k;
    private int metadata;
    private int altMetadata;
    private int metadataBits;
    private int renderType;

    private int blockFace;
    private int textureFace;
    private int textureFaceOrig;
    private int rotateUV;

    private boolean offsetsComputed;
    private boolean haveOffsets;
    private int di;
    private int dj;
    private int dk;

    @Override
    public void clear() {
        super.clear();
        i = j = k = 0;
        renderType = -1;
        metadata = 0;
        blockFace = textureFace = 0;
        rotateUV = 0;
        offsetsComputed = false;
        haveOffsets = false;
        di = dj = dk = 0;
    }

    @Override
    public int getI() {
        return i;
    }

    @Override
    public int getJ() {
        return j;
    }

    @Override
    public int getK() {
        return k;
    }

    @Override
    public int getBlockFace() {
        return blockFace;
    }

    @Override
    public int getTextureFace() {
        return textureFace;
    }

    @Override
    public int getTextureFaceOrig() {
        return textureFaceOrig;
    }

    @Override
    public String getTextureFaceName() {
        throw new UnsupportedOperationException("getTextureName");
    }

    @Override
    public int getFaceForHV() {
        return blockFace;
    }

    @Override
    public int[] getOffset(int blockFace, int relativeDirection) {
        return NEIGHBOR_OFFSET[blockFace][rotateUV(relativeDirection)];
    }

    @Override
    public boolean setCoordOffsetsForRenderType() {
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

    @Override
    public int getDI() {
        return di;
    }

    @Override
    public int getDJ() {
        return dj;
    }

    @Override
    public int getDK() {
        return dk;
    }

    @Override
    public boolean shouldConnect(Block neighbor, int[] offset) {
        return (metadataBits & (1 << BlockAPI.getMetadataAt(blockAccess, i + offset[0], j + offset[1], k + offset[2]))) != 0;
    }

    void setBlock(Block block, IBlockAccess blockAccess, int i, int j, int k) {
        this.block = block;
        this.blockAccess = blockAccess;
        inWorld = true;
        this.i = i;
        this.j = j;
        this.k = k;
        renderType = block.getRenderType();
        metadata = altMetadata = BlockAPI.getMetadataAt(blockAccess, i, j, k);
        offsetsComputed = false;
    }

    // TODO: 1.8
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

    void setBlockMetadata(Block block, int metadata, int face) {
        this.block = block;
        blockAccess = null;
        inWorld = false;
        i = j = k = 0;
        renderType = block.getRenderType();
        blockFace = textureFace = textureFaceOrig = face;
        this.metadata = metadata;
        metadataBits = 1 << metadata;
        di = dj = dk = 0;
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

    private int rotateUV(int neighbor) {
        return (neighbor + rotateUV) & 7;
    }

    boolean logIt() {
        //return i == -31 && j == 72 && (k == 412 || k == 413);
        //return j == 72 && (metadata == 7 || metadata == 11) && blockFace == 1 && BlockAPI.getBlockName(block).equals("minecraft:log");
        return false;
    }
}
