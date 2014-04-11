package com.prupe.mcpatcher.cit;

import com.prupe.mcpatcher.MCLogger;
import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.mal.resource.TexturePackAPI;
import com.prupe.mcpatcher.mal.tile.TileLoader;
import com.prupe.mcpatcher.mal.item.ItemAPI;
import com.prupe.mcpatcher.mal.nbt.NBTRule;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import net.minecraft.src.ResourceLocation;

import java.io.File;
import java.util.*;

abstract class OverrideBase implements Comparable<OverrideBase> {
    static final MCLogger logger = MCLogger.getLogger(MCPatcherUtils.CUSTOM_ITEM_TEXTURES, "CIT");

    private static final int MAX_DAMAGE = 65535;
    private static final int MAX_STACK_SIZE = 65535;

    final ResourceLocation propertiesName;
    final ResourceLocation textureName;
    final Map<String, ResourceLocation> alternateTextures;
    final int weight;
    final Set<Item> items;
    final BitSet damagePercent;
    final BitSet damage;
    final int damageMask;
    final BitSet stackSize;
    final BitSet enchantmentIDs;
    final BitSet enchantmentLevels;
    private final List<NBTRule> nbtRules = new ArrayList<NBTRule>();
    boolean error;

    int lastEnchantmentLevel;

    static OverrideBase create(ResourceLocation filename) {
        if (new File(filename.getPath()).getName().equals("cit.properties")) {
            return null;
        }
        Properties properties = TexturePackAPI.getProperties(filename);
        if (properties == null) {
            return null;
        }
        String type = MCPatcherUtils.getStringProperty(properties, "type", "item").toLowerCase();
        OverrideBase override;
        if (type.equals("item")) {
            if (!CITUtils.enableItems) {
                return null;
            }
            override = new ItemOverride(filename, properties);
        } else if (type.equals("enchantment") || type.equals("overlay")) {
            if (!CITUtils.enableEnchantments) {
                return null;
            }
            override = new Enchantment(filename, properties);
        } else if (type.equals("armor")) {
            if (!CITUtils.enableArmor) {
                return null;
            }
            override = new ArmorOverride(filename, properties);
        } else {
            logger.error("%s: unknown type '%s'", filename, type);
            return null;
        }
        return override.error ? null : override;
    }

    OverrideBase(ResourceLocation propertiesName, Properties properties) {
        this.propertiesName = propertiesName;

        alternateTextures = getAlternateTextures(properties);

        String value = MCPatcherUtils.getStringProperty(properties, "source", "");
        ResourceLocation resource = null;
        if (value.equals("")) {
            value = MCPatcherUtils.getStringProperty(properties, "texture", "");
        }
        if (value.equals("")) {
            value = MCPatcherUtils.getStringProperty(properties, "tile", "");
        }
        if (value.equals("")) {
            if (MCPatcherUtils.isNullOrEmpty(alternateTextures)) {
                resource = TileLoader.getDefaultAddress(propertiesName);
                if (!TexturePackAPI.hasResource(resource)) {
                    resource = null;
                }
            }
        } else {
            resource = TileLoader.parseTileAddress(propertiesName, value);
            if (!TexturePackAPI.hasResource(resource)) {
                error("source texture %s not found", value);
                resource = null;
            }
        }
        textureName = resource;

        weight = MCPatcherUtils.getIntProperty(properties, "weight", 0);

        value = MCPatcherUtils.getStringProperty(properties, "items", "");
        if (value.equals("")) {
            value = MCPatcherUtils.getStringProperty(properties, "matchItems", "");
        }
        if (value.equals("")) {
            items = null;
        } else {
            items = new HashSet<Item>();
            for (String s : value.split("\\s+")) {
                Item item = ItemAPI.parseItemName(s);
                if (item != null) {
                    items.add(item);
                }
            }
        }

        value = MCPatcherUtils.getStringProperty(properties, "damage", "");
        if (value.equals("")) {
            damage = null;
            damagePercent = null;
        } else if (value.contains("%")) {
            damage = null;
            damagePercent = parseBitSet(value.replace("%", ""), 0, 100);
        } else {
            damage = parseBitSet(value, 0, MAX_DAMAGE);
            damagePercent = null;
        }
        damageMask = MCPatcherUtils.getIntProperty(properties, "damageMask", MAX_DAMAGE);
        stackSize = parseBitSet(properties, "stackSize", 0, MAX_STACK_SIZE);
        enchantmentIDs = parseBitSet(properties, "enchantmentIDs", 0, CITUtils.MAX_ENCHANTMENTS - 1);
        enchantmentLevels = parseBitSet(properties, "enchantmentLevels", 0, CITUtils.MAX_ENCHANTMENTS - 1);

        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            String name = (String) entry.getKey();
            if (name.startsWith(NBTRule.NBT_RULE_PREFIX)) {
                value = (String) entry.getValue();
                NBTRule rule = NBTRule.create(name, value);
                if (rule == null) {
                    error("invalid nbt rule: %s", value);
                } else {
                    nbtRules.add(rule);
                }
            }
        }
    }

    public int compareTo(OverrideBase o) {
        int result = o.weight - weight;
        if (result != 0) {
            return result;
        }
        return propertiesName.toString().compareTo(o.propertiesName.toString());
    }

    boolean match(ItemStack itemStack, int[] itemEnchantmentLevels, boolean hasEffect) {
        return matchDamage(itemStack) &&
            matchDamagePercent(itemStack) &&
            matchStackSize(itemStack) &&
            matchEnchantment(itemEnchantmentLevels, hasEffect) &&
            matchNBT(itemStack);
    }

    String preprocessAltTextureKey(String name) {
        return name;
    }

    private Map<String, ResourceLocation> getAlternateTextures(Properties properties) {
        Map<String, ResourceLocation> tmpMap = new HashMap<String, ResourceLocation>();
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            String name;
            if (key.startsWith("source.")) {
                name = key.substring(7);
            } else if (key.startsWith("texture.")) {
                name = key.substring(8);
            } else if (key.startsWith("tile.")) {
                name = key.substring(5);
            } else {
                continue;
            }
            name = preprocessAltTextureKey(name);
            if (MCPatcherUtils.isNullOrEmpty(name)) {
                continue;
            }
            ResourceLocation resource = TileLoader.parseTileAddress(propertiesName, value);
            if (resource != null) {
                tmpMap.put(name, resource);
            }
        }
        return tmpMap.isEmpty() ? null : tmpMap;
    }

    private boolean matchDamage(ItemStack itemStack) {
        return damage == null || damage.get(itemStack.getItemDamage() & damageMask);
    }

    private boolean matchDamagePercent(ItemStack itemStack) {
        if (damagePercent == null) {
            return true;
        }
        int maxDamage = itemStack.getMaxDamage();
        if (maxDamage == 0) {
            return false;
        }
        int percent = (100 * itemStack.getItemDamage()) / maxDamage;
        if (percent < 0) {
            percent = 0;
        } else if (percent > 100) {
            percent = 100;
        }
        return damagePercent.get(percent);
    }

    private boolean matchStackSize(ItemStack itemStack) {
        return stackSize == null || stackSize.get(itemStack.stackSize);
    }

    private boolean matchEnchantment(int[] itemEnchantmentLevels, boolean hasEffect) {
        if (enchantmentLevels == null && enchantmentIDs == null) {
            return true;
        } else if (itemEnchantmentLevels == null) {
            return (lastEnchantmentLevel = getEnchantmentLevelMatch(hasEffect)) >= 0;
        } else {
            return (lastEnchantmentLevel = getEnchantmentLevelMatch(itemEnchantmentLevels)) >= 0;
        }
    }

    private int getEnchantmentLevelMatch(boolean hasEffect) {
        if (hasEffect && enchantmentIDs == null && enchantmentLevels.get(1)) {
            return 1;
        } else {
            return -1;
        }
    }

    private int getEnchantmentLevelMatch(int[] itemEnchantmentLevels) {
        int matchLevel = -1;
        if (enchantmentIDs == null) {
            int sum = 0;
            for (int level : itemEnchantmentLevels) {
                sum += level;
            }
            if (enchantmentLevels.get(sum)) {
                return sum;
            }
        } else if (enchantmentLevels == null) {
            for (int id = enchantmentIDs.nextSetBit(0); id >= 0; id = enchantmentIDs.nextSetBit(id + 1)) {
                if (itemEnchantmentLevels[id] > 0) {
                    matchLevel = Math.max(matchLevel, itemEnchantmentLevels[id]);
                }
            }
        } else {
            for (int id = enchantmentIDs.nextSetBit(0); id >= 0; id = enchantmentIDs.nextSetBit(id + 1)) {
                if (enchantmentLevels.get(itemEnchantmentLevels[id])) {
                    matchLevel = Math.max(matchLevel, itemEnchantmentLevels[id]);
                }
            }
        }
        return matchLevel;
    }

    private boolean matchNBT(ItemStack itemStack) {
        for (NBTRule rule : nbtRules) {
            if (!rule.match(itemStack.stackTagCompound)) {
                return false;
            }
        }
        return true;
    }

    abstract String getType();

    @Override
    public String toString() {
        return String.format("ItemOverride{%s, %s, %s}", getType(), propertiesName, textureName);
    }

    void error(String format, Object... o) {
        error = true;
        logger.error(propertiesName + ": " + format, o);
    }

    private static BitSet parseBitSet(Properties properties, String tag, int min, int max) {
        String value = MCPatcherUtils.getStringProperty(properties, tag, "");
        return parseBitSet(value, min, max);
    }

    private static BitSet parseBitSet(String value, int min, int max) {
        if (value.equals("")) {
            return null;
        }
        BitSet bits = new BitSet();
        for (int i : MCPatcherUtils.parseIntegerList(value, min, max)) {
            bits.set(i);
        }
        return bits;
    }
}
