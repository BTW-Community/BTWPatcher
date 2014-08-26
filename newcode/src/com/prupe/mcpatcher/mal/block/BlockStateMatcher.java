package com.prupe.mcpatcher.mal.block;

import com.prupe.mcpatcher.MCLogger;
import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.mal.resource.PropertiesFile;
import net.minecraft.src.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

abstract public class BlockStateMatcher {
    private final String fullString;
    private final ThreadLocal<Object> threadLocal = new ThreadLocal<Object>();

    protected final Block block;
    protected Object data;

    protected BlockStateMatcher(PropertiesFile source, String fullString, Block block, String metadataList, Map<String, String> properties) {
        this.fullString = fullString;
        this.block = block;
    }

    final public Block getBlock() {
        return block;
    }

    final public Object getData() {
        return data;
    }

    final public void setData(Object data) {
        this.data = data;
    }

    final public Object getThreadData() {
        return threadLocal.get();
    }

    final public void setThreadData(Object data) {
        threadLocal.set(data);
    }

    @Override
    final public String toString() {
        return fullString;
    }

    abstract public boolean match(IBlockAccess blockAccess, int i, int j, int k);

    abstract public boolean match(Block block, int metadata);

    abstract public boolean matchBlockState(Object blockState);

    abstract public boolean isUnfiltered();

    final static class V1 extends BlockStateMatcher {
        private static final int MAX_METADATA = 15;
        private static final int NO_METADATA = -1;

        private final int metadataBits;

        private static Block doublePlantBlock;

        V1(PropertiesFile source, String fullString, Block block, String metadataList, Map<String, String> properties) {
            super(source, fullString, block, metadataList, properties);
            if (MCPatcherUtils.isNullOrEmpty(metadataList)) {
                metadataBits = NO_METADATA;
            } else {
                int bits = 0;
                for (int i : MCPatcherUtils.parseIntegerList(metadataList, 0, MAX_METADATA)) {
                    bits |= 1 << i;
                }
                metadataBits = bits;
            }
            doublePlantBlock = BlockAPI.parseBlockName("minecraft:double_plant");
        }

        @Override
        public boolean match(IBlockAccess blockAccess, int i, int j, int k) {
            Block block = BlockAPI.getBlockAt(blockAccess, i, j, k);
            if (block != this.block) {
                return false;
            }
            int metadata = BlockAPI.getMetadataAt(blockAccess, i, j, k);
            if (block == doublePlantBlock) {
                if ((metadata & 0x8) != 0 && BlockAPI.getBlockAt(blockAccess, i, j - 1, k) == block) {
                    metadata = BlockAPI.getMetadataAt(blockAccess, i, j - 1, k);
                }
                metadata &= 0x7;
            }
            return (metadataBits & (1 << metadata)) != 0;
        }

        @Override
        public boolean match(Block block, int metadata) {
            return block == this.block && (metadataBits & (1 << metadata)) != 0;
        }

        @Override
        public boolean matchBlockState(Object blockState) {
            throw new UnsupportedOperationException("match by block state");
        }

        @Override
        public boolean isUnfiltered() {
            return metadataBits == NO_METADATA;
        }
    }

    public final static class V2 extends BlockStateMatcher {
        private final Map<IBlockStateProperty, Set<Comparable>> propertyMap = new HashMap<IBlockStateProperty, Set<Comparable>>();

        public static void dumpBlockStates(MCLogger logger) {
            for (Block block : BlockAPI.getAllBlocks()) {
                logger.info("Block %s", BlockAPI.getBlockName(block));
                IBlockState state = block.getBlockState();
                for (IBlockStateProperty property : state.getProperties()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("  ").append(property.getName()).append(" (").append(property.getValueClass().getName()).append("):");
                    for (Comparable value : property.getValues()) {
                        sb.append(' ').append(value.toString());
                    }
                    logger.info("%s", sb.toString());
                }
            }
        }

        V2(PropertiesFile source, String fullString, Block block, String metadataList, Map<String, String> properties) {
            super(source, fullString, block, metadataList, properties);
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
                        if (Integer.class.isAssignableFrom(property.getValueClass())) {
                            parseIntegerValues(property, valueSet, entry.getValue());
                        } else {
                            for (String s : entry.getValue().split("\\s*,\\s*")) {
                                if (s.equals("")) {
                                    continue;
                                }
                                Comparable propertyValue = parseNonIntegerValue(property, s);
                                if (propertyValue == null) {
                                    source.warning("unknown value %s for block %s property %s",
                                        s, BlockAPI.getBlockName(block), property.getName()
                                    );
                                } else {
                                    valueSet.add(propertyValue);
                                }
                            }
                        }
                    }
                }
                if (!foundProperty) {
                    source.warning("unknown property %s for block %s", name, BlockAPI.getBlockName(block));
                }
            }
        }

        private void parseIntegerValues(IBlockStateProperty property, Set<Comparable> valueSet, String values) {
            int min = Integer.MAX_VALUE;
            int max = Integer.MIN_VALUE;
            for (Comparable c : valueSet) {
                min = Math.min(min, (Integer) c);
                max = Math.max(max, (Integer) c);
            }
            for (int i : MCPatcherUtils.parseIntegerList(values, min, max)) {
                valueSet.add(i);
            }
        }

        private Comparable parseNonIntegerValue(IBlockStateProperty property, String value) {
            for (Comparable propertyValue : property.getValues()) {
                if (value.equals(propertyValue.toString())) {
                    return propertyValue;
                }
            }
            return null;
        }

        @Override
        public boolean match(IBlockAccess blockAccess, int i, int j, int k) {
            return match(blockAccess.getBlockState(new Position(i, j, k)));
        }

        @Override
        public boolean match(Block block, int metadata) {
            throw new UnsupportedOperationException("match by metadata");
        }

        @Override
        public boolean matchBlockState(Object blockState) {
            return match((IBlockState) blockState);
        }

        @Override
        public boolean isUnfiltered() {
            return propertyMap.isEmpty();
        }

        private boolean match(IBlockState state) {
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
    }
}
