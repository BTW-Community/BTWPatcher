package com.prupe.mcpatcher.hd;

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
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.*;

public class FancyDial {
    private static final MCLogger logger = MCLogger.getLogger(MCPatcherUtils.CUSTOM_ANIMATIONS, "Animation");

    private static final ResourceAddress ITEMS_PNG = new ResourceAddress("textures/atlas/items.png");
    private static final double ANGLE_UNSET = Double.MAX_VALUE;

    private static final boolean fboSupported = GLContext.getCapabilities().GL_EXT_framebuffer_object;
    private static final boolean gl13Supported = GLContext.getCapabilities().OpenGL13;
    private static final boolean enableCompass = Config.getBoolean(MCPatcherUtils.EXTENDED_HD, "fancyCompass", true);
    private static final boolean enableClock = Config.getBoolean(MCPatcherUtils.EXTENDED_HD, "fancyClock", true);
    private static final boolean useGL13 = Config.getBoolean(MCPatcherUtils.EXTENDED_HD, "useGL13", true);
    private static final boolean useScratchTexture = Config.getBoolean(MCPatcherUtils.EXTENDED_HD, "useScratchTexture", true);
    private static final int glAttributes;
    private static boolean initialized;
    private static boolean inUpdateAll;
    private static final int drawList = GL11.glGenLists(1);

    private static final Field subTexturesField;

    private static final Map<TextureStitched, Properties> setupInfo = new WeakHashMap<TextureStitched, Properties>();
    private static final Map<TextureStitched, FancyDial> instances = new WeakHashMap<TextureStitched, FancyDial>();

    private final TextureStitched icon;
    private final String name;
    private final int x0;
    private final int y0;
    private final int width;
    private final int height;
    private final boolean needExtraUpdate;
    private final int itemsTexture;
    private int scratchTexture;
    private final ByteBuffer scratchTextureBuffer;
    private int frameBuffer;
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
        logger.config("fbo: supported=%s", fboSupported);
        logger.config("GL13: supported=%s, enabled=%s", gl13Supported, useGL13);

        int bits = GL11.GL_VIEWPORT_BIT | GL11.GL_SCISSOR_BIT | GL11.GL_DEPTH_BITS | GL11.GL_LIGHTING_BIT;
        if (gl13Supported && useGL13) {
            bits |= GL13.GL_MULTISAMPLE_BIT;
        }
        glAttributes = bits;

        Field field = null;
        try {
            for (Field f : TextureStitched.class.getDeclaredFields()) {
                if (List.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    field = f;
                    break;
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        subTexturesField = field;

        GL11.glNewList(drawList, GL11.GL_COMPILE);
        drawBox();
        GL11.glEndList();
    }

    public static void setup(TextureStitched icon) {
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
        Properties properties = TexturePackAPI.getProperties(new ResourceAddress("textures/items/" + name + ".properties"));
        if (properties != null) {
            logger.fine("found custom %s", name);
            setupInfo.put(icon, properties);
        }
    }

    public static boolean update(TextureStitched icon) {
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
            List<TextureStitched> keys = new ArrayList<TextureStitched>();
            keys.addAll(setupInfo.keySet());
            for (TextureStitched icon : keys) {
                getInstance(icon);
            }
        }
        inUpdateAll = true;
        for (FancyDial instance : instances.values()) {
            if (instance != null && instance.needExtraUpdate) {
                instance.icon.updateAnimation();
            }
        }
        inUpdateAll = false;
    }

    static void postUpdateAll() {
        if (!initialized) {
            return;
        }
        for (FancyDial instance : instances.values()) {
            if (instance != null) {
                instance.postRender();
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
        try {
            FancyDial instance = new FancyDial(icon, properties);
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

    private FancyDial(TextureStitched icon, Properties properties) {
        this.icon = icon;
        name = icon.getIconName();
        x0 = icon.getX0();
        y0 = icon.getY0();
        width = icon.getWidth();
        height = icon.getHeight();
        needExtraUpdate = !hasAnimation(icon);
        if (needExtraUpdate) {
            logger.fine("%s needs direct .update() call", icon.getIconName());
        }

        TexturePackAPI.bindTexture(ITEMS_PNG);
        itemsTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);

        final int targetTexture;
        if (useScratchTexture) {
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            scratchTexture = GL11.glGenTextures();
            MipmapHelper.setupTexture(scratchTexture, image, false, false, new ResourceAddress("textures/items/" + name + "_scratch"));
            targetTexture = scratchTexture;
            scratchTextureBuffer = ByteBuffer.allocateDirect(4 * width * height);
            logger.fine("rendering %s to %dx%d scratch texture %d", name, width, height, scratchTexture);
        } else {
            scratchTexture = -1;
            scratchTextureBuffer = null;
            logger.fine("rendering %s directly to %s", name, ITEMS_PNG);
            targetTexture = itemsTexture;
        }

        if (itemsTexture < 0) {
            logger.severe("could not get items texture");
            return;
        }

        logger.fine("setting up %s", this);

        boolean debug = false;
        for (int i = 0; ; i++) {
            Layer layer = newLayer("textures/items/" + name + ".properties", properties, "." + i);
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

        frameBuffer = EXTFramebufferObject.glGenFramebuffersEXT();
        if (frameBuffer < 0) {
            logger.severe("could not get framebuffer object");
            return;
        }
        EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, frameBuffer);
        EXTFramebufferObject.glFramebufferTexture2DEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT, GL11.GL_TEXTURE_2D, targetTexture, 0);
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
                ByteBuffer byteBuffer = scratchTextureBuffer == null ? ByteBuffer.allocateDirect(4 * width * height) : scratchTextureBuffer;
                IntBuffer intBuffer = byteBuffer.asIntBuffer();
                int[] argb = new int[width * height];
                EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, frameBuffer);
                File path = MCPatcherUtils.getMinecraftPath("custom_" + name + ".png");
                logger.info("generating %d %s frames", outputFrames, name);
                for (int i = 0; i < outputFrames; i++) {
                    render(i * (360.0 / outputFrames), false);
                    if (scratchTexture < 0) {
                        byteBuffer.position(0);
                        GL11.glReadPixels(x0, y0, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, byteBuffer);
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
        if (scratchTexture >= 0) {
            GL11.glViewport(0, 0, width, height);
        } else {
            GL11.glViewport(x0, y0, width, height);
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            GL11.glScissor(x0, y0, width, height);
        }

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
            logger.severe("%s during %s update", GLU.gluErrorString(glError), icon.getIconName());
            ok = false;
        } else if (!inUpdateAll) {
            postRender();
        }
        return ok;
    }

    private void postRender() {
        if (ok && !skipPostRender && scratchTexture >= 0) {
            TexturePackAPI.bindTexture(scratchTexture);
            scratchTextureBuffer.position(0);
            GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, scratchTextureBuffer);
            scratchTextureBuffer.position(0);
            TexturePackAPI.bindTexture(itemsTexture);
            GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, x0, y0, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, scratchTextureBuffer);
        }
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
        if (scratchTexture >= 0) {
            TexturePackAPI.deleteTexture(scratchTexture);
            scratchTexture = -1;
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

    private static boolean hasAnimation(Icon icon) {
        if (icon instanceof TextureStitched && subTexturesField != null) {
            try {
                List list = (List) subTexturesField.get(icon);
                return list != null && list.size() > 1;
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        return true;
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

    Layer newLayer(String filename, Properties properties, String suffix) {
        ResourceAddress textureName = TexturePackAPI.parseResourceAddress(MCPatcherUtils.getStringProperty(properties, "source" + suffix, ""));
        if (textureName == null) {
            return null;
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
            logger.error("%s: unknown blend method %s", filename, blend);
            return null;
        }
        boolean debug = MCPatcherUtils.getBooleanProperty(properties, "debug" + suffix, false);
        return new Layer(textureName, scaleX, scaleY, offsetX, offsetY, angleMultiplier, angleOffset, blendMethod, debug);
    }

    private class Layer {
        final ResourceAddress textureName;
        final float scaleX;
        final float scaleY;
        final float offsetX;
        final float offsetY;
        final float rotationMultiplier;
        final float rotationOffset;
        final BlendMethod blendMethod;
        final boolean debug;

        Layer(ResourceAddress textureName, float scaleX, float scaleY, float offsetX, float offsetY, float rotationMultiplier, float rotationOffset, BlendMethod blendMethod, boolean debug) {
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
