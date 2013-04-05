package com.prupe.mcpatcher.mod;

import com.prupe.mcpatcher.*;
import net.minecraft.src.*;

import java.util.*;

public class CITUtils {
    private static final MCLogger logger = MCLogger.getLogger(MCPatcherUtils.CUSTOM_ITEM_TEXTURES, "CIT");

    private static final int MAX_ENCHANTMENTS = 256;
    private static final int ITEM_ID_POTION = 373;

    private static final boolean enable = Config.getBoolean(MCPatcherUtils.CONNECTED_TEXTURES, "items", true);

    private static TileLoader tileLoader;
    private static final ItemOverride[][] overrides = new ItemOverride[Item.itemsList.length][];
    private static final ItemOverlay[][] enchantmentOverlays = new ItemOverlay[MAX_ENCHANTMENTS][];
    private static final ItemOverlay testOverlay = new ItemOverlay("/cit/overlay.properties", "%blur%/cit/overlay.png", BlendMethod.DODGE, 33.0f, 1.0f);

    private static ItemOverlay armorOverlay;

    public static Icon getIcon(Icon icon, Item item, ItemStack itemStack) {
        if (enable) {
            int itemID = item.itemID;
            if (itemID >= 0 && itemID < overrides.length && overrides[itemID] != null) {
                for (ItemOverride override : overrides[itemID]) {
                    if (override.match(icon, itemID, itemStack)) {
                        return override.icon;
                    }
                }
            }
        }
        return icon;
    }

    private static float getFade() {
        return (float) Math.sin((System.currentTimeMillis() % 1000L) / 1000.0 * 2.0 * Math.PI);
    }

    public static boolean renderOverlayHeld(ItemStack itemStack) {
        if (!enable || itemStack == null) {
            return false;
        }
        ItemOverlay.beginOuter3D();
        testOverlay.render3D(Tessellator.instance, getFade(), 256, 256);
        ItemOverlay.endOuter3D();
        return true;
    }

    public static boolean renderOverlayDropped(ItemStack itemStack) {
        if (!enable || itemStack == null) {
            return false;
        }
        ItemOverlay.beginOuter3D();
        testOverlay.render3D(Tessellator.instance, getFade(), 256, 256);
        ItemOverlay.endOuter3D();
        return true;
    }

    public static boolean renderOverlayGUI(ItemStack itemStack, int x, int y, float z) {
        if (!enable || itemStack == null) {
            return false;
        }
        ItemOverlay.beginOuter2D();
        testOverlay.render2D(Tessellator.instance, getFade(), x - 2, y - 2, x + 18, y + 18, z - 50.0f);
        ItemOverlay.endOuter2D();
        return true;
    }

    public static boolean preRenderArmor(EntityLiving entity, int pass) {
        armorOverlay = null;
        if (!enable) {
            return false;
        }
        ItemStack itemStack = entity.getCurrentArmor(3 - pass);
        if (itemStack == null) {
            return false;
        }
        testOverlay.beginArmor(getFade());
        armorOverlay = testOverlay;
        return true;
    }

    public static void postRenderArmor() {
        if (armorOverlay != null) {
            armorOverlay.endArmor();
            armorOverlay = null;
        }
    }

    static void refresh() {
        tileLoader = new TileLoader(logger);
        Arrays.fill(overrides, null);
        Arrays.fill(enchantmentOverlays, null);
        if (enable) {
            for (String s : TexturePackAPI.listResources("/cit", ".properties")) {
                ItemOverride override = ItemOverride.create(s);
                if (override != null) {
                    for (int i : override.itemsIDs) {
                        overrides[i] = registerOverride(overrides[i], override);
                        logger.fine("registered %s to item %d (%s)", override, i, getItemName(i));
                    }
                }
            }
        }
    }

    private static ItemOverride[] registerOverride(ItemOverride[] list, ItemOverride override) {
        if (override != null) {
            if (list == null) {
                list = new ItemOverride[]{override};
            } else {
                ItemOverride[] newList = new ItemOverride[list.length + 1];
                System.arraycopy(list, 0, newList, 0, list.length);
                newList[list.length] = override;
                list = newList;
            }
        }
        return list;
    }

    private static String getItemName(int itemID) {
        if (itemID >= 0 && itemID < Item.itemsList.length) {
            Item item = Item.itemsList[itemID];
            if (item != null) {
                String name = item.getItemName();
                if (name != null) {
                    return name;
                }
            }
        }
        return "unknown item " + itemID;
    }

    private static float getEnchantmentLevels(NBTTagCompound nbt, float[] levels) {
        float total = 0.0f;
        if (nbt != null) {
            Arrays.fill(levels, 0.0f);
            NBTBase base = nbt.getTag("ench");
            if (base == null) {
                base = nbt.getTag("StoredEnchantments");
            }
            if (base instanceof NBTTagList) {
                NBTTagList list = (NBTTagList) base;
                for (int i = 0; i < list.tagCount(); i++) {
                    base = list.tagAt(i);
                    if (base instanceof NBTTagCompound) {
                        short id = ((NBTTagCompound) base).getShort("id");
                        short level = ((NBTTagCompound) base).getShort("lvl");
                        if (id >= 0 && id < MAX_ENCHANTMENTS && level > 0) {
                            levels[id] += level;
                            total += level;
                        }
                    }
                }
            }
        }
        return total;
    }

    static void registerIcons(TextureMap textureMap, Stitcher stitcher, String mapName, Map<StitchHolder, List<Texture>> map) {
        if (!mapName.equals("items")) {
            return;
        }
        for (ItemOverride[] overrides1 : overrides) {
            if (overrides1 != null) {
                for (ItemOverride override : overrides1) {
                    override.registerIcon(textureMap, stitcher, map);
                }
            }
        }
        tileLoader.finish();
    }

    private static class ItemOverride {
        private static final int NBITS = 65535;

        private final String propertiesName;
        Icon icon;
        private final List<String> textureNames = new ArrayList<String>();
        final List<Integer> itemsIDs = new ArrayList<Integer>();
        private final BitSet damage;
        private final BitSet stackSize;
        private final List<String[]> nbtRules = new ArrayList<String[]>();
        private final String textureName;
        private boolean error;

        static ItemOverride create(String filename) {
            Properties properties = TexturePackAPI.getProperties(filename);
            if (properties == null) {
                return null;
            }
            ItemOverride override = new ItemOverride(filename, properties);
            return override.error ? null : override;
        }

        private ItemOverride(String propertiesName, Properties properties) {
            this.propertiesName = propertiesName;
            String directory = propertiesName.replaceFirst("/[^/]*$", "");

            String value = MCPatcherUtils.getStringProperty(properties, "source", "");
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
                        if (token.equals(getItemName(i))) {
                            itemsIDs.add(i);
                            break;
                        }
                    }
                }
            }
            if (itemsIDs.isEmpty()) {
                error("no matching items specified");
            }

            value = MCPatcherUtils.getStringProperty(properties, "damage", "");
            if (value.equals("")) {
                damage = null;
            } else {
                damage = new BitSet();
                for (int i : MCPatcherUtils.parseIntegerList(value, 0, NBITS)) {
                    damage.set(i);
                }
            }

            value = MCPatcherUtils.getStringProperty(properties, "stackSize", "");
            if (value.equals("")) {
                stackSize = null;
            } else {
                stackSize = new BitSet();
                for (int i : MCPatcherUtils.parseIntegerList(value, 0, NBITS)) {
                    stackSize.set(i);
                }
            }

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

            if (!error) {
                tileLoader.preload(textureName, textureNames, false);
            }
        }

        void registerIcon(TextureMap textureMap, Stitcher stitcher, Map<StitchHolder, List<Texture>> map) {
            Icon[] icons = tileLoader.registerIcons(textureMap, stitcher, map, textureNames);
            icon = icons[0];
        }

        boolean match(Icon icon, int itemID, ItemStack itemStack) {
            if (damage != null && !damage.get(itemStack.getItemDamage())) {
                return false;
            }
            if (stackSize != null && !stackSize.get(itemStack.stackSize)) {
                return false;
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
            return String.format("ItemOverride{%s, %s}", propertiesName, textureName);
        }

        private void error(String format, Object... o) {
            error = true;
            logger.error(propertiesName + ": " + format, o);
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
}
