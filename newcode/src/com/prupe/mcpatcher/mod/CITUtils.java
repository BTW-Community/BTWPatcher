package com.prupe.mcpatcher.mod;

import com.prupe.mcpatcher.*;
import net.minecraft.src.*;

import java.util.*;

public class CITUtils {
    private static final MCLogger logger = MCLogger.getLogger(MCPatcherUtils.CUSTOM_ITEM_TEXTURES, "CIT");

    static final int MAX_ITEMS = Item.itemsList.length;
    static final int MAX_ENCHANTMENTS = 256;
    private static final int ITEM_ID_POTION = 373;

    private static final boolean enableItems = Config.getBoolean(MCPatcherUtils.CONNECTED_TEXTURES, "items", true);
    private static final boolean enableOverlays = Config.getBoolean(MCPatcherUtils.CONNECTED_TEXTURES, "itemOverlays", true);
    private static final boolean enableArmor = Config.getBoolean(MCPatcherUtils.CONNECTED_TEXTURES, "armor", true);

    static TileLoader tileLoader;
    private static final ItemOverride[][] items = new ItemOverride[MAX_ITEMS][];
    private static final ItemOverride[][] overlays = new ItemOverride[MAX_ITEMS][];
    private static final ItemOverride[][] armors = new ItemOverride[MAX_ITEMS][];

    private static Matches armorMatches;
    private static int armorMatchIndex;

    public static Icon getIcon(Icon icon, Item item, ItemStack itemStack) {
        if (enableItems) {
            int itemID = item.itemID;
            if (itemID >= 0 && itemID < items.length && items[itemID] != null) {
                int[] enchantmentLevels = getEnchantmentLevels(itemStack.stackTagCompound);
                for (ItemOverride override : items[itemID]) {
                    if (override.match(itemID, itemStack, enchantmentLevels)) {
                        return override.icon;
                    }
                }
            }
        }
        return icon;
    }

    public static String getArmorTexture(String texture, ItemStack itemStack) {
        if (enableArmor) {
            int itemID = itemStack.itemID;
            if (itemID >= 0 && itemID < armors.length && armors[itemID] != null) {
                int[] enchantmentLevels = getEnchantmentLevels(itemStack.stackTagCompound);
                for (ItemOverride override : armors[itemID]) {
                    if (override.match(itemID, itemStack, enchantmentLevels)) {
                        return override.textureName;
                    }
                }
            }
        }
        return texture;
    }

    private static class Matches {
        private final List<ItemOverride> matches = new ArrayList<ItemOverride>();
        private final List<Integer> levels = new ArrayList<Integer>();
        private int total;

        Matches(ItemStack itemStack) {
            int[] enchantmentLevels = getEnchantmentLevels(itemStack.stackTagCompound);
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

        ItemOverride getOverride(int index) {
            return matches.get(index);
        }

        float getFade(int index) {
            return (float) levels.get(index) / (float) total;
        }
    }

    public static boolean renderOverlayHeld(ItemStack itemStack) {
        if (!enableOverlays || itemStack == null) {
            return false;
        }
        Matches matches = new Matches(itemStack);
        if (matches.isEmpty()) {
            return false;
        }
        ItemOverlay.beginOuter3D();
        for (int i = 0; i < matches.size(); i++) {
            matches.getOverride(i).overlay.render3D(Tessellator.instance, matches.getFade(i), 256, 256);
        }
        ItemOverlay.endOuter3D();
        return true;
    }

    public static boolean renderOverlayDropped(ItemStack itemStack) {
        return renderOverlayHeld(itemStack);
    }

    public static boolean renderOverlayGUI(ItemStack itemStack, int x, int y, float z) {
        if (!enableOverlays || itemStack == null) {
            return false;
        }
        Matches matches = new Matches(itemStack);
        if (matches.isEmpty()) {
            return false;
        }
        ItemOverlay.beginOuter2D();
        for (int i = 0; i < matches.size(); i++) {
            matches.getOverride(i).overlay.render2D(Tessellator.instance, matches.getFade(i), x - 2, y - 2, x + 18, y + 18, z - 50.0f);
        }
        ItemOverlay.endOuter2D();
        return true;
    }

    public static boolean setupArmorOverlays(EntityLiving entity, int pass) {
        if (!enableOverlays || entity == null) {
            return false;
        }
        ItemStack itemStack = entity.getCurrentArmor(3 - pass);
        if (itemStack == null) {
            return false;
        }
        armorMatches = new Matches(itemStack);
        armorMatchIndex = 0;
        return !armorMatches.isEmpty();
    }

    public static boolean preRenderArmorOverlay() {
        if (armorMatchIndex < armorMatches.size()) {
            ItemOverlay overlay = armorMatches.getOverride(armorMatchIndex).overlay;
            overlay.beginArmor(armorMatches.getFade(armorMatchIndex));
            return true;
        } else {
            armorMatches = null;
            armorMatchIndex = 0;
            return false;
        }
    }

    public static void postRenderArmorOverlay() {
        armorMatches.getOverride(armorMatchIndex).overlay.endArmor();
        armorMatchIndex++;
    }

    static void refresh() {
        tileLoader = new TileLoader(logger);
        Arrays.fill(items, null);
        Arrays.fill(overlays, null);
        Arrays.fill(armors, null);
        if (enableItems || enableOverlays || enableArmor) {
            List<String> paths = new ArrayList<String>();
            loadOverridesFromPath("/cit", paths);
            Collections.sort(paths, new Comparator<String>() {
                public int compare(String o1, String o2) {
                    o1 = o1.replaceAll(".*/", "").replaceFirst("\\.properties$", "");
                    o2 = o2.replaceAll(".*/", "").replaceFirst("\\.properties$", "");
                    return o1.compareTo(o2);
                }
            });
            for (String path : paths) {
                ItemOverride override = ItemOverride.create(path);
                if (override != null) {
                    ItemOverride[][] list;
                    switch (override.type) {
                        case ItemOverride.ITEM:
                            override.preload(tileLoader);
                            list = items;
                            break;

                        case ItemOverride.OVERLAY:
                            list = overlays;
                            break;

                        case ItemOverride.ARMOR:
                            list = armors;
                            break;

                        default:
                            logger.severe("unknown ItemOverride type %d", override.type);
                            continue;
                    }
                    for (int i : override.itemsIDs) {
                        list[i] = registerOverride(list[i], override);
                        logger.fine("registered %s to item %d (%s)", override, i, getItemName(i));
                    }
                }
            }
        }
    }

    private static void loadOverridesFromPath(String directory, List<String> paths) {
        Collections.addAll(paths, TexturePackAPI.listResources(directory, ".properties"));
        for (String subdir : TexturePackAPI.listDirectories(directory)) {
            loadOverridesFromPath(subdir, paths);
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

    static String getItemName(int itemID) {
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

    private static int[] getEnchantmentLevels(NBTTagCompound nbt) {
        int[] levels = null;
        if (nbt != null) {
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
                            if (levels == null) {
                                levels = new int[MAX_ENCHANTMENTS];
                            }
                            levels[id] += level;
                        }
                    }
                }
            }
        }
        return levels;
    }

    static void registerIcons(TextureMap textureMap, Stitcher stitcher, String mapName, Map<StitchHolder, List<Texture>> map) {
        if (!mapName.equals("items")) {
            return;
        }
        for (ItemOverride[] overrides1 : items) {
            if (overrides1 != null) {
                for (ItemOverride override : overrides1) {
                    override.registerIcon(textureMap, stitcher, map);
                }
            }
        }
        tileLoader.finish();
    }
}
