package com.prupe.mcpatcher.cc;

import com.prupe.mcpatcher.MCLogger;
import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.TexturePackAPI;
import net.minecraft.src.ResourceAddress;

final class ColorMap {
    private static final MCLogger logger = MCLogger.getLogger(MCPatcherUtils.CUSTOM_COLORS);

    private static final int COLORMAP_SIZE = 256;
    private static final float COLORMAP_SCALE = COLORMAP_SIZE - 1;

    private int[] map;
    private int mapDefault;

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

    void loadColorMap(boolean useCustom, ResourceAddress resource) {
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
}
