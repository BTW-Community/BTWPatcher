package com.prupe.mcpatcher.mod;

import net.minecraft.src.ItemStack;

import java.util.ArrayList;
import java.util.List;

class ItemOverlayList {
    private final List<ItemOverride> matches = new ArrayList<ItemOverride>();
    private final List<Integer> levels = new ArrayList<Integer>();
    private int total;

    ItemOverlayList(ItemOverride[][] overlays, ItemStack itemStack) {
        int[] enchantmentLevels = CITUtils.getEnchantmentLevels(itemStack.stackTagCompound);
        int itemID = itemStack.itemID;
        if (itemID >= 0 && itemID < overlays.length && overlays[itemID] != null) {
            for (ItemOverride override : overlays[itemID]) {
                if (override.match(itemID, itemStack, enchantmentLevels)) {
                    matches.add(override);
                    int level = Math.max(override.lastEnchantmentLevel, 1);
                    levels.add(level);
                    total += level;
                }
            }
        }
    }

    boolean isEmpty() {
        return size() == 0 || total <= 0;
    }

    int size() {
        return matches.size();
    }

    ItemOverlay getOverlay(int index) {
        return matches.get(index).overlay;
    }

    float getFade(int index) {
        return (float) levels.get(index) / (float) total;
    }
}
