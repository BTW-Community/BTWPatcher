package com.prupe.mcpatcher.ctm;

import com.prupe.mcpatcher.MCLogger;
import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.cc.ColorizeBlock18;
import com.prupe.mcpatcher.mal.block.BlockAPI;
import com.prupe.mcpatcher.mal.tile.FaceInfo;
import net.minecraft.src.*;

import java.util.*;
import java.util.logging.Level;

public class CTMUtils18 extends RenderBlockState {
    private static final MCLogger logger = MCLogger.getLogger(MCPatcherUtils.CONNECTED_TEXTURES);

    private static final int[][][] NEIGHBOR_OFFSET = new int[][][]{
        makeNeighborOffset(WEST_FACE, NORTH_FACE, EAST_FACE, SOUTH_FACE), // BOTTOM_FACE - flipped n-s in 1.8 custom models
        makeNeighborOffset(WEST_FACE, SOUTH_FACE, EAST_FACE, NORTH_FACE), // TOP_FACE
        makeNeighborOffset(EAST_FACE, BOTTOM_FACE, WEST_FACE, TOP_FACE), // NORTH_FACE
        makeNeighborOffset(WEST_FACE, BOTTOM_FACE, EAST_FACE, TOP_FACE), // SOUTH_FACE
        makeNeighborOffset(NORTH_FACE, BOTTOM_FACE, SOUTH_FACE, TOP_FACE), // WEST_FACE
        makeNeighborOffset(SOUTH_FACE, BOTTOM_FACE, NORTH_FACE, TOP_FACE), // EAST_FACE
    };

    private static final ThreadLocal<CTMUtils18> instances = new ThreadLocal<CTMUtils18>();

    private static final Set<IBlockStateProperty> ignoredProperties = new HashSet<IBlockStateProperty>();
    private static final Map<Block, IBlockStateProperty> halfProperties = new IdentityHashMap<Block, IBlockStateProperty>();
    private static Block bedBlock;
    private static IBlockStateProperty bedHeadProperty;
    private static IBlockStateProperty bedFacingProperty;

    private IModel model;
    private IBlockState blockState;
    private Position position;
    private Direction direction;
    private int effectiveFace;
    private int uvRotation;
    private int hvFace;
    private String textureFaceName;

    private final TileOverrideIterator.IJK ijkIterator = CTMUtils.newIJKIterator();

    private final ColorizeBlock18 colorizeBlock;

    static void reset() {
        ignoredProperties.clear();
        halfProperties.clear();
        bedBlock = BlockAPI.getFixedBlock("minecraft:bed");
        bedHeadProperty = getPropertyByName(bedBlock, "part");
        bedFacingProperty = getPropertyByName(bedBlock, "facing");

        for (Block block : BlockAPI.getAllBlocks()) {
            logger.config("Block %s", BlockAPI.getBlockName(block));
            IBlockState state = block.getBlockState();
            for (IBlockStateProperty property : state.getProperties()) {
                String name = property.getName();
                if (name.equals("half") || name.equals("part")) {
                    ignoredProperties.add(property);
                    if (name.equals("half")) {
                        halfProperties.put(block, property);
                    }
                }
                if (logger.isLoggable(Level.CONFIG)) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("  ").append(name).append(" (").append(property.getValueClass().getName()).append("):");
                    for (Comparable value : property.getValues()) {
                        sb.append(' ').append(value.toString());
                    }
                    logger.info("%s", sb.toString());
                }
            }
        }
    }

    private static IBlockStateProperty getPropertyByName(Block block, String propertyName) {
        if (block != null) {
            for (IBlockStateProperty property : block.getBlockState().getProperties()) {
                if (propertyName.equals(property.getName())) {
                    return property;
                }
            }
        }
        return null;
    }

    private static boolean comparePropertyValues(Comparable a, Comparable b) {
        if (a == b) {
            return true;
        } else if (a == null) {
            return false;
        } else {
            return a.equals(b);
        }
    }

    public static CTMUtils18 getInstance() {
        CTMUtils18 instance = instances.get();
        if (instance == null) {
            instance = new CTMUtils18();
            instances.set(instance);
        }
        return instance;
    }

    private CTMUtils18() {
        logger.info("new CTMUtils18() for %s", Thread.currentThread());
        colorizeBlock = new ColorizeBlock18(this);
    }

    public boolean preRender(IBlockAccess blockAccess, IModel model, IBlockState blockState, Position position, Block block, boolean useAO) {
        this.blockAccess = blockAccess;
        this.model = model;
        this.blockState = blockState;
        this.position = position;
        this.block = block;
        this.useAO = useAO;
        direction = null;
        inWorld = true;
        offsetsComputed = false;
        haveOffsets = false;
        di = dj = dk = 0;

        colorizeBlock.preRender(blockAccess, model, blockState, position, block, useAO);

        return true;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
        colorizeBlock.setDirection(direction);
    }

    public void setDirectionWater(Direction direction) {
        this.direction = direction;
        colorizeBlock.setDirectionWater(direction);
    }

    public boolean useColormap(ModelFace face) {
        return colorizeBlock.useColormap(face);
    }

    public int colorMultiplier(int color) {
        return colorizeBlock.colorMultiplier(color);
    }

    public float getVertexColor(float color, int vertex, int channel) {
        return colorizeBlock.getVertexColor(color, vertex, channel);
    }

    public void applyVertexColor(Tessellator tessellator, float base, int vertex) {
        colorizeBlock.applyVertexColor(tessellator, base, vertex);
    }

    public ModelFace getModelFace(ModelFace origFace) {
        FaceInfo faceInfo = FaceInfo.getFaceInfo(origFace);
        if (faceInfo == null) {
            textureFaceName = null;
            return origFace;
        }
        TextureAtlasSprite origIcon = faceInfo.getSprite();
        textureFaceName = faceInfo.getTextureName();
        setUVFace(faceInfo);
        ijkIterator.go(this, origIcon);
        TextureAtlasSprite newIcon = (TextureAtlasSprite) ijkIterator.getIcon();
        return faceInfo.getAltFace(newIcon);
    }

    private void setUVFace(FaceInfo faceInfo) {
        effectiveFace = faceInfo.getEffectiveFace();
        uvRotation = faceInfo.getUVRotation();
        hvFace = effectiveFace >= 0 ? effectiveFace : RenderBlockState.NORTH_FACE;
    }

    @Override
    public int getI() {
        return position.getI();
    }

    @Override
    public int getJ() {
        return position.getJ();
    }

    @Override
    public int getK() {
        return position.getK();
    }

    @Override
    public int getBlockFace() {
        return effectiveFace;
    }

    @Override
    public int getTextureFace() {
        return direction == null ? -1 : direction.ordinal();
    }

    @Override
    public int getTextureFaceOrig() {
        return getTextureFace();
    }

    @Override
    public String getTextureFaceName() {
        return textureFaceName;
    }

    @Override
    public int getFaceForHV() {
        return hvFace;
    }

    @Override
    public int[] getOffset(int blockFace, int relativeDirection) {
        return NEIGHBOR_OFFSET[blockFace][(relativeDirection + uvRotation) & 7];
    }

    @Override
    public boolean setCoordOffsetsForRenderType() {
        if (!offsetsComputed) {
            offsetsComputed = true;
            if (block == bedBlock) {
                if (getEnumProperty(bedHeadProperty) > 0) { // part=foot
                    switch (getEnumProperty(bedFacingProperty)) {
                        case 0:
                            dk = 1; // head is one block south
                            break;

                        case 1:
                            di = -1; // head is one block west
                            break;

                        case 2:
                            dk = -1; // head is one block north
                            break;

                        case 3:
                            di = 1; // head is one block east
                            break;

                        default:
                            return false;
                    }
                    haveOffsets = true;
                }
            } else if (getEnumProperty(halfProperties.get(block)) > 0) { // half=lower
                di = 1;
                haveOffsets = true;
            }
        }
        return haveOffsets;
    }

    private int getEnumProperty(IBlockStateProperty property) {
        if (property != null) {
            Comparable value = blockState.getProperty(property);
            if (value instanceof Enum) {
                return ((Enum) value).ordinal();
            }
        }
        return -1;
    }

    @Override
    public boolean shouldConnectByBlock(Block neighbor, int neighborI, int neighborJ, int neighborK) {
        if (block != neighbor) {
            return false;
        }
        IBlockState neighborState = blockAccess.getBlockState(new Position(neighborI, neighborJ, neighborK));
        for (IBlockStateProperty property : blockState.getProperties()) {
            if (ignoredProperties.contains(property)) {
                continue;
            }
            if (!neighborState.getProperties().contains(property)) {
                return false;
            }
            Comparable value = blockState.getProperty(property);
            Comparable neighborValue = neighborState.getProperty(property);
            if (!comparePropertyValues(value, neighborValue)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean shouldConnectByTile(Block neighbor, Icon origIcon, int neighborI, int neighborJ, int neighborK) {
        Position neighborPosition = new Position(neighborI, neighborJ, neighborK);
        IBlockState neighborState = blockAccess.getBlockState(neighborPosition);
        IModel neighborModel = Minecraft.getInstance().getRenderBlockDispatcher().getModel(neighborState, blockAccess, neighborPosition);
        List<ModelFace> neighborFaces = direction == null ? neighborModel.getDefaultFaces() : neighborModel.getFaces(direction);
        if (!neighborFaces.isEmpty()) {
            for (ModelFace neighborFace : neighborFaces) {
                if (origIcon == FaceInfo.getFaceInfo(neighborFace).getSprite()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("CTMUtils18{");
        if (block != null) {
            sb.append(BlockAPI.getBlockName(block)).append(" ");
        }
        if (position != null) {
            sb.append(" @").append(position.getI()).append(',').append(position.getJ()).append(',').append(position.getK());
        }
        if (direction != null) {
            sb.append(' ').append(direction.toString());
        }
        return sb.append('}').toString();
    }
}
