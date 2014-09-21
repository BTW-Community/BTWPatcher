package com.prupe.mcpatcher.cit;

import com.prupe.mcpatcher.MCLogger;
import com.prupe.mcpatcher.MCPatcherUtils;
import net.minecraft.src.ItemBlock;
import net.minecraft.src.ItemStack;

public class CITUtils18 {
    private static final MCLogger logger = MCLogger.getLogger(MCPatcherUtils.CUSTOM_ITEM_TEXTURES, "CIT");

    private static ItemStack currentItem;
    private static int currentLayer;
    private static ItemOverride itemOverride;
    private static ArmorOverride armorOverride;
    private static EnchantmentList enchantments;

    public static void preRender(ItemStack itemStack, int layer) {
        currentLayer = layer;
        if (itemStack == null || itemStack.getItem() instanceof ItemBlock) {
            clear();
        } else {
            currentItem = itemStack;
            itemOverride = CITUtils.findItemOverride(itemStack);
            armorOverride = CITUtils.findArmorOverride(itemStack);
            enchantments = CITUtils.findEnchantments(itemStack);
            if (logger.logEvery(5000L)) {
                logger.info("preRender(%s, %d) -> %s %s %s",
                    currentItem, currentLayer, itemOverride, armorOverride, enchantments
                );
            }
        }
    }

    static void clear() {
        currentItem = null;
        itemOverride = null;
        armorOverride = null;
        enchantments = null;
    }
}
