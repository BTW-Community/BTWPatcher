package com.prupe.mcpatcher.mal.biome;

import com.prupe.mcpatcher.MAL;
import com.prupe.mcpatcher.MCLogger;
import com.prupe.mcpatcher.MCPatcherUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.src.BiomeGenBase;

import java.lang.reflect.Method;
import java.util.BitSet;

abstract public class BiomeAPI {
    private static final MCLogger logger = MCLogger.getLogger(MCPatcherUtils.CUSTOM_COLORS);
    private static final BiomeAPI instance = MAL.newInstance(BiomeAPI.class, "biome");

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

    public static void parseBiomeList(String list, BitSet bits) {
        if (MCPatcherUtils.isNullOrEmpty(list)) {
            return;
        }
        for (String s : list.toLowerCase().split("\\s+")) {
            if (s.isEmpty()) {
                continue;
            }
            for (BiomeGenBase biome : BiomeGenBase.biomeList) {
                if (biome != null && biome.biomeName != null &&
                    s.equals(biome.biomeName.toLowerCase().replace(" ", ""))) {
                    bits.set(biome.biomeID);
                }
            }
        }
    }

    public static int getBiomeIDAt(int i, int j, int k) {
        BiomeGenBase biome = getBiomeGenAt(i, j, k);
        return biome == null ? BiomeGenBase.biomeList.length : biome.biomeID;
    }

    public static BiomeGenBase getBiomeGenAt(int i, int j, int k) {
        if (lastBiome == null || i != lastI || k != lastK) {
            lastI = i;
            lastK = k;
            lastBiome = Minecraft.getInstance().theWorld.getBiomeGenAt(i, k);
        }
        return lastBiome;
    }

    public static float getTemperature(int i, int j, int k) {
        return instance.getTemperaturef_Impl(getBiomeGenAt(i, j, k), i, j, k);
    }

    public static float getRainfall(int i, int j, int k) {
        return getBiomeGenAt(i, j, k).getRainfallf();
    }

    public static int getWaterColorMultiplier(int i, int j, int k) {
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

    public static void setupBiome(BiomeGenBase biome) {
        int x = (int) (256.0f * (1.0f - biome.temperature));
        int y = (int) (256.0f * (1.0f - biome.temperature * biome.rainfall));
        logger.finer("setupBiome #%d \"%s\" %06x (%d,%d)", biome.biomeID, biome.biomeName, biome.waterColorMultiplier, x, y);
    }

    abstract protected float getTemperaturef_Impl(BiomeGenBase biome, int i, int j, int k);

    BiomeAPI() {
    }

    final private static class V1 extends BiomeAPI {
        @Override
        protected float getTemperaturef_Impl(BiomeGenBase biome, int i, int j, int k) {
            return biome.getTemperaturef();
        }
    }

    final private static class V2 extends BiomeAPI {
        @Override
        protected float getTemperaturef_Impl(BiomeGenBase biome, int i, int j, int k) {
            return biome.getTemperaturef(i, j, k);
        }
    }
}
