package com.prupe.mcpatcher.mal.block;

import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.mal.resource.PropertiesFile;
import net.minecraft.src.*;

import java.util.*;
import java.util.logging.Level;

abstract public class BlockStateMatcher {
    private final String fullString;
    private final ThreadLocal<Object> threadLocal = new ThreadLocal<Object>();

    protected final Block block;
    protected Object data;

    protected BlockStateMatcher(PropertiesFile source, String metaString, Block block, String metadataList, Map<String, String> properties) {
        this.fullString = BlockAPI.getBlockName(block) + metaString;
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

        V1(PropertiesFile source, String metaString, Block block, String metadataList, Map<String, String> properties) {
            super(source, metaString, block, metadataList, properties);
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

        V2(PropertiesFile source, String metaString, Block block, String metadataList, Map<String, String> properties) {
            super(source, metaString, block, metadataList, properties);
            IBlockState state = block.getBlockState();
            if (properties.isEmpty() && !MCPatcherUtils.isNullOrEmpty(metadataList)) {
                translateProperties(block, MCPatcherUtils.parseIntegerList(metadataList, 0, 15), properties);
                if (!properties.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    for (Map.Entry<String, String> entry : properties.entrySet()) {
                        if (sb.length() > 0) {
                            sb.append(':');
                        }
                        sb.append(entry.getKey()).append('=').append(entry.getValue());
                    }
                    source.warning("expanded %s:%s to %s", BlockAPI.getBlockName(block), metadataList, sb);
                }
            }
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
                                    source.warning("must be one of:%s", getPropertyValues(property));
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

        private static void translateProperties(Block block, int[] metadataList, Map<String, String> properties) {
            if (BlockAPI.getBlockName(block).equals("minecraft:log")) {
                StringBuilder sb = new StringBuilder();
                for (int i : metadataList) {
                    sb.append(i).append(',');
                    if ((i & 0xc) == 0) {
                        i &= 0x3;
                        sb.append(i | 0x4).append(',');
                        sb.append(i | 0x8).append(',');
                    }
                }
                metadataList = MCPatcherUtils.parseIntegerList(sb.toString(), 0, 15);
            }
            Map<IBlockStateProperty, Set<Comparable>> tmpMap = new HashMap<IBlockStateProperty, Set<Comparable>>();
            for (int i : metadataList) {
                IBlockState blockState = block.getStateFromMetadata(i);
                for (IBlockStateProperty property : blockState.getProperties()) {
                    Set<Comparable> values = tmpMap.get(property);
                    if (values == null) {
                        values = new HashSet<Comparable>();
                        tmpMap.put(property, values);
                    }
                    values.add(blockState.getProperty(property));
                }
            }
            for (IBlockStateProperty property : block.getBlockState().getProperties()) {
                Set<Comparable> values = tmpMap.get(property);
                if (values != null && values.size() > 0 && values.size() < property.getValues().size()) {
                    StringBuilder sb = new StringBuilder();
                    for (Comparable value : values) {
                        if (sb.length() > 0) {
                            sb.append(',');
                        }
                        sb.append(value.toString());
                    }
                    properties.put(property.getName(), sb.toString());
                }
            }
        }

        private static void parseIntegerValues(IBlockStateProperty property, Set<Comparable> valueSet, String values) {
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

        private static Comparable parseNonIntegerValue(IBlockStateProperty property, String value) {
            for (Comparable propertyValue : property.getValues()) {
                if (value.equals(propertyValue.toString())) {
                    return propertyValue;
                }
            }
            return null;
        }

        @SuppressWarnings("unchecked")
        private static String getPropertyValues(IBlockStateProperty property) {
            StringBuilder sb = new StringBuilder();
            List<Comparable> values = new ArrayList<Comparable>(property.getValues());
            Collections.sort(values);
            for (Comparable value : values) {
                sb.append(' ').append(value.toString());
            }
            return sb.toString();
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
