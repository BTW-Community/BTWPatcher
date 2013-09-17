package com.prupe.mcpatcher.ctm;

import com.prupe.mcpatcher.*;
import com.prupe.mcpatcher.mal.block.BlockAPI;
import net.minecraft.src.*;

import java.util.*;

public class CTMUtils {
    private static final MCLogger logger = MCLogger.getLogger(MCPatcherUtils.CONNECTED_TEXTURES, "CTM");

    private static final boolean enableStandard = Config.getBoolean(MCPatcherUtils.CONNECTED_TEXTURES, "standard", true);
    private static final boolean enableNonStandard = Config.getBoolean(MCPatcherUtils.CONNECTED_TEXTURES, "nonStandard", true);
    private static final boolean enableGrass = Config.getBoolean(MCPatcherUtils.CONNECTED_TEXTURES, "grass", false);
    private static final int maxRecursion = Config.getInt(MCPatcherUtils.CONNECTED_TEXTURES, "maxRecursion", 4);

    private static Block grassBlock;
    private static Block mycelBlock;
    static Block snowBlock;
    static Block craftedSnowBlock;

    private static final List<ITileOverride> allOverrides = new ArrayList<ITileOverride>();
    private static final Map<Block, List<ITileOverride>> blockOverrides = new IdentityHashMap<Block, List<ITileOverride>>();
    private static final Map<String, List<ITileOverride>> tileOverrides = new IdentityHashMap<String, List<ITileOverride>>();
    private static TileLoader tileLoader;
    private static TileOverrideImpl.BetterGrass betterGrass;

    static boolean active;
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
                grassBlock = BlockAPI.parseBlockName("minecraft:grass");
                mycelBlock = BlockAPI.parseBlockName("minecraft:mycelium");
                snowBlock = BlockAPI.parseBlockName("minecraft:snow_layer");
                craftedSnowBlock = BlockAPI.parseBlockName("minecraft:snow");

                RenderPassAPI.instance.clear();
                allOverrides.clear();
                blockOverrides.clear();
                tileOverrides.clear();
                tileLoader = new TileLoader("textures/blocks", true, logger);
                betterGrass = null;

                if (enableStandard || enableNonStandard) {
                    for (ResourceLocation resource : TexturePackAPI.listResources(TexturePackAPI.MCPATCHER_SUBDIR + "ctm", ".properties", true, false, true)) {
                        registerOverride(TileOverride.create(resource, tileLoader));
                    }
                }
            }

            @Override
            public void afterChange() {
                if (enableGrass) {
                    registerOverride(betterGrass = new TileOverrideImpl.BetterGrass(tileLoader, grassBlock, "grass"));
                    registerOverride(new TileOverrideImpl.BetterGrass(tileLoader, mycelBlock, "mycel"));
                }
                for (ITileOverride override : allOverrides) {
                    override.registerIcons();
                }
                for (Map.Entry<Block, List<ITileOverride>> entry : blockOverrides.entrySet()) {
                    for (ITileOverride override : entry.getValue()) {
                        if (override.getRenderPass() >= 0) {
                            RenderPassAPI.instance.setRenderPassForBlock(entry.getKey(), override.getRenderPass());
                        }
                    }
                }
                for (List<ITileOverride> overrides : blockOverrides.values()) {
                    Collections.sort(overrides);
                }
                for (List<ITileOverride> overrides : tileOverrides.values()) {
                    Collections.sort(overrides);
                }
            }

            private void sortOverrides(ITileOverride[] overrides) {
                if (overrides != null) {
                    Arrays.sort(overrides);
                }
            }
        });
    }

    public static void start() {
        lastOverride = null;
        active = tileLoader.setDefaultTextureMap(Tessellator.instance);
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

    public static void reset() {
    }

    public static void finish() {
        reset();
        RenderPassAPI.instance.finish();
        TessellatorUtils.clearDefaultTextureMap(Tessellator.instance);
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
            boolean registered = false;
            if (override.getMatchingBlocks() != null) {
                for (Block block : override.getMatchingBlocks()) {
                    if (block == null) {
                        continue;
                    }
                    List<ITileOverride> list = blockOverrides.get(block);
                    if (list == null) {
                        list = new ArrayList<ITileOverride>();
                        blockOverrides.put(block, list);
                    }
                    list.add(override);
                    logger.fine("using %s for block %s", override, BlockAPI.getBlockName(block));
                    registered = true;
                }
            }
            if (override.getMatchingTiles() != null) {
                for (String name : override.getMatchingTiles()) {
                    List<ITileOverride> list = tileOverrides.get(name);
                    if (list == null) {
                        list = new ArrayList<ITileOverride>();
                        tileOverrides.put(name, list);
                    }
                    list.add(override);
                    logger.fine("using %s for tile %s", override, name);
                    registered = true;
                }
            }
            if (registered) {
                allOverrides.add(override);
            }
        }
    }

    abstract private static class TileOverrideIterator implements Iterator<ITileOverride> {
        private final Block block;
        private Icon currentIcon;
        private List<ITileOverride> blockOverride;
        private List<ITileOverride> iconOverride;
        private final Set<ITileOverride> skipOverrides = new HashSet<ITileOverride>();

        private int blockPos;
        private int iconPos;
        private boolean foundNext;
        private ITileOverride nextOverride;

        private ITileOverride lastMatchedOverride;

        TileOverrideIterator(Block block, Icon icon) {
            this.block = block;
            currentIcon = icon;
            blockOverride = blockOverrides.get(block);
            iconOverride = tileOverrides.get(this.currentIcon.getIconName());
        }

        private void resetForNextPass() {
            blockOverride = null;
            iconOverride = CTMUtils.tileOverrides.get(currentIcon.getIconName());
            blockPos = 0;
            iconPos = 0;
            foundNext = false;
        }

        public boolean hasNext() {
            if (foundNext) {
                return true;
            }
            if (iconOverride != null) {
                while (iconPos < iconOverride.size()) {
                    if (checkOverride(iconOverride.get(iconPos++))) {
                        return true;
                    }
                }
            }
            if (blockOverride != null) {
                while (blockPos < blockOverride.size()) {
                    if (checkOverride(blockOverride.get(blockPos++))) {
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
