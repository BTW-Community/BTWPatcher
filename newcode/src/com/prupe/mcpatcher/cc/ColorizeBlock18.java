package com.prupe.mcpatcher.cc;

import com.prupe.mcpatcher.MCLogger;
import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.colormap.ColorUtils;
import com.prupe.mcpatcher.colormap.IColorMap;
import com.prupe.mcpatcher.mal.block.BlockAPI;
import com.prupe.mcpatcher.mal.block.RenderPassAPI;
import com.prupe.mcpatcher.mal.resource.PropertiesFile;
import com.prupe.mcpatcher.mal.resource.TexturePackAPI;
import com.prupe.mcpatcher.mal.resource.TexturePackChangeHandler;
import net.minecraft.src.*;

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
            ColorizeBlock.reset();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static boolean preRender(IBlockAccess blockAccess, IModel model, IBlockState blockState, Position position, Block block, boolean useAO) {
        ColorizeBlock18 instance = getInstance();
        if (instance == null) {
            instance = new ColorizeBlock18();
            instances.set(instance);
        }
        return instance.preRender1(blockAccess, model, blockState, position, block, useAO);
    }

    public static ColorizeBlock18 getInstance() {
        return instances.get();
    }

    private ColorizeBlock18() {
        logger.info("new ColorizeBlock18() for %s", Thread.currentThread());
    }

    private boolean preRender1(IBlockAccess blockAccess, IModel model, IBlockState blockState, Position position, Block block, boolean useAO) {
        this.blockAccess = blockAccess;
        this.model = model;
        this.blockState = blockState;
        this.position = position;
        this.block = block;
        this.useAO = useAO;
        direction = null;

        useCM = RenderPassAPI.instance.useColorMultiplierThisPass(block);
        if (useCM) {
            colorMap = ColorizeBlock.findColorMap(block, blockAccess, position.getI(), position.getJ(), position.getK());
        } else {
            colorMap = null;
        }
        isSmooth = false;

        return true;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
        if (ColorizeBlock.enableSmoothBiomes && direction != null && colorMap != null) {
            isSmooth = true;
            int[][] offsets = ColorizeBlock.FACE_VERTICES[direction.ordinal()];
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

    public boolean useColormap(boolean use) {
        return useCM && (use || (colorMap != null && block != grassBlock && block != mycelBlock));
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
