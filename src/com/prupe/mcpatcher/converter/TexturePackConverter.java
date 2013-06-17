package com.prupe.mcpatcher.converter;

import com.prupe.mcpatcher.Logger;
import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.UserInterface;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

abstract public class TexturePackConverter {
    protected final File input;
    protected File output;
    protected ZipFile inZip;
    protected ZipOutputStream outZip;
    protected final Map<String, ByteArrayOutputStream> outData = new HashMap<String, ByteArrayOutputStream>();
    protected final List<String> messages = new ArrayList<String>();

    protected TexturePackConverter(File input) {
        this.input = input;
    }

    public List<String> getMessages() {
        return messages;
    }

    public File getOutputFile() {
        return output;
    }

    abstract public boolean convert(UserInterface ui);

    protected static String getEntryName(String name) {
        return name.replaceFirst("^/", "");
    }

    protected Properties getProperties(String name) {
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

    protected BufferedImage getImage(String name) {
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

    protected void copyEntry(ZipEntry in) {
        copyEntry(in, in.getName());
    }

    protected void copyEntry(ZipEntry in, String outName) {
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

    protected void addDirectory(String name) {
        outData.put(getEntryName(name), null);
    }

    protected void removeEntry(String name) {
        outData.remove(getEntryName(name));
    }

    protected void addEntry(String name, Properties properties) {
        addEntry(name, properties, null);
    }

    protected void addEntry(String name, Properties properties, String comment) {
        OutputStream outputStream = getOutputStream(name);
        try {
            properties.store(outputStream, comment);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            MCPatcherUtils.close(outputStream);
        }
    }

    protected void addEntry(String name, BufferedImage image) {
        OutputStream outputStream = getOutputStream(name);
        try {
            ImageIO.write(image, "png", outputStream);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            MCPatcherUtils.close(outputStream);
        }
    }

    protected OutputStream getOutputStream(String name) {
        name = name.replaceFirst("^/", "");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outData.put(name, outputStream);
        return outputStream;
    }

    protected void addMessage(int level, String format, Object... params) {
        String message = String.format(format, params);
        Logger.log(Logger.LOG_JAR, "%s", message);
        if (level > 0) {
            messages.add(message);
        }
    }
}
