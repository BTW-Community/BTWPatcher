package com.prupe.mcpatcher.cc;

import com.prupe.mcpatcher.Config;
import com.prupe.mcpatcher.MCLogger;
import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.TexturePackAPI;
import com.prupe.mcpatcher.mal.block.BlockAPI;
import net.minecraft.src.Block;
import net.minecraft.src.IBlockAccess;
import net.minecraft.src.ResourceLocation;
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
    private static final ResourceLocation PINECOLOR = TexturePackAPI.newMCPatcherResourceLocation("colormap/pine.png");
    private static final ResourceLocation BIRCHCOLOR = TexturePackAPI.newMCPatcherResourceLocation("colormap/birch.png");
    private static final ResourceLocation FOLIAGECOLOR = TexturePackAPI.newMCPatcherResourceLocation("colormap/foliage.png");
    private static final ResourceLocation WATERCOLOR = TexturePackAPI.newMCPatcherResourceLocation("colormap/water.png");
    private static final ResourceLocation UNDERWATERCOLOR = TexturePackAPI.newMCPatcherResourceLocation("colormap/underwater.png");
    private static final ResourceLocation FOGCOLOR0 = TexturePackAPI.newMCPatcherResourceLocation("colormap/fog0.png");
    private static final ResourceLocation SKYCOLOR0 = TexturePackAPI.newMCPatcherResourceLocation("colormap/sky0.png");

    private static final String PALETTE_BLOCK_KEY = "palette.block.";

    private static final int BLOCK_ID_PUMPKIN_STEM = 104;
    private static final int BLOCK_ID_MELON_STEM = 105;

    private static final ColorMap[] blockColorMaps = new ColorMap[BlockAPI.getNumBlocks()]; // bitmaps from palette.block.*
    private static final Map<Float, ColorMap> blockMetaColorMaps = new HashMap<Float, ColorMap>(); // bitmaps from palette.block.*
    private static int lilypadColor; // lilypad
    private static float[][] redstoneColor; // colormap/redstone.png
    private static int[] pumpkinStemColors; // colormap/pumpkinstem.png
    private static int[] melonStemColors; // colormap/melonstem.png

    private static final int blockBlendRadius = Config.getInt(MCPatcherUtils.CUSTOM_COLORS, "blockBlendRadius", 1);
    private static final float blockBlendScale = (float) Math.pow(2 * blockBlendRadius + 1, -2);

    public static float[] waterColor;

    static {
        try {
            reset();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    static void reset() {
        Colorizer.fixedColorMaps[Colorizer.COLOR_MAP_SWAMP_GRASS] = new ColorMap(0x4e4e4e);
        Colorizer.fixedColorMaps[Colorizer.COLOR_MAP_SWAMP_FOLIAGE] = new ColorMap(0x4e4e4e);
        Colorizer.fixedColorMaps[Colorizer.COLOR_MAP_PINE] = new ColorMap(0x619961);
        Colorizer.fixedColorMaps[Colorizer.COLOR_MAP_BIRCH] = new ColorMap(0x80a755);
        Colorizer.fixedColorMaps[Colorizer.COLOR_MAP_FOLIAGE] = new ColorMap(0x48b518);
        Colorizer.fixedColorMaps[Colorizer.COLOR_MAP_WATER] = new ColorMap(0xffffff);
        Colorizer.fixedColorMaps[Colorizer.COLOR_MAP_UNDERWATER] = new ColorMap(0x050533);
        Colorizer.fixedColorMaps[Colorizer.COLOR_MAP_FOG0] = new ColorMap(0xc0d8ff);
        Colorizer.fixedColorMaps[Colorizer.COLOR_MAP_SKY0] = new ColorMap(0xffffff);

        Arrays.fill(blockColorMaps, null);
        blockMetaColorMaps.clear();

        lilypadColor = 0x208030;
        waterColor = new float[]{0.2f, 0.3f, 1.0f};
        redstoneColor = null;
        pumpkinStemColors = null;
        melonStemColors = null;
    }

    static void reloadColorMaps(Properties properties) {
        Colorizer.fixedColorMaps[Colorizer.COLOR_MAP_SWAMP_GRASS].loadColorMap(Colorizer.useSwampColors, SWAMPGRASSCOLOR);
        Colorizer.fixedColorMaps[Colorizer.COLOR_MAP_SWAMP_FOLIAGE].loadColorMap(Colorizer.useSwampColors, SWAMPFOLIAGECOLOR);
        Colorizer.fixedColorMaps[Colorizer.COLOR_MAP_PINE].loadColorMap(Colorizer.useTreeColors, PINECOLOR);
        Colorizer.fixedColorMaps[Colorizer.COLOR_MAP_BIRCH].loadColorMap(Colorizer.useTreeColors, BIRCHCOLOR);
        Colorizer.fixedColorMaps[Colorizer.COLOR_MAP_FOLIAGE].loadColorMap(Colorizer.useTreeColors, FOLIAGECOLOR);
        Colorizer.fixedColorMaps[Colorizer.COLOR_MAP_FOLIAGE].clear();
        Colorizer.fixedColorMaps[Colorizer.COLOR_MAP_WATER].loadColorMap(Colorizer.useWaterColors, WATERCOLOR);
        Colorizer.fixedColorMaps[Colorizer.COLOR_MAP_UNDERWATER].loadColorMap(Colorizer.useWaterColors, UNDERWATERCOLOR);
        Colorizer.fixedColorMaps[Colorizer.COLOR_MAP_FOG0].loadColorMap(Colorizer.useFogColors, FOGCOLOR0);
        Colorizer.fixedColorMaps[Colorizer.COLOR_MAP_SKY0].loadColorMap(Colorizer.useFogColors, SKYCOLOR0);
    }

    static void reloadSwampColors(Properties properties) {
        int[] temp = new int[]{lilypadColor};
        Colorizer.loadIntColor("lilypad", temp, 0);
        lilypadColor = temp[0];
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
            ResourceLocation address = TexturePackAPI.parseResourceLocation(Colorizer.COLOR_PROPERTIES, key.substring(PALETTE_BLOCK_KEY.length()).trim());
            if (address == null) {
                continue;
            }
            ColorMap colorMap = new ColorMap(0xffffff);
            colorMap.loadColorMap(true, address);
            if (!colorMap.isCustom()) {
                continue;
            }
            for (String idString : value.split("\\s+")) {
                String[] tokens = idString.split(":");
                int[] tokensInt = new int[tokens.length];
                try {
                    for (int i = 0; i < tokens.length; i++) {
                        tokensInt[i] = Integer.parseInt(tokens[i]);
                    }
                } catch (NumberFormatException e) {
                    continue;
                }
                switch (tokensInt.length) {
                    case 1:
                        if (tokensInt[0] < 0 || tokensInt[0] >= blockColorMaps.length) {
                            continue;
                        }
                        blockColorMaps[tokensInt[0]] = colorMap;
                        break;

                    case 2:
                        blockMetaColorMaps.put(ColorMap.getBlockMetaKey(tokensInt[0], tokensInt[1]), colorMap);
                        break;

                    default:
                        continue;
                }
                logger.finer("using %s for block %s, default color %06x", key, idString, colorMap.colorize());
            }
        }
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

    public static int colorizeBiome(int defaultColor, int index, double temperature, double rainfall) {
        return Colorizer.fixedColorMaps[index].colorize(defaultColor, temperature, rainfall);
    }

    public static int colorizeBiome(int defaultColor, int index) {
        return Colorizer.fixedColorMaps[index].colorize(defaultColor);
    }

    public static int colorizeBiome(int defaultColor, int index, int i, int j, int k) {
        return Colorizer.fixedColorMaps[index].colorize(defaultColor, i, j, k);
    }

    public static int colorizeBiomeWithBlending(int defaultColor, int index, int i, int j, int k) {
        return colorizeWithBlending(Colorizer.fixedColorMaps[index], defaultColor, i, j, k);
    }

    public static int colorizeWater(Object dummy, int i, int k) {
        return Colorizer.fixedColorMaps[Colorizer.COLOR_MAP_WATER].colorize(BiomeHelper.getWaterColorMultiplier(i, 64, k), i, 64, k);
    }

    public static int colorizeBlock(Block block, int i, int j, int k, int metadata) {
        ColorMap colorMap = null;
        int blockID = BlockAPI.getBlockId(block);
        if (!blockMetaColorMaps.isEmpty()) {
            colorMap = blockMetaColorMaps.get(ColorMap.getBlockMetaKey(blockID, metadata));
        }
        if (colorMap == null && blockID >= 0 && blockID < blockColorMaps.length) {
            colorMap = blockColorMaps[blockID];
        }
        return colorizeWithBlending(colorMap, 0xffffff, i, j, k);
    }

    private static int colorizeWithBlending(ColorMap colorMap, int defaultColor, int i, int j, int k) {
        if (colorMap == null || !colorMap.isCustom() || blockBlendRadius <= 0) {
            return defaultColor;
        }
        float[] f = new float[3];
        colorMap.colorizeWithBlending(i, j, k, blockBlendRadius, f);
        return Colorizer.float3ToInt(f);
    }

    public static int colorizeBlock(Block block) {
        ColorMap colorMap = blockColorMaps[BlockAPI.getBlockId(block)];
        if (colorMap == null) {
            return 0xffffff;
        } else {
            return colorMap.colorize(0xffffff);
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

    public static int getItemColorFromDamage(int defaultColor, int blockID, int damage) {
        if (blockID == 8 || blockID == 9) {
            return colorizeBiome(defaultColor, Colorizer.COLOR_MAP_WATER);
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

    public static boolean computeWaterColor(double x, double y, double z) {
        if (Colorizer.useParticleColors && Colorizer.fixedColorMaps[Colorizer.COLOR_MAP_WATER].isCustom()) {
            int rgb = colorizeBiome(0xffffff, Colorizer.COLOR_MAP_WATER, (int) x, (int) y, (int) z);
            float[] multiplier = new float[3];
            Colorizer.intToFloat3(rgb, multiplier);
            for (int i = 0; i < 3; i++) {
                waterColor[i] = multiplier[i] * ColorizeEntity.waterBaseColor[i];
            }
            return true;
        } else {
            return false;
        }
    }

    public static void computeWaterColor() {
        int rgb = colorizeBiome(0xffffff, Colorizer.COLOR_MAP_WATER);
        Colorizer.intToFloat3(rgb, waterColor);
    }

    public static void colorizeWaterBlockGL(int blockID) {
        if (blockID == 8 || blockID == 9) {
            computeWaterColor();
            GL11.glColor4f(waterColor[0], waterColor[1], waterColor[2], 1.0f);
        }
    }
}
