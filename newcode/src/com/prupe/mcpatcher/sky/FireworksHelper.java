package com.prupe.mcpatcher.sky;

import com.prupe.mcpatcher.*;
import com.prupe.mcpatcher.mal.resource.BlendMethod;
import com.prupe.mcpatcher.mal.resource.TexturePackAPI;
import net.minecraft.src.EntityFX;
import net.minecraft.src.EntityFireworkOverlayFX;
import net.minecraft.src.EntityFireworkSparkFX;
import net.minecraft.src.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.util.Properties;

public class FireworksHelper {
    private static final int LIT_LAYER = 3;
    private static final int EXTRA_LAYER = LIT_LAYER + 1;
    private static final ResourceLocation PARTICLE_PROPERTIES = TexturePackAPI.newMCPatcherResourceLocation("particle.properties");

    private static final MCLogger logger = MCLogger.getLogger(MCPatcherUtils.BETTER_SKIES);
    private static final boolean enable = Config.getBoolean(MCPatcherUtils.BETTER_SKIES, "brightenFireworks", true);
    private static BlendMethod blendMethod;

    public static int getFXLayer(EntityFX entity) {
        if (enable && (entity instanceof EntityFireworkSparkFX || entity instanceof EntityFireworkOverlayFX)) {
            return EXTRA_LAYER;
        } else {
            return entity.getFXLayer();
        }
    }

    public static boolean skipThisLayer(boolean skip, int layer) {
        return skip || layer == LIT_LAYER || (!enable && layer > LIT_LAYER);
    }

    public static void setParticleBlendMethod(int layer) {
        if (enable && layer == EXTRA_LAYER && blendMethod != null) {
            blendMethod.applyBlending();
        } else {
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        }
    }

    static void reload() {
        Properties properties = TexturePackAPI.getProperties(PARTICLE_PROPERTIES);
        String blend = MCPatcherUtils.getStringProperty(properties, "blend." + EXTRA_LAYER, "add");
        blendMethod = BlendMethod.parse(blend);
        if (blendMethod == null) {
            logger.error("%s: unknown blend method %s", PARTICLE_PROPERTIES, blend);
        } else if (enable) {
            logger.config("using %s blending for fireworks particles", blendMethod);
        } else {
            logger.config("using default blending for fireworks particles");
        }
    }
}
