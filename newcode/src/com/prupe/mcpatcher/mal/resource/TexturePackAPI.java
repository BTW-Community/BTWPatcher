package com.prupe.mcpatcher.mal.resource;

import com.prupe.mcpatcher.MAL;
import com.prupe.mcpatcher.MCLogger;
import com.prupe.mcpatcher.MCPatcherUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.src.*;
import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Pattern;

abstract public class TexturePackAPI {
    private static final MCLogger logger = MCLogger.getLogger("Texture Pack");

    public static final String DEFAULT_NAMESPACE = "minecraft";
    public static final String MCPATCHER_SUBDIR = "mcpatcher/";

    public static TexturePackAPI instance = MAL.newInstance(TexturePackAPI.class, "texturepack");

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

    public static ResourceLocation newMCPatcherResourceLocation(String path) {
        return new ResourceLocation(MCPATCHER_SUBDIR + path);
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
        if (resource instanceof ResourceLocationWithSource) {
            try {
                return ((ResourceLocationWithSource) resource).getSource().getInputStream(resource);
            } catch (IOException e) {
                // nothing
            }
        }
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

    abstract protected InputStream getInputStream_Impl(ResourceLocation resource) throws IOException;

    abstract protected InputStream getInputStream_Impl(ResourcePack resourcePack, ResourceLocation resource) throws IOException;

    abstract protected String getFullPath_Impl(ResourceLocation resource);

    abstract protected String parseNamespace_Impl(String path);

    abstract protected String select_Impl(String v1Path, String v2Path);

    abstract protected boolean isDefaultResourcePack_Impl();

    final private static class V1 extends TexturePackAPI {
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
        protected String parseNamespace_Impl(String path) {
            return path.length() > 0 ? DEFAULT_NAMESPACE : null;
        }

        @Override
        protected String select_Impl(String v1Path, String v2Path) {
            return v1Path;
        }

        @Override
        protected boolean isDefaultResourcePack_Impl() {
            ResourcePack texturePack = getTexturePack();
            return texturePack == null || texturePack instanceof DefaultResourcePack;
        }
    }

    final private static class V2 extends TexturePackAPI {
        private static final String ASSETS = "assets/";

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
        protected String parseNamespace_Impl(String path) {
            if (path.startsWith(ASSETS)) {
                path = path.substring(ASSETS.length());
                int slash = path.indexOf('/');
                if (slash > 0) {
                    return path.substring(0, slash - 1);
                }
            }
            return null;
        }

        @Override
        protected String select_Impl(String v1Path, String v2Path) {
            return v2Path;
        }

        @Override
        protected boolean isDefaultResourcePack_Impl() {
            return getResourcePacks(DEFAULT_NAMESPACE).size() <= 1;
        }
    }
}
