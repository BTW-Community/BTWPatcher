package net.minecraft.src;

public interface IBlockAccess {
    public BiomeGenBase getBiomeGenAt(int i, int k); // 1.2 and up

    public WorldChunkManager getWorldChunkManager(); // 1.1 and below

    public int getBlockId(int i, int j, int k); // pre-13w36a

    public Block getBlockAt(int i, int j, int k); // 13w36a+

    public int getBlockMetadata(int i, int j, int k);
}
