package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.MCLogger;
import com.pclewis.mcpatcher.MCPatcherUtils;
import com.pclewis.mcpatcher.TexturePackAPI;
import net.minecraft.src.EntityFX;
import net.minecraft.src.EntityFireworkOverlayFX;
import net.minecraft.src.EntityFireworkSparkFX;
import org.lwjgl.opengl.GL11;

import java.util.Properties;

public class FireworksHelper {
    private static final int LIT_LAYER = 3;
    private static final int DODGE_LAYER = LIT_LAYER + 1;
    private static final String PARTICLES_PROPERTIES = "/particles.properties";

    private static final MCLogger logger = MCLogger.getLogger(MCPatcherUtils.BETTER_SKIES);
    private static final boolean enable = MCPatcherUtils.getBoolean(MCPatcherUtils.BETTER_SKIES, "brightenFireworks", true);
    private static int srcBlend = GL11.GL_SRC_ALPHA;
    private static int dstBlend = GL11.GL_ONE;

    public static int getFXLayer(EntityFX entity) {
        if (enable && (entity instanceof EntityFireworkSparkFX || entity instanceof EntityFireworkOverlayFX)) {
            return DODGE_LAYER;
        } else {
            return entity.getFXLayer();
        }
    }

    public static boolean skipThisLayer(boolean skip, int layer) {
        return skip || layer == LIT_LAYER || (!enable && layer > LIT_LAYER);
    }

    public static void setParticleBlendMethod(int layer) {
        if (enable && layer == DODGE_LAYER) {
            GL11.glBlendFunc(srcBlend, dstBlend);
        } else {
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        }
    }

    static void reload() {
        Properties properties = TexturePackAPI.getProperties(PARTICLES_PROPERTIES);
        srcBlend = MCPatcherUtils.getIntProperty(properties, "srcBlend." + DODGE_LAYER, GL11.GL_SRC_ALPHA);
        dstBlend = MCPatcherUtils.getIntProperty(properties, "dstBlend." + DODGE_LAYER, GL11.GL_ONE);
        if (enable) {
            logger.config("using glBlendFunc(%d, %d) for fireworks particles", srcBlend, dstBlend);
        } else {
            logger.config("using default blending for fireworks particles");
        }
    }
}
