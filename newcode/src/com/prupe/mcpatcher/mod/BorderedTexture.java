package com.prupe.mcpatcher.mod;

import net.minecraft.src.Texture;
import net.minecraft.src.TextureStitched;

import java.util.List;

public class BorderedTexture extends TextureStitched {
    private float minU;
    private float maxU;
    private float minV;
    private float maxV;
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
            minU = (float) x0 / (float) texture.getWidth();
            maxU = (float) (x0 + width) / (float) texture.getWidth();
            minV = (float) y0 / (float) texture.getHeight();
            maxV = (float) (y0 + height) / (float) texture.getHeight();
        } else {
            minU = super.getMinU();
            maxU = super.getMaxU();
            minV = super.getMinV();
            maxV = super.getMaxV();
        }
        scaledWidth = (maxU - minU) / 16.0f;
        scaledHeight = (maxV - minV) / 16.0f;
    }

    @Override
    public float getMinU() {
        return minU;
    }

    @Override
    public float getMaxU() {
        return maxU;
    }

    @Override
    public float getInterpolatedU(double u) {
        return border > 0 ? minU + (float) u * scaledWidth : super.getInterpolatedU(u);
    }

    @Override
    public float getMinV() {
        return minV;
    }

    @Override
    public float getMaxV() {
        return maxV;
    }

    @Override
    public float getInterpolatedV(double v) {
        return border > 0 ? minV + (float) v * scaledHeight : super.getInterpolatedV(v);
    }
}
