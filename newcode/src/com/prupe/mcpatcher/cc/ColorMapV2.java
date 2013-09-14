package com.prupe.mcpatcher.cc;

import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.WeightedIndex;
import com.prupe.mcpatcher.mal.biome.BiomeAPI;
import net.minecraft.src.BiomeGenBase;

import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.Properties;

final class ColorMapV2 {
    private static final int COLORMAP_WIDTH = 256;
    private static final int COLORMAP_HEIGHT = 256;

    final int width;
    final int height;
    final float maxX;
    final float maxY;
    final int[] rgb;
    final float[] biomeStart = new float[BiomeGenBase.biomeList.length];
    final float[] biomeWidth = new float[BiomeGenBase.biomeList.length];
    final int defaultColor;

    final float yScale;
    final float yVariance;

    ColorMapV2(BufferedImage image, Properties properties) {
        width = image.getWidth();
        height = image.getHeight();
        maxX = width - 1.0f;
        maxY = height - 1.0f;
        rgb = new int[width * height];
        image.getRGB(0, 0, width, height, rgb, 0, width);
        float xScale = (float) width / (float) COLORMAP_WIDTH;
        yScale = (float) height / (float) COLORMAP_HEIGHT;
        yVariance = MCPatcherUtils.getFloatProperty(properties, "yVariance", yScale - 1.0f);
        for (int i = 0; i < biomeStart.length; i++) {
            if (xScale > 1.0f) {
                biomeStart[i] = (float) i * xScale;
                biomeWidth[i] = xScale - 1.0f;
            } else {
                biomeStart[i] = i % width;
            }
        }
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            if (key.endsWith(".x") && !MCPatcherUtils.isNullOrEmpty(value)) {
                key = key.substring(0, key.length() - 2);
                BiomeGenBase biome = BiomeAPI.findBiomeByName(key);
                if (biome != null) {
                    String[] token = value.trim().split("-");
                    try {
                        float start = clamp(Float.parseFloat(token[0]), 0.0f, maxX);
                        float end = clamp(token.length > 1 ? Float.parseFloat(token[1]) : start, 0.0f, maxX);
                        biomeStart[biome.biomeID] = start;
                        biomeWidth[biome.biomeID] = end - start;
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        defaultColor = MCPatcherUtils.getHexProperty(properties, "defaultColor", getRGB(0.0f, 64.0f * yScale));
    }

    float getX(BiomeGenBase biome, int i, int j, int k) {
        int id = biome.biomeID;
        float x = biomeStart[id];
        float w = biomeWidth[id];
        if (w != 0.0f) {
            x += w * noise0to1(i, j, k, id);
        }
        return x;
    }

    float getY(BiomeGenBase biome, int i, int j, int k) {
        float y = (float) j * yScale;
        if (yVariance > 0.0f) {
            y += yVariance * noiseMinus1to1(k, -j, i, ~biome.biomeID);
        }
        return y;
    }

    int getColorMultiplier(BiomeGenBase biome, int i, int j, int k) {
        return getRGB(getX(biome, i, j, k), getY(biome, i, j, k));
    }

    int getColorMultiplier() {
        return defaultColor;
    }

    int getRGB(float x, float y) {
        x = clamp(x, 0.0f, maxX);
        y = clamp(y, 0.0f, maxY);

        int x0 = (int) x;
        int dx = (int) (256.0f * (x - (float) x0));
        int x1 = x0 + 1;

        int y0 = (int) y;
        int dy = (int) (256.0f * (y - (float) y0));
        int y1 = y0 + 1;

        if (dx == 0 && dy == 0) {
            return getRGB(x0, y0);
        } else if (dx == 0) {
            return interpolate(x0, y0, x0, y1, dy);
        } else if (dy == 0) {
            return interpolate(x0, y0, x1, y0, dx);
        } else {
            return interpolate(
                interpolate(x0, y0, x1, y0, dx),
                interpolate(x0, y1, x1, y1, dx),
                dy
            );
        }
    }

    private int getRGB(int x, int y) {
        return rgb[x + width * y];
    }

    private int interpolate(int x1, int y1, int x2, int y2, int a1) {
        return interpolate(getRGB(x1, y1), getRGB(x2, y2), a1);
    }

    private static int interpolate(int rgb1, int rgb2, int a1) {
        int a2 = 256 - a1;

        int r1 = (rgb1 >> 16) & 0xff;
        int g1 = (rgb1 >> 8) & 0xff;
        int b1 = rgb1 & 0xff;

        int r2 = (rgb2 >> 16) & 0xff;
        int g2 = (rgb2 >> 8) & 0xff;
        int b2 = rgb2 & 0xff;

        int r = (a1 * r1 + a2 * r2) >> 8;
        int g = (a1 * g1 + a2 * g2) >> 8;
        int b = (a1 * b1 + a2 * b2) >> 8;

        return (r << 16) | (g << 8) | b;
    }

    private static float noise0to1(int i, int j, int k, int l) {
        int hash = (int) WeightedIndex.hash128To64(i, j, k, l) & Integer.MAX_VALUE;
        return (float) ((double) hash / (double) Integer.MAX_VALUE);
    }

    private static float noiseMinus1to1(int i, int j, int k, int l) {
        int hash = (int) WeightedIndex.hash128To64(i, j, k, l);
        return (float) ((double) hash / (double) Integer.MIN_VALUE);
    }

    private static float clamp(float i, float min, float max) {
        if (i < min) {
            return min;
        } else if (i > max) {
            return max;
        } else {
            return i;
        }
    }
}
