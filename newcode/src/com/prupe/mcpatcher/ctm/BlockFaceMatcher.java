package com.prupe.mcpatcher.ctm;

import com.prupe.mcpatcher.MAL;
import com.prupe.mcpatcher.MCPatcherUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static com.prupe.mcpatcher.ctm.RenderBlockState.*;

abstract public class BlockFaceMatcher {
    private static final Class<? extends BlockFaceMatcher> matcherClass = MAL.getVersion("block") <= 2 ? V1.class : V2.class;

    public static BlockFaceMatcher create(String propertyValue) {
        if (!MCPatcherUtils.isNullOrEmpty(propertyValue)) {
            String[] values = propertyValue.toLowerCase().split("\\s+");
            try {
                return matcherClass.getDeclaredConstructor(String[].class).newInstance((Object) values);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    abstract public boolean match(RenderBlockState renderBlockState);

    final public static class V1 extends BlockFaceMatcher {
        private final int faces;

        protected V1(String[] value) {
            int flags = 0;
            for (String val : value) {
                if (val.equals("bottom")) {
                    flags |= (1 << BOTTOM_FACE);
                } else if (val.equals("top")) {
                    flags |= (1 << TOP_FACE);
                } else if (val.equals("north")) {
                    flags |= (1 << NORTH_FACE);
                } else if (val.equals("south")) {
                    flags |= (1 << SOUTH_FACE);
                } else if (val.equals("east")) {
                    flags |= (1 << EAST_FACE);
                } else if (val.equals("west")) {
                    flags |= (1 << WEST_FACE);
                } else if (val.equals("side") || val.equals("sides")) {
                    flags |= (1 << NORTH_FACE) | (1 << SOUTH_FACE) | (1 << EAST_FACE) | (1 << WEST_FACE);
                } else if (val.equals("all")) {
                    flags = -1;
                }
            }
            faces = flags;
        }

        @Override
        public boolean match(RenderBlockState renderBlockState) {
            int face = renderBlockState.getTextureFace();
            return face >= 0 && (faces & (1 << face)) != 0;
        }
    }

    final public static class V2 extends BlockFaceMatcher {
        private final Set<String> faces = new HashSet<String>();

        protected V2(String[] value) {
            Collections.addAll(faces, value);
        }

        @Override
        public boolean match(RenderBlockState renderBlockState) {
            return faces.contains(renderBlockState.getTextureFaceName());
        }
    }
}
