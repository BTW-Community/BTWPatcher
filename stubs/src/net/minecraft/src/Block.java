package net.minecraft.src;

public class Block {
    public static Block blocksList[];
    public static int lightValue[];

    public int blockID;

    public int getBlockTexture(IBlockAccess blockAccess, int i, int j, int k, int face) {
        return 0;
    }

    public boolean renderAsNormalBlock() {
        return false;
    }

    public int getRenderBlockPass() {
        return 0;
    }

    public float blockStrength(EntityPlayer thePlayer) {
        return 0;
    }

    public boolean shouldSideBeRendered(IBlockAccess blockAccess, int i, int j, int k, int face) {
        return true;
    }
}
