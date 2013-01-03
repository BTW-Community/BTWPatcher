package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.MCLogger;
import com.pclewis.mcpatcher.MCPatcherUtils;
import com.pclewis.mcpatcher.TexturePackAPI;
import net.minecraft.src.Compass;
import net.minecraft.src.RenderEngine;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.util.glu.GLU;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Properties;

public class FancyCompass {
    private static final MCLogger logger = MCLogger.getLogger(MCPatcherUtils.HD_TEXTURES);

    private static final String ITEMS_PNG = "/gui/items.png";
    private static final String COMPASS_BASE_PNG = "/misc/compass_base.png";
    private static final String COMPASS_DIAL_PNG = "/misc/compass_dial.png";
    private static final String COMPASS_OVERLAY_PNG = "/misc/compass_overlay.png";
    private static final String COMPASS_PROPERTIES = "/misc/compass.properties";

    private static final int COMPASS_TILE_NUM = 54;
    private static final float RELATIVE_X = (COMPASS_TILE_NUM % 16) / 16.0f;
    private static final float RELATIVE_Y = (COMPASS_TILE_NUM / 16) / 16.0f;

    private static final boolean fboSupported = GLContext.getCapabilities().GL_EXT_framebuffer_object;
    private static final boolean gl13Supported = GLContext.getCapabilities().OpenGL13;
    private static final boolean useGL13 = MCPatcherUtils.getBoolean(MCPatcherUtils.HD_TEXTURES, "useGL13", true);
    private static final int drawList = GL11.glGenLists(1);

    private static FancyCompass instance;

    private static final HashSet<Integer> keysDown = new HashSet<Integer>();

    private final float scaleX;
    private final float scaleY;
    private final float offsetX;
    private final float offsetY;
    private final boolean debug;

    private final int itemsTexture;
    private final int baseTexture;
    private final int dialTexture;
    private final int overlayTexture;
    private final int scratchTexture;
    private final ByteBuffer scratchTextureBuffer;
    private final int tileSize;
    private final int compassX;
    private final int compassY;
    private final int frameBuffer;

    private static final float STEP = 0.01f;
    private float scaleXDelta;
    private float scaleYDelta;
    private float offsetXDelta;
    private float offsetYDelta;

    static {
        logger.config("fbo: supported=%s", fboSupported);
        logger.config("GL13: supported=%s, enabled=%s", gl13Supported, useGL13);
    }

    private FancyCompass() {
        RenderEngine renderEngine = MCPatcherUtils.getMinecraft().renderEngine;

        itemsTexture = renderEngine.getTexture(ITEMS_PNG);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, itemsTexture);
        tileSize = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH) / 16;
        compassX = (int) (RELATIVE_X * tileSize * 16);
        compassY = (int) (RELATIVE_Y * tileSize * 16);

        final int targetTexture;
        String config = MCPatcherUtils.getString(MCPatcherUtils.HD_TEXTURES, "use_glReadPixels", "").trim().toLowerCase();
        if (config.equals("") ? tileSize > 64 : Boolean.parseBoolean(config)) {
            BufferedImage image = new BufferedImage(tileSize, tileSize, BufferedImage.TYPE_INT_ARGB);
            scratchTexture = renderEngine.allocateAndSetupTexture(image);
            targetTexture = scratchTexture;
            scratchTextureBuffer = ByteBuffer.allocateDirect(4 * tileSize * tileSize);
            logger.fine("rendering compass to %dx%d scratch texture", tileSize, tileSize);
        } else {
            scratchTexture = -1;
            scratchTextureBuffer = null;
            targetTexture = itemsTexture;
            logger.fine("rendering compass directly to %s", ITEMS_PNG);
        }

        Properties properties = TexturePackAPI.getProperties(COMPASS_PROPERTIES);
        scaleX = MCPatcherUtils.getFloatProperty(properties, "scaleX", 1.0f);
        scaleY = MCPatcherUtils.getFloatProperty(properties, "scaleY", 0.5f);
        offsetX = MCPatcherUtils.getFloatProperty(properties, "offsetX", 1.0f / (2 * tileSize));
        offsetY = MCPatcherUtils.getFloatProperty(properties, "offsetY", -1.0f / (2 * tileSize));
        renderEngine.blurTexture = MCPatcherUtils.getBooleanProperty(properties, "filter", false);
        debug = MCPatcherUtils.getBooleanProperty(properties, "debug", false);

        BufferedImage image = TexturePackAPI.getImage(COMPASS_BASE_PNG);
        if (image == null) {
            image = new BufferedImage(tileSize, tileSize, BufferedImage.TYPE_INT_ARGB);
            BufferedImage items = TexturePackAPI.getImage(ITEMS_PNG);
            Graphics2D graphics2D = image.createGraphics();
            int sx = (int) (items.getWidth() * RELATIVE_X);
            int sy = (int) (items.getHeight() * RELATIVE_Y);
            graphics2D.drawImage(items,
                0, 0, image.getWidth(), image.getHeight(),
                sx, sy, sx + items.getWidth() / 16, sy + items.getHeight() / 16,
                null
            );
        }
        baseTexture = renderEngine.allocateAndSetupTexture(image);

        image = TexturePackAPI.getImage(COMPASS_DIAL_PNG);
        dialTexture = renderEngine.allocateAndSetupTexture(image);

        image = TexturePackAPI.getImage(COMPASS_OVERLAY_PNG);
        if (image == null) {
            overlayTexture = -1;
        } else {
            overlayTexture = renderEngine.allocateAndSetupTexture(image);
        }

        frameBuffer = EXTFramebufferObject.glGenFramebuffersEXT();
        EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, frameBuffer);
        EXTFramebufferObject.glFramebufferTexture2DEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT, GL11.GL_TEXTURE_2D, targetTexture, 0);
        EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, 0);

        GL11.glNewList(drawList, GL11.GL_COMPILE);
        drawBox();
        GL11.glEndList();
    }

    private void onTick(Compass compass) {
        boolean f = false;
        if (!debug) {
            f = true;
        } else if (tap(Keyboard.KEY_NUMPAD2)) {
            scaleYDelta -= STEP;
        } else if (tap(Keyboard.KEY_NUMPAD8)) {
            scaleYDelta += STEP;
        } else if (tap(Keyboard.KEY_NUMPAD4)) {
            scaleXDelta -= STEP;
        } else if (tap(Keyboard.KEY_NUMPAD6)) {
            scaleXDelta += STEP;
        } else if (tap(Keyboard.KEY_DOWN)) {
            offsetYDelta += STEP;
        } else if (tap(Keyboard.KEY_UP)) {
            offsetYDelta -= STEP;
        } else if (tap(Keyboard.KEY_LEFT)) {
            offsetXDelta -= STEP;
        } else if (tap(Keyboard.KEY_RIGHT)) {
            offsetXDelta += STEP;
        } else if (tap(Keyboard.KEY_MULTIPLY)) {
            scaleXDelta = scaleYDelta = offsetXDelta = offsetYDelta = 0.0f;
        } else {
            f = true;
        }
        if (!f) {
            logger.info("");
            logger.info("scaleX = %f", scaleX + scaleXDelta);
            logger.info("scaleY = %f", scaleY + scaleYDelta);
            logger.info("offsetX = %f", offsetX + offsetXDelta);
            logger.info("offsetY = %f", offsetY + offsetYDelta);
        }

        int bits = GL11.GL_VIEWPORT_BIT | GL11.GL_SCISSOR_BIT | GL11.GL_DEPTH_BITS | GL11.GL_LIGHTING_BIT;
        if (gl13Supported && useGL13) {
            bits |= GL13.GL_MULTISAMPLE_BIT;
        }
        GL11.glPushAttrib(bits);
        if (scratchTexture >= 0) {
            GL11.glViewport(0, 0, tileSize, tileSize);
        } else {
            GL11.glViewport(compassX, compassY, tileSize, tileSize);
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            GL11.glScissor(compassX, compassY, tileSize, tileSize);
        }

        EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, frameBuffer);

        boolean lightmapEnabled = false;
        if (GLContext.getCapabilities().OpenGL13) {
            GL13.glActiveTexture(GL13.GL_TEXTURE1);
            lightmapEnabled = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
            if (lightmapEnabled) {
                GL11.glDisable(GL11.GL_TEXTURE_2D);
            }
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
        }
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        GL11.glDisable(GL11.GL_LIGHTING);
        if (gl13Supported && useGL13) {
            GL11.glDisable(GL13.GL_MULTISAMPLE);
        }

        GL11.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(-1.0f, 1.0f, -1.0f, 1.0f, -1.0f, 1.0f);

        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, baseTexture);
        GL11.glCallList(drawList);

        GL11.glPushMatrix();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, dialTexture);
        float angle = (float) (180.0 - compass.currentAngle * 180.0 / Math.PI);
        GL11.glTranslatef(offsetX + offsetXDelta, offsetY + offsetYDelta, 0.0f);
        GL11.glScalef(scaleX + scaleXDelta, scaleY + scaleYDelta, 1.0f);
        GL11.glRotatef(angle, 0.0f, 0.0f, 1.0f);
        GL11.glCallList(drawList);
        GL11.glPopMatrix();

        if (overlayTexture >= 0) {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, overlayTexture);
            drawBox();
        }

        if (scratchTexture >= 0) {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, scratchTexture);
            scratchTextureBuffer.position(0);
            GL11.glReadPixels(0, 0, tileSize, tileSize, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, scratchTextureBuffer);
            scratchTextureBuffer.position(0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, itemsTexture);
            GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, compassX, compassY, tileSize, tileSize, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, scratchTextureBuffer);
        }

        EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, 0);
        GL11.glPopAttrib();

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();

        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();

        if (lightmapEnabled) {
            GL13.glActiveTexture(GL13.GL_TEXTURE1);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
        }
    }

    private void drawBox() {
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0.0f, 0.0f);
        GL11.glVertex3f(-1.0f, -1.0f, 0.0f);
        GL11.glTexCoord2f(1.0f, 0.0f);
        GL11.glVertex3f(1.0f, -1.0f, 0.0f);
        GL11.glTexCoord2f(1.0f, 1.0f);
        GL11.glVertex3f(1.0f, 1.0f, 0.0f);
        GL11.glTexCoord2f(0.0f, 1.0f);
        GL11.glVertex3f(-1.0f, 1.0f, 0.0f);
        GL11.glEnd();
    }

    private void finish() {
        if (frameBuffer >= 0) {
            EXTFramebufferObject.glDeleteFramebuffersEXT(frameBuffer);
        }
        RenderEngine renderEngine = MCPatcherUtils.getMinecraft().renderEngine;
        if (scratchTexture >= 0) {
            renderEngine.deleteTexture(scratchTexture);
        }
        if (baseTexture >= 0) {
            renderEngine.deleteTexture(baseTexture);
        }
        if (dialTexture >= 0) {
            renderEngine.deleteTexture(dialTexture);
        }
        if (overlayTexture >= 0) {
            renderEngine.deleteTexture(overlayTexture);
        }
    }

    private static boolean tap(int key) {
        if (Keyboard.isKeyDown(key)) {
            if (!keysDown.contains(key)) {
                keysDown.add(key);
                return true;
            }
        } else {
            keysDown.remove(key);
        }
        return false;
    }

    static boolean refresh() {
        if (instance != null) {
            instance.finish();
            instance = null;
        }
        if (fboSupported && TexturePackAPI.hasResource(COMPASS_DIAL_PNG)) {
            try {
                instance = new FancyCompass();
                int error = GL11.glGetError();
                if (error != 0 || instance.frameBuffer < 0) {
                    logger.severe("%s during compass setup", GLU.gluErrorString(error));
                    instance.finish();
                    instance = null;
                }
            } catch (Throwable e) {
                e.printStackTrace();
            } finally {
                MCPatcherUtils.getMinecraft().renderEngine.blurTexture = false;
            }
        }
        return instance != null;
    }

    public static void update(Compass compass) {
        if (instance != null) {
            instance.onTick(compass);
        }
    }
}
