package net.minecraft.src;

public class RenderBlocks {
    public IBlockAccess blockAccess;

    public int uvRotateEast;
    public int uvRotateWest;
    public int uvRotateSouth;
    public int uvRotateNorth;
    public int uvRotateTop;
    public int uvRotateBottom;

    public boolean hasOverrideTexture() {
        return false;
    }

    public Icon getIconBySideAndMetadata(Block block, int face, int metadata) {
        return null;
    }

    public Icon getIconBySide(Block block, int face) {
        return null;
    }
}
