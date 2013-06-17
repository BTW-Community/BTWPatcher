package com.prupe.mcpatcher.converter;

import com.prupe.mcpatcher.Logger;
import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.TileMapping;
import com.prupe.mcpatcher.UserInterface;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class TexturePackConverter {
    private static final Pattern CUSTOM_BLOCK_PNG = Pattern.compile("(?:anim)?/custom_terrain_(\\d+).png");
    private static final Pattern CUSTOM_ITEM_PNG = Pattern.compile("(?:anim)?/custom_item_(\\d+).png");
    private static final Pattern CTM_TERRAIN_PROPERTIES = Pattern.compile("terrain(\\d+)(.*)\\.properties$");
    private static final String COMPASS_BASE_PNG = "/misc/compass_base.png";
    private static final String COMPASS_PROPERTIES = "/misc/compass.properties";
    private static final String COMPASS_DIAL_PNG = "/misc/compass_dial.png";
    private static final String COMPASS_OVERLAY_PNG = "/misc/compass_overlay.png";
    private static final String DEFAULT_COMPASS_PNG = "/textures/items/compass.png";

    private static final int BLOCK_ID_GLASS = 20;
    private static final int BLOCK_ID_GLASS_PANE = 102;
    private static final int BLOCK_ID_BOOKSHELF = 47;
    private static final int TILE_NUM_SANDSTONE_SIDE = 192;

    private static final int NUM_PASSES = 4;

    private static final HashMap<String, Integer> fixedDuration = new HashMap<String, Integer>();
    private static final HashSet<String> doubleImages = new HashSet<String>();
    private static final int[] skipBlockTiles = new int[]{
        14, /* portal */
        31, 47, /* fire */
        205, /* still water */
        206, 207, 222, 223, /* flowing water */
        237, /* still lava */
        238, 239, 254, 255, /* flowing lava */

    };

    private final File input;
    private final File output;
    private ZipFile inZip;
    private ZipOutputStream outZip;
    private final Map<String, ByteArrayOutputStream> outData = new HashMap<String, ByteArrayOutputStream>();
    private final List<String> messages = new ArrayList<String>();
    private final Map<String, BufferedImage> ctmSources = new HashMap<String, BufferedImage>();
    private final Map<String, String> ctmTiles = new HashMap<String, String>();

    static {
        fixedDuration.put("fire_0", 1);
        fixedDuration.put("lava_flow", 1);
        fixedDuration.put("lava", 1);
        fixedDuration.put("water", 1);

        doubleImages.add("lava_flow");
        doubleImages.add("water_flow");
    }

    public TexturePackConverter(File input) {
        this.input = input;
        output = new File(input.getParentFile(), "mcpatcher-converted-" + input.getName().replaceFirst("^converted-", ""));
    }

    public List<String> getMessages() {
        return messages;
    }

    public File getOutputFile() {
        return output;
    }

    public boolean convert(UserInterface ui) {
        if (this.input.equals(output)) {
            addMessage(2, "ERROR: Input and output files are the same");
            return false;
        }
        try {
            addMessage(0, "");
            addMessage(0, "Converting texture pack");
            ui.setStatusText("Reading %s...", input.getName());
            inZip = new ZipFile(input);
            final List<? extends ZipEntry> inEntries = Collections.list(inZip.entries());
            addMessage(0, "  input:  %s", input.getPath());
            addMessage(0, "    %d bytes", input.length());
            addMessage(0, "    %d files", inEntries.size());
            addMessage(0, "  output: %s", output.getPath());

            int total = NUM_PASSES * inEntries.size();
            int progress = 0;
            outer:
            for (int pass = 1; pass <= NUM_PASSES; pass++) {
                addMessage(0, "");
                addMessage(0, "Pass #%d", pass);
                for (ZipEntry entry : inEntries) {
                    ui.updateProgress(progress++, total);
                    String name = entry.getName();
                    if (entry.isDirectory()) {
                        continue;
                    }
                    switch (pass) {
                        case 1:
                            copyEntry(entry);
                            break;

                        case 2:
                            TileMapping tileMapping = TileMapping.getTileMapping("/" + name);
                            if (tileMapping != null) {
                                if (name.equals("terrain.png")) {
                                    convertTilesheet(entry, tileMapping, skipBlockTiles);
                                    for (int i = 0; i <= 2; i++) {
                                        String carrot = getEntryName("/textures/blocks/carrots_" + i + ".png");
                                        String potato = getEntryName("/textures/blocks/potatoes_" + i + ".png");
                                        if (outData.containsKey(carrot) && !outData.containsKey(potato)) {
                                            addMessage(0, "copy %s -> %s", carrot, potato);
                                            outData.put(potato, outData.get(carrot));
                                        }
                                    }
                                } else {
                                    convertTilesheet(entry, tileMapping, null);
                                }
                                removeEntry(name);
                            }
                            if (name.matches("terrain/(sun|moon|sky).*")) {
                                convertSky(entry);
                                removeEntry(name);
                            }
                            if (name.matches("mob/(pig)?zombie\\d*.png")) {
                                convertZombieSkin(entry);
                            }
                            if (name.matches("mob/.*_eyes\\d*\\.png")) {
                                convertEyes(entry);
                            }
                            if (name.equals("misc/compass_dial.png")) {
                                convertCompass(entry);
                            }
                            if (name.equals("ctm.png")) {
                                convertDefaultCTM(entry);
                            }
                            if (name.startsWith("ctm/") && name.endsWith(".properties")) {
                                convertCTM(entry);
                            }
                            break;

                        case 3:
                            if (name.endsWith("custom_water_still.png") && convertAnimation(entry, "water", "blocks")) {
                                removeEntry(name);
                            }
                            if (name.endsWith("custom_water_flowing.png") && convertAnimation(entry, "water_flow", "blocks")) {
                                removeEntry(name);
                            }
                            if (name.endsWith("custom_lava_still.png") && convertAnimation(entry, "lava", "blocks")) {
                                removeEntry(name);
                            }
                            if (name.endsWith("custom_lava_flowing.png") && convertAnimation(entry, "lava_flow", "blocks")) {
                                removeEntry(name);
                            }
                            if (name.endsWith("custom_portal.png") && convertAnimation(entry, "portal", "blocks")) {
                                removeEntry(name);
                            }
                            if (name.endsWith("custom_fire_e_w.png") && convertAnimation(entry, "fire_0", "blocks")) {
                                removeEntry(name);
                            }
                            if (name.endsWith("custom_fire_n_s.png") && convertAnimation(entry, "fire_1", "blocks")) {
                                removeEntry(name);
                            }
                            break;

                        case 4:
                            if (convertAnimation(entry, CUSTOM_BLOCK_PNG, TileMapping.BLOCKS, "blocks")) {
                                removeEntry(name);
                                removeEntry(name.replace(".png", ".properties"));
                            } else if (convertAnimation(entry, CUSTOM_ITEM_PNG, TileMapping.ITEMS, "items")) {
                                removeEntry(name);
                                removeEntry(name.replace(".png", ".properties"));
                            } else if (name.startsWith("anim/") && name.endsWith(".properties") && convertCTMAnimation(entry)) {
                                removeEntry(name);
                            }
                            break;

                        default:
                            break outer;
                    }
                }
            }
            MCPatcherUtils.close(inZip);
            inZip = null;

            List<String> names = new ArrayList<String>();
            names.addAll(outData.keySet());
            for (String name : names) {
                if (!name.endsWith("/") && !name.equals("/")) {
                    addDirectory(name.replaceAll("[^/]+$", ""));
                }
            }
            removeEntry("");

            List<Map.Entry<String, ByteArrayOutputStream>> outEntries = new ArrayList<Map.Entry<String, ByteArrayOutputStream>>();
            ui.setStatusText("Writing %s...", output.getName());
            Logger.log(Logger.LOG_JAR, "");
            Logger.log(Logger.LOG_JAR, "Writing %s", output.getName());
            outEntries.addAll(outData.entrySet());
            Collections.sort(outEntries, new Comparator<Map.Entry<String, ByteArrayOutputStream>>() {
                public int compare(Map.Entry<String, ByteArrayOutputStream> o1, Map.Entry<String, ByteArrayOutputStream> o2) {
                    return o1.getKey().compareTo(o2.getKey());
                }
            });
            outZip = new ZipOutputStream(new FileOutputStream(output));
            total = outEntries.size();
            progress = 0;
            for (Map.Entry<String, ByteArrayOutputStream> e : outEntries) {
                ui.updateProgress(progress++, total);
                String name = e.getKey();
                ByteArrayOutputStream data = e.getValue();
                outZip.putNextEntry(new ZipEntry(name));
                Logger.log(Logger.LOG_JAR, "  %s", name);
                if (data != null) {
                    outZip.write(data.toByteArray());
                    outZip.closeEntry();
                }
            }
            outZip.close();
            outZip = null;

            ui.setStatusText("");
            addMessage(0, "");
            addMessage(0, "Conversion finished");
            addMessage(0, "  input:  %s", input.getPath());
            addMessage(0, "    %d bytes", input.length());
            addMessage(0, "    %d files", inEntries.size());
            addMessage(0, "  output: %s", output.getPath());
            addMessage(0, "    %d bytes", output.length());
            addMessage(0, "    %d files", outEntries.size());
        } catch (IOException e) {
            e.printStackTrace();
            addMessage(2, "ERROR: %s", e);
            MCPatcherUtils.close(outZip);
            addMessage(0, "deleting %s", output.getPath());
            output.delete();
            return false;
        } finally {
            MCPatcherUtils.close(inZip);
            MCPatcherUtils.close(outZip);
        }
        return true;
    }

    private boolean convertAnimation(ZipEntry entry, Pattern pattern, String[] list, String type) {
        String name = entry.getName();
        if (!name.startsWith("anim")) {
            return false;
        }
        Matcher matcher = pattern.matcher(name);
        if (!matcher.matches()) {
            return false;
        }
        int index;
        try {
            index = Integer.parseInt(matcher.group(1));
            if (index < 0 || index >= list.length || list[index] == null) {
                addMessage(1, "WARNING: unknown tile animation %s", name);
                return false;
            }
        } catch (NumberFormatException e) {
            return false;
        }
        return convertAnimation(entry, list[index], type);
    }

    private boolean convertAnimation(ZipEntry entry, String tileName, String type) {
        String name = entry.getName();
        String propertiesName = name.replace(".png", ".properties");
        Properties properties = getProperties(propertiesName);
        BufferedImage image = null;
        boolean forceTxt = fixedDuration.containsKey(tileName);
        int defaultDuration = forceTxt ? fixedDuration.get(tileName) : 1;
        forceTxt = true; // 13w09a+ requires txt files for all animations
        if (properties != null || forceTxt) {
            String txtName = "textures/" + type + "/" + tileName + ".txt";
            image = getImage(name);
            if (image == null) {
                return false;
            }
            int numTiles = image.getHeight() / image.getWidth();
            convertAnimationProperties(propertiesName, properties, txtName, defaultDuration, forceTxt, numTiles);
        }

        String newName = "textures/" + type + "/" + tileName + ".png";
        if (doubleImages.contains(tileName)) {
            if (image == null) {
                image = getImage(name);
                if (image == null) {
                    return false;
                }
            }
            addMessage(0, "%s -> %s (x2)", entry.getName(), newName);
            int width = image.getWidth();
            int height = image.getHeight();
            int numTiles = height / width;
            BufferedImage newImage = new BufferedImage(width * 2, height * 2, BufferedImage.TYPE_INT_ARGB);
            int[] rgb = new int[width * width];
            for (int i = 0; i < numTiles; i++) {
                image.getRGB(0, i * width, width, width, rgb, 0, width);
                newImage.setRGB(0, 2 * i * width, width, width, rgb, 0, width);
                newImage.setRGB(width, 2 * i * width, width, width, rgb, 0, width);
                newImage.setRGB(0, (2 * i + 1) * width, width, width, rgb, 0, width);
                newImage.setRGB(width, (2 * i + 1) * width, width, width, rgb, 0, width);
            }
            addEntry(newName, newImage);
        } else {
            addMessage(0, "%s -> %s", entry.getName(), newName);
            copyEntry(entry, newName);
        }

        return true;
    }

    private boolean convertAnimationProperties(String propertiesName, Properties properties, String txtName, int defaultDuration, boolean forceTxt, int numTiles) {
        if (properties == null) {
            properties = new Properties();
        }
        defaultDuration = MCPatcherUtils.getIntProperty(properties, "duration", defaultDuration);
        PrintStream txtStream = new PrintStream(getOutputStream(txtName));
        int i;
        for (i = 0; ; i++) {
            String tile = properties.getProperty("tile." + i, "");
            String duration = properties.getProperty("duration." + i, "");
            if (tile.equals("") && duration.equals("")) {
                break;
            }
            if (tile.equals("")) {
                tile = "" + i;
            }
            txtStream.print(tile);
            try {
                int d = Integer.parseInt(duration);
                if (d <= 1) {
                    d = defaultDuration;
                }
                if (d > 1 || forceTxt) {
                    txtStream.print('*');
                    txtStream.print(duration);
                }
            } catch (NumberFormatException e) {
            }
            txtStream.println();
        }
        if (i == 0 && numTiles > 0 && (defaultDuration > 1 || forceTxt)) {
            for (i = 0; i < numTiles; i++) {
                txtStream.print(i);
                txtStream.print('*');
                txtStream.println(defaultDuration);
            }
        }
        txtStream.close();
        if (i > 0) {
            addMessage(0, "%s -> %s", propertiesName, txtName);
        } else {
            removeEntry(txtName);
        }
        return true;
    }

    private boolean convertTilesheet(ZipEntry entry, TileMapping tileMapping, int[] skipTiles) {
        String name = entry.getName();
        BufferedImage image = getImage(name);
        if (image == null) {
            return false;
        }
        int tileWidth = image.getWidth() / 16;
        int tileHeight = image.getHeight() / 16;
        BufferedImage newImage = new BufferedImage(tileWidth, tileHeight, BufferedImage.TYPE_INT_ARGB);
        int[] rgb = new int[tileWidth * tileHeight];
        String[][] tileNames = tileMapping.getTileNames();
        if (skipTiles != null) {
            for (int i : skipTiles) {
                tileNames[i] = null;
            }
        }
        for (int i = 0; i < tileNames.length; i++) {
            String[] names = tileNames[i];
            if (names != null && names.length > 0) {
                image.getRGB((i % 16) * tileWidth, (i / 16) * tileHeight, tileWidth, tileHeight, rgb, 0, tileWidth);
                newImage.setRGB(0, 0, tileWidth, tileHeight, rgb, 0, tileWidth);
                for (String s : names) {
                    if (!outData.containsKey(getEntryName(s))) {
                        addMessage(0, "%s %d,%d -> %s", name, i % 16, i / 16, s);
                        addEntry(s, newImage);
                    }
                }
            }
        }
        return true;
    }

    private boolean convertSky(ZipEntry entry) {
        String name = entry.getName();
        String newName = name.replaceFirst("^terrain", "environment");
        if (name.endsWith(".properties")) {
            Properties properties = getProperties(name);
            if (properties != null) {
                String source = properties.getProperty("source", "");
                properties.setProperty("source", source.replaceFirst("^/terrain", "/environment"));
                addEntry(newName, properties);
                return true;
            }
        }
        copyEntry(entry, newName);
        return true;
    }

    private boolean convertZombieSkin(ZipEntry entry) {
        String name = entry.getName();
        BufferedImage image = getImage(name);
        if (image == null) {
            return false;
        }
        int width = image.getWidth();
        int height = image.getHeight();
        if (width != 2 * height) {
            return false;
        }
        addMessage(0, "%s (%dx%d) -> %s (%dx%d)", name, width, height, name, width, 2 * height);
        BufferedImage newImage = new BufferedImage(width, 2 * height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics2D = newImage.createGraphics();
        graphics2D.drawImage(image, 0, 0, width, height, 0, 0, width, height, null);
        addEntry(name, newImage);
        return true;
    }

    private boolean convertEyes(ZipEntry entry) {
        String name = entry.getName();
        BufferedImage image = getImage(name);
        if (image == null) {
            return false;
        }
        int width = image.getWidth();
        int height = image.getHeight();
        int count = 0;
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                int argb = image.getRGB(i, j);
                if ((argb & 0xff000000) == 0 && argb != 0) {
                    image.setRGB(i, j, 0);
                    count++;
                }
            }
        }
        if (count > 0) {
            addMessage(0, "%s -> %s (fixed %d transparent pixels)", name, name, count);
            addEntry(name, image);
        }
        return true;
    }

    private boolean convertCompass(ZipEntry entry) {
        String name = entry.getName();
        BufferedImage image = getImage(name);
        if (image == null) {
            return false;
        }
        Properties oldProperties = getProperties(COMPASS_PROPERTIES);
        if (oldProperties == null) {
            oldProperties = new Properties();
        }
        Properties newProperties = new Properties();
        if (getImage(COMPASS_BASE_PNG) == null) {
            newProperties.setProperty("source.0", DEFAULT_COMPASS_PNG);
        } else {
            newProperties.setProperty("source.0", COMPASS_BASE_PNG);
        }
        String prefix = (MCPatcherUtils.getBooleanProperty(oldProperties, "filter", false) ? "%blur%" : "");
        newProperties.setProperty("source.1", prefix + COMPASS_DIAL_PNG);
        newProperties.setProperty("scaleX.1", oldProperties.getProperty("scaleX", "1.0"));
        newProperties.setProperty("scaleY.1", oldProperties.getProperty("scaleY", "0.5"));
        newProperties.setProperty("offsetX.1", oldProperties.getProperty("offsetX", "" + (0.5f / image.getWidth())));
        newProperties.setProperty("offsetY.1", oldProperties.getProperty("offsetY", "" + (-0.5f / image.getHeight())));
        newProperties.setProperty("rotationSpeed.1", "1.0");
        newProperties.setProperty("rotationOffset.1", "180.0");
        if (getImage(COMPASS_OVERLAY_PNG) != null) {
            newProperties.setProperty("source.2", COMPASS_OVERLAY_PNG);
        }
        addEntry(COMPASS_PROPERTIES, newProperties);
        return true;
    }

    private boolean convertDefaultCTM(ZipEntry entry) {
        String name = entry.getName();
        Properties properties = new Properties();

        properties.clear();
        properties.setProperty("source", name);
        properties.setProperty("method", "glass");
        properties.setProperty("connect", "block");
        properties.setProperty("blockIDs", "" + BLOCK_ID_GLASS);
        convertCTM(entry, name, "ctm/default", "glass.properties", properties);

        properties.clear();
        properties.setProperty("source", name);
        properties.setProperty("method", "glass");
        properties.setProperty("connect", "block");
        properties.setProperty("blockIDs", "" + BLOCK_ID_GLASS_PANE);
        convertCTM(entry, name, "ctm/default", "glasspane.properties", properties);

        properties.clear();
        properties.setProperty("source", name);
        properties.setProperty("method", "bookshelf");
        properties.setProperty("connect", "block");
        properties.setProperty("blockIDs", "" + BLOCK_ID_BOOKSHELF);
        convertCTM(entry, name, "ctm/default", "bookshelf.properties", properties);

        properties.clear();
        properties.setProperty("source", name);
        properties.setProperty("method", "sandstone");
        properties.setProperty("connect", "tile");
        properties.setProperty("tileIDs", "" + TILE_NUM_SANDSTONE_SIDE);
        properties.setProperty("metadata", "0");
        convertCTM(entry, name, "ctm/default", "sandstone.properties", properties);

        removeEntry(name);
        return true;
    }

    private boolean convertCTM(ZipEntry entry) {
        String name = entry.getName();
        String newName = name.replaceAll(".*/", "");
        Properties properties = getProperties(name);
        if (properties == null) {
            return false;
        }
        String source = properties.getProperty("source", "");
        if (source.equals("")) {
            addMessage(0, "%s already converted", name);
            return false;
        }
        String subdir = source.replaceFirst("\\.png$", "").replaceFirst("^/", "");
        Matcher m = CTM_TERRAIN_PROPERTIES.matcher(newName);
        if (m.find()) {
            try {
                int id = Integer.parseInt(m.group(1));
                if (id >= 0 && id < TileMapping.BLOCKS.length && TileMapping.BLOCKS[id] != null) {
                    String suffix = m.group(2);
                    newName = TileMapping.BLOCKS[id] + (suffix.equals("") ? "" : "_" + suffix) + ".properties";
                    if (!suffix.equals("") && properties.getProperty("tileIDs", "").trim().equals("")) {
                        properties.setProperty("tileIDs", "" + id);
                    }
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        return convertCTM(entry, name, subdir, newName, properties);
    }

    private boolean convertCTM(ZipEntry entry, String name, String subdir, String newName, Properties properties) {
        String source = getEntryName(properties.getProperty("source", "").trim());
        if (source.equals("")) {
            addMessage(0, "%s already converted", name);
            return false;
        }

        String tileList = properties.getProperty("tiles", "").trim();
        if (tileList.equals("")) {
            String method = properties.getProperty("method", "ctm").trim().toLowerCase();
            if (method.equals("default") || method.equals("glass") || method.equals("ctm")) {
                tileList = "0-11 16-27 32-43 48-58";
            } else if (method.equals("random")) {
                // no default
            } else if (method.equals("fixed") || method.equals("static")) {
                // no default
            } else if (method.equals("bookshelf") || method.equals("horizontal")) {
                tileList = "12-15";
            } else if (method.equals("vertical")) {
                // no default
            } else if (method.equals("sandstone") || method.equals("top")) {
                tileList = "66";
            } else if (method.equals("repeat") || method.equals("pattern")) {
                // no default
            } else {
                addMessage(1, "WARNING: %s has invalid method %s", name, method);
                return false;
            }
            properties.setProperty("tiles", tileList);
        }
        int[] tiles = MCPatcherUtils.parseIntegerList(tileList, 0, 255);
        if (tiles.length == 0) {
            addMessage(1, "WARNING: %s has no tiles defined", name);
            return false;
        }

        BufferedImage image = getImage(source);
        if (image == null) {
            addMessage(1, "WARNING: %s source texture %s not found", name, source);
            return false;
        }
        ctmSources.put(source, image);
        int width = image.getWidth();
        int height = image.getHeight();
        int tileWidth = width / 16;
        int tileHeight = height / 16;
        BufferedImage subImage = new BufferedImage(tileWidth, tileHeight, BufferedImage.TYPE_INT_ARGB);
        int[] rgb = new int[tileWidth * tileHeight];
        for (int i = 0; i < tiles.length; i++) {
            int j = tiles[i];
            image.getRGB((j % 16) * tileWidth, (j / 16) * tileHeight, tileWidth, tileHeight, rgb, 0, tileWidth);
            subImage.setRGB(0, 0, tileWidth, tileHeight, rgb, 0, tileWidth);
            String tileName = subdir + "/" + j + ".png";
            addEntry(tileName, subImage);
            addMessage(0, "%s %d,%d -> %s", source, j % 16, j / 16, tileName);
            ctmTiles.put(tileName, source + "#" + j);
        }

        int[] tileIDs = MCPatcherUtils.parseIntegerList(properties.getProperty("tileIDs", ""), 0, 255);
        StringBuilder sb = new StringBuilder();
        for (int i : tileIDs) {
            String s = TileMapping.BLOCKS[i];
            if (s != null) {
                sb.append(' ').append(s);
            }
        }
        properties.remove("tileIDs");
        if (sb.length() > 0) {
            properties.setProperty("matchTiles", sb.toString().trim());
        }

        String blockIDs = properties.getProperty("blockIDs", "").trim();
        properties.remove("blockIDs");
        if (!blockIDs.equals("")) {
            properties.setProperty("matchBlocks", blockIDs);
        }

        removeEntry(name);
        removeEntry(source);
        properties.remove("source");
        String comment = String.format(
            " Converted from /%s and /%s\n" +
                " Individual tiles are in /%s",
            name, source, subdir
        );
        addEntry(subdir + "/" + newName, properties, comment);
        addMessage(0, "%s -> %s", name, newName);
        return true;
    }

    private boolean convertCTMAnimation(ZipEntry entry) {
        String name = entry.getName();
        Properties properties = getProperties(name);
        if (properties == null) {
            return false;
        }
        String to = getEntryName(properties.getProperty("to", ""));
        BufferedImage ctmImage = ctmSources.get(to);
        if (ctmImage == null) {
            return false;
        }
        int tileWidth = ctmImage.getWidth() / 16;
        int tileHeight = ctmImage.getHeight() / 16;

        int tileX;
        int tileY;
        int tileW;
        int tileH;
        int w;
        try {
            int x = Integer.parseInt(properties.getProperty("x", ""));
            int y = Integer.parseInt(properties.getProperty("y", ""));
            w = Integer.parseInt(properties.getProperty("w", ""));
            int h = Integer.parseInt(properties.getProperty("h", ""));
            if (x % tileWidth != 0 || y % tileHeight != 0 || w % tileWidth != 0 || h % tileHeight != 0) {
                addMessage(1, "WARNING: cannot convert ctm animation %s not on tile boundaries", name);
                return false;
            }
            tileX = x / tileWidth;
            tileY = y / tileHeight;
            tileW = w / tileWidth;
            tileH = h / tileHeight;
        } catch (NumberFormatException e) {
            return false;
        }

        String from = getEntryName(properties.getProperty("from", ""));
        BufferedImage fromImage = getImage(from);
        if (fromImage == null) {
            addMessage(1, "WARNING: animation %s source %s not found", name, from);
            return false;
        }
        if (w != fromImage.getWidth()) {
            BufferedImage newImage = new BufferedImage(w, fromImage.getHeight() * w / fromImage.getWidth(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics2D = newImage.createGraphics();
            graphics2D.drawImage(fromImage, 0, 0, null);
            fromImage = newImage;
        }
        int numTiles = fromImage.getHeight() / fromImage.getWidth();

        BufferedImage newImage = new BufferedImage(ctmImage.getWidth() / 16, ctmImage.getHeight() / 16 * numTiles, BufferedImage.TYPE_INT_ARGB);
        int[] rgb = new int[newImage.getWidth() * newImage.getHeight()];

        int match = 0;
        for (int x = 0; x < tileW; x++) {
            for (int y = 0; y < tileH; y++) {
                int animTile = 16 * (tileY + y) + tileX + x;
                for (Map.Entry<String, String> e : ctmTiles.entrySet()) {
                    String convertedTileName = e.getKey();
                    String origCTMName = e.getValue();
                    int pos = origCTMName.lastIndexOf('#');
                    if (pos <= 0) {
                        continue;
                    }
                    int ctmTile;
                    try {
                        ctmTile = Integer.parseInt(origCTMName.substring(pos + 1));
                    } catch (NumberFormatException e1) {
                        e1.printStackTrace();
                        continue;
                    }
                    origCTMName = origCTMName.substring(0, pos);
                    if (!origCTMName.equals(to) || animTile != ctmTile) {
                        continue;
                    }
                    for (int i = 0; i < numTiles; i++) {
                        fromImage.getRGB(x * tileWidth, (y + i * tileH) * tileHeight, tileWidth, tileHeight, rgb, 0, tileWidth);
                        newImage.setRGB(0, i * tileHeight, tileWidth, tileHeight, rgb, 0, tileWidth);
                    }
                    addMessage(0, "%s %+d,%+d -> %s (%d frame ctm animation)",
                        from, x * tileWidth, y * tileHeight, convertedTileName, numTiles
                    );
                    addEntry(convertedTileName, newImage);
                    convertAnimationProperties(name, properties, convertedTileName.replaceAll("\\.png$", ".txt"), 1, true, numTiles);
                    removeEntry(from);
                    match++;
                }
            }
        }

        return match > 0;
    }

    private static String getEntryName(String name) {
        return name.replaceFirst("^/", "");
    }

    private Properties getProperties(String name) {
        ZipEntry entry = inZip.getEntry(getEntryName(name));
        if (entry != null) {
            InputStream inputStream = null;
            try {
                inputStream = inZip.getInputStream(entry);
                Properties properties = new Properties();
                properties.load(inputStream);
                return properties;
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                MCPatcherUtils.close(inputStream);
            }
        }
        return null;
    }

    private BufferedImage getImage(String name) {
        ZipEntry entry = inZip.getEntry(getEntryName(name));
        if (entry != null) {
            InputStream inputStream = null;
            try {
                inputStream = inZip.getInputStream(entry);
                return ImageIO.read(inputStream);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                MCPatcherUtils.close(inputStream);
            }
        }
        return null;
    }

    private void copyEntry(ZipEntry in) {
        copyEntry(in, in.getName());
    }

    private void copyEntry(ZipEntry in, String outName) {
        InputStream inputStream = null;
        OutputStream outputStream = getOutputStream(outName);
        try {
            inputStream = inZip.getInputStream(in);
            byte[] buffer = new byte[1024];
            while (true) {
                int count = inputStream.read(buffer);
                if (count < 0) {
                    break;
                }
                outputStream.write(buffer, 0, count);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            MCPatcherUtils.close(inputStream);
            MCPatcherUtils.close(outputStream);
        }
    }

    private void addDirectory(String name) {
        outData.put(getEntryName(name), null);
    }

    private void removeEntry(String name) {
        outData.remove(getEntryName(name));
    }

    private void addEntry(String name, Properties properties) {
        addEntry(name, properties, null);
    }

    private void addEntry(String name, Properties properties, String comment) {
        OutputStream outputStream = getOutputStream(name);
        try {
            properties.store(outputStream, comment);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            MCPatcherUtils.close(outputStream);
        }
    }

    private void addEntry(String name, BufferedImage image) {
        OutputStream outputStream = getOutputStream(name);
        try {
            ImageIO.write(image, "png", outputStream);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            MCPatcherUtils.close(outputStream);
        }
    }

    private OutputStream getOutputStream(String name) {
        name = name.replaceFirst("^/", "");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outData.put(name, outputStream);
        return outputStream;
    }

    private void addMessage(int level, String format, Object... params) {
        String message = String.format(format, params);
        Logger.log(Logger.LOG_JAR, "%s", message);
        if (level > 0) {
            messages.add(message);
        }
    }
}
