package com.prupe.mcpatcher.cc;

import com.prupe.mcpatcher.MCLogger;
import com.prupe.mcpatcher.MCPatcherUtils;
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

        useCM = RenderPassAPI.instance.useColorMultiplierThisPass(block);
        if (useCM) {
            colorMap = ColorizeBlock.findColorMap(block, blockAccess, position.getI(), position.getJ(), position.getK());
        } else {
            colorMap = null;
        }

        return true;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
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
}
