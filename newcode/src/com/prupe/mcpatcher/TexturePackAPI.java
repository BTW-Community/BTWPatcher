package com.prupe.mcpatcher;

import net.minecraft.client.Minecraft;
import net.minecraft.src.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class TexturePackAPI {
    private static final MCLogger logger = MCLogger.getLogger("Texture Pack");

    public static TexturePackAPI instance = new TexturePackAPI();
    public static boolean enableTextureBorder;

    private static final ArrayList<Field> textureMapFields = new ArrayList<Field>();

    static {
        try {
            for (Field field : RenderEngine.class.getDeclaredFields()) {
                if (HashMap.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    textureMapFields.add(field);
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static ITexturePack getTexturePack() {
        Minecraft minecraft = MCPatcherUtils.getMinecraft();
        if (minecraft == null) {
            return null;
        }
        TexturePackList texturePackList = minecraft.texturePackList;
        if (texturePackList == null) {
            return null;
        }
        return texturePackList.getSelectedTexturePack();
    }

    public static boolean isDefaultTexturePack() {
        return getTexturePack() instanceof TexturePackDefault;
    }

    public static String[] parseTextureName(String s) {
        String[] result = new String[]{null, s};
        if (s.startsWith("##")) {
            result[0] = "##";
            result[1] = s.substring(2);
        } else if (s.startsWith("%")) {
            int index = s.indexOf('%', 1);
            if (index > 0) {
                result[0] = s.substring(0, index + 1);
                result[1] = s.substring(index + 1);
            }
        }
        return result;
    }

    public static InputStream getInputStream(String s) {
        return instance.getInputStreamImpl(s);
    }

    public static boolean hasResource(String s) {
        if (s.endsWith(".png")) {
            return getImage(s) != null;
        } else if (s.endsWith(".properties")) {
            return getProperties(s) != null;
        } else {
            InputStream is = getInputStream(s);
            MCPatcherUtils.close(is);
            return is != null;
        }
    }

    public static BufferedImage getImage(String s) {
        return instance.getImageImpl(s);
    }

    public static BufferedImage getImage(Object o, String s) {
        return getImage(s);
    }

    public static BufferedImage getImage(Object o1, Object o2, String s) {
        return getImage(s);
    }

    public static Properties getProperties(String s) {
        Properties properties = new Properties();
        if (getProperties(s, properties)) {
            return properties;
        } else {
            return null;
        }
    }

    public static boolean getProperties(String s, Properties properties) {
        return instance.getPropertiesImpl(s, properties);
    }

    private static String fixupPath(String path) {
        if (path == null) {
            path = "";
        }
        return path.replace('\\', '/').replaceFirst("^/", "").replaceFirst("/$", "");
    }

    private static boolean directlyContains(String parent, String child) {
        parent = fixupPath(parent);
        child = fixupPath(child);
        if (!child.startsWith(parent)) {
            return false;
        }
        child = child.substring(parent.length());
        return child.matches("/[^/]+/?");
    }

    public static String[] listResources(String directory, String suffix) {
        directory = fixupPath(directory);
        if (suffix == null) {
            suffix = "";
        }

        ITexturePack texturePack = getTexturePack();
        ArrayList<String> resources = new ArrayList<String>();
        if (texturePack instanceof TexturePackCustom) {
            ZipFile zipFile = ((TexturePackCustom) texturePack).zipFile;
            if (zipFile != null) {
                for (ZipEntry entry : Collections.list(zipFile.entries())) {
                    final String name = entry.getName();
                    if (!entry.isDirectory() && directlyContains(directory, name) && name.endsWith(suffix)) {
                        resources.add("/" + name);
                    }
                }
            }
        } else if (texturePack instanceof TexturePackFolder) {
            File folder = ((TexturePackFolder) texturePack).texturePackFile;
            if (folder != null && folder.isDirectory()) {
                String[] list = new File(folder, directory).list();
                if (list != null) {
                    for (String s : list) {
                        if (s.endsWith(suffix)) {
                            resources.add("/" + directory + "/" + s);
                        }
                    }
                }
            }
        }

        Collections.sort(resources);
        return resources.toArray(new String[resources.size()]);
    }

    public static String[] listDirectories(String directory) {
        directory = fixupPath(directory);
        ITexturePack texturePack = getTexturePack();
        ArrayList<String> resources = new ArrayList<String>();
        if (texturePack instanceof TexturePackCustom) {
            ZipFile zipFile = ((TexturePackCustom) texturePack).zipFile;
            if (zipFile != null) {
                for (ZipEntry entry : Collections.list(zipFile.entries())) {
                    final String name = fixupPath(entry.getName());
                    if (entry.isDirectory() && directlyContains(directory, name)) {
                        resources.add("/" + name);
                    }
                }
            }
        } else if (texturePack instanceof TexturePackFolder) {
            File folder = ((TexturePackFolder) texturePack).texturePackFile;
            if (folder != null && folder.isDirectory()) {
                File subfolder = new File(folder, directory);
                String[] list = subfolder.list();
                if (list != null) {
                    for (String s : list) {
                        if (new File(subfolder, s).isDirectory()) {
                            resources.add("/" + directory + "/" + s);
                        }
                    }
                }
            }
        }

        Collections.sort(resources);
        return resources.toArray(new String[resources.size()]);
    }

    public static int getTextureIfLoaded(String s) {
        RenderEngine renderEngine = MCPatcherUtils.getMinecraft().renderEngine;
        for (Field field : textureMapFields) {
            try {
                HashMap map = (HashMap) field.get(renderEngine);
                if (map != null) {
                    Object value = map.get(s);
                    if (value instanceof Integer) {
                        return (Integer) value;
                    }
                }
            } catch (IllegalAccessException e) {
            }
        }
        return -1;
    }

    public static boolean isTextureLoaded(String s) {
        return getTextureIfLoaded(s) >= 0;
    }

    public static void bindTexture(String s) {
        MCPatcherUtils.getMinecraft().renderEngine.bindTextureByName(s);
    }

    public static void bindTexture(int texture) {
        MCPatcherUtils.getMinecraft().renderEngine.bindTexture(texture);
    }

    public static void clearBoundTexture() {
        MCPatcherUtils.getMinecraft().renderEngine.clearBoundTexture();
    }

    public static int unloadTexture(String s) {
        int texture = getTextureIfLoaded(s);
        if (texture >= 0) {
            logger.finest("unloading texture %s", s);
            RenderEngine renderEngine = MCPatcherUtils.getMinecraft().renderEngine;
            renderEngine.deleteTexture(texture);
            for (Field field : textureMapFields) {
                try {
                    HashMap map = (HashMap) field.get(renderEngine);
                    if (map != null) {
                        map.remove(s);
                    }
                } catch (IllegalAccessException e) {
                }
            }
        }
        return texture;
    }

    public static String getTextureName(int texture) {
        if (texture >= 0) {
            RenderEngine renderEngine = MCPatcherUtils.getMinecraft().renderEngine;
            for (Field field : textureMapFields) {
                try {
                    HashMap map = (HashMap) field.get(renderEngine);
                    for (Object o : map.entrySet()) {
                        Map.Entry entry = (Map.Entry) o;
                        Object value = entry.getValue();
                        Object key = entry.getKey();
                        if (value instanceof Integer && key instanceof String && (Integer) value == texture) {
                            return (String) key;
                        }
                    }
                } catch (IllegalAccessException e) {
                }
            }
        }
        return null;
    }

    public static IntBuffer getIntBuffer(IntBuffer buffer, int[] data) {
        buffer.clear();
        final int have = buffer.capacity();
        final int needed = data.length;
        if (needed > have) {
            logger.finest("resizing gl buffer from 0x%x to 0x%x", have, needed);
            buffer = ByteBuffer.allocateDirect(4 * needed).order(buffer.order()).asIntBuffer();
        }
        buffer.put(data);
        buffer.position(0).limit(needed);
        return buffer;
    }

    protected InputStream getInputStreamImpl(String s) {
        s = parseTextureName(s)[1];
        ITexturePack texturePack = getTexturePack();
        if (texturePack == null) {
            return TexturePackAPI.class.getResourceAsStream(s);
        } else {
            try {
                return texturePack.getResourceAsStream(s);
            } catch (Throwable e) {
                return null;
            }
        }
    }

    protected BufferedImage getImageImpl(String s) {
        InputStream input = getInputStream(s);
        BufferedImage image = null;
        if (input != null) {
            try {
                image = ImageIO.read(input);
            } catch (IOException e) {
                logger.error("could not read %s", s);
                e.printStackTrace();
            } finally {
                MCPatcherUtils.close(input);
            }
        }
        return image;
    }

    protected boolean getPropertiesImpl(String s, Properties properties) {
        if (properties != null) {
            InputStream input = getInputStream(s);
            try {
                if (input != null) {
                    properties.load(input);
                    return true;
                }
            } catch (IOException e) {
                logger.error("could not read %s");
                e.printStackTrace();
            } finally {
                MCPatcherUtils.close(input);
            }
        }
        return false;
    }
}
