package com.prupe.mcpatcher.cit;

import com.prupe.mcpatcher.MCLogger;
import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.ctm.CTMUtils;
import com.prupe.mcpatcher.ctm.CTMUtils18;
import com.prupe.mcpatcher.mal.item.ItemAPI;
import com.prupe.mcpatcher.mal.resource.TexturePackAPI;
import com.prupe.mcpatcher.mal.tile.FaceInfo;
import net.minecraft.src.*;

public class CITUtils18 {
    private static final MCLogger logger = MCLogger.getLogger(MCPatcherUtils.CUSTOM_ITEM_TEXTURES, "CIT");

    private static ItemStack currentItem;
    private static CTMUtils18 ctm;
    private static ItemOverride itemOverride;
    private static boolean renderingEnchantment;

    public static void preRender(ItemStack itemStack) {
        if (renderingEnchantment) {
            // rendering custom enchantment -- keep current state
        } else if (itemStack == null) {
            clear();
        } else if (itemStack.getItem() instanceof ItemBlock) {
            clear();
            ctm = CTMUtils18.getInstance();
            ctm.preRenderHeld(null, ((ItemBlock) itemStack.getItem()).getBlock(), itemStack.getItemDamage());
        } else {
            ctm = null;
            currentItem = itemStack;
            itemOverride = CITUtils.findItemOverride(itemStack);
        }
    }

    public static void postRender() {
        if (ctm != null) {
            ctm.clear();
        }
        clear();
    }

    public static ModelFace getModelFace(ModelFace origFace) {
        if (renderingEnchantment) {
            return FaceInfo.getFaceInfo(origFace).getNonAtlasFace();
        } else if (ctm != null) {
            int face = FaceInfo.getFaceInfo(origFace).getEffectiveFace();
            ctm.setDirection(face < 0 ? null : Direction.values()[face]);
            return ctm.getModelFace(origFace);
        } else if (itemOverride == null) {
            return origFace;
        } else {
            FaceInfo faceInfo = FaceInfo.getFaceInfo(origFace);
            TextureAtlasSprite newIcon = (TextureAtlasSprite) itemOverride.getReplacementIcon(faceInfo.getSprite());
            return faceInfo.getAltFace(newIcon);
        }
    }

    public static boolean renderEnchantments3D(RenderItemCustom renderItem, IModel model) {
        if (currentItem != null) {
            EnchantmentList enchantments = CITUtils.findEnchantments(currentItem);
            if (!enchantments.isEmpty()) {
                renderingEnchantment = true;
                Enchantment.beginOuter3D();
                for (int i = 0; i < enchantments.size(); i++) {
                    Enchantment enchantment = enchantments.getEnchantment(i);
                    float intensity = enchantments.getIntensity(i);
                    if (intensity > 0.0f && enchantment.bindTexture(null)) {
                        enchantment.begin(intensity);
                        renderItem.renderItem1(model, -1, null);
                        enchantment.end();
                    }
                }
                Enchantment.endOuter3D();
                TexturePackAPI.bindTexture(TexturePackAPI.ITEMS_PNG);
                renderingEnchantment = false;
            }
        }
        return !CITUtils.useGlint;
    }

    public static ResourceLocation getArmorTexture(ResourceLocation origTexture, ItemStack itemStack, int slot) {
        ArmorOverride override = CITUtils.findArmorOverride(itemStack);
        if (override == null) {
            return origTexture;
        } else {
            return override.getReplacementTexture(origTexture);
        }
    }

    public static boolean renderArmorEnchantments(EntityLivingBase entity, ModelBase model, ItemStack itemStack, int slot, float f1, float f2, float f3, float f4, float f5, float f6) {
        EnchantmentList enchantments = CITUtils.findEnchantments(itemStack);
        if (!enchantments.isEmpty()) {
            Enchantment.beginOuter3D();
            for (int i = 0; i < enchantments.size(); i++) {
                Enchantment enchantment = enchantments.getEnchantment(i);
                float intensity = enchantments.getIntensity(i);
                if (intensity > 0.0f && enchantment.bindTexture(null)) {
                    enchantment.begin(intensity);
                    model.render(entity, f1, f2, f3, f4, f5, f6);
                    enchantment.end();
                }
            }
            Enchantment.endOuter3D();
        }
        return !CITUtils.useGlint;
    }

    static void clear() {
        currentItem = null;
        ctm = null;
        itemOverride = null;
        renderingEnchantment = false;
    }
}
