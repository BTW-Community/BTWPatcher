package com.prupe.mcpatcher.cc;

import com.prupe.mcpatcher.MCLogger;
import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.colormap.ColorUtils;
import com.prupe.mcpatcher.colormap.IColorMap;
import com.prupe.mcpatcher.mal.block.BlockAPI;
import com.prupe.mcpatcher.mal.block.BlockStateMatcher;
import com.prupe.mcpatcher.mal.block.RenderPassAPI;
import com.prupe.mcpatcher.mal.resource.PropertiesFile;
import com.prupe.mcpatcher.mal.resource.TexturePackAPI;
import com.prupe.mcpatcher.mal.resource.TexturePackChangeHandler;
import com.prupe.mcpatcher.mal.util.InputHandler;
import net.minecraft.src.*;
import org.lwjgl.input.Keyboard;

import java.util.List;

public class ColorizeBlock18 {
    private static final MCLogger logger = MCLogger.getLogger(MCPatcherUtils.CUSTOM_COLORS);

    private static final ResourceLocation COLOR_PROPERTIES = TexturePackAPI.newMCPatcherResourceLocation("color.properties");

    private static final ThreadLocal<ColorizeBlock18> instances = new ThreadLocal<ColorizeBlock18>();

    private static Block grassBlock;
    private static Block mycelBlock;

    private IBlockAccess blockAccess;
    private IModel model;
    private IBlockState blockState;
    private Position position;
    private Block block;
    private boolean useAO;
    private Direction direction;

    private boolean useCM;
    private IColorMap colorMap;
    private boolean isSmooth;
    private final float[][] vertexColors = new float[4][3];

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

    private static Thread foo;
    private static int bar;

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
                if (foo == null) {
                    foo = new Thread(new Runnable() {
                        private final InputHandler kk = new InputHandler("CC", true);

                        @Override
                        public void run() {
                            try {
                                Thread.sleep(10000);
                                logger.info("starting * thread");
                                while (true) {
                                    Thread.sleep(100);
                                    if (kk.isKeyPressed(Keyboard.KEY_MULTIPLY)) {
                                        bar = (bar + 1) % 3;
                                        logger.info("bar = %s", bar == 0 ? "normal" : bar == 1 ? "red,white,blue" : "red,green,blue");
                                    }
                                }
                            } catch (Throwable e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    foo.start();
                }
            }
        });
    }

    public static void reset() {
        try {
            grassBlock = BlockAPI.getFixedBlock("minecraft:grass");
            mycelBlock = BlockAPI.getFixedBlock("minecraft:mycelium");
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
        this.direction = direction;
        if (ColorizeBlock.enableSmoothBiomes && direction != null && colorMap != null) {
            isSmooth = true;
            int[][] offsets = FACE_VERTICES[direction.ordinal()];
            computeVertexColor(offsets[0], 0, vertexColors[0]);
            computeVertexColor(offsets[1], 1, vertexColors[1]);
            computeVertexColor(offsets[2], 2, vertexColors[2]);
            computeVertexColor(offsets[3], 3, vertexColors[3]);
        } else {
            isSmooth = false;
        }
    }

    private void computeVertexColor(int[] offsets, int index, float[] color) {
        int i = position.getI() + offsets[0];
        int j = position.getJ() + offsets[1];
        int k = position.getK() + offsets[2];
        int rgb = 0;
        switch (bar) {
            case 1:
                switch (index) {
                    default:
                    case 0: // top left
                        rgb = 0x000000;
                        break;

                    case 1: // bottom left
                        rgb = 0xff0000;
                        break;

                    case 2: // bottom right
                        rgb = 0xffffff;
                        break;

                    case 3: // top right
                        rgb = 0x0000ff;
                        break;
                }
                ColorUtils.intToFloat3(rgb, color);
                break;

            case 2:
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
                break;

            default:
                float[] tmp = colorMap.getColorMultiplierF(blockAccess, i, j, k);
                color[0] = tmp[0];
                color[1] = tmp[1];
                color[2] = tmp[2];
                break;
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
}
