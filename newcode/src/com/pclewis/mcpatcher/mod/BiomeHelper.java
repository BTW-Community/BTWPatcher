package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.MCLogger;
import com.pclewis.mcpatcher.MCPatcherUtils;
import net.minecraft.src.BiomeGenBase;
import net.minecraft.src.IBlockAccess;
import net.minecraft.src.WorldChunkManager;

import java.lang.reflect.Method;

abstract class BiomeHelper {
    private static final MCLogger logger = MCLogger.getLogger(MCPatcherUtils.CUSTOM_COLORS);

    static BiomeHelper instance;

    IBlockAccess blockAccess;

    static String getBiomeNameAt(int i, int j, int k) {
        if (instance == null) {
            return null;
        } else {
            BiomeGenBase biome = instance.getBiomeGenAt(i, j, k);
            return biome == null ? null : biome.biomeName;
        }
    }

    BiomeHelper(IBlockAccess blockAccess) {
        this.blockAccess = blockAccess;
    }

    boolean useBlockBlending() {
        return false;
    }

    abstract BiomeGenBase getBiomeGenAt(int i, int j, int k);

    abstract float getTemperature(int i, int j, int k);

    abstract float getRainfall(int i, int j, int k);

    abstract int getWaterColorMultiplier(int i, int j, int k);

    static class Stub extends BiomeHelper {
        Stub() {
            super(null);
        }

        @Override
        BiomeGenBase getBiomeGenAt(int i, int j, int k) {
            return null;
        }

        @Override
        float getTemperature(int i, int j, int k) {
            return 0.5f;
        }

        @Override
        float getRainfall(int i, int j, int k) {
            return 1.0f;
        }

        @Override
        int getWaterColorMultiplier(int i, int j, int k) {
            return 0xffffff;
        }
    }

    static class Old extends BiomeHelper {
        WorldChunkManager chunkManager;

        Old(IBlockAccess blockAccess) {
            super(blockAccess);
            chunkManager = blockAccess.getWorldChunkManager();
        }

        @Override
        BiomeGenBase getBiomeGenAt(int i, int j, int k) {
            return chunkManager.getBiomeGenAt(i, k);
        }

        @Override
        float getTemperature(int i, int j, int k) {
            return chunkManager.getTemperature(i, j, k);
        }

        @Override
        float getRainfall(int i, int j, int k) {
            return chunkManager.getRainfall(i, k);
        }

        @Override
        int getWaterColorMultiplier(int i, int j, int k) {
            return getBiomeGenAt(i, j, k).waterColorMultiplier;
        }
    }

    static class New extends BiomeHelper {
        private static boolean logged;

        private Method getWaterColorMultiplier;
        private BiomeGenBase lastBiome;
        private int lastI;
        private int lastK;

        New(IBlockAccess blockAccess) {
            super(blockAccess);
            if (!logged) {
                logged = true;
                logger.config("biomes v1.2 detected");
            }
            try {
                getWaterColorMultiplier = BiomeGenBase.class.getDeclaredMethod("getWaterColorMultiplier");
                getWaterColorMultiplier.setAccessible(true);
                logger.config("forge getWaterColorMultiplier detected");
            } catch (NoSuchMethodException e) {
            }
        }

        @Override
        boolean useBlockBlending() {
            return true;
        }

        @Override
        BiomeGenBase getBiomeGenAt(int i, int j, int k) {
            if (lastBiome == null || i != lastI || k != lastK) {
                lastI = i;
                lastK = k;
                lastBiome = blockAccess.getBiomeGenAt(i, k);
            }
            return lastBiome;
        }

        @Override
        float getTemperature(int i, int j, int k) {
            return getBiomeGenAt(i, j, k).getTemperaturef();
        }

        @Override
        float getRainfall(int i, int j, int k) {
            return getBiomeGenAt(i, j, k).getRainfallf();
        }

        @Override
        int getWaterColorMultiplier(int i, int j, int k) {
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
}
