package com.prupe.mcpatcher.mod;

import com.prupe.mcpatcher.*;
import net.minecraft.src.*;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.util.glu.GLU;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.*;

public class FancyDial {
    private static final MCLogger logger = MCLogger.getLogger(MCPatcherUtils.CUSTOM_ANIMATIONS, "Animation");

    private static final String ITEMS_PNG = "/gui/items.png";

    private static final boolean fboSupported = GLContext.getCapabilities().GL_EXT_framebuffer_object;
    private static final boolean gl13Supported = GLContext.getCapabilities().OpenGL13;
    private static final boolean enableCompass = Config.getBoolean(MCPatcherUtils.EXTENDED_HD, "fancyCompass", true);
    private static final boolean enableClock = Config.getBoolean(MCPatcherUtils.EXTENDED_HD, "fancyClock", true);
    private static final boolean useGL13 = Config.getBoolean(MCPatcherUtils.EXTENDED_HD, "useGL13", true);
    private static boolean initialized;
    private static final int drawList = GL11.glGenLists(1);

    private static final Map<TextureStitched, Properties> setupInfo = new WeakHashMap<TextureStitched, Properties>();
    private static final Map<TextureStitched, FancyDial> instances = new WeakHashMap<TextureStitched, FancyDial>();

    private static final HashSet<Integer> keysDown = new HashSet<Integer>();

    private final TextureStitched icon;
    private final String name;
    private boolean needExtraUpdate;
    private boolean ok;
    private int outputFrames;

    private final List<Layer> layers = new ArrayList<Layer>();
    private int frameBuffer = -1;

    private boolean debug;

    private static final float STEP = 0.01f;
    private float scaleXDelta;
    private float scaleYDelta;
    private float offsetXDelta;
    private float offsetYDelta;

    static {
        logger.config("fbo: supported=%s", fboSupported);
        logger.config("GL13: supported=%s, enabled=%s", gl13Supported, useGL13);

        GL11.glNewList(drawList, GL11.GL_COMPILE);
        drawBox();
        GL11.glEndList();
    }

    public static void setup(TextureStitched icon) {
        if (!fboSupported) {
            return;
        }
        if (icon instanceof TextureCompass && !enableCompass) {
            return;
        }
        if (icon instanceof TextureClock && !enableClock) {
            return;
        }
        String name = icon.getName();
        Properties properties = TexturePackAPI.getProperties("/misc/" + name + ".properties");
        if (properties != null) {
            logger.fine("found custom %s", name);
            setupInfo.put(icon, properties);
        }
    }

    public static boolean update(TextureStitched icon) {
        if (!initialized) {
            logger.finer("deferring %s update until initialization finishes", icon.getName());
            return false;
        }
        FancyDial instance = instances.get(icon);
        if (instance == null) {
            instance = getInstance(icon);
            if (instance == null) {
                return false;
            }
        }
        return instance.render();
    }

    static void updateAll() {
        if (!initialized) {
            logger.finer("deferring %s update until initialization finishes", FancyDial.class.getSimpleName());
            return;
        }
        if (!setupInfo.isEmpty()) {
            List<TextureStitched> keys = new ArrayList<TextureStitched>();
            keys.addAll(setupInfo.keySet());
            for (TextureStitched icon : keys) {
                getInstance(icon);
            }
        }
        for (FancyDial instance : instances.values()) {
            if (instance != null && instance.needExtraUpdate) {
                instance.icon.update();
            }
        }
    }

    static void refresh() {
        logger.finer("FancyDial.refresh");
        for (FancyDial instance : instances.values()) {
            if (instance != null) {
                instance.finish();
            }
        }
        instances.clear();
        initialized = true;
    }

    private static FancyDial getInstance(TextureStitched icon) {
        Properties properties = setupInfo.get(icon);
        if (properties == null) {
            return null;
        }
        setupInfo.remove(icon);
        RenderEngine renderEngine = MCPatcherUtils.getMinecraft().renderEngine;
        try {
            FancyDial instance = new FancyDial(renderEngine, icon, properties);
            if (instance.ok) {
                instances.put(icon, instance);
                return instance;
            }
            instance.finish();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    private FancyDial(RenderEngine renderEngine, TextureStitched icon, Properties properties) {
        this.icon = icon;
        name = icon.getName();
        String baseTextureName = "/textures/items/" + name + ".png";
        BufferedImage image = TexturePackAPI.getImage(baseTextureName);
        if (image == null) {
            logger.error("could not get %s", baseTextureName);
            return;
        }
        needExtraUpdate = (image.getHeight() % image.getWidth() != 0 || image.getHeight() / image.getWidth() <= 1);
        if (needExtraUpdate) {
            logger.fine("%s needs direct .update() call", name);
        }

        TexturePackAPI.bindTexture(ITEMS_PNG);
        int itemsTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        if (itemsTexture < 0) {
            logger.severe("could not get items texture");
            return;
        }
        logger.fine("setting up %s", this);

        for (int i = 0; ; i++) {
            Layer layer = newLayer("/misc/" + name + ".properties", properties, "." + i);
            if (layer == null) {
                if (i > 0) {
                    break;
                }
                continue;
            }
            layers.add(layer);
            debug |= layer.debug;
            logger.fine("  new %s", layer);
        }
        if (layers.size() < 2) {
            logger.error("custom %s needs at least two layers defined", name);
            return;
        }

        outputFrames = MCPatcherUtils.getIntProperty(properties, "outputFrames", 0);

        frameBuffer = EXTFramebufferObject.glGenFramebuffersEXT();
        if (frameBuffer < 0) {
            logger.severe("could not get framebuffer object");
            return;
        }
        EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, frameBuffer);
        EXTFramebufferObject.glFramebufferTexture2DEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT, GL11.GL_TEXTURE_2D, itemsTexture, 0);
        EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, 0);

        int glError = GL11.glGetError();
        if (glError != 0) {
            logger.severe("%s during %s setup", GLU.gluErrorString(glError), name);
            return;
        }
        ok = true;
    }

    private boolean render() {
        if (!ok) {
            return false;
        }

        boolean changed = true;
        if (!debug) {
            changed = false;
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
            changed = false;
        }
        if (changed) {
            logger.info("");
            logger.info("scaleX  %+f", scaleXDelta);
            logger.info("scaleY  %+f", scaleYDelta);
            logger.info("offsetX %+f", offsetXDelta);
            logger.info("offsetY %+f", offsetYDelta);
        }

        if (outputFrames > 0) {
            try {
                int width = getIconWidth(icon);
                int height = getIconHeight(icon);
                BufferedImage image = new BufferedImage(width, outputFrames * height, BufferedImage.TYPE_INT_ARGB);
                ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * width * height);
                IntBuffer intBuffer = byteBuffer.asIntBuffer();
                int[] argb = new int[width * height];
                EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, frameBuffer);
                File path = MCPatcherUtils.getMinecraftPath("custom_" + name + ".png");
                logger.info("generating %d %s frames", outputFrames, name);
                for (int i = 0; i < outputFrames; i++) {
                    render(i * (360.0 / outputFrames), false);
                    byteBuffer.position(0);
                    GL11.glReadPixels(icon.getX0(), icon.getY0(), width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, byteBuffer);
                    intBuffer.position(0);
                    for (int j = 0; j < argb.length; j++) {
                        argb[j] = Integer.rotateRight(intBuffer.get(j), 8);
                    }
                    image.setRGB(0, i * height, width, height, argb, 0, width);
                }
                ImageIO.write(image, "png", path);
                logger.info("wrote %dx%d %s", image.getWidth(), image.getHeight(), path.getPath());
            } catch (Throwable e) {
                e.printStackTrace();
            } finally {
                EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, 0);
            }
            outputFrames = 0;
        }

        return render(getAngle(icon), true);
    }

    private boolean render(double angle, boolean bindFB) {
        int bits = GL11.GL_VIEWPORT_BIT | GL11.GL_SCISSOR_BIT | GL11.GL_DEPTH_BITS | GL11.GL_LIGHTING_BIT;
        if (gl13Supported && useGL13) {
            bits |= GL13.GL_MULTISAMPLE_BIT;
        }
        GL11.glPushAttrib(bits);
        final int x0 = icon.getX0();
        final int y0 = icon.getY0();
        final int width = getIconWidth(icon);
        final int height = getIconHeight(icon);
        GL11.glViewport(x0, y0, width, height);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x0, y0, width, height);

        if (bindFB) {
            EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, frameBuffer);
        }

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

        for (Layer layer : layers) {
            layer.blendMethod.applyBlending();
            GL11.glPushMatrix();
            TexturePackAPI.bindTexture(layer.textureName);
            float offsetX = layer.offsetX;
            float offsetY = layer.offsetY;
            float scaleX = layer.scaleX;
            float scaleY = layer.scaleY;
            if (layer.debug) {
                offsetX += offsetXDelta;
                offsetY += offsetYDelta;
                scaleX += scaleXDelta;
                scaleY += scaleYDelta;
            }
            GL11.glTranslatef(offsetX, offsetY, 0.0f);
            GL11.glScalef(scaleX, scaleY, 1.0f);
            float layerAngle = (float) (angle * layer.rotationMultiplier + layer.rotationOffset);
            GL11.glRotatef(layerAngle, 0.0f, 0.0f, 1.0f);
            GL11.glCallList(drawList);
            GL11.glPopMatrix();
        }

        if (bindFB) {
            EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, 0);
        }
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
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        int glError = GL11.glGetError();
        if (glError != 0) {
            logger.severe("%s during %s update", GLU.gluErrorString(glError), icon.getName());
            ok = false;
        }
        return ok;
    }

    private static void drawBox() {
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
            frameBuffer = -1;
        }
        layers.clear();
        ok = false;
    }

    @Override
    public String toString() {
        return String.format("FancyDial{%s, %dx%d @ %d,%d}",
            name, getIconWidth(icon), getIconHeight(icon), icon.getX0(), icon.getY0()
        );
    }

    @Override
    protected void finalize() throws Throwable {
        finish();
        super.finalize();
    }

    private static int getIconWidth(Icon icon) {
        return Math.round(icon.getTextureWidth() * (icon.getNormalizedX1() - icon.getNormalizedX0()));
    }

    private static int getIconHeight(Icon icon) {
        return Math.round(icon.getTextureHeight() * (icon.getNormalizedY1() - icon.getNormalizedY0()));
    }

    private static double getAngle(Icon icon) {
        if (icon instanceof TextureCompass) {
            return ((TextureCompass) icon).currentAngle * 180.0 / Math.PI;
        } else if (icon instanceof TextureClock) {
            return ((TextureClock) icon).currentAngle * 360.0;
        } else {
            return 0.0;
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

    Layer newLayer(String filename, Properties properties, String suffix) {
        String textureName = MCPatcherUtils.getStringProperty(properties, "source" + suffix, "");
        if (textureName.equals("")) {
            return null;
        }
        boolean filter = MCPatcherUtils.getBooleanProperty(properties, "filter", false);
        if (filter && !textureName.startsWith("%blur%")) {
            textureName = "%blur%" + textureName;
        }
        if (!TexturePackAPI.hasResource(textureName)) {
            logger.error("%s: could not read %s", filename, textureName);
            return null;
        }
        float scaleX = MCPatcherUtils.getFloatProperty(properties, "scaleX" + suffix, 1.0f);
        float scaleY = MCPatcherUtils.getFloatProperty(properties, "scaleY" + suffix, 1.0f);
        float offsetX = MCPatcherUtils.getFloatProperty(properties, "offsetX" + suffix, 0.0f);
        float offsetY = MCPatcherUtils.getFloatProperty(properties, "offsetY" + suffix, 0.0f);
        float angleMultiplier = MCPatcherUtils.getFloatProperty(properties, "rotationSpeed" + suffix, 0.0f);
        float angleOffset = MCPatcherUtils.getFloatProperty(properties, "rotationOffset" + suffix, 0.0f);
        String blend = MCPatcherUtils.getStringProperty(properties, "blend" + suffix, "alpha");
        BlendMethod blendMethod = BlendMethod.parse(blend);
        if (blendMethod == null) {
            logger.error("%s: unknown blend method %s", blend);
            return null;
        }
        boolean debug = MCPatcherUtils.getBooleanProperty(properties, "debug" + suffix, false);
        return new Layer(textureName, scaleX, scaleY, offsetX, offsetY, angleMultiplier, angleOffset, blendMethod, debug);
    }

    private class Layer {
        final String textureName;
        final float scaleX;
        final float scaleY;
        final float offsetX;
        final float offsetY;
        final float rotationMultiplier;
        final float rotationOffset;
        final BlendMethod blendMethod;
        final boolean debug;

        Layer(String textureName, float scaleX, float scaleY, float offsetX, float offsetY, float rotationMultiplier, float rotationOffset, BlendMethod blendMethod, boolean debug) {
            this.textureName = textureName;
            this.scaleX = scaleX;
            this.scaleY = scaleY;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.rotationMultiplier = rotationMultiplier;
            this.rotationOffset = rotationOffset;
            this.blendMethod = blendMethod;
            this.debug = debug;
        }

        @Override
        public String toString() {
            return String.format("Layer{%s %f %f %+f %+f x%f}", textureName, scaleX, scaleY, offsetX, offsetY, rotationMultiplier);
        }
    }
}
