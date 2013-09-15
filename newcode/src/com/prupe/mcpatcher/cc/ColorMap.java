package com.prupe.mcpatcher.cc;

import com.prupe.mcpatcher.MCLogger;
import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.TexturePackAPI;
import com.prupe.mcpatcher.WeightedIndex;
import com.prupe.mcpatcher.mal.biome.BiomeAPI;
import net.minecraft.src.BiomeGenBase;
import net.minecraft.src.ResourceLocation;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;

import static com.prupe.mcpatcher.cc.Colorizer.intToFloat3;

abstract class ColorMap {
    private static final MCLogger logger = MCLogger.getLogger(MCPatcherUtils.CUSTOM_COLORS);

    private static final int COLORMAP_WIDTH = 256;
    private static final int COLORMAP_HEIGHT = 256;

    private final int[] map;
    private final int mapDefault;
    final int width;
    final int height;
    final float maxX;
    final float maxY;

    private final float[] xy = new float[2];

    private int lastBlendI = Integer.MIN_VALUE;
    private int lastBlendJ = Integer.MIN_VALUE;
    private int lastBlendK = Integer.MIN_VALUE;
    private final float[] tmpBlendResult = new float[3];
    private final float[] lastBlendResult = new float[3];

    static ColorMap loadColorMap(boolean useCustom, ResourceLocation resource) {
        if (!useCustom) {
            return null;
        }
        BufferedImage image = TexturePackAPI.getImage(resource);
        if (image == null) {
            return null;
        }
        ResourceLocation propertiesResource = TexturePackAPI.transformResourceLocation(resource, ".png", ".properties");
        Properties properties = TexturePackAPI.getProperties(propertiesResource);
        if (properties == null) {
            properties = new Properties();
        }
        int format = MCPatcherUtils.getIntProperty(properties, "format", 1);
        if (format <= 1) {
            return new V1(image, properties);
        } else if (format == 2) {
            return new V2(image, properties);
        } else {
            logger.error("%s: unknown format %d", propertiesResource, format);
            return null;
        }
    }

    static int getBlockMetaKey(int blockID, int metadata) {
        return (blockID << 4) | (metadata & 0xf);
    }

    ColorMap(BufferedImage image, Properties properties) {
        map = MCPatcherUtils.getImageRGB(image);
        width = image.getWidth();
        height = image.getHeight();
        maxX = width - 1.0f;
        maxY = height - 1.0f;
        mapDefault = MCPatcherUtils.getHexProperty(properties, "defaultColor", getDefaultColor());
    }

    void multiplyMap(float[] f) {
        float[] g = new float[3];
        for (int i = 0; i < map.length; i++) {
            Colorizer.intToFloat3(map[i], g);
            g[0] *= f[0];
            g[1] *= f[1];
            g[2] *= f[2];
            map[i] = Colorizer.float3ToInt(g);
        }
    }

    int getColorMultiplier(int i, int j, int k) {
        computeXY(BiomeAPI.getBiomeGenAt(i, j, k), i, j, k, xy);
        return getRGB(xy[0], xy[1]);
    }

    int getColorMultiplier(int i, int j, int k, int radius) {
        getColorMultiplier(i, j, k, radius, tmpBlendResult);
        return Colorizer.float3ToInt(tmpBlendResult);
    }

    void getColorMultiplier(int i, int j, int k, int radius, float[] result) {
        if (i == lastBlendI && j == lastBlendJ && k == lastBlendK) {
            System.arraycopy(lastBlendResult, 0, result, 0, 3);
            return;
        }
        Arrays.fill(result, 0.0f);
        for (int offsetI = -radius; offsetI <= radius; offsetI++) {
            for (int offsetK = -radius; offsetK <= radius; offsetK++) {
                int rgb = getColorMultiplier(i, j, k);
                intToFloat3(rgb, lastBlendResult);
                result[0] += lastBlendResult[0];
                result[1] += lastBlendResult[1];
                result[2] += lastBlendResult[2];
            }
        }
        int diameter = 2 * radius + 1;
        float scale = 1.0f / (float) (diameter * diameter);
        result[0] *= scale;
        result[1] *= scale;
        result[2] *= scale;
        System.arraycopy(result, 0, lastBlendResult, 0, 3);
        lastBlendI = i;
        lastBlendJ = j;
        lastBlendK = k;
    }

    int getColorMultiplier() {
        return mapDefault;
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
        return map[x + width * y];
    }

    private int interpolate(int x1, int y1, int x2, int y2, int a2) {
        return interpolate(getRGB(x1, y1), getRGB(x2, y2), a2);
    }

    private static int interpolate(int rgb1, int rgb2, int a2) {
        int a1 = 256 - a2;

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

    abstract int getDefaultColor();

    abstract void computeXY(BiomeGenBase biome, int i, int j, int k, float[] f);

    private static final class V1 extends ColorMap {
        private V1(BufferedImage image, Properties properties) {
            super(image, properties);
        }

        @Override
        int getDefaultColor() {
            return getRGB(maxX * 0.5f, maxY * 0.5f);
        }

        @Override
        void computeXY(BiomeGenBase biome, int i, int j, int k, float[] f) {
            float temperature = Colorizer.clamp(BiomeAPI.getTemperature(biome, i, j, k));
            float rainfall = Colorizer.clamp(BiomeAPI.getRainfall(biome, i, j, k));
            f[0] = maxX * (1.0f - temperature);
            f[1] = maxY * (1.0f - temperature * rainfall);
        }
    }

    private static final class V2 extends ColorMap {
        private final float[] biomeStart = new float[BiomeGenBase.biomeList.length];
        private final float[] biomeWidth = new float[BiomeGenBase.biomeList.length];

        private final float yScale;
        private final float yVariance;

        private V2(BufferedImage image, Properties properties) {
            super(image, properties);

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
        }

        @Override
        int getDefaultColor() {
            return getRGB(0.0f, 64.0f);
        }

        @Override
        void computeXY(BiomeGenBase biome, int i, int j, int k, float[] f) {
            f[0] = getX(biome, i, j, k);
            f[1] = getY(biome, i, j, k);
        }

        private float getX(BiomeGenBase biome, int i, int j, int k) {
            int id = biome.biomeID;
            float x = biomeStart[id];
            float w = biomeWidth[id];
            if (w != 0.0f) {
                x += w * noise0to1(i, j, k, id);
            }
            return x;
        }

        private float getY(BiomeGenBase biome, int i, int j, int k) {
            float y = (float) j * yScale;
            if (yVariance != 0.0f) {
                y += yVariance * noiseMinus1to1(k, -j, i, ~biome.biomeID);
            }
            return y;
        }
    }

    /*
    static int getX(double temperature, double rainfall) {
        return (int) (COLORMAP_SCALE * (1.0 - Colorizer.clamp(temperature)));
    }

    static int getY(double temperature, double rainfall) {
        return (int) (COLORMAP_SCALE * (1.0 - Colorizer.clamp(rainfall) * Colorizer.clamp(temperature)));
    }

    static float getBlockMetaKey(int blockID, int metadata) {
        return blockID + (metadata & 0xff) / 256.0f;
    }

    ColorMap(int defaultColor) {
        mapDefault = defaultColor;
    }

    void loadColorMap(boolean useCustom, ResourceLocation resource) {
        if (!useCustom) {
            return;
        }
        map = MCPatcherUtils.getImageRGB(TexturePackAPI.getImage(resource));
        if (map == null) {
            return;
        }
        if (map.length != COLORMAP_SIZE * COLORMAP_SIZE) {
            logger.error("%s must be %dx%d", resource, COLORMAP_SIZE, COLORMAP_SIZE);
            map = null;
            return;
        }
        mapDefault = colorize(0xffffff, 0.5, 1.0);
        logger.fine("using %s, default color %06x", resource, mapDefault);
    }

    boolean isCustom() {
        return map != null;
    }

    void clear() {
        map = null;
    }

    int colorize() {
        return mapDefault;
    }

    int colorize(int defaultColor) {
        return map == null ? defaultColor : mapDefault;
    }

    int colorize(int defaultColor, double temperature, double rainfall) {
        if (map == null) {
            return defaultColor;
        } else {
            return map[COLORMAP_SIZE * getY(temperature, rainfall) + getX(temperature, rainfall)];
        }
    }

    int colorize(int defaultColor, int i, int j, int k) {
        return colorize(defaultColor, BiomeAPI.getTemperature(i, j, k), BiomeAPI.getRainfall(i, j, k));
    }
    */
}
