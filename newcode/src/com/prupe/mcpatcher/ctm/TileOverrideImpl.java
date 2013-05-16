package com.prupe.mcpatcher.ctm;

import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.TileLoader;
import com.prupe.mcpatcher.WeightedIndex;
import net.minecraft.src.Block;
import net.minecraft.src.IBlockAccess;
import net.minecraft.src.Icon;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

class TileOverrideImpl {
    final static class CTM extends TileOverride {
        // Index into this array is formed from these bit values:
        // 128 64  32
        // 1   *   16
        // 2   4   8
        private static final int[] neighborMap = new int[]{
            0, 3, 0, 3, 12, 5, 12, 15, 0, 3, 0, 3, 12, 5, 12, 15,
            1, 2, 1, 2, 4, 7, 4, 29, 1, 2, 1, 2, 13, 31, 13, 14,
            0, 3, 0, 3, 12, 5, 12, 15, 0, 3, 0, 3, 12, 5, 12, 15,
            1, 2, 1, 2, 4, 7, 4, 29, 1, 2, 1, 2, 13, 31, 13, 14,
            36, 17, 36, 17, 24, 19, 24, 43, 36, 17, 36, 17, 24, 19, 24, 43,
            16, 18, 16, 18, 6, 46, 6, 21, 16, 18, 16, 18, 28, 9, 28, 22,
            36, 17, 36, 17, 24, 19, 24, 43, 36, 17, 36, 17, 24, 19, 24, 43,
            37, 40, 37, 40, 30, 8, 30, 34, 37, 40, 37, 40, 25, 23, 25, 45,
            0, 3, 0, 3, 12, 5, 12, 15, 0, 3, 0, 3, 12, 5, 12, 15,
            1, 2, 1, 2, 4, 7, 4, 29, 1, 2, 1, 2, 13, 31, 13, 14,
            0, 3, 0, 3, 12, 5, 12, 15, 0, 3, 0, 3, 12, 5, 12, 15,
            1, 2, 1, 2, 4, 7, 4, 29, 1, 2, 1, 2, 13, 31, 13, 14,
            36, 39, 36, 39, 24, 41, 24, 27, 36, 39, 36, 39, 24, 41, 24, 27,
            16, 42, 16, 42, 6, 20, 6, 10, 16, 42, 16, 42, 28, 35, 28, 44,
            36, 39, 36, 39, 24, 41, 24, 27, 36, 39, 36, 39, 24, 41, 24, 27,
            37, 38, 37, 38, 30, 11, 30, 32, 37, 38, 37, 38, 25, 33, 25, 26,
        };

        CTM(String filePrefix, Properties properties, TileLoader tileLoader) {
            super(filePrefix, properties, tileLoader);
        }

        @Override
        String getMethod() {
            return "ctm";
        }

        @Override
        String checkTileMap() {
            if (getNumberOfTiles() >= 47) {
                return null;
            } else {
                return "requires at least 47 tiles";
            }
        }

        @Override
        boolean requiresFace() {
            return true;
        }

        @Override
        Icon getTileImpl(IBlockAccess blockAccess, Block block, Icon origIcon, int i, int j, int k, int face) {
            int[][] offsets = NEIGHBOR_OFFSET[face];
            int neighborBits = 0;
            for (int bit = 0; bit < 8; bit++) {
                if (shouldConnect(blockAccess, block, origIcon, i, j, k, face, offsets[bit])) {
                    neighborBits |= (1 << bit);
                }
            }
            return icons[neighborMap[neighborBits]];
        }

        @Override
        Icon getTileImpl(Block block, Icon origIcon, int face, int metadata) {
            return icons[0];
        }
    }

    static class Horizontal extends TileOverride {
        // Index into this array is formed from these bit values:
        // 1   *   2
        private static final int[] neighborMap = new int[]{
            3, 2, 0, 1,
        };

        Horizontal(String filePrefix, Properties properties, TileLoader tileLoader) {
            super(filePrefix, properties, tileLoader);
        }

        @Override
        String getMethod() {
            return "horizontal";
        }

        @Override
        String checkTileMap() {
            if (getNumberOfTiles() == 4) {
                return null;
            } else {
                return "requires exactly 4 tiles";
            }
        }

        @Override
        Icon getTileImpl(IBlockAccess blockAccess, Block block, Icon origIcon, int i, int j, int k, int face) {
            if (face < 0) {
                face = NORTH_FACE;
            } else if (reorient(face) <= TOP_FACE) {
                return null;
            }
            int[][] offsets = NEIGHBOR_OFFSET[face];
            int neighborBits = 0;
            if (shouldConnect(blockAccess, block, origIcon, i, j, k, face, offsets[rotateUV(REL_L)])) {
                neighborBits |= 1;
            }
            if (shouldConnect(blockAccess, block, origIcon, i, j, k, face, offsets[rotateUV(REL_R)])) {
                neighborBits |= 2;
            }
            return icons[neighborMap[neighborBits]];
        }

        @Override
        Icon getTileImpl(Block block, Icon origIcon, int face, int metadata) {
            return icons[3];
        }
    }

    final static class HorizontalVertical extends Horizontal {
        // Index into this array is formed from these bit values:
        // 32  16  8
        //     *
        // 1   2   4
        private static final int[] neighborMap = new int[]{
            3, 3, 6, 3, 3, 3, 3, 3, 3, 3, 6, 3, 3, 3, 3, 3,
            4, 4, 5, 4, 4, 4, 4, 4, 3, 3, 6, 3, 3, 3, 3, 3,
            3, 3, 6, 3, 3, 3, 3, 3, 3, 3, 6, 3, 3, 3, 3, 3,
            3, 3, 6, 3, 3, 3, 3, 3, 3, 3, 6, 3, 3, 3, 3, 3,
        };

        HorizontalVertical(String filePrefix, Properties properties, TileLoader tileLoader) {
            super(filePrefix, properties, tileLoader);
        }

        @Override
        String getMethod() {
            return "horizontal+vertical";
        }

        @Override
        String checkTileMap() {
            if (getNumberOfTiles() == 7) {
                return null;
            } else {
                return "requires exactly 7 tiles";
            }
        }

        @Override
        Icon getTileImpl(IBlockAccess blockAccess, Block block, Icon origIcon, int i, int j, int k, int face) {
            Icon icon = super.getTileImpl(blockAccess, block, origIcon, i, j, k, face);
            if (icon != icons[3]) {
                return icon;
            }
            int[][] offsets = NEIGHBOR_OFFSET[face];
            int neighborBits = 0;
            if (shouldConnect(blockAccess, block, origIcon, i, j, k, face, offsets[rotateUV(REL_DL)])) {
                neighborBits |= 1;
            }
            if (shouldConnect(blockAccess, block, origIcon, i, j, k, face, offsets[rotateUV(REL_D)])) {
                neighborBits |= 2;
            }
            if (shouldConnect(blockAccess, block, origIcon, i, j, k, face, offsets[rotateUV(REL_DR)])) {
                neighborBits |= 4;
            }
            if (shouldConnect(blockAccess, block, origIcon, i, j, k, face, offsets[rotateUV(REL_UR)])) {
                neighborBits |= 8;
            }
            if (shouldConnect(blockAccess, block, origIcon, i, j, k, face, offsets[rotateUV(REL_U)])) {
                neighborBits |= 16;
            }
            if (shouldConnect(blockAccess, block, origIcon, i, j, k, face, offsets[rotateUV(REL_UL)])) {
                neighborBits |= 32;
            }
            return icons[neighborMap[neighborBits]];
        }
    }

    static class Vertical extends TileOverride {
        // Index into this array is formed from these bit values:
        // 2
        // *
        // 1
        private static final int[] neighborMap = new int[]{
            3, 2, 0, 1,
        };

        Vertical(String filePrefix, Properties properties, TileLoader tileLoader) {
            super(filePrefix, properties, tileLoader);
        }

        @Override
        String getMethod() {
            return "vertical";
        }

        @Override
        String checkTileMap() {
            if (getNumberOfTiles() == 4) {
                return null;
            } else {
                return "requires exactly 4 tiles";
            }
        }

        @Override
        Icon getTileImpl(IBlockAccess blockAccess, Block block, Icon origIcon, int i, int j, int k, int face) {
            if (face < 0) {
                face = NORTH_FACE;
            } else if (reorient(face) <= TOP_FACE) {
                return null;
            }
            int[][] offsets = NEIGHBOR_OFFSET[face];
            int neighborBits = 0;
            if (shouldConnect(blockAccess, block, origIcon, i, j, k, face, offsets[rotateUV(REL_D)])) {
                neighborBits |= 1;
            }
            if (shouldConnect(blockAccess, block, origIcon, i, j, k, face, offsets[rotateUV(REL_U)])) {
                neighborBits |= 2;
            }
            return icons[neighborMap[neighborBits]];
        }

        @Override
        Icon getTileImpl(Block block, Icon origIcon, int face, int metadata) {
            return icons[3];
        }
    }

    final static class VerticalHorizontal extends Vertical {
        // Index into this array is formed from these bit values:
        // 32     16
        // 1   *   8
        // 2       4
        private static final int[] neighborMap = new int[]{
            3, 6, 3, 3, 3, 6, 3, 3, 4, 5, 4, 4, 3, 6, 3, 3,
            3, 6, 3, 3, 3, 6, 3, 3, 3, 6, 3, 3, 3, 6, 3, 3,
            3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 3, 3, 3, 3,
            3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3,
        };

        VerticalHorizontal(String filePrefix, Properties properties, TileLoader tileLoader) {
            super(filePrefix, properties, tileLoader);
        }

        @Override
        String getMethod() {
            return "vertical+horizontal";
        }

        @Override
        String checkTileMap() {
            if (getNumberOfTiles() == 7) {
                return null;
            } else {
                return "requires exactly 7 tiles";
            }
        }

        @Override
        Icon getTileImpl(IBlockAccess blockAccess, Block block, Icon origIcon, int i, int j, int k, int face) {
            Icon icon = super.getTileImpl(blockAccess, block, origIcon, i, j, k, face);
            if (icon != icons[3]) {
                return icon;
            }
            int[][] offsets = NEIGHBOR_OFFSET[face];
            int neighborBits = 0;
            if (shouldConnect(blockAccess, block, origIcon, i, j, k, face, offsets[rotateUV(REL_L)])) {
                neighborBits |= 1;
            }
            if (shouldConnect(blockAccess, block, origIcon, i, j, k, face, offsets[rotateUV(REL_DL)])) {
                neighborBits |= 2;
            }
            if (shouldConnect(blockAccess, block, origIcon, i, j, k, face, offsets[rotateUV(REL_DR)])) {
                neighborBits |= 4;
            }
            if (shouldConnect(blockAccess, block, origIcon, i, j, k, face, offsets[rotateUV(REL_R)])) {
                neighborBits |= 8;
            }
            if (shouldConnect(blockAccess, block, origIcon, i, j, k, face, offsets[rotateUV(REL_UR)])) {
                neighborBits |= 16;
            }
            if (shouldConnect(blockAccess, block, origIcon, i, j, k, face, offsets[rotateUV(REL_UL)])) {
                neighborBits |= 32;
            }
            return icons[neighborMap[neighborBits]];
        }
    }

    final static class Top extends TileOverride {
        Top(String filePrefix, Properties properties, TileLoader tileLoader) {
            super(filePrefix, properties, tileLoader);
        }

        @Override
        String getMethod() {
            return "top";
        }

        @Override
        String checkTileMap() {
            if (getNumberOfTiles() == 1) {
                return null;
            } else {
                return "requires exactly 1 tile";
            }
        }

        @Override
        Icon getTileImpl(IBlockAccess blockAccess, Block block, Icon origIcon, int i, int j, int k, int face) {
            if (face < 0) {
                face = NORTH_FACE;
            } else if (reorient(face) <= TOP_FACE) {
                return null;
            }
            int[][] offsets = NEIGHBOR_OFFSET[face];
            if (shouldConnect(blockAccess, block, origIcon, i, j, k, face, offsets[rotateUV(REL_U)])) {
                return icons[0];
            }
            return null;
        }

        @Override
        Icon getTileImpl(Block block, Icon origIcon, int face, int metadata) {
            return null;
        }
    }

    final static class Random1 extends TileOverride {
        private static final long P1 = 0x1c3764a30115L;
        private static final long P2 = 0x227c1adccd1dL;
        private static final long P3 = 0xe0d251c03ba5L;
        private static final long P4 = 0xa2fb1377aeb3L;
        private static final long MULTIPLIER = 0x5deece66dL;
        private static final long ADDEND = 0xbL;

        private final int symmetry;
        private final WeightedIndex chooser;

        Random1(String filePrefix, Properties properties, TileLoader tileLoader) {
            super(filePrefix, properties, tileLoader);

            String sym = properties.getProperty("symmetry", "none");
            if (sym.equals("all")) {
                symmetry = 6;
            } else if (sym.equals("opposite")) {
                symmetry = 2;
            } else {
                symmetry = 1;
            }

            chooser = WeightedIndex.create(getNumberOfTiles(), properties.getProperty("weights", ""));
            if (chooser == null) {
                error("invalid weights");
            }
        }

        @Override
        String getMethod() {
            return "random";
        }

        @Override
        Icon getTileImpl(IBlockAccess blockAccess, Block block, Icon origIcon, int i, int j, int k, int face) {
            if (getNumberOfTiles() == 1) {
                return icons[0];
            }
            if (face < 0) {
                face = 0;
            }
            face = reorient(face) / symmetry;
            long n = P1 * i * (i + ADDEND) + P2 * j * (j + ADDEND) + P3 * k * (k + ADDEND) + P4 * face * (face + ADDEND);
            n = MULTIPLIER * (n + i + j + k + face) + ADDEND;
            int index = chooser.choose(n);
            return icons[index];
        }

        @Override
        Icon getTileImpl(Block block, Icon origIcon, int face, int metadata) {
            return icons[0];
        }
    }

    final static class Repeat extends TileOverride {
        private final int width;
        private final int height;
        private final int symmetry;

        Repeat(String filePrefix, Properties properties, TileLoader tileLoader) {
            super(filePrefix, properties, tileLoader);
            width = MCPatcherUtils.getIntProperty(properties, "width", 0);
            height = MCPatcherUtils.getIntProperty(properties, "height", 0);
            if (width <= 0 || height <= 0) {
                error("invalid width and height (%dx%d)", width, height);
            }

            String sym = properties.getProperty("symmetry", "none");
            if (sym.equals("opposite")) {
                symmetry = ~1;
            } else {
                symmetry = -1;
            }
        }

        @Override
        String getMethod() {
            return "repeat";
        }

        @Override
        String checkTileMap() {
            if (getNumberOfTiles() == width * height) {
                return null;
            } else {
                return String.format("requires exactly %dx%d tiles", width, height);
            }
        }

        @Override
        Icon getTileImpl(IBlockAccess blockAccess, Block block, Icon origIcon, int i, int j, int k, int face) {
            if (face < 0) {
                face = 0;
            }
            face &= symmetry;
            int x;
            int y;
            switch (face) {
                case TOP_FACE:
                case BOTTOM_FACE:
                    if (rotateTop) {
                        x = k;
                        y = i;
                    } else {
                        x = i;
                        y = k;
                    }
                    break;

                case NORTH_FACE:
                    x = -i - 1;
                    y = -j;
                    break;

                case SOUTH_FACE:
                    x = i;
                    y = -j;
                    break;

                case WEST_FACE:
                    x = k;
                    y = -j;
                    break;

                case EAST_FACE:
                    x = -k - 1;
                    y = -j;
                    break;

                default:
                    return null;
            }
            x %= width;
            if (x < 0) {
                x += width;
            }
            y %= height;
            if (y < 0) {
                y += height;
            }
            return icons[width * y + x];
        }

        @Override
        Icon getTileImpl(Block block, Icon origIcon, int face, int metadata) {
            return icons[0];
        }
    }

    final static class Fixed extends TileOverride {
        Fixed(String filePrefix, Properties properties, TileLoader tileLoader) {
            super(filePrefix, properties, tileLoader);
        }

        @Override
        String getMethod() {
            return "fixed";
        }

        @Override
        String checkTileMap() {
            if (getNumberOfTiles() == 1) {
                return null;
            } else {
                return "requires exactly 1 tile";
            }
        }

        @Override
        Icon getTileImpl(IBlockAccess blockAccess, Block block, Icon origIcon, int i, int j, int k, int face) {
            return icons[0];
        }


        @Override
        Icon getTileImpl(Block block, Icon origIcon, int face, int metadata) {
            return icons[0];
        }
    }

    final static class BetterGrass implements ITileOverride {
        private static final int[][] OFFSET_MATRIX = {
            {0, -1},
            {0, 1},
            {-1, 0},
            {1, 0},
        };

        private final int blockID;
        private final String tileName;
        private final Icon fullTile;
        private final Icon fullSnowTile;

        BetterGrass(TileLoader tileLoader, int blockID, String tileName) {
            this.blockID = blockID;
            this.tileName = tileName;
            fullSnowTile = tileLoader.getIcon("snow");
            fullTile = tileLoader.getIcon(tileName + "_top");
        }

        @Override
        public String toString() {
            return "BetterGrass{" + tileName + "}";
        }

        public boolean isDisabled() {
            return false;
        }

        public void registerIcons() {
        }

        public Set<Integer> getMatchingBlocks() {
            Set<Integer> ids = new HashSet<Integer>();
            ids.add(blockID);
            return ids;
        }

        public Set<String> getMatchingTiles() {
            return null;
        }

        public int getRenderPass() {
            return 0;
        }

        public Icon getTile(IBlockAccess blockAccess, Block block, Icon origIcon, int i, int j, int k, int face) {
            if (face < 2 || face > 5) {
                return null;
            }
            int[] offset = OFFSET_MATRIX[face - 2];
            if (blockAccess.getBlockId(i + offset[0], j - 1, k + offset[1]) == blockID) {
                int neighborBlock = blockAccess.getBlockId(i, j + 1, k);
                if (neighborBlock == CTMUtils.BLOCK_ID_SNOW || neighborBlock == CTMUtils.BLOCK_ID_CRAFTED_SNOW) {
                    neighborBlock = blockAccess.getBlockId(i + offset[0], j, k + offset[1]);
                    if (neighborBlock == CTMUtils.BLOCK_ID_SNOW || neighborBlock == CTMUtils.BLOCK_ID_CRAFTED_SNOW) {
                        return fullSnowTile;
                    } else {
                        return null;
                    }
                } else {
                    return fullTile;
                }
            }
            return null;
        }

        boolean isBetterGrass(IBlockAccess blockAccess, Block block, int i, int j, int k, int face) {
            return block.blockID == blockID && getTile(blockAccess, block, null, i, j, k, face) == fullTile;
        }

        public Icon getTile(Block block, Icon origIcon, int face, int metadata) {
            return null;
        }
    }
}
