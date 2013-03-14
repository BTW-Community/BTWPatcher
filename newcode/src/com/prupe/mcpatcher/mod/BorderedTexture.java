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
    public void setup(Texture texture, List<Texture> subTextures, int x0, int y0, int width, int height, boolean flipped) {
        super.setup(texture, subTextures, x0, y0, width, height, flipped);
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
            normalizedX0 = super.getNormalizedX0();
            normalizedX1 = super.getNormalizedX1();
            normalizedY0 = super.getNormalizedY0();
            normalizedY1 = super.getNormalizedY1();
        }
        scaledWidth = (normalizedX1 - normalizedX0) / 16.0f;
        scaledHeight = (normalizedY1 - normalizedY0) / 16.0f;
    }

    @Override
    public float getNormalizedX0() {
        return normalizedX0;
    }

    @Override
    public float getNormalizedX1() {
        return normalizedX1;
    }

    @Override
    public float interpolateX(double x) {
        return border > 0 ? normalizedX0 + (float) x * scaledWidth : super.interpolateX(x);
    }

    @Override
    public float getNormalizedY0() {
        return normalizedY0;
    }

    @Override
    public float getNormalizedY1() {
        return normalizedY1;
    }

    @Override
    public float interpolateY(double y) {
        return border > 0 ? normalizedY0 + (float) y * scaledHeight : super.interpolateY(y);
    }
}
