package com.prupe.mcpatcher.mod;

import net.minecraft.src.Texture;
import net.minecraft.src.TextureStitched;

import java.util.List;

public class BorderedTexture extends TextureStitched {
    private float normalizedX0;
    private float normalizedX1;
    private float normalizedY0;
    private float normalizedY1;
    private float normalizedWidth;
    private float normalizedHeight;

    public BorderedTexture(String name) {
        super(name);
    }

    @Override
    public void setup(Texture texture, List<Texture> subTextures, int x0, int y0, int width, int height, boolean flipped) {
        super.setup(texture, subTextures, x0, y0, width, height, flipped);
        int border = subTextures == null || subTextures.isEmpty() ? 0 : subTextures.get(0).border;
        x0 += border;
        y0 += border;
        width -= 2 * border;
        height -= 2 * border;
        normalizedX0 = (float) x0 / (float) texture.getWidth();
        normalizedX1 = (float) (x0 + width) / (float) texture.getWidth();
        normalizedY0 = (float) y0 / (float) texture.getHeight();
        normalizedY1 = (float) (y0 + height) / (float) texture.getHeight();
        normalizedWidth = normalizedX1 - normalizedX0;
        normalizedHeight = normalizedY1 - normalizedY0;
        normalizedX1 = minusEpsilon(normalizedX1);
        normalizedY1 = minusEpsilon(normalizedY1);
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
        return minusEpsilon(normalizedX0 + ((float) x / 16.0f) * normalizedWidth);
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
        return minusEpsilon(normalizedY0 + ((float) y / 16.0f) * normalizedHeight);
    }

    private static float minusEpsilon(float f) {
        if (f > 0.0f) {
            return Float.intBitsToFloat(Float.floatToRawIntBits(f) - 1);
        } else {
            return f;
        }
    }
}
