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
                    List<ResourcePack> packs = entry.getValue().resourcePacks;
                    if (packs != null) {
                        list.removeAll(packs);
                        list.addAll(packs);
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

    public static boolean hasCustomResource(ResourceLocation resource) {
        InputStream jar = null;
        InputStream pack = null;
        try {
            String path = "assets/" + resource.getNamespace() + "/" + resource.getPath();
            pack = getInputStream(resource);
            if (pack == null) {
                return false;
            }
            jar = Minecraft.class.getResourceAsStream(path);
            if (jar == null) {
                return true;
            }
            byte[] buffer1 = new byte[4096];
            byte[] buffer2 = new byte[4096];
            int read1;
            int read2;
            while ((read1 = pack.read(buffer1)) > 0) {
                read2 = jar.read(buffer2);
                if (read1 != read2) {
                    return true;
                }
                for (int i = 0; i < read1; i++) {
                    if (buffer1[i] != buffer2[i]) {
                        return true;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            MCPatcherUtils.close(jar);
            MCPatcherUtils.close(pack);
        }
        return false;
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
        if (MCPatcherUtils.isNullOrEmpty(directory)) {
            directory = "";
        } else if (!directory.endsWith("/")) {
            directory += '/';
        }

        List<ResourceLocation> resources = new ArrayList<ResourceLocation>();
        Set<ResourceLocation> allFiles = new HashSet<ResourceLocation>();
        Set<ResourceLocation> allDirectories = new HashSet<ResourceLocation>();
        listAllResources(allFiles, allDirectories);

        boolean allNamespaces = MCPatcherUtils.isNullOrEmpty(namespace);
        for (ResourceLocation resource : directories ? allDirectories : allFiles) {
            if (!allNamespaces && !namespace.equals(resource.getNamespace())) {
                continue;
            }
            String path = resource.getPath();
            if (!path.endsWith(suffix)) {
                continue;
            }
            if (!path.startsWith(directory)) {
                continue;
            }
            if (!recursive) {
                String subpath = path.substring(directory.length());
                if (subpath.contains("/")) {
                    continue;
                }
            }
            resources.add(resource);
        }

        final String suffixExpr = Pattern.quote(suffix) + "$";
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
                    f1 = f1.replaceAll(".*/", "").replaceFirst(suffixExpr, "");
                    f2 = f2.replaceAll(".*/", "").replaceFirst(suffixExpr, "");
                }
                return f1.compareTo(f2);
            }
        });
        return resources;
    }

    private static void listAllResources(Set<ResourceLocation> files, Set<ResourceLocation> directories) {
        for (ResourcePack resourcePack : getResourcePacks(null)) {
            if (resourcePack instanceof FileResourcePack) {
                ZipFile zipFile = ((FileResourcePack) resourcePack).zipFile;
                listAllResources(zipFile, files, directories);
            } else if (resourcePack instanceof DefaultResourcePack) {
                Map<String, File> map = ((DefaultResourcePack) resourcePack).map;
                listAllResources(map, files, directories);
            } else if (resourcePack instanceof AbstractResourcePack) {
                File base = ((AbstractResourcePack) resourcePack).file;
                listAllResources(base, files, directories);
            }
        }
    }

    private static void listAllResources(ZipFile zipFile, Set<ResourceLocation> files, Set<ResourceLocation> directories) {
        if (zipFile == null) {
            return;
        }
        for (ZipEntry entry : Collections.list(zipFile.entries())) {
            String path = entry.getName();
            if (!path.startsWith("assets/")) {
                continue;
            }
            path = path.substring(7);
            int slash = path.indexOf('/');
            if (slash < 0) {
                continue;
            }
            String namespace = path.substring(0, slash);
            path = path.substring(slash + 1);
            ResourceLocation resource = new ResourceLocation(namespace, path);
            if (entry.isDirectory()) {
                directories.add(resource);
            } else {
                files.add(resource);
            }
        }
    }

    private static void listAllResources(File directory, Set<ResourceLocation> files, Set<ResourceLocation> directories) {
        if (directory == null || !directory.isDirectory()) {
            return;
        }
        directory = new File(directory, "assets");
        if (!directory.isDirectory()) {
            return;
        }
        File[] subdirs = directory.listFiles();
        if (subdirs == null) {
            return;
        }
        for (File subdir : subdirs) {
            Set<String> allFiles = new HashSet<String>();
            listAllFiles(subdir, "", allFiles);
            for (String path : allFiles) {
                File file = new File(subdir, path);
                ResourceLocation resource = new ResourceLocation(subdir.getName(), path.replace(File.separatorChar, '/'));
                if (file.isDirectory()) {
                    directories.add(resource);
                } else if (file.isFile()) {
                    files.add(resource);
                }
            }
        }
    }

    private static void listAllFiles(File base, String subdir, Set<String> files) {
        File[] entries = new File(base, subdir).listFiles();
        if (entries == null) {
            return;
        }
        for (File file : entries) {
            String newPath = subdir + file.getName();
            if (files.add(newPath)) {
                if (file.isDirectory()) {
                    listAllFiles(base, subdir + file.getName() + '/', files);
                }
            }
        }
    }

    private static void listAllResources(Map<String, File> map, Set<ResourceLocation> files, Set<ResourceLocation> directories) {
        if (map == null) {
            return;
        }
        for (Map.Entry<String, File> entry : map.entrySet()) {
            String key = entry.getKey();
            File file = entry.getValue();
            ResourceLocation resource = new ResourceLocation(key);
            if (file.isDirectory()) {
                directories.add(resource);
            } else if (file.isFile()) {
                files.add(resource);
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
