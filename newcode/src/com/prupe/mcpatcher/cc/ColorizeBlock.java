package com.prupe.mcpatcher.cc;

import com.prupe.mcpatcher.Config;
import com.prupe.mcpatcher.MCLogger;
import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.TexturePackAPI;
import com.prupe.mcpatcher.mal.biome.BiomeAPI;
import com.prupe.mcpatcher.mal.block.BlockAPI;
import net.minecraft.client.Minecraft;
import net.minecraft.src.*;
import org.lwjgl.opengl.GL11;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class ColorizeBlock {
    private static final MCLogger logger = MCLogger.getLogger(MCPatcherUtils.CUSTOM_COLORS);

    private static final ResourceLocation REDSTONE_COLORS = TexturePackAPI.newMCPatcherResourceLocation("colormap/redstone.png");
    private static final ResourceLocation STEM_COLORS = TexturePackAPI.newMCPatcherResourceLocation("colormap/stem.png");
    private static final ResourceLocation PUMPKIN_STEM_COLORS = TexturePackAPI.newMCPatcherResourceLocation("colormap/pumpkinstem.png");
    private static final ResourceLocation MELON_STEM_COLORS = TexturePackAPI.newMCPatcherResourceLocation("colormap/melonstem.png");
    private static final ResourceLocation SWAMPGRASSCOLOR = TexturePackAPI.newMCPatcherResourceLocation("colormap/swampgrass.png");
    private static final ResourceLocation SWAMPFOLIAGECOLOR = TexturePackAPI.newMCPatcherResourceLocation("colormap/swampfoliage.png");
    private static final ResourceLocation DEFAULT_GRASSCOLOR = new ResourceLocation("minecraft", "textures/colormap/grass.png");
    private static final ResourceLocation DEFAULT_FOLIAGECOLOR = new ResourceLocation("minecraft", "textures/colormap/foliage.png");
    private static final ResourceLocation PINECOLOR = TexturePackAPI.newMCPatcherResourceLocation("colormap/pine.png");
    private static final ResourceLocation BIRCHCOLOR = TexturePackAPI.newMCPatcherResourceLocation("colormap/birch.png");
    private static final ResourceLocation WATERCOLOR = TexturePackAPI.newMCPatcherResourceLocation("colormap/water.png");

    private static final String PALETTE_BLOCK_KEY = "palette.block.";

    private static final int BLOCK_ID_GRASS = 2;
    private static final int BLOCK_ID_WATER = 8;
    private static final int BLOCK_ID_WATER_STATIC = 9;
    private static final int BLOCK_ID_LEAVES = 18;
    private static final int BLOCK_ID_TALL_GRASS = 31;
    private static final int BLOCK_ID_PUMPKIN_STEM = 104;
    private static final int BLOCK_ID_MELON_STEM = 105;
    private static final int BLOCK_ID_VINE = 106;

    private static final ColorMap[] blockColorMaps = new ColorMap[BlockAPI.getNumBlocks()]; // bitmaps from palette.block.*
    private static final Map<Integer, ColorMap> blockMetaColorMaps = new HashMap<Integer, ColorMap>(); // bitmaps from palette.block.*
    private static ColorMap swampGrassColorMap;
    private static ColorMap swampFoliageColorMap;
    private static ColorMap waterColorMap;
    private static int lilypadColor; // lilypad
    private static float[][] redstoneColor; // colormap/redstone.png
    private static int[] pumpkinStemColors; // colormap/pumpkinstem.png
    private static int[] melonStemColors; // colormap/melonstem.png

    private static BiomeGenBase swampBiome;
    private static Block waterBlock;

    private static final int blockBlendRadius = Config.getInt(MCPatcherUtils.CUSTOM_COLORS, "blockBlendRadius", 1);

    public static int blockColor;
    public static float[] waterColor;

    private static final float[] AO_BASE = new float[]{0.5f, 1.0f, 0.8f, 0.8f, 0.6f, 0.6f};

    private static final int[][][] FACE_VERTICES = new int[][][]{
        // bottom face (y=0)
        {
            {0, 0, 1}, // top left
            {0, 0, 0}, // bottom left
            {1, 0, 0}, // bottom right
            {1, 0, 1}, // top right
        },
        // top face (y=1)
        {
            {1, 1, 1},
            {1, 1, 0},
            {0, 1, 0},
            {0, 1, 1},
        },
        // north face (z=0)
        {
            {0, 1, 0},
            {1, 1, 0},
            {1, 0, 0},
            {0, 0, 0},
        },
        // south face (z=1)
        {
            {0, 1, 1},
            {0, 0, 1},
            {1, 0, 1},
            {1, 1, 1},
        },
        // west face (x=0)
        {
            {0, 1, 1},
            {0, 1, 0},
            {0, 0, 0},
            {0, 0, 1},
        },
        // east face (x=1)
        {
            {1, 0, 1},
            {1, 0, 0},
            {1, 1, 0},
            {1, 1, 1},
        },
    };

    private static ColorMap lastColorMap;
    private static int lastI = Integer.MIN_VALUE;
    private static int lastJ = Integer.MIN_VALUE;
    private static int lastK = Integer.MIN_VALUE;
    private static final float[][][][] cubeColors = new float[2][2][2][3];

    static {
        try {
            reset();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    static void reset() {
        swampBiome = BiomeAPI.findBiomeByName("Swampland");
        waterBlock = BlockAPI.getBlockById(BLOCK_ID_WATER);

        Arrays.fill(blockColorMaps, null);
        blockMetaColorMaps.clear();
        swampGrassColorMap = null;
        swampFoliageColorMap = null;
        waterColorMap = null;
        lastColorMap = null;

        lilypadColor = 0x208030;
        waterColor = new float[]{0.2f, 0.3f, 1.0f};
        redstoneColor = null;
        pumpkinStemColors = null;
        melonStemColors = null;
    }

    static void reloadFoliageColors(Properties properties) {
        registerColorMap(DEFAULT_GRASSCOLOR, BLOCK_ID_GRASS + " " + BLOCK_ID_TALL_GRASS + ":1,2");
        registerColorMap(DEFAULT_FOLIAGECOLOR, BLOCK_ID_LEAVES + ":0,4,8,12 " + BLOCK_ID_VINE);
        registerColorMap(PINECOLOR, BLOCK_ID_LEAVES + ":1,5,9,13");
        registerColorMap(BIRCHCOLOR, BLOCK_ID_LEAVES + ":2,6,10,14");
    }

    static void reloadWaterColors(Properties properties) {
        waterColorMap = registerColorMap(WATERCOLOR, BLOCK_ID_WATER + " " + BLOCK_ID_WATER_STATIC);
        if (waterColorMap != null) {
            waterColorMap.multiplyMap(ColorizeEntity.waterBaseColor);
        }
    }

    static void reloadSwampColors(Properties properties) {
        int[] temp = new int[]{lilypadColor};
        Colorizer.loadIntColor("lilypad", temp, 0);
        lilypadColor = temp[0];

        swampGrassColorMap = ColorMap.loadColorMap(Colorizer.useSwampColors, SWAMPGRASSCOLOR);
        swampFoliageColorMap = ColorMap.loadColorMap(Colorizer.useSwampColors, SWAMPFOLIAGECOLOR);
    }

    static void reloadBlockColors(Properties properties) {
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            if (!(entry.getKey() instanceof String) || !(entry.getValue() instanceof String)) {
                continue;
            }
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            if (!key.startsWith(PALETTE_BLOCK_KEY)) {
                continue;
            }
            key = key.substring(PALETTE_BLOCK_KEY.length()).trim();
            ResourceLocation resource = TexturePackAPI.parseResourceLocation(Colorizer.COLOR_PROPERTIES, key);
            if (resource == null) {
                continue;
            }
            registerColorMap(resource, value);
        }
    }

    private static ColorMap registerColorMap(ResourceLocation resource, String idList) {
        ColorMap colorMap = ColorMap.loadColorMap(true, resource);
        if (colorMap == null) {
            return null;
        }
        for (String idString : idList.split("\\s+")) {
            String[] tokens = idString.split(":");
            int[] blockIds = MCPatcherUtils.parseIntegerList(tokens[0], 0, blockColorMaps.length - 1);
            if (tokens.length > 1) {
                int[] metadata = MCPatcherUtils.parseIntegerList(tokens[1], 0, 15);
                for (int blockId : blockIds) {
                    for (int meta : metadata) {
                        blockMetaColorMaps.put(ColorMap.getBlockMetaKey(blockId, meta), colorMap);
                    }
                }
            } else {
                for (int blockId : blockIds) {
                    blockColorMaps[blockId] = colorMap;
                }
            }
            logger.finer("using %s for block %s, default color %06x", resource, idString, colorMap.getColorMultiplier());
        }
        return colorMap;
    }

    static void reloadRedstoneColors(Properties properties) {
        int[] rgb = MCPatcherUtils.getImageRGB(TexturePackAPI.getImage(REDSTONE_COLORS));
        if (rgb != null && rgb.length >= 16) {
            redstoneColor = new float[16][];
            for (int i = 0; i < 16; i++) {
                float[] f = new float[3];
                Colorizer.intToFloat3(rgb[i], f);
                redstoneColor[i] = f;
            }
        }
    }

    static void reloadStemColors(Properties properties) {
        int[] stemColors = getStemRGB(STEM_COLORS);
        pumpkinStemColors = getStemRGB(PUMPKIN_STEM_COLORS);
        if (pumpkinStemColors == null) {
            pumpkinStemColors = stemColors;
        }
        melonStemColors = getStemRGB(MELON_STEM_COLORS);
        if (melonStemColors == null) {
            melonStemColors = stemColors;
        }
    }

    private static int[] getStemRGB(ResourceLocation resource) {
        int[] rgb = MCPatcherUtils.getImageRGB(TexturePackAPI.getImage(resource));
        return rgb == null || rgb.length < 8 ? null : rgb;
    }

    private static ColorMap findColorMap(int blockId, int metadata) {
        ColorMap colorMap = blockMetaColorMaps.get(ColorMap.getBlockMetaKey(blockId, metadata));
        if (colorMap != null) {
            return colorMap;
        }
        return blockColorMaps[blockId];
    }

    private static ColorMap findColorMap(int blockId, int metadata, BiomeGenBase biome) {
        if (biome == swampBiome) {
            switch (blockId) {
                case BLOCK_ID_GRASS:
                    if (swampGrassColorMap != null) {
                        return swampGrassColorMap;
                    }

                case BLOCK_ID_LEAVES:
                    if (swampFoliageColorMap != null) {
                        return swampFoliageColorMap;
                    }

                default:
                    break;
            }
        }
        return findColorMap(blockId, metadata);
    }

    private static ColorMap findColorMap(Block block, IBlockAccess blockAccess, int i, int j, int k) {
        int metadata = blockAccess.getBlockMetadata(i, j, k);
        int blockId = BlockAPI.getBlockId(block);
        return findColorMap(blockId, metadata, BiomeAPI.getBiomeGenAt(i, j, k));
    }

    public static boolean colorizeBlock(Block block) {
        ColorMap colorMap = blockColorMaps[BlockAPI.getBlockId(block)];
        if (colorMap == null) {
            return false;
        } else {
            blockColor = colorMap.getColorMultiplier();
            return true;
        }
    }

    public static boolean colorizeBlock(Block block, int metadata) {
        int blockId = BlockAPI.getBlockId(block);
        ColorMap colorMap = findColorMap(blockId, metadata);
        if (colorMap == null) {
            return false;
        } else {
            blockColor = colorMap.getColorMultiplier();
            return true;
        }
    }

    public static boolean colorizeBlock(Block block, IBlockAccess blockAccess, int i, int j, int k) {
        ColorMap colorMap = findColorMap(block, blockAccess, i, j, k);
        return colorizeBlock(block, blockAccess, colorMap, i, j, k);
    }

    private static boolean colorizeBlock(Block block, IBlockAccess blockAccess, ColorMap colorMap, int i, int j, int k) {
        if (colorMap == null) {
            return false;
        } else {
            blockColor = colorMap.getColorMultiplier(i, j, k, blockBlendRadius);
            return true;
        }
    }

    public static int getColorMultiplier(Block block, int i, int j, int k) {
        if (colorizeBlock(block, Minecraft.getInstance().theWorld, i, j, k)) {
            return blockColor;
        } else {
            return 0xffffff;
        }
    }

    public static void computeWaterColor() {
        int color = waterColorMap == null ? waterBlock.getBlockColor() : waterColorMap.getColorMultiplier();
        Colorizer.setColorF(color);
    }

    public static boolean computeWaterColor(int i, int j, int k) {
        if (waterColorMap == null) {
            return false;
        } else {
            Colorizer.setColorF(waterColorMap.getColorMultiplier(i, j, k));
            return true;
        }
    }

    public static int colorizeStem(int defaultColor, Block block, int blockMetadata) {
        int[] colors;
        switch (BlockAPI.getBlockId(block)) {
            case BLOCK_ID_PUMPKIN_STEM:
                colors = pumpkinStemColors;
                break;

            case BLOCK_ID_MELON_STEM:
                colors = melonStemColors;
                break;

            default:
                return defaultColor;
        }
        return colors == null ? defaultColor : colors[blockMetadata & 0x7];
    }

    public static int getLilyPadColor() {
        return lilypadColor;
    }

    public static int getItemColorFromDamage(int defaultColor, int blockId, int damage) {
        if (blockId == BLOCK_ID_WATER || blockId == BLOCK_ID_WATER_STATIC) {
            return colorizeBlock(BlockAPI.getBlockById(blockId), damage) ? blockColor : defaultColor;
        } else {
            return defaultColor;
        }
    }

    public static boolean computeRedstoneWireColor(int current) {
        if (redstoneColor == null) {
            return false;
        } else {
            System.arraycopy(redstoneColor[Math.max(Math.min(current, 15), 0)], 0, Colorizer.setColor, 0, 3);
            return true;
        }
    }

    public static int colorizeRedstoneWire(IBlockAccess blockAccess, int i, int j, int k, int defaultColor) {
        if (redstoneColor == null) {
            return defaultColor;
        } else {
            int metadata = Math.max(Math.min(blockAccess.getBlockMetadata(i, j, k), 15), 0);
            return Colorizer.float3ToInt(redstoneColor[metadata]);
        }
    }

    public static void colorizeWaterBlockGL(int blockID) {
        if (blockID == BLOCK_ID_WATER || blockID == BLOCK_ID_WATER_STATIC) {
            GL11.glColor4f(waterColor[0], waterColor[1], waterColor[2], 1.0f);
        }
    }

    private static void computeCubeColors(Block block, IBlockAccess blockAccess, ColorMap colorMap, int i, int j, int k, int di, int dj, int dk) {
        int rgb;
        if (true) { // TODO: remove
            colorizeBlock(block, blockAccess, colorMap, i + di, j + dj, k + dk);
            rgb = blockColor;
        } else {
            rgb = 0;
            rgb |= (i + di) % 2 == 0 ? 0 : 0xff0000;
            rgb |= (j + dj) % 2 == 0 ? 0 : 0xff00;
            rgb |= (k + dk) % 2 == 0 ? 0 : 0xff;
        }
        Colorizer.intToFloat3(rgb, cubeColors[di][dj][dk]);
    }

    private static ColorMap computeCubeColors(Block block, IBlockAccess blockAccess, int i, int j, int k) {
        ColorMap colorMap = findColorMap(block, blockAccess, i, j, k);
        if (colorMap != null) {
            computeCubeColors(block, blockAccess, colorMap, i, j, k, 0, 0, 0);
            computeCubeColors(block, blockAccess, colorMap, i, j, k, 0, 1, 0);
            computeCubeColors(block, blockAccess, colorMap, i, j, k, 0, 0, 1);
            computeCubeColors(block, blockAccess, colorMap, i, j, k, 0, 1, 1);
            computeCubeColors(block, blockAccess, colorMap, i, j, k, 1, 0, 0);
            computeCubeColors(block, blockAccess, colorMap, i, j, k, 1, 1, 0);
            computeCubeColors(block, blockAccess, colorMap, i, j, k, 1, 0, 1);
            computeCubeColors(block, blockAccess, colorMap, i, j, k, 1, 1, 1);
        }
        return colorMap;
    }

    public static boolean setupBiomeSmoothing(RenderBlocks renderBlocks, Block block, IBlockAccess blockAccess,
                                              int i, int j, int k, int face,
                                              boolean useColor, float r, float g, float b,
                                              float topLeft, float bottomLeft, float bottomRight, float topRight) {
        if (face != 1 && !useColor) { // TODO: also check for renderPass > 1
            return false;
        }
        if (i != lastI || j != lastJ || k != lastK) {
            lastColorMap = computeCubeColors(block, blockAccess, i, j, k);
            lastI = i;
            lastJ = j;
            lastK = k;
        }
        if (lastColorMap == null) {
            return false;
        }

        float aoBase = AO_BASE[face];
        topLeft *= aoBase;
        bottomLeft *= aoBase;
        bottomRight *= aoBase;
        topRight *= aoBase;

        int[][] offsets = FACE_VERTICES[face];
        int[] offset;
        float[] color;

        offset = offsets[0];
        color = cubeColors[offset[0]][offset[1]][offset[2]];
        renderBlocks.colorRedTopLeft = topLeft * color[0];
        renderBlocks.colorGreenTopLeft = topLeft * color[1];
        renderBlocks.colorBlueTopLeft = topLeft * color[2];

        offset = offsets[1];
        color = cubeColors[offset[0]][offset[1]][offset[2]];
        renderBlocks.colorRedBottomLeft = bottomLeft * color[0];
        renderBlocks.colorGreenBottomLeft = bottomLeft * color[1];
        renderBlocks.colorBlueBottomLeft = bottomLeft * color[2];

        offset = offsets[2];
        color = cubeColors[offset[0]][offset[1]][offset[2]];
        renderBlocks.colorRedBottomRight = bottomRight * color[0];
        renderBlocks.colorGreenBottomRight = bottomRight * color[1];
        renderBlocks.colorBlueBottomRight = bottomRight * color[2];

        offset = offsets[3];
        color = cubeColors[offset[0]][offset[1]][offset[2]];
        renderBlocks.colorRedTopRight = topRight * color[0];
        renderBlocks.colorGreenTopRight = topRight * color[1];
        renderBlocks.colorBlueTopRight = topRight * color[2];

        return true;
    }
}
