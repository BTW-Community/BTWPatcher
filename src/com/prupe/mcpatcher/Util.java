package com.prupe.mcpatcher;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.*;

public class Util {
    public static final byte[] JSON_SIGNATURE = "{".getBytes();
    public static final byte[] JAR_SIGNATURE = "PK".getBytes();
    public static final int SHORT_TIMEOUT = 6000;
    public static final int LONG_TIMEOUT = 30000;

    static int bits;
    static File devDir;

    static {
        try {
            bits = Integer.parseInt(System.getProperty("sun.arch.data.model"));
        } catch (Throwable e) {
            bits = 32;
        }
        try {
            File path = new File(MCPatcher.class.getProtectionDomain().getCodeSource().getLocation().getPath());
            if (path.isDirectory()) {
                devDir = path.getParentFile().getParentFile().getParentFile();
            }
        } catch (Throwable e) {
            e.printStackTrace();
            devDir = null;
        }
    }

    static byte b(int value, int index) {
        return (byte) ((value >> (index * 8)) & 0xFF);
    }

    static byte[] marshal16(int value) {
        return new byte[]{Util.b(value, 1), Util.b(value, 0)};
    }

    static byte[] marshalString(String value) {
        byte[] bytes = value.getBytes();
        int len = bytes.length;
        ByteArrayOutputStream bos = new ByteArrayOutputStream(len + 2);
        try {
            bos.write(marshal16(len));
            bos.write(bytes);
        } catch (IOException e) {
            Logger.log(e);
        }
        return bos.toByteArray();
    }

    static int demarshal(byte[] data, int offset, int length) {
        int result = 0;
        for (int i = 0; i < length; i++) {
            result <<= 8;
            result |= data[i + offset] & 0xff;
        }
        return result;
    }

    static int demarshal(byte[] data) {
        return demarshal(data, 0, data.length);
    }

    public static void copyStream(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[1024];
        while (true) {
            int count = input.read(buffer);
            if (count < 0) {
                break;
            }
            output.write(buffer, 0, count);
        }
    }

    public static void copyFile(File input, File output) throws IOException {
        FileInputStream is = null;
        FileOutputStream os = null;
        try {
            is = new FileInputStream(input);
            os = new FileOutputStream(output);
            copyStream(is, os);
        } finally {
            MCPatcherUtils.close(is);
            MCPatcherUtils.close(os);
        }
    }

    static boolean contains(byte[] array, int item) {
        byte itemb = (byte) item;
        for (byte b : array) {
            if (itemb == b) {
                return true;
            }
        }
        return false;
    }

    static String computeMD5(File file) {
        String md5 = null;
        FileInputStream input = null;
        ByteArrayOutputStream output = null;
        DigestOutputStream dos = null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            input = new FileInputStream(file);
            output = new ByteArrayOutputStream();
            dos = new DigestOutputStream(output, md);
            copyStream(input, dos);
            MCPatcherUtils.close(dos);
            md5 = BinaryRegex.binToStr(md.digest()).replaceAll(" ", "");
        } catch (Exception e) {
            Logger.log(e);
        } finally {
            MCPatcherUtils.close(input);
            MCPatcherUtils.close(dos);
            MCPatcherUtils.close(output);
        }
        return md5;
    }

    static void logOSInfo() {
        Logger.log(Logger.LOG_MAIN, "MCPatcher version is %s", MCPatcher.VERSION_STRING);
        Logger.log(Logger.LOG_MAIN, "OS: %s %s %s",
            System.getProperty("os.name"),
            System.getProperty("os.version"),
            System.getProperty("os.arch")
        );
        Logger.log(Logger.LOG_MAIN, "JVM: %s %s (%d bit)",
            System.getProperty("java.vendor"),
            System.getProperty("java.version"),
            bits
        );
        Logger.log(Logger.LOG_MAIN, "Classpath: %s", System.getProperty("java.class.path"));
    }

    public static URL newURL(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void fetchURL(URL url, File local, boolean forceRemote, int timeoutMS, byte[] signature) throws PatcherException {
        boolean alreadyExists = checkSignature(local, signature);
        if (forceRemote || !alreadyExists) {
            BufferedInputStream input = null;
            OutputStream output = null;
            UserInterface ui = MCPatcher.ui;
            try {
                System.out.printf("Fetching %s...\n", url);
                ui.setStatusText("Downloading %s...", local.getName());
                URLConnection connection = null;
                loop:
                for (int redirect = 1; redirect <= 5; redirect++) {
                    connection = url.openConnection();
                    connection.setConnectTimeout(timeoutMS);
                    connection.setReadTimeout(timeoutMS / 2);
                    if (connection instanceof HttpURLConnection) {
                        HttpURLConnection httpConnection = (HttpURLConnection) connection;
                        httpConnection.setInstanceFollowRedirects(true);
                        httpConnection.connect();
                        switch (httpConnection.getResponseCode() / 100) {
                            case 2:
                                break loop;

                            case 3:
                                String newURL = httpConnection.getHeaderField("Location");
                                if (!MCPatcherUtils.isNullOrEmpty(newURL)) {
                                    url = new URL(newURL);
                                    if ("http".equals(url.getProtocol()) || "https".equals(url.getProtocol()) || "ftp".equals(url.getProtocol())) {
                                        System.out.printf("Redirect #%d: %s\n", redirect, url);
                                        connection = null;
                                        continue loop;
                                    }
                                }
                                // fall through

                            default:
                                throw new IOException("HTTP response code: " + httpConnection.getResponseCode());
                        }
                    }
                }
                if (connection == null) {
                    throw new IOException("Redirect limit exceeded");
                }
                input = new BufferedInputStream(connection.getInputStream());
                if (checkSignature(input, signature)) {
                    output = new FileOutputStream(local);
                    int expLen = connection.getContentLength();
                    if (expLen <= 0) {
                        copyStream(input, output);
                    } else {
                        byte[] buffer = new byte[1024];
                        int total = 0;
                        int count;
                        while ((count = input.read(buffer)) > 0) {
                            total += count;
                            ui.updateProgress(total, expLen);
                            output.write(buffer, 0, count);
                        }
                    }
                }
            } catch (IOException e) {
                throw new PatcherException.DownloadException(e, url, local);
            } finally {
                MCPatcherUtils.close(input);
                MCPatcherUtils.close(output);
                ui.updateProgress(0, 0);
            }
        }
        if (!checkSignature(local, signature)) {
            if (!alreadyExists) {
                local.delete();
            }
            throw new PatcherException.DownloadException(url, local);
        }
    }

    public static boolean checkSignature(File file, byte[] signature) {
        if (signature == null || signature.length <= 0) {
            return true;
        }
        if (!file.isFile()) {
            return false;
        }
        BufferedInputStream input = null;
        try {
            input = new BufferedInputStream(new FileInputStream(file));
            return checkSignature(input, signature);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            MCPatcherUtils.close(input);
        }
    }

    public static boolean checkSignature(BufferedInputStream input, byte[] signature) throws IOException {
        if (signature != null && signature.length > 0 && input.markSupported()) {
            try {
                input.mark(signature.length);
                byte[] header = new byte[signature.length];
                int count = input.read(header);
                return count == header.length && Arrays.equals(signature, header);
            } finally {
                input.reset();
            }
        }
        return true;
    }

    public static <K, V> void reorderMap(LinkedHashMap<K, V> map, int fromIndex, int toIndex) {
        List<K> keys = new ArrayList<K>();
        keys.addAll(map.keySet());
        K target = keys.remove(fromIndex);
        if (toIndex >= keys.size()) {
            keys.add(target);
        } else if (toIndex >= 0) {
            keys.add(toIndex, target);
        }
        LinkedHashMap<K, V> newMap = new LinkedHashMap<K, V>();
        for (K key : keys) {
            newMap.put(target, map.get(key));
        }
        map.clear();
        map.putAll(newMap);
    }

    public static <K, V> void sortMap(LinkedHashMap<K, V> map, Comparator<Map.Entry<K, V>> comparator) {
        List<Map.Entry<K, V>> entries = new ArrayList<Map.Entry<K, V>>();
        entries.addAll(map.entrySet());
        Collections.sort(entries, comparator);
        map.clear();
        for (Map.Entry<K, V> entry : entries) {
            map.put(entry.getKey(), entry.getValue());
        }
    }
}
