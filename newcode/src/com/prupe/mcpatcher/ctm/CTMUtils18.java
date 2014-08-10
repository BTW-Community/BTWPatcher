package com.prupe.mcpatcher.ctm;

import com.prupe.mcpatcher.MCLogger;
import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.mal.block.BlockAPI;
import com.prupe.mcpatcher.mal.block.BlockStateMatcher;
import com.prupe.mcpatcher.mal.item.ItemAPI;
import com.prupe.mcpatcher.mal.resource.TexturePackChangeHandler;
import net.minecraft.src.Block;
import net.minecraft.src.Item;

public class CTMUtils18 {
    private static final MCLogger logger = MCLogger.getLogger(MCPatcherUtils.CONNECTED_TEXTURES, "CTM");

    static {
        TexturePackChangeHandler.register(new TexturePackChangeHandler("CTM", 2) {
            private boolean logged;

            @Override
            public void beforeChange() {
            }

            @Override
            public void afterChange() {
                if (!logged) {
                    logged = true;
                    BlockStateMatcher.V2.dumpBlockStates(logger);
                    Block block = BlockAPI.getFixedBlock("minecraft:stone");
                    logger.info("%s", BlockAPI.getBlockName(block));
                    Item item = ItemAPI.getFixedItem("minecraft:iron_sword");
                    logger.info("%s", ItemAPI.getItemName(item));
                }
            }
        });
    }

    public static void reset() {
        logger.info("CTMUtils18.reset");
    }
}
