package net.minecraft.src;

import java.io.File;

public class Minecraft {
    public ResourcePackRepository texturePackList;
    public RenderEngine renderEngine; // 1.5
    public GameSettings gameSettings;
    public EntityLivingBase renderViewEntity;
    public EntityRenderer entityRenderer;
    public int displayWidth;
    public int displayHeight;
    public MovingObjectPosition objectMouseOver;
    public GuiScreen currentScreen;
    public Timer timer;
    public Profiler mcProfiler;
    public EntityClientPlayerMP thePlayer;
    public WorldClient theWorld;

    public static Minecraft getInstance() {
        return null;
    }

    public static File getMinecraftDir() {
        return null;
    }

    public static File getAppDir(String s) {
        return null;
    }

    public static boolean isAmbientOcclusionEnabled() {
        return false;
    }

    public void scheduleTexturePackRefresh() {
    }

    public static int getMaxTextureSize() {
        return 0;
    }

    public TextureManager getTextureManager() {
        return null;
    }

    public ResourceManager getResourceManager() {
        return null;
    }

    // 1.8+
    public RenderBlockDispatcher getRenderBlockDispatcher() {
        return null;
    }
}
