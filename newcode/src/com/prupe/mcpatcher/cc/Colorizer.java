package com.prupe.mcpatcher.cc;

import com.prupe.mcpatcher.*;
import net.minecraft.src.Potion;
import net.minecraft.src.ResourceLocation;

import java.util.Properties;

public class Colorizer {
    private static final MCLogger logger = MCLogger.getLogger(MCPatcherUtils.CUSTOM_COLORS);

    static final ResourceLocation COLOR_PROPERTIES = TexturePackAPI.newMCPatcherResourceLocation("color.properties");
    private static Properties properties;

    static final boolean useWaterColors = Config.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "water", true);
    static final boolean useSwampColors = Config.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "swamp", true);
    static final boolean useTreeColors = Config.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "tree", true);
    static final boolean usePotionColors = Config.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "potion", true);
    static final boolean useParticleColors = Config.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "particle", true);
    static final boolean useFogColors = Config.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "fog", true);
    static final boolean useCloudType = Config.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "clouds", true);
    static final boolean useRedstoneColors = Config.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "redstone", true);
    static final boolean useStemColors = Config.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "stem", true);
    static final boolean useMapColors = Config.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "map", true);
    static final boolean useDyeColors = Config.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "dye", true);
    static final boolean useBlockColors = Config.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "otherBlocks", true);
    static final boolean useTextColors = Config.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "text", true);
    static final boolean useXPOrbColors = Config.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "xporb", true);
    static final boolean useEggColors = Config.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "egg", true);

    public static final int COLOR_MAP_SWAMP_GRASS = 0;
    public static final int COLOR_MAP_SWAMP_FOLIAGE = 1;
    public static final int COLOR_MAP_PINE = 2;
    public static final int COLOR_MAP_BIRCH = 3;
    public static final int COLOR_MAP_FOLIAGE = 4;
    public static final int COLOR_MAP_WATER = 5;
    public static final int COLOR_MAP_UNDERWATER = 6;
    public static final int COLOR_MAP_FOG0 = 7;
    public static final int COLOR_MAP_SKY0 = 8;
    public static final int NUM_FIXED_COLOR_MAPS = 9;

    static final ColorMap[] fixedColorMaps = new ColorMap[NUM_FIXED_COLOR_MAPS]; // bitmaps from FIXED_COLOR_MAPS

    public static final float[] setColor = new float[3];

    static {
        try {
            reset();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        TexturePackChangeHandler.register(new TexturePackChangeHandler(MCPatcherUtils.CUSTOM_COLORS, 2) {
            @Override
            public void beforeChange() {
                reset();
            }

            @Override
            public void afterChange() {
                reloadColorProperties();
                ColorizeBlock.reloadColorMaps(properties);
                if (useFogColors) {
                    ColorizeWorld.reloadFogColors(properties);
                }
                if (usePotionColors) {
                    ColorizeItem.reloadPotionColors(properties);
                }
                if (useSwampColors) {
                    ColorizeBlock.reloadSwampColors(properties);
                }
                if (useBlockColors) {
                    ColorizeBlock.reloadBlockColors(properties);
                }
                if (useParticleColors) {
                    ColorizeEntity.reloadParticleColors(properties);
                }
                if (useRedstoneColors) {
                    ColorizeBlock.reloadRedstoneColors(properties);
                }
                if (useStemColors) {
                    ColorizeBlock.reloadStemColors(properties);
                }
                if (useCloudType) {
                    ColorizeWorld.reloadCloudType(properties);
                }
                if (useMapColors) {
                    ColorizeItem.reloadMapColors(properties);
                }
                if (useDyeColors) {
                    ColorizeEntity.reloadDyeColors(properties);
                }
                if (useTextColors) {
                    ColorizeWorld.reloadTextColors(properties);
                }
                if (useXPOrbColors) {
                    ColorizeEntity.reloadXPOrbColors(properties);
                }
            }
        });
    }

    public static void setColorF(int color) {
        intToFloat3(color, setColor);
    }

    static void init() {
    }

    private static void reset() {
        properties = new Properties();

        ColorizeBlock.reset();
        Lightmap.reset();
        ColorizeItem.reset();
        ColorizeWorld.reset();
        ColorizeEntity.reset();
    }

    private static void reloadColorProperties() {
        if (TexturePackAPI.getProperties(COLOR_PROPERTIES, properties)) {
            logger.finer("reloading %s", COLOR_PROPERTIES);
        }
    }

    static String getStringKey(String[] keys, int index) {
        if (keys != null && index >= 0 && index < keys.length && keys[index] != null) {
            return keys[index];
        } else {
            return "" + index;
        }
    }

    static void loadIntColor(String key, Potion potion) {
        potion.color = loadIntColor(key, potion.color);
    }

    static boolean loadIntColor(String key, int[] color, int index) {
        logger.config("%s=%06x", key, color[index]);
        String value = properties.getProperty(key, "");
        if (!value.equals("")) {
            try {
                color[index] = Integer.parseInt(value, 16);
                return true;
            } catch (NumberFormatException e) {
            }
        }
        return false;
    }

    static int loadIntColor(String key, int color) {
        logger.config("%s=%06x", key, color);
        return MCPatcherUtils.getHexProperty(properties, key, color);
    }

    static void loadFloatColor(String key, float[] color) {
        int intColor = float3ToInt(color);
        intToFloat3(loadIntColor(key, intColor), color);
    }

    static void intToFloat3(int rgb, float[] f, int offset) {
        if ((rgb & 0xffffff) == 0xffffff) {
            f[offset] = f[offset + 1] = f[offset + 2] = 1.0f;
        } else {
            f[offset] = (float) (rgb & 0xff0000) / (float) 0xff0000;
            f[offset + 1] = (float) (rgb & 0xff00) / (float) 0xff00;
            f[offset + 2] = (float) (rgb & 0xff) / (float) 0xff;
        }
    }

    static void intToFloat3(int rgb, float[] f) {
        intToFloat3(rgb, f, 0);
    }

    static int float3ToInt(float[] f, int offset) {
        return ((int) (255.0f * f[offset])) << 16 | ((int) (255.0f * f[offset + 1])) << 8 | (int) (255.0f * f[offset + 2]);
    }

    static int float3ToInt(float[] f) {
        return float3ToInt(f, 0);
    }

    static float clamp(float f) {
        if (f < 0.0f) {
            return 0.0f;
        } else if (f > 1.0f) {
            return 1.0f;
        } else {
            return f;
        }
    }

    static double clamp(double d) {
        if (d < 0.0) {
            return 0.0;
        } else if (d > 1.0) {
            return 1.0;
        } else {
            return d;
        }
    }

    static void clamp(float[] f) {
        for (int i = 0; i < f.length; i++) {
            f[i] = clamp(f[i]);
        }
    }
}
