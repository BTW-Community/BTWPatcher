package com.prupe.mcpatcher.mob;

import com.prupe.mcpatcher.MCLogger;
import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.TexturePackAPI;
import com.prupe.mcpatcher.WeightedIndex;
import net.minecraft.src.ResourceAddress;

import java.util.*;

class MobRuleList {
    private static final MCLogger logger = MCLogger.getLogger(MCPatcherUtils.RANDOM_MOBS);

    public static final String ALTERNATIVES_REGEX = "_(eyes|overlay|tame|angry|collar|fur|invulnerable|shooting)\\.properties$";

    private static final Map<ResourceAddress, MobRuleList> allRules = new HashMap<ResourceAddress, MobRuleList>();

    private final ResourceAddress baseSkin;
    private final List<ResourceAddress> allSkins;
    private final int skinCount;
    private final List<MobRuleEntry> entries;

    private MobRuleList(ResourceAddress baseSkin) {
        this.baseSkin = baseSkin;
        allSkins = new ArrayList<ResourceAddress>();
        allSkins.add(baseSkin);
        for (int i = 2; ; i++) {
            ResourceAddress skin = new ResourceAddress(baseSkin.getNamespace(), baseSkin.getPath().replaceFirst("\\.png$", "" + i + ".png"));
            if (!TexturePackAPI.hasResource(skin)) {
                break;
            }
            allSkins.add(skin);
        }
        skinCount = allSkins.size();
        if (skinCount <= 1) {
            entries = null;
            return;
        }
        logger.fine("found %d variations for %s", skinCount, baseSkin);

        ResourceAddress filename = new ResourceAddress(baseSkin.getNamespace(), baseSkin.getPath().replace(".png", ".properties"));
        ResourceAddress altFilename = new ResourceAddress(baseSkin.getNamespace(), filename.getPath().replaceFirst(ALTERNATIVES_REGEX, ".properties"));
        Properties properties = TexturePackAPI.getProperties(filename);
        if (properties == null && !filename.equals(altFilename)) {
            properties = TexturePackAPI.getProperties(altFilename);
            if (properties != null) {
                logger.fine("using %s for %s", altFilename, baseSkin);
            }
        }
        ArrayList<MobRuleEntry> tmpEntries = new ArrayList<MobRuleEntry>();
        if (properties != null) {
            for (int i = 0; ; i++) {
                MobRuleEntry entry = MobRuleEntry.load(properties, i, skinCount);
                if (entry == null) {
                    if (i > 0) {
                        break;
                    }
                } else {
                    logger.fine("  %s", entry.toString());
                    tmpEntries.add(entry);
                }
            }
        }
        entries = tmpEntries.isEmpty() ? null : tmpEntries;
    }

    ResourceAddress getSkin(long key, int i, int j, int k, String biome) {
        if (entries == null) {
            int index = (int) (key % skinCount);
            if (index < 0) {
                index += skinCount;
            }
            return allSkins.get(index);
        } else {
            for (MobRuleEntry entry : entries) {
                if (entry.match(i, j, k, biome)) {
                    int index = entry.weightedIndex.choose(key);
                    return allSkins.get(entry.skins[index]);
                }
            }
        }
        return baseSkin;
    }

    static MobRuleList get(ResourceAddress texture) {
        MobRuleList list = allRules.get(texture);
        if (list == null) {
            list = new MobRuleList(texture);
            allRules.put(texture, list);
        }
        return list;
    }

    static void clear() {
        allRules.clear();
    }

    private static class MobRuleEntry {
        final int[] skins;
        final WeightedIndex weightedIndex;
        private final Set<String> biomes;
        private final int minHeight;
        private final int maxHeight;

        static MobRuleEntry load(Properties properties, int index, int limit) {
            String skinList = properties.getProperty("skins." + index, "").trim().toLowerCase();
            int[] skins;
            if (skinList.equals("*") || skinList.equals("all") || skinList.equals("any")) {
                skins = new int[limit];
                for (int i = 0; i < skins.length; i++) {
                    skins[i] = i;
                }
            } else {
                skins = MCPatcherUtils.parseIntegerList(skinList, 1, limit);
                if (skins.length <= 0) {
                    return null;
                }
                for (int i = 0; i < skins.length; i++) {
                    skins[i]--;
                }
            }

            WeightedIndex chooser = WeightedIndex.create(skins.length, properties.getProperty("weights." + index, ""));
            if (chooser == null) {
                return null;
            }

            HashSet<String> biomes = new HashSet<String>();
            String biomeList = properties.getProperty("biomes." + index, "").trim().toLowerCase();
            if (!biomeList.equals("")) {
                Collections.addAll(biomes, biomeList.split("\\s+"));
            }
            if (biomes.isEmpty()) {
                biomes = null;
            }

            int minHeight = MCPatcherUtils.getIntProperty(properties, "minHeight." + index, -1);
            int maxHeight = MCPatcherUtils.getIntProperty(properties, "maxHeight." + index, Integer.MAX_VALUE);
            if (minHeight < 0 || minHeight > maxHeight) {
                minHeight = -1;
                maxHeight = Integer.MAX_VALUE;
            }

            return new MobRuleEntry(skins, chooser, biomes, minHeight, maxHeight);
        }

        MobRuleEntry(int[] skins, WeightedIndex weightedIndex, HashSet<String> biomes, int minHeight, int maxHeight) {
            this.skins = skins;
            this.weightedIndex = weightedIndex;
            this.biomes = biomes;
            this.minHeight = minHeight;
            this.maxHeight = maxHeight;
        }

        boolean match(int i, int j, int k, String biome) {
            if (biomes != null) {
                if (!biomes.contains(biome)) {
                    return false;
                }
            }
            if (minHeight >= 0) {
                if (j < minHeight || j > maxHeight) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("skins:");
            for (int i : skins) {
                sb.append(' ').append(i + 1);
            }
            if (biomes != null) {
                sb.append(", biomes:");
                for (String s : biomes) {
                    sb.append(' ').append(s);
                }
            }
            if (minHeight >= 0) {
                sb.append(", height: ").append(minHeight).append('-').append(maxHeight);
            }
            sb.append(", weights: ").append(weightedIndex.toString());
            return sb.toString();
        }
    }
}
