package com.prupe.mcpatcher.mod;

import com.prupe.mcpatcher.*;
import net.minecraft.src.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class CITUtils {
    private static final MCLogger logger = MCLogger.getLogger(MCPatcherUtils.CUSTOM_ITEM_TEXTURES, "CIT");

    static final int MAX_ITEMS = Item.itemsList.length;
    static final int MAX_ENCHANTMENTS = 256;
    static int LOWEST_ITEM_ID;
    static int HIGHEST_ITEM_ID;
    static final Map<String, Integer> itemNameMap = new HashMap<String, Integer>();
    private static final int ITEM_ID_POTION = 373;

    private static final boolean enableItems = Config.getBoolean(MCPatcherUtils.CUSTOM_ITEM_TEXTURES, "items", true);
    private static final boolean enableOverlays = Config.getBoolean(MCPatcherUtils.CUSTOM_ITEM_TEXTURES, "overlays", true);
    private static final boolean enableArmor = Config.getBoolean(MCPatcherUtils.CUSTOM_ITEM_TEXTURES, "armor", true);

    static TileLoader tileLoader;
    private static final ItemOverride[][] items = new ItemOverride[MAX_ITEMS][];
    private static final ItemOverride[][] overlays = new ItemOverride[MAX_ITEMS][];
    private static final ItemOverride[][] armors = new ItemOverride[MAX_ITEMS][];

    private static ItemOverlayList armorMatches;
    private static int armorMatchIndex;

    private static Icon lastIcon;

    static {
        TexturePackChangeHandler.register(new TexturePackChangeHandler(MCPatcherUtils.CUSTOM_ITEM_TEXTURES, 3) {
            @Override
            public void beforeChange() {
                tileLoader = new TileLoader("blocks/items/", false, logger);
                Arrays.fill(items, null);
                Arrays.fill(overlays, null);
                Arrays.fill(armors, null);
                lastIcon = null;
                itemNameMap.clear();
                for (LOWEST_ITEM_ID = 256; LOWEST_ITEM_ID < MAX_ITEMS; LOWEST_ITEM_ID++) {
                    if (Item.itemsList[LOWEST_ITEM_ID] != null) {
                        break;
                    }
                }
                for (HIGHEST_ITEM_ID = MAX_ITEMS - 1; HIGHEST_ITEM_ID >= 0; HIGHEST_ITEM_ID--) {
                    if (Item.itemsList[HIGHEST_ITEM_ID] != null) {
                        break;
                    }
                }
                if (LOWEST_ITEM_ID <= HIGHEST_ITEM_ID) {
                    for (int i = LOWEST_ITEM_ID; i <= HIGHEST_ITEM_ID; i++) {
                        Item item = Item.itemsList[i];
                        if (item != null) {
                            String name = item.getItemName();
                            if (name != null) {
                                itemNameMap.put(name, i);
                            }
                        }
                    }
                    logger.fine("%d items, lowest used item id is %d (%s), highest is %d (%s)",
                        itemNameMap.size(),
                        LOWEST_ITEM_ID, getItemName(LOWEST_ITEM_ID),
                        HIGHEST_ITEM_ID, getItemName(HIGHEST_ITEM_ID)
                    );
                }
                if (enableItems || enableOverlays || enableArmor) {
                    for (String path : TexturePackAPI.listResources("/cit", ".properties", true, false, true)) {
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
                            if (override.itemsIDs == null) {
                                logger.fine("registered %s to all items", override);
                                for (int i = LOWEST_ITEM_ID; i <= HIGHEST_ITEM_ID; i++) {
                                    registerOverride(list, i, override);
                                }
                            } else {
                                for (int i = override.itemsIDs.nextSetBit(0); i >= 0; i = override.itemsIDs.nextSetBit(i + 1)) {
                                    registerOverride(list, i, override);
                                    logger.fine("registered %s to item %d (%s)", override, i, getItemName(i));
                                }
                            }
                        }
                    }
                }
            }

            @Override
            public void afterChange() {
                for (ItemOverride[] overrides1 : items) {
                    if (overrides1 != null) {
                        for (ItemOverride override : overrides1) {
                            override.registerIcon(tileLoader);
                        }
                    }
                }
            }

            private void registerOverride(ItemOverride[][] list, int itemID, ItemOverride override) {
                if (Item.itemsList[itemID] != null) {
                    list[itemID] = registerOverride(list[itemID], override);
                }
            }

            private ItemOverride[] registerOverride(ItemOverride[] list, ItemOverride override) {
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
        });
    }

    public static void init() {
    }

    public static Icon getIcon(Icon icon, Item item, ItemStack itemStack) {
        lastIcon = icon;
        if (enableItems) {
            ItemOverride override = findMatch(items, itemStack);
            if (override != null) {
                lastIcon = override.icon;
            }
        }
        return lastIcon;
    }

    public static String getArmorTexture(String texture, EntityLiving entity, ItemStack itemStack) {
        if (enableArmor) {
            int layer = texture.endsWith("_b.png") ? 1 : 0;
            ItemOverride override = findMatch(armors, itemStack, layer);
            if (override != null) {
                return override.textureName;
            }
        }
        return texture;
    }

    private static ItemOverride findMatch(ItemOverride[][] overrides, ItemStack itemStack) {
        int itemID = itemStack.itemID;
        if (itemID >= 0 && itemID < overrides.length && overrides[itemID] != null) {
            int[] enchantmentLevels = getEnchantmentLevels(itemStack.stackTagCompound);
            for (ItemOverride override : overrides[itemID]) {
                if (override.match(itemID, itemStack, enchantmentLevels)) {
                    return override;
                }
            }
        }
        return null;
    }

    private static ItemOverride findMatch(ItemOverride[][] overrides, ItemStack itemStack, int layer) {
        int itemID = itemStack.itemID;
        if (itemID >= 0 && itemID < overrides.length && overrides[itemID] != null) {
            int[] enchantmentLevels = getEnchantmentLevels(itemStack.stackTagCompound);
            for (ItemOverride override : overrides[itemID]) {
                if (override.armorLayer == layer && override.match(itemID, itemStack, enchantmentLevels)) {
                    return override;
                }
            }
        }
        return null;
    }

    public static boolean renderOverlayHeld(ItemStack itemStack) {
        if (!enableOverlays || itemStack == null) {
            return false;
        }
        ItemOverlayList matches = new ItemOverlayList(overlays, itemStack);
        if (matches.isEmpty()) {
            return false;
        }
        int width;
        int height;
        if (lastIcon == null) {
            width = height = 256;
        } else {
            width = lastIcon.getWidth();
            height = lastIcon.getHeight();
        }
        ItemOverlay.beginOuter3D();
        for (int i = 0; i < matches.size(); i++) {
            matches.getOverlay(i).render3D(Tessellator.instance, matches.getIntensity(i), width, height);
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
        ItemOverlayList matches = new ItemOverlayList(overlays, itemStack);
        if (matches.isEmpty()) {
            return false;
        }
        ItemOverlay.beginOuter2D();
        for (int i = 0; i < matches.size(); i++) {
            matches.getOverlay(i).render2D(Tessellator.instance, matches.getIntensity(i), x - 2, y - 2, x + 18, y + 18, z - 50.0f);
        }
        ItemOverlay.endOuter2D();
        return true;
    }

    public static boolean setupArmorOverlays(EntityLiving entity, int pass) {
        if (!enableOverlays || entity == null) {
            return false;
        }
        ItemStack itemStack = null;
        // 13w16 eliminated EntityLiving.getCurrentArmor, so this nonsense is required.
        // For 1.5, EntityLivingSub is simply mapped to EntityLiving so that the first
        // if branch will always be used.
        if (entity instanceof EntityLivingSub) {
            itemStack = ((EntityLivingSub) entity).getCurrentArmor(3 - pass);
        } else if (entity instanceof EntityPlayer) {
            itemStack = ((EntityPlayer) entity).getCurrentArmor(3 - pass);
        }
        if (itemStack == null) {
            return false;
        }
        armorMatches = new ItemOverlayList(overlays, itemStack);
        armorMatchIndex = 0;
        return !armorMatches.isEmpty();
    }

    public static boolean preRenderArmorOverlay() {
        if (armorMatchIndex < armorMatches.size()) {
            ItemOverlay overlay = armorMatches.getOverlay(armorMatchIndex);
            overlay.beginArmor(armorMatches.getIntensity(armorMatchIndex));
            return true;
        } else {
            armorMatches = null;
            armorMatchIndex = 0;
            return false;
        }
    }

    public static void postRenderArmorOverlay() {
        armorMatches.getOverlay(armorMatchIndex).endArmor();
        armorMatchIndex++;
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

    static int[] getEnchantmentLevels(NBTTagCompound nbt) {
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
}
