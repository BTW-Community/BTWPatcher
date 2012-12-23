package com.pclewis.mcpatcher.mod;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

public class FireworksHelper {
    public static void setParticleBlendMethod() {
        if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
            GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
        } else {
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        }
    }
}
