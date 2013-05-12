package com.prupe.mcpatcher.cc;

import com.prupe.mcpatcher.Config;
import com.prupe.mcpatcher.MCLogger;
import com.prupe.mcpatcher.MCPatcherUtils;
import net.minecraft.src.BiomeGenBase;
import net.minecraft.src.Entity;
import net.minecraft.src.World;

import java.util.*;

import static com.prupe.mcpatcher.cc.Colorizer.*;

public class ColorizeWorld {
    private static final MCLogger logger = MCLogger.getLogger(MCPatcherUtils.CUSTOM_COLORS);

    private static final int fogBlendRadius = Config.getInt(MCPatcherUtils.CUSTOM_COLORS, "fogBlendRadius", 7);
    private static final float fogBlendScale = (float) Math.pow(2 * fogBlendRadius + 1, -2);

    private static final String TEXT_KEY = "text.";
    private static final String TEXT_CODE_KEY = TEXT_KEY + "code.";

    private static final int CLOUDS_DEFAULT = 0;
    private static final int CLOUDS_FAST = 1;
    private static final int CLOUDS_FANCY = 2;
    private static int cloudType = CLOUDS_DEFAULT;

    private static final List<BiomeGenBase> biomes = new ArrayList<BiomeGenBase>();
    private static boolean biomesLogged;

    private static Entity fogCamera;

    private static final Map<Integer, Integer> textColorMap = new HashMap<Integer, Integer>(); // text.*
    private static final int[] textCodeColors = new int[32]; // text.code.*
    private static final boolean[] textCodeColorSet = new boolean[32];
    private static int signTextColor; // text.sign

    public static float[] netherFogColor;
    public static float[] endFogColor;
    public static int endSkyColor;

    static {
        try {
            reset();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    static void reset() {
        netherFogColor = new float[]{0.2f, 0.03f, 0.03f};
        endFogColor = new float[]{0.075f, 0.075f, 0.094f};
        endSkyColor = 0x181818;

        cloudType = CLOUDS_DEFAULT;

        textColorMap.clear();
        for (int i = 0; i < textCodeColorSet.length; i++) {
            textCodeColorSet[i] = false;
        }
        signTextColor = 0;
    }

    static void reloadFogColors(Properties properties) {
        loadFloatColor("fog.nether", netherFogColor);
        loadFloatColor("fog.end", endFogColor);
        endSkyColor = loadIntColor("sky.end", endSkyColor);
    }

    static void reloadCloudType(Properties properties) {
        String value = properties.getProperty("clouds", "").trim().toLowerCase();
        if (value.equals("fast")) {
            cloudType = CLOUDS_FAST;
        } else if (value.equals("fancy")) {
            cloudType = CLOUDS_FANCY;
        }
    }

    static void reloadTextColors(Properties properties) {
        for (int i = 0; i < textCodeColors.length; i++) {
            textCodeColorSet[i] = loadIntColor(TEXT_CODE_KEY + i, textCodeColors, i);
            if (textCodeColorSet[i] && i + 16 < textCodeColors.length) {
                textCodeColors[i + 16] = (textCodeColors[i] & 0xfcfcfc) >> 2;
                textCodeColorSet[i + 16] = true;
            }
        }
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            if (!(entry.getKey() instanceof String) || !(entry.getValue() instanceof String)) {
                continue;
            }
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            if (!key.startsWith(TEXT_KEY) || key.startsWith(TEXT_CODE_KEY)) {
                continue;
            }
            key = key.substring(TEXT_KEY.length()).trim();
            try {
                int newColor;
                int oldColor;
                if (key.equals("xpbar")) {
                    oldColor = 0x80ff20;
                } else if (key.equals("boss")) {
                    oldColor = 0xff00ff;
                } else {
                    oldColor = Integer.parseInt(key, 16);
                }
                newColor = Integer.parseInt(value, 16);
                textColorMap.put(oldColor, newColor);
            } catch (NumberFormatException e) {
            }
        }
        signTextColor = loadIntColor("text.sign", 0);
    }

    public static void setupBiome(BiomeGenBase biome) {
        biomes.add(biome);
    }

    public static void setupForFog(Entity entity) {
        fogCamera = entity;
        if (!biomesLogged) {
            biomesLogged = true;
            for (BiomeGenBase biome : biomes) {
                int x = ColorMap.getX(biome.temperature, biome.rainfall);
                int y = ColorMap.getY(biome.temperature, biome.rainfall);
                logger.finer("setupBiome #%d \"%s\" %06x (%d,%d)", biome.biomeID, biome.biomeName, biome.waterColorMultiplier, x, y);
            }
        }
    }

    public static boolean computeFogColor(int index) {
        if (index < 0 || index >= fixedColorMaps.length || fogCamera == null || !fixedColorMaps[index].isCustom()) {
            return false;
        }
        float[] f = new float[3];
        int x = (int) fogCamera.posX;
        int y = (int) fogCamera.posY;
        int z = (int) fogCamera.posZ;
        setColor[0] = 0.0f;
        setColor[1] = 0.0f;
        setColor[2] = 0.0f;
        for (int i = -fogBlendRadius; i <= fogBlendRadius; i++) {
            for (int j = -fogBlendRadius; j <= fogBlendRadius; j++) {
                int rgb = ColorizeBlock.colorizeBiome(0xffffff, index, x + i, y, z + j);
                intToFloat3(rgb, f);
                setColor[0] += f[0];
                setColor[1] += f[1];
                setColor[2] += f[2];
            }
        }
        setColor[0] *= fogBlendScale;
        setColor[1] *= fogBlendScale;
        setColor[2] *= fogBlendScale;
        return true;
    }

    public static boolean computeFogColor(World world, float f) {
        if (world.worldProvider.worldType == 0 && computeFogColor(COLOR_MAP_FOG0)) {
            computeLightningFlash(world, f);
            return true;
        } else {
            return false;
        }
    }

    public static boolean computeSkyColor(World world, float f) {
        if (world.worldProvider.worldType == 0 && computeFogColor(COLOR_MAP_SKY0)) {
            computeLightningFlash(world, f);
            return true;
        } else {
            return false;
        }
    }

    private static void computeLightningFlash(World world, float f) {
        if (world.lightningFlash > 0) {
            f = 0.45f * clamp(world.lightningFlash - f);
            setColor[0] = setColor[0] * (1.0f - f) + 0.8f * f;
            setColor[1] = setColor[1] * (1.0f - f) + 0.8f * f;
            setColor[2] = setColor[2] * (1.0f - f) + 0.8f * f;
        }
    }

    public static boolean drawFancyClouds(boolean fancyGraphics) {
        switch (cloudType) {
            case CLOUDS_FAST:
                return false;

            case CLOUDS_FANCY:
                return true;

            default:
                return fancyGraphics;
        }
    }

    public static int colorizeText(int defaultColor) {
        int high = defaultColor & 0xff000000;
        defaultColor &= 0xffffff;
        Integer newColor = textColorMap.get(defaultColor);
        if (newColor == null) {
            return high | defaultColor;
        } else {
            return high | newColor;
        }
    }

    public static int colorizeText(int defaultColor, int index) {
        if (index < 0 || index >= textCodeColors.length || !textCodeColorSet[index]) {
            return defaultColor;
        } else {
            return (defaultColor & 0xff000000) | textCodeColors[index];
        }
    }

    public static int colorizeSignText() {
        return signTextColor;
    }
}
