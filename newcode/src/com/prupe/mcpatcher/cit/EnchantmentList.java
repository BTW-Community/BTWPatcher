package com.prupe.mcpatcher.cit;

import com.prupe.mcpatcher.MCPatcherUtils;
import net.minecraft.src.ItemStack;

import java.util.*;

class EnchantmentList {
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
            String value = MCPatcherUtils.getStringProperty(properties, "method", "").toLowerCase();
            if (value.equals("layered")) {
                applyMethod = LAYERED;
            } else if (value.equals("cycle")) {
                applyMethod = CYCLE;
            } else {
                applyMethod = AVERAGE;
            }
            limit = MCPatcherUtils.getIntProperty(properties, "cap", limit);
            fade = MCPatcherUtils.getFloatProperty(properties, "fade", fade);
        }
    }

    EnchantmentList(ItemOverride[][] overlays, ItemStack itemStack) {
        BitSet layersPresent = new BitSet();
        Map<Integer, Layer> tmpLayers = new HashMap<Integer, Layer>();
        int[] enchantmentLevels = CITUtils.getEnchantmentLevels(itemStack.stackTagCompound);
        int itemID = itemStack.itemID;
        if (itemID >= 0 && itemID < overlays.length && overlays[itemID] != null) {
            for (ItemOverride override : overlays[itemID]) {
                if (override.match(itemID, itemStack, enchantmentLevels)) {
                    int level = Math.max(override.lastEnchantmentLevel, 1);
                    Enchantment overlay = override.overlay;
                    int layer = overlay.layer;
                    Layer newLayer = new Layer(overlay, level);
                    Layer oldLayer = tmpLayers.get(layer);
                    if (oldLayer == null || newLayer.overlay.compareTo(oldLayer.overlay) > 0) {
                        tmpLayers.put(layer, newLayer);
                    }
                    layersPresent.set(layer);
                }
            }
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

    Enchantment getOverlay(int index) {
        return layers.get(index).overlay;
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
        final Enchantment overlay;
        final int level;
        float intensity;

        Layer(Enchantment overlay, int level) {
            this.overlay = overlay;
            this.level = level;
        }

        float getEffectiveDuration() {
            return overlay.duration + 2.0f * fade;
        }
    }
}
