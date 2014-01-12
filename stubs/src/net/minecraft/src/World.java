package net.minecraft.src;

import java.util.List;

public class World implements IBlockAccess {
    public WorldInfo worldInfo;
    public List loadedEntityList;
    public WorldProvider worldProvider;
    public int lightningFlash;

    public float getSunAngle(float f) {
        return 1.0f;
    }

    public float getCelestialAngle(float f) {
        return 1.0f;
    }

    public float getRainStrength(float f) {
        return 0.0f;
    }

    public long getWorldTime() {
        return 0L;
    }

    public int getBlockId(int x, int y, int z) {
        return 0;
    }

    @Override
    public Block getBlock(int i, int j, int k) {
        return null;
    }

    public int getBlockLightValue(int x, int y, int z) {
        return 0;
    }

    public void markBlockNeedsUpdate(int x, int y, int z) {
    }

    public void playSoundAtEntity(Entity var1, String var2, float var3, float var4) {
    }

    public boolean addWeatherEffect(Entity var1) {
        return true;
    }

    public List getEntitiesWithinAABBExcludingEntity(Entity var1, AxisAlignedBB var2) {
        return null;
    }

    public BiomeGenBase getBiomeGenAt(int i, int k) {
        return null;
    }

    public WorldChunkManager getWorldChunkManager() {
        return null;
    }

    public int getBlockMetadata(int i, int j, int k) {
        return 0;
    }

    // 14w02a+
    @Override
    public BiomeGenBase getBiomeGenAt(Position position) {
        return null;
    }

    @Override
    public int getBlockId(Position position) {
        return 0;
    }

    @Override
    public Block getBlock(Position position) {
        return null;
    }

    @Override
    public int getBlockMetadata(Position position) {
        return 0;
    }
}
