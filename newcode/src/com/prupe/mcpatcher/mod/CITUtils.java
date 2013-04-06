package com.prupe.mcpatcher.mod;

import com.prupe.mcpatcher.*;
import net.minecraft.src.*;

import java.util.*;

public class CITUtils {
    private static final MCLogger logger = MCLogger.getLogger(MCPatcherUtils.CUSTOM_ITEM_TEXTURES, "CIT");

    static final int MAX_ENCHANTMENTS = 256;
    private static final int ITEM_ID_POTION = 373;

    private static final boolean enableItems = Config.getBoolean(MCPatcherUtils.CONNECTED_TEXTURES, "items", true);
    private static final boolean enableOverlays = Config.getBoolean(MCPatcherUtils.CONNECTED_TEXTURES, "itemOverlays", true);
    private static final boolean enableArmor = Config.getBoolean(MCPatcherUtils.CONNECTED_TEXTURES, "armor", true);

    static TileLoader tileLoader;
    private static final ItemOverride[][] overrides = new ItemOverride[Item.itemsList.length][];
    private static final ItemOverlay[][] enchantmentOverlays = new ItemOverlay[MAX_ENCHANTMENTS][];
    private static final ItemOverlay testOverlay = new ItemOverlay("/cit/overlay.properties", "%blur%/cit/overlay.png", BlendMethod.DODGE, 33.0f, 1.0f);

    private static ItemOverlay armorOverlay;

    public static Icon getIcon(Icon icon, Item item, ItemStack itemStack) {
        if (enableItems) {
            int[] enchantmentLevels = getEnchantmentLevels(itemStack.stackTagCompound);
            int itemID = item.itemID;
            if (itemID >= 0 && itemID < overrides.length && overrides[itemID] != null) {
                for (ItemOverride override : overrides[itemID]) {
                    if (override.match(icon, itemID, itemStack, enchantmentLevels)) {
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
        if (!enableOverlays || itemStack == null) {
            return false;
        }
        ItemOverlay.beginOuter3D();
        testOverlay.render3D(Tessellator.instance, getFade(), 256, 256);
        ItemOverlay.endOuter3D();
        return true;
    }

    public static boolean renderOverlayDropped(ItemStack itemStack) {
        if (!enableOverlays || itemStack == null) {
            return false;
        }
        ItemOverlay.beginOuter3D();
        testOverlay.render3D(Tessellator.instance, getFade(), 256, 256);
        ItemOverlay.endOuter3D();
        return true;
    }

    public static boolean renderOverlayGUI(ItemStack itemStack, int x, int y, float z) {
        if (!enableOverlays || itemStack == null) {
            return false;
        }
        ItemOverlay.beginOuter2D();
        testOverlay.render2D(Tessellator.instance, getFade(), x - 2, y - 2, x + 18, y + 18, z - 50.0f);
        ItemOverlay.endOuter2D();
        return true;
    }

    public static boolean preRenderArmor(EntityLiving entity, int pass) {
        armorOverlay = null;
        if (!enableOverlays) {
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
                    for (int i : override.itemsIDs) {
                        overrides[i] = registerOverride(overrides[i], override);
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
        for (ItemOverride[] overrides1 : overrides) {
            if (overrides1 != null) {
                for (ItemOverride override : overrides1) {
                    override.registerIcon(textureMap, stitcher, map);
                }
            }
        }
        tileLoader.finish();
    }
}
