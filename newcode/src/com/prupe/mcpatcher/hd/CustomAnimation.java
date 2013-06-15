package com.prupe.mcpatcher.hd;

import com.prupe.mcpatcher.*;
import net.minecraft.src.ResourceAddress;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.List;

public class CustomAnimation implements Comparable<CustomAnimation> {
    private static final MCLogger logger = MCLogger.getLogger(MCPatcherUtils.CUSTOM_ANIMATIONS, "Animation");

    private static final boolean enable = Config.getBoolean(MCPatcherUtils.EXTENDED_HD, "animations", true);
    private static final Map<ResourceAddress, Properties> pending = new HashMap<ResourceAddress, Properties>();
    private static final List<CustomAnimation> animations = new ArrayList<CustomAnimation>();

    private final ResourceAddress propertiesName;
    private final ResourceAddress dstName;
    private final ResourceAddress srcName;
    private final int mipmapLevel;
    private final ByteBuffer imageData;
    private final int x;
    private final int y;
    private final int w;
    private final int h;

    private int currentFrame;
    private int currentDelay;
    private int numFrames;
    private int[] tileOrder;
    private int[] tileDelay;
    private final int numTiles;
    private boolean error;

    static {
        TexturePackChangeHandler.register(new TexturePackChangeHandler(MCPatcherUtils.EXTENDED_HD, 1) {
            @Override
            public void beforeChange() {
                if (!pending.isEmpty()) {
                    logger.fine("%d animations were never registered:", pending.size());
                    for (ResourceAddress resource : pending.keySet()) {
                        logger.fine("  %s", resource);
                    }
                    pending.clear();
                }
                animations.clear();
                MipmapHelper.reset();
                FancyDial.refresh();
            }

            @Override
            public void afterChange() {
                if (enable) {
                    for (ResourceAddress resource : TexturePackAPI.listResources("anim", ".properties", true, true, false)) {
                        Properties properties = TexturePackAPI.getProperties(resource);
                        if (properties != null) {
                            pending.put(resource, properties);
                        }
                    }
                }
            }
        });
    }

    public static void updateAll() {
        FancyDial.updateAll();
        if (!pending.isEmpty()) {
            try {
                checkPendingAnimations();
            } catch (Throwable e) {
                e.printStackTrace();
                logger.error("%d remaining animations cleared", pending.size());
                pending.clear();
            }
        }
        for (CustomAnimation animation : animations) {
            animation.update();
        }
        FancyDial.postUpdateAll();
    }

    private static void checkPendingAnimations() {
        List<ResourceAddress> done = new ArrayList<ResourceAddress>();
        for (Map.Entry<ResourceAddress, Properties> entry : pending.entrySet()) {
            ResourceAddress name = entry.getKey();
            Properties properties = entry.getValue();
            ResourceAddress textureName = TexturePackAPI.parseResourceAddress(MCPatcherUtils.getStringProperty(properties, "to", ""));
            if (TexturePackAPI.isTextureLoaded(textureName)) {
                addStrip(name, properties);
                done.add(name);
            }
        }
        if (!done.isEmpty()) {
            for (ResourceAddress name : done) {
                pending.remove(name);
            }
            Collections.sort(animations);
        }
    }

    private static void addStrip(ResourceAddress propertiesName, Properties properties) {
        ResourceAddress dstName = TexturePackAPI.parseResourceAddress(properties.getProperty("to", ""));
        if (dstName == null) {
            logger.error("%s: missing to= property");
            return;
        }
        ResourceAddress srcName = TexturePackAPI.parseResourceAddress(properties.getProperty("from", ""));
        if (srcName == null) {
            logger.error("%s: missing to= property");
            return;
        }
        BufferedImage srcImage = TexturePackAPI.getImage(srcName);
        if (srcImage == null) {
            logger.error("%s: image %s not found in texture pack", propertiesName, srcName);
            return;
        }
        int x = MCPatcherUtils.getIntProperty(properties, "x", 0);
        int y = MCPatcherUtils.getIntProperty(properties, "y", 0);
        int w = MCPatcherUtils.getIntProperty(properties, "w", 0);
        int h = MCPatcherUtils.getIntProperty(properties, "h", 0);
        if (dstName.getPath().startsWith("textures/atlas/")) {
            logger.error("%s: animations cannot have a target of %s", dstName);
            return;
        }
        if (x < 0 || y < 0 || w <= 0 || h <= 0) {
            logger.error("%s: %s has invalid dimensions x=%d,y=%d,w=%d,h=%d", propertiesName, srcName, x, y, w, h);
            return;
        }
        TexturePackAPI.bindTexture(dstName);
        int dstWidth = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
        int dstHeight = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
        int levels = MipmapHelper.getMipmapLevelsForCurrentTexture();
        if (x + w > dstWidth || y + h > dstHeight) {
            logger.error("%s: %s dimensions x=%d,y=%d,w=%d,h=%d exceed %s size %dx%d",
                propertiesName, srcName, x, y, w, h, dstName, dstWidth, dstHeight
            );
            return;
        }
        int width = srcImage.getWidth();
        int height = srcImage.getHeight();
        if (width != w) {
            srcImage = resizeImage(srcImage, w);
            width = srcImage.getWidth();
            height = srcImage.getHeight();
        }
        if (width != w || height < h) {
            logger.error("%s: %s dimensions %dx%d do not match %dx%d", propertiesName, srcName, width, height, w, h);
            return;
        }
        ByteBuffer imageData = ByteBuffer.allocateDirect(4 * width * height);
        int[] argb = new int[width * height];
        byte[] rgba = new byte[4 * width * height];
        srcImage.getRGB(0, 0, width, height, argb, 0, width);
        ARGBtoRGBA(argb, rgba);
        imageData.put(rgba).flip();
        for (int mipmapLevel = 0; mipmapLevel <= levels; mipmapLevel++) {
            add(new CustomAnimation(propertiesName, srcName, dstName, mipmapLevel, x, y, w, h, imageData, height / h, properties));
            if (((x | y | w | h) & 0x1) != 0 || w <= 0 || h <= 0) {
                break;
            }
            ByteBuffer newImage = ByteBuffer.allocateDirect(width * height);
            MipmapHelper.scaleHalf(imageData.asIntBuffer(), width, height, newImage.asIntBuffer(), 0);
            imageData = newImage;
            width >>= 1;
            height >>= 1;
            x >>= 1;
            y >>= 1;
            w >>= 1;
            h >>= 1;
        }
    }

    private static void add(CustomAnimation animation) {
        if (animation != null) {
            animations.add(animation);
            if (animation.mipmapLevel == 0) {
                logger.fine("new %s", animation);
            }
        }
    }

    private CustomAnimation(ResourceAddress propertiesName, ResourceAddress srcName, ResourceAddress dstName, int mipmapLevel, int x, int y, int w, int h, ByteBuffer imageData, int numFrames, Properties properties) {
        this.propertiesName = propertiesName;
        this.srcName = srcName;
        this.dstName = dstName;
        this.mipmapLevel = mipmapLevel;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.imageData = imageData;
        this.numFrames = numFrames;
        currentFrame = -1;
        numTiles = numFrames;
        loadProperties(properties);
    }

    void update() {
        if (error) {
            return;
        }
        int texture = TexturePackAPI.getTextureIfLoaded(dstName);
        if (texture < 0) {
            return;
        }
        if (--currentDelay > 0) {
            return;
        }
        if (++currentFrame >= numFrames) {
            currentFrame = 0;
        }
        TexturePackAPI.bindTexture(texture);
        update(texture, 0, 0);
        int glError = GL11.glGetError();
        if (glError != 0) {
            logger.severe("%s: %s", this, GLU.gluErrorString(glError));
            error = true;
            return;
        }
        currentDelay = getDelay();
    }

    public int compareTo(CustomAnimation o) {
        return dstName.getPath().compareTo(o.dstName.getPath());
    }

    @Override
    public String toString() {
        return String.format("CustomAnimation{%s %s %dx%d -> %s%s @ %d,%d (%d frames)}",
            propertiesName, srcName, w, h, dstName, (mipmapLevel > 0 ? "#" + mipmapLevel : ""), x, y, numFrames
        );
    }

    private static void ARGBtoRGBA(int[] src, byte[] dest) {
        for (int i = 0; i < src.length; i++) {
            int v = src[i];
            dest[(i * 4) + 3] = (byte) ((v >> 24) & 0xff);
            dest[(i * 4) + 0] = (byte) ((v >> 16) & 0xff);
            dest[(i * 4) + 1] = (byte) ((v >> 8) & 0xff);
            dest[(i * 4) + 2] = (byte) ((v >> 0) & 0xff);
        }
    }

    private static BufferedImage resizeImage(BufferedImage image, int width) {
        if (width == image.getWidth()) {
            return image;
        }
        int height = image.getHeight() * width / image.getWidth();
        logger.finer("resizing to %dx%d", width, height);
        BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics2D = newImage.createGraphics();
        graphics2D.drawImage(image, 0, 0, width, height, null);
        return newImage;
    }

    private void loadProperties(Properties properties) {
        loadTileOrder(properties);
        if (tileOrder == null) {
            tileOrder = new int[numFrames];
            for (int i = 0; i < numFrames; i++) {
                tileOrder[i] = i % numTiles;
            }
        }
        tileDelay = new int[numFrames];
        loadTileDelay(properties);
        for (int i = 0; i < numFrames; i++) {
            tileDelay[i] = Math.max(tileDelay[i], 1);
        }
    }

    private void loadTileOrder(Properties properties) {
        if (properties == null) {
            return;
        }
        int i = 0;
        for (; getIntValue(properties, "tile.", i) != null; i++) {
        }
        if (i > 0) {
            numFrames = i;
            tileOrder = new int[numFrames];
            for (i = 0; i < numFrames; i++) {
                tileOrder[i] = Math.abs(getIntValue(properties, "tile.", i)) % numTiles;
            }
        }
    }

    private void loadTileDelay(Properties properties) {
        if (properties == null) {
            return;
        }
        Integer defaultValue = getIntValue(properties, "duration");
        for (int i = 0; i < numFrames; i++) {
            Integer value = getIntValue(properties, "duration.", i);
            if (value != null) {
                tileDelay[i] = value;
            } else if (defaultValue != null) {
                tileDelay[i] = defaultValue;
            }
        }
    }

    private static Integer getIntValue(Properties properties, String key) {
        try {
            String value = properties.getProperty(key);
            if (value != null && value.matches("^\\d+$")) {
                return Integer.parseInt(value);
            }
        } catch (NumberFormatException e) {
        }
        return null;
    }

    private static Integer getIntValue(Properties properties, String prefix, int index) {
        return getIntValue(properties, prefix + index);
    }

    private void update(int texture, int dx, int dy) {
        GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, mipmapLevel, x + dx, y + dy, w, h, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) imageData.position(4 * w * h * tileOrder[currentFrame]));
    }

    private int getDelay() {
        return tileDelay[currentFrame];
    }
}
