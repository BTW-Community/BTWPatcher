package net.minecraft.src;

public class FontRenderer {
    public int fontHeight = 9;

    public float[] charWidthf; // added by HD Font
    public ResourceLocation defaultFont; // added by HD Font
    public ResourceLocation hdFont; // added by HD Font
    public boolean isHD; // added by HD Font
    public float fontAdj; // added by HD Font

    public int getCharWidth(char c) { // 1.2.4 and up
        return 0;
    }

    public void readFontData() {
    }
}
