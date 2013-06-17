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
    protected final List<ZipEntry> inEntries = new ArrayList<ZipEntry>();

    private ZipOutputStream outZip;
    private final Map<String, ByteArrayOutputStream> outData = new HashMap<String, ByteArrayOutputStream>();
    private final List<String> messages = new ArrayList<String>();

    protected TexturePackConverter(File input) {
        this.input = input;
    }

    public List<String> getMessages() {
        return messages;
    }

    public File getOutputFile() {
        return output;
    }

    final public boolean convert(UserInterface ui) {
        if (this.input.equals(output)) {
            addMessage(2, "ERROR: Input and output files are the same");
            return false;
        }
        try {
            preConvert(ui);
            convertImpl(ui);
            postConvert(ui);
        } catch (Throwable e) {
            e.printStackTrace();
            addMessage(2, "ERROR: %s", e);
            MCPatcherUtils.close(outZip);
            if (output.isFile()) {
                addMessage(0, "deleting %s", output.getPath());
                output.delete();
            }
            return false;
        } finally {
            MCPatcherUtils.close(inZip);
            MCPatcherUtils.close(outZip);
        }
        return true;
    }

    private void preConvert(UserInterface ui) throws IOException {
        messages.clear();
        addMessage(0, "");
        addMessage(0, "Converting texture pack");
        ui.setStatusText("Reading %s...", input.getName());
        inZip = new ZipFile(input);
        inEntries.clear();
        for (ZipEntry entry : Collections.list(inZip.entries())) {
            if (!entry.isDirectory()) {
                inEntries.add(entry);
            }
        }
        Collections.sort(inEntries, new Comparator<ZipEntry>() {
            public int compare(ZipEntry o1, ZipEntry o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        addMessage(0, "  input:  %s", input.getPath());
        addMessage(0, "    %d bytes", input.length());
        addMessage(0, "    %d files", inEntries.size());
        addMessage(0, "  output: %s", output.getPath());
    }

    private void postConvert(UserInterface ui) throws IOException {
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
        int total = outEntries.size();
        int progress = 0;
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
    }

    abstract protected void convertImpl(UserInterface ui) throws Exception;

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

    protected boolean hasEntry(String name) {
        return outData.containsKey(getEntryName(name));
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

    protected void copyEntry(String from, String to) {
        ByteArrayOutputStream data = outData.get(getEntryName(from));
        if (data == null) {
            outData.remove(getEntryName(to));
        } else {
            outData.put(getEntryName(to), data);
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
