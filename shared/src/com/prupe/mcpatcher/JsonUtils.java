package com.prupe.mcpatcher;

import com.google.gson.*;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

public class JsonUtils {
    public static Gson newGson() {
        GsonBuilder builder = new GsonBuilder();
        builder.setPrettyPrinting();
        return builder.create();
    }

    public static <T> T parseJson(File path, Class<T> cl) {
        if (path == null || !path.isFile() || path.length() <= 0) {
            return null;
        }
        InputStream input = null;
        try {
            input = new FileInputStream(path);
            return parseJson(input, cl);
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        } finally {
            MCPatcherUtils.close(input);
        }
    }

    public static <T> T parseJson(InputStream input, Class<T> cl) {
        if (input == null) {
            return null;
        }
        try {
            InputStreamReader reader = new InputStreamReader(input);
            return JsonUtils.newGson().fromJson(reader, cl);
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
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

    private JsonUtils() {
    }
}
