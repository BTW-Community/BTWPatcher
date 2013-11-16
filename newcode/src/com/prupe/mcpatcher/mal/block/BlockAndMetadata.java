package com.prupe.mcpatcher.mal.block;

import com.prupe.mcpatcher.MCPatcherUtils;
import net.minecraft.src.Block;

import java.util.ArrayList;
import java.util.List;

final public class BlockAndMetadata {
    public static final int MAX_METADATA = 15;

    private static final int MAX_METADATA_MASK = (1 << (MAX_METADATA + 1)) - 1;
    private static final int NO_METADATA = -1;

    private final Block block;
    private final int metadataList;

    public static BlockAndMetadata parse(String name, String defaultMetadata) {
        int metadataList = NO_METADATA;
        String metadataString = defaultMetadata;
        if (metadataString == null) {
            metadataString = "";
        }
        if (name.matches(".*:[-0-9, ]+")) {
            int pos = name.lastIndexOf(':');
            metadataString = name.substring(pos + 1);
            name = name.substring(0, pos);
        } else if (metadataString.startsWith(":")) {
            metadataString = metadataString.substring(1);
        }
        if (!MCPatcherUtils.isNullOrEmpty(metadataString)) {
            metadataList = 0;
            for (int meta : MCPatcherUtils.parseIntegerList(metadataString, 0, MAX_METADATA)) {
                metadataList |= (1 << meta);
            }
        }
        Block block = BlockAPI.parseBlockName(name);
        return block == null ? null : new BlockAndMetadata(block, metadataList);
    }

    private BlockAndMetadata(Block block, int metadataList) {
        this.block = block;
        this.metadataList = metadataList;
    }

    private BlockAndMetadata(Block block) {
        this(block, NO_METADATA);
    }

    public boolean match(Block block, int metadata) {
        return block == this.block && match(metadata);
    }

    public boolean match(int metadata) {
        return (metadataList & (1 << metadata)) != 0;
    }

    public Block getBlock() {
        return block;
    }

    public int getMetadataBits() {
        return metadataList & MAX_METADATA_MASK;
    }

    public boolean hasMetadata() {
        return metadataList != NO_METADATA;
    }

    public int[] getMetadataList() {
        List<Integer> list = new ArrayList<Integer>();
        for (int i = 0; i <= MAX_METADATA; i++) {
            if (match(i)) {
                list.add(i);
            }
        }
        int[] array = new int[list.size()];
        for (int i = 0; i < array.length; i++) {
            array[i] = list.get(i);
        }
        return array;
    }

    public BlockAndMetadata combine(Integer newMask) {
        if (!hasMetadata()) {
            return this;
        } else if (newMask == null) {
            return new BlockAndMetadata(getBlock());
        } else {
            return new BlockAndMetadata(getBlock(), metadataList | newMask);
        }
    }

    public BlockAndMetadata combine(BlockAndMetadata that) {
        if (that == null) {
            return this;
        } else if (getBlock() != that.getBlock()) {
            throw new IllegalArgumentException("cannot combine " + BlockAPI.getBlockName(getBlock()) + " and " + BlockAPI.getBlockName(that.getBlock()));
        } else {
            return combine(that.metadataList);
        }
    }

    @Override
    public String toString() {
        if (hasMetadata()) {
            StringBuilder sb = new StringBuilder(BlockAPI.getBlockName(block));
            boolean first = true;
            for (int i : getMetadataList()) {
                sb.append(first ? ':' : ',');
                sb.append(i);
                first = false;
            }
            return sb.toString();
        } else {
            return BlockAPI.getBlockName(block);
        }
    }

    @Override
    public int hashCode() {
        return block.hashCode() ^ metadataList;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof BlockAndMetadata)) {
            return false;
        }
        BlockAndMetadata that = (BlockAndMetadata) o;
        return this.block == that.block && this.metadataList == that.metadataList;
    }
}
