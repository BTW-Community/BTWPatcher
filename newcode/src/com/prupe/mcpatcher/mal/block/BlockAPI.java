package com.prupe.mcpatcher.mal.block;

import com.prupe.mcpatcher.MAL;
import com.prupe.mcpatcher.MCPatcherUtils;
import net.minecraft.src.Block;
import net.minecraft.src.IBlockAccess;

import java.util.*;

abstract public class BlockAPI {
    public static final int MAX_METADATA = 15;
    public static final int NO_METADATA = MAX_METADATA + 1;
    public static final int METADATA_ARRAY_SIZE = NO_METADATA + 1;

    private static final BlockAPI instance = MAL.newInstance(BlockAPI.class, "block");

    public static Block getFixedBlock(String name) {
        Set<String> names = new HashSet<String>();
        names.add(name);
        instance.getPossibleBlockNames_Impl(name, names);
        for (Block block : getAllBlocks()) {
            if (instance.matchBlock_Impl(block, names)) {
                return block;
            }
        }
        throw new IllegalArgumentException("unknown block " + name);
    }

    public static Block parseBlockName(String name) {
        if (name.matches("\\d+")) {
            int id = Integer.parseInt(name);
            return instance.getBlockById_Impl(id);
        }
        Set<String> names = new HashSet<String>();
        names.add(name);
        instance.getPossibleBlockNames_Impl(name, names);
        for (Block block : getAllBlocks()) {
            if (instance.matchBlock_Impl(block, names)) {
                return block;
            }
        }
        return null;
    }

    public static Block parseBlockAndMetadata(String name, int[] metadata) {
        metadata[0] = (1 << NO_METADATA);
        if (name.matches(".*:[-0-9, ]+")) {
            metadata[0] = 0;
            int pos = name.lastIndexOf(':');
            for (int meta : MCPatcherUtils.parseIntegerList(name.substring(pos + 1), 0, MAX_METADATA)) {
                metadata[0] |= (1 << meta);
            }
            name = name.substring(0, pos);
        }
        return parseBlockName(name);
    }

    public static String getBlockName(Block block) {
        return block == null ? "(null)" : block.getShortName().replaceFirst("^tile\\.", "");
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

    // used by custom colors ItemRenderer patch in 1.6 only
    public static Block getBlockById(int id) {
        return instance.getBlockById_Impl(id);
    }

    abstract protected Block getBlockAt_Impl(IBlockAccess blockAccess, int i, int j, int k);

    abstract protected List<Block> getAllBlocks_Impl();

    protected void getPossibleBlockNames_Impl(String name, Set<String> aliases) {
    }

    protected boolean matchBlock_Impl(Block block, Set<String> names) {
        return names.contains(getBlockName(block));
    }

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
        protected void getPossibleBlockNames_Impl(String name, Set<String> aliases) {
            if (name.startsWith("minecraft:")) {
                aliases.add(name.substring(10));
            }
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
        protected boolean matchBlock_Impl(Block block, Set<String> names) {
            for (String name : names) {
                if (block == Block.blockRegistry.get(name)) {
                    return true;
                }
            }
            return super.matchBlock_Impl(block, names);
        }

        @Override
        protected Block getBlockById_Impl(int id) {
            return Block.blockRegistry.getById(id);
        }
    }
}
