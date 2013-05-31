package com.prupe.mcpatcher.cit;

import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.TexturePackAPI;
import net.minecraft.src.Potion;

import java.util.*;

class PotionReplacer {
    private static final int ITEM_ID_POTION = 373;
    private static final int ITEM_ID_GLASS_BOTTLE = 374;

    private static final int SPLASH_BIT = 0x4000;
    private static final int EFFECT_BITS = 0x400f;
    private static final int MUNDANE_BITS = 0x403f;
    private static final int WATER_BITS = 0xffff;

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

    private static final Map<String, Integer> mundanePotionMap = new HashMap<String, Integer>();

    final List<ItemOverride> overrides = new ArrayList<ItemOverride>();

    static {
        int[] mundaneIds = new int[]{
            0, 7, 11, 13, 15,
            16, 23, 27, 29, 31,
            32, 39, 43, 45, 47,
            48, 55, 59, 61, 63,
        };

        String[] mundaneNames = new String[]{
            "mundane", "uninteresting", "bland", "clear",
            "milky", "diffuse", "artless", "thin",
            "awkward", "flat", "bulky", "bungling",
            "buttered", "smooth", "suave", "debonair",
            "thick", "elegant", "fancy", "charming",
            "dashing", "refined", "cordial", "sparkling",
            "potent", "foul", "odorless", "rank",
            "harsh", "acrid", "gross", "stinky",
        };

        for (int i : mundaneIds) {
            mundanePotionMap.put(mundaneNames[i / 2], i);
        }
    }

    PotionReplacer() {
        String path = getPotionPath("water", false);
        if (TexturePackAPI.hasResource(path)) {
            registerVanillaPotion(path, ITEM_ID_POTION, 0, WATER_BITS);
        }
        path = getPotionPath("empty", false);
        if (TexturePackAPI.hasResource(path)) {
            registerVanillaPotion(path, ITEM_ID_GLASS_BOTTLE, 0, 0);
        }
        registerPotionsByEffect(false);
        registerPotionsByEffect(true);
        registerMundanePotions(false);
        registerMundanePotions(true);
        registerOtherPotions(false);
        registerOtherPotions(true);
    }

    private static String getPotionPath(String name, boolean splash) {
        return MCPatcherUtils.TEXTURE_PACK_PREFIX + "cit/custom_potion_" +
            (splash ? "splash_" : "") + name + ".png";
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
                        damage |= SPLASH_BIT;
                    }
                    registerVanillaPotion(path, damage, EFFECT_BITS);
                }
                if (!splash) {
                    registerCustomPotion(path, effect);
                }
            }
        }
    }

    private void registerMundanePotions(boolean splash) {
        for (Map.Entry<String, Integer> entry : mundanePotionMap.entrySet()) {
            int damage = entry.getValue();
            if (splash) {
                damage |= SPLASH_BIT;
            }
            registerMundanePotion(entry.getKey(), damage, splash);
        }
    }

    private void registerMundanePotion(String name, int damage, boolean splash) {
        String path = getPotionPath(name, splash);
        if (TexturePackAPI.hasResource(path)) {
            registerVanillaPotion(path, damage, MUNDANE_BITS);
        }
    }

    private void registerOtherPotions(boolean splash) {
        String path = getPotionPath("other", splash);
        if (TexturePackAPI.hasResource(path)) {
            Properties properties = new Properties();
            properties.setProperty("type", "item");
            properties.setProperty("matchItems", String.valueOf(ITEM_ID_POTION));
            StringBuilder sb = new StringBuilder();
            for (int i : mundanePotionMap.values()) {
                if (splash) {
                    i |= SPLASH_BIT;
                }
                sb.append(' ').append(i);
            }
            properties.setProperty("damage", sb.toString().trim());
            properties.setProperty("damageMask", String.valueOf(MUNDANE_BITS));
            properties.setProperty("texture", path);
            properties.setProperty("layer", "0");
            addOverride(properties);
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
        addOverride(properties);
    }

    private void registerCustomPotion(String path, int effect) {
        Properties properties = new Properties();
        properties.setProperty("type", "item");
        properties.setProperty("matchItems", String.valueOf(ITEM_ID_POTION));
        properties.setProperty("nbt.CustomPotionEffects.0.Id", String.valueOf(effect));
        properties.setProperty("texture", path);
        properties.setProperty("layer", "0");
        addOverride(properties);
    }

    private void addOverride(Properties properties) {
        ItemOverride layer0 = new ItemOverride("(0)", properties);
        if (layer0.error) {
            return;
        }

        properties.setProperty("texture", "blank");
        properties.setProperty("layer", "1");
        ItemOverride layer1 = new ItemOverride("(1)", properties);
        if (layer1.error) {
            return;
        }

        overrides.add(layer0);
        overrides.add(layer1);
    }
}
