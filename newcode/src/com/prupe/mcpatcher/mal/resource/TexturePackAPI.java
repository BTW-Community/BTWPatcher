package com.prupe.mcpatcher.mal.resource;

import com.prupe.mcpatcher.MAL;
import com.prupe.mcpatcher.MCLogger;
import com.prupe.mcpatcher.MCPatcherUtils;
import net.minecraft.src.*;
import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.*;
import java.util.regex.Pattern;

abstract public class TexturePackAPI {
    private static final MCLogger logger = MCLogger.getLogger("Texture Pack");

    public static final String DEFAULT_NAMESPACE = "minecraft";

    private static final TexturePackAPI instance = MAL.newInstance(TexturePackAPI.class, "texturepack");

    public static final String MCPATCHER_SUBDIR = TexturePackAPI.select("/", "mcpatcher/");

    public static boolean isInitialized() {
        return instance != null && instance.isInitialized_Impl();
    }

    public static void scheduleTexturePackRefresh() {
        Minecraft.getInstance().scheduleTexturePackRefresh();
    }

    public static List<ResourcePack> getResourcePacks(String namespace) {
        List<ResourcePack> list = new ArrayList<ResourcePack>();
        instance.getResourcePacks_Impl(namespace, list);
        return list;
    }

    public static Set<String> getNamespaces() {
        Set<String> set = new HashSet<String>();
        set.add(DEFAULT_NAMESPACE);
        instance.getNamespaces_Impl(set);
        return set;
    }

    public static boolean isDefaultTexturePack() {
        return instance.isDefaultResourcePack_Impl();
    }

    public static InputStream getInputStream(ResourceLocation resource) {
        try {
            if (resource instanceof ResourceLocationWithSource) {
                try {
                    return instance.getInputStream_Impl(((ResourceLocationWithSource) resource).getSource(), resource);
                } catch (IOException e) {
                }
            }
            return resource == null ? null : instance.getInputStream_Impl(resource);
        } catch (IOException e) {
            return null;
        }
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
            String path = instance.getFullPath_Impl(resource);
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
        if (resource == null) {
            return null;
        }
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

    public static Properties getProperties(ResourceLocation resource) {
        Properties properties = new Properties();
        if (getProperties(resource, properties)) {
            return properties;
        } else {
            return null;
        }
    }

    public static boolean getProperties(ResourceLocation resource, Properties properties) {
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

    public static ResourceLocation transformResourceLocation(ResourceLocation resource, String oldExt, String newExt) {
        return new ResourceLocation(resource.getNamespace(), resource.getPath().replaceFirst(Pattern.quote(oldExt) + "$", newExt));
    }

    public static ResourceLocation parsePath(String path) {
        return MCPatcherUtils.isNullOrEmpty(path) ? null : instance.parsePath_Impl(path.replace(File.separatorChar, '/'));
    }

    public static ResourceLocation parseResourceLocation(ResourceLocation baseResource, String path) {
        return MCPatcherUtils.isNullOrEmpty(path) ? null : instance.parseResourceLocation_Impl(baseResource, path);
    }

    public static <T> T select(T v1, T v2) {
        return instance.select_Impl(v1, v2);
    }

    public static ResourceLocation newMCPatcherResourceLocation(String v1Path, String v2Path) {
        return new ResourceLocation(MCPATCHER_SUBDIR + select(v1Path, v2Path).replaceFirst("^/+", ""));
    }

    public static ResourceLocation newMCPatcherResourceLocation(String path) {
        return newMCPatcherResourceLocation(path, path);
    }

    public static int getTextureIfLoaded(ResourceLocation resource) {
        return resource == null ? -1 : instance.getTextureIfLoaded_Impl(resource);
    }

    public static boolean isTextureLoaded(ResourceLocation resource) {
        return getTextureIfLoaded(resource) >= 0;
    }

    public static TextureObject getTextureObject(ResourceLocation resource) {
        return Minecraft.getInstance().getTextureManager().getTexture(resource);
    }

    public static void bindTexture(ResourceLocation resource) {
        if (resource != null) {
            instance.bindTexture_Impl(resource);
        }
    }

    public static void bindTexture(int texture) {
        if (texture >= 0) {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        }
    }

    public static void unloadTexture(ResourceLocation resource) {
        if (resource != null) {
            instance.unloadTexture_Impl(resource);
        }
    }

    public static void deleteTexture(int texture) {
        if (texture >= 0) {
            GL11.glDeleteTextures(texture);
        }
    }

    public static void flushUnusedTextures() {
        instance.flushUnusedTextures_Impl();
    }

    abstract protected boolean isInitialized_Impl();

    abstract protected void getResourcePacks_Impl(String namespace, List<ResourcePack> resourcePacks);

    abstract protected void getNamespaces_Impl(Set<String> namespaces);

    abstract protected InputStream getInputStream_Impl(ResourceLocation resource) throws IOException;

    abstract protected InputStream getInputStream_Impl(ResourcePack resourcePack, ResourceLocation resource) throws IOException;

    abstract protected String getFullPath_Impl(ResourceLocation resource);

    abstract protected ResourceLocation parsePath_Impl(String path);

    abstract protected ResourceLocation parseResourceLocation_Impl(ResourceLocation baseResource, String path);

    abstract protected <T> T select_Impl(T v1, T v2);

    abstract protected boolean isDefaultResourcePack_Impl();

    abstract protected int getTextureIfLoaded_Impl(ResourceLocation resource);

    abstract protected void bindTexture_Impl(ResourceLocation resource);

    abstract protected void unloadTexture_Impl(ResourceLocation resource);

    abstract protected void flushUnusedTextures_Impl();

    final private static class V1 extends TexturePackAPI {
        private final List<Field> textureMapFields = new ArrayList<Field>();

        V1() {
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

        private ResourcePack getTexturePack() {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft == null) {
                return null;
            }
            ResourcePackRepository texturePackList = minecraft.texturePackList;
            if (texturePackList == null) {
                return null;
            }
            return texturePackList.getSelectedTexturePack();
        }

        @Override
        protected boolean isInitialized_Impl() {
            return Minecraft.getInstance().texturePackList != null;
        }

        @Override
        protected void getResourcePacks_Impl(String namespace, List<ResourcePack> resourcePacks) {
            ResourcePack resourcePack = getTexturePack();
            if (resourcePack != null) {
                resourcePacks.add(resourcePack);
            }
        }

        @Override
        protected void getNamespaces_Impl(Set<String> namespaces) {
        }

        @Override
        protected InputStream getInputStream_Impl(ResourceLocation resource) throws IOException {
            return getInputStream_Impl(getTexturePack(), resource);
        }

        @Override
        protected InputStream getInputStream_Impl(ResourcePack resourcePack, ResourceLocation resource) throws IOException {
            return resourcePack == null ? null : resourcePack.getInputStream(resource.getPath());
        }

        @Override
        protected String getFullPath_Impl(ResourceLocation resource) {
            return resource.getPath();
        }

        @Override
        protected ResourceLocation parsePath_Impl(String path) {
            return new ResourceLocation(path);
        }

        @Override
        protected ResourceLocation parseResourceLocation_Impl(ResourceLocation baseResource, String path) {
            if (path.startsWith("./")) {
                // Relative to properties file:
                // ./path -> (dir of base file)/path
                return new ResourceLocation(baseResource.getNamespace(), baseResource.getPath().replaceFirst("[^/]+$", "") + path.substring(2));
            } else if (path.startsWith("/")) {
                return new ResourceLocation(path);
            } else {
                return new ResourceLocation(baseResource.getNamespace(), baseResource.getPath().replaceFirst("[^/]+$", "") + path);
            }
        }

        @Override
        protected <T> T select_Impl(T v1, T v2) {
            return v1;
        }

        @Override
        protected boolean isDefaultResourcePack_Impl() {
            ResourcePack texturePack = getTexturePack();
            return texturePack == null || texturePack instanceof DefaultResourcePack;
        }

        @Override
        protected int getTextureIfLoaded_Impl(ResourceLocation resource) {
            RenderEngine renderEngine = Minecraft.getInstance().renderEngine;
            String path = resource.getPath();
            if (path.equals("/terrain.png") || path.equals("/gui/items.png")) {
                return renderEngine.getTexture(path);
            }
            for (Field field : textureMapFields) {
                try {
                    HashMap map = (HashMap) field.get(renderEngine);
                    if (map != null) {
                        Object value = map.get(resource.toString());
                        if (value instanceof Integer) {
                            return (Integer) value;
                        }
                    }
                } catch (IllegalAccessException e) {
                }
            }
            return -1;
        }

        @Override
        protected void bindTexture_Impl(ResourceLocation resource) {
            Minecraft.getInstance().renderEngine.bindTextureByName(resource.toString());
        }

        @Override
        protected void unloadTexture_Impl(ResourceLocation resource) {
            int texture = getTextureIfLoaded(resource);
            if (texture >= 0) {
                logger.finest("unloading texture %s", resource);
                RenderEngine renderEngine = Minecraft.getInstance().renderEngine;
                renderEngine.deleteTexture(texture);
                for (Field field : textureMapFields) {
                    try {
                        HashMap map = (HashMap) field.get(renderEngine);
                        if (map != null) {
                            map.remove(resource.toString());
                        }
                    } catch (IllegalAccessException e) {
                    }
                }
            }
        }

        @Override
        protected void flushUnusedTextures_Impl() {
            // switching packs is so hopelessly broken in 1.5 that there's no point
        }
    }

    final private static class V2 extends TexturePackAPI {
        private static final String ASSETS = "assets/";

        private ResourceManager getResourceManager() {
            return Minecraft.getInstance().getResourceManager();
        }

        @Override
        protected boolean isInitialized_Impl() {
            return getResourceManager() != null;
        }

        @Override
        protected void getResourcePacks_Impl(String namespace, List<ResourcePack> resourcePacks) {
            ResourceManager resourceManager = getResourceManager();
            if (resourceManager instanceof SimpleReloadableResourceManager) {
                for (Map.Entry<String, FallbackResourceManager> entry : ((SimpleReloadableResourceManager) resourceManager).namespaceMap.entrySet()) {
                    if (namespace == null || namespace.equals(entry.getKey())) {
                        List<ResourcePack> packs = entry.getValue().resourcePacks;
                        if (packs != null) {
                            resourcePacks.removeAll(packs);
                            resourcePacks.addAll(packs);
                        }
                    }
                }
            }
        }

        @Override
        protected void getNamespaces_Impl(Set<String> namespaces) {
            ResourceManager resourceManager = getResourceManager();
            if (resourceManager instanceof SimpleReloadableResourceManager) {
                namespaces.addAll(((SimpleReloadableResourceManager) resourceManager).namespaceMap.keySet());
            }
        }

        @Override
        protected InputStream getInputStream_Impl(ResourceLocation resource) throws IOException {
            return Minecraft.getInstance().getResourceManager().getResource(resource).getInputStream();
        }

        @Override
        protected InputStream getInputStream_Impl(ResourcePack resourcePack, ResourceLocation resource) throws IOException {
            return resourcePack.getInputStream(resource);
        }

        @Override
        protected String getFullPath_Impl(ResourceLocation resource) {
            return ASSETS + resource.getNamespace() + "/" + resource.getPath();
        }

        @Override
        protected ResourceLocation parsePath_Impl(String path) {
            if (path.startsWith(ASSETS)) {
                path = path.substring(ASSETS.length());
                int slash = path.indexOf('/');
                if (slash > 0 && slash + 1 < path.length()) {
                    return new ResourceLocation(path.substring(0, slash), path.substring(slash + 1));
                }
            }
            return null;
        }

        @Override
        protected ResourceLocation parseResourceLocation_Impl(ResourceLocation baseResource, String path) {
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
            ResourceLocation resource;
            if (path.startsWith("~/")) {
                // Relative to namespace mcpatcher dir:
                // ~/path -> assets/(namespace of base file)/mcpatcher/path
                resource = new ResourceLocation(baseResource.getNamespace(), MCPATCHER_SUBDIR + path.substring(2));
            } else if (path.startsWith("./")) {
                // Relative to properties file:
                // ./path -> (dir of base file)/path
                resource = new ResourceLocation(baseResource.getNamespace(), baseResource.getPath().replaceFirst("[^/]+$", "") + path.substring(2));
            } else if (!absolute && !path.contains("/")) {
                // Relative to properties file:
                // filename -> (dir of base file)/filename
                resource = new ResourceLocation(baseResource.getNamespace(), baseResource.getPath().replaceFirst("[^/]+$", "") + path);
            } else {
                // Absolute path, w/o namespace:
                // path/filename -> assets/(namespace of base file)/path/filename
                resource = new ResourceLocation(baseResource.getNamespace(), path);
            }
            if (baseResource instanceof ResourceLocationWithSource) {
                resource = new ResourceLocationWithSource(((ResourceLocationWithSource) baseResource).getSource(), resource);
            }
            return resource;
        }

        @Override
        protected <T> T select_Impl(T v1, T v2) {
            return v2;
        }

        @Override
        protected boolean isDefaultResourcePack_Impl() {
            return getResourcePacks(DEFAULT_NAMESPACE).size() <= 1;
        }

        @Override
        protected int getTextureIfLoaded_Impl(ResourceLocation resource) {
            TextureObject texture = Minecraft.getInstance().getTextureManager().getTexture(resource);
            return texture instanceof AbstractTexture ? ((AbstractTexture) texture).glTextureId : -1;
        }

        @Override
        protected void bindTexture_Impl(ResourceLocation resource) {
            Minecraft.getInstance().getTextureManager().bindTexture(resource);
        }

        @Override
        protected void unloadTexture_Impl(ResourceLocation resource) {
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

        @Override
        protected void flushUnusedTextures_Impl() {
            TextureManager textureManager = Minecraft.getInstance().getTextureManager();
            if (textureManager != null) {
                Set<ResourceLocation> texturesToUnload = new HashSet<ResourceLocation>();
                for (Map.Entry<ResourceLocation, TextureObject> entry : textureManager.texturesByName.entrySet()) {
                    ResourceLocation resource = entry.getKey();
                    TextureObject texture = entry.getValue();
                    if (texture instanceof SimpleTexture && !(texture instanceof ThreadDownloadImageData) && !TexturePackAPI.hasResource(resource)) {
                        texturesToUnload.add(resource);
                    }
                }
                for (ResourceLocation resource : texturesToUnload) {
                    unloadTexture(resource);
                }
            }
        }
    }
}
