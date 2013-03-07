package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.Config;
import com.pclewis.mcpatcher.MCLogger;
import com.pclewis.mcpatcher.MCPatcherUtils;
import com.pclewis.mcpatcher.TexturePackAPI;
import net.minecraft.src.RenderEngine;
import net.minecraft.src.Texture;
import org.lwjgl.opengl.*;
import org.lwjgl.util.glu.GLU;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.*;

public class MipmapHelper {
    private static final MCLogger logger = MCLogger.getLogger(MCPatcherUtils.MIPMAP);

    private static final String MIPMAP_PROPERTIES = "/mipmap.properties";

    private static final boolean mipmapSupported;
    private static final boolean mipmapEnabled = Config.getBoolean(MCPatcherUtils.EXTENDED_HD, "mipmap", false);
    private static final int maxMipmapLevel = Config.getInt(MCPatcherUtils.EXTENDED_HD, "maxMipmapLevel", 3);
    private static final boolean useMipmap;
    private static final int mipmapAlignment = (1 << Config.getInt(MCPatcherUtils.EXTENDED_HD, "mipmapAlignment", 3)) - 1;
    private static final int byteBufferAllocation = Config.getInt(MCPatcherUtils.EXTENDED_HD, "byteBufferAllocation", 1);

    private static final boolean anisoSupported;
    private static final int anisoLevel;
    private static final int anisoMax;

    private static final boolean lodSupported;
    private static int lodBias;

    private static final HashMap<String, Reference<BufferedImage>> imagePool = new HashMap<String, Reference<BufferedImage>>();
    private static final HashMap<Integer, Reference<ByteBuffer>> bufferPool = new HashMap<Integer, Reference<ByteBuffer>>();

    private static int bgColorFix = 4;

    public static int currentLevel;

    private static final HashMap<String, Integer> mipmapType = new HashMap<String, Integer>();
    private static final int MIPMAP_NONE = 0;
    private static final int MIPMAP_BASIC = 1;
    private static final int MIPMAP_ALPHA = 2;

    private static Texture currentTexture;
    private static boolean flippedTextureLogged;

    static {
        mipmapSupported = GLContext.getCapabilities().OpenGL12;
        useMipmap = mipmapSupported && mipmapEnabled && maxMipmapLevel > 0;

        anisoSupported = GLContext.getCapabilities().GL_EXT_texture_filter_anisotropic;
        if (anisoSupported) {
            anisoMax = (int) GL11.glGetFloat(EXTTextureFilterAnisotropic.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT);
            checkGLError("glGetFloat(GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT)");
            anisoLevel = Math.max(Math.min(Config.getInt(MCPatcherUtils.EXTENDED_HD, "anisotropicFiltering", 1), anisoMax), 1);
        } else {
            anisoMax = anisoLevel = 1;
        }

        lodSupported = GLContext.getCapabilities().GL_EXT_texture_lod_bias;
        if (lodSupported) {
            lodBias = Config.getInt(MCPatcherUtils.EXTENDED_HD, "lodBias", 0);
        }

        logger.config("mipmap: supported=%s, enabled=%s, level=%d", mipmapSupported, mipmapEnabled, maxMipmapLevel);
        logger.config("anisotropic: supported=%s, level=%d, max=%d", anisoSupported, anisoLevel, anisoMax);
        logger.config("lod bias: supported=%s, bias=%d", lodSupported, lodBias);
    }

    public static void setupTexture(int target, int level, int internalFormat, int width, int height, int border, int format, int dataType, ByteBuffer buffer, Texture texture) {
        int[] byteOrder = (format == GL11.GL_RGBA ? new int[]{3, 0, 1, 2} : new int[]{3, 2, 1, 0});
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        byte[] rgba = new byte[4 * width * height];
        int[] argb = new int[width * height];
        buffer.position(0);
        buffer.get(rgba, 0, rgba.length);
        for (int i = 0; i < rgba.length; i += 4) {
            argb[i / 4] = ((rgba[i + byteOrder[0]] & 0xff) << 24) |
                ((rgba[i + byteOrder[1]] & 0xff) << 16) |
                ((rgba[i + byteOrder[2]] & 0xff) << 8) |
                (rgba[i + byteOrder[3]] & 0xff);
        }
        image.setRGB(0, 0, width, height, argb, 0, width);

        currentTexture = texture;
        setupTexture(MCPatcherUtils.getMinecraft().renderEngine, image, texture.getGLTexture(), false, false, texture.getName());
        currentTexture = null;
    }

    public static ByteBuffer allocateByteBuffer(int capacity) {
        if (byteBufferAllocation == 0) {
            return ByteBuffer.allocateDirect(capacity);
        } else {
            return ByteBuffer.allocate(capacity);
        }
    }

    public static void copySubTexture(Texture dst, Texture src, int x, int y, boolean flipped) {
        ByteBuffer srcBuffer = src.getByteBuffer();
        srcBuffer.position(0);
        if (byteBufferAllocation == 1 && !srcBuffer.isDirect()) {
            logger.finer("creating %d direct byte buffer for texture %s", srcBuffer.capacity(), src.getName());
            ByteBuffer newBuffer = ByteBuffer.allocateDirect(srcBuffer.capacity()).order(srcBuffer.order());
            newBuffer.put(srcBuffer).flip();
            src.byteBuffer = srcBuffer = newBuffer;
        }
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, dst.getGLTexture());
        int mipmaps = dst.useMipmaps ? getMipmapLevels() : 0;
        int width = src.getWidth();
        int height = src.getHeight();
        if (flipped && !flippedTextureLogged) {
            flippedTextureLogged = true;
            logger.warning("copySubTexture(%s, %s, %d, %d, %s): flipped texture not yet supported",
                dst.getName(), src.getName(), x, y, flipped
            );
        }
        for (int i = 0; ; i++) {
            if (byteBufferAllocation == 2 && !srcBuffer.isDirect()) {
                ByteBuffer newBuffer = getPooledBuffer(srcBuffer.capacity());
                newBuffer.put(srcBuffer).flip();
                srcBuffer = newBuffer;
            }
            GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, i, x, y, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, srcBuffer);
            checkGLError("%s -> %s: glTexSubImage2D(%d, %d, %d, %d, %d)",
                src.getName(), dst.getName(), i, x, y, width, height
            );
            ByteBuffer newBuffer = getPooledBuffer(width * height);
            scaleHalf(srcBuffer.asIntBuffer(), width, height, newBuffer.asIntBuffer(), 0);
            if (i >= mipmaps) {
                break;
            }
            width >>= 1;
            height >>= 1;
            x >>= 1;
            y >>= 1;
            srcBuffer = newBuffer;
        }
    }

    public static void setupTexture(RenderEngine renderEngine, BufferedImage image, int texture, boolean blurTexture, boolean clampTexture, String textureName) {
        if (texture < 0 || image == null) {
            return;
        }
        long s1 = System.currentTimeMillis();
        ArrayList<BufferedImage> mipmapImages = getMipmapsForTexture(image, textureName);
        long s2 = System.currentTimeMillis();
        setupTextureMipmaps(renderEngine, mipmapImages, texture, textureName, blurTexture, clampTexture);
        long s3 = System.currentTimeMillis();
        if (mipmapImages.size() > 1) {
            logger.finer("%s: generate %dms, setup %dms, total %dms", textureName, s2 - s1, s3 - s2, s3 - s1);
        }
    }

    public static void setupTexture(RenderEngine renderEngine, BufferedImage image, int texture, String textureName) {
        setupTexture(renderEngine, image, texture, renderEngine.blurTexture, renderEngine.clampTexture, textureName);
    }

    private static ArrayList<BufferedImage> getMipmapsForTexture(BufferedImage image, String textureName) {
        ArrayList<BufferedImage> mipmapImages = new ArrayList<BufferedImage>();
        mipmapImages.add(image);
        int type = getMipmapType(textureName, mipmapImages);
        if (type < MIPMAP_BASIC) {
            return mipmapImages;
        }
        int width = image.getWidth();
        int height = image.getHeight();
        if (getCustomMipmaps(mipmapImages, textureName, width, height)) {
            logger.fine("using %d custom mipmaps for %s", mipmapImages.size() - 1, textureName);
            return mipmapImages;
        }
        int mipmaps = getMipmapLevels(image.getWidth(), image.getHeight(), 2);
        if (mipmaps <= 0) {
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, mipmaps);
            type = MIPMAP_NONE;
            if (textureName != null) {
                mipmapType.put(textureName, type);
            }
            return mipmapImages;
        }
        logger.fine("generating %d mipmaps for %s, alpha=%s", mipmaps, textureName, type >= MIPMAP_ALPHA);
        image = convertToARGB(image);
        mipmapImages.set(0, image);
        int scale = 1 << bgColorFix;
        int gcd = gcd(width, height);
        if (bgColorFix > 0 && gcd % scale == 0 && ((gcd / scale) & (gcd / scale - 1)) == 0) {
            long s1 = System.currentTimeMillis();
            BufferedImage scaledImage = mipmapImages.get(mipmapImages.size() - 1);
            while (gcd(scaledImage.getWidth(), scaledImage.getHeight()) > scale) {
                scaledImage = scaleHalf(scaledImage);
            }
            long s2 = System.currentTimeMillis();
            setBackgroundColor(image, scaledImage);
            long s3 = System.currentTimeMillis();
            logger.finer("bg fix: scaling %dms, setbg %dms", s2 - s1, s3 - s2);
        }
        BufferedImage origImage = image;
        for (int i = 0; i < mipmaps; i++) {
            origImage = scaleHalf(origImage);
            if (type >= MIPMAP_ALPHA) {
                image = origImage;
            } else {
                image = getPooledImage(origImage.getWidth(), origImage.getHeight(), 1);
                origImage.copyData(image.getRaster());
                resetOnOffTransparency(image);
            }
            mipmapImages.add(image);
        }
        return mipmapImages;
    }

    private static void setupTextureMipmaps(RenderEngine renderEngine, ArrayList<BufferedImage> mipmapImages, int texture, String textureName, boolean blurTexture, boolean clampTexture) {
        try {
            int mipmaps = mipmapImages.size() - 1;
            for (currentLevel = 0; currentLevel <= mipmaps; currentLevel++) {
                if (currentLevel == 1) {
                    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST_MIPMAP_LINEAR);
                    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
                    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, mipmaps);
                    if (currentTexture != null) {
                        currentTexture.useMipmaps = true;
                        currentTexture.glMinFilter = GL11.GL_NEAREST_MIPMAP_LINEAR;
                        currentTexture.glMagFilter = GL11.GL_NEAREST;
                    }
                    checkGLError("set GL_TEXTURE_MAX_LEVEL = %d", mipmaps);
                    if (anisoSupported && anisoLevel > 1) {
                        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT, anisoLevel);
                        checkGLError("set GL_TEXTURE_MAX_ANISOTROPY_EXT = %f", anisoLevel);
                    }
                    if (lodSupported) {
                        GL11.glTexEnvi(EXTTextureLODBias.GL_TEXTURE_FILTER_CONTROL_EXT, EXTTextureLODBias.GL_TEXTURE_LOD_BIAS_EXT, lodBias);
                        checkGLError("set GL_TEXTURE_LOD_BIAS_EXT = %d", lodBias);
                    }
                }
                BufferedImage image = mipmapImages.get(currentLevel);
                renderEngine.setupTexture2(image, texture, blurTexture, clampTexture);
                checkGLError("setupTexture %s#%d", textureName, currentLevel);
                if (currentLevel > 0) {
                    logger.finest("%s mipmap level %d (%dx%d)", textureName, currentLevel, image.getWidth(), image.getHeight());
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            currentLevel = 0;
        }
    }

    static void reset() {
        bgColorFix = 4;
        mipmapType.clear();
        forceMipmapType("terrain", MIPMAP_BASIC);
        forceMipmapType("items", MIPMAP_NONE);
        Properties properties = TexturePackAPI.getProperties(MIPMAP_PROPERTIES);
        if (properties != null) {
            bgColorFix = MCPatcherUtils.getIntProperty(properties, "bgColorFix", 4);
            for (Map.Entry entry : properties.entrySet()) {
                if (entry.getKey() instanceof String && entry.getValue() instanceof String) {
                    String key = ((String) entry.getKey()).trim();
                    String value = ((String) entry.getValue()).trim().toLowerCase();
                    if (key.startsWith("/")) {
                        if (value.equals("none")) {
                            mipmapType.put(key, MIPMAP_NONE);
                        } else if (value.equals("basic") || value.equals("opaque")) {
                            mipmapType.put(key, MIPMAP_BASIC);
                        } else if (value.equals("alpha")) {
                            mipmapType.put(key, MIPMAP_ALPHA);
                        } else {
                            logger.error("%s: unknown value '%s' for %s", MIPMAP_PROPERTIES, value, key);
                        }
                    }
                }
            }
        }
    }

    private static boolean getCustomMipmaps(ArrayList<BufferedImage> mipmaps, String texture, int baseWidth, int baseHeight) {
        boolean added = false;
        if (useMipmap && texture != null && texture.startsWith("/")) {
            for (int i = 1; baseWidth > 0 && baseHeight > 0 && i <= maxMipmapLevel; i++) {
                baseWidth >>>= 1;
                baseHeight >>>= 1;
                String name = texture.replace(".png", "-mipmap" + i + ".png");
                BufferedImage image = TexturePackAPI.getImage(name);
                if (image == null) {
                    break;
                }
                int width = image.getWidth();
                int height = image.getHeight();
                if (width == baseWidth && height == baseHeight) {
                    mipmaps.add(image);
                    added = true;
                } else {
                    logger.error("%s has wrong size %dx%d (expecting %dx%d)", name, width, height, baseWidth, baseHeight);
                    break;
                }
            }
        }
        return added;
    }

    private static int getMipmapType(String texture, ArrayList<BufferedImage> mipmapImages) {
        BufferedImage image = mipmapImages == null ? null : mipmapImages.get(0);
        if (!useMipmap || texture == null) {
            return MIPMAP_NONE;
        } else if (mipmapType.containsKey(texture)) {
            return mipmapType.get(texture);
        } else if (texture.startsWith("%") ||
            texture.startsWith("##") ||
            texture.startsWith("/achievement/") ||
            texture.startsWith("/environment/") ||
            texture.startsWith("/font/") ||
            texture.startsWith("/gui/") ||
            texture.startsWith("/misc/") ||
            texture.startsWith("/terrain/") ||
            texture.startsWith("/title/") ||
            texture.contains("item")) {
            return MIPMAP_NONE;
        } else if (image == null) {
            return MIPMAP_BASIC;
        } else {
            image = convertToARGB(image);
            mipmapImages.set(0, image);
            IntBuffer buffer = getARGBAsIntBuffer(image);
            for (int i = 0; i < buffer.limit(); i++) {
                int alpha = buffer.get(i) >>> 24;
                if (alpha > 0x1a && alpha < 0xe5) {
                    logger.finer("%s alpha transparency? yes, by pixel search", texture);
                    mipmapType.put(texture, MIPMAP_ALPHA);
                    return MIPMAP_ALPHA;
                }
            }
            logger.finer("%s alpha transparency? no, by pixel search", texture);
            mipmapType.put(texture, MIPMAP_BASIC);
            return MIPMAP_BASIC;
        }
    }

    static void forceMipmapType(String texture, int type) {
        if (!useMipmap) {
            return;
        }
        boolean reload = false;
        if (mipmapType.containsKey(texture)) {
            reload = mipmapType.get(texture) != type;
            if (!reload) {
                return;
            }
        }
        mipmapType.put(texture, type);
        if (reload) {
            logger.finer("force %s -> %d (reloading)", texture, type);
            int id = TexturePackAPI.getTextureIfLoaded(texture);
            if (id >= 0) {
                setupTexture(MCPatcherUtils.getMinecraft().renderEngine, TexturePackAPI.getImage(texture), id, texture);
            }
        } else {
            logger.finer("force %s -> %d", texture, type);
        }
    }

    static int getMipmapLevels() {
        int filter = GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER);
        if (filter != GL11.GL_NEAREST_MIPMAP_LINEAR && filter != GL11.GL_NEAREST_MIPMAP_NEAREST) {
            return 0;
        }
        return GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL);
    }

    private static int gcd(int a, int b) {
        return BigInteger.valueOf(a).gcd(BigInteger.valueOf(b)).intValue();
    }

    private static int getMipmapLevels(int width, int height, int minSize) {
        int size = gcd(width, height);
        int mipmap;
        for (mipmap = 0; size >= minSize && ((size & 1) == 0) && mipmap < maxMipmapLevel; size >>= 1, mipmap++) {
        }
        return mipmap;
    }

    private static BufferedImage getPooledImage(int width, int height, int index) {
        String key = String.format("%dx%d#%d", width, height, index);
        Reference<BufferedImage> ref = imagePool.get(key);
        BufferedImage image = (ref == null ? null : ref.get());
        if (image == null) {
            image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            imagePool.put(key, new SoftReference<BufferedImage>(image));
        }
        return image;
    }

    private static ByteBuffer getPooledBuffer(int size) {
        Reference<ByteBuffer> ref = bufferPool.get(size);
        ByteBuffer buffer = (ref == null ? null : ref.get());
        if (buffer == null) {
            buffer = ByteBuffer.allocateDirect(size);
            bufferPool.put(size, new SoftReference<ByteBuffer>(buffer));
        }
        buffer.position(0);
        return buffer;
    }

    private static BufferedImage convertToARGB(BufferedImage image) {
        if (image == null) {
            return null;
        } else if (image.getType() == BufferedImage.TYPE_INT_ARGB) {
            return image;
        } else {
            int width = image.getWidth();
            int height = image.getHeight();
            logger.fine("converting %dx%d image to ARGB", width, height);
            BufferedImage newImage = getPooledImage(width, height, 0);
            Graphics2D graphics = newImage.createGraphics();
            Arrays.fill(getARGBAsIntBuffer(newImage).array(), 0);
            graphics.drawImage(image, 0, 0, null);
            return newImage;
        }
    }

    private static IntBuffer getARGBAsIntBuffer(BufferedImage image) {
        DataBuffer buffer = image.getRaster().getDataBuffer();
        if (buffer instanceof DataBufferInt) {
            return IntBuffer.wrap(((DataBufferInt) buffer).getData());
        } else if (buffer instanceof DataBufferByte) {
            return ByteBuffer.wrap(((DataBufferByte) buffer).getData()).order(ByteOrder.BIG_ENDIAN).asIntBuffer();
        } else {
            int width = image.getWidth();
            int height = image.getHeight();
            int[] pixels = new int[width * height];
            image.getRGB(0, 0, width, height, pixels, 0, width);
            return IntBuffer.wrap(pixels);
        }
    }

    private static void setBackgroundColor(BufferedImage image, BufferedImage scaledImage) {
        int width = image.getWidth();
        int height = image.getHeight();
        int scale = width / scaledImage.getWidth();
        IntBuffer buffer = getARGBAsIntBuffer(image);
        IntBuffer scaledBuffer = getARGBAsIntBuffer(scaledImage);
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                int k = width * j + i;
                int pixel = buffer.get(k);
                if ((pixel & 0xff000000) == 0) {
                    pixel = scaledBuffer.get((j / scale) * (width / scale) + i / scale);
                    buffer.put(k, pixel & 0x00ffffff);
                }
            }
        }
    }

    private static void resetOnOffTransparency(BufferedImage image) {
        IntBuffer rgb = getARGBAsIntBuffer(image);
        for (int i = 0; i < rgb.limit(); i++) {
            int pixel = rgb.get(i);
            if (pixel >>> 24 < 0x7f) {
                rgb.put(i, pixel & 0x00ffffff);
            } else {
                rgb.put(i, pixel | 0xff000000);
            }
        }
    }

    static void scaleHalf(IntBuffer in, int w, int h, IntBuffer out, int rotate) {
        for (int i = 0; i < w / 2; i++) {
            for (int j = 0; j < h / 2; j++) {
                int k = w * 2 * j + 2 * i;
                int pixel00 = in.get(k);
                int pixel01 = in.get(k + 1);
                int pixel10 = in.get(k + w);
                int pixel11 = in.get(k + w + 1);
                if (rotate != 0) {
                    pixel00 = Integer.rotateLeft(pixel00, rotate);
                    pixel01 = Integer.rotateLeft(pixel01, rotate);
                    pixel10 = Integer.rotateLeft(pixel10, rotate);
                    pixel11 = Integer.rotateLeft(pixel11, rotate);
                }
                int pixel = average4RGBA(pixel00, pixel01, pixel10, pixel11);
                if (rotate != 0) {
                    pixel = Integer.rotateRight(pixel, rotate);
                }
                out.put(w / 2 * j + i, pixel);
            }
        }
    }

    private static BufferedImage scaleHalf(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage scaledImage = getPooledImage(width / 2, height / 2, 0);
        scaleHalf(getARGBAsIntBuffer(image), width, height, getARGBAsIntBuffer(scaledImage), 8);
        return scaledImage;
    }

    private static int average4RGBA(int pixel00, int pixel01, int pixel10, int pixel11) {
        int a00 = pixel00 & 0xff;
        int a01 = pixel01 & 0xff;
        int a10 = pixel10 & 0xff;
        int a11 = pixel11 & 0xff;
        switch ((a00 << 24) | (a01 << 16) | (a10 << 8) | a11) {
            case 0xff000000:
                return pixel00;

            case 0x00ff0000:
                return pixel01;

            case 0x0000ff00:
                return pixel10;

            case 0x000000ff:
                return pixel11;

            case 0xffff0000:
                return average2RGBA(pixel00, pixel01);

            case 0xff00ff00:
                return average2RGBA(pixel00, pixel10);

            case 0xff0000ff:
                return average2RGBA(pixel00, pixel11);

            case 0x00ffff00:
                return average2RGBA(pixel01, pixel10);

            case 0x00ff00ff:
                return average2RGBA(pixel01, pixel11);

            case 0x0000ffff:
                return average2RGBA(pixel10, pixel11);

            case 0x00000000:
            case 0xffffffff:
                return average2RGBA(average2RGBA(pixel00, pixel11), average2RGBA(pixel01, pixel10));

            default:
                int a = a00 + a01 + a10 + a11;
                int pixel = a >> 2;
                for (int i = 8; i < 32; i += 8) {
                    int average = (a00 * ((pixel00 >> i) & 0xff) + a01 * ((pixel01 >> i) & 0xff) +
                        a10 * ((pixel10 >> i) & 0xff) + a11 * ((pixel11 >> i) & 0xff)) / a;
                    pixel |= (average << i);
                }
                return pixel;
        }
    }

    private static int average2RGBA(int a, int b) {
        return (((a & 0xfefefefe) >>> 1) + ((b & 0xfefefefe) >>> 1)) | (a & b & 0x01010101);
    }

    private static void checkGLError(String format, Object... params) {
        int error = GL11.glGetError();
        if (error != 0) {
            String message = GLU.gluErrorString(error) + ": " + String.format(format, params);
            new RuntimeException(message).printStackTrace();
        }
    }
}
