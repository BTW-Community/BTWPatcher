package com.prupe.mcpatcher.cit;

import com.prupe.mcpatcher.MCLogger;
import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.TexturePackAPI;
import net.minecraft.src.Potion;
import net.minecraft.src.PotionHelper;
import net.minecraft.src.ResourceAddress;

import java.util.*;

class PotionReplacer {
    private static final MCLogger logger = MCLogger.getLogger(MCPatcherUtils.CUSTOM_ITEM_TEXTURES, "CIT");

    private static final int ITEM_ID_POTION = 373;
    private static final int ITEM_ID_GLASS_BOTTLE = 374;

    private static final String LAYER_POTION_CONTENTS = "potion_overlay";
    private static final String LAYER_POTION_DRINKABLE = "potion_bottle_drinkable";
    private static final String LAYER_POTION_SPLASH = "potion_bottle_splash";
    private static final String LAYER_EMPTY_BOTTLE = "potion_bottle_empty";

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
        try {
            for (int i : new int[]{0, 7, 11, 13, 15, 16, 23, 27, 29, 31, 32, 39, 43, 45, 47, 48, 55, 59, 61, 63}) {
                String name = PotionHelper.getMundaneName(i).replaceFirst("^potion\\.prefix\\.", "");
                mundanePotionMap.put(name, i);
                logger.fine("%s potion -> damage value %d", name, i);
            }
            for (int i = 0; i < Potion.potionTypes.length; i++) {
                Potion potion = Potion.potionTypes[i];
                if (potion != null) {
                    logger.fine("%s potion -> effect %d", potion.getName().replaceFirst("^potion\\.", ""), i);
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    PotionReplacer() {
        ResourceAddress path = getPotionPath("water", false);
        if (TexturePackAPI.hasResource(path)) {
            registerVanillaPotion(path, 0, WATER_BITS, false);
        }
        path = getPotionPath("empty", false);
        if (TexturePackAPI.hasResource(path)) {
            registerEmptyBottle(path);
        }
        registerPotionsByEffect(false);
        registerPotionsByEffect(true);
        registerMundanePotions(false);
        registerMundanePotions(true);
        registerOtherPotions(false);
        registerOtherPotions(true);
    }

    private static ResourceAddress getPotionPath(String name, boolean splash) {
        return new ResourceAddress("textures/cit/potion/" + (splash ? "splash/" : "normal/") + name + ".png");
    }

    private static Properties newProperties(ResourceAddress path, int itemID, String layer) {
        Properties properties = new Properties();
        properties.setProperty("type", "item");
        properties.setProperty("items", String.valueOf(itemID));
        properties.setProperty("texture." + layer, path.toString());
        properties.setProperty("texture." + LAYER_POTION_CONTENTS, "blank");
        properties.setProperty("weight", "-1");
        return properties;
    }

    private static Properties newProperties(ResourceAddress path, int itemID, boolean splash) {
        String layer = splash ? LAYER_POTION_SPLASH : LAYER_POTION_DRINKABLE;
        return newProperties(path, itemID, layer);
    }

    private void registerPotionsByEffect(boolean splash) {
        for (int effect = 0; effect < Potion.potionTypes.length; effect++) {
            if (Potion.potionTypes[effect] == null) {
                continue;
            }
            ResourceAddress path = getPotionPath(Potion.potionTypes[effect].getName().replaceFirst("^potion\\.", ""), splash);
            if (TexturePackAPI.hasResource(path)) {
                if (effect < POTION_EFFECTS.length && POTION_EFFECTS[effect] >= 0) {
                    int damage = POTION_EFFECTS[effect];
                    if (splash) {
                        damage |= SPLASH_BIT;
                    }
                    registerVanillaPotion(path, damage, EFFECT_BITS, splash);
                }
                if (!splash) {
                    registerCustomPotion(path, effect, splash);
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
        ResourceAddress path = getPotionPath(name, splash);
        if (TexturePackAPI.hasResource(path)) {
            registerVanillaPotion(path, damage, MUNDANE_BITS, splash);
        }
    }

    private void registerOtherPotions(boolean splash) {
        ResourceAddress path = getPotionPath("other", splash);
        if (TexturePackAPI.hasResource(path)) {
            Properties properties = newProperties(path, ITEM_ID_POTION, splash);
            StringBuilder sb = new StringBuilder();
            for (int i : mundanePotionMap.values()) {
                if (splash) {
                    i |= SPLASH_BIT;
                }
                sb.append(' ').append(i);
            }
            properties.setProperty("damage", sb.toString().trim());
            properties.setProperty("damageMask", String.valueOf(MUNDANE_BITS));
            addOverride(path, properties);
        }
    }

    private void registerVanillaPotion(ResourceAddress path, int damage, int mask, boolean splash) {
        Properties properties = newProperties(path, ITEM_ID_POTION, splash);
        properties.setProperty("damage", String.valueOf(damage));
        properties.setProperty("damageMask", String.valueOf(mask));
        addOverride(path, properties);
    }

    private void registerCustomPotion(ResourceAddress path, int effect, boolean splash) {
        Properties properties = newProperties(path, ITEM_ID_POTION, splash);
        properties.setProperty("nbt.CustomPotionEffects.0.Id", String.valueOf(effect));
        addOverride(path, properties);
    }

    private void registerEmptyBottle(ResourceAddress path) {
        Properties properties = newProperties(path, ITEM_ID_GLASS_BOTTLE, LAYER_EMPTY_BOTTLE);
        addOverride(path, properties);
    }

    private void addOverride(ResourceAddress path, Properties properties) {
        ResourceAddress propertiesName = TexturePackAPI.newResourceAddress(path, ".png", ".properties");
        ItemOverride override = new ItemOverride(propertiesName, properties);
        if (!override.error) {
            overrides.add(override);
        }
    }
}
