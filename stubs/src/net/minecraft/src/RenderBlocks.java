package net.minecraft.src;

public class RenderBlocks {
    public IBlockAccess blockAccess;

    public int uvRotateEast;
    public int uvRotateWest;
    public int uvRotateSouth;
    public int uvRotateNorth;
    public int uvRotateTop;
    public int uvRotateBottom;

    // made public by Custom Colors
    public float colorRedTopLeft;
    public float colorGreenTopLeft;
    public float colorBlueTopLeft;
    public float colorRedBottomLeft;
    public float colorGreenBottomLeft;
    public float colorBlueBottomLeft;
    public float colorRedBottomRight;
    public float colorGreenBottomRight;
    public float colorBlueBottomRight;
    public float colorRedTopRight;
    public float colorGreenTopRight;
    public float colorBlueTopRight;

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
