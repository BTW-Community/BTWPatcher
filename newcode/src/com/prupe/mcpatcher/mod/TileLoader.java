package com.prupe.mcpatcher.mod;

import com.prupe.mcpatcher.Config;
import com.prupe.mcpatcher.MCLogger;
import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.TexturePackAPI;
import net.minecraft.src.*;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

public class TileLoader {
    private static final MCLogger logger = MCLogger.getLogger(MCPatcherUtils.CONNECTED_TEXTURES, "CTM");

    private static final boolean debugTextures = Config.getBoolean(MCPatcherUtils.CONNECTED_TEXTURES, "debugTextures", false);
    private static String overrideTextureName;

    private final Map<String, List<Texture>> tileTextures = new HashMap<String, List<Texture>>();
    private final Map<String, Icon> loadedIcons = new HashMap<String, Icon>();

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

    boolean preload(String name, List<String> tileNames, boolean alternate) {
        if (!name.toLowerCase().endsWith(".png")) {
            name += ".png";
        }
        if (tileTextures.containsKey(name)) {
            tileNames.add(name);
            return true;
        }
        List<Texture> textures;
        try {
            overrideTextureName = name;
            if (debugTextures || !TexturePackAPI.hasResource(name)) {
                BufferedImage fallbackImage = generateDebugTexture(name, 64, 64, alternate);
                Texture texture = TextureManager.getInstance().setupTexture(
                    name, 2, fallbackImage.getWidth(), fallbackImage.getHeight(), GL11.GL_CLAMP, GL11.GL_RGBA, GL11.GL_NEAREST, GL11.GL_NEAREST, false, fallbackImage
                );
                if (texture == null) {
                    return false;
                }
                textures = new ArrayList<Texture>();
                textures.add(texture);
            } else {
                textures = TextureManager.getInstance().createTexture(name.replaceFirst("^/", ""));
                if (textures == null || textures.isEmpty()) {
                    return false;
                }
            }
        } finally {
            overrideTextureName = null;
        }
        tileNames.add(name);
        tileTextures.put(name, textures);
        return true;
    }

    Icon[] registerIcons(TextureMap textureMap, Stitcher stitcher, Map<StitchHolder, List<Texture>> map, List<String> tileNames) {
        Icon[] icons = new Icon[tileNames.size()];
        for (int i = 0; i < tileNames.size(); i++) {
            String imageName = tileNames.get(i);
            if (imageName == null) {
                continue;
            }
            icons[i] = loadedIcons.get(imageName);
            if (icons[i] != null) {
                continue;
            }
            List<Texture> textures = tileTextures.get(imageName);
            if (textures == null || textures.isEmpty()) {
                logger.error("tile for %s unexpectedly missing", imageName);
                continue;
            }
            Texture texture = textures.get(0);
            StitchHolder holder = new StitchHolder(texture);
            stitcher.registerStitchHolder(holder);
            map.put(holder, textures);
            icons[i] = textureMap.getIcon(imageName);
            TessellatorUtils.registerIcon(textureMap, icons[i]);
            loadedIcons.put(imageName, icons[i]);
            String extra = (textures.size() > 1 ? ", " + textures.size() + " frames" : "");
            logger.finer("%s -> icon: %dx%d%s", imageName, texture.getWidth(), texture.getHeight(), extra);
        }
        return icons;
    }

    int getTextureSize(List<String> tileNames) {
        Set<String> names = new HashSet<String>();
        names.addAll(tileNames);
        int size = 0;
        for (String name : names) {
            size += getTextureSize(name);
        }
        return size;
    }

    int getTextureSize(String name) {
        if (name == null) {
            return 0;
        }
        List<Texture> textures = tileTextures.get(name);
        if (textures == null || textures.isEmpty() || loadedIcons.get(name) != null) {
            return 0;
        } else {
            return textures.get(0).getWidth() * textures.get(0).getHeight();
        }
    }
    
    void finish() {
        tileTextures.clear();
    }
}
