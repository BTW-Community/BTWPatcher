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
    static final int ITEM_ID_POTION = 373;
    static final int ITEM_ID_GLASS_BOTTLE = 374;
    static final int ITEM_ID_ENCHANTED_BOOK = 403;

    private static final int[] POTION_EFFECTS = new int[]{
        -1, // 0:  none
        2,  // 1:  moveSpeed
        10, // 2:  moveSlowdown
        -1, // 3:  digSpeed
        -1, // 4:  digSlowDown
        9,  // 5:  damageBoost
        5,  // 6:  heal
        12, // 7:  harm
        -1, // 8:  jump
        -1, // 9:  confusion
        1,  // 10: regeneration
        -1, // 11: resistance
        3,  // 12: fireResistance
        -1, // 13: waterBreathing
        14, // 14: invisibility
        -1, // 15: blindness
        6,  // 16: nightVision
        -1, // 17: hunger
        8,  // 18: weakness
        4,  // 19: poison
        -1, // 20: wither
    };

    private static final boolean enableItems = Config.getBoolean(MCPatcherUtils.CUSTOM_ITEM_TEXTURES, "items", true);
    private static final boolean enableEnchantments = Config.getBoolean(MCPatcherUtils.CUSTOM_ITEM_TEXTURES, "enchantments", true);
    private static final boolean enableArmor = Config.getBoolean(MCPatcherUtils.CUSTOM_ITEM_TEXTURES, "armor", true);

    private static TileLoader tileLoader;
    private static final ItemOverride[][] items = new ItemOverride[MAX_ITEMS][];
    private static final ItemOverride[][] enchantments = new ItemOverride[MAX_ITEMS][];
    private static final ItemOverride[][] armors = new ItemOverride[MAX_ITEMS][];

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
                        registerOverride(ItemOverride.create(path));
                    }
                    if (enableItems) {
                        registerPotions();
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
                Properties properties = TexturePackAPI.getProperties(MCPatcherUtils.TEXTURE_PACK_PREFIX + CIT_PROPERTIES);
                if (properties == null) {
                    properties = TexturePackAPI.getProperties(MCPatcherUtils.TEXTURE_PACK_PREFIX + "cit/" + CIT_PROPERTIES);
                }
                useGlint = MCPatcherUtils.getBooleanProperty(properties, "useGlint", true);
                EnchantmentList.setProperties(properties);
            }

            private void registerOverride(ItemOverride override) {
                if (override != null && !override.error) {
                    ItemOverride[][] list;
                    switch (override.type) {
                        case ItemOverride.ITEM:
                            if (!enableItems) {
                                return;
                            }
                            override.preload(tileLoader);
                            list = items;
                            break;

                        case ItemOverride.ENCHANTMENT:
                            if (!enableEnchantments) {
                                return;
                            }
                            list = enchantments;
                            break;

                        case ItemOverride.ARMOR:
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

            private String getPotionPath(String name, boolean splash) {
                return MCPatcherUtils.TEXTURE_PACK_PREFIX + "cit/custom_potion_" +
                    (splash ? "splash_" : "") + name + ".png";
            }

            private void registerPotions() {
                String path = getPotionPath("water", false);
                if (TexturePackAPI.hasResource(path)) {
                    registerVanillaPotion(path, ITEM_ID_POTION, 0, 0xffff);
                }
                path = getPotionPath("empty", false);
                if (TexturePackAPI.hasResource(path)) {
                    registerVanillaPotion(path, ITEM_ID_GLASS_BOTTLE, 0, 0);
                }
                registerPotionsByEffect(false);
                registerPotionsByEffect(true);
                path = getPotionPath("other", false);
                if (TexturePackAPI.hasResource(path)) {
                    registerVanillaPotion(path, ITEM_ID_POTION, 0, 0x401f);
                }
                path = getPotionPath("other", true);
                if (TexturePackAPI.hasResource(path)) {
                    registerVanillaPotion(path, ITEM_ID_POTION, 0x4000, 0x400f);
                }
            }

            private void registerPotionsByEffect(boolean splash) {
                for (int effect = 0; effect < Potion.potionTypes.length; effect++) {
                    if (Potion.potionTypes[effect] == null) {
                        continue;
                    }
                    String path = getPotionPath(Potion.potionTypes[effect].getName().replaceFirst("^potion\\.", ""), splash);
                    if (TexturePackAPI.hasResource(path)) {
                        if (effect < POTION_EFFECTS.length && POTION_EFFECTS[effect] >= 0) {
                            int damage = POTION_EFFECTS[effect];
                            if (splash) {
                                damage |= 0x4000;
                            }
                            registerVanillaPotion(path, damage, 0x400f);
                        }
                        if (!splash) {
                            registerCustomPotion(path, effect);
                        }
                    }
                }
            }

            private void registerMundanePotion(String name, int damage) {
                registerMundanePotion(name, damage, false);
                registerMundanePotion(name, damage | 0x4000, true);
            }

            private void registerMundanePotion(String name, int damage, boolean splash) {
                String path = getPotionPath(name, splash);
                if (TexturePackAPI.hasResource(path)) {
                    registerVanillaPotion(path, damage, 0x403f);
                }
            }

            private void registerOtherPotions(boolean splash) {
                String path = getPotionPath("other", splash);
                if (TexturePackAPI.hasResource(path)) {
                    Properties properties = new Properties();
                    properties.setProperty("type", "item");
                    properties.setProperty("matchItems", String.valueOf(ITEM_ID_POTION));
                    StringBuilder sb = new StringBuilder();
                    for (int i : new int[]{0, 7, 11, 13, 15, 16, 23, 27, 29, 31, 32, 39, 43, 45, 47, 48, 55, 59, 61, 63}) {
                        if (splash) {
                            i |= 0x4000;
                        }
                        sb.append(' ').append(i);
                    }
                    properties.setProperty("damage", sb.toString().trim());
                    properties.setProperty("damageMask", String.valueOf(0x403f));
                    properties.setProperty("texture", path);
                    properties.setProperty("layer", "0");
                    registerOverride(new ItemOverride("(none)", properties));

                    properties.setProperty("texture", "blank");
                    properties.setProperty("layer", "1");
                    registerOverride(new ItemOverride("(none)", properties));
                }
            }

            private void registerVanillaPotion(String path, int damage, int mask) {
                registerVanillaPotion(path, ITEM_ID_POTION, damage, mask);
            }

            private void registerVanillaPotion(String path, int itemID, int damage, int mask) {
                Properties properties = new Properties();
                properties.setProperty("type", "item");
                properties.setProperty("matchItems", String.valueOf(itemID));
                properties.setProperty("damage", String.valueOf(damage));
                properties.setProperty("damageMask", String.valueOf(mask));
                properties.setProperty("texture", path);
                properties.setProperty("layer", "0");
                registerOverride(new ItemOverride("(none)", properties));

                properties.setProperty("texture", "blank");
                properties.setProperty("layer", "1");
                registerOverride(new ItemOverride("(none)", properties));
            }

            private void registerCustomPotion(String path, int effect) {
                Properties properties = new Properties();
                properties.setProperty("type", "item");
                properties.setProperty("matchItems", String.valueOf(ITEM_ID_POTION));
                properties.setProperty("nbt.CustomPotionEffects.0.Id", String.valueOf(effect));
                properties.setProperty("texture", path);
                properties.setProperty("layer", "0");
                registerOverride(new ItemOverride("(none)", properties));

                properties.setProperty("texture", "blank");
                properties.setProperty("layer", "1");
                registerOverride(new ItemOverride("(none)", properties));
            }
        });
    }

    public static void init() {
    }

    public static Icon getIcon(Icon icon, ItemStack itemStack, int renderPass) {
        lastIcon = icon;
        if (enableItems) {
            ItemOverride override = findMatch(items, itemStack, renderPass);
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

    private static ItemOverride findMatch(ItemOverride[][] overrides, ItemStack itemStack, int renderPass) {
        lastRenderPass = renderPass;
        int itemID = itemStack.itemID;
        if (itemID >= 0 && itemID < overrides.length && overrides[itemID] != null) {
            int[] enchantmentLevels = getEnchantmentLevels(itemID, itemStack.stackTagCompound);
            boolean hasEffect = itemStack.hasEffect();
            for (ItemOverride override : overrides[itemID]) {
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
