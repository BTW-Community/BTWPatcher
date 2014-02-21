package com.prupe.mcpatcher.ctm;

import com.prupe.mcpatcher.*;
import com.prupe.mcpatcher.mal.block.BlockAPI;
import com.prupe.mcpatcher.mal.block.RenderPassAPI;
import com.prupe.mcpatcher.mal.resource.ResourceList;
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

    private static boolean haveCullFace;
    private static int currentCullFace;
    private static final BlockOrientation blockOrientation = new BlockOrientation();

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
                blockOrientation.clear();
                ijkIterator.clear();
                metadataIterator.clear();
                allOverrides.clear();
                blockOverrides.clear();
                tileOverrides.clear();
                lastOverride = null;
                blankIcon = null;
                tileLoader = new TileLoader("textures/blocks", logger);

                if (enableStandard || enableNonStandard) {
                    for (ResourceLocation resource : ResourceList.getInstance().listResources(TexturePackAPI.MCPATCHER_SUBDIR + "ctm", ".properties", true)) {
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

    public static void setCullFace(int cullFace) {
        haveCullFace = true;
        currentCullFace = cullFace;
    }

    private static void clearCullFace() {
        haveCullFace = false;
    }

    public static Icon getBlockIcon(Icon icon, RenderBlocks renderBlocks, Block block, IBlockAccess blockAccess, int i, int j, int k, int face) {
        lastOverride = null;
        if (checkFace(face)) {
            if (haveCullFace) {
                blockOrientation.setup(block, blockAccess, i, j, k, currentCullFace, face);
            } else {
                blockOrientation.setup(block, blockAccess, i, j, k, face);
            }
            lastOverride = ijkIterator.go(blockOrientation, icon);
            if (lastOverride != null) {
                icon = ijkIterator.getIcon();
            }
        }
        clearCullFace();
        return lastOverride == null && skipDefaultRendering(block) ? blankIcon : icon;
    }

    public static Icon getBlockIcon(Icon icon, RenderBlocks renderBlocks, Block block, int face, int metadata) {
        lastOverride = null;
        if (checkFace(face) && checkRenderType(block)) {
            blockOrientation.setup(block, metadata, face);
            lastOverride = metadataIterator.go(blockOrientation, icon);
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
