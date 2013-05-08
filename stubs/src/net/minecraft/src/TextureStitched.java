package net.minecraft.src;

import java.util.List;

public class TextureStitched implements Icon {
    public int getWidth() {
        return 0;
    }

    public int getHeight() {
        return 0;
    }

    public float getMinU() {
        return 0;
    }

    public float getMaxU() {
        return 0;
    }

    public float getInterpolatedU(double u) {
        return 0;
    }

    public float getMinV() {
        return 0;
    }

    public float getMaxV() {
        return 0;
    }

    public float getInterpolatedV(double v) {
        return 0;
    }

    public String getIconName() {
        return null;
    }

    public int getSheetWidth() {
        return 0;
    }

    public int getSheetHeight() {
        return 0;
    }

    public TextureStitched(String name) {
    }

    public void init(Texture texture, List<Texture> subTextures, int x0, int y0, int width, int height, boolean flipped) {
    }

    public void init(int tilesheetWidth, int tilesheetHeight, int x0, int y0, boolean flipped) {
    }

    public void updateAnimation() {
    }

    public void copy(TextureStitched stitched) {
    }
}