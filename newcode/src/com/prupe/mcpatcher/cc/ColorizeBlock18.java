package com.prupe.mcpatcher.cc;

import com.prupe.mcpatcher.MCLogger;
import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.mal.resource.PropertiesFile;
import com.prupe.mcpatcher.mal.resource.TexturePackAPI;
import com.prupe.mcpatcher.mal.resource.TexturePackChangeHandler;
import net.minecraft.src.ResourceLocation;

public class ColorizeBlock18 {
    private static final MCLogger logger = MCLogger.getLogger(MCPatcherUtils.CUSTOM_COLORS);

    private static final ResourceLocation COLOR_PROPERTIES = TexturePackAPI.newMCPatcherResourceLocation("color.properties");

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
            ColorizeBlock.reset();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
