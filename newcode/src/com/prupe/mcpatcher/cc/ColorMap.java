package com.prupe.mcpatcher.cc;

import com.prupe.mcpatcher.*;
import com.prupe.mcpatcher.mal.biome.BiomeAPI;
import net.minecraft.src.BiomeGenBase;
import net.minecraft.src.ResourceLocation;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

abstract class ColorMap implements IColorMap {
    private static final MCLogger logger = MCLogger.getLogger(MCPatcherUtils.CUSTOM_COLORS);

    private static final int COLORMAP_WIDTH = 256;
    private static final int COLORMAP_HEIGHT = 256;

    static final String CUSTOM_COLORMAP_DIR = TexturePackAPI.MCPATCHER_SUBDIR + "colormap/custom";
    static final List<ResourceLocation> unusedPNGs = new ArrayList<ResourceLocation>();

    private static int defaultColorMapFormat;
    private static boolean defaultFlipY;
    private static float defaultYVariance;

    private final ResourceLocation resource;
    protected final int[] map;
    protected final int width;
    protected final int height;
    protected final float maxX;
    protected final float maxY;

    private final float[] xy = new float[2];
    private final float[] lastColor = new float[3];

    static IColorMap loadVanillaColorMap(ResourceLocation vanillaImage, ResourceLocation swampImage) {
        Properties properties = new Properties();
        properties.setProperty("format", "1");
        properties.setProperty("source", vanillaImage.toString());
        if (swampImage != null) {
            properties.setProperty("altSource", swampImage.toString());
        }
        return loadColorMap(true, vanillaImage, properties);
    }

    static IColorMap loadFixedColorMap(boolean useCustom, ResourceLocation propertiesResource) {
        return loadColorMap(useCustom, propertiesResource, null);
    }

    static IColorMap loadColorMap(boolean useCustom, ResourceLocation resource, Properties properties) {
        IColorMap map = loadColorMap1(useCustom, resource, properties);
        if (map instanceof ColorMap) {
            unusedPNGs.remove(((ColorMap) map).resource);
        }
        return map;
    }

    private static IColorMap loadColorMap1(boolean useCustom, ResourceLocation resource, Properties properties) {
        if (!useCustom || resource == null) {
            return null;
        }

        ResourceLocation propertiesResource;
        ResourceLocation imageResource;
        if (resource.toString().endsWith(".png")) {
            propertiesResource = TexturePackAPI.transformResourceLocation(resource, ".png", ".properties");
            imageResource = resource;
        } else if (resource.toString().endsWith(".properties")) {
            propertiesResource = resource;
            imageResource = TexturePackAPI.transformResourceLocation(resource, ".properties", ".png");
        } else {
            return null;
        }
        if (properties == null) {
            properties = TexturePackAPI.getProperties(propertiesResource);
            if (properties == null) {
                properties = new Properties();
            }
        }

        int format = MCPatcherUtils.getIntProperty(properties, "format", defaultColorMapFormat);
        if (format == 0) {
            int color = MCPatcherUtils.getHexProperty(properties, "color", 0xffffff);
            return new Fixed(color);
        }

        String path = MCPatcherUtils.getStringProperty(properties, "source", "");
        if (!MCPatcherUtils.isNullOrEmpty(path)) {
            imageResource = TexturePackAPI.parseResourceLocation(resource, path);
        }
        BufferedImage image = TexturePackAPI.getImage(imageResource);
        if (image == null) {
            return null;
        }

        switch (format) {
            case 1:
                IColorMap defaultMap = new TempHumidity(imageResource, properties, image);
                path = MCPatcherUtils.getStringProperty(properties, "altSource", "");
                if (Colorizer.useSwampColors && !MCPatcherUtils.isNullOrEmpty(path)) {
                    ResourceLocation swampResource = TexturePackAPI.parseResourceLocation(resource, path);
                    image = TexturePackAPI.getImage(swampResource);
                    if (image != null) {
                        IColorMap swampMap = new TempHumidity(swampResource, properties, image);
                        return new Swamp(defaultMap, swampMap);
                    }
                }
                return defaultMap;

            case 2:
                Grid grid = new Grid(imageResource, properties, image);
                if (grid.isInteger()) {
                    return new IntegerGrid(imageResource, properties, grid.map);
                } else {
                    return grid;
                }

            default:
                logger.error("%s: unknown format %d", resource, format);
                return null;
        }
    }

    static void reset() {
        unusedPNGs.clear();
        defaultColorMapFormat = 1;
        defaultFlipY = false;
        defaultYVariance = Config.getInt(MCPatcherUtils.CUSTOM_COLORS, "yVariance", 0);
    }

    static void reloadColorMapSettings(Properties properties) {
        unusedPNGs.addAll(TexturePackAPI.listResources(CUSTOM_COLORMAP_DIR, ".png", true, false, false));
        defaultColorMapFormat = MCPatcherUtils.getIntProperty(properties, "palette.format", 1);
        defaultFlipY = MCPatcherUtils.getBooleanProperty(properties, "palette.flipY", false);
        defaultYVariance = MCPatcherUtils.getFloatProperty(properties, "palette.yVariance", 0.0f);
    }

    ColorMap(ResourceLocation resource, Properties properties, BufferedImage image) {
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
        Colorizer.intToFloat3(rgb, lastColor);
        return lastColor;
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

    protected static int clamp(int i, int min, int max) {
        if (i < min) {
            return min;
        } else if (i > max) {
            return max;
        } else {
            return i;
        }
    }

    static final class Fixed implements IColorMap {
        private final int colorI;
        private final float[] colorF = new float[3];

        Fixed(int color) {
            colorI = color;
            Colorizer.intToFloat3(colorI, colorF);
        }

        @Override
        public String toString() {
            return String.format("Fixed{%06x}", colorI);
        }

        @Override
        public boolean isHeightDependent() {
            return false;
        }

        @Override
        public int getColorMultiplier() {
            return colorI;
        }

        @Override
        public int getColorMultiplier(int i, int j, int k) {
            return colorI;
        }

        @Override
        public float[] getColorMultiplierF(int i, int j, int k) {
            return colorF;
        }
    }

    static final class Water implements IColorMap {
        private final float[] lastColor = new float[3];

        @Override
        public String toString() {
            return String.format("Water{%06x}", getColorMultiplier());
        }

        @Override
        public boolean isHeightDependent() {
            return false;
        }

        @Override
        public int getColorMultiplier() {
            return BiomeAPI.getWaterColorMultiplier(BiomeAPI.findBiomeByName("Ocean"));
        }

        @Override
        public int getColorMultiplier(int i, int j, int k) {
            return BiomeAPI.getWaterColorMultiplier(BiomeAPI.getBiomeGenAt(i, j, k));
        }

        @Override
        public float[] getColorMultiplierF(int i, int j, int k) {
            Colorizer.intToFloat3(getColorMultiplier(i, j, k), lastColor);
            return lastColor;
        }
    }

    static final class Swamp implements IColorMap {
        private final IColorMap defaultMap;
        private final IColorMap swampMap;
        private final BiomeGenBase swampBiome;

        Swamp(IColorMap defaultMap, IColorMap swampMap) {
            this.defaultMap = defaultMap;
            this.swampMap = swampMap;
            swampBiome = BiomeAPI.findBiomeByName("Swampland");
        }

        @Override
        public String toString() {
            return defaultMap.toString();
        }

        @Override
        public boolean isHeightDependent() {
            return defaultMap.isHeightDependent() || swampMap.isHeightDependent();
        }

        @Override
        public int getColorMultiplier() {
            return defaultMap.getColorMultiplier();
        }

        @Override
        public int getColorMultiplier(int i, int j, int k) {
            IColorMap map = BiomeAPI.getBiomeGenAt(i, j, k) == swampBiome ? swampMap : defaultMap;
            return map.getColorMultiplier(i, j, k);
        }

        @Override
        public float[] getColorMultiplierF(int i, int j, int k) {
            IColorMap map = BiomeAPI.getBiomeGenAt(i, j, k) == swampBiome ? swampMap : defaultMap;
            return map.getColorMultiplierF(i, j, k);
        }
    }

    static final class TempHumidity extends ColorMap {
        private final int defaultColor;

        private TempHumidity(ResourceLocation resource, Properties properties, BufferedImage image) {
            super(resource, properties, image);

            defaultColor = MCPatcherUtils.getHexProperty(properties, "color", getRGB(maxX * 0.5f, maxY * 0.5f));
        }

        @Override
        public boolean isHeightDependent() {
            return BiomeAPI.isColorHeightDependent;
        }

        @Override
        public int getColorMultiplier() {
            return defaultColor;
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
        private final float[] biomeX = new float[BiomeGenBase.biomeList.length];
        private final float yVariance;
        private final int defaultColor;

        private Grid(ResourceLocation resource, Properties properties, BufferedImage image) {
            super(resource, properties, image);

            if (MCPatcherUtils.getBooleanProperty(properties, "flipY", defaultFlipY)) {
                int[] temp = new int[width];
                for (int i = 0; i < map.length / 2; i += width) {
                    int j = map.length - width - i;
                    System.arraycopy(map, i, temp, 0, width);
                    System.arraycopy(map, j, map, i, width);
                    System.arraycopy(temp, 0, map, j, width);
                }
            }

            yVariance = Math.max(MCPatcherUtils.getFloatProperty(properties, "yVariance", defaultYVariance), 0.0f);
            for (int i = 0; i < biomeX.length; i++) {
                biomeX[i] = i % width;
            }
            for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                String key = (String) entry.getKey();
                String value = (String) entry.getValue();
                if (key.endsWith(".x") && !MCPatcherUtils.isNullOrEmpty(value)) {
                    key = key.substring(0, key.length() - 2);
                    BiomeGenBase biome = BiomeAPI.findBiomeByName(key);
                    if (biome != null && biome.biomeID >= 0 && biome.biomeID < BiomeGenBase.biomeList.length) {
                        try {
                            biomeX[biome.biomeID] = Float.parseFloat(value);
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            defaultColor = MCPatcherUtils.getHexProperty(properties, "color", getRGB(biomeX[1], getY(ColorMapBase.DEFAULT_HEIGHT)));
        }

        boolean isInteger() {
            if (width != COLORMAP_WIDTH || height != COLORMAP_HEIGHT) {
                return false;
            }
            if (yVariance != 0.0f) {
                return false;
            }
            for (int i = 0; i < biomeX.length; i++) {
                if (biomeX[i] != i) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean isHeightDependent() {
            return true;
        }

        @Override
        public int getColorMultiplier() {
            return defaultColor;
        }

        @Override
        protected void computeXY(BiomeGenBase biome, int i, int j, int k, float[] f) {
            f[0] = getX(biome, i, j, k);
            f[1] = getY(biome, i, j, k);
        }

        private float getX(BiomeGenBase biome, int i, int j, int k) {
            return biomeX[biome.biomeID];
        }

        private float getY(int j) {
            return (float) j;
        }

        private float getY(BiomeGenBase biome, int i, int j, int k) {
            float y = getY(j);
            if (yVariance != 0.0f) {
                y += yVariance * noiseMinus1to1(k, -j, i, ~biome.biomeID);
            }
            return y;
        }
    }

    static final class IntegerGrid implements IColorMap {
        private final ResourceLocation resource;
        private final int[] map;
        private final float[] lastColor = new float[3];
        private final int defaultColor;

        IntegerGrid(ResourceLocation resource, Properties properties, int[] map) {
            this.resource = resource;
            this.map = map;
            defaultColor = MCPatcherUtils.getHexProperty(properties, "color", getRGB(1, ColorMapBase.DEFAULT_HEIGHT));
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{" + resource + "}";
        }

        @Override
        public boolean isHeightDependent() {
            return true;
        }

        @Override
        public int getColorMultiplier() {
            return defaultColor;
        }

        @Override
        public int getColorMultiplier(int i, int j, int k) {
            int x = clamp(BiomeAPI.getBiomeIDAt(i, j, k), 0, COLORMAP_WIDTH - 1);
            int y = clamp(j, 0, COLORMAP_HEIGHT - 1);
            return getRGB(x, y);
        }

        @Override
        public float[] getColorMultiplierF(int i, int j, int k) {
            int rgb = getColorMultiplier(i, j, k);
            Colorizer.intToFloat3(rgb, lastColor);
            return lastColor;
        }

        private int getRGB(int x, int y) {
            return map[y * COLORMAP_WIDTH + x];
        }
    }
}
