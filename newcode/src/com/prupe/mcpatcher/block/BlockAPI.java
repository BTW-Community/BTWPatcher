package com.prupe.mcpatcher.block;

import com.prupe.mcpatcher.MAL;
import net.minecraft.src.Block;
import net.minecraft.src.IBlockAccess;

abstract public class BlockAPI {
    private static final BlockAPI instance = MAL.newInstance(BlockAPI.class, "block");

    public static Block getBlockAt(IBlockAccess blockAccess, int i, int j, int k) {
        return instance.getBlockAt_Impl(blockAccess, i, j, k);
    }

    public static int getBlockIdAt(IBlockAccess blockAccess, int i, int j, int k) {
        return instance.getBlockIdAt_Impl(blockAccess, i, j, k);
    }

    public static Block getBlockById(int id) {
        return instance.getBlockById_Impl(id);
    }

    public static Block getBlockByCanonicalId(int id) {
        return instance.getBlockByCanonicalId_Impl(id);
    }

    public static int getBlockId(Block block) {
        return instance.getBlockId_Impl(block);
    }

    public static int getNumBlocks() {
        return instance.getNumBlocks_Impl();
    }

    abstract protected Block getBlockAt_Impl(IBlockAccess blockAccess, int i, int j, int k);
    
    abstract protected int getBlockIdAt_Impl(IBlockAccess blockAccess, int i, int j, int k);

    abstract protected Block getBlockById_Impl(int id);

    abstract protected Block getBlockByCanonicalId_Impl(int id);
    
    abstract protected int getBlockId_Impl(Block block);

    abstract protected int getNumBlocks_Impl();

    BlockAPI() {
    }
    
    final private static class V1 extends BlockAPI {
        @Override
        protected Block getBlockAt_Impl(IBlockAccess blockAccess, int i, int j, int k) {
            return Block.blocksList[blockAccess.getBlockId(i, j, k)];
        }

        @Override
        protected int getBlockIdAt_Impl(IBlockAccess blockAccess, int i, int j, int k) {
            return blockAccess.getBlockId(i, j, k); 
        }

        @Override
        protected Block getBlockById_Impl(int id) {
            return Block.blocksList[id];
        }

        @Override
        protected Block getBlockByCanonicalId_Impl(int id) {
            return Block.blocksList[id];
        }

        @Override
        protected int getBlockId_Impl(Block block) {
            return block.blockID;
        }

        @Override
        protected int getNumBlocks_Impl() {
            return Block.blocksList.length;
        }
    }

    final private static class V2 extends BlockAPI {
        @Override
        protected Block getBlockAt_Impl(IBlockAccess blockAccess, int i, int j, int k) {
            return blockAccess.getBlockAt(i, j, k);
        }

        @Override
        protected int getBlockIdAt_Impl(IBlockAccess blockAccess, int i, int j, int k) {
            return getBlockId_Impl(getBlockAt_Impl(blockAccess, i, j, k));
        }

        @Override
        protected Block getBlockById_Impl(int id) {
            return Block.blockRegistry.getById(id);
        }

        @Override
        protected Block getBlockByCanonicalId_Impl(int id) {
            return Block.blockRegistry.getById(id);
        }

        @Override
        protected int getBlockId_Impl(Block block) {
            return Block.blockRegistry.getId(block);
        }

        @Override
        protected int getNumBlocks_Impl() {
            return 4096;
        }
    }
}
