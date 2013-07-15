package com.prupe.mcpatcher.cc;

import com.prupe.mcpatcher.MCLogger;
import com.prupe.mcpatcher.MCPatcherUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.src.BiomeGenBase;

import java.lang.reflect.Method;

class BiomeHelper {
    private static final MCLogger logger = MCLogger.getLogger(MCPatcherUtils.CUSTOM_COLORS);

    private static Method getWaterColorMultiplier;
    private static BiomeGenBase lastBiome;
    private static int lastI;
    private static int lastK;

    static {
        try {
            getWaterColorMultiplier = BiomeGenBase.class.getDeclaredMethod("getWaterColorMultiplier");
            getWaterColorMultiplier.setAccessible(true);
            logger.config("forge getWaterColorMultiplier detected");
        } catch (NoSuchMethodException e) {
        }
    }

    static String getBiomeNameAt(int i, int j, int k) {
        BiomeGenBase biome = getBiomeGenAt(i, j, k);
        if (biome == null) {
            return "";
        }
        String biomeName = biome.biomeName;
        if (biomeName == null) {
            return "";
        }
        return biomeName.toLowerCase().replace(" ", "");
    }

    static BiomeGenBase getBiomeGenAt(int i, int j, int k) {
        if (lastBiome == null || i != lastI || k != lastK) {
            lastI = i;
            lastK = k;
            lastBiome = Minecraft.getInstance().theWorld.getBiomeGenAt(i, k);
        }
        return lastBiome;
    }

    static float getTemperature(int i, int j, int k) {
        return getBiomeGenAt(i, j, k).getTemperaturef();
    }

    static float getRainfall(int i, int j, int k) {
        return getBiomeGenAt(i, j, k).getRainfallf();
    }

    static int getWaterColorMultiplier(int i, int j, int k) {
        BiomeGenBase biome = getBiomeGenAt(i, j, k);
        if (getWaterColorMultiplier != null) {
            try {
                return (Integer) getWaterColorMultiplier.invoke(biome);
            } catch (Throwable e) {
                e.printStackTrace();
                getWaterColorMultiplier = null;
            }
        }
        return biome.waterColorMultiplier;
    }
}
