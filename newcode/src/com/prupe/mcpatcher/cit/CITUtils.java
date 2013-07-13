package com.prupe.mcpatcher.cit;

import com.prupe.mcpatcher.*;
import net.minecraft.src.*;

import java.awt.image.BufferedImage;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class CITUtils {
    private static final MCLogger logger = MCLogger.getLogger(MCPatcherUtils.CUSTOM_ITEM_TEXTURES, "CIT");

    static final String CIT_PROPERTIES = "cit.properties";
    private static final ResourceLocation CIT_PROPERTIES1 = TexturePackAPI.newMCPatcherResourceLocation(CIT_PROPERTIES);
    private static final ResourceLocation CIT_PROPERTIES2 = TexturePackAPI.newMCPatcherResourceLocation("cit/" + CIT_PROPERTIES);
    static final ResourceLocation FIXED_ARMOR_RESOURCE = new ResourceLocation("textures/models/armor/iron_layer_1.png");

    private static final int MAX_ITEMS = Item.itemsList.length;
    static final int MAX_ENCHANTMENTS = 256;
    static int LOWEST_ITEM_ID;
    static int HIGHEST_ITEM_ID;
    static final Map<String, Integer> itemNameMap = new HashMap<String, Integer>();

    private static final int ITEM_ID_ENCHANTED_BOOK = 403;

    static final boolean enableItems = Config.getBoolean(MCPatcherUtils.CUSTOM_ITEM_TEXTURES, "items", true);
    static final boolean enableEnchantments = Config.getBoolean(MCPatcherUtils.CUSTOM_ITEM_TEXTURES, "enchantments", true);
    static final boolean enableArmor = Config.getBoolean(MCPatcherUtils.CUSTOM_ITEM_TEXTURES, "armor", true);

    private static TileLoader tileLoader;
    private static final ItemOverride[][] items = new ItemOverride[MAX_ITEMS][];
    private static final Enchantment[][] enchantments = new Enchantment[MAX_ITEMS][];
    private static final ArmorOverride[][] armors = new ArmorOverride[MAX_ITEMS][];

    private static boolean useGlint;

    private static EnchantmentList armorMatches;
    private static int armorMatchIndex;

    private static int lastRenderPass;
    private static Icon lastIcon;

    private static Field potionItemStackField;

    static {
        for (Field f : EntityPotion.class.getDeclaredFields()) {
            if (ItemStack.class.isAssignableFrom(f.getType())) {
                f.setAccessible(true);
                potionItemStackField = f;
                break;
            }
        }

        TexturePackChangeHandler.register(new TexturePackChangeHandler(MCPatcherUtils.CUSTOM_ITEM_TEXTURES, 3) {
            @Override
            public void beforeChange() {
                tileLoader = new TileLoader("textures/items", false, logger);
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

                BufferedImage image = TexturePackAPI.getImage(FIXED_ARMOR_RESOURCE);
                if (image == null) {
                    Enchantment.baseArmorWidth = 64.0f;
                    Enchantment.baseArmorHeight = 32.0f;
                } else {
                    Enchantment.baseArmorWidth = image.getWidth();
                    Enchantment.baseArmorHeight = image.getHeight();
                }

                Properties properties = TexturePackAPI.getProperties(CIT_PROPERTIES1);
                if (properties == null) {
                    properties = TexturePackAPI.getProperties(CIT_PROPERTIES2);
                }
                useGlint = MCPatcherUtils.getBooleanProperty(properties, "useGlint", true);
                EnchantmentList.setProperties(properties);

                if (enableItems || enableEnchantments || enableArmor) {
                    for (ResourceLocation resource : TexturePackAPI.listResources(TexturePackAPI.MCPATCHER_SUBDIR + "cit", ".properties", true, false, true)) {
                        registerOverride(OverrideBase.create(resource));
                    }
                    if (enableItems) {
                        PotionReplacer replacer = new PotionReplacer();
                        for (ItemOverride override : replacer.overrides) {
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
                sortOverrides(items);
                sortOverrides(enchantments);
                sortOverrides(armors);
            }

            private void registerOverride(OverrideBase override) {
                if (override != null && !override.error) {
                    OverrideBase[][] list;
                    if (override instanceof ItemOverride) {
                        ((ItemOverride) override).preload(tileLoader);
                        list = items;
                    } else if (override instanceof Enchantment) {
                        list = enchantments;
                    } else if (override instanceof ArmorOverride) {
                        list = armors;
                    } else {
                        logger.severe("unknown ItemOverride type %d", override.getClass().getName());
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
                    Class<? extends OverrideBase> overrideClass = list.getClass().getComponentType().getComponentType().asSubclass(OverrideBase.class);
                    list[itemID] = registerOverride(list[itemID], override, overrideClass);
                }
            }

            private OverrideBase[] registerOverride(OverrideBase[] list, OverrideBase override, Class<? extends OverrideBase> overrideClass) {
                if (override == null) {
                    // nothing
                } else if (!overrideClass.isAssignableFrom(override.getClass())) {
                    logger.severe("unexpected class %s in %s array", override.getClass().getName(), overrideClass.getSimpleName());
                } else if (list == null) {
                    list = (OverrideBase[]) Array.newInstance(overrideClass, 1);
                    list[0] = override;
                } else {
                    OverrideBase[] newList = (OverrideBase[]) Array.newInstance(overrideClass, list.length + 1);
                    System.arraycopy(list, 0, newList, 0, list.length);
                    newList[list.length] = override;
                    list = newList;
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
        if (icon == lastIcon && renderPass == lastRenderPass) {
            return icon;
        }
        lastRenderPass = renderPass;
        lastIcon = icon;
        if (enableItems) {
            ItemOverride override = findMatch(items, itemStack);
            if (override != null) {
                Icon newIcon = override.getReplacementIcon(icon);
                if (newIcon != null) {
                    lastIcon = newIcon;
                }
            }
        }
        return lastIcon;
    }

    public static Icon getEntityIcon(Icon icon, Entity entity) {
        if (entity instanceof EntityPotion) {
            if (potionItemStackField != null) {
                try {
                    return getIcon(icon, (ItemStack) potionItemStackField.get(entity), 1);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    potionItemStackField = null;
                }
            }
        }
        return icon;
    }

    public static ResourceLocation getArmorTexture(ResourceLocation texture, EntityLivingBase entity, ItemStack itemStack) {
        if (enableArmor) {
            ArmorOverride override = findMatch(armors, itemStack);
            if (override != null) {
                ResourceLocation newTexture = override.getReplacementTexture(texture);
                if (newTexture != null) {
                    return newTexture;
                }
            }
        }
        return texture;
    }

    private static <T extends OverrideBase> T findMatch(T[][] overrides, ItemStack itemStack) {
        int itemID = itemStack.itemID;
        if (itemID >= 0 && itemID < overrides.length && overrides[itemID] != null) {
            int[] enchantmentLevels = getEnchantmentLevels(itemID, itemStack.stackTagCompound);
            boolean hasEffect = itemStack.hasEffect();
            for (T override : overrides[itemID]) {
                if (override.match(itemStack, enchantmentLevels, hasEffect)) {
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
            return !useGlint;
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
            return !useGlint;
        }
        Enchantment.beginOuter2D();
        for (int i = 0; i < matches.size(); i++) {
            matches.getEnchantment(i).render2D(Tessellator.instance, matches.getIntensity(i), x - 2, y - 2, x + 18, y + 18, z - 50.0f);
        }
        Enchantment.endOuter2D();
        return !useGlint;
    }

    public static boolean setupArmorEnchantments(EntityLivingBase entity, int pass) {
        if (!enableEnchantments) {
            return false;
        }
        ItemStack itemStack = entity.getCurrentItemOrArmor(4 - pass);
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
