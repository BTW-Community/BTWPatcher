package com.prupe.mcpatcher.hd;

import com.prupe.mcpatcher.*;
import net.minecraft.client.Minecraft;
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
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.*;

public class FancyDial {
    private static final MCLogger logger = MCLogger.getLogger(MCPatcherUtils.CUSTOM_ANIMATIONS, "Animation");

    private static final ResourceLocation ITEMS_PNG = new ResourceLocation("textures/atlas/items.png");
    private static final double ANGLE_UNSET = Double.MAX_VALUE;
    private static final int NUM_SCRATCH_TEXTURES = 3;

    private static final boolean fboSupported = GLContext.getCapabilities().GL_EXT_framebuffer_object;
    private static final boolean gl13Supported = GLContext.getCapabilities().OpenGL13;
    private static final boolean enableCompass = Config.getBoolean(MCPatcherUtils.EXTENDED_HD, "fancyCompass", true);
    private static final boolean enableClock = Config.getBoolean(MCPatcherUtils.EXTENDED_HD, "fancyClock", true);
    private static final boolean useGL13;
    private static final boolean useScratchTexture = Config.getBoolean(MCPatcherUtils.EXTENDED_HD, "useScratchTexture", true);
    private static final int glAttributes;
    private static boolean initialized;
    private static final int drawList = GL11.glGenLists(1);

    private static final Map<TextureAtlasSprite, ResourceLocation> setupInfo = new WeakHashMap<TextureAtlasSprite, ResourceLocation>();
    private static final Map<TextureAtlasSprite, FancyDial> instances = new WeakHashMap<TextureAtlasSprite, FancyDial>();

    private final TextureAtlasSprite icon;
    private final ResourceLocation resource;
    private final String name;
    private final int x0;
    private final int y0;
    private final int width;
    private final int height;
    private final int itemsTexture;
    private final int[] scratchTexture = new int[NUM_SCRATCH_TEXTURES];
    private final ByteBuffer scratchBuffer;
    private final int[] frameBuffer = new int[NUM_SCRATCH_TEXTURES];
    private int scratchIndex;
    private int outputFrames;

    private boolean ok;
    private double lastAngle = ANGLE_UNSET;
    private boolean skipPostRender;

    private final List<Layer> layers = new ArrayList<Layer>();

    private InputHandler keyboard;
    private static final float STEP = 0.01f;
    private float scaleXDelta;
    private float scaleYDelta;
    private float offsetXDelta;
    private float offsetYDelta;

    static {
        useGL13 = gl13Supported && Config.getBoolean(MCPatcherUtils.EXTENDED_HD, "useGL13", true);

        logger.config("fbo: supported=%s", fboSupported);
        logger.config("GL13: supported=%s, enabled=%s", gl13Supported, useGL13);

        int bits = GL11.GL_VIEWPORT_BIT | GL11.GL_SCISSOR_BIT | GL11.GL_DEPTH_BITS | GL11.GL_LIGHTING_BIT;
        if (gl13Supported && useGL13) {
            bits |= GL13.GL_MULTISAMPLE_BIT;
        }
        glAttributes = bits;

        GL11.glNewList(drawList, GL11.GL_COMPILE);
        drawBox();
        GL11.glEndList();
    }

    public static void setup(TextureAtlasSprite icon) {
        if (!fboSupported) {
            return;
        }
        String name = icon.getIconName();
        if ("compass".equals(icon.getIconName())) {
            if (!enableCompass) {
                return;
            }
        } else if ("clock".equals(icon.getIconName())) {
            if (!enableClock) {
                return;
            }
        } else {
            logger.warning("ignoring custom animation for %s not compass or clock", icon.getIconName());
            return;
        }
        ResourceLocation resource = TexturePackAPI.newMCPatcherResourceLocation("dial/" + name + ".properties");
        if (TexturePackAPI.hasResource(resource)) {
            logger.fine("found custom %s (%s)", name, resource);
            setupInfo.put(icon, resource);
        }
    }

    public static boolean update(TextureAtlasSprite icon) {
        if (!initialized) {
            logger.finer("deferring %s update until initialization finishes", icon.getIconName());
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
            List<TextureAtlasSprite> keys = new ArrayList<TextureAtlasSprite>();
            keys.addAll(setupInfo.keySet());
            for (TextureAtlasSprite icon : keys) {
                getInstance(icon);
            }
        }
    }

    static void clear() {
        logger.finer("FancyDial.clear");
        for (FancyDial instance : instances.values()) {
            if (instance != null) {
                instance.finish();
            }
        }
        instances.clear();
        initialized = true;
    }

    static void registerAnimations() {
        TextureObject texture = TexturePackAPI.getTextureObject(ITEMS_PNG);
        if (texture instanceof TextureAtlas) {
            List<TextureAtlasSprite> animations = ((TextureAtlas) texture).animations;
            for (FancyDial instance : instances.values()) {
                if (!animations.contains(instance.icon)) {
                    logger.fine("registered %s animation", instance.name);
                    animations.add(instance.icon);
                }
            }
        }
    }

    private static FancyDial getInstance(TextureAtlasSprite icon) {
        ResourceLocation resource = setupInfo.get(icon);
        Properties properties = TexturePackAPI.getProperties(resource);
        setupInfo.remove(icon);
        if (properties == null) {
            return null;
        }
        try {
            FancyDial instance = new FancyDial(icon, resource, properties);
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

    private FancyDial(TextureAtlasSprite icon, ResourceLocation resource, Properties properties) {
        this.icon = icon;
        this.resource = resource;
        name = icon.getIconName();
        x0 = icon.getX0();
        y0 = icon.getY0();
        width = icon.getWidth();
        height = icon.getHeight();
        scratchBuffer = ByteBuffer.allocateDirect(4 * width * height);

        TexturePackAPI.bindTexture(ITEMS_PNG);
        itemsTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        if (itemsTexture < 0) {
            logger.severe("could not get items texture");
            return;
        }

        logger.fine("setting up %s", this);
        for (int i = 0; i < NUM_SCRATCH_TEXTURES; i++) {
            setupFrameBuffer(i);
        }

        boolean debug = false;
        for (int i = 0; ; i++) {
            Layer layer = newLayer(resource, properties, "." + i);
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
        keyboard = new InputHandler(name, debug);
        if (layers.size() < 2) {
            logger.error("custom %s needs at least two layers defined", name);
            return;
        }

        outputFrames = MCPatcherUtils.getIntProperty(properties, "outputFrames", 0);

        int glError = GL11.glGetError();
        if (glError != 0) {
            logger.severe("%s during %s setup", GLU.gluErrorString(glError), name);
            return;
        }
        ok = true;
    }

    private void setupFrameBuffer(int i) {
        int targetTexture;
        if (useScratchTexture) {
            targetTexture = scratchTexture[i] = GL11.glGenTextures();
            MipmapHelper.setupTexture(scratchTexture[i], width, height, TexturePackAPI.transformResourceLocation(resource, ".properties", "_scratch" + i).getPath());
            if (i == 0) {
                logger.fine("rendering %s to %dx%d scratch texture %d", name, width, height, scratchTexture[i]);
            }
        } else {
            scratchTexture[i] = -1;
            targetTexture = itemsTexture;
            if (i == 0) {
                logger.fine("rendering %s directly to %s", name, ITEMS_PNG);
            }
        }
        frameBuffer[i] = EXTFramebufferObject.glGenFramebuffersEXT();
        if (frameBuffer[i] < 0) {
            logger.severe("could not get framebuffer object");
            return;
        }
        EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, frameBuffer[i]);
        EXTFramebufferObject.glFramebufferTexture2DEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT, GL11.GL_TEXTURE_2D, targetTexture, 0);
        EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, 0);
    }

    private boolean render() {
        if (!ok) {
            return false;
        }

        boolean changed = true;
        if (!keyboard.isEnabled()) {
            changed = false;
        } else if (keyboard.isKeyPressed(Keyboard.KEY_NUMPAD2)) {
            scaleYDelta -= STEP;
        } else if (keyboard.isKeyPressed(Keyboard.KEY_NUMPAD8)) {
            scaleYDelta += STEP;
        } else if (keyboard.isKeyPressed(Keyboard.KEY_NUMPAD4)) {
            scaleXDelta -= STEP;
        } else if (keyboard.isKeyPressed(Keyboard.KEY_NUMPAD6)) {
            scaleXDelta += STEP;
        } else if (keyboard.isKeyPressed(Keyboard.KEY_DOWN)) {
            offsetYDelta += STEP;
        } else if (keyboard.isKeyPressed(Keyboard.KEY_UP)) {
            offsetYDelta -= STEP;
        } else if (keyboard.isKeyPressed(Keyboard.KEY_LEFT)) {
            offsetXDelta -= STEP;
        } else if (keyboard.isKeyPressed(Keyboard.KEY_RIGHT)) {
            offsetXDelta += STEP;
        } else if (keyboard.isKeyPressed(Keyboard.KEY_MULTIPLY)) {
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
            lastAngle = ANGLE_UNSET;
        }

        if (outputFrames > 0) {
            try {
                BufferedImage image = new BufferedImage(width, outputFrames * height, BufferedImage.TYPE_INT_ARGB);
                IntBuffer intBuffer = scratchBuffer.asIntBuffer();
                int[] argb = new int[width * height];
                EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, frameBuffer[0]);
                File path = MCPatcherUtils.getMinecraftPath("custom_" + name + ".png");
                logger.info("generating %d %s frames", outputFrames, name);
                for (int i = 0; i < outputFrames; i++) {
                    render(i * (360.0 / outputFrames), false);
                    if (scratchTexture[0] < 0) {
                        intBuffer.position(0);
                        GL11.glReadPixels(x0, y0, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, intBuffer);
                    }
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
        if (angle == lastAngle) {
            skipPostRender = true;
            return true;
        }
        skipPostRender = false;
        lastAngle = angle;
        GL11.glPushAttrib(glAttributes);
        if (useScratchTexture) {
            GL11.glViewport(0, 0, width, height);
        } else {
            GL11.glViewport(x0, y0, width, height);
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            GL11.glScissor(x0, y0, width, height);
        }

        if (bindFB) {
            EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, frameBuffer[(scratchIndex + NUM_SCRATCH_TEXTURES - 1) % NUM_SCRATCH_TEXTURES]);
        }

        boolean lightmapEnabled = false;
        if (gl13Supported) {
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
            logger.severe("%s during %s update", GLU.gluErrorString(glError), icon.getIconName());
            ok = false;
        } else {
            postRender();
        }
        return ok;
    }

    private void postRender() {
        int texture = scratchTexture[scratchIndex];
        if (ok && !skipPostRender && texture >= 0) {
            TexturePackAPI.bindTexture(texture);
            scratchBuffer.position(0);
            GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, scratchBuffer);
            scratchBuffer.position(0);
            TexturePackAPI.bindTexture(itemsTexture);
            GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, x0, y0, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, scratchBuffer);
        }
        scratchIndex = (scratchIndex + 1) % NUM_SCRATCH_TEXTURES;
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
        for (int i = 0; i < NUM_SCRATCH_TEXTURES; i++) {
            if (frameBuffer[i] >= 0) {
                EXTFramebufferObject.glDeleteFramebuffersEXT(frameBuffer[i]);
                frameBuffer[i] = -1;
            }
            if (scratchTexture[i] >= 0) {
                TexturePackAPI.deleteTexture(scratchTexture[i]);
                scratchTexture[i] = -1;
            }
        }
        layers.clear();
        ok = false;
    }

    @Override
    public String toString() {
        return String.format("FancyDial{%s, %dx%d @ %d,%d}", name, width, height, x0, y0);
    }

    @Override
    protected void finalize() throws Throwable {
        finish();
        super.finalize();
    }

    private static boolean hasAnimation(TextureAtlasSprite icon) {
        return icon.animationFrames.size() <= 1;
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

    Layer newLayer(ResourceLocation resource, Properties properties, String suffix) {
        String textureName = MCPatcherUtils.getStringProperty(properties, "source" + suffix, "");
        if (textureName.isEmpty()) {
            return null;
        }
        ResourceLocation textureResource = TexturePackAPI.parseResourceLocation(resource, textureName);
        if (textureResource == null) {
            return null;
        }
        if (!TexturePackAPI.hasResource(textureResource)) {
            logger.error("%s: could not read %s", resource, textureResource);
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
            logger.error("%s: unknown blend method %s", resource, blend);
            return null;
        }
        boolean debug = MCPatcherUtils.getBooleanProperty(properties, "debug" + suffix, false);
        return new Layer(textureResource, scaleX, scaleY, offsetX, offsetY, angleMultiplier, angleOffset, blendMethod, debug);
    }

    private class Layer {
        final ResourceLocation textureName;
        final float scaleX;
        final float scaleY;
        final float offsetX;
        final float offsetY;
        final float rotationMultiplier;
        final float rotationOffset;
        final BlendMethod blendMethod;
        final boolean debug;

        Layer(ResourceLocation textureName, float scaleX, float scaleY, float offsetX, float offsetY, float rotationMultiplier, float rotationOffset, BlendMethod blendMethod, boolean debug) {
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
