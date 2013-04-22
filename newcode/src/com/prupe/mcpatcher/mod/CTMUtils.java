package com.prupe.mcpatcher.mod;

import com.prupe.mcpatcher.*;
import net.minecraft.client.Minecraft;
import net.minecraft.src.*;

import java.awt.image.BufferedImage;
import java.util.*;

public class CTMUtils {
    private static final MCLogger logger = MCLogger.getLogger(MCPatcherUtils.CONNECTED_TEXTURES, "CTM");

    private static final boolean enableStandard = Config.getBoolean(MCPatcherUtils.CONNECTED_TEXTURES, "standard", true);
    private static final boolean enableNonStandard = Config.getBoolean(MCPatcherUtils.CONNECTED_TEXTURES, "nonStandard", true);
    private static final boolean enableGrass = Config.getBoolean(MCPatcherUtils.CONNECTED_TEXTURES, "grass", false);
    private static final int splitTextures = Config.getInt(MCPatcherUtils.CONNECTED_TEXTURES, "splitTextures", 1);
    private static final int maxRecursion = Config.getInt(MCPatcherUtils.CONNECTED_TEXTURES, "maxRecursion", 4);

    static final int BLOCK_ID_LOG = 17;
    static final int BLOCK_ID_QUARTZ = 155;
    static final int BLOCK_ID_GLASS = 20;
    static final int BLOCK_ID_GLASS_PANE = 102;
    static final int BLOCK_ID_BOOKSHELF = 47;
    static final int BLOCK_ID_GRASS = 2;
    static final int BLOCK_ID_MYCELIUM = 110;
    static final int BLOCK_ID_SNOW = 78;
    static final int BLOCK_ID_CRAFTED_SNOW = 80;

    private static final ITileOverride blockOverrides[][] = new ITileOverride[Block.blocksList.length][];
    private static final Map<String, ITileOverride[]> tileOverrides = new HashMap<String, ITileOverride[]>();
    private static TileOverrideImpl.BetterGrass betterGrass;

    static boolean active;
    static TextureMap terrainMap;
    private static List<TextureMap> ctmMaps = new ArrayList<TextureMap>();
    private static TileLoader tileLoader;

    static ITileOverride lastOverride;

    static {
        try {
            Class.forName(MCPatcherUtils.RENDER_PASS_CLASS).getMethod("finish").invoke(null);
        } catch (Throwable e) {
        }

        TexturePackChangeHandler.register(new TexturePackChangeHandler(MCPatcherUtils.CONNECTED_TEXTURES, 3) {
            @Override
            public void initialize() {
            }

            @Override
            public void beforeChange() {
                RenderPassAPI.instance.clear();
                Arrays.fill(blockOverrides, null);
                tileOverrides.clear();
                tileLoader = new TileLoader("terrain", true, logger);
                betterGrass = null;

                if (enableStandard || enableNonStandard) {
                    for (String s : TexturePackAPI.listResources("/ctm", ".properties", true, false, true)) {
                        registerOverride(TileOverride.create(s, tileLoader));
                    }
                }
            }

            @Override
            public void afterChange() {
                if (enableGrass) {
                    betterGrass = new TileOverrideImpl.BetterGrass(TileLoader.terrainMap, BLOCK_ID_GRASS, "grass");
                    new TileOverrideImpl.BetterGrass(TileLoader.itemsMap, BLOCK_ID_MYCELIUM, "mycel");
                }
                for (int i = 0; i < blockOverrides.length; i++) {
                    if (blockOverrides[i] != null && Block.blocksList[i] != null) {
                        for (ITileOverride override : blockOverrides[i]) {
                            if (override != null && !override.isDisabled() && override.getRenderPass() >= 0) {
                                RenderPassAPI.instance.setRenderPassForBlock(Block.blocksList[i], override.getRenderPass());
                            }
                        }
                    }
                }
            }
        });
    }

    public static void start() {
        lastOverride = null;
        if (terrainMap == null) {
            active = false;
            Tessellator.instance.textureMap = null;
        } else {
            active = true;
            Tessellator.instance.textureMap = terrainMap;
        }
    }

    public static Icon getTile(RenderBlocks renderBlocks, Block block, int i, int j, int k, Icon origIcon, Tessellator tessellator) {
        return getTile(renderBlocks, block, i, j, k, -1, origIcon, tessellator);
    }

    public static Icon getTile(RenderBlocks renderBlocks, Block block, final int i, final int j, final int k, final int face, Icon icon, Tessellator tessellator) {
        lastOverride = null;
        if (checkFace(face) && checkBlock(renderBlocks, block)) {
            final IBlockAccess blockAccess = renderBlocks.blockAccess;
            TileOverrideIterator iterator = new TileOverrideIterator(block, icon) {
                @Override
                Icon getTile(ITileOverride override, Block block, Icon currentIcon) {
                    return override.getTile(blockAccess, block, currentIcon, i, j, k, face);
                }
            };
            lastOverride = iterator.go();
            if (lastOverride != null) {
                icon = iterator.getIcon();
            }
        }
        return lastOverride == null && skipDefaultRendering(block) ? null : icon;
    }

    public static Icon getTile(RenderBlocks renderBlocks, Block block, int face, int metadata, Tessellator tessellator) {
        return getTile(renderBlocks, block, face, metadata, renderBlocks.getIconBySideAndMetadata(block, face, metadata), tessellator);
    }

    public static Icon getTile(RenderBlocks renderBlocks, Block block, int face, Tessellator tessellator) {
        return getTile(renderBlocks, block, face, 0, renderBlocks.getIconBySide(block, face), tessellator);
    }

    private static Icon getTile(RenderBlocks renderBlocks, Block block, final int face, final int metadata, Icon icon, Tessellator tessellator) {
        lastOverride = null;
        if (checkFace(face) && checkRenderType(block)) {
            TileOverrideIterator iterator = new TileOverrideIterator(block, icon) {
                @Override
                Icon getTile(ITileOverride override, Block block, Icon currentIcon) {
                    return override.getTile(block, currentIcon, face, metadata);
                }
            };
            lastOverride = iterator.go();
            if (lastOverride != null) {
                icon = iterator.getIcon();
            }
        }
        return icon;
    }

    public static void updateAnimations() {
        for (TextureMap textureMap : ctmMaps) {
            textureMap.updateAnimations();
        }
    }

    public static void reset() {
    }

    public static void finish() {
        reset();
        RenderPassAPI.instance.finish();
        Tessellator.instance.textureMap = null;
        lastOverride = null;
        active = false;
    }

    public static boolean isBetterGrass(IBlockAccess blockAccess, Block block, int i, int j, int k, int face) {
        return betterGrass != null && betterGrass.isBetterGrass(blockAccess, block, i, j, k, face);
    }

    private static boolean checkBlock(RenderBlocks renderBlocks, Block block) {
        return active && renderBlocks.blockAccess != null;
    }

    private static boolean checkFace(int face) {
        return active && (face < 0 ? enableNonStandard : enableStandard);
    }

    private static boolean checkRenderType(Block block) {
        switch (block.getRenderType()) {
            case 11: // fence
            case 21: // fence gate
                return false;

            default:
                return true;
        }
    }

    private static boolean skipDefaultRendering(Block block) {
        return RenderPassAPI.instance.skipDefaultRendering(block);
    }

    private static void registerOverride(ITileOverride override) {
        if (override != null && !override.isDisabled()) {
            if (override.getMatchingBlocks() != null) {
                for (int index : override.getMatchingBlocks()) {
                    String blockName = "";
                    if (index >= 0 && index < Block.blocksList.length && Block.blocksList[index] != null) {
                        blockName = Block.blocksList[index].getShortName();
                        if (blockName == null) {
                            blockName = "";
                        } else {
                            blockName = " (" + blockName + ")";
                        }
                    }
                    blockOverrides[index] = registerOverride(blockOverrides[index], override, "block " + index + blockName);
                }
            }
            if (override.getMatchingTiles() != null) {
                for (String name : override.getMatchingTiles()) {
                    tileOverrides.put(name, registerOverride(tileOverrides.get(name), override, "tile " + name));
                }
            }
        }
    }

    private static ITileOverride[] registerOverride(ITileOverride[] overrides, ITileOverride override, String description) {
        logger.fine("using %s for %s", override.toString(), description);
        if (overrides == null) {
            return new ITileOverride[]{override};
        } else {
            ITileOverride[] newList = new ITileOverride[overrides.length + 1];
            System.arraycopy(overrides, 0, newList, 0, overrides.length);
            newList[overrides.length] = override;
            return newList;
        }
    }

    abstract private static class TileOverrideIterator implements Iterator<ITileOverride> {
        private final Block block;
        private Icon currentIcon;
        private ITileOverride[] blockOverrides;
        private ITileOverride[] iconOverrides;
        private final Set<ITileOverride> skipOverrides = new HashSet<ITileOverride>();

        private int blockPos;
        private int iconPos;
        private boolean foundNext;
        private ITileOverride nextOverride;

        private ITileOverride lastMatchedOverride;

        TileOverrideIterator(Block block, Icon icon) {
            this.block = block;
            currentIcon = icon;
            blockOverrides = CTMUtils.blockOverrides[block.blockID];
            iconOverrides = CTMUtils.tileOverrides.get(this.currentIcon.getIconName());
        }

        private void resetForNextPass() {
            blockOverrides = null;
            iconOverrides = CTMUtils.tileOverrides.get(currentIcon.getIconName());
            blockPos = 0;
            iconPos = 0;
            foundNext = false;
        }

        public boolean hasNext() {
            if (foundNext) {
                return true;
            }
            if (iconOverrides != null) {
                while (iconPos < iconOverrides.length) {
                    if (checkOverride(iconOverrides[iconPos++])) {
                        return true;
                    }
                }
            }
            if (blockOverrides != null) {
                while (blockPos < blockOverrides.length) {
                    if (checkOverride(blockOverrides[blockPos++])) {
                        return true;
                    }
                }
            }
            return false;
        }

        public ITileOverride next() {
            if (!foundNext) {
                throw new IllegalStateException("next called before hasNext() == true");
            }
            foundNext = false;
            return nextOverride;
        }

        public void remove() {
            throw new UnsupportedOperationException("remove not supported");
        }

        private boolean checkOverride(ITileOverride override) {
            if (override != null && !override.isDisabled() && !skipOverrides.contains(override)) {
                foundNext = true;
                nextOverride = override;
                return true;
            } else {
                return false;
            }
        }

        ITileOverride go() {
            pass:
            for (int pass = 0; pass < maxRecursion; pass++) {
                while (hasNext()) {
                    ITileOverride override = next();
                    Icon newIcon = getTile(override, block, currentIcon);
                    if (newIcon != null) {
                        lastMatchedOverride = override;
                        skipOverrides.add(override);
                        currentIcon = newIcon;
                        resetForNextPass();
                        continue pass;
                    }
                }
                break;
            }
            return lastMatchedOverride;
        }

        Icon getIcon() {
            return currentIcon;
        }

        abstract Icon getTile(ITileOverride override, Block block, Icon currentIcon);
    }
}
