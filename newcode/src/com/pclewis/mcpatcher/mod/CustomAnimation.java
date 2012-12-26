package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.MCLogger;
import com.pclewis.mcpatcher.MCPatcherUtils;
import com.pclewis.mcpatcher.TexturePackAPI;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Random;

public class CustomAnimation {
    private static final MCLogger logger = MCLogger.getLogger(MCPatcherUtils.HD_TEXTURES);

    private static final String CLASS_NAME = CustomAnimation.class.getSimpleName();

    private static final Random rand = new Random();
    private static final ArrayList<CustomAnimation> animations = new ArrayList<CustomAnimation>();

    private static int boundTexture = -1;

    private final String dstName;
    private final String srcName;
    private final int mipmapLevel;
    private final ByteBuffer imageData;
    private final int tileCount;
    private final int x;
    private final int y;
    private final int w;
    private final int h;

    private int currentFrame;
    private int currentDelay;
    private int numFrames;
    private boolean error;

    private Delegate delegate;

    public static void updateAll() {
        boundTexture = -1;
        for (CustomAnimation animation : animations) {
            animation.update();
        }
    }

    static void clear() {
        animations.clear();
    }

    static void addStrip(Properties properties) {
        if (properties == null) {
            return;
        }
        try {
            String textureName = properties.getProperty("to", "");
            String srcName = properties.getProperty("from", "");
            int tileCount = Integer.parseInt(properties.getProperty("tiles", "1"));
            int x = Integer.parseInt(properties.getProperty("x", ""));
            int y = Integer.parseInt(properties.getProperty("y", ""));
            int w = Integer.parseInt(properties.getProperty("w", ""));
            int h = Integer.parseInt(properties.getProperty("h", ""));
            if (!"".equals(textureName) && !"".equals(srcName)) {
                newStrip(textureName, tileCount, srcName, TexturePackAPI.getImage(srcName), x, y, w, h, properties);
            }
        } catch (NumberFormatException e) {
        }
    }

    static void addStripOrTile(String textureName, String name, int tileNumber, int tileCount, int minScrollDelay, int maxScrollDelay) {
        if (!addStrip(textureName, name, tileNumber, tileCount)) {
            add(newTile(textureName, tileCount, tileNumber, minScrollDelay, maxScrollDelay));
        }
    }

    static boolean addStrip(String textureName, String name, int tileNumber, int tileCount) {
        String srcName = "/anim/custom_" + name + ".png";
        BufferedImage srcImage = TexturePackAPI.getImage(srcName);
        if (srcImage == null) {
            return false;
        }
        int tileSize = getTileSize(textureName);
        newStrip(textureName, tileCount, srcName, srcImage, (tileNumber % 16) * tileSize, (tileNumber / 16) * tileSize, tileSize, tileSize, null);
        return true;
    }

    private static void add(CustomAnimation animation) {
        if (animation != null) {
            animations.add(animation);
            if (animation.mipmapLevel == 0) {
                logger.fine("new %s", animation);
            }
        }
    }

    private static void newStrip(String dstName, int tileCount, String srcName, BufferedImage srcImage, int x, int y, int w, int h, Properties properties) {
        if (srcImage == null) {
            logger.severe("%s: image %s not found in texture pack", CLASS_NAME, srcName);
            return;
        }
        if (x < 0 || y < 0 || w <= 0 || h <= 0 || tileCount <= 0) {
            logger.severe("%s: %s invalid dimensions x=%d,y=%d,w=%d,h=%d,count=%d", CLASS_NAME, srcName, x, y, w, h, tileCount);
            return;
        }
        int textureID = MCPatcherUtils.getMinecraft().renderEngine.getTexture(dstName);
        if (textureID <= 0) {
            logger.severe("%s: invalid id %d for texture %s", CLASS_NAME, textureID, dstName);
            return;
        }
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
        int dstWidth = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
        int dstHeight = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
        int levels = MipmapHelper.getMipmapLevels();
        if (x + tileCount * w > dstWidth || y + tileCount * h > dstHeight) {
            logger.severe("%s: %s invalid dimensions x=%d,y=%d,w=%d,h=%d,count=%d", CLASS_NAME, srcName, x, y, w, h, tileCount);
            return;
        }
        int width = srcImage.getWidth();
        int height = srcImage.getHeight();
        if (width != w) {
            srcImage = TextureUtils.resizeImage(srcImage, w);
            width = srcImage.getWidth();
            height = srcImage.getHeight();
        }
        if (width != w || height < h) {
            logger.severe("%s: %s dimensions %dx%d do not match %dx%d", CLASS_NAME, srcName, width, height, w, h);
            return;
        }
        ByteBuffer imageData = ByteBuffer.allocateDirect(4 * width * height);
        int[] argb = new int[width * height];
        byte[] rgba = new byte[4 * width * height];
        srcImage.getRGB(0, 0, width, height, argb, 0, width);
        ARGBtoRGBA(argb, rgba);
        imageData.put(rgba).flip();
        for (int mipmapLevel = 0; mipmapLevel <= levels; mipmapLevel++) {
            add(new CustomAnimation(srcName, dstName, mipmapLevel, tileCount, x, y, w, h, imageData, height / h, properties));
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

    private static CustomAnimation newTile(String textureName, int tileCount, int tileNumber, int minScrollDelay, int maxScrollDelay) {
        int tileSize = getTileSize(textureName);
        int x = (tileNumber % 16) * tileSize;
        int y = (tileNumber / 16) * tileSize;
        int w = tileSize;
        int h = tileSize;
        if (x < 0 || y < 0 || w <= 0 || h <= 0 || x + tileCount * w > 16 * tileSize || y + tileCount * h > 16 * tileSize) {
            logger.severe("%s: %s invalid dimensions x=%d,y=%d,w=%d,h=%d", CLASS_NAME, textureName, x, y, w, h);
            return null;
        }
        try {
            return new CustomAnimation(textureName, tileCount, x, y, w, h, minScrollDelay, maxScrollDelay);
        } catch (IOException e) {
            return null;
        }
    }

    private CustomAnimation(String srcName, String dstName, int mipmapLevel, int tileCount, int x, int y, int w, int h, ByteBuffer imageData, int numFrames, Properties properties) {
        this.srcName = srcName;
        this.dstName = dstName;
        this.mipmapLevel = mipmapLevel;
        this.tileCount = tileCount;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.imageData = imageData;
        this.numFrames = numFrames;
        currentFrame = -1;
        delegate = new Strip(properties);
    }

    private CustomAnimation(String dstName, int tileCount, int x, int y, int w, int h, int minScrollDelay, int maxScrollDelay) throws IOException {
        this.srcName = dstName;
        this.dstName = dstName;
        this.mipmapLevel = 0;
        this.tileCount = tileCount;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.imageData = ByteBuffer.allocateDirect(4 * w * h);
        this.numFrames = h;
        currentFrame = -1;
        delegate = new Tile(minScrollDelay, maxScrollDelay);
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
        if (texture != boundTexture) {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
            boundTexture = texture;
        }
        for (int i = 0; i < tileCount; i++) {
            for (int j = 0; j < tileCount; j++) {
                delegate.update(texture, i * w, j * h);
                int glError = GL11.glGetError();
                if (glError != 0) {
                    logger.severe("%s: %s", this, GLU.gluErrorString(glError));
                    error = true;
                    return;
                }
            }
        }
        currentDelay = delegate.getDelay();
    }

    @Override
    public String toString() {
        return String.format("%s %s %dx%d -> %s%s @ %d,%d (%d frames)",
            CLASS_NAME, srcName, w, h, dstName, (mipmapLevel > 0 ? "#" + mipmapLevel : ""), x, y, numFrames
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

    private static int getTileSize(String textureName) {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, MCPatcherUtils.getMinecraft().renderEngine.getTexture(textureName));
        return GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH) / 16;
    }

    private interface Delegate {
        public void update(int texture, int dx, int dy);

        public int getDelay();
    }

    private class Tile implements Delegate {
        private final int minScrollDelay;
        private final int maxScrollDelay;

        Tile(int minScrollDelay, int maxScrollDelay) throws IOException {
            this.minScrollDelay = minScrollDelay;
            this.maxScrollDelay = maxScrollDelay;
            error = this.minScrollDelay < 0;
            BufferedImage tiles = TexturePackAPI.getImage(dstName);
            int rgbInt[] = new int[w * h];
            byte rgbByte[] = new byte[4 * w * h];
            tiles.getRGB(x, y, w, h, rgbInt, 0, w);
            ARGBtoRGBA(rgbInt, rgbByte);
            imageData.put(rgbByte);
        }

        public void update(int texture, int dx, int dy) {
            int rowOffset = h - currentFrame;
            GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, mipmapLevel, x + dx, y + dy + h - rowOffset, w, rowOffset, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) imageData.position(0));
            GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, mipmapLevel, x + dx, y + dy, w, h - rowOffset, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) imageData.position(4 * w * rowOffset));
        }

        public int getDelay() {
            if (maxScrollDelay > 0) {
                return rand.nextInt(maxScrollDelay - minScrollDelay + 1) + minScrollDelay;
            } else {
                return 0;
            }
        }
    }

    private class Strip implements Delegate {
        private int[] tileOrder;
        private int[] tileDelay;
        private final int numTiles;

        Strip(Properties properties) {
            numTiles = numFrames;
            if (properties == null) {
                properties = TexturePackAPI.getProperties(srcName.replace(".png", ".properties"));
            }
            loadProperties(properties);
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

        private Integer getIntValue(Properties properties, String key) {
            try {
                String value = properties.getProperty(key);
                if (value != null && value.matches("^\\d+$")) {
                    return Integer.parseInt(value);
                }
            } catch (NumberFormatException e) {
            }
            return null;
        }

        private Integer getIntValue(Properties properties, String prefix, int index) {
            return getIntValue(properties, prefix + index);
        }

        public void update(int texture, int dx, int dy) {
            GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, mipmapLevel, x + dx, y + dy, w, h, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) imageData.position(4 * w * h * tileOrder[currentFrame]));
        }

        public int getDelay() {
            return tileDelay[currentFrame];
        }
    }
}
