package com.prupe.mcpatcher.cit;

import com.prupe.mcpatcher.MCLogger;
import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.TexturePackAPI;
import com.prupe.mcpatcher.TileLoader;
import net.minecraft.src.*;

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
    final BitSet itemsIDs;
    final BitSet damage;
    final int damageMask;
    final BitSet stackSize;
    final BitSet enchantmentIDs;
    final BitSet enchantmentLevels;
    final List<String[]> nbtRules = new ArrayList<String[]>();
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

        String value = MCPatcherUtils.getStringProperty(properties, "source", "");
        ResourceLocation resource;
        if (value.equals("")) {
            value = MCPatcherUtils.getStringProperty(properties, "texture", "");
        }
        if (value.equals("")) {
            value = MCPatcherUtils.getStringProperty(properties, "tile", "");
        }
        if (value.equals("")) {
            resource = TileLoader.getDefaultAddress(propertiesName);
            if (!TexturePackAPI.hasResource(resource)) {
                resource = null;
            }
        } else {
            resource = TileLoader.parseTileAddress(propertiesName, value);
            if (!TexturePackAPI.hasResource(resource)) {
                error("source texture %s not found", value);
                resource = null;
            }
        }
        textureName = resource;
        alternateTextures = getAlternateTextures(properties);

        weight = MCPatcherUtils.getIntProperty(properties, "weight", 0);

        value = MCPatcherUtils.getStringProperty(properties, "items", "");
        if (value.equals("")) {
            value = MCPatcherUtils.getStringProperty(properties, "matchItems", "");
        }
        if (value.equals("")) {
            itemsIDs = null;
        } else {
            BitSet ids = parseBitSet(value, CITUtils.LOWEST_ITEM_ID, CITUtils.HIGHEST_ITEM_ID);
            boolean all = true;
            for (int i = CITUtils.LOWEST_ITEM_ID; i <= CITUtils.HIGHEST_ITEM_ID; i++) {
                if (Item.itemsList[i] != null && !ids.get(i)) {
                    all = false;
                    break;
                }
            }
            itemsIDs = all ? null : ids;
        }

        damage = parseBitSet(properties, "damage", 0, MAX_DAMAGE);
        damageMask = MCPatcherUtils.getIntProperty(properties, "damageMask", MAX_DAMAGE);
        stackSize = parseBitSet(properties, "stackSize", 0, MAX_STACK_SIZE);
        enchantmentIDs = parseBitSet(properties, "enchantmentIDs", 0, CITUtils.MAX_ENCHANTMENTS - 1);
        enchantmentLevels = parseBitSet(properties, "enchantmentLevels", 0, CITUtils.MAX_ENCHANTMENTS - 1);

        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            String name = (String) entry.getKey();
            value = (String) entry.getValue();
            if (name.startsWith("nbt.")) {
                String[] rule = name.split("\\.");
                if (rule.length > 1) {
                    rule[0] = value;
                    for (int i = 1; i < rule.length; i++) {
                        if ("*".equals(rule[i])) {
                            rule[i] = null;
                        }
                    }
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
        for (String[] rule : nbtRules) {
            if (!matchNBTTagCompound(rule, 1, rule[0], itemStack.stackTagCompound)) {
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

    private static boolean matchNBT(String[] rule, int index, String value, NBTBase nbt) {
        if (nbt instanceof NBTTagByte) {
            return matchNBTTagByte(rule, index, value, (NBTTagByte) nbt);
        } else if (nbt instanceof NBTTagCompound) {
            return matchNBTTagCompound(rule, index, value, (NBTTagCompound) nbt);
        } else if (nbt instanceof NBTTagList) {
            return matchNBTTagList(rule, index, value, (NBTTagList) nbt);
        } else if (nbt instanceof NBTTagDouble) {
            return matchNBTTagDouble(rule, index, value, (NBTTagDouble) nbt);
        } else if (nbt instanceof NBTTagFloat) {
            return matchNBTTagFloat(rule, index, value, (NBTTagFloat) nbt);
        } else if (nbt instanceof NBTTagInteger) {
            return matchNBTTagInteger(rule, index, value, (NBTTagInteger) nbt);
        } else if (nbt instanceof NBTTagLong) {
            return matchNBTTagLong(rule, index, value, (NBTTagLong) nbt);
        } else if (nbt instanceof NBTTagShort) {
            return matchNBTTagShort(rule, index, value, (NBTTagShort) nbt);
        } else if (nbt instanceof NBTTagString) {
            return matchNBTTagString(rule, index, value, (NBTTagString) nbt);
        } else {
            return false;
        }
    }

    private static boolean matchNBTTagCompound(String[] rule, int index, String value, NBTTagCompound nbt) {
        if (nbt == null || index >= rule.length) {
            return false;
        }
        if (rule[index] == null) {
            for (NBTBase nbtBase : nbt.getTags()) {
                if (matchNBT(rule, index + 1, value, nbtBase)) {
                    return true;
                }
            }
        } else {
            return matchNBT(rule, index + 1, value, nbt.getTag(rule[index]));
        }
        return false;
    }

    private static boolean matchNBTTagList(String[] rule, int index, String value, NBTTagList nbt) {
        if (index >= rule.length) {
            return false;
        }
        if (rule[index] == null) {
            for (int i = 0; i < nbt.tagCount(); i++) {
                if (matchNBT(rule, index + 1, value, nbt.tagAt(i))) {
                    return true;
                }
            }
        } else {
            try {
                int tagNum = Integer.parseInt(rule[index]);
                return tagNum >= 0 && tagNum < nbt.tagCount() && matchNBT(rule, index + 1, value, nbt.tagAt(tagNum));
            } catch (NumberFormatException e) {
            }
        }
        return false;
    }

    private static boolean matchNBTTagByte(String[] rule, int index, String value, NBTTagByte nbt) {
        try {
            return nbt.data == Byte.parseByte(value);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean matchNBTTagDouble(String[] rule, int index, String value, NBTTagDouble nbt) {
        try {
            return nbt.data == Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean matchNBTTagFloat(String[] rule, int index, String value, NBTTagFloat nbt) {
        try {
            return nbt.data == Float.parseFloat(value);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean matchNBTTagInteger(String[] rule, int index, String value, NBTTagInteger nbt) {
        try {
            return nbt.data == Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean matchNBTTagLong(String[] rule, int index, String value, NBTTagLong nbt) {
        try {
            return nbt.data == Long.parseLong(value);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean matchNBTTagShort(String[] rule, int index, String value, NBTTagShort nbt) {
        try {
            return nbt.data == Short.parseShort(value);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean matchNBTTagString(String[] rule, int index, String value, NBTTagString nbt) {
        return value.equals(nbt.data);
    }
}
