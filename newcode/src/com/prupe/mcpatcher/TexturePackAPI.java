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
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class TexturePackAPI {
    private static final MCLogger logger = MCLogger.getLogger("Texture Pack");

    public static final String DEFAULT_NAMESPACE = "minecraft";

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
        return getResourcePacks(DEFAULT_NAMESPACE).size() <= 1;
    }

    public static InputStream getInputStream(ResourceAddress resource) {
        return instance.getInputStreamImpl(resource);
    }

    public static boolean hasResource(ResourceAddress resource) {
        if (resource.getPath().endsWith(".png")) {
            return getImage(resource) != null;
        } else if (resource.getPath().endsWith(".properties")) {
            return getProperties(resource) != null;
        } else {
            InputStream is = getInputStream(resource);
            MCPatcherUtils.close(is);
            return is != null;
        }
    }

    public static BufferedImage getImage(ResourceAddress resource) {
        return instance.getImageImpl(resource);
    }

    public static BufferedImage getImage(Object o, ResourceAddress resource) {
        return getImage(resource);
    }

    public static BufferedImage getImage(Object o1, Object o2, ResourceAddress resource) {
        return getImage(resource);
    }

    public static Properties getProperties(ResourceAddress resource) {
        Properties properties = new Properties();
        if (getProperties(resource, properties)) {
            return properties;
        } else {
            return null;
        }
    }

    public static boolean getProperties(ResourceAddress resource, Properties properties) {
        return instance.getPropertiesImpl(resource, properties);
    }

    public static ResourceAddress parseResourceAddress(String path) {
        if (path == null || path.equals("")) {
            return null;
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
        return new ResourceAddress(path);
    }

    public static List<ResourceAddress> listResources(String directory, String suffix, boolean recursive, boolean directories, boolean sortByFilename) {
        if (suffix == null) {
            suffix = "";
        }
        List<ResourceAddress> resources = new ArrayList<ResourceAddress>();
        findResources(DEFAULT_NAMESPACE, directory, suffix, recursive, directories, resources);
        if (sortByFilename) {
            Collections.sort(resources, new Comparator<ResourceAddress>() {
                public int compare(ResourceAddress o1, ResourceAddress o2) {
                    String f1 = o1.getPath().replaceAll(".*/", "").replaceFirst("\\.properties", "");
                    String f2 = o2.getPath().replaceAll(".*/", "").replaceFirst("\\.properties", "");
                    int result = f1.compareTo(f2);
                    if (result != 0) {
                        return result;
                    }
                    return o1.getPath().compareTo(o2.getPath());
                }
            });
        } else {
            Collections.sort(resources, new Comparator<ResourceAddress>() {
                public int compare(ResourceAddress o1, ResourceAddress o2) {
                    return o1.getPath().compareTo(o2.getPath());
                }
            });
        }
        return resources;
    }

    private static void findResources(String namespace, String directory, String suffix, boolean recursive, boolean directories, Collection<ResourceAddress> resources) {
        for (IResourcePack resourcePack : getResourcePacks(namespace)) {
            logger.info("%s", resourcePack);
            if (resourcePack instanceof ResourcePackZip) {
                ZipFile zipFile = ((ResourcePackZip) resourcePack).zipFile;
                if (zipFile != null) {
                    findResources(zipFile, namespace, "assets/" + namespace, directory, suffix, recursive, directories, resources);
                }
            } else if (resourcePack instanceof ResourcePackDefault) {
                if (!DEFAULT_NAMESPACE.equals(namespace)) {
                    continue;
                }
                File base = ((ResourcePackDefault) resourcePack).file;
                if (base != null && base.isDirectory()) {
                    findResources(base, namespace, directory, suffix, recursive, directories, resources);
                }
            } else if (resourcePack instanceof ResourcePackBase) {
                File base = ((ResourcePackBase) resourcePack).file;
                if (base == null || !base.isDirectory()) {
                    continue;
                }
                base = new File(base, "assets/" + namespace);
                if (base.isDirectory()) {
                    findResources(base, namespace, directory, suffix, recursive, directories, resources);
                }
            }
        }
    }

    private static void findResources(ZipFile zipFile, String namespace, String root, String directory, String suffix, boolean recursive, boolean directories, Collection<ResourceAddress> resources) {
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
                    resources.add(new ResourceAddress(namespace, name));
                }
            } else {
                String subpath = name.substring(directory.length());
                if (subpath.equals("") || subpath.startsWith("/")) {
                    if (recursive || subpath.equals("") || !subpath.substring(1).contains("/")) {
                        resources.add(new ResourceAddress(namespace, name.substring(root.length() + 1)));
                    }
                }
            }
        }
    }

    private static void findResources(File base, String namespace, String directory, String suffix, boolean recursive, boolean directories, Collection<ResourceAddress> resources) {
        File subdirectory = new File(base, directory);
        String[] list = subdirectory.list();
        if (list != null) {
            String pathComponent = directory.equals("") ? "" : directory + "/";
            for (String s : list) {
                File entry = new File(subdirectory, s);
                String resourceName = pathComponent + s;
                if (entry.isDirectory()) {
                    if (directories && s.endsWith(suffix)) {
                        resources.add(new ResourceAddress(namespace, resourceName));
                    }
                    if (recursive) {
                        findResources(base, namespace, pathComponent + s, suffix, recursive, directories, resources);
                    }
                } else if (s.endsWith(suffix) && !directories) {
                    resources.add(new ResourceAddress(namespace, resourceName));
                }
            }
        }
    }

    public static int getTextureIfLoaded(ResourceAddress resource) {
        ITexture texture = MCPatcherUtils.getMinecraft().getTextureManager().getTexture(resource);
        return texture instanceof TextureBase ? ((TextureBase) texture).glTextureId : -1;
    }

    public static boolean isTextureLoaded(ResourceAddress resource) {
        return getTextureIfLoaded(resource) >= 0;
    }

    public static void bindTexture(ResourceAddress resource) {
        MCPatcherUtils.getMinecraft().getTextureManager().bindTexture(resource);
    }

    public static void bindTexture(int texture) {
        if (texture >= 0) {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        }
    }

    public static void unloadTexture(ResourceAddress resource) {
        TextureManager textureManager = MCPatcherUtils.getMinecraft().getTextureManager();
        ITexture texture = textureManager.getTexture(resource);
        if (texture != null && !(texture instanceof TextureMap) && !(texture instanceof TextureWithData)) {
            if (texture instanceof TextureBase) {
                ((TextureBase) texture).unloadGLTexture();
            }
            logger.finer("unloading texture %s", resource);
            textureManager.texturesByName.remove(resource);
        }
    }

    public static void deleteTexture(int texture) {
        if (texture >= 0) {
            GL11.glDeleteTextures(texture);
        }
    }

    protected InputStream getInputStreamImpl(ResourceAddress resource) {
        try {
            return Minecraft.getInstance().getResourceBundle().getResource(resource).getInputStream();
        } catch (IOException e) {
            return null;
        }
    }

    protected BufferedImage getImageImpl(ResourceAddress resource) {
        InputStream input = getInputStream(resource);
        BufferedImage image = null;
        if (input != null) {
            try {
                image = ImageIO.read(input);
            } catch (IOException e) {
                logger.error("could not read %s", resource);
                e.printStackTrace();
            } finally {
                MCPatcherUtils.close(input);
            }
        }
        return image;
    }

    protected boolean getPropertiesImpl(ResourceAddress resource, Properties properties) {
        if (properties != null) {
            InputStream input = getInputStream(resource);
            try {
                if (input != null) {
                    properties.load(input);
                    return true;
                }
            } catch (IOException e) {
                logger.error("could not read %s", resource);
                e.printStackTrace();
            } finally {
                MCPatcherUtils.close(input);
            }
        }
        return false;
    }
}
