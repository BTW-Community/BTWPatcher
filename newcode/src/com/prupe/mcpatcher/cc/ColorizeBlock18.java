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

    private static Block grassBlock;
    private static Block mycelBlock;

    private final RenderBlockCustom renderBlocks;

    private IBlockAccess blockAccess;
    private IModel model;
    private IBlockState blockState;
    private Position position;
    private Block block;
    private boolean useAO;

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

    public ColorizeBlock18(RenderBlockCustom renderBlocks) {
        this.renderBlocks = renderBlocks;
        logger.info("new ColorizeBlock18(%s)", renderBlocks);
    }

    public boolean preRender(IBlockAccess blockAccess, IModel model, IBlockState blockState, Position position, Block block, boolean useAO) {
        this.blockAccess = blockAccess;
        this.model = model;
        this.blockState = blockState;
        this.position = position;
        this.block = block;
        this.useAO = useAO;

        colorMap = ColorizeBlock.findColorMap(block, blockAccess, position.getI(), position.getJ(), position.getK());

        return true;
    }

    public boolean useColormap(boolean use) {
        return RenderPassAPI.instance.useColorMultiplierThisPass(block) && (use || (colorMap != null && block != grassBlock && block != mycelBlock));
    }

    public int colorMultiplier(int color) {
        if (colorMap == null) {
            return color;
        } else {
            return colorMap.getColorMultiplier(blockAccess, position.getI(), position.getJ(), position.getK());
        }
    }
}
