package com.prupe.mcpatcher.mod;

import net.minecraft.src.Texture;
import net.minecraft.src.TextureStitched;

import java.util.List;

public class BorderedTexture extends TextureStitched {
    private float normalizedX0;
    private float normalizedX1;
    private float normalizedY0;
    private float normalizedY1;
    private float scaledWidth;
    private float scaledHeight;
    private int border;

    public BorderedTexture(String name) {
        super(name);
    }

    @Override
    public void init(Texture texture, List<Texture> subTextures, int x0, int y0, int width, int height, boolean flipped) {
        super.init(texture, subTextures, x0, y0, width, height, flipped);
        border = subTextures == null || subTextures.isEmpty() ? 0 : subTextures.get(0).border;
        if (border > 0) {
            x0 += border;
            y0 += border;
            width -= 2 * border;
            height -= 2 * border;
            normalizedX0 = (float) x0 / (float) texture.getWidth();
            normalizedX1 = (float) (x0 + width) / (float) texture.getWidth();
            normalizedY0 = (float) y0 / (float) texture.getHeight();
            normalizedY1 = (float) (y0 + height) / (float) texture.getHeight();
        } else {
            normalizedX0 = super.getMinU();
            normalizedX1 = super.getMaxU();
            normalizedY0 = super.getMinV();
            normalizedY1 = super.getMaxV();
        }
        scaledWidth = (normalizedX1 - normalizedX0) / 16.0f;
        scaledHeight = (normalizedY1 - normalizedY0) / 16.0f;
    }

    @Override
    public float getMinU() {
        return normalizedX0;
    }

    @Override
    public float getMaxU() {
        return normalizedX1;
    }

    @Override
    public float getInterpolatedU(double x) {
        return border > 0 ? normalizedX0 + (float) x * scaledWidth : super.getInterpolatedU(x);
    }

    @Override
    public float getMinV() {
        return normalizedY0;
    }

    @Override
    public float getMaxV() {
        return normalizedY1;
    }

    @Override
    public float getInterpolatedV(double y) {
        return border > 0 ? normalizedY0 + (float) y * scaledHeight : super.getInterpolatedV(y);
    }
}
