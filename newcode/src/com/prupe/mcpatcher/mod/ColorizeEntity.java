package com.prupe.mcpatcher.mod;

import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.TexturePackAPI;
import net.minecraft.src.EntitySheep;
import net.minecraft.src.ItemDye;

import java.util.Properties;
import java.util.Random;

import static com.prupe.mcpatcher.mod.Colorizer.*;

public class ColorizeEntity {
    private static final String LAVA_DROP_COLORS = MCPatcherUtils.TEXTURE_PACK_PREFIX + "misc/lavadropcolor.png";
    private static final String MYCELIUM_COLORS = MCPatcherUtils.TEXTURE_PACK_PREFIX + "misc/myceliumparticlecolor.png";
    private static final String XPORB_COLORS = MCPatcherUtils.TEXTURE_PACK_PREFIX + "misc/xporbcolor.png";

    static float[] waterBaseColor; // particle.water
    private static float[] lavaDropColors; // misc/lavadropcolor.png

    public static float[] portalColor = new float[]{1.0f, 0.3f, 0.9f};

    private static final Random random = new Random();
    private static int[] myceliumColors;

    private static int[] xpOrbColors;

    private static final int[] origDyeColors = ItemDye.dyeColors.clone(); // dye.*
    private static final float[][] origFleeceColors = new float[EntitySheep.fleeceColorTable.length][]; // sheep.*

    public static final float[][] armorColors = new float[EntitySheep.fleeceColorTable.length][]; // armor.*
    public static int undyedLeatherColor; // armor.default

    public static final float[][] collarColors = new float[EntitySheep.fleeceColorTable.length][]; // collar.*

    static {
        try {
            for (int i = 0; i < EntitySheep.fleeceColorTable.length; i++) {
                origFleeceColors[i] = EntitySheep.fleeceColorTable[i].clone();
                armorColors[i] = EntitySheep.fleeceColorTable[i].clone();
                collarColors[i] = EntitySheep.fleeceColorTable[i].clone();
            }
            reset();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    static void reset() {
        waterBaseColor = new float[]{0.2f, 0.3f, 1.0f};
        portalColor = new float[]{1.0f, 0.3f, 0.9f};
        lavaDropColors = null;
        System.arraycopy(origDyeColors, 0, ItemDye.dyeColors, 0, origDyeColors.length);
        for (int i = 0; i < origFleeceColors.length; i++) {
            EntitySheep.fleeceColorTable[i] = origFleeceColors[i].clone();
            armorColors[i] = origFleeceColors[i].clone();
            collarColors[i] = origFleeceColors[i].clone();
        }
        undyedLeatherColor = 0xa06540;
        myceliumColors = null;
        xpOrbColors = null;
    }

    static void reloadParticleColors(Properties properties) {
        loadFloatColor("drop.water", waterBaseColor);
        loadFloatColor("particle.water", waterBaseColor);
        loadFloatColor("particle.portal", portalColor);
        int[] rgb = MCPatcherUtils.getImageRGB(TexturePackAPI.getImage(LAVA_DROP_COLORS));
        if (rgb != null) {
            lavaDropColors = new float[3 * rgb.length];
            for (int i = 0; i < rgb.length; i++) {
                intToFloat3(rgb[i], lavaDropColors, 3 * i);
            }
        }
        myceliumColors = MCPatcherUtils.getImageRGB(TexturePackAPI.getImage(MYCELIUM_COLORS));
    }

    static void reloadDyeColors(Properties properties) {
        for (int i = 0; i < ItemDye.dyeColors.length; i++) {
            loadIntColor("dye." + getStringKey(ItemDye.dyeColorNames, i), ItemDye.dyeColors, i);
        }
        for (int i = 0; i < EntitySheep.fleeceColorTable.length; i++) {
            String key = getStringKey(ItemDye.dyeColorNames, EntitySheep.fleeceColorTable.length - 1 - i);
            loadFloatColor("sheep." + key, EntitySheep.fleeceColorTable[i]);
            loadFloatColor("armor." + key, armorColors[i]);
            loadFloatColor("collar." + key, collarColors[i]);
        }
        undyedLeatherColor = loadIntColor("armor.default", undyedLeatherColor);
    }

    static void reloadXPOrbColors(Properties properties) {
        xpOrbColors = MCPatcherUtils.getImageRGB(TexturePackAPI.getImage(XPORB_COLORS));
    }

    public static int colorizeXPOrb(int origColor, float timer) {
        if (xpOrbColors == null || xpOrbColors.length == 0) {
            return origColor;
        } else {
            return xpOrbColors[(int) ((Math.sin(timer / 4.0) + 1.0) * (xpOrbColors.length - 1) / 2.0)];
        }
    }

    public static boolean computeLavaDropColor(int age) {
        if (lavaDropColors == null) {
            return false;
        } else {
            int offset = 3 * Math.max(Math.min(lavaDropColors.length / 3 - 1, age - 20), 0);
            System.arraycopy(lavaDropColors, offset, setColor, 0, 3);
            return true;
        }
    }

    public static boolean computeMyceliumParticleColor() {
        if (myceliumColors == null) {
            return false;
        } else {
            setColorF(myceliumColors[random.nextInt(myceliumColors.length)]);
            return true;
        }
    }

}
