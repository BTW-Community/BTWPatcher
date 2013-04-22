package com.prupe.mcpatcher;

import com.prupe.mcpatcher.mod.TessellatorUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.src.*;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

abstract public class TileLoader {
    private static final MCLogger logger = MCLogger.getLogger(MCPatcherUtils.CONNECTED_TEXTURES, "CTM");

    private static final List<TileLoader> loaders = new ArrayList<TileLoader>();

    private static final boolean debugTextures = Config.getBoolean(MCPatcherUtils.CONNECTED_TEXTURES, "debugTextures", false);
    private static String overrideTextureName;

    private static final int MAX_CTM_TEXTURE_SIZE;

    protected final MCLogger subLogger;
    private final List<String> tileNames = new ArrayList<String>();
    private final Set<String> tilesToRegister = new HashSet<String>();
    private final Map<String, List<Texture>> tileTextures = new HashMap<String, List<Texture>>();
    private final Map<String, Icon> iconMap = new HashMap<String, Icon>();

    static {
        int maxSize = Minecraft.getMaxTextureSize();
        logger.config("max texture size is %dx%d", maxSize, maxSize);
        MAX_CTM_TEXTURE_SIZE = (maxSize * maxSize) * 7 / 8;
    }

    public static void registerIcons(TextureMap textureMap, Stitcher stitcher, String mapName, Map<StitchHolder, List<Texture>> map) {
        for (TileLoader loader : loaders) {
            if (loader.isForThisMap(mapName)) {
                while (!loader.tilesToRegister.isEmpty() && loader.registerOneIcon(textureMap, stitcher, map)) {
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

    static BufferedImage generateDebugTexture(String text, int width, int height, boolean alternate) {
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

    public TileLoader(MCLogger logger) {
        subLogger = logger;
    }

    protected static String expandTileName(String defaultDirectory, String name) {
        if (!name.toLowerCase().endsWith(".png")) {
            name += ".png";
        }
        defaultDirectory = defaultDirectory.replaceFirst("/$", "");
        if (!name.startsWith("/") && !defaultDirectory.equals("")) {
            name = defaultDirectory + "/" + name;
        }
        return name;
    }

    private static int getTextureSize(List<Texture> textures) {
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

    private static int getTextureSize(Collection<List<Texture>> textures) {
        int size = 0;
        for (List<Texture> texture : textures) {
            size += getTextureSize(texture);
        }
        return size;
    }

    protected boolean preloadTile(String path, boolean alternate) {
        if (tileTextures.containsKey(path)) {
            tileNames.add(path);
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
        tileNames.add(path);
        tilesToRegister.add(path);
        tileTextures.put(path, textures);
        return true;
    }

    protected String getNextTilesheetName() {
        return null;
    }

    protected boolean isForThisMap(String mapName) {
        return false;
    }

    private boolean registerOneIcon(TextureMap textureMap, Stitcher stitcher, Map<StitchHolder, List<Texture>> map) {
        String name = tilesToRegister.iterator().next();
        List<Texture> textures = tileTextures.get(name);
        if (textures == null || textures.isEmpty()) {
            subLogger.error("tile for %s unexpectedly missing", name);
            tilesToRegister.remove(name);
            return true;
        }
        int currentSize = getTextureSize(map.values());
        int newSize = getTextureSize(textures);
        if (newSize + currentSize > MAX_CTM_TEXTURE_SIZE) {
            return false;
        }
        Texture texture = textures.get(0);
        StitchHolder holder = new StitchHolder(texture);
        stitcher.addStitchHolder(holder);
        map.put(holder, textures);
        Icon icon = textureMap.registerIcon(name);
        TessellatorUtils.registerIcon(textureMap, icon);
        iconMap.put(name, icon);
        String extra = (textures.size() > 1 ? ", " + textures.size() + " frames" : "");
        subLogger.finer("%s -> icon: %dx%d%s", name, texture.getWidth(), texture.getHeight(), extra);
        tilesToRegister.remove(name);
        return false;
    }

    public Icon getIcon(String path) {
        return iconMap.get(path);
    }
}
