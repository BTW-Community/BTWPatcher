package com.prupe.mcpatcher;

import net.minecraft.client.Minecraft;
import net.minecraft.src.*;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

public class TileLoader {
    private static final MCLogger logger = MCLogger.getLogger("Tilesheet");

    private static final List<TileLoader> loaders = new ArrayList<TileLoader>();

    private static final boolean debugTextures = Config.getBoolean(MCPatcherUtils.CONNECTED_TEXTURES, "debugTextures", false);
    private static final int splitTextures = Config.getInt(MCPatcherUtils.CONNECTED_TEXTURES, "splitTextures", 1);
    private static String overrideTextureName;

    private static final TexturePackChangeHandler changeHandler;
    private static boolean changeHandlerCalled;
    private static boolean registerIconsCalled;
    private static final Set<TextureMap> overflowMaps = new HashSet<TextureMap>();

    private static final int OVERFLOW_TEXTURE_MAP_INDEX = 2;
    private static final long MAX_TILESHEET_SIZE;
    private static final BufferedImage missingTextureImage = generateDebugTexture("missing", 64, 64, false);

    protected final String mapName;
    protected final boolean allowOverflow;
    protected final MCLogger subLogger;

    private int overflowIndex;
    private TextureMap textureMap;
    private final Set<String> tilesToRegister = new HashSet<String>();
    private final Map<String, List<Texture>> tileTextures = new HashMap<String, List<Texture>>();
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
                        textureMap.getTexture().unloadGLTexture();
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
                overflowMaps.clear();
                loaders.clear();
            }

            @Override
            public void afterChange() {
                loop:
                while (true) {
                    for (TileLoader loader : loaders) {
                        if (!loader.tilesToRegister.isEmpty()) {
                            if (loader.allowOverflow && splitTextures > 0) {
                                registerIconsCalled = false;
                                String mapName = loader.mapName + "_overflow" + ++loader.overflowIndex;
                                logger.fine("new TextureMap(%s)", mapName);
                                TextureMap map = new TextureMap(OVERFLOW_TEXTURE_MAP_INDEX, mapName, "not_used", missingTextureImage);
                                map.refreshTextures();
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
                for (TileLoader loader : loaders) {
                    loader.finish();
                }
                changeHandlerCalled = false;
            }
        };
        TexturePackChangeHandler.register(changeHandler);
    }

    public static void registerIcons(TextureMap textureMap, Stitcher stitcher, String mapName, Map<StitchHolder, List<Texture>> map) {
        registerIconsCalled = true;
        if (!changeHandlerCalled) {
            logger.severe("beforeChange was not called, invoking directly");
            changeHandler.beforeChange();
        }
        TessellatorUtils.registerTextureMap(textureMap, mapName);
        for (TileLoader loader : loaders) {
            if (loader.textureMap == null && mapName.equals(loader.mapName)) {
                loader.textureMap = textureMap;
            }
            if (loader.isForThisMap(mapName)) {
                while (!loader.tilesToRegister.isEmpty() && loader.registerOneIcon(textureMap, stitcher, mapName, map)) {
                    // nothing
                }
            }
        }
    }

    public static String getOverridePath(String prefix, String name, String ext) {
        String path;
        if (name.startsWith("/")) {
            path = name.substring(1).replaceFirst("\\.[^.]+$", "") + ext;
        } else {
            path = prefix + name + ext;
        }
        logger.finer("getOverridePath(%s, %s, %s) -> %s", prefix, name, ext, path);
        return path;
    }

    public static String getOverrideTextureName(String name) {
        if (overrideTextureName == null) {
            if (name.matches("^\\d+$")) {
                logger.warning("no override set for %s", name);
            }
            return name;
        } else {
            logger.finer("getOverrideTextureName(%s) -> %s", name, overrideTextureName);
            return overrideTextureName;
        }
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

    public TileLoader(String mapName, boolean allowOverflow, MCLogger logger) {
        this.mapName = mapName;
        this.allowOverflow = allowOverflow;
        subLogger = logger;
        loaders.add(this);
    }

    private static long getTextureSize(List<Texture> textures) {
        if (textures.isEmpty()) {
            return 0;
        } else {
            Texture texture = textures.get(0);
            if (texture == null) {
                return 0;
            }
            return 4 * texture.getWidth() * texture.getHeight();
        }
    }

    private static long getTextureSize(Collection<List<Texture>> textures) {
        long size = 0;
        for (List<Texture> texture : textures) {
            size += getTextureSize(texture);
        }
        return size;
    }

    public boolean preloadTile(String path, boolean alternate) {
        if (tileTextures.containsKey(path)) {
            return true;
        }
        List<Texture> textures;
        try {
            overrideTextureName = path;
            if (debugTextures || !TexturePackAPI.hasResource(path)) {
                BufferedImage fallbackImage = generateDebugTexture(path, 64, 64, alternate);
                Texture texture = TextureManager.getInstance().createTextureFromImage(
                    path, 2, fallbackImage.getWidth(), fallbackImage.getHeight(), GL11.GL_CLAMP, GL11.GL_RGBA, GL11.GL_NEAREST, GL11.GL_NEAREST, false, fallbackImage
                );
                if (texture == null) {
                    return false;
                }
                textures = new ArrayList<Texture>();
                textures.add(texture);
            } else {
                textures = TextureManager.getInstance().createTextureFromFile(path.replaceFirst("^/", ""));
                if (textures == null || textures.isEmpty()) {
                    return false;
                }
            }
        } finally {
            overrideTextureName = null;
        }
        tilesToRegister.add(path);
        tileTextures.put(path, textures);
        return true;
    }

    protected boolean isForThisMap(String mapName) {
        if (allowOverflow && splitTextures > 1) {
            return mapName.startsWith(this.mapName + "_overflow");
        } else {
            return mapName.startsWith(this.mapName);
        }
    }

    private boolean registerOneIcon(TextureMap textureMap, Stitcher stitcher, String mapName, Map<StitchHolder, List<Texture>> map) {
        String name = tilesToRegister.iterator().next();
        List<Texture> textures = tileTextures.get(name);
        if (textures == null || textures.isEmpty()) {
            subLogger.error("tile for %s unexpectedly missing", name);
            tilesToRegister.remove(name);
            return true;
        }
        long currentSize = getTextureSize(map.values());
        long newSize = getTextureSize(textures);
        if (newSize + currentSize > MAX_TILESHEET_SIZE) {
            float sizeMB = (float) currentSize / 1048576.0f;
            if (currentSize <= 0) {
                subLogger.error("%s too big for any tilesheet (%.1fMB), dropping", name, sizeMB);
                tilesToRegister.remove(name);
                return true;
            } else {
                subLogger.warning("%s nearly full (%.1fMB), will start a new tilesheet", mapName, sizeMB);
                return false;
            }
        }
        Texture texture = textures.get(0);
        StitchHolder holder = new StitchHolder(texture);
        stitcher.addStitchHolder(holder);
        map.put(holder, textures);
        Icon icon = textureMap.registerIcon(name);
        if (mapName.contains("_overflow")) {
            TessellatorUtils.registerIcon(textureMap, icon);
        }
        iconMap.put(name, icon);
        String extra = (textures.size() > 1 ? ", " + textures.size() + " frames" : "");
        subLogger.finer("%s -> %s icon: %dx%d%s", name, mapName, texture.getWidth(), texture.getHeight(), extra);
        tilesToRegister.remove(name);
        return true;
    }

    public void finish() {
        tilesToRegister.clear();
        tileTextures.clear();
    }

    public Icon getIcon(String path) {
        return iconMap.get(path);
    }

    public boolean setDefaultTextureMap(Tessellator tessellator) {
        tessellator.textureMap = textureMap;
        return textureMap != null;
    }

    public Icon registerIcon(String name) {
        return textureMap.registerIcon(name);
    }
}
