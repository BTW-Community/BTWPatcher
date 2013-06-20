package com.prupe.mcpatcher;

import net.minecraft.client.Minecraft;
import net.minecraft.src.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class TileLoader {
    private static final MCLogger logger = MCLogger.getLogger("Tilesheet");

    private static final List<TileLoader> loaders = new ArrayList<TileLoader>();

    private static final ResourceAddress BLANK_RESOURCE = new ResourceAddress(MCPatcherUtils.BLANK_PNG);

    private static final boolean debugTextures = Config.getBoolean(MCPatcherUtils.CONNECTED_TEXTURES, "debugTextures", false);
    private static final int splitTextures = Config.getInt(MCPatcherUtils.CONNECTED_TEXTURES, "splitTextures", 1);
    private static final Map<String, String> specialTextures = new HashMap<String, String>();

    private static final TexturePackChangeHandler changeHandler;
    private static boolean changeHandlerCalled;
    private static boolean registerIconsCalled;
    private static final Set<TextureMap> overflowMaps = new HashSet<TextureMap>();

    private static final int OVERFLOW_TEXTURE_MAP_INDEX = 2;
    private static final long MAX_TILESHEET_SIZE;

    protected final String mapName;
    protected final boolean allowOverflow;
    protected final MCLogger subLogger;

    private int overflowIndex;
    private TextureMap baseTextureMap;
    private Map<String, TextureStitched> baseTexturesByName;
    private final Set<ResourceAddress> tilesToRegister = new HashSet<ResourceAddress>();
    private final Map<ResourceAddress, BufferedImage> tileImages = new HashMap<ResourceAddress, BufferedImage>();
    private final Map<String, Icon> iconMap = new HashMap<String, Icon>();

    static {
        long maxSize = 4096L;
        try {
            maxSize = Minecraft.getMaxTextureSize();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        MAX_TILESHEET_SIZE = (maxSize * maxSize * 4) * 7 / 8;
        logger.config("max texture size is %dx%d (%.1fMB)", maxSize, maxSize, MAX_TILESHEET_SIZE / 1048576.0f);

        changeHandler = new TexturePackChangeHandler("Tilesheet API", 2) {
            @Override
            public void initialize() {
            }

            @Override
            public void beforeChange() {
                changeHandlerCalled = true;
                TessellatorUtils.clear(Tessellator.instance);
                for (TextureMap textureMap : overflowMaps) {
                    try {
                        textureMap.unloadGLTexture();
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
                overflowMaps.clear();
                loaders.clear();
                specialTextures.clear();
            }

            @Override
            public void afterChange() {
                loop:
                while (true) {
                    for (TileLoader loader : loaders) {
                        if (!loader.tilesToRegister.isEmpty()) {
                            if (loader.allowOverflow && splitTextures > 0) {
                                registerIconsCalled = false;
                                String mapName = loader.mapName + "_overflow" + (++loader.overflowIndex);
                                logger.fine("new TextureMap(%s)", mapName);
                                TextureMap map = new TextureMap(OVERFLOW_TEXTURE_MAP_INDEX, mapName);
                                map.refreshTextures1(TexturePackAPI.getResourceBundle());
                                if (!registerIconsCalled) {
                                    logger.severe("TileLoader.registerIcons was never called!  Possible conflict in TextureMap.class");
                                    break loop;
                                }
                                overflowMaps.add(map);
                            } else {
                                loader.subLogger.warning("could not load all %s tiles (%d remaining)", loader.mapName, loader.tilesToRegister.size());
                                loader.tilesToRegister.clear();
                            }
                            continue loop;
                        }
                    }
                    break;
                }
                changeHandlerCalled = false;
            }

            @Override
            public void afterChange2() {
                for (TileLoader loader : loaders) {
                    loader.finish();
                }
            }
        };
        TexturePackChangeHandler.register(changeHandler);
    }

    public static void registerIcons(TextureMap textureMap, String mapName, Map<String, TextureStitched> map) {
        logger.fine("before registerIcons(%s) %d icons", mapName, map.size());
        registerIconsCalled = true;
        if (!changeHandlerCalled) {
            logger.severe("beforeChange was not called, invoking directly");
            changeHandler.beforeChange();
        }
        TessellatorUtils.registerTextureMap(textureMap, mapName);
        for (TileLoader loader : loaders) {
            if (loader.baseTextureMap == null && mapName.equals(loader.mapName)) {
                loader.baseTextureMap = textureMap;
                loader.baseTexturesByName = map;
            }
            if (loader.isForThisMap(mapName) && !loader.tilesToRegister.isEmpty()) {
                loader.subLogger.fine("adding icons to %s (%d remaining)", mapName, loader.tilesToRegister.size(), mapName);
                while (!loader.tilesToRegister.isEmpty() && loader.registerOneIcon(textureMap, mapName, map)) {
                    // nothing
                }
                loader.subLogger.fine("done adding icons to %s (%d remaining)", mapName, loader.tilesToRegister.size(), mapName);
            }
        }
        logger.fine("after registerIcons(%s) %d icons", mapName, map.size());
    }

    public static String getOverridePath(String prefix, String name, String ext) {
        String path;
        if (name.endsWith(".png")) {
            path = name.replaceFirst("\\.[^.]+$", "") + ext;
        } else {
            path = prefix + name + ext;
        }
        logger.finer("getOverridePath(%s, %s, %s) -> %s", prefix, name, ext, path);
        return path;
    }

    public static boolean isSpecialTexture(TextureMap map, String texture, String special) {
        return special.equals(texture) || special.equals(specialTextures.get(texture));
    }

    public static BufferedImage getOverrideImage(ResourceAddress resource) throws IOException {
        BufferedImage image;
        for (TileLoader loader : loaders) {
            image = loader.tileImages.get(resource);
            if (image != null) {
                return image;
            }
        }
        image = TexturePackAPI.getImage(resource);
        if (image == null) {
            throw new FileNotFoundException(resource + " not found");
        }
        return image;
    }

    public static void updateAnimations() {
        for (TextureMap textureMap : overflowMaps) {
            textureMap.updateAnimations();
        }
    }

    public static BufferedImage generateDebugTexture(String text, int width, int height, boolean alternate) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics graphics = image.getGraphics();
        graphics.setColor(alternate ? new Color(0, 255, 255, 128) : Color.WHITE);
        graphics.fillRect(0, 0, width, height);
        graphics.setColor(alternate ? Color.RED : Color.BLACK);
        int ypos = 10;
        if (alternate) {
            ypos += height / 2;
        }
        int charsPerRow = width / 8;
        if (charsPerRow <= 0) {
            return image;
        }
        while (text.length() % charsPerRow != 0) {
            text += " ";
        }
        while (ypos < height && !text.equals("")) {
            graphics.drawString(text.substring(0, charsPerRow), 1, ypos);
            ypos += graphics.getFont().getSize();
            text = text.substring(charsPerRow);
        }
        return image;
    }

    static void init() {
    }

    public static ResourceAddress getBlocksAtlas() {
        return TextureMap.blocksAtlas;
    }

    public static ResourceAddress getItemsAtlas() {
        return TextureMap.itemsAtlas;
    }

    public TileLoader(String mapName, boolean allowOverflow, MCLogger logger) {
        this.mapName = mapName;
        this.allowOverflow = allowOverflow;
        subLogger = logger;
        loaders.add(this);
    }

    private static long getTextureSize(TextureStitched texture) {
        return texture == null ? 0 : 4 * texture.getWidth() * texture.getHeight();
    }

    private static long getTextureSize(Collection<TextureStitched> textures) {
        long size = 0;
        for (TextureStitched texture : textures) {
            size += getTextureSize(texture);
        }
        return size;
    }

    public static ResourceAddress getDefaultAddress(ResourceAddress propertiesAddress) {
        return TexturePackAPI.newResourceAddress(propertiesAddress, ".properties", ".png");
    }

    public static ResourceAddress parseTileAddress(ResourceAddress propertiesAddress, String value) {
        if (value.equals("blank")) {
            return BLANK_RESOURCE;
        }
        if (value.equals("null") || value.equals("none") || value.equals("default")) {
            return null;
        }
        if (value.equals("")) {
            return null;
        }
        if (!value.endsWith(".png")) {
            value += ".png";
        }
        if (!value.contains("/")) {
            value = propertiesAddress.getPath().replaceFirst("[^/]+$", "") + value;
        }
        return TexturePackAPI.parseResourceAddress(propertiesAddress, value);
    }

    public boolean preloadTile(ResourceAddress resource, boolean alternate, String special) {
        if (tileImages.containsKey(resource)) {
            return true;
        }
        BufferedImage image = null;
        if (!debugTextures) {
            image = TexturePackAPI.getImage(resource);
            if (image == null) {
                subLogger.warning("missing %s", resource);
            }
        }
        if (image == null) {
            image = generateDebugTexture(resource.getPath(), 64, 64, alternate);
        }
        tilesToRegister.add(resource);
        tileImages.put(resource, image);
        if (special != null) {
            specialTextures.put(resource.getPath(), special);
        }
        return true;
    }

    public boolean preloadTile(ResourceAddress resource, boolean alternate) {
        return preloadTile(resource, alternate, null);
    }

    protected boolean isForThisMap(String mapName) {
        if (allowOverflow && splitTextures > 1) {
            return mapName.startsWith(this.mapName + "_overflow");
        } else {
            return mapName.startsWith(this.mapName);
        }
    }

    private boolean registerDefaultIcon(String name) {
        if (name.startsWith(mapName) && name.endsWith(".png") && baseTextureMap != null) {
            String defaultName = name.substring(mapName.length()).replaceFirst("\\.png$", "");
            TextureStitched texture = baseTexturesByName.get(defaultName);
            if (texture != null) {
                subLogger.finer("%s -> existing icon %s", name, defaultName);
                iconMap.put(name, texture);
                return true;
            }
        }
        return false;
    }

    private boolean registerOneIcon(TextureMap textureMap, String mapName, Map<String, TextureStitched> map) {
        ResourceAddress resource = tilesToRegister.iterator().next();
        String name = resource.getPath();
        if (registerDefaultIcon(name)) {
            tilesToRegister.remove(resource);
            return true;
        }
        BufferedImage image = tileImages.get(resource);
        if (image == null) {
            subLogger.error("tile for %s unexpectedly missing", resource);
            tilesToRegister.remove(resource);
            return true;
        }
        int width = image.getWidth();
        int height = image.getHeight();
        long currentSize = getTextureSize(map.values());
        long newSize = 4 * width * width;
        if (newSize + currentSize > MAX_TILESHEET_SIZE) {
            float sizeMB = (float) currentSize / 1048576.0f;
            if (currentSize <= 0) {
                subLogger.error("%s too big for any tilesheet (%.1fMB), dropping", name, sizeMB);
                tilesToRegister.remove(resource);
                return true;
            } else {
                subLogger.warning("%s nearly full (%.1fMB), will start a new tilesheet", mapName, sizeMB);
                return false;
            }
        }
        Icon icon = textureMap.registerIcon(name);
        map.put(name, (TextureStitched) icon);
        if (mapName.contains("_overflow")) {
            TessellatorUtils.registerIcon(textureMap, icon);
        }
        iconMap.put(name, icon);
        String extra = (width == height ? "" : ", " + (height / width) + " frames");
        subLogger.finer("%s -> %s icon %dx%d%s", name, mapName, width, width, extra);
        tilesToRegister.remove(resource);
        return true;
    }

    public void finish() {
        tilesToRegister.clear();
        tileImages.clear();
    }

    public Icon getIcon(String name) {
        if (name == null || name.equals("")) {
            return null;
        }
        Icon icon = iconMap.get(name);
        if (icon == null && baseTexturesByName != null) {
            icon = baseTexturesByName.get(name);
        }
        return icon;
    }

    public boolean setDefaultTextureMap(Tessellator tessellator) {
        tessellator.textureMap = baseTextureMap;
        return baseTextureMap != null;
    }
}
