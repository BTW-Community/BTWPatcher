package com.prupe.mcpatcher.mod;

import com.prupe.mcpatcher.MCLogger;
import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.TexturePackAPI;
import net.minecraft.src.*;

import java.util.*;

class ItemOverride {
    private static final MCLogger logger = MCLogger.getLogger(MCPatcherUtils.CUSTOM_ITEM_TEXTURES, "CIT");

    private static final int MAX_DAMAGE = 65535;
    private static final int MAX_STACK_SIZE = 65535;

    static final int ITEM = 0;
    static final int OVERLAY = 1;
    static final int ARMOR = 2;

    private final String propertiesName;
    final int type;
    Icon icon;
    final String textureName;
    private final List<String> textureNames = new ArrayList<String>();
    final ItemOverlay overlay;
    final List<Integer> itemsIDs = new ArrayList<Integer>();
    private final BitSet damage;
    private final BitSet stackSize;
    private final BitSet enchantmentIDs;
    private final BitSet enchantmentLevels;
    private final List<String[]> nbtRules = new ArrayList<String[]>();
    private boolean error;

    int lastEnchantmentLevel;

    static ItemOverride create(String filename) {
        Properties properties = TexturePackAPI.getProperties(filename);
        if (properties == null) {
            return null;
        }
        ItemOverride override = new ItemOverride(filename, properties);
        return override.error ? null : override;
    }

    ItemOverride(String propertiesName, Properties properties) {
        this.propertiesName = propertiesName;
        String directory = propertiesName.replaceFirst("/[^/]*$", "");

        String value = MCPatcherUtils.getStringProperty(properties, "type", "item").toLowerCase();
        if (value.equals("item")) {
            type = ITEM;
        } else if (value.equals("overlay")) {
            type = OVERLAY;
        } else if (value.equals("armor")) {
            type = ARMOR;
        } else {
            error("unknown type %s", value);
            type = ITEM;
        }

        value = MCPatcherUtils.getStringProperty(properties, "source", "");
        if (value.equals("")) {
            value = MCPatcherUtils.getStringProperty(properties, "texture", "");
        }
        if (value.equals("")) {
            value = MCPatcherUtils.getStringProperty(properties, "tile", "");
        }
        if (value.equals("")) {
            value = propertiesName.replaceFirst("\\.properties$", ".png");
            if (!TexturePackAPI.hasResource(value)) {
                error("no source texture name specified");
            }
        }
        textureName = value.startsWith("/") ? value : directory + "/" + value;

        if (type == OVERLAY) {
            overlay = ItemOverlay.create(this, properties);
            if (overlay == null) {
                error = true;
            }
        } else {
            overlay = null;
        }

        value = MCPatcherUtils.getStringProperty(properties, "matchItems", "");
        for (String token : value.split("\\s+")) {
            if (token.equals("")) {
                // nothing
            } else if (token.matches("\\d+") || token.matches("\\d+-\\d+")) {
                for (int i : MCPatcherUtils.parseIntegerList(token, 0, Item.itemsList.length - 1)) {
                    itemsIDs.add(i);
                }
            } else {
                for (int i = 0; i < Item.itemsList.length; i++) {
                    if (token.equals(CITUtils.getItemName(i))) {
                        itemsIDs.add(i);
                        break;
                    }
                }
            }
        }
        if (itemsIDs.isEmpty()) {
            error("no matching items specified");
        }

        damage = parseBitSet(properties, "damage", MAX_DAMAGE);
        stackSize = parseBitSet(properties, "stackSize", MAX_STACK_SIZE);
        enchantmentIDs = parseBitSet(properties, "enchantmentIDs", CITUtils.MAX_ENCHANTMENTS - 1);
        enchantmentLevels = parseBitSet(properties, "enchantmentLevels", CITUtils.MAX_ENCHANTMENTS - 1);

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

    void preload(TileLoader tileLoader) {
        if (type == ITEM) {
            tileLoader.preload(textureName, textureNames, false);
        }
    }

    void registerIcon(TextureMap textureMap, Stitcher stitcher, Map<StitchHolder, List<Texture>> map) {
        Icon[] icons = CITUtils.tileLoader.registerIcons(textureMap, stitcher, map, textureNames);
        icon = icons[0];
    }

    boolean match(int itemID, ItemStack itemStack, int[] itemEnchantmentLevels) {
        if (damage != null && !damage.get(itemStack.getItemDamage())) {
            return false;
        }
        if (stackSize != null && !stackSize.get(itemStack.stackSize)) {
            return false;
        }
        if (itemEnchantmentLevels != null && (enchantmentIDs != null || enchantmentLevels != null)) {
            lastEnchantmentLevel = 0;
            if (enchantmentIDs == null) {
                int sum = 0;
                for (int level : itemEnchantmentLevels) {
                    sum += level;
                }
                if (!enchantmentLevels.get(sum)) {
                    return false;
                }
                lastEnchantmentLevel = sum;
            } else {
                for (int id = 0; id >= 0; id = enchantmentIDs.nextSetBit(id)) {
                    if (enchantmentLevels == null) {
                        if (itemEnchantmentLevels[id] > 0) {
                            lastEnchantmentLevel = Math.max(lastEnchantmentLevel, itemEnchantmentLevels[id]);
                        }
                    } else {
                        if (enchantmentLevels.get(itemEnchantmentLevels[id])) {
                            lastEnchantmentLevel = Math.max(lastEnchantmentLevel, itemEnchantmentLevels[id]);
                        }
                    }
                }
                if (lastEnchantmentLevel <= 0) {
                    return false;
                }
            }
        }
        for (String[] rule : nbtRules) {
            if (!matchNBTTagCompound(rule, 1, rule[0], itemStack.stackTagCompound)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        String typeString = type == ITEM ? "item" : type == OVERLAY ? "overlay" : type == ARMOR ? "armor" : "unknown type " + type;
        return String.format("ItemOverride{%s, %s, %s}", typeString, propertiesName, textureName);
    }

    void error(String format, Object... o) {
        error = true;
        logger.error(propertiesName + ": " + format, o);
    }

    private static BitSet parseBitSet(Properties properties, String tag, int max) {
        String value = MCPatcherUtils.getStringProperty(properties, tag, "");
        if (value.equals("")) {
            return null;
        }
        BitSet bits = new BitSet();
        for (int i : MCPatcherUtils.parseIntegerList(value, 0, max)) {
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
