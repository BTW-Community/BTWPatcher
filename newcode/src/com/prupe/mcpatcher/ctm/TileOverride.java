package com.prupe.mcpatcher.ctm;

import com.prupe.mcpatcher.MCLogger;
import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.TexturePackAPI;
import com.prupe.mcpatcher.TileLoader;
import com.prupe.mcpatcher.mal.biome.BiomeAPI;
import com.prupe.mcpatcher.mal.block.BlockAPI;
import com.prupe.mcpatcher.mal.block.BlockAndMetadata;
import com.prupe.mcpatcher.mal.block.RenderPassAPI;
import net.minecraft.src.Block;
import net.minecraft.src.IBlockAccess;
import net.minecraft.src.Icon;
import net.minecraft.src.ResourceLocation;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.prupe.mcpatcher.ctm.BlockOrientation.*;

abstract class TileOverride implements ITileOverride {
    private static final MCLogger logger = MCLogger.getLogger(MCPatcherUtils.CONNECTED_TEXTURES, "CTM");

    private static final int META_MASK = 0xffff;

    private static final int[][] NORMALS = new int[][]{
        GO_DOWN,
        GO_UP,
        GO_NORTH,
        GO_SOUTH,
        GO_WEST,
        GO_EAST,
    };

    private static final int CONNECT_BY_BLOCK = 0;
    private static final int CONNECT_BY_TILE = 1;
    private static final int CONNECT_BY_MATERIAL = 2;

    private final ResourceLocation propertiesFile;
    private final String baseFilename;
    private final TileLoader tileLoader;
    private final int renderPass;
    private final int weight;
    private final Map<Block, Integer> matchBlocks;
    private final Set<String> matchTiles;
    private final int defaultMetaMask;
    private final int faces;
    private final int connectType;
    private final boolean innerSeams;
    private final BitSet biomes;
    private final BitSet height;

    private final List<ResourceLocation> tileNames = new ArrayList<ResourceLocation>();
    protected Icon[] icons;
    private boolean disabled;
    private int warnCount;
    private int matchMetadata;

    static TileOverride create(ResourceLocation propertiesFile, TileLoader tileLoader) {
        if (propertiesFile == null) {
            return null;
        }
        Properties properties = TexturePackAPI.getProperties(propertiesFile);
        if (properties == null) {
            return null;
        }

        String method = properties.getProperty("method", "default").trim().toLowerCase();
        TileOverride override = null;

        if (method.equals("default") || method.equals("glass") || method.equals("ctm")) {
            override = new TileOverrideImpl.CTM(propertiesFile, properties, tileLoader);
        } else if (method.equals("random")) {
            override = new TileOverrideImpl.Random1(propertiesFile, properties, tileLoader);
            if (override.getNumberOfTiles() == 1) {
                override = new TileOverrideImpl.Fixed(propertiesFile, properties, tileLoader);
            }
        } else if (method.equals("fixed") || method.equals("static")) {
            override = new TileOverrideImpl.Fixed(propertiesFile, properties, tileLoader);
        } else if (method.equals("bookshelf") || method.equals("horizontal")) {
            override = new TileOverrideImpl.Horizontal(propertiesFile, properties, tileLoader);
        } else if (method.equals("horizontal+vertical") || method.equals("h+v")) {
            override = new TileOverrideImpl.HorizontalVertical(propertiesFile, properties, tileLoader);
        } else if (method.equals("vertical")) {
            override = new TileOverrideImpl.Vertical(propertiesFile, properties, tileLoader);
        } else if (method.equals("vertical+horizontal") || method.equals("v+h")) {
            override = new TileOverrideImpl.VerticalHorizontal(propertiesFile, properties, tileLoader);
        } else if (method.equals("sandstone") || method.equals("top")) {
            override = new TileOverrideImpl.Top(propertiesFile, properties, tileLoader);
        } else if (method.equals("repeat") || method.equals("pattern")) {
            override = new TileOverrideImpl.Repeat(propertiesFile, properties, tileLoader);
        } else {
            logger.error("%s: unknown method \"%s\"", propertiesFile, method);
        }

        if (override != null && !override.disabled) {
            String status = override.checkTileMap();
            if (status != null) {
                override.error("invalid %s tile map: %s", override.getMethod(), status);
            }
        }

        return override == null || override.disabled ? null : override;
    }

    protected TileOverride(ResourceLocation propertiesFile, Properties properties, TileLoader tileLoader) {
        this.propertiesFile = propertiesFile;
        String texturesDirectory = propertiesFile.getPath().replaceFirst("/[^/]*$", "");
        baseFilename = propertiesFile.getPath().replaceFirst(".*/", "").replaceFirst("\\.properties$", "");
        this.tileLoader = tileLoader;

        String renderPassStr = MCPatcherUtils.getStringProperty(properties, "renderPass", "");
        renderPass = RenderPassAPI.instance.parseRenderPass(renderPassStr);
        if (renderPassStr.matches("\\d+") && renderPass >= 0 && renderPass <= RenderPassAPI.MAX_EXTRA_RENDER_PASS) {
            warn("renderPass=%s is deprecated, use renderPass=%s instead",
                renderPassStr, RenderPassAPI.instance.getRenderPassName(renderPass)
            );
        }

        loadIcons(properties);
        if (tileNames.isEmpty()) {
            error("no images found in %s/", texturesDirectory);
        }

        String value;
        if (baseFilename.matches("block\\d+.*")) {
            value = baseFilename.replaceFirst("block(\\d+).*", "$1");
        } else {
            value = "";
        }
        matchBlocks = getBlockList(
            MCPatcherUtils.getStringProperty(properties, "matchBlocks", value),
            MCPatcherUtils.getStringProperty(properties, "metadata", "")
        );
        matchTiles = getTileList(properties, "matchTiles");
        if (matchBlocks.isEmpty() && matchTiles.isEmpty()) {
            matchTiles.add(baseFilename);
        }
        int bits = 0;
        for (int i : MCPatcherUtils.parseIntegerList(MCPatcherUtils.getStringProperty(properties, "metadata", "0-15"), 0, 15)) {
            bits |= 1 << i;
        }
        defaultMetaMask = bits;

        int flags = 0;
        for (String val : properties.getProperty("faces", "all").trim().toLowerCase().split("\\s+")) {
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

        String connectType1 = properties.getProperty("connect", "").trim().toLowerCase();
        if (connectType1.equals("")) {
            connectType = matchTiles.isEmpty() ? CONNECT_BY_BLOCK : CONNECT_BY_TILE;
        } else if (connectType1.equals("block")) {
            connectType = CONNECT_BY_BLOCK;
        } else if (connectType1.equals("tile")) {
            connectType = CONNECT_BY_TILE;
        } else if (connectType1.equals("material")) {
            connectType = CONNECT_BY_MATERIAL;
        } else {
            error("invalid connect type %s", connectType1);
            connectType = CONNECT_BY_BLOCK;
        }

        innerSeams = MCPatcherUtils.getBooleanProperty(properties, "innerSeams", false);

        String biomeList = properties.getProperty("biomes", "");
        if (biomeList.isEmpty()) {
            biomes = null;
        } else {
            biomes = new BitSet();
            BiomeAPI.parseBiomeList(biomeList, biomes);
        }

        height = BiomeAPI.getHeightListProperty(properties, "");

        if (renderPass > RenderPassAPI.MAX_EXTRA_RENDER_PASS) {
            error("invalid renderPass %s", renderPassStr);
        } else if (renderPass >= 0 && !matchTiles.isEmpty()) {
            error("renderPass=%s must be block-based not tile-based", RenderPassAPI.instance.getRenderPassName(renderPass));
        }

        weight = MCPatcherUtils.getIntProperty(properties, "weight", 0);
    }

    private boolean addIcon(ResourceLocation resource) {
        tileNames.add(resource);
        return tileLoader.preloadTile(resource, renderPass > RenderPassAPI.MAX_BASE_RENDER_PASS);
    }

    private void loadIcons(Properties properties) {
        tileNames.clear();
        String tileList = properties.getProperty("tiles", "").trim();
        ResourceLocation blankResource = RenderPassAPI.instance.getBlankResource(renderPass);
        if (tileList.equals("")) {
            for (int i = 0; ; i++) {
                ResourceLocation resource = TileLoader.parseTileAddress(propertiesFile, String.valueOf(i), blankResource);
                if (!TexturePackAPI.hasResource(resource)) {
                    break;
                }
                if (!addIcon(resource)) {
                    break;
                }
            }
        } else {
            Pattern range = Pattern.compile("(\\d+)-(\\d+)");
            for (String token : tileList.split("\\s+")) {
                Matcher matcher = range.matcher(token);
                if (token.equals("")) {
                    // nothing
                } else if (matcher.matches()) {
                    try {
                        int from = Integer.parseInt(matcher.group(1));
                        int to = Integer.parseInt(matcher.group(2));
                        for (int i = from; i <= to; i++) {
                            ResourceLocation resource = TileLoader.parseTileAddress(propertiesFile, String.valueOf(i), blankResource);
                            if (TexturePackAPI.hasResource(resource)) {
                                addIcon(resource);
                            } else {
                                warn("could not find image %s", resource);
                                tileNames.add(null);
                            }
                        }
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                } else {
                    ResourceLocation resource = TileLoader.parseTileAddress(propertiesFile, token, blankResource);
                    if (resource == null) {
                        tileNames.add(null);
                    } else if (TexturePackAPI.hasResource(resource)) {
                        addIcon(resource);
                    } else {
                        warn("could not find image %s", resource);
                        tileNames.add(null);
                    }
                }
            }
        }
    }

    private Map<Block, Integer> getBlockList(String property, String defaultMetadata) {
        Map<Block, BlockAndMetadata> blockMetas = new IdentityHashMap<Block, BlockAndMetadata>();
        for (String token : property.split("\\s+")) {
            if (token.equals("")) {
                // nothing
            } else if (token.matches("\\d+-\\d+")) {
                for (int id : MCPatcherUtils.parseIntegerList(token, 0, 65535)) {
                    BlockAndMetadata blockMeta = BlockAndMetadata.parse(String.valueOf(id), defaultMetadata);
                    if (blockMeta == null) {
                        warn("unknown block id %d", id);
                    } else {
                        BlockAndMetadata oldBlockMeta = blockMetas.get(blockMeta.getBlock());
                        blockMetas.put(blockMeta.getBlock(), blockMeta.combine(oldBlockMeta));
                    }
                }
            } else {
                BlockAndMetadata blockMeta = BlockAndMetadata.parse(token, defaultMetadata);
                if (blockMeta == null) {
                    warn("unknown block %s", token);
                } else {
                    BlockAndMetadata oldBlockMeta = blockMetas.get(blockMeta.getBlock());
                    blockMetas.put(blockMeta.getBlock(), blockMeta.combine(oldBlockMeta));
                }
            }
        }
        Map<Block, Integer> blocks = new IdentityHashMap<Block, Integer>();
        for (Map.Entry<Block, BlockAndMetadata> entry : blockMetas.entrySet()) {
            blocks.put(entry.getKey(), entry.getValue().getMetadataBits());
        }
        return blocks;
    }

    private Set<String> getTileList(Properties properties, String key) {
        Set<String> list = new HashSet<String>();
        String property = properties.getProperty(key, "");
        for (String token : property.split("\\s+")) {
            if (token.equals("")) {
                // nothing
            } else if (token.contains("/")) {
                if (!token.endsWith(".png")) {
                    token += ".png";
                }
                ResourceLocation resource = TexturePackAPI.parseResourceLocation(propertiesFile, token);
                if (resource != null) {
                    list.add(resource.toString());
                }
            } else {
                list.add(token);
            }
        }
        return list;
    }

    protected int getNumberOfTiles() {
        return tileNames.size();
    }

    String checkTileMap() {
        return null;
    }

    boolean requiresFace() {
        return false;
    }

    @Override
    public String toString() {
        return String.format("%s[%s] (%d tiles)", getMethod(), propertiesFile, getNumberOfTiles());
    }

    @Override
    public final void registerIcons() {
        icons = new Icon[tileNames.size()];
        for (int i = 0; i < icons.length; i++) {
            icons[i] = tileLoader.getIcon(tileNames.get(i));
        }
    }

    final void error(String format, Object... params) {
        if (propertiesFile != null) {
            logger.error(propertiesFile + ": " + format, params);
        }
        disabled = true;
    }

    final void warn(String format, Object... params) {
        if (propertiesFile != null && warnCount < 10) {
            logger.warning(propertiesFile + ": " + format, params);
            warnCount++;
        }
    }

    @Override
    final public boolean isDisabled() {
        return disabled;
    }

    @Override
    final public Set<Block> getMatchingBlocks() {
        return matchBlocks.keySet();
    }

    @Override
    final public Set<String> getMatchingTiles() {
        return matchTiles;
    }

    @Override
    final public int getRenderPass() {
        return renderPass;
    }

    @Override
    final public int getWeight() {
        return weight;
    }

    @Override
    public int compareTo(ITileOverride o) {
        int result = o.getWeight() - getWeight();
        if (result != 0) {
            return result;
        }
        if (o instanceof TileOverride) {
            return baseFilename.compareTo(((TileOverride) o).baseFilename);
        } else {
            return -1;
        }
    }

    final boolean shouldConnect(BlockOrientation blockOrientation, Icon icon, int relativeDirection) {
        return shouldConnect(blockOrientation, icon, blockOrientation.getOffset(relativeDirection));
    }

    final boolean shouldConnect(BlockOrientation blockOrientation, Icon icon, int blockFace, int relativeDirection) {
        return shouldConnect(blockOrientation, icon, blockOrientation.getOffset(blockFace, relativeDirection));
    }

    private boolean shouldConnect(BlockOrientation blockOrientation, Icon icon, int[] offset) {
        IBlockAccess blockAccess = blockOrientation.blockAccess;
        Block block = blockOrientation.block;
        int i = blockOrientation.i;
        int j = blockOrientation.j;
        int k = blockOrientation.k;
        int metadata = blockOrientation.metadata;
        i += offset[0];
        j += offset[1];
        k += offset[2];
        int neighborMeta = BlockAPI.getMetadataAt(blockAccess, i, j, k);
        Block neighbor = BlockAPI.getBlockAt(blockAccess, i, j, k);
        if (neighbor == null) {
            return false;
        }
        if (block == neighbor && matchMetadata != META_MASK && metadata != neighborMeta) {
            return false;
        }
        int blockFace = blockOrientation.blockFace;
        if (blockFace >= 0 && innerSeams) {
            int[] normal = NORMALS[blockFace];
            if (!BlockAPI.shouldSideBeRendered(neighbor, blockAccess, i + normal[0], j + normal[1], k + normal[2], blockFace)) {
                return false;
            }
        }
        switch (connectType) {
            case CONNECT_BY_BLOCK:
                return neighbor == block;

            case CONNECT_BY_TILE:
                return BlockAPI.getBlockIcon(neighbor, blockAccess, i, j, k, blockOrientation.textureFaceOrig) == icon;

            case CONNECT_BY_MATERIAL:
                return block.blockMaterial == neighbor.blockMaterial;

            default:
                return false;
        }
    }

    final boolean exclude(Block block, int face, int metadataMask) {
        if (block == null) {
            return true;
        } else if ((faces & (1 << face)) == 0) {
            return true;
        } else if (matchMetadata != META_MASK && (matchMetadata & metadataMask) == 0) {
            return true;
        }
        return false;
    }

    @Override
    public final Icon getTileWorld(BlockOrientation blockOrientation, Icon origIcon) {
        if (icons == null) {
            error("no images loaded, disabling");
            return null;
        }
        IBlockAccess blockAccess = blockOrientation.blockAccess;
        Block block = blockOrientation.block;
        int i = blockOrientation.i;
        int j = blockOrientation.j;
        int k = blockOrientation.k;
        if (blockOrientation.blockFace < 0 && requiresFace()) {
            warn("method=%s is not supported for non-standard block %s:%d @ %d %d %d",
                getMethod(), BlockAPI.getBlockName(block), BlockAPI.getMetadataAt(blockAccess, i, j, k), i, j, k
            );
            return null;
        }
        if (block == null || RenderPassAPI.instance.skipThisRenderPass(block, renderPass)) {
            return null;
        }
        Integer metadataEntry = matchBlocks.get(block);
        matchMetadata = metadataEntry == null ? META_MASK : metadataEntry;
        if (exclude(block, blockOrientation.textureFace, blockOrientation.metadataBits)) {
            return null;
        }
        if (height != null && !height.get(j)) {
            return null;
        }
        if (biomes != null && !biomes.get(BiomeAPI.getBiomeIDAt(blockAccess, i, j, k))) {
            return null;
        }
        return getTileWorld_Impl(blockOrientation, origIcon);
    }

    @Override
    public final Icon getTileHeld(BlockOrientation blockOrientation, Icon origIcon) {
        if (icons == null) {
            error("no images loaded, disabling");
            return null;
        }
        Block block = blockOrientation.block;
        if (block == null || RenderPassAPI.instance.skipThisRenderPass(block, renderPass)) {
            return null;
        }
        int face = blockOrientation.textureFace;
        if (face < 0 && requiresFace()) {
            warn("method=%s is not supported for non-standard block %s:%d",
                getMethod(), BlockAPI.getBlockName(block), blockOrientation.metadata
            );
            return null;
        }
        if (height != null || biomes != null) {
            return null;
        }
        Integer metadataEntry = matchBlocks.get(block);
        matchMetadata = metadataEntry == null ? defaultMetaMask : metadataEntry;
        if (exclude(block, face, blockOrientation.metadataBits)) {
            return null;
        } else {
            return getTileHeld_Impl(blockOrientation, origIcon);
        }
    }

    abstract String getMethod();

    abstract Icon getTileWorld_Impl(BlockOrientation blockOrientation, Icon origIcon);

    abstract Icon getTileHeld_Impl(BlockOrientation blockOrientation, Icon origIcon);
}
