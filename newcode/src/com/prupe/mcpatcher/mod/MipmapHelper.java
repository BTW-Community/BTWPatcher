package com.prupe.mcpatcher.mod;

import com.prupe.mcpatcher.Config;
import com.prupe.mcpatcher.MCLogger;
import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.TexturePackAPI;
import net.minecraft.src.RenderEngine;
import net.minecraft.src.Texture;
import net.minecraft.src.TextureUtils;
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

    private static final int TEX_FORMAT = GL12.GL_BGRA;
    private static final int TEX_DATA_TYPE = GL12.GL_UNSIGNED_INT_8_8_8_8_REV;

    private static final int MIN_ALPHA = 0x1a;
    private static final int MAX_ALPHA = 0xe5;

    private static final boolean mipmapSupported;
    static final boolean mipmapEnabled = Config.getBoolean(MCPatcherUtils.EXTENDED_HD, "mipmap", false);
    static final int maxMipmapLevel = Config.getInt(MCPatcherUtils.EXTENDED_HD, "maxMipmapLevel", 3);
    private static final boolean useMipmap;
    private static final int mipmapAlignment = (1 << Config.getInt(MCPatcherUtils.EXTENDED_HD, "mipmapAlignment", 3)) - 1;
    private static final int byteBufferAllocation = Config.getInt(MCPatcherUtils.EXTENDED_HD, "byteBufferAllocation", 1);

    private static final boolean anisoSupported;
    static final int anisoLevel;
    private static final int anisoMax;

    private static final boolean lodSupported;
    private static int lodBias;

    private static final Map<String, Reference<BufferedImage>> imagePool = new HashMap<String, Reference<BufferedImage>>();
    private static final Map<Integer, Reference<ByteBuffer>> bufferPool = new HashMap<Integer, Reference<ByteBuffer>>();

    private static int bgColorFix = 4;

    public static int currentLevel;

    private static final Map<String, Boolean> mipmapType = new HashMap<String, Boolean>();

    private static Texture currentTexture;
    private static boolean enableTransparencyFix = true;
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

    private static void setupTexture(int width, int height, boolean blur, boolean clamp, String textureName) {
        int mipmaps = useMipmapsForTexture(textureName) ? getMipmapLevels(width, height, 1) : 0;
        logger.fine("setupTexture(%s) %dx%d %d mipmaps", textureName, width, height, mipmaps);
        int magFilter = blur ? GL11.GL_LINEAR : GL11.GL_NEAREST;
        int minFilter = mipmaps > 0 ? GL11.GL_NEAREST_MIPMAP_LINEAR : magFilter;
        int wrap = clamp ? GL11.GL_CLAMP : GL11.GL_REPEAT;
        if (mipmaps > 0) {
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, mipmaps);
            checkGLError("%s: set GL_TEXTURE_MAX_LEVEL = %d", textureName, mipmaps);
            if (anisoSupported && anisoLevel > 1) {
                GL11.glTexParameterf(GL11.GL_TEXTURE_2D, EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT, anisoLevel);
                checkGLError("%s: set GL_TEXTURE_MAX_ANISOTROPY_EXT = %f", textureName, anisoLevel);
            }
            if (lodSupported) {
                GL11.glTexEnvi(EXTTextureLODBias.GL_TEXTURE_FILTER_CONTROL_EXT, EXTTextureLODBias.GL_TEXTURE_LOD_BIAS_EXT, lodBias);
                checkGLError("%s: set GL_TEXTURE_LOD_BIAS_EXT = %d", textureName, lodBias);
            }
        }
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, minFilter);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, magFilter);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, wrap);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, wrap);
        for (int level = 0; level <= mipmaps; level++) {
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, level, GL11.GL_RGBA, width, height, 0, TEX_FORMAT, TEX_DATA_TYPE, (IntBuffer) null);
            checkGLError("%s: glTexImage2D %dx%d level %d", textureName, width, height, level);
            width >>= 1;
            height >>= 1;
        }
    }

    public static void setupTexture(int[] rgb, int width, int height, int x, int y, boolean blur, boolean clamp, String textureName) {
        setupTexture(width, height, blur, clamp, textureName);
        copySubTexture(rgb, width, height, x, y, textureName);
    }

    public static int setupTexture(int glTexture, BufferedImage image, boolean blur, boolean clamp, String textureName) {
        int width = image.getWidth();
        int height = image.getHeight();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, glTexture);
        logger.fine("setupTexture(%s, %d, %dx%d, %s, %s)", textureName, glTexture, width, height, blur, clamp);
        int[] rgb = new int[width * height];
        image.getRGB(0, 0, width, height, rgb, 0, width);
        setupTexture(rgb, width, height, 0, 0, blur, clamp, textureName);
        return glTexture;
    }

    public static void copySubTexture(int[] rgb, int width, int height, int x, int y, String textureName) {
        IntBuffer buffer = getPooledBuffer(width * height * 4).asIntBuffer();
        buffer.put(rgb).position(0);
        int mipmaps = getMipmapLevels();
        IntBuffer newBuffer;
        logger.finest("copySubTexture %s %d,%d %dx%d %d mipmaps", textureName, x, y, width, height, mipmaps);
        for (int level = 0; ; ) {
            GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, level, x, y, width, height, TEX_FORMAT, TEX_DATA_TYPE, buffer);
            checkGLError("%s: glTexSubImage2D(%d, %d, %d, %d, %d)", textureName, level, x, y, width, height);
            if (level >= mipmaps) {
                break;
            }
            newBuffer = getPooledBuffer(width * height).asIntBuffer();
            scaleHalf(buffer, width, height, newBuffer, 0);
            buffer = newBuffer;
            level++;
            x >>= 1;
            y >>= 1;
            width >>= 1;
            height >>= 1;
        }
    }

    // TODO: 1.5 stuff

    public static void setupTexture(int target, int level, int internalFormat, int width, int height, int border, int format, int dataType, ByteBuffer buffer, Texture texture) {
        if (!useMipmapsForTexture(texture.getTextureName())) {
            GL11.glTexImage2D(target, level, internalFormat, width, height, border, format, dataType, getDirectByteBuffer(buffer, true));
            return;
        }
        int[] byteOrder = (format == GL11.GL_RGBA ? new int[]{3, 0, 1, 2} : new int[]{3, 2, 1, 0});
        BufferedImage image = getPooledImage(width, height, 0);
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

        try {
            currentTexture = texture;
            enableTransparencyFix = false;
            setupTexture(MCPatcherUtils.getMinecraft().renderEngine, image, texture.getGlTextureId(), false, false, texture.getTextureName());
        } finally {
            enableTransparencyFix = true;
            currentTexture = null;
        }
    }

    public static ByteBuffer allocateByteBuffer(int capacity) {
        if (byteBufferAllocation == 0) {
            return ByteBuffer.allocateDirect(capacity);
        } else {
            return ByteBuffer.allocate(capacity);
        }
    }

    public static void copySubTexture(Texture dst, Texture src, int x, int y, boolean flipped) {
        ByteBuffer srcBuffer = src.getTextureData();
        srcBuffer.position(0);
        if (byteBufferAllocation == 1 && !srcBuffer.isDirect()) {
            logger.finer("creating %d direct byte buffer for texture %s", srcBuffer.capacity(), src.getTextureName());
            src.textureData = srcBuffer = getDirectByteBuffer(srcBuffer, false);
        }
        TexturePackAPI.bindTexture(dst.getGlTextureId());
        int mipmaps = dst.mipmapActive ? getMipmapLevels() : 0;
        int width = src.getWidth();
        int height = src.getHeight();
        if (flipped && !flippedTextureLogged) {
            flippedTextureLogged = true;
            logger.warning("copySubTexture(%s, %s, %d, %d, %s): flipped texture not yet supported",
                dst.getTextureName(), src.getTextureName(), x, y, flipped
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
                src.getTextureName(), dst.getTextureName(), i, x, y, width, height
            );
            if (i >= mipmaps) {
                break;
            }
            ByteBuffer newBuffer = getPooledBuffer(width * height);
            scaleHalf(srcBuffer.asIntBuffer(), width, height, newBuffer.asIntBuffer(), 0);
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

    private static ArrayList<BufferedImage> getMipmapsForTexture(BufferedImage image, String textureName) {
        ArrayList<BufferedImage> mipmapImages = new ArrayList<BufferedImage>();
        mipmapImages.add(image);
        if (!useMipmapsForTexture(textureName)) {
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
            return mipmapImages;
        }
        logger.fine("generating %d mipmaps for %s", mipmaps, textureName);
        image = convertToARGB(image);
        mipmapImages.set(0, image);
        int scale = 1 << bgColorFix;
        int gcd = gcd(width, height);
        if (enableTransparencyFix && bgColorFix > 0 && gcd % scale == 0 && ((gcd / scale) & (gcd / scale - 1)) == 0) {
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
        for (int i = 0; i < mipmaps; i++) {
            image = scaleHalf(image);
            mipmapImages.add(image);
        }
        return mipmapImages;
    }

    static BufferedImage fixTransparency(String name, BufferedImage image) {
        if (image == null) {
            return image;
        }
        long s1 = System.currentTimeMillis();
        image = convertToARGB(image);
        int width = image.getWidth();
        int height = image.getHeight();
        IntBuffer buffer = getARGBAsIntBuffer(image);
        IntBuffer scaledBuffer = buffer;
        outer:
        while (width % 2 == 0 && height % 2 == 0) {
            for (int i = 0; i < scaledBuffer.limit(); i++) {
                if (scaledBuffer.get(i) >>> 24 == 0) {
                    IntBuffer newBuffer = getPooledBuffer(width * height).asIntBuffer();
                    scaleHalf(scaledBuffer, width, height, newBuffer, 8);
                    scaledBuffer = newBuffer;
                    width >>= 1;
                    height >>= 1;
                    continue outer;
                }
            }
            break;
        }
        long s2 = System.currentTimeMillis();
        if (scaledBuffer != buffer) {
            setBackgroundColor(buffer, image.getWidth(), image.getHeight(), scaledBuffer, image.getWidth() / width);
        }
        long s3 = System.currentTimeMillis();
        logger.finer("bg fix (tile %s): scaling %dms, setbg %dms", name, s2 - s1, s3 - s2);
        return image;
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
                        currentTexture.mipmapActive = true;
                        currentTexture.textureMinFilter = GL11.GL_NEAREST_MIPMAP_LINEAR;
                        currentTexture.textureMagFilter = GL11.GL_NEAREST;
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
                renderEngine.setupTextureExt(image, texture, blurTexture, clampTexture);
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
        mipmapType.put("terrain", true);
        mipmapType.put("items", false);
        Properties properties = TexturePackAPI.getProperties(MIPMAP_PROPERTIES);
        if (properties != null) {
            bgColorFix = MCPatcherUtils.getIntProperty(properties, "bgColorFix", 4);
            for (Map.Entry entry : properties.entrySet()) {
                if (entry.getKey() instanceof String && entry.getValue() instanceof String) {
                    String key = ((String) entry.getKey()).trim();
                    boolean value = Boolean.parseBoolean(((String) entry.getValue()).trim().toLowerCase());
                    if (key.startsWith("/")) {
                        mipmapType.put(key, value);
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

    private static boolean useMipmapsForTexture(String texture) {
        if (!useMipmap || texture == null) {
            return false;
        } else if (mipmapType.containsKey(texture)) {
            return mipmapType.get(texture);
        } else if (texture.startsWith("%") ||
            texture.startsWith("##") ||
            texture.startsWith(MCPatcherUtils.TEXTURE_PACK_PREFIX + "achievement/") ||
            texture.startsWith(MCPatcherUtils.TEXTURE_PACK_PREFIX + "environment/") ||
            texture.startsWith(MCPatcherUtils.TEXTURE_PACK_PREFIX + "font/") ||
            texture.startsWith(MCPatcherUtils.TEXTURE_PACK_PREFIX + "gui/") ||
            texture.startsWith(MCPatcherUtils.TEXTURE_PACK_PREFIX + "misc/") ||
            texture.startsWith(MCPatcherUtils.TEXTURE_PACK_PREFIX + "terrain/") ||
            texture.startsWith(MCPatcherUtils.TEXTURE_PACK_PREFIX + "title/") ||
            texture.contains("item")) {
            return false;
        } else {
            return true;
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
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.position(0);
        return buffer;
    }

    private static ByteBuffer getDirectByteBuffer(ByteBuffer buffer, boolean pooled) {
        if (buffer.isDirect()) {
            return buffer;
        } else {
            ByteBuffer newBuffer = pooled ? getPooledBuffer(buffer.capacity()) : ByteBuffer.allocateDirect(buffer.capacity());
            newBuffer.order(buffer.order());
            newBuffer.put(buffer);
            newBuffer.flip();
            return newBuffer;
        }
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
        setBackgroundColor(buffer, width, height, scaledBuffer, scale);
    }

    private static void setBackgroundColor(IntBuffer buffer, int width, int height, IntBuffer scaledBuffer, int scale) {
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
