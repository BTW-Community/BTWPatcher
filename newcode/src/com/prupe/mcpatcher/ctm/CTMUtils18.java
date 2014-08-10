package com.prupe.mcpatcher.ctm;

import com.prupe.mcpatcher.MCLogger;
import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.mal.block.BlockStateMatcher;
import com.prupe.mcpatcher.mal.resource.TexturePackChangeHandler;

public class CTMUtils18 {
    private static final MCLogger logger = MCLogger.getLogger(MCPatcherUtils.CONNECTED_TEXTURES, "CTM");

    static {
        TexturePackChangeHandler.register(new TexturePackChangeHandler("CTM", 2) {
            @Override
            public void beforeChange() {
            }

            @Override
            public void afterChange() {
                BlockStateMatcher.V2.dumpBlockStates(logger);
            }
        });
    }

    public static void reset() {
        logger.info("CTMUtils18.reset");
    }
}
