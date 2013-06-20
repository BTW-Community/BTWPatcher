package com.prupe.mcpatcher.cc;

import com.prupe.mcpatcher.MCLogger;
import com.prupe.mcpatcher.MCPatcherUtils;
import net.minecraft.src.MapColor;
import net.minecraft.src.Potion;

import java.util.*;

public class ColorizeItem {
    private static final MCLogger logger = MCLogger.getLogger(MCPatcherUtils.CUSTOM_COLORS);

    private static final Map<Integer, String> entityNamesByID = new HashMap<Integer, String>();
    private static final Map<Integer, Integer> spawnerEggShellColors = new HashMap<Integer, Integer>(); // egg.shell.*
    private static final Map<Integer, Integer> spawnerEggSpotColors = new HashMap<Integer, Integer>(); // egg.spots.*

    private static int waterBottleColor; // potion.water
    private static final List<Potion> potions = new ArrayList<Potion>(); // potion.*

    private static final String[] MAP_MATERIALS = new String[]{
        "air",
        "grass",
        "sand",
        "cloth",
        "tnt",
        "ice",
        "iron",
        "foliage",
        "snow",
        "clay",
        "dirt",
        "stone",
        "water",
        "wood",
    };

    static {
        try {
            reset();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    static void reset() {
        spawnerEggShellColors.clear();
        spawnerEggSpotColors.clear();

        waterBottleColor = 0x385dc6;
        for (Potion potion : potions) {
            potion.color = potion.origColor;
        }

        for (MapColor mapColor : MapColor.mapColorArray) {
            if (mapColor != null) {
                mapColor.colorValue = mapColor.origColorValue;
            }
        }
    }

    static void reloadPotionColors(Properties properties) {
        for (Potion potion : potions) {
            Colorizer.loadIntColor(potion.name, potion);
        }
        int[] temp = new int[]{waterBottleColor};
        Colorizer.loadIntColor("potion.water", temp, 0);
        waterBottleColor = temp[0];
    }

    static void reloadMapColors(Properties properties) {
        for (int i = 0; i < MapColor.mapColorArray.length; i++) {
            if (MapColor.mapColorArray[i] != null) {
                int[] rgb = new int[]{MapColor.mapColorArray[i].origColorValue};
                Colorizer.loadIntColor("map." + Colorizer.getStringKey(MAP_MATERIALS, i), rgb, 0);
                MapColor.mapColorArray[i].colorValue = rgb[0];
            }
        }
    }

    public static void setupSpawnerEgg(String entityName, int entityID, int defaultShellColor, int defaultSpotColor) {
        logger.config("egg.shell.%s=%06x", entityName, defaultShellColor);
        logger.config("egg.spots.%s=%06x", entityName, defaultSpotColor);
        entityNamesByID.put(entityID, entityName);
    }

    public static void setupPotion(Potion potion) {
        potion.origColor = potion.color;
        potions.add(potion);
    }

    public static int colorizeSpawnerEgg(int defaultColor, int entityID, int spots) {
        if (!Colorizer.useEggColors) {
            return defaultColor;
        }
        Integer value = null;
        Map<Integer, Integer> eggMap = (spots == 0 ? spawnerEggShellColors : spawnerEggSpotColors);
        if (eggMap.containsKey(entityID)) {
            value = eggMap.get(entityID);
        } else if (entityNamesByID.containsKey(entityID)) {
            String name = entityNamesByID.get(entityID);
            if (name != null) {
                int[] tmp = new int[]{defaultColor};
                Colorizer.loadIntColor((spots == 0 ? "egg.shell." : "egg.spots.") + name, tmp, 0);
                eggMap.put(entityID, tmp[0]);
                value = tmp[0];
            }
        }
        return value == null ? defaultColor : value;
    }

    public static int getWaterBottleColor() {
        return waterBottleColor;
    }
}
