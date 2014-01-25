package com.prupe.mcpatcher.ctm;

import com.prupe.mcpatcher.*;
import com.prupe.mcpatcher.mal.block.BlockAPI;
import com.prupe.mcpatcher.mal.block.RenderPassAPI;
import net.minecraft.src.*;

import java.util.*;

public class CTMUtils {
    private static final MCLogger logger = MCLogger.getLogger(MCPatcherUtils.CONNECTED_TEXTURES, "CTM");

    private static final boolean enableStandard = Config.getBoolean(MCPatcherUtils.CONNECTED_TEXTURES, "standard", true);
    private static final boolean enableNonStandard = Config.getBoolean(MCPatcherUtils.CONNECTED_TEXTURES, "nonStandard", true);

    private static final List<ITileOverride> allOverrides = new ArrayList<ITileOverride>();
    private static final Map<Block, List<ITileOverride>> blockOverrides = new IdentityHashMap<Block, List<ITileOverride>>();
    private static final Map<String, List<ITileOverride>> tileOverrides = new HashMap<String, List<ITileOverride>>();
    private static TileLoader tileLoader;

    private static ITileOverride lastOverride;
    private static Icon blankIcon;

    private static final TileOverrideIterator.IJK ijkIterator = new TileOverrideIterator.IJK(blockOverrides, tileOverrides);
    private static final TileOverrideIterator.Metadata metadataIterator = new TileOverrideIterator.Metadata(blockOverrides, tileOverrides);

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
                GlassPaneRenderer.clear();
                ijkIterator.clear();
                metadataIterator.clear();
                allOverrides.clear();
                blockOverrides.clear();
                tileOverrides.clear();
                lastOverride = null;
                blankIcon = null;
                tileLoader = new TileLoader("textures/blocks", logger);

                if (enableStandard || enableNonStandard) {
                    for (ResourceLocation resource : TexturePackAPI.listResources(TexturePackAPI.MCPATCHER_SUBDIR + "ctm", ".properties", true, false, true)) {
                        registerOverride(TileOverride.create(resource, tileLoader));
                    }
                }
                for (ResourceLocation resource : BlendMethod.getAllBlankResources()) {
                    tileLoader.preloadTile(resource, false);
                }
            }

            @Override
            public void afterChange() {
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
                setBlankResource();
            }
        });
    }

    public static Icon getBlockIcon(Icon icon, RenderBlocks renderBlocks, Block block, IBlockAccess blockAccess, int i, int j, int k, int face) {
        lastOverride = null;
        face = remapFace(block, face);
        if (checkFace(face)) {
            ijkIterator.setup(blockAccess, block, i, j, k, face, icon);
            lastOverride = ijkIterator.go();
            if (lastOverride != null) {
                icon = ijkIterator.getIcon();
            }
        }
        return lastOverride == null && skipDefaultRendering(block) ? blankIcon : icon;
    }

    public static Icon getBlockIcon(Icon icon, RenderBlocks renderBlocks, Block block, int face, int metadata) {
        lastOverride = null;
        face = remapFace(block, face);
        if (checkFace(face) && checkRenderType(block)) {
            metadataIterator.setup(block, face, metadata, icon);
            lastOverride = metadataIterator.go();
            if (lastOverride != null) {
                icon = metadataIterator.getIcon();
            }
        }
        return icon;
    }

    public static Icon getBlockIcon(Icon icon, RenderBlocks renderBlocks, Block block, int face) {
        return getBlockIcon(icon, renderBlocks, block, face, 0);
    }

    public static void reset() {
    }

    private static int remapFace(Block block, int face) {
        switch (block.getRenderType()) {
            case 1: // renderCrossedSquares
                return -1;

            default:
                return face;
        }
    }

    private static boolean checkFace(int face) {
        return face < 0 ? enableNonStandard : enableStandard;
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

    static void setBlankResource() {
        blankIcon = tileLoader.getIcon(RenderPassAPI.instance.getBlankResource());
    }
}
