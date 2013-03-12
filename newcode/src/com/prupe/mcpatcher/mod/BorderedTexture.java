package com.prupe.mcpatcher.mod;

import net.minecraft.src.Texture;
import net.minecraft.src.TextureStitched;

import java.util.List;

public class BorderedTexture extends TextureStitched {
    public BorderedTexture(String name) {
        super(name);
    }

    @Override
    public void setup(Texture texture, List subTextures, int x0, int y0, int width, int height, boolean flipped) {
        super.setup(texture, subTextures, x0, y0, width, height, flipped);
    }

    @Override
    public int getX0() {
        return 0;
    }

    @Override
    public int getY0() {
        return 0;
    }

    @Override
    public float getNormalizedX0() {
        return 0;
    }

    @Override
    public float getNormalizedX1() {
        return 0;
    }

    @Override
    public float interpolateX(double var1) {
        return 0;
    }

    @Override
    public float getNormalizedY0() {
        return 0;
    }

    @Override
    public float getNormalizedY1() {
        return 0;
    }

    @Override
    public float interpolateY(double var1) {
        return 0;
    }
}
