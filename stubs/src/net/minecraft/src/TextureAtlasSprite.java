package net.minecraft.src;

import java.nio.IntBuffer;
import java.util.List;

public class TextureAtlasSprite implements Icon {
    public List<int[]> animationFrames; // made public by Extended HD
    public List<IntBuffer[]> mipmaps; // added by Extended HD

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

    public TextureAtlasSprite(String name) {
    }

    // 1.5
    public void init(Texture texture, List<Texture> animations, int x0, int y0, int width, int height, boolean flipped) {
    }

    // 1.6+
    public void init(int tilesheetWidth, int tilesheetHeight, int x0, int y0, boolean flipped) {
    }

    public int getX0() {
        return 0;
    }

    public int getY0() {
        return 0;
    }

    public void updateAnimation() {
    }

    public void copy(TextureAtlasSprite stitched) {
    }

    // 14w25+
    public static TextureAtlasSprite createSprite(ResourceLocation resource) {
        return null;
    }
}