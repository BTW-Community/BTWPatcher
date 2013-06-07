package com.prupe.mcpatcher.cit;

import com.prupe.mcpatcher.*;
import net.minecraft.src.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class CITUtils {
    private static final MCLogger logger = MCLogger.getLogger(MCPatcherUtils.CUSTOM_ITEM_TEXTURES, "CIT");

    static final String CIT_PROPERTIES = "cit.properties";
    static final int MAX_ITEMS = Item.itemsList.length;
    static final int MAX_ENCHANTMENTS = 256;
    static int LOWEST_ITEM_ID;
    static int HIGHEST_ITEM_ID;
    static final Map<String, Integer> itemNameMap = new HashMap<String, Integer>();

    static final int ITEM_ID_COMPASS = 345;
    static final int ITEM_ID_CLOCK = 347;
    static final int ITEM_ID_ENCHANTED_BOOK = 403;

    private static final boolean enableItems = Config.getBoolean(MCPatcherUtils.CUSTOM_ITEM_TEXTURES, "items", true);
    private static final boolean enableEnchantments = Config.getBoolean(MCPatcherUtils.CUSTOM_ITEM_TEXTURES, "enchantments", true);
    private static final boolean enableArmor = Config.getBoolean(MCPatcherUtils.CUSTOM_ITEM_TEXTURES, "armor", true);

    private static TileLoader tileLoader;
    private static final OverrideBase[][] items = new OverrideBase[MAX_ITEMS][];
    private static final OverrideBase[][] enchantments = new OverrideBase[MAX_ITEMS][];
    private static final OverrideBase[][] armors = new OverrideBase[MAX_ITEMS][];

    private static boolean useGlint;

    private static EnchantmentList armorMatches;
    private static int armorMatchIndex;

    private static int lastRenderPass;
    private static Icon lastIcon;

    static {
        TexturePackChangeHandler.register(new TexturePackChangeHandler(MCPatcherUtils.CUSTOM_ITEM_TEXTURES, 3) {
            @Override
            public void beforeChange() {
                tileLoader = new TileLoader("textures/items/", false, logger);
                Arrays.fill(items, null);
                Arrays.fill(enchantments, null);
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
                if (enableItems || enableEnchantments || enableArmor) {
                    for (String path : TexturePackAPI.listResources(MCPatcherUtils.TEXTURE_PACK_PREFIX + "cit", ".properties", true, false, true)) {
                        registerOverride(OverrideBase.create(path));
                    }
                    if (enableItems) {
                        PotionReplacer replacer = new PotionReplacer();
                        for (OverrideBase override : replacer.overrides) {
                            registerOverride(override);
                        }
                    }
                }
            }

            @Override
            public void afterChange() {
                for (OverrideBase[] overrides1 : items) {
                    if (overrides1 != null) {
                        for (OverrideBase override : overrides1) {
                            ((ItemOverride) override).registerIcon(tileLoader);
                        }
                    }
                }
                Properties properties = TexturePackAPI.getProperties(MCPatcherUtils.TEXTURE_PACK_PREFIX + CIT_PROPERTIES);
                if (properties == null) {
                    properties = TexturePackAPI.getProperties(MCPatcherUtils.TEXTURE_PACK_PREFIX + "cit/" + CIT_PROPERTIES);
                }
                useGlint = MCPatcherUtils.getBooleanProperty(properties, "useGlint", true);
                EnchantmentList.setProperties(properties);
                sortOverrides(items);
                sortOverrides(enchantments);
                sortOverrides(armors);
            }

            private void registerOverride(OverrideBase override) {
                if (override != null && !override.error) {
                    OverrideBase[][] list;
                    switch (override.type) {
                        case OverrideBase.ITEM:
                            if (!enableItems) {
                                return;
                            }
                            ((ItemOverride) override).preload(tileLoader);
                            list = items;
                            break;

                        case OverrideBase.ENCHANTMENT:
                            if (!enableEnchantments) {
                                return;
                            }
                            list = enchantments;
                            break;

                        case OverrideBase.ARMOR:
                            if (!enableArmor) {
                                return;
                            }
                            list = armors;
                            break;

                        default:
                            logger.severe("unknown ItemOverride type %d", override.type);
                            return;
                    }
                    if (override.itemsIDs == null) {
                        logger.fine("registered %s to all items", override);
                        for (int i = LOWEST_ITEM_ID; i <= HIGHEST_ITEM_ID; i++) {
                            registerOverride(list, i, override);
                        }
                    } else {
                        int j = 0;
                        for (int i = override.itemsIDs.nextSetBit(0); i >= 0; i = override.itemsIDs.nextSetBit(i + 1)) {
                            registerOverride(list, i, override);
                            if (j < 10) {
                                logger.fine("registered %s to item %d (%s)", override, i, getItemName(i));
                            } else if (j == 10) {
                                logger.fine("... %d total", override.itemsIDs.cardinality());
                            }
                            j++;
                        }
                    }
                }
            }

            private void registerOverride(OverrideBase[][] list, int itemID, OverrideBase override) {
                if (Item.itemsList[itemID] != null) {
                    list[itemID] = registerOverride(list[itemID], override);
                }
            }

            private OverrideBase[] registerOverride(OverrideBase[] list, OverrideBase override) {
                if (override != null) {
                    if (list == null) {
                        list = new OverrideBase[]{override};
                    } else {
                        OverrideBase[] newList = new OverrideBase[list.length + 1];
                        System.arraycopy(list, 0, newList, 0, list.length);
                        newList[list.length] = override;
                        list = newList;
                    }
                }
                return list;
            }

            private void sortOverrides(OverrideBase[][] overrides) {
                for (OverrideBase[] list : overrides) {
                    if (list != null) {
                        Arrays.sort(list);
                    }
                }
            }
        });
    }

    public static void init() {
    }

    public static Icon getIcon(Icon icon, ItemStack itemStack, int renderPass) {
        lastIcon = icon;
        if (enableItems) {
            OverrideBase override = findMatch(items, itemStack, renderPass);
            if (override != null) {
                lastIcon = ((ItemOverride) override).icon;
            }
        }
        return lastIcon;
    }

    public static String getArmorTexture(String texture, EntityLiving entity, ItemStack itemStack) {
        if (enableArmor) {
            int layer = texture.endsWith("_b.png") ? 1 : 0;
            OverrideBase override = findMatch(armors, itemStack, layer);
            if (override != null) {
                return override.textureName;
            }
        }
        return texture;
    }

    private static OverrideBase findMatch(OverrideBase[][] overrides, ItemStack itemStack, int renderPass) {
        lastRenderPass = renderPass;
        int itemID = itemStack.itemID;
        if (itemID >= 0 && itemID < overrides.length && overrides[itemID] != null) {
            int[] enchantmentLevels = getEnchantmentLevels(itemID, itemStack.stackTagCompound);
            boolean hasEffect = itemStack.hasEffect();
            for (OverrideBase override : overrides[itemID]) {
                if (override.layer == renderPass && override.match(itemStack, enchantmentLevels, hasEffect)) {
                    return override;
                }
            }
        }
        return null;
    }

    public static boolean renderEnchantmentHeld(ItemStack itemStack, int renderPass) {
        if (itemStack == null || renderPass != 0) {
            return true;
        }
        if (!enableEnchantments) {
            return false;
        }
        EnchantmentList matches = new EnchantmentList(enchantments, itemStack);
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
        Enchantment.beginOuter3D();
        for (int i = 0; i < matches.size(); i++) {
            matches.getEnchantment(i).render3D(Tessellator.instance, matches.getIntensity(i), width, height);
        }
        Enchantment.endOuter3D();
        return !useGlint;
    }

    public static boolean renderEnchantmentDropped(ItemStack itemStack) {
        return renderEnchantmentHeld(itemStack, lastRenderPass);
    }

    public static boolean renderEnchantmentGUI(ItemStack itemStack, int x, int y, float z) {
        if (!enableEnchantments || itemStack == null) {
            return false;
        }
        EnchantmentList matches = new EnchantmentList(enchantments, itemStack);
        if (matches.isEmpty()) {
            return false;
        }
        Enchantment.beginOuter2D();
        for (int i = 0; i < matches.size(); i++) {
            matches.getEnchantment(i).render2D(Tessellator.instance, matches.getIntensity(i), x - 2, y - 2, x + 18, y + 18, z - 50.0f);
        }
        Enchantment.endOuter2D();
        return !useGlint;
    }

    public static boolean setupArmorEnchantments(EntityLiving entity, int pass) {
        if (!enableEnchantments || !(entity instanceof EntityLivingSub)) {
            return false;
        }
        ItemStack itemStack = ((EntityLivingSub) entity).getCurrentArmor(3 - pass);
        if (itemStack == null) {
            return false;
        }
        armorMatches = new EnchantmentList(enchantments, itemStack);
        armorMatchIndex = 0;
        return !armorMatches.isEmpty();
    }

    public static boolean preRenderArmorEnchantment() {
        if (armorMatchIndex < armorMatches.size()) {
            Enchantment enchantment = armorMatches.getEnchantment(armorMatchIndex);
            enchantment.beginArmor(armorMatches.getIntensity(armorMatchIndex));
            return true;
        } else {
            armorMatches = null;
            armorMatchIndex = 0;
            return false;
        }
    }

    public static void postRenderArmorEnchantment() {
        armorMatches.getEnchantment(armorMatchIndex).endArmor();
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

    static int[] getEnchantmentLevels(int itemID, NBTTagCompound nbt) {
        int[] levels = null;
        if (nbt != null) {
            NBTBase base;
            if (itemID == ITEM_ID_ENCHANTED_BOOK) {
                base = nbt.getTag("StoredEnchantments");
            } else {
                base = nbt.getTag("ench");
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
