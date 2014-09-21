package com.prupe.mcpatcher.cit;

import com.prupe.mcpatcher.MCLogger;
import com.prupe.mcpatcher.MCPatcherUtils;
import net.minecraft.src.ItemBlock;
import net.minecraft.src.ItemStack;

public class CITUtils18 {
    private static final MCLogger logger = MCLogger.getLogger(MCPatcherUtils.CUSTOM_ITEM_TEXTURES, "CIT");

    private static ItemStack currentItem;

    public static void preRender(ItemStack itemStack) {
        if (itemStack.getItem() instanceof ItemBlock) {
            currentItem = null;
            return;
        }
        currentItem = itemStack;
        if (logger.logEvery(5000L)) {
            logger.info("preRender(%s)", currentItem);
        }
    }

    static void clear() {
        currentItem = null;
    }
}
