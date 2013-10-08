package com.prupe.mcpatcher.cc;

import com.prupe.mcpatcher.MCLogger;
import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.TexturePackAPI;
import com.prupe.mcpatcher.WeightedIndex;
import com.prupe.mcpatcher.mal.biome.BiomeAPI;
import net.minecraft.src.BiomeGenBase;
import net.minecraft.src.ResourceLocation;

import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.Properties;

abstract class ColorMap implements IColorMap {
    private static final MCLogger logger = MCLogger.getLogger(MCPatcherUtils.CUSTOM_COLORS);

    private static final int COLORMAP_WIDTH = 256;
    private static final int COLORMAP_HEIGHT = 256;

    private final ResourceLocation resource;
    protected final int[] map;
    protected final int width;
    protected final int height;
    protected final float maxX;
    protected final float maxY;

    private final float[] xy = new float[2];
    private final float[] lastResult = new float[3];

    static IColorMap loadColorMap(boolean useCustom, ResourceLocation resource) {
        return loadColorMap(useCustom, resource, null);
    }

    static IColorMap loadColorMap(boolean useCustom, ResourceLocation resource, ResourceLocation swampResource) {
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
        switch (format) {
            case 0:
                int color = MCPatcherUtils.getHexProperty(properties, "color", 0xffffff);
                return new ColorMapBase.Fixed(color);

            case 1:
                IColorMap defaultMap = new TempHumidity(resource, image, properties);
                IColorMap swampMap = loadColorMap(Colorizer.useSwampColors, swampResource);
                if (swampMap != null) {
                    return new ColorMapBase.Swamp(defaultMap, swampMap);
                }
                return defaultMap;

            case 2:
                return new Grid(resource, image, properties);

            default:
                logger.error("%s: unknown format %d", propertiesResource, format);
                return null;
        }
    }

    ColorMap(ResourceLocation resource, BufferedImage image, Properties properties) {
        this.resource = resource;
        map = MCPatcherUtils.getImageRGB(image);
        width = image.getWidth();
        height = image.getHeight();
        for (int i = 0; i < map.length; i++) {
            map[i] &= 0xffffff;
        }
        maxX = width - 1.0f;
        maxY = height - 1.0f;
    }

    abstract protected void computeXY(BiomeGenBase biome, int i, int j, int k, float[] f);

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + resource + "}";
    }

    @Override
    public final int getColorMultiplier(int i, int j, int k) {
        computeXY(BiomeAPI.getBiomeGenAt(i, j, k), i, j, k, xy);
        return getRGB(xy[0], xy[1]);
    }

    @Override
    public final float[] getColorMultiplierF(int i, int j, int k) {
        int rgb = getColorMultiplier(i, j, k);
        Colorizer.intToFloat3(rgb, lastResult);
        return lastResult;
    }

    protected int getRGB(float x, float y) {
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

    protected static float noise0to1(int i, int j, int k, int l) {
        int hash = (int) WeightedIndex.hash128To64(i, j, k, l) & Integer.MAX_VALUE;
        return (float) ((double) hash / (double) Integer.MAX_VALUE);
    }

    protected static float noiseMinus1to1(int i, int j, int k, int l) {
        int hash = (int) WeightedIndex.hash128To64(i, j, k, l);
        return (float) ((double) hash / (double) Integer.MIN_VALUE);
    }

    protected static float clamp(float i, float min, float max) {
        if (i < min) {
            return min;
        } else if (i > max) {
            return max;
        } else {
            return i;
        }
    }

    static final class TempHumidity extends ColorMap {
        private TempHumidity(ResourceLocation resource, BufferedImage image, Properties properties) {
            super(resource, image, properties);
        }

        @Override
        public boolean isHeightDependent() {
            return BiomeAPI.isColorHeightDependent;
        }

        @Override
        public int getColorMultiplier() {
            return getRGB(maxX * 0.5f, maxY * 0.5f);
        }

        @Override
        protected void computeXY(BiomeGenBase biome, int i, int j, int k, float[] f) {
            float temperature = Colorizer.clamp(BiomeAPI.getTemperature(biome, i, j, k));
            float rainfall = Colorizer.clamp(BiomeAPI.getRainfall(biome, i, j, k));
            f[0] = maxX * (1.0f - temperature);
            f[1] = maxY * (1.0f - temperature * rainfall);
        }
    }

    static final class Grid extends ColorMap {
        private final float[] biomeStart = new float[BiomeGenBase.biomeList.length];
        private final float[] biomeWidth = new float[BiomeGenBase.biomeList.length];

        private final float yScale;
        private final float yVariance;

        private Grid(ResourceLocation resource, BufferedImage image, Properties properties) {
            super(resource, image, properties);

            int[] temp = new int[width];
            for (int i = 0; i < map.length / 2; i += width) {
                int j = map.length - width - i;
                System.arraycopy(map, i, temp, 0, width);
                System.arraycopy(map, j, map, i, width);
                System.arraycopy(temp, 0, map, j, width);
            }

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
        public boolean isHeightDependent() {
            return true;
        }

        @Override
        public int getColorMultiplier() {
            return getRGB(biomeStart[1], getY(ColorMapBase.DEFAULT_HEIGHT));
        }

        @Override
        protected void computeXY(BiomeGenBase biome, int i, int j, int k, float[] f) {
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

        private float getY(int j) {
            return (float) j * yScale;
        }

        private float getY(BiomeGenBase biome, int i, int j, int k) {
            float y = getY(j);
            if (yVariance != 0.0f) {
                y += yVariance * noiseMinus1to1(k, -j, i, ~biome.biomeID);
            }
            return y;
        }
    }
}
