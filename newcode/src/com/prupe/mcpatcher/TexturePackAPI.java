package com.prupe.mcpatcher;

import net.minecraft.client.Minecraft;
import net.minecraft.src.*;
import org.lwjgl.opengl.GL11;

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

    private static Field textureIDField;

    static {
        try {
            for (Field field : TexturePlain.class.getDeclaredFields()) {
                if (field.getType() == Integer.TYPE) {
                    field.setAccessible(true);
                    textureIDField = field;
                    break;
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

    public static String[] listResources(String directory, String suffix) {
        List<String> resources = listResources(directory, suffix, false, false, false);
        return resources.toArray(new String[resources.size()]);
    }

    public static String[] listDirectories(String directory) {
        List<String> resources = listResources(directory, "", false, true, false);
        return resources.toArray(new String[resources.size()]);
    }

    public static List<String> listResources(String directory, String suffix, boolean recursive, boolean directories, boolean sortByFilename) {
        directory = fixupPath(directory);
        if (suffix == null) {
            suffix = "";
        }
        List<String> resources = new ArrayList<String>();
        findResources(directory, suffix, recursive, directories, resources);
        if (sortByFilename) {
            Collections.sort(resources, new Comparator<String>() {
                public int compare(String o1, String o2) {
                    String f1 = o1.replaceAll(".*/", "").replaceFirst("\\.properties", "");
                    String f2 = o2.replaceAll(".*/", "").replaceFirst("\\.properties", "");
                    int result = f1.compareTo(f2);
                    if (result != 0) {
                        return result;
                    }
                    return o1.compareTo(o2);
                }
            });
        } else {
            Collections.sort(resources);
        }
        return resources;
    }

    private static void findResources(String directory, String suffix, boolean recursive, boolean directories, Collection<String> resources) {
        ITexturePack texturePack = getTexturePack();
        if (texturePack instanceof TexturePackCustom) {
            ZipFile zipFile = ((TexturePackCustom) texturePack).zipFile;
            if (zipFile != null) {
                for (ZipEntry entry : Collections.list(zipFile.entries())) {
                    if (entry.isDirectory() != directories) {
                        continue;
                    }
                    String name = fixupPath(entry.getName());
                    if (!name.startsWith(directory) || !name.endsWith(suffix)) {
                        continue;
                    }
                    if (directory.equals("")) {
                        if (recursive || !name.contains("/")) {
                            resources.add("/" + name);
                        }
                    } else {
                        String subpath = name.substring(directory.length());
                        if (subpath.equals("") || subpath.startsWith("/")) {
                            if (recursive || subpath.equals("") || !subpath.substring(1).contains("/")) {
                                resources.add("/" + name);
                            }
                        }
                    }
                }
            }
        } else if (texturePack instanceof TexturePackFolder) {
            File base = ((TexturePackFolder) texturePack).texturePackFile;
            if (base != null && base.isDirectory()) {
                findResources(base, directory, suffix, recursive, directories, resources);
            }
        }
    }

    private static void findResources(File base, String directory, String suffix, boolean recursive, boolean directories, Collection<String> resources) {
        File subdirectory = new File(base, directory);
        String[] list = subdirectory.list();
        if (list != null) {
            String pathComponent = directory.equals("") ? "" : directory + "/";
            for (String s : list) {
                File entry = new File(subdirectory, s);
                String resourceName = "/" + pathComponent + s;
                if (entry.isDirectory()) {
                    if (directories && s.endsWith(suffix)) {
                        resources.add(resourceName);
                    }
                    if (recursive) {
                        findResources(base, pathComponent + s, suffix, recursive, directories, resources);
                    }
                } else if (s.endsWith(suffix) && !directories) {
                    resources.add(resourceName);
                }
            }
        }
    }

    public static int getTextureIfLoaded(String s) {
        ILoadableTexture texture = MCPatcherUtils.getMinecraft().getTextureManager().getTexture(s);
        if (texture instanceof TexturePlain && textureIDField != null) {
            try {
                return (Integer) textureIDField.get(texture);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                textureIDField = null;
            }
        }
        return -1;
    }

    public static boolean isTextureLoaded(String s) {
        return getTextureIfLoaded(s) >= 0;
    }

    public static void bindTexture(String s) {
        MCPatcherUtils.getMinecraft().getTextureManager().bindTexture(s);
    }

    public static void bindTexture(int texture) {
        if (texture >= 0) {
            TextureUtils.bindTexture(texture);
        }
    }

    public static int unloadTexture(String s) {
        int id = -1;
        ILoadableTexture texture = MCPatcherUtils.getMinecraft().getTextureManager().getTexture(s);
        if (texture instanceof TexturePlain && textureIDField != null) {
            try {
                id = (Integer) textureIDField.get(texture);
                deleteTexture(id);
                textureIDField.set(texture, -1);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                textureIDField = null;
            }
        }
        return id;
    }

    public static void deleteTexture(int texture) {
        if (texture >= 0) {
            GL11.glDeleteTextures(texture);
        }
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
