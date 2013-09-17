package com.prupe.mcpatcher.mal.block;

import com.prupe.mcpatcher.MAL;
import com.prupe.mcpatcher.MCPatcherUtils;
import net.minecraft.src.Block;
import net.minecraft.src.IBlockAccess;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

abstract public class BlockAPI {
    public static final int MAX_METADATA = 15;
    public static final int NO_METADATA = MAX_METADATA + 1;
    public static final int METADATA_ARRAY_SIZE = NO_METADATA + 1;

    private static final BlockAPI instance = MAL.newInstance(BlockAPI.class, "block");

    public static Block parseBlockName(String name) {
        if (name.matches("\\d+")) {
            int id = Integer.parseInt(name);
            return instance.getBlockById_Impl(id);
        }
        name = instance.getCanonicalName_Impl(name);
        for (Block block : getAllBlocks()) {
            if (name.equals(getBlockName(block))) {
                return block;
            }
        }
        return null;
    }

    public static Block parseBlockAndMetadata(String name, int[] metadata) {
        metadata[0] = NO_METADATA;
        if (name.matches(".*:[-0-9,]+")) {
            metadata[0] = 0;
            int pos = name.lastIndexOf(':');
            for (int meta : MCPatcherUtils.parseIntegerList(name.substring(pos + 1), 0, MAX_METADATA)) {
                metadata[0] |= (1 << meta);
            }
            name = name.substring(0, pos - 1);
        }
        return parseBlockName(name);
    }

    public static String getBlockName(Block block) {
        return block.getShortName();
    }

    public static String getBlockName(Block block, int metadata) {
        StringBuilder sb = new StringBuilder(getBlockName(block));
        if (metadata != NO_METADATA) {
            boolean first = true;
            for (int i = 0; i <= MAX_METADATA; i++) {
                if ((metadata & (1 << i)) != 0) {
                    sb.append(first ? ':' : ',');
                    sb.append(i);
                    first = false;
                }
            }
        }
        return sb.toString();
    }

    public static List<Block> getAllBlocks() {
        List<Block> blocks = new ArrayList<Block>();
        for (Block block : instance.getAllBlocks_Impl()) {
            if (block != null && !blocks.contains(block)) {
                blocks.add(block);
            }
        }
        return blocks;
    }

    public static Block getBlockAt(IBlockAccess blockAccess, int i, int j, int k) {
        return instance.getBlockAt_Impl(blockAccess, i, j, k);
    }

    abstract protected Block getBlockAt_Impl(IBlockAccess blockAccess, int i, int j, int k);

    abstract protected List<Block> getAllBlocks_Impl();

    abstract protected String getCanonicalName_Impl(String name);

    abstract protected Block getBlockById_Impl(int id);

    BlockAPI() {
    }

    final private static class V1 extends BlockAPI {
        @Override
        protected Block getBlockAt_Impl(IBlockAccess blockAccess, int i, int j, int k) {
            return Block.blocksList[blockAccess.getBlockId(i, j, k)];
        }

        @Override
        protected List<Block> getAllBlocks_Impl() {
            return Arrays.asList(Block.blocksList);
        }

        @Override
        protected String getCanonicalName_Impl(String name) {
            if (name.startsWith("minecraft:")) {
                name = name.substring(10);
            }
            return name;
        }

        @Override
        protected Block getBlockById_Impl(int id) {
            return id >= 0 && id < Block.blocksList.length ? Block.blocksList[id] : null;
        }
    }

    final private static class V2 extends BlockAPI {
        @Override
        protected Block getBlockAt_Impl(IBlockAccess blockAccess, int i, int j, int k) {
            return blockAccess.getBlock(i, j, k);
        }

        @Override
        protected List<Block> getAllBlocks_Impl() {
            return Block.blockRegistry.getAll();
        }

        @Override
        protected String getCanonicalName_Impl(String name) {
            if (!name.contains(":")) {
                name = "minecraft:" + name;
            }
            return name;
        }

        @Override
        protected Block getBlockById_Impl(int id) {
            return Block.blockRegistry.getById(id);
        }
    }
}
