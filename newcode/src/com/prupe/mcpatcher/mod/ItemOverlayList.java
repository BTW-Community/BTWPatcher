package com.prupe.mcpatcher.mod;

import net.minecraft.src.ItemStack;

import java.util.*;

class ItemOverlayList {
    private final List<ItemOverride> matches = new ArrayList<ItemOverride>();
    private final List<Integer> levels = new ArrayList<Integer>();
    private final Map<Integer, Group> groups = new HashMap<Integer, Group>();
    private int total;

    ItemOverlayList(ItemOverride[][] overlays, ItemStack itemStack) {
        int[] enchantmentLevels = CITUtils.getEnchantmentLevels(itemStack.stackTagCompound);
        int itemID = itemStack.itemID;
        if (itemID >= 0 && itemID < overlays.length && overlays[itemID] != null) {
            for (ItemOverride override : overlays[itemID]) {
                if (override.match(itemID, itemStack, enchantmentLevels)) {
                    int level = Math.max(override.lastEnchantmentLevel, 1);
                    getGroup(override.overlay).add(override.overlay, level);
                }
            }
        }
    }

    boolean isEmpty() {
        return groups.isEmpty();
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

    private Group getGroup(ItemOverlay overlay) {
        Group group = groups.get(overlay.groupID);
        if (group == null) {
            group = new Group(overlay);
            groups.put(overlay.groupID, group);
        }
        return group;
    }

    private static class Group {
        final List<Entry> entries = new ArrayList<Entry>();
        final int method;
        final int limit;
        int total;

        Group(ItemOverlay overlay) {
            method = overlay.applyMethod;
            limit = overlay.limit;
        }

        void add(ItemOverlay overlay, int level) {
            entries.add(new Entry(overlay, level));
            total += level;
        }

        void computeIntensities() {
            switch (method) {
                case ItemOverlay.AVERAGE:
                case ItemOverlay.TOP:
                    Collections.sort(entries, new Comparator<Entry>() {
                        public int compare(Entry o1, Entry o2) {
                            int diff = o1.overlay.weight - o2.overlay.weight;
                            if (diff != 0) {
                                return diff;
                            }
                            return o1.level - o2.level;
                        }
                    });
                    if (limit > 0) {
                        while (entries.size() > limit) {
                            entries.remove(limit);
                        }
                    }
                    total = 0;
                    for (Entry entry : entries) {
                        total += entry.level;
                    }
                    break;

                case ItemOverlay.CYCLE:
                    break;

                default:
                    break;
            }
        }

        float getIntensity(int index) {
            return (float) entries.get(index).level / (float) total;
        }
    }

    private static class Entry {
        final ItemOverlay overlay;
        final int level;

        Entry(ItemOverlay overlay, int level) {
            this.overlay = overlay;
            this.level = level;
        }
    }
}
