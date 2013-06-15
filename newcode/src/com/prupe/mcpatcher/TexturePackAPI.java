package com.prupe.mcpatcher;

import net.minecraft.client.Minecraft;
import net.minecraft.src.*;
import net.minecraft.src.ResourceBundle;
import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class TexturePackAPI {
    private static final MCLogger logger = MCLogger.getLogger("Texture Pack");

    public static TexturePackAPI instance = new TexturePackAPI();

    public static List<IResourcePack> getResourcePacks(String namespace) {
        List<IResourcePack> list = new ArrayList<IResourcePack>();
        IResourceBundle resourceBundle = Minecraft.getInstance().getResourceBundle();
        if (resourceBundle instanceof TextureResourceBundle) {
            for (Map.Entry<String, ResourceBundle> entry : ((TextureResourceBundle) resourceBundle).namespaceMap.entrySet()) {
                if (namespace == null || namespace.equals(entry.getKey())) {
                    ResourceBundle bundle = entry.getValue();
                    list.addAll(bundle.resourcePacks);
                }
            }
        }
        return list;
    }

    public static boolean isDefaultTexturePack() {
        return getResourcePacks("minecraft").size() <= 1;
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

    public static String fixupPath(String path) {
        if (path == null) {
            return "";
        }
        if (path.startsWith("%blur%")) {
            path = path.substring(6);
        }
        if (path.startsWith("%clamp%")) {
            path = path.substring(7);
        }
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        return path;
    }

    public static List<String> listResources(String directory, String suffix, boolean recursive, boolean directories, boolean sortByFilename) {
        if (suffix == null) {
            suffix = "";
        }
        List<String> resources = new ArrayList<String>();
        findResources("minecraft", "assets/minecraft", directory, suffix, recursive, directories, resources);
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

    private static void findResources(String namespace, String root, String directory, String suffix, boolean recursive, boolean directories, Collection<String> resources) {
        for (IResourcePack resourcePack : getResourcePacks(namespace)) {
            if (resourcePack instanceof ResourcePackZip) {
                ZipFile zipFile = ((ResourcePackZip) resourcePack).zipFile;
                if (zipFile != null) {
                    findResources(zipFile, root, directory, suffix, recursive, directories, resources);
                }
            } else if (resourcePack instanceof ResourcePackBase || resourcePack instanceof ResourcePackDefault) {
                File base;
                if (resourcePack instanceof ResourcePackBase) {
                    base = ((ResourcePackBase) resourcePack).file;
                } else {
                    base = ((ResourcePackDefault) resourcePack).file;
                }
                if (base == null || !base.isDirectory()) {
                    continue;
                }
                base = new File(base, root);
                if (base.isDirectory()) {
                    findResources(base, directory, suffix, recursive, directories, resources);
                }
            }
        }
    }

    private static void findResources(ZipFile zipFile, String root, String directory, String suffix, boolean recursive, boolean directories, Collection<String> resources) {
        String base = root + "/" + directory;
        for (ZipEntry entry : Collections.list(zipFile.entries())) {
            if (entry.isDirectory() != directories) {
                continue;
            }
            String name = entry.getName().replaceFirst("^/", "");
            if (!name.startsWith(base) || !name.endsWith(suffix)) {
                continue;
            }
            if (directory.equals("")) {
                if (recursive || !name.contains("/")) {
                    resources.add(name);
                }
            } else {
                String subpath = name.substring(directory.length());
                if (subpath.equals("") || subpath.startsWith("/")) {
                    if (recursive || subpath.equals("") || !subpath.substring(1).contains("/")) {
                        resources.add(name.substring(root.length() + 1));
                    }
                }
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
                String resourceName = pathComponent + s;
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
        ITexture texture = MCPatcherUtils.getMinecraft().getTextureManager().getTexture(new ResourceAddress(s));
        return texture instanceof TextureBase ? ((TextureBase) texture).glTextureId : -1;
    }

    public static boolean isTextureLoaded(String s) {
        return getTextureIfLoaded(s) >= 0;
    }

    public static void bindTexture(String s) {
        MCPatcherUtils.getMinecraft().getTextureManager().bindTexture(new ResourceAddress(s));
    }

    public static void bindTexture(int texture) {
        if (texture >= 0) {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        }
    }

    public static void loadTexture(String s, boolean blur, boolean clamp) {
        MCPatcherUtils.getMinecraft().getTextureManager().addTexture(new ResourceAddress(s), new TextureNamed(s, blur, clamp));
    }

    public static void unloadTexture(String s) {
        TextureManager textureManager = MCPatcherUtils.getMinecraft().getTextureManager();
        ITexture texture = textureManager.getTexture(new ResourceAddress(s));
        if (texture != null && !(texture instanceof TextureMap) && !(texture instanceof TextureWithData)) {
            if (texture instanceof TextureBase) {
                ((TextureBase) texture).unloadGLTexture();
            }
            logger.finer("unloading texture %s", s);
            textureManager.texturesByName.remove(new ResourceAddress(s));
        }
    }

    public static void deleteTexture(int texture) {
        if (texture >= 0) {
            GL11.glDeleteTextures(texture);
        }
    }

    protected InputStream getInputStreamImpl(String s) {
        try {
            return Minecraft.getInstance().getResourceBundle().getResource(new ResourceAddress(s)).getInputStream();
        } catch (IOException e) {
            return null;
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
