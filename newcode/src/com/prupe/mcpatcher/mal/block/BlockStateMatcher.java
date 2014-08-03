package com.prupe.mcpatcher.mal.block;

import com.prupe.mcpatcher.MCLogger;
import com.prupe.mcpatcher.MCPatcherUtils;
import net.minecraft.src.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

abstract public class BlockStateMatcher {
    protected final Block block;

    protected BlockStateMatcher(MCLogger logger, ResourceLocation source, Block block, String metadataList, Map<String, String> properties) {
        this.block = block;
    }

    final public Block getBlock() {
        return block;
    }

    abstract public boolean match(IBlockAccess blockAccess, int i, int j, int k);

    abstract public boolean match(Block block, int metadata);

    abstract public boolean isUnfiltered();

    final static class V1 extends BlockStateMatcher {
        private static final int MAX_METADATA = 15;
        private static final int NO_METADATA = -1;

        private final int metadataBits;

        V1(MCLogger logger, ResourceLocation source, Block block, String metadataList, Map<String, String> properties) {
            super(logger, source, block, metadataList, properties);
            if (MCPatcherUtils.isNullOrEmpty(metadataList)) {
                metadataBits = NO_METADATA;
            } else {
                int bits = 0;
                for (int i : MCPatcherUtils.parseIntegerList(metadataList, 0, MAX_METADATA)) {
                    bits |= 1 << i;
                }
                metadataBits = bits;
            }
        }

        @Override
        public boolean match(IBlockAccess blockAccess, int i, int j, int k) {
            return match(blockAccess.getBlock(i, j, k), blockAccess.getBlockMetadata(i, j, k));
        }

        @Override
        public boolean match(Block block, int metadata) {
            return block == this.block && (metadataBits & (1 << metadata)) != 0;
        }

        @Override
        public boolean isUnfiltered() {
            return metadataBits == NO_METADATA;
        }
    }

    final static class V2 extends BlockStateMatcher {
        private final Map<IBlockStateProperty, Set<Comparable>> propertyMap = new HashMap<IBlockStateProperty, Set<Comparable>>();

        V2(MCLogger logger, ResourceLocation source, Block block, String metadataList, Map<String, String> properties) {
            super(logger, source, block, metadataList, properties);
            IBlockState state = block.getBlockState();
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                String name = entry.getKey();
                boolean foundProperty = false;
                for (IBlockStateProperty property : state.getProperties()) {
                    if (name.equals(property.getName())) {
                        foundProperty = true;
                        Set<Comparable> valueSet = propertyMap.get(property);
                        if (valueSet == null) {
                            valueSet = new HashSet<Comparable>();
                            propertyMap.put(property, valueSet);
                        }
                        for (String s : entry.getValue().split("\\s*,\\s*")) {
                            if (s.equals("")) {
                                continue;
                            }
                            boolean foundValue = false;
                            for (Comparable propertyValue : property.getValues()) {
                                if (s.equals(propertyValue.toString())) {
                                    foundValue = true;
                                    valueSet.add(propertyValue);
                                    break;
                                }
                            }
                            if (!foundValue) {
                                logger.warning("%s: unknown value %s for block %s property %s",
                                    source, s, BlockAPI.getBlockName(block), property.getName()
                                );
                            }
                        }
                    }
                }
                if (!foundProperty) {
                    logger.warning("%s: unknown property %s for block %s", source, name, BlockAPI.getBlockName(block));
                }
            }
        }

        @Override
        public boolean match(IBlockAccess blockAccess, int i, int j, int k) {
            IBlockState state = blockAccess.getBlockState(new Position(i, j, k));
            if (state == null || state.getBlock() != block) {
                return false;
            }
            for (Map.Entry<IBlockStateProperty, Set<Comparable>> entry : propertyMap.entrySet()) {
                IBlockStateProperty property = entry.getKey();
                Set<Comparable> values = entry.getValue();
                if (!values.contains(state.getProperty(property))) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean match(Block block, int metadata) {
            throw new UnsupportedOperationException("match by metadata");
        }

        @Override
        public boolean isUnfiltered() {
            return propertyMap.isEmpty();
        }
    }
}
