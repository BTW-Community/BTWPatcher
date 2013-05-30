package com.prupe.mcpatcher.cit;

import com.prupe.mcpatcher.MCLogger;
import com.prupe.mcpatcher.MCPatcherUtils;
import net.minecraft.src.ItemStack;

import java.util.*;

class EnchantmentList {
    private static final MCLogger logger = MCLogger.getLogger(MCPatcherUtils.CUSTOM_ITEM_TEXTURES, "CIT");

    private static final float PI = (float) Math.PI;

    private static final int AVERAGE = 0;
    private static final int LAYERED = 1;
    private static final int CYCLE = 2;

    private static int applyMethod;
    private static int limit;
    private static float fade;

    private final List<Layer> layers = new ArrayList<Layer>();

    static void setProperties(Properties properties) {
        applyMethod = LAYERED;
        limit = 99;
        fade = 0.5f;
        if (properties != null) {
            String value = MCPatcherUtils.getStringProperty(properties, "method", "average").toLowerCase();
            if (value.equals("layered")) {
                applyMethod = LAYERED;
            } else if (value.equals("cycle")) {
                applyMethod = CYCLE;
            } else if (value.equals("average")) {
                applyMethod = AVERAGE;
            } else {
                logger.warning("%s: unknown enchantment layering method '%s'", CITUtils.CIT_PROPERTIES, value);
                applyMethod = AVERAGE;
            }
            limit = Math.max(MCPatcherUtils.getIntProperty(properties, "cap", limit), 0);
            fade = Math.max(MCPatcherUtils.getFloatProperty(properties, "fade", fade), 0.0f);
        }
    }

    EnchantmentList(ItemOverride[][] enchantments, ItemStack itemStack) {
        BitSet layersPresent = new BitSet();
        Map<Integer, Layer> tmpLayers = new HashMap<Integer, Layer>();
        int itemID = itemStack.itemID;
        int[] enchantmentLevels = CITUtils.getEnchantmentLevels(itemID, itemStack.stackTagCompound);
        boolean hasEffect = itemStack.hasEffect();
        if (itemID >= 0 && itemID < enchantments.length && enchantments[itemID] != null) {
            for (ItemOverride enchantment : enchantments[itemID]) {
                if (enchantment.match(itemStack, enchantmentLevels, hasEffect)) {
                    int level = Math.max(enchantment.lastEnchantmentLevel, 1);
                    int layer = enchantment.layer;
                    Layer newLayer = new Layer((Enchantment) enchantment, level);
                    Layer oldLayer = tmpLayers.get(layer);
                    if (oldLayer == null || newLayer.enchantment.compareTo(oldLayer.enchantment) > 0) {
                        tmpLayers.put(layer, newLayer);
                    }
                    layersPresent.set(layer);
                }
            }
        }
        if (layersPresent.isEmpty()) {
            return;
        }
        while (layersPresent.cardinality() > limit) {
            int layer = layersPresent.nextSetBit(0);
            layersPresent.clear(layer);
            tmpLayers.remove(layer);
        }
        for (int i = layersPresent.nextSetBit(0); i >= 0; i = layersPresent.nextSetBit(i + 1)) {
            layers.add(tmpLayers.get(i));
        }
        switch (applyMethod) {
            default:
                computeIntensitiesAverage();
                break;

            case LAYERED:
                computeIntensitiesLayered();
                break;

            case CYCLE:
                computeIntensitiesCycle();
                break;
        }
    }

    boolean isEmpty() {
        return layers.isEmpty();
    }

    int size() {
        return layers.size();
    }

    Enchantment getEnchantment(int index) {
        return layers.get(index).enchantment;
    }

    float getIntensity(int index) {
        return layers.get(index).intensity;
    }

    private void computeIntensitiesAverage() {
        int total = 0;
        for (Layer layer : layers) {
            total += layer.level;
        }
        computeIntensitiesAverage(total);
    }

    private void computeIntensitiesLayered() {
        int max = 0;
        for (Layer layer : layers) {
            Math.max(max, layer.level);
        }
        computeIntensitiesAverage(max);
    }

    private void computeIntensitiesAverage(int denominator) {
        if (denominator > 0) {
            for (Layer layer : layers) {
                layer.intensity = (float) layer.level / (float) denominator;
            }
        } else {
            for (Layer layer : layers) {
                layer.intensity = 1.0f;
            }
        }
    }

    private void computeIntensitiesCycle() {
        float total = 0.0f;
        for (Layer layer : layers) {
            total += layer.getEffectiveDuration();
        }
        float timestamp = (float) ((System.currentTimeMillis() / 1000.0) % total);
        for (Layer layer : layers) {
            if (timestamp <= 0.0f) {
                break;
            }
            float duration = layer.getEffectiveDuration();
            if (timestamp < duration) {
                float denominator = (float) Math.sin(PI * fade / duration);
                layer.intensity = (float) (Math.sin(PI * timestamp / duration) / (denominator == 0.0f ? 1.0f : denominator));
            }
            timestamp -= duration;
        }
    }

    private static class Layer {
        final Enchantment enchantment;
        final int level;
        float intensity;

        Layer(Enchantment enchantment, int level) {
            this.enchantment = enchantment;
            this.level = level;
        }

        float getEffectiveDuration() {
            return enchantment.duration + 2.0f * fade;
        }
    }
}
