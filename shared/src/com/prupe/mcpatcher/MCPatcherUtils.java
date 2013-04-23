package com.prupe.mcpatcher;

import net.minecraft.client.Minecraft;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;

/**
 * Collection of static methods available to mods at runtime.  This class is always injected into
 * the output minecraft jar.
 */
public class MCPatcherUtils {
    private static File minecraftDir = null;
    private static String directoryStr = "";
    private static boolean isGame;

    private static Minecraft minecraft;
    private static String minecraftVersion;
    private static String patcherVersion;

    public static final String EXTENDED_HD = "Extended HD";
    public static final String HD_FONT = "HD Font";
    public static final String RANDOM_MOBS = "Random Mobs";
    public static final String CUSTOM_COLORS = "Custom Colors";
    public static final String CONNECTED_TEXTURES = "Connected Textures";
    public static final String BETTER_SKIES = "Better Skies";
    public static final String BETTER_GLASS = "Better Glass";
    public static final String CUSTOM_ITEM_TEXTURES = "Custom Item Textures";
    public static final String GLSL_SHADERS = "GLSL Shaders";

    public static final String CUSTOM_ANIMATIONS = "Custom Animations";
    public static final String MIPMAP = "Mipmap";

    public static final String UTILS_CLASS = "com.prupe.mcpatcher.MCPatcherUtils";
    public static final String LOGGER_CLASS = "com.prupe.mcpatcher.MCLogger";
    public static final String CONFIG_CLASS = "com.prupe.mcpatcher.Config";
    public static final String TILE_MAPPING_CLASS = "com.prupe.mcpatcher.TileMapping";
    public static final String PROFILER_API_CLASS = "com.prupe.mcpatcher.ProfilerAPI";
    public static final String TEXTURE_PACK_API_CLASS = "com.prupe.mcpatcher.TexturePackAPI";
    public static final String TEXTURE_PACK_CHANGE_HANDLER_CLASS = "com.prupe.mcpatcher.TexturePackChangeHandler";
    public static final String WEIGHTED_INDEX_CLASS = "com.prupe.mcpatcher.WeightedIndex";
    public static final String BLEND_METHOD_CLASS = "com.prupe.mcpatcher.BlendMethod";
    public static final String TILE_LOADER_CLASS = "com.prupe.mcpatcher.TileLoader";
    public static final String TESSELLATOR_UTILS_CLASS = "com.prupe.mcpatcher.TessellatorUtils";

    public static final String GL11_CLASS = "org.lwjgl.opengl.GL11";

    public static final String CUSTOM_ANIMATION_CLASS = "com.prupe.mcpatcher.mod.CustomAnimation";
    public static final String FANCY_DIAL_CLASS = "com.prupe.mcpatcher.mod.FancyDial";
    public static final String MIPMAP_HELPER_CLASS = "com.prupe.mcpatcher.mod.MipmapHelper";
    public static final String AA_HELPER_CLASS = "com.prupe.mcpatcher.mod.AAHelper";
    public static final String BORDERED_TEXTURE_CLASS = "com.prupe.mcpatcher.mod.BorderedTexture";
    public static final String FONT_UTILS_CLASS = "com.prupe.mcpatcher.mod.FontUtils";
    public static final String RANDOM_MOBS_CLASS = "com.prupe.mcpatcher.mod.MobRandomizer";
    public static final String MOB_RULE_LIST_CLASS = "com.prupe.mcpatcher.mod.MobRuleList";
    public static final String MOB_OVERLAY_CLASS = "com.prupe.mcpatcher.mod.MobOverlay";
    public static final String COLORIZER_CLASS = "com.prupe.mcpatcher.mod.Colorizer";
    public static final String COLORIZE_WORLD_CLASS = "com.prupe.mcpatcher.mod.ColorizeWorld";
    public static final String COLORIZE_ITEM_CLASS = "com.prupe.mcpatcher.mod.ColorizeItem";
    public static final String COLORIZE_ENTITY_CLASS = "com.prupe.mcpatcher.mod.ColorizeEntity";
    public static final String COLORIZE_BLOCK_CLASS = "com.prupe.mcpatcher.mod.ColorizeBlock";
    public static final String COLOR_MAP_CLASS = "com.prupe.mcpatcher.mod.ColorMap";
    public static final String BIOME_HELPER_CLASS = "com.prupe.mcpatcher.mod.BiomeHelper";
    public static final String LIGHTMAP_CLASS = "com.prupe.mcpatcher.mod.Lightmap";
    public static final String CTM_UTILS_CLASS = "com.prupe.mcpatcher.mod.CTMUtils";
    public static final String TILE_OVERRIDE_INTERFACE = "com.prupe.mcpatcher.mod.ITileOverride";
    public static final String TILE_OVERRIDE_CLASS = "com.prupe.mcpatcher.mod.TileOverride";
    public static final String TILE_OVERRIDE_IMPL_CLASS = "com.prupe.mcpatcher.mod.TileOverrideImpl";
    public static final String GLASS_PANE_RENDERER_CLASS = "com.prupe.mcpatcher.mod.GlassPaneRenderer";
    public static final String RENDER_PASS_CLASS = "com.prupe.mcpatcher.mod.RenderPass";
    public static final String RENDER_PASS_API_CLASS = "com.prupe.mcpatcher.mod.RenderPassAPI";
    public static final String SKY_RENDERER_CLASS = "com.prupe.mcpatcher.mod.SkyRenderer";
    public static final String FIREWORKS_HELPER_CLASS = "com.prupe.mcpatcher.mod.FireworksHelper";
    public static final String CIT_UTILS_CLASS = "com.prupe.mcpatcher.mod.CITUtils";
    public static final String ITEM_OVERRIDE_CLASS = "com.prupe.mcpatcher.mod.ItemOverride";
    public static final String ITEM_OVERLAY_CLASS = "com.prupe.mcpatcher.mod.ItemOverlay";
    public static final String ITEM_OVERLAY_LIST_CLASS = "com.prupe.mcpatcher.mod.ItemOverlayList";
    public static final String SHADERS_CLASS = "com.prupe.mcpatcher.mod.Shaders";

    private MCPatcherUtils() {
    }

    static {
        isGame = true;
        try {
            if (Class.forName("com.prupe.mcpatcher.MCPatcher") != null) {
                isGame = false;
            }
        } catch (ClassNotFoundException e) {
        } catch (Throwable e) {
            e.printStackTrace();
        }

        if (isGame) {
            if (setGameDir(new File(".")) || setGameDir(getDefaultGameDir())) {
                directoryStr = String.format("Game directory:    %s", minecraftDir.getPath());
            } else {
                directoryStr = String.format("Current directory: %s", new File(".").getAbsolutePath());
            }
        }
    }

    static File getDefaultGameDir() {
        String os = System.getProperty("os.name").toLowerCase();
        String baseDir = null;
        String subDir = ".minecraft";
        if (os.contains("win")) {
            baseDir = System.getenv("APPDATA");
        } else if (os.contains("mac")) {
            subDir = "Library/Application Support/minecraft";
        }
        if (baseDir == null) {
            baseDir = System.getProperty("user.home");
        }
        return new File(baseDir, subDir);
    }

    static boolean setGameDir(File dir) {
        if (dir != null && dir.isDirectory() &&
            ((new File(dir, "bin/lwjgl.jar").exists() && new File(dir, "resources").isDirectory()) || // 1.5
                (new File(dir, "libraries").isDirectory() && new File(dir, "versions").isDirectory()))) { // 13w16a
            minecraftDir = dir.getAbsoluteFile();
        } else {
            minecraftDir = null;
        }
        return Config.load(minecraftDir);
    }

    /**
     * Get the path to a file/directory within the minecraft folder.
     *
     * @param subdirs zero or more path components
     * @return combined path
     */
    public static File getMinecraftPath(String... subdirs) {
        File f = minecraftDir;
        for (String s : subdirs) {
            f = new File(f, s);
        }
        return f;
    }

    /**
     * Returns true if running inside game, false if running inside MCPatcher.  Useful for
     * code shared between mods and runtime classes.
     *
     * @return true if in game
     */
    public static boolean isGame() {
        return isGame;
    }

    /**
     * Get a value from a properties file.
     *
     * @param properties   properties file
     * @param key          property name
     * @param defaultValue default value if not found in properties file
     * @return property value
     */
    public static String getStringProperty(Properties properties, String key, String defaultValue) {
        if (properties == null) {
            return defaultValue;
        } else {
            return properties.getProperty(key, defaultValue).trim();
        }
    }

    /**
     * Get a value from a properties file.
     *
     * @param properties   properties file
     * @param key          property name
     * @param defaultValue default value if not found in properties file
     * @return property value
     */
    public static int getIntProperty(Properties properties, String key, int defaultValue) {
        if (properties != null) {
            String value = properties.getProperty(key, "").trim();
            if (!value.equals("")) {
                try {
                    return Integer.parseInt(value);
                } catch (NumberFormatException e) {
                }
            }
        }
        return defaultValue;
    }

    /**
     * Get a value from a properties file.
     *
     * @param properties   properties file
     * @param key          property name
     * @param defaultValue default value if not found in properties file
     * @return property value
     */
    public static boolean getBooleanProperty(Properties properties, String key, boolean defaultValue) {
        if (properties != null) {
            String value = properties.getProperty(key, "").trim().toLowerCase();
            if (!value.equals("")) {
                return Boolean.parseBoolean(value);
            }
        }
        return defaultValue;
    }

    /**
     * Get a value from a properties file.
     *
     * @param properties   properties file
     * @param key          property name
     * @param defaultValue default value if not found in properties file
     * @return property value
     */
    public static float getFloatProperty(Properties properties, String key, float defaultValue) {
        if (properties != null) {
            String value = properties.getProperty(key, "").trim();
            if (!value.equals("")) {
                try {
                    return Float.parseFloat(value);
                } catch (NumberFormatException e) {
                }
            }
        }
        return defaultValue;
    }

    /**
     * Convenience method to close a stream ignoring exceptions.
     *
     * @param closeable closeable object
     */
    public static void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Convenience method to close a zip file ignoring exceptions.
     *
     * @param zip zip file
     */
    public static void close(ZipFile zip) {
        if (zip != null) {
            try {
                zip.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void setMinecraft(Minecraft minecraft) {
        MCPatcherUtils.minecraft = minecraft;
    }

    public static void setVersions(String minecraftVersion, String patcherVersion) {
        MCPatcherUtils.minecraftVersion = minecraftVersion;
        MCPatcherUtils.patcherVersion = patcherVersion;
        System.out.println();
        System.out.printf("MCPatcherUtils initialized:\n");
        System.out.printf("Minecraft version: %s\n", minecraftVersion);
        System.out.printf("MCPatcher version: %s\n", patcherVersion);
        System.out.println(directoryStr);
        System.out.printf("Max heap memory:   %.1fMB\n", Runtime.getRuntime().maxMemory() / 1048576.0f);
        try {
            Class<?> vm = Class.forName("sun.misc.VM");
            Method method = vm.getDeclaredMethod("maxDirectMemory");
            long memory = (Long) method.invoke(null);
            System.out.printf("Max direct memory: %.1fMB\n", memory / 1048576.0f);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        System.out.println();
    }

    /**
     * Get minecraft object.
     *
     * @return minecraft
     */
    public static Minecraft getMinecraft() {
        return minecraft;
    }

    /**
     * Get shortened version of currently running Minecraft, e.g., 1.9pre4.
     *
     * @return string
     */
    public static String getMinecraftVersion() {
        return minecraftVersion;
    }

    /**
     * Get version of MCPatcher.
     *
     * @return string
     */
    public static String getPatcherVersion() {
        return patcherVersion;
    }

    /**
     * Attempts to read image.  Closes input stream regardless of success or failure.
     *
     * @param input open input stream
     * @return image or null
     */
    public static BufferedImage readImage(InputStream input) {
        BufferedImage image = null;
        if (input != null) {
            try {
                image = ImageIO.read(input);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                close(input);
            }
        }
        return image;
    }

    /**
     * Attempts to read a properties file.  Closes input stream regardless of success or failure.
     *
     * @param input open input stream
     * @return properties object or null
     */
    public static Properties readProperties(InputStream input) {
        Properties properties = new Properties();
        if (readProperties(input, properties)) {
            return properties;
        } else {
            return null;
        }
    }

    /**
     * Attempts to read a properties file.  Closes input stream regardless of success or failure.
     *
     * @param input      open input stream
     * @param properties initial properties object
     * @return true if properties were successfully read
     */
    public static boolean readProperties(InputStream input, Properties properties) {
        if (input != null && properties != null) {
            try {
                properties.load(input);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                close(input);
            }
        }
        return false;
    }

    /**
     * Get array of rgb values from image.
     *
     * @param image input image
     * @return rgb array
     */
    public static int[] getImageRGB(BufferedImage image) {
        if (image == null) {
            return null;
        } else {
            int width = image.getWidth();
            int height = image.getHeight();
            int[] rgb = new int[width * height];
            image.getRGB(0, 0, width, height, rgb, 0, width);
            return rgb;
        }
    }

    /**
     * Parse a comma-separated list of integers/ranges.
     *
     * @param list     comma- or space-separated list, e.g., 2-4,5,8,12-20
     * @param minValue smallest value allowed in the list
     * @param maxValue largest value allowed in the list
     * @return possibly empty integer array
     */
    public static int[] parseIntegerList(String list, int minValue, int maxValue) {
        ArrayList<Integer> tmpList = new ArrayList<Integer>();
        Pattern p = Pattern.compile("(\\d*)-(\\d*)");
        for (String token : list.replace(',', ' ').split("\\s+")) {
            try {
                if (token.matches("\\d+")) {
                    tmpList.add(Integer.parseInt(token));
                } else {
                    Matcher m = p.matcher(token);
                    if (m.matches()) {
                        String a = m.group(1);
                        String b = m.group(2);
                        int min = a.equals("") ? minValue : Integer.parseInt(a);
                        int max = b.equals("") ? maxValue : Integer.parseInt(b);
                        for (int i = min; i <= max; i++) {
                            tmpList.add(i);
                        }
                    }
                }
            } catch (NumberFormatException e) {
            }
        }
        if (minValue <= maxValue) {
            for (int i = 0; i < tmpList.size(); ) {
                if (tmpList.get(i) < minValue || tmpList.get(i) > maxValue) {
                    tmpList.remove(i);
                } else {
                    i++;
                }
            }
        }
        int[] a = new int[tmpList.size()];
        for (int i = 0; i < a.length; i++) {
            a[i] = tmpList.get(i);
        }
        return a;
    }
}
