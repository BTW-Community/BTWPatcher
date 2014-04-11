package com.prupe.mcpatcher.mal.resource;

import com.prupe.mcpatcher.MCLogger;
import com.prupe.mcpatcher.MCPatcherUtils;
import net.minecraft.src.*;

import java.io.File;
import java.util.*;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ResourceList {
    private static final MCLogger logger = MCLogger.getLogger("Texture Pack");

    private static ResourceList instance;
    private static final Map<ResourcePack, Integer> resourcePackOrder = new WeakHashMap<ResourcePack, Integer>();

    private final ResourcePack resourcePack;
    private final Set<ResourceLocationWithSource> allResources = new TreeSet<ResourceLocationWithSource>(new ResourceLocationWithSource.Comparator1());

    public static ResourceList getInstance() {
        if (instance == null) {
            List<ResourcePack> resourcePacks = TexturePackAPI.getResourcePacks(null);
            int order = resourcePacks.size();
            resourcePackOrder.clear();
            for (ResourcePack resourcePack : resourcePacks) {
                resourcePackOrder.put(resourcePack, order);
                order--;
            }
            instance = new ResourceList();
        }
        return instance;
    }

    public static void clearInstance() {
        instance = null;
    }

    public static int getResourcePackOrder(ResourcePack resourcePack) {
        Integer i = resourcePackOrder.get(resourcePack);
        return i == null ? Integer.MAX_VALUE : i;
    }

    private ResourceList() {
        this.resourcePack = null;
        for (ResourcePack resourcePack : TexturePackAPI.getResourcePacks(null)) {
            ResourceList sublist;
            if (resourcePack instanceof FileResourcePack) {
                sublist = new ResourceList((FileResourcePack) resourcePack);
            } else if (resourcePack instanceof DefaultResourcePack) {
                sublist = new ResourceList((DefaultResourcePack) resourcePack);
            } else if (resourcePack instanceof AbstractResourcePack) {
                sublist = new ResourceList((AbstractResourcePack) resourcePack);
            } else {
                continue;
            }
            allResources.removeAll(sublist.allResources);
            allResources.addAll(sublist.allResources);
        }
        logger.fine("new %s", this);
        if (logger.isLoggable(Level.FINEST)) {
            for (ResourceLocationWithSource resource : allResources) {
                logger.finest("%s -> %s", resource, resource.getSource().getName());
            }
        }
    }

    private ResourceList(FileResourcePack resourcePack) {
        this.resourcePack = resourcePack;
        scanZipFile(resourcePack.zipFile);
        logger.fine("new %s", this);
    }

    private ResourceList(DefaultResourcePack resourcePack) {
        this.resourcePack = resourcePack;
        String version = MCPatcherUtils.getMinecraftVersion();
        File jar = MCPatcherUtils.getMinecraftPath("versions", version, version + ".jar");
        if (jar.isFile()) {
            ZipFile zipFile = null;
            try {
                zipFile = new ZipFile(jar);
                scanZipFile(zipFile);
            } catch (Throwable e) {
                e.printStackTrace();
            } finally {
                MCPatcherUtils.close(zipFile);
            }
        }
        Map<String, File> map = resourcePack.map;
        if (map != null) {
            for (Map.Entry<String, File> entry : map.entrySet()) {
                String key = entry.getKey();
                File file = entry.getValue();
                ResourceLocation resource = new ResourceLocation(key);
                addResource(resource, file.isFile(), file.isDirectory());
            }
        }
        if (!allResources.isEmpty()) {
            logger.fine("new %s", this);
        }
    }

    private ResourceList(AbstractResourcePack resourcePack) {
        this.resourcePack = resourcePack;
        File directory = resourcePack.file;
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
                addResource(resource, file.isFile(), file.isDirectory());
            }
        }
        logger.fine("new %s", this);
    }

    private void scanZipFile(ZipFile zipFile) {
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
            addResource(resource, !entry.isDirectory(), entry.isDirectory());
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

    private void addResource(ResourceLocation resource, boolean isFile, boolean isDirectory) {
        if (isFile) {
            allResources.add(new ResourceLocationWithSource(resourcePack, resource));
        } else if (isDirectory) {
            if (!resource.getPath().endsWith("/")) {
                resource = new ResourceLocation(resource.getNamespace(), resource.getPath() + '/');
            }
            allResources.add(new ResourceLocationWithSource(resourcePack, resource));
        }
    }

    public List<ResourceLocation> listResources(String directory, String suffix, boolean sortByFilename) {
        return listResources(directory, suffix, true, false, sortByFilename);
    }

    public List<ResourceLocation> listResources(String directory, String suffix, boolean recursive, boolean directories, boolean sortByFilename) {
        return listResources(null, directory, suffix, recursive, directories, sortByFilename);
    }

    public List<ResourceLocation> listResources(String namespace, String directory, String suffix, boolean recursive, boolean directories, final boolean sortByFilename) {
        if (suffix == null) {
            suffix = "";
        }
        if (MCPatcherUtils.isNullOrEmpty(directory)) {
            directory = "";
        } else if (!directory.endsWith("/")) {
            directory += '/';
        }

        Set<ResourceLocationWithSource> tmpList = new TreeSet<ResourceLocationWithSource>(
            new ResourceLocationWithSource.Comparator1(true, sortByFilename ? suffix : null)
        );
        boolean allNamespaces = MCPatcherUtils.isNullOrEmpty(namespace);
        for (ResourceLocationWithSource resource : allResources) {
            if (directories != resource.isDirectory()) {
                continue;
            }
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
            tmpList.add(resource);
        }

        return new ArrayList<ResourceLocation>(tmpList);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ResourceList: ");
        if (resourcePack == null) {
            sb.append("(combined) ");
        } else {
            sb.append(resourcePack.getName()).append(' ');
        }
        int fileCount = 0;
        int directoryCount = 0;
        Set<String> namespaces = new HashSet<String>();
        for (ResourceLocationWithSource resource : allResources) {
            if (resource.isDirectory()) {
                directoryCount++;
            } else {
                fileCount++;
            }
            namespaces.add(resource.getNamespace());
        }
        sb.append(fileCount).append(" files, ");
        sb.append(directoryCount).append(" directories in ");
        sb.append(namespaces.size()).append(" namespaces");
        return sb.toString();
    }
}
