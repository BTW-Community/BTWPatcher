package com.prupe.mcpatcher.cc;

import com.prupe.mcpatcher.MCLogger;
import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.TexturePackAPI;
import net.minecraft.src.ResourceLocation;

import java.util.Arrays;

import static com.prupe.mcpatcher.cc.Colorizer.intToFloat3;

final class ColorMap {
    private static final MCLogger logger = MCLogger.getLogger(MCPatcherUtils.CUSTOM_COLORS);

    private static final int COLORMAP_SIZE = 256;
    private static final float COLORMAP_SCALE = COLORMAP_SIZE - 1;

    private int[] map;
    private int mapDefault;

    private int lastBlendI = Integer.MIN_VALUE;
    private int lastBlendK = Integer.MAX_VALUE;
    private final float[] lastBlendResult = new float[3];

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
        return colorize(defaultColor, BiomeHelper.getTemperature(i, j, k), BiomeHelper.getRainfall(i, j, k));
    }

    void colorizeWithBlending(int i, int j, int k, int radius, float[] result) {
        if (i == lastBlendI && k == lastBlendK) {
            System.arraycopy(lastBlendResult, 0, result, 0, 3);
        }
        Arrays.fill(result, 0.0f);
        for (int offsetI = -radius; offsetI <= radius; offsetI++) {
            for (int offsetK = -radius; offsetK <= radius; offsetK++) {
                int rgb = colorize(mapDefault, i + offsetI, j, k + offsetI);
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
        lastBlendK = k;
    }
}
