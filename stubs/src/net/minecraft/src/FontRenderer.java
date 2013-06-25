package net.minecraft.src;

public class FontRenderer {
    public int fontHeight = 9;

    public float[] charWidthf; // added by HD Font
    public ResourceAddress defaultFont; // added by HD Font
    public ResourceAddress hdFont; // added by HD Font
    public boolean isHD; // added by HD Font

    public int getCharWidth(char c) { // 1.2.4 and up
        return 0;
    }

    public void readFontData() {
    }
}
