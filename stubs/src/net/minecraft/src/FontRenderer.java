package net.minecraft.src;

public class FontRenderer {
    public int fontHeight = 9;

    public float[] charWidthf; // added by HD Font
    public boolean isHD; // added by HD Font
    public float fontAdj; // added by HD Font

    // added by HD Font
    public ResourceLocation getDefaultFont() {
        return null;
    }

    // added by HD Font
    public void setDefaultFont(ResourceLocation resource) {
    }

    // added by HD Font
    public ResourceLocation getHDFont() {
        return null;
    }

    // added by HD Font
    public void setHDFont(ResourceLocation resource) {
    }

    public int getCharWidth(char c) { // 1.2.4 and up
        return 0;
    }

    public void readFontData() {
    }
}
