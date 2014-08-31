package com.prupe.mcpatcher.cc;

import com.prupe.mcpatcher.MCLogger;
import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.ctm.CTMUtils;
import com.prupe.mcpatcher.ctm.TileOverrideIterator;
import com.prupe.mcpatcher.mal.biome.ColorUtils;
import com.prupe.mcpatcher.mal.biome.IColorMap;
import com.prupe.mcpatcher.mal.block.BlockAPI;
import com.prupe.mcpatcher.mal.block.BlockStateMatcher;
import com.prupe.mcpatcher.mal.block.RenderBlockState;
import com.prupe.mcpatcher.mal.block.RenderPassAPI;
import com.prupe.mcpatcher.mal.resource.PropertiesFile;
import com.prupe.mcpatcher.mal.resource.TexturePackAPI;
import com.prupe.mcpatcher.mal.resource.TexturePackChangeHandler;
import net.minecraft.src.*;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class ColorizeBlock18 extends RenderBlockState {
    private static final MCLogger logger = MCLogger.getLogger(MCPatcherUtils.CUSTOM_COLORS);

    private static final ResourceLocation COLOR_PROPERTIES = TexturePackAPI.newMCPatcherResourceLocation("color.properties");

    private static final ThreadLocal<ColorizeBlock18> instances = new ThreadLocal<ColorizeBlock18>();

    private static Block grassBlock;
    private static Block mycelBlock;

    private IModel model;
    private IBlockState blockState;
    private Position position;
    private Direction direction;

    private boolean useCM;
    private IColorMap colorMap;
    private boolean isSmooth;
    private final float[][] vertexColors = new float[4][3];

    private final TileOverrideIterator.IJK ijkIterator = CTMUtils.newIJKIterator();
    private final TileOverrideIterator.Metadata metadataIterator = CTMUtils.newMetadataIterator();

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
            {0, 1, 0},
            {0, 1, 1},
            {1, 1, 1},
            {1, 1, 0},
        },
        // north face (z=0)
        {
            {1, 1, 0},
            {1, 0, 0},
            {0, 0, 0},
            {0, 1, 0},
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
            {0, 1, 0},
            {0, 0, 0},
            {0, 0, 1},
            {0, 1, 1},
        },
        // east face (x=1)
        {
            {1, 1, 1},
            {1, 0, 1},
            {1, 0, 0},
            {1, 1, 0},
        }
    };

    private static final int[][][] FACE_VERTICES_WATER = new int[][][]{
        // bottom face (y=0)
        {
            {0, 0, 1}, // top left
            {0, 0, 0}, // bottom left
            {1, 0, 0}, // bottom right
            {1, 0, 1}, // top right
        },
        // top face (y=1)
        {
            {0, 1, 0},
            {0, 1, 1},
            {1, 1, 1},
            {1, 1, 0},
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
            {1, 1, 1},
            {0, 1, 1},
            {0, 0, 1},
            {1, 0, 1},
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
            {1, 1, 0},
            {1, 1, 1},
            {1, 0, 1},
            {1, 0, 0},
        }
    };

    static {
        TexturePackChangeHandler.register(new TexturePackChangeHandler(MCPatcherUtils.CUSTOM_COLORS, 2) {
            @Override
            public void beforeChange() {
                reset();
            }

            @Override
            public void afterChange() {
                PropertiesFile properties = PropertiesFile.getNonNull(logger, COLOR_PROPERTIES);
                ColorizeBlock.reloadAll(properties);
            }
        });
    }

    public static void reset() {
        try {
            grassBlock = BlockAPI.getFixedBlock("minecraft:grass");
            mycelBlock = BlockAPI.getFixedBlock("minecraft:mycelium");
            alt.clear();
            ColorizeBlock.reset();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static ColorizeBlock18 getInstance() {
        ColorizeBlock18 instance = instances.get();
        if (instance == null) {
            instance = new ColorizeBlock18();
            instances.set(instance);
        }
        return instance;
    }

    private ColorizeBlock18() {
        logger.info("new ColorizeBlock18() for %s", Thread.currentThread());
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

        colorMap = null;
        useCM = RenderPassAPI.instance.useColorMultiplierThisPass(block);
        if (useCM) {
            List<BlockStateMatcher> maps = ColorizeBlock.findColorMaps(block);
            if (maps != null) {
                for (BlockStateMatcher matcher : maps) {
                    if (matcher.matchBlockState(blockState)) {
                        colorMap = ColorizeBlock.getThreadLocal(matcher);
                        break;
                    }
                }
            }
        }
        isSmooth = false;

        return true;
    }

    public void setDirection(Direction direction) {
        setDirection(direction, FACE_VERTICES);
    }

    public void setDirectionWater(Direction direction) {
        setDirection(direction, FACE_VERTICES_WATER);
    }

    private void setDirection(Direction direction, int[][][] faceVertices) {
        this.direction = direction;
        if (ColorizeBlock.enableSmoothBiomes && direction != null && colorMap != null) {
            isSmooth = true;
            int[][] offsets = faceVertices[direction.ordinal()];
            computeVertexColor(offsets[0], vertexColors[0]);
            computeVertexColor(offsets[1], vertexColors[1]);
            computeVertexColor(offsets[2], vertexColors[2]);
            computeVertexColor(offsets[3], vertexColors[3]);
        } else {
            isSmooth = false;
        }
    }

    private void computeVertexColor(int[] offsets, float[] color) {
        int i = position.getI() + offsets[0];
        int j = position.getJ() + offsets[1];
        int k = position.getK() + offsets[2];
        if (ColorizeBlock.enableTestColorSmoothing) {
            int rgb = 0;
            if (i % 2 == 0) {
                rgb |= 0xff0000;
            }
            if (j % 2 == 0) {
                rgb |= 0x00ff00;
            }
            if (k % 2 == 0) {
                rgb |= 0x0000ff;
            }
            ColorUtils.intToFloat3(rgb, color);
        } else {
            float[] tmp = colorMap.getColorMultiplierF(blockAccess, i, j, k);
            color[0] = tmp[0];
            color[1] = tmp[1];
            color[2] = tmp[2];
        }
    }

    public boolean useColormap(ModelFace face) {
        return useCM && (face.useColormap() || (colorMap != null && block != grassBlock && block != mycelBlock));
    }

    public int colorMultiplier(int color) {
        if (colorMap == null) {
            return color;
        } else {
            return colorMap.getColorMultiplier(blockAccess, position.getI(), position.getJ(), position.getK());
        }
    }

    public float getVertexColor(float color, int vertex, int channel) {
        if (isSmooth) {
            return vertexColors[vertex][channel];
        } else {
            return color;
        }
    }

    public void applyVertexColor(Tessellator tessellator, float base, int vertex) {
        if (isSmooth) {
            float[] rgb = vertexColors[vertex];
            tessellator.setColorOpaque_F(base * rgb[0], base * rgb[1], base * rgb[2]);
        }
    }

    private static final Map<ModelFace, TextureAtlasSprite> modelFaceSprites = new IdentityHashMap<ModelFace, TextureAtlasSprite>();
    private static final Map<ModelFace, Map<Icon, ModelFace>> alt = new IdentityHashMap<ModelFace, Map<Icon, ModelFace>>();

    public static ModelFace registerModelFaceSprite(ModelFace face, TextureAtlasSprite sprite) {
        modelFaceSprites.put(face, sprite);
        return face;
    }

    private static TextureAtlasSprite getSprite(ModelFace face) {
        if (face instanceof ModelFaceSprite) {
            return ((ModelFaceSprite) face).sprite;
        } else {
            return modelFaceSprites.get(face);
        }
    }

    public ModelFace getModelFace(ModelFace origFace) {
        TextureAtlasSprite origIcon = getSprite(origFace);
        if (origIcon == null) {
            return origFace;
        }
        ModelFace newFace;
        synchronized (alt) {
            Map<Icon, ModelFace> tmp = alt.get(origFace);
            if (tmp == null) {
                tmp = new HashMap<Icon, ModelFace>();
                alt.put(origFace, tmp);
            }
            ijkIterator.go(this, origIcon);
            TextureAtlasSprite newIcon = (TextureAtlasSprite) ijkIterator.getIcon();
            if (newIcon == origIcon) {
                return origFace;
            }
            newFace = tmp.get(newIcon);
            if (newFace == null) {
                newFace = new ModelFaceSprite(origFace, newIcon);
                logger.info("%s (%s) -> %s (%s)", origFace, origIcon.getIconName(), newFace, newIcon.getIconName());
                tmp.put(newIcon, newFace);
            }
        }
        return newFace;
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
        return direction == null ? -1 : direction.ordinal();
    }

    @Override
    public int getTextureFace() {
        return getBlockFace();
    }

    @Override
    public int getTextureFaceOrig() {
        return getTextureFace();
    }

    @Override
    public int getFaceForHV() {
        return getBlockFace();
    }

    @Override
    public int[] getOffset(int blockFace, int relativeDirection) {
        return FACE_VERTICES[0][0];
    }

    @Override
    public boolean setCoordOffsetsForRenderType() {
        return false;
    }

    @Override
    public int getDI() {
        return 0;
    }

    @Override
    public int getDJ() {
        return 0;
    }

    @Override
    public int getDK() {
        return 0;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ColorizeBlock18{");
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
