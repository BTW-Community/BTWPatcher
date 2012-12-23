package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.MCPatcherUtils;
import net.minecraft.src.EntityFX;
import net.minecraft.src.EntityFireworkSparkFX;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

public class FireworksHelper {
    private static final int LIT_LAYER = 3;
    private static final int DODGE_LAYER = LIT_LAYER + 1;

    private static final boolean enable = MCPatcherUtils.getBoolean(MCPatcherUtils.BETTER_SKIES, "brightenFireworks", true);

    public static int getFXLayer(EntityFX entity) {
        if (enable && entity instanceof EntityFireworkSparkFX) {
            return DODGE_LAYER;
        } else {
            return entity.getFXLayer();
        }
    }

    public static boolean skipThisLayer(boolean skip, int layer) {
        return skip || layer == LIT_LAYER || (!enable && layer > LIT_LAYER);
    }

    public static void setParticleBlendMethod(int layer) {
        if (enable && !Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) && layer == DODGE_LAYER) {
            GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
        } else {
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        }
    }
}
