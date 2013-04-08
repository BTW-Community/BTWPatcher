package com.prupe.mcpatcher.mod;

import net.minecraft.src.ItemStack;

import java.util.*;

class ItemOverlayList {
    private static final float PI = (float) Math.PI;

    private final Map<Integer, Group> groups = new HashMap<Integer, Group>();
    private final List<Entry> entries = new ArrayList<Entry>();

    ItemOverlayList(ItemOverride[][] overlays, ItemStack itemStack) {
        int[] enchantmentLevels = CITUtils.getEnchantmentLevels(itemStack.stackTagCompound);
        int itemID = itemStack.itemID;
        if (itemID >= 0 && itemID < overlays.length && overlays[itemID] != null) {
            for (ItemOverride override : overlays[itemID]) {
                if (override.match(itemID, itemStack, enchantmentLevels)) {
                    int level = Math.max(override.lastEnchantmentLevel, 1);
                    Entry entry = getGroup(override.overlay).add(override.overlay, level);
                    entries.add(entry);
                }
            }
        }
        for (Group group : groups.values()) {
            group.computeIntensities();
        }
    }

    boolean isEmpty() {
        return groups.isEmpty();
    }

    int size() {
        return entries.size();
    }

    ItemOverlay getOverlay(int index) {
        return entries.get(index).overlay;
    }

    float getIntensity(int index) {
        return entries.get(index).intensity;
    }

    private Group getGroup(ItemOverlay overlay) {
        Group group = groups.get(overlay.groupID);
        if (group == null) {
            switch (overlay.applyMethod) {
                case ItemOverlay.AVERAGE:
                case ItemOverlay.TOP:
                    group = new AverageGroup(overlay);
                    break;

                case ItemOverlay.CYCLE:
                    group = new CycleGroup(overlay);
                    break;
            }
            groups.put(overlay.groupID, group);
        }
        return group;
    }

    abstract private static class Group {
        final List<Entry> entries = new ArrayList<Entry>();
        final int method;
        final int limit;

        Group(ItemOverlay overlay) {
            method = overlay.applyMethod;
            limit = overlay.limit;
        }

        Entry add(ItemOverlay overlay, int level) {
            Entry entry = new Entry(overlay, level);
            entries.add(entry);
            return entry;
        }

        void computeIntensities() {
            for (Entry entry : entries) {
                entry.intensity = 0.0f;
            }
        }
    }

    private static class AverageGroup extends Group {
        int total;

        AverageGroup(ItemOverlay overlay) {
            super(overlay);
        }

        @Override
        void computeIntensities() {
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
                if (method == ItemOverlay.AVERAGE) {
                    total += entry.level;
                } else {
                    total = Math.max(total, entry.level);
                }
            }
            if (total > 0) {
                for (Entry entry : entries) {
                    entry.intensity = (float) entry.level / (float) total;
                }
            }
        }
    }

    private static class CycleGroup extends Group {
        float total;

        CycleGroup(ItemOverlay overlay) {
            super(overlay);
        }

        @Override
        Entry add(ItemOverlay overlay, int level) {
            Entry entry = super.add(overlay, level);
            entry.start = total;
            total += overlay.duration;
            return entry;
        }

        @Override
        void computeIntensities() {
            if (total <= 0.0f) {
                return;
            }
            float timestamp = (float) ((System.currentTimeMillis() / 1000.0) % total);
            for (Entry entry : entries) {
                if (timestamp <= 0.0f) {
                    break;
                }
                float duration = entry.overlay.duration;
                if (timestamp < duration) {
                    entry.intensity = (float) Math.sin(PI * timestamp / duration);
                }
                timestamp -= duration;
            }
        }
    }

    private static class Entry {
        final ItemOverlay overlay;
        final int level;
        float start;
        float intensity;

        Entry(ItemOverlay overlay, int level) {
            this.overlay = overlay;
            this.level = level;
        }
    }
}
