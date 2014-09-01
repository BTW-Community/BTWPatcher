package com.prupe.mcpatcher.ctm;

import com.prupe.mcpatcher.mal.block.BlockStateMatcher;
import net.minecraft.src.*;

abstract public class RenderBlockState {
    public static final int BOTTOM_FACE = 0; // 0, -1, 0
    public static final int TOP_FACE = 1; // 0, 1, 0
    public static final int NORTH_FACE = 2; // 0, 0, -1
    public static final int SOUTH_FACE = 3; // 0, 0, 1
    public static final int WEST_FACE = 4; // -1, 0, 0
    public static final int EAST_FACE = 5; // 1, 0, 0

    public static final int[] GO_DOWN = new int[]{0, -1, 0};
    public static final int[] GO_UP = new int[]{0, 1, 0};
    public static final int[] GO_NORTH = new int[]{0, 0, -1};
    public static final int[] GO_SOUTH = new int[]{0, 0, 1};
    public static final int[] GO_WEST = new int[]{-1, 0, 0};
    public static final int[] GO_EAST = new int[]{1, 0, 0};

    public static final int[][] NORMALS = new int[][]{
        GO_DOWN,
        GO_UP,
        GO_NORTH,
        GO_SOUTH,
        GO_WEST,
        GO_EAST,
    };

    public static final int REL_L = 0;
    public static final int REL_DL = 1;
    public static final int REL_D = 2;
    public static final int REL_DR = 3;
    public static final int REL_R = 4;
    public static final int REL_UR = 5;
    public static final int REL_U = 6;
    public static final int REL_UL = 7;

    protected IBlockAccess blockAccess;
    protected Block block;
    protected boolean useAO;
    protected boolean inWorld;
    protected BlockStateMatcher matcher;

    final public IBlockAccess getBlockAccess() {
        return blockAccess;
    }

    final public Block getBlock() {
        return block;
    }

    final public boolean useAO() {
        return useAO;
    }

    final public boolean isInWorld() {
        return inWorld;
    }

    final public void setFilter(BlockStateMatcher matcher) {
        this.matcher = matcher;
    }

    final public BlockStateMatcher getFilter() {
        return matcher;
    }

    public void clear() {
        blockAccess = null;
        block = null;
        useAO = false;
        inWorld = false;
    }

    abstract public int getI();

    abstract public int getJ();

    abstract public int getK();

    abstract public int getBlockFace();

    abstract public int getTextureFace();

    abstract public int getTextureFaceOrig();

    abstract public int getFaceForHV();

    abstract public int[] getOffset(int blockFace, int relativeDirection);

    abstract public boolean setCoordOffsetsForRenderType();

    abstract public int getDI();

    abstract public int getDJ();

    abstract public int getDK();
}
