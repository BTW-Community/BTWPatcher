package com.prupe.mcpatcher;

import net.minecraft.client.Minecraft;
import net.minecraft.src.*;
import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class TexturePackAPI {
    private static final MCLogger logger = MCLogger.getLogger("Texture Pack");

    public static final String DEFAULT_NAMESPACE = "minecraft";
    public static final String MCPATCHER_SUBDIR = "mcpatcher/";

    public static TexturePackAPI instance = new TexturePackAPI();

    public static List<ResourcePack> getResourcePacks(String namespace) {
        List<ResourcePack> list = new ArrayList<ResourcePack>();
        ResourceManager resourceManager = getResourceManager();
        if (resourceManager instanceof SimpleReloadableResourceManager) {
            for (Map.Entry<String, FallbackResourceManager> entry : ((SimpleReloadableResourceManager) resourceManager).namespaceMap.entrySet()) {
                if (namespace == null || namespace.equals(entry.getKey())) {
                    FallbackResourceManager resourceManager1 = entry.getValue();
                    if (resourceManager1.resourcePacks != null) {
                        list.addAll(resourceManager1.resourcePacks);
                    }
                }
            }
        }
        Collections.reverse(list);
        return list;
    }

    public static Set<String> getNamespaces() {
        Set<String> set = new HashSet<String>();
        ResourceManager resourceManager = getResourceManager();
        if (resourceManager instanceof SimpleReloadableResourceManager) {
            set.addAll(((SimpleReloadableResourceManager) resourceManager).namespaceMap.keySet());
        }
        return set;
    }

    public static ResourceManager getResourceManager() {
        return Minecraft.getInstance().getResourceManager();
    }

    public static boolean isDefaultTexturePack() {
        return getResourcePacks(DEFAULT_NAMESPACE).size() <= 1;
    }

    public static InputStream getInputStream(ResourceLocation resource) {
        return resource == null ? null : instance.getInputStreamImpl(resource);
    }

    public static boolean hasResource(ResourceLocation resource) {
        if (resource == null) {
            return false;
        } else if (resource.getPath().endsWith(".png")) {
            return getImage(resource) != null;
        } else if (resource.getPath().endsWith(".properties")) {
            return getProperties(resource) != null;
        } else {
            InputStream is = getInputStream(resource);
            MCPatcherUtils.close(is);
            return is != null;
        }
    }

    public static BufferedImage getImage(ResourceLocation resource) {
        return resource == null ? null : instance.getImageImpl(resource);
    }

    public static Properties getProperties(ResourceLocation resource) {
        Properties properties = new Properties();
        if (getProperties(resource, properties)) {
            return properties;
        } else {
            return null;
        }
    }

    public static boolean getProperties(ResourceLocation resource, Properties properties) {
        return resource != null && instance.getPropertiesImpl(resource, properties);
    }

    public static ResourceLocation transformResourceLocation(ResourceLocation resource, String oldExt, String newExt) {
        return new ResourceLocation(resource.getNamespace(), resource.getPath().replaceFirst(Pattern.quote(oldExt) + "$", newExt));
    }

    public static ResourceLocation parseResourceLocation(ResourceLocation baseResource, String path) {
        if (path == null || path.equals("")) {
            return null;
        }
        boolean absolute = false;
        if (path.startsWith("%blur%")) {
            path = path.substring(6);
        }
        if (path.startsWith("%clamp%")) {
            path = path.substring(7);
        }
        if (path.startsWith("/")) {
            path = path.substring(1);
            absolute = true;
        }
        if (path.startsWith("assets/minecraft/")) {
            path = path.substring(17);
            absolute = true;
        }
        // Absolute path, including namespace:
        // namespace:path/filename -> assets/namespace/path/filename
        int colon = path.indexOf(':');
        if (colon >= 0) {
            return new ResourceLocation(path.substring(0, colon), path.substring(colon + 1));
        }
        // Relative to namespace mcpatcher dir:
        // ~/path -> assets/(namespace of base file)/mcpatcher/path
        if (path.startsWith("~/")) {
            return new ResourceLocation(baseResource.getNamespace(), MCPATCHER_SUBDIR + path.substring(2));
        }
        // Relative to properties file:
        // ./path -> (dir of base file)/path
        if (path.startsWith("./")) {
            return new ResourceLocation(baseResource.getNamespace(), baseResource.getPath().replaceFirst("[^/]+$", "") + path.substring(2));
        }
        // Relative to properties file:
        // filename -> (dir of base file)/filename
        if (!absolute && !path.contains("/")) {
            return new ResourceLocation(baseResource.getNamespace(), baseResource.getPath().replaceFirst("[^/]+$", "") + path);
        }
        // Absolute path, w/o namespace:
        // path/filename -> assets/(namespace of base file)/path/filename
        return new ResourceLocation(baseResource.getNamespace(), path);
    }

    public static ResourceLocation newMCPatcherResourceLocation(String path) {
        return new ResourceLocation(MCPATCHER_SUBDIR + path);
    }

    public static List<ResourceLocation> listResources(String directory, String suffix, boolean recursive, boolean directories, boolean sortByFilename) {
        return listResources(null, directory, suffix, recursive, directories, sortByFilename);
    }

    public static List<ResourceLocation> listResources(String namespace, String directory, String suffix, boolean recursive, boolean directories, final boolean sortByFilename) {
        if (suffix == null) {
            suffix = "";
        }
        List<ResourceLocation> resources = new ArrayList<ResourceLocation>();
        if (MCPatcherUtils.isNullOrEmpty(namespace)) {
            for (String namespace1 : getNamespaces()) {
                findResources(namespace1, directory, suffix, recursive, directories, resources);
            }
        } else {
            findResources(namespace, directory, suffix, recursive, directories, resources);
        }
        Collections.sort(resources, new Comparator<ResourceLocation>() {
            @Override
            public int compare(ResourceLocation o1, ResourceLocation o2) {
                String n1 = o1.getNamespace();
                String n2 = o2.getNamespace();
                int result = n1.compareTo(n2);
                if (result != 0) {
                    return result;
                }
                String f1 = o1.getPath();
                String f2 = o2.getPath();
                if (sortByFilename) {
                    f1 = f1.replaceAll(".*/", "").replaceFirst("\\.properties", "");
                    f2 = f2.replaceAll(".*/", "").replaceFirst("\\.properties", "");
                }
                return f1.compareTo(f2);
            }
        });
        return resources;
    }

    private static void findResources(String namespace, String directory, String suffix, boolean recursive, boolean directories, Collection<ResourceLocation> resources) {
        for (ResourcePack resourcePack : getResourcePacks(namespace)) {
            if (resourcePack instanceof FileResourcePack) {
                ZipFile zipFile = ((FileResourcePack) resourcePack).zipFile;
                if (zipFile != null) {
                    findResources(zipFile, namespace, "assets/" + namespace, directory, suffix, recursive, directories, resources);
                }
            } else if (resourcePack instanceof DefaultResourcePack) {
                if (!DEFAULT_NAMESPACE.equals(namespace)) {
                    continue;
                }
                File base = ((DefaultResourcePack) resourcePack).file;
                if (base != null && base.isDirectory()) {
                    findResources(base, namespace, directory, suffix, recursive, directories, resources);
                }
            } else if (resourcePack instanceof AbstractResourcePack) {
                File base = ((AbstractResourcePack) resourcePack).file;
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

    private static void findResources(ZipFile zipFile, String namespace, String root, String directory, String suffix, boolean recursive, boolean directories, Collection<ResourceLocation> resources) {
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
                    resources.add(new ResourceLocation(namespace, name));
                }
            } else {
                String subpath = name.substring(base.length());
                if (subpath.equals("") || subpath.startsWith("/")) {
                    if (recursive || subpath.equals("") || !subpath.substring(1).contains("/")) {
                        resources.add(new ResourceLocation(namespace, name.substring(root.length() + 1)));
                    }
                }
            }
        }
    }

    private static void findResources(File base, String namespace, String directory, String suffix, boolean recursive, boolean directories, Collection<ResourceLocation> resources) {
        File subdirectory = new File(base, directory);
        String[] list = subdirectory.list();
        if (list != null) {
            String pathComponent = directory.equals("") ? "" : directory + "/";
            for (String s : list) {
                File entry = new File(subdirectory, s);
                String resourceName = pathComponent + s;
                if (entry.isDirectory()) {
                    if (directories && s.endsWith(suffix)) {
                        resources.add(new ResourceLocation(namespace, resourceName));
                    }
                    if (recursive) {
                        findResources(base, namespace, pathComponent + s, suffix, recursive, directories, resources);
                    }
                } else if (s.endsWith(suffix) && !directories) {
                    resources.add(new ResourceLocation(namespace, resourceName));
                }
            }
        }
    }

    public static int getTextureIfLoaded(ResourceLocation resource) {
        if (resource == null) {
            return -1;
        }
        TextureObject texture = Minecraft.getInstance().getTextureManager().getTexture(resource);
        return texture instanceof AbstractTexture ? ((AbstractTexture) texture).glTextureId : -1;
    }

    public static boolean isTextureLoaded(ResourceLocation resource) {
        return getTextureIfLoaded(resource) >= 0;
    }

    public static TextureObject getTextureObject(ResourceLocation resource) {
        return Minecraft.getInstance().getTextureManager().getTexture(resource);
    }

    public static void bindTexture(ResourceLocation resource) {
        Minecraft.getInstance().getTextureManager().bindTexture(resource);
    }

    public static void bindTexture(int texture) {
        if (texture >= 0) {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        }
    }

    public static void unloadTexture(ResourceLocation resource) {
        TextureManager textureManager = Minecraft.getInstance().getTextureManager();
        TextureObject texture = textureManager.getTexture(resource);
        if (texture != null && !(texture instanceof TextureAtlas) && !(texture instanceof DynamicTexture)) {
            if (texture instanceof AbstractTexture) {
                ((AbstractTexture) texture).unloadGLTexture();
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

    protected InputStream getInputStreamImpl(ResourceLocation resource) {
        try {
            return Minecraft.getInstance().getResourceManager().getResource(resource).getInputStream();
        } catch (IOException e) {
            return null;
        }
    }

    protected BufferedImage getImageImpl(ResourceLocation resource) {
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

    protected boolean getPropertiesImpl(ResourceLocation resource, Properties properties) {
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
