package com.prupe.mcpatcher.cc;

import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.mal.biome.ColorUtils;
import com.prupe.mcpatcher.mal.biome.BiomeAPI;
import com.prupe.mcpatcher.mal.resource.PropertiesFile;
import com.prupe.mcpatcher.mal.resource.TexturePackAPI;
import net.minecraft.src.EntityLivingBase;
import net.minecraft.src.EntitySheep;
import net.minecraft.src.ItemDye;
import net.minecraft.src.ResourceLocation;

import java.util.Random;

public class ColorizeEntity {
    private static final ResourceLocation LAVA_DROP_COLORS = TexturePackAPI.newMCPatcherResourceLocation("/misc/lavadropcolor.png", "colormap/lavadrop.png");
    private static final ResourceLocation MYCELIUM_COLORS = TexturePackAPI.newMCPatcherResourceLocation("/misc/myceliumparticlecolor.png", "colormap/myceliumparticle.png");
    private static final ResourceLocation XPORB_COLORS = TexturePackAPI.newMCPatcherResourceLocation("/misc/xporbcolor.png", "colormap/xporb.png");

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

    static void reloadParticleColors(PropertiesFile properties) {
        Colorizer.loadFloatColor("drop.water", waterBaseColor);
        Colorizer.loadFloatColor("particle.water", waterBaseColor);
        Colorizer.loadFloatColor("particle.portal", portalColor);
        int[] rgb = MCPatcherUtils.getImageRGB(TexturePackAPI.getImage(LAVA_DROP_COLORS));
        if (rgb != null) {
            lavaDropColors = new float[3 * rgb.length];
            for (int i = 0; i < rgb.length; i++) {
                ColorUtils.intToFloat3(rgb[i], lavaDropColors, 3 * i);
            }
        }
        myceliumColors = MCPatcherUtils.getImageRGB(TexturePackAPI.getImage(MYCELIUM_COLORS));
    }

    static void reloadDyeColors(PropertiesFile properties) {
        for (int i = 0; i < ItemDye.dyeColors.length; i++) {
            Colorizer.loadIntColor("dye." + Colorizer.getStringKey(ItemDye.dyeColorNames, i), ItemDye.dyeColors, i);
        }
        for (int i = 0; i < EntitySheep.fleeceColorTable.length; i++) {
            String key = Colorizer.getStringKey(ItemDye.dyeColorNames, EntitySheep.fleeceColorTable.length - 1 - i);
            Colorizer.loadFloatColor("sheep." + key, EntitySheep.fleeceColorTable[i]);
            Colorizer.loadFloatColor("armor." + key, armorColors[i]);
            Colorizer.loadFloatColor("collar." + key, collarColors[i]);
        }
        undyedLeatherColor = Colorizer.loadIntColor("armor.default", undyedLeatherColor);
    }

    static void reloadXPOrbColors(PropertiesFile properties) {
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
            System.arraycopy(lavaDropColors, offset, Colorizer.setColor, 0, 3);
            return true;
        }
    }

    public static boolean computeMyceliumParticleColor() {
        if (myceliumColors == null) {
            return false;
        } else {
            Colorizer.setColorF(myceliumColors[random.nextInt(myceliumColors.length)]);
            return true;
        }
    }

    public static int getPotionEffectColor(int defaultColor, EntityLivingBase entity) {
        return defaultColor == 0 ? defaultColor : entity.overridePotionColor;
    }

    public static void computeSuspendColor(int defaultColor, int i, int j, int k) {
        if (ColorizeWorld.underwaterColor != null) {
            defaultColor = ColorizeWorld.underwaterColor.getColorMultiplier(BiomeAPI.getWorld(), i, j, k);
        }
        Colorizer.setColorF(defaultColor);
    }
}
