package com.prupe.mcpatcher.cit;

import com.prupe.mcpatcher.MCLogger;
import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.mal.tile.FaceInfo;
import net.minecraft.src.ItemBlock;
import net.minecraft.src.ItemStack;
import net.minecraft.src.ModelFace;
import net.minecraft.src.TextureAtlasSprite;

public class CITUtils18 {
    private static final MCLogger logger = MCLogger.getLogger(MCPatcherUtils.CUSTOM_ITEM_TEXTURES, "CIT");

    private static ItemStack currentItem;
    private static int currentColor;
    private static ItemOverride itemOverride;
    private static ArmorOverride armorOverride;
    private static EnchantmentList enchantments;

    public static void preRender(ItemStack itemStack, int color) {
        currentColor = color;
        if (itemStack == null) {
            // rendering enchantment -- keep current state
        } else if (itemStack.getItem() instanceof ItemBlock) {
            clear();
        } else {
            currentItem = itemStack;
            itemOverride = CITUtils.findItemOverride(itemStack);
            armorOverride = CITUtils.findArmorOverride(itemStack);
            enchantments = CITUtils.findEnchantments(itemStack);
            if (logger.logEvery(5000L)) {
                logger.info("preRender(%s, %08x) -> %s %s %s",
                    currentItem, currentColor, itemOverride, armorOverride, enchantments
                );
            }
        }
    }

    public static ModelFace getModelFace(ModelFace origFace) {
        if (itemOverride == null) {
            return origFace;
        } else {
            FaceInfo faceInfo = FaceInfo.getFaceInfo(origFace);
            TextureAtlasSprite newIcon = (TextureAtlasSprite) itemOverride.getReplacementIcon(faceInfo.getSprite());
            return faceInfo.getAltFace(newIcon);
        }
    }

    static void clear() {
        currentItem = null;
        itemOverride = null;
        armorOverride = null;
        enchantments = null;
    }
}
