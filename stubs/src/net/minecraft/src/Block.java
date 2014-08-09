package net.minecraft.src;

public class Block {
    public static Registry<Block> blockRegistry; // 13w36a-14w24
    public static BlockRegistry blockRegistry1; // 14w25a+

    // pre-13w36a
    public static Block[] blocksList;
    public static int[] lightValue;

    public int blockID;
    public Material blockMaterial;

    public Icon getBlockIcon(IBlockAccess blockAccess, int i, int j, int k, int face) {
        return null;
    }

    public boolean renderAsNormalBlock() {
        return false;
    }

    public int getRenderBlockPass() {
        return 0;
    }

    public boolean shouldSideBeRendered(IBlockAccess blockAccess, int i, int j, int k, int face) {
        return true;
    }

    public String getShortName() {
        return null;
    }

    public int getRenderType() {
        return 0;
    }

    public int getBlockColor() {
        return 0xffffff;
    }

    public int getRenderColor(int metadata) {
        return 0xfffffff;
    }

    public int colorMultiplier(IBlockAccess blockAccess, int i, int j, int k) {
        return 0xffffff;
    }

    // 13w36a+
    public int getLightValue() {
        return 0;
    }

    // 14w02a+
    public Icon getBlockIcon(IBlockAccess blockAccess, Position position, Direction direction) {
        return null;
    }

    public boolean shouldSideBeRendered(IBlockAccess blockAccess, Position position, Direction direction) {
        return true;
    }

    // 14w03a+
    public RenderPassEnum getRenderBlockPassEnum() {
        return null;
    }

    // 14w25a+
    public IBlockState getBlockState() {
        return null;
    }
}
