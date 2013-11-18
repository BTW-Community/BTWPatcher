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

abstract class TileOverride implements ITileOverride {
    private static final MCLogger logger = MCLogger.getLogger(MCPatcherUtils.CONNECTED_TEXTURES, "CTM");

    static final int BOTTOM_FACE = 0; // 0, -1, 0
    static final int TOP_FACE = 1; // 0, 1, 0
    static final int NORTH_FACE = 2; // 0, 0, -1
    static final int SOUTH_FACE = 3; // 0, 0, 1
    static final int WEST_FACE = 4; // -1, 0, 0
    static final int EAST_FACE = 5; // 1, 0, 0

    static final int REL_L = 0;
    static final int REL_DL = 1;
    static final int REL_D = 2;
    static final int REL_DR = 3;
    static final int REL_R = 4;
    static final int REL_UR = 5;
    static final int REL_U = 6;
    static final int REL_UL = 7;

    private static final int META_MASK = 0xffff;
    private static final int ORIENTATION_U_D = 0;
    private static final int ORIENTATION_E_W = 1 << 16;
    private static final int ORIENTATION_N_S = 2 << 16;
    private static final int ORIENTATION_E_W_2 = 3 << 16;
    private static final int ORIENTATION_N_S_2 = 4 << 16;

    private static final int[][] ROTATE_UV_MAP = new int[][]{
        {WEST_FACE, EAST_FACE, NORTH_FACE, SOUTH_FACE, TOP_FACE, BOTTOM_FACE, 2, -2, 2, -2, 0, 0},
        {NORTH_FACE, SOUTH_FACE, TOP_FACE, BOTTOM_FACE, WEST_FACE, EAST_FACE, 0, 0, 0, 0, -2, 2},
        {WEST_FACE, EAST_FACE, NORTH_FACE, SOUTH_FACE, TOP_FACE, BOTTOM_FACE, 2, -2, -2, -2, 0, 0},
        {NORTH_FACE, SOUTH_FACE, TOP_FACE, BOTTOM_FACE, WEST_FACE, EAST_FACE, 0, 0, 0, 0, -2, -2},
    };

    private static final int[] GO_DOWN = new int[]{0, -1, 0};
    private static final int[] GO_UP = new int[]{0, 1, 0};
    private static final int[] GO_NORTH = new int[]{0, 0, -1};
    private static final int[] GO_SOUTH = new int[]{0, 0, 1};
    private static final int[] GO_WEST = new int[]{-1, 0, 0};
    private static final int[] GO_EAST = new int[]{1, 0, 0};

    private static final int[][] NORMALS = new int[][]{
        GO_DOWN,
        GO_UP,
        GO_NORTH,
        GO_SOUTH,
        GO_WEST,
        GO_EAST,
    };

    // NEIGHBOR_OFFSETS[a][b][c] = offset from starting block
    // a: face 0-5
    // b: neighbor 0-7
    //    7   6   5
    //    0   *   4
    //    1   2   3
    // c: coordinate (x,y,z) 0-2
    protected static final int[][][] NEIGHBOR_OFFSET = new int[][][]{
        // BOTTOM_FACE
        {
            GO_WEST,
            add(GO_WEST, GO_SOUTH),
            GO_SOUTH,
            add(GO_EAST, GO_SOUTH),
            GO_EAST,
            add(GO_EAST, GO_NORTH),
            GO_NORTH,
            add(GO_WEST, GO_NORTH),
        },
        // TOP_FACE
        {
            GO_WEST,
            add(GO_WEST, GO_SOUTH),
            GO_SOUTH,
            add(GO_EAST, GO_SOUTH),
            GO_EAST,
            add(GO_EAST, GO_NORTH),
            GO_NORTH,
            add(GO_WEST, GO_NORTH),
        },
        // NORTH_FACE
        {
            GO_EAST,
            add(GO_EAST, GO_DOWN),
            GO_DOWN,
            add(GO_WEST, GO_DOWN),
            GO_WEST,
            add(GO_WEST, GO_UP),
            GO_UP,
            add(GO_EAST, GO_UP),
        },
        // SOUTH_FACE
        {
            GO_WEST,
            add(GO_WEST, GO_DOWN),
            GO_DOWN,
            add(GO_EAST, GO_DOWN),
            GO_EAST,
            add(GO_EAST, GO_UP),
            GO_UP,
            add(GO_WEST, GO_UP),
        },
        // WEST_FACE
        {
            GO_NORTH,
            add(GO_NORTH, GO_DOWN),
            GO_DOWN,
            add(GO_SOUTH, GO_DOWN),
            GO_SOUTH,
            add(GO_SOUTH, GO_UP),
            GO_UP,
            add(GO_NORTH, GO_UP),
        },
        // EAST_FACE
        {
            GO_SOUTH,
            add(GO_SOUTH, GO_DOWN),
            GO_DOWN,
            add(GO_NORTH, GO_DOWN),
            GO_NORTH,
            add(GO_NORTH, GO_UP),
            GO_UP,
            add(GO_SOUTH, GO_UP),
        },
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
    private final int faces;
    private final int connectType;
    private final boolean innerSeams;
    private final BitSet biomes;
    private final int minHeight;
    private final int maxHeight;

    private final List<ResourceLocation> tileNames = new ArrayList<ResourceLocation>();
    protected Icon[] icons;
    private boolean disabled;
    private int warnCount;
    private int matchMetadata;
    private int[] reorient;
    private int rotateUV;
    protected boolean rotateTop;

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

        minHeight = MCPatcherUtils.getIntProperty(properties, "minHeight", -1);
        maxHeight = MCPatcherUtils.getIntProperty(properties, "maxHeight", Integer.MAX_VALUE);

        renderPass = MCPatcherUtils.getIntProperty(properties, "renderPass", -1);
        if (renderPass > 3) {
            error("renderPass must be 0-3");
        } else if (renderPass >= 0 && !matchTiles.isEmpty()) {
            error("renderPass=%d must be block-based not tile-based", renderPass);
        }

        weight = MCPatcherUtils.getIntProperty(properties, "weight", 0);
    }

    private boolean addIcon(ResourceLocation resource) {
        tileNames.add(resource);
        return tileLoader.preloadTile(resource, renderPass > 2);
    }

    private void loadIcons(Properties properties) {
        tileNames.clear();
        String tileList = properties.getProperty("tiles", "").trim();
        if (tileList.equals("")) {
            for (int i = 0; ; i++) {
                ResourceLocation resource = TileLoader.parseTileAddress(propertiesFile, String.valueOf(i));
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
                            ResourceLocation resource = TileLoader.parseTileAddress(propertiesFile, String.valueOf(i));
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
                    ResourceLocation resource = TileLoader.parseTileAddress(propertiesFile, token);
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

    private static int[] add(int[] a, int[] b) {
        if (a.length != b.length) {
            throw new RuntimeException("arrays to add are not same length");
        }
        int[] c = new int[a.length];
        for (int i = 0; i < c.length; i++) {
            c[i] = a[i] + b[i];
        }
        return c;
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

    final public boolean isDisabled() {
        return disabled;
    }

    final public Set<Block> getMatchingBlocks() {
        return matchBlocks.keySet();
    }

    final public Set<String> getMatchingTiles() {
        return matchTiles;
    }

    final public int getRenderPass() {
        return renderPass;
    }

    final public int getWeight() {
        return weight;
    }

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

    final boolean shouldConnect(IBlockAccess blockAccess, Block block, Icon icon, int i, int j, int k, int face, int[] offset) {
        int metadata = blockAccess.getBlockMetadata(i, j, k);
        i += offset[0];
        j += offset[1];
        k += offset[2];
        int neighborMeta = blockAccess.getBlockMetadata(i, j, k);
        Block neighbor = BlockAPI.getBlockAt(blockAccess, i, j, k);
        if (exclude(neighbor, face, neighborMeta)) {
            return false;
        }
        int orientation = getOrientationFromMetadata(block, metadata);
        int neighborOrientation = getOrientationFromMetadata(neighbor, neighborMeta);
        if ((orientation & ~META_MASK) != (neighborOrientation & ~META_MASK)) {
            return false;
        }
        if (matchMetadata != META_MASK) {
            if ((orientation & META_MASK) != (neighborOrientation & META_MASK)) {
                return false;
            }
        }
        if (face >= 0 && innerSeams) {
            int[] normal = NORMALS[face];
            if (!neighbor.shouldSideBeRendered(blockAccess, i + normal[0], j + normal[1], k + normal[2], face)) {
                return false;
            }
        }
        switch (connectType) {
            case CONNECT_BY_BLOCK:
                return neighbor == block;

            case CONNECT_BY_TILE:
                return neighbor.getBlockIcon(blockAccess, i, j, k, face) == icon;

            case CONNECT_BY_MATERIAL:
                return block.blockMaterial == neighbor.blockMaterial;

            default:
                return false;
        }
    }

    final int reorient(int face) {
        if (face < 0 || face > 5 || reorient == null) {
            return face;
        } else {
            return reorient[face];
        }
    }

    final int rotateUV(int neighbor) {
        return (neighbor + rotateUV) & 7;
    }

    final boolean exclude(Block block, int face, int metadata) {
        if (block == null) {
            return true;
        } else if ((faces & (1 << reorient(face))) == 0) {
            return true;
        }
        if (matchMetadata != META_MASK && metadata >= 0 && metadata <= BlockAndMetadata.MAX_METADATA) {
            int altMetadata = getOrientationFromMetadata(block, metadata) & META_MASK;
            if ((matchMetadata & ((1 << metadata) | (1 << altMetadata))) == 0) {
                return true;
            }
        }
        return false;
    }

    private static int getOrientationFromMetadata(Block block, int metadata) {
        int newMeta = metadata;
        int orientation = ORIENTATION_U_D;

        switch (block.getRenderType()) {
            case 31: // renderBlockLog (also applies to hay)
                switch (metadata & 0xc) {
                    case 0:
                        newMeta = metadata & ~0xc;
                        break;

                    case 4:
                        newMeta = metadata & ~0xc;
                        orientation = ORIENTATION_E_W;
                        break;

                    case 8:
                        newMeta = metadata & ~0xc;
                        orientation = ORIENTATION_N_S;
                        break;

                    default:
                        break;
                }
                break;

            case 39: // renderBlockQuartz
                switch (metadata) {
                    case 3:
                        newMeta = 2;
                        orientation = ORIENTATION_E_W_2;
                        break;

                    case 4:
                        newMeta = 2;
                        orientation = ORIENTATION_N_S_2;
                        break;

                    default:
                        break;
                }
                break;

            default:
                break;
        }

        return orientation | newMeta;
    }

    private void setupOrientation(int orientation, int face) {
        switch (orientation & ~META_MASK) {
            case ORIENTATION_E_W:
                reorient = ROTATE_UV_MAP[0];
                rotateUV = ROTATE_UV_MAP[0][face + 6];
                rotateTop = true;
                break;

            case ORIENTATION_N_S:
                reorient = ROTATE_UV_MAP[1];
                rotateUV = ROTATE_UV_MAP[1][face + 6];
                rotateTop = false;
                break;

            case ORIENTATION_E_W_2:
                reorient = ROTATE_UV_MAP[2];
                rotateUV = ROTATE_UV_MAP[2][face + 6];
                rotateTop = true;
                break;

            case ORIENTATION_N_S_2:
                reorient = ROTATE_UV_MAP[3];
                rotateUV = ROTATE_UV_MAP[3][face + 6];
                rotateTop = false;
                break;

            default:
                reorient = null;
                rotateUV = 0;
                rotateTop = false;
                break;
        }
    }

    private static int remapFaceByRenderType(Block block, int metadata, int face) {
        switch (block.getRenderType()) {
            case 20: // renderBlockVine
                switch (metadata) {
                    case 1:
                        return NORTH_FACE;

                    case 2:
                        return EAST_FACE;

                    case 4:
                        return SOUTH_FACE;

                    case 8:
                        return WEST_FACE;

                    default:
                        break;
                }
                break;

            case 8: // renderBlockLadder
                switch (metadata) {
                    case 2:
                    case 3:
                    case 4:
                    case 5:
                        return metadata;

                    default:
                        break;
                }
                break;

            default:
                break;
        }
        return face;
    }

    public final Icon getTile(IBlockAccess blockAccess, Block block, Icon origIcon, int i, int j, int k, int face) {
        if (icons == null) {
            error("no images loaded, disabling");
            return null;
        }
        if (face < 0 && requiresFace()) {
            warn("method=%s is not supported for non-standard block %s:%d @ %d %d %d",
                getMethod(), BlockAPI.getBlockName(block), blockAccess.getBlockMetadata(i, j, k), i, j, k
            );
            return null;
        }
        if (block == null || RenderPassAPI.instance.skipThisRenderPass(block, renderPass)) {
            return null;
        }
        Integer metadataEntry = matchBlocks.get(block);
        matchMetadata = metadataEntry == null ? META_MASK : metadataEntry;
        int metadata = blockAccess.getBlockMetadata(i, j, k);
        setupOrientation(getOrientationFromMetadata(block, metadata), face);
        face = remapFaceByRenderType(block, metadata, face);
        if (exclude(block, face, metadata)) {
            return null;
        }
        if (j < minHeight || j > maxHeight) {
            return null;
        }
        if (biomes != null && !biomes.get(BiomeAPI.getBiomeIDAt(i, j, k))) {
            return null;
        }
        return getTileImpl(blockAccess, block, origIcon, i, j, k, face);
    }

    public final Icon getTile(Block block, Icon origIcon, int face, int metadata) {
        if (icons == null) {
            error("no images loaded, disabling");
            return null;
        }
        if (face < 0 && requiresFace()) {
            warn("method=%s is not supported for non-standard block %s:%d",
                getMethod(), BlockAPI.getBlockName(block), metadata
            );
            return null;
        }
        if (minHeight >= 0 || maxHeight < Integer.MAX_VALUE || biomes != null) {
            return null;
        }
        Integer metadataEntry = matchBlocks.get(block);
        matchMetadata = metadataEntry == null ? META_MASK : metadataEntry;
        setupOrientation(getOrientationFromMetadata(block, metadata), face);
        if (exclude(block, face, metadata)) {
            return null;
        } else {
            return getTileImpl(block, origIcon, face, metadata);
        }
    }

    abstract String getMethod();

    abstract Icon getTileImpl(IBlockAccess blockAccess, Block block, Icon origIcon, int i, int j, int k, int face);

    abstract Icon getTileImpl(Block block, Icon origIcon, int face, int metadata);
}
