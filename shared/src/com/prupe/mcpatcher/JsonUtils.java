package com.prupe.mcpatcher;

import com.google.gson.*;

import java.io.*;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

public class JsonUtils {
    static Proxy proxy = Proxy.NO_PROXY;

    public static final byte[] JSON_SIGNATURE = "{".getBytes();
    public static final byte[] JAR_SIGNATURE = "PK".getBytes();

    public static final int SHORT_TIMEOUT = 6000;
    public static final int LONG_TIMEOUT = 30000;

    public static Gson newGson() {
        GsonBuilder builder = new GsonBuilder();
        builder.setPrettyPrinting();
        return builder.create();
    }

    public static URL newURL(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean fetchURL(URL url, File local, boolean forceRemote, int timeoutMS, byte[] signature) {
        boolean success = true;
        if (forceRemote || !local.isFile() || local.length() <= 0) {
            BufferedInputStream input = null;
            OutputStream output = null;
            try {
                System.out.printf("Fetching %s...\n", url);
                URLConnection connection = url.openConnection(proxy);
                connection.setConnectTimeout(timeoutMS);
                connection.setReadTimeout(timeoutMS / 2);
                input = new BufferedInputStream(connection.getInputStream());
                if (checkSignature(input, signature)) {
                    output = new FileOutputStream(local);
                    copyStream(input, output);
                }
            } catch (IOException e) {
                e.printStackTrace();
                success = false;
            } finally {
                MCPatcherUtils.close(input);
                MCPatcherUtils.close(output);
            }
        }
        if (success && checkSignature(local, signature)) {
            return true;
        } else {
            local.delete();
            return false;
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

    public static <T> T parseJson(File path, Class<T> cl) {
        if (path == null || !path.isFile() || path.length() <= 0) {
            return null;
        }
        InputStream input = null;
        try {
            input = new FileInputStream(path);
            InputStreamReader reader = new InputStreamReader(input);
            return JsonUtils.newGson().fromJson(reader, cl);
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        } finally {
            MCPatcherUtils.close(input);
        }
    }

    public static JsonObject parseJson(File path) {
        Reader input = null;
        try {
            input = new FileReader(path);
            JsonParser parser = new JsonParser();
            return parser.parse(input).getAsJsonObject();
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        } finally {
            MCPatcherUtils.close(input);
        }
    }

    public static boolean writeJson(JsonElement json, File path) {
        PrintWriter output = null;
        try {
            output = new PrintWriter(path);
            Gson gson = newGson();
            gson.toJson(json, output);
            output.println();
            return true;
        } catch (Throwable e) {
            e.printStackTrace();
            MCPatcherUtils.close(output);
            path.delete();
            return false;
        } finally {
            MCPatcherUtils.close(output);
        }
    }

    public static boolean writeJson(Object object, File path) {
        PrintWriter output = null;
        try {
            output = new PrintWriter(path);
            Gson gson = newGson();
            gson.toJson(object, object.getClass(), output);
            output.println();
            return true;
        } catch (Throwable e) {
            e.printStackTrace();
            MCPatcherUtils.close(output);
            path.delete();
            return false;
        } finally {
            MCPatcherUtils.close(output);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends JsonElement> T cloneJson(T json) {
        return (T) new JsonParser().parse(json.toString());
    }

    public static <T> T cloneJson(T json, Class<T> jsonClass) {
        Gson gson = newGson();
        return gson.fromJson(gson.toJson(json, jsonClass), jsonClass);
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

    private JsonUtils() {
    }
}
