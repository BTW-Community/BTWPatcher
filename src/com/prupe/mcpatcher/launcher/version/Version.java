package com.prupe.mcpatcher.launcher.version;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.prupe.mcpatcher.*;

import java.io.*;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipFile;

public class Version implements Comparable<Version> {
    private static final String BASE_URL = "http://s3.amazonaws.com/Minecraft.Download/versions/";

    private static final String TAG_ID = "id";
    private static final String TAG_TIME = "time";
    private static final String TAG_RELEASE_TIME = "releaseTime";

    private static final String LEGACY = "legacy";
    private static final String LEGACY_VALUE = "${auth_player_name} ${auth_session}";

    private static final String USERNAME_SESSION = "username_session";
    private static final String USERNAME_SESSION_VALUE = "--username ${auth_player_name} --session ${auth_session}";

    private static final String USERNAME_SESSION_VERSION = "username_session_version";
    private static final String USERNAME_SESSION_VERSION_VALUE = "--username ${auth_player_name} --session ${auth_session} --version ${version_name}";

    private static final DateFormat[] DATE_FORMATS = new DateFormat[]{
        new SimpleDateFormat("yyyy-MM-dd\'T\'HH:mm:ssZ"),
        DateFormat.getDateTimeInstance(2, 2, Locale.US),
    };

    String id;
    String type = "release";
    String processArguments = USERNAME_SESSION_VERSION;
    String minecraftArguments = "";
    String mainClass = "net.minecraft.client.main.Main";
    List<Library> libraries = new ArrayList<Library>();

    public static Version getLocalVersion(String id) {
        InputStream input = null;
        File local = getJarPath(id);
        if (!local.isFile()) {
            return null;
        }
        try {
            fetchJson(id, false);
            local = getJsonPath(id);
            if (!local.isFile()) {
                return null;
            }
            input = new FileInputStream(local);
            InputStreamReader reader = new InputStreamReader(input);
            return JsonUtils.newGson().fromJson(reader, Version.class);
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        } finally {
            MCPatcherUtils.close(input);
        }
    }

    public static boolean fetchJson(String id, boolean forceRemote) {
        return JsonUtils.fetchURL(getJsonURL(id), getJsonPath(id), forceRemote, JsonUtils.LONG_TIMEOUT, JsonUtils.JSON_SIGNATURE);
    }

    public static File getJsonPath(String id) {
        return MCPatcherUtils.getMinecraftPath("versions", id, id + ".json");
    }

    public static URL getJsonURL(String id) {
        return JsonUtils.newURL(BASE_URL + id + "/" + id + ".json");
    }

    public static boolean fetchJar(String id, boolean forceRemote) {
        return JsonUtils.fetchURL(getJarURL(id), getJarPath(id), forceRemote, JsonUtils.LONG_TIMEOUT, JsonUtils.JAR_SIGNATURE);
    }

    public static File getJarPath(String id) {
        return MCPatcherUtils.getMinecraftPath("versions", id, id + ".jar");
    }

    public static URL getJarURL(String id) {
        return JsonUtils.newURL(BASE_URL + id + "/" + id + ".jar");
    }

    public static boolean deleteLocalFiles(String id) {
        File baseDir = getJsonPath(id).getParentFile();
        File[] list = baseDir.listFiles();
        if (list != null) {
            for (File f : list) {
                if (f.isDirectory()) {
                    File[] list1 = baseDir.listFiles();
                    if (list1 != null) {
                        for (File f1 : list1) {
                            f1.delete();
                        }
                    }
                }
                f.delete();
            }
        }
        baseDir.delete();
        return !baseDir.exists();
    }

    private Version() {
    }

    @Override
    public String toString() {
        return String.format("Version{%s, %s}", type, id);
    }

    public String getId() {
        return id;
    }

    public boolean isSnapshot() {
        return "snapshot".equals(type);
    }

    public File getJsonPath() {
        return getJsonPath(id);
    }

    public File getJarPath() {
        return getJarPath(id);
    }

    public List<Library> getLibraries() {
        return libraries;
    }

    public boolean isComplete() {
        return getJsonPath().isFile() && getJarPath().isFile();
    }

    public boolean isPatched() {
        File jar = getJarPath();
        ZipFile zip = null;
        try {
            zip = new ZipFile(jar);
            if (zip.getEntry(Config.MCPATCHER_PROPERTIES) != null) {
                return true;
            }
        } catch (IOException e) {
            Logger.log(e);
        } finally {
            MCPatcherUtils.close(zip);
        }
        return false;
    }

    public Version copyToNewVersion(JsonObject base, String newid) {
        JsonObject json = JsonUtils.parseJson(getJsonPath());
        if (json == null) {
            return null;
        }
        if (base != null) {
            for (Map.Entry<String, JsonElement> entry : base.entrySet()) {
                json.add(entry.getKey(), entry.getValue());
            }
        }
        File outPath = getJsonPath(newid);
        json.addProperty(TAG_ID, newid);
        updateDateField(json, TAG_TIME);
        updateDateField(json, TAG_RELEASE_TIME);
        outPath.getParentFile().mkdirs();
        if (!JsonUtils.writeJson(json, outPath)) {
            return null;
        }
        return getLocalVersion(newid);
    }

    private static void updateDateField(JsonObject json, String field) {
        JsonElement element = json.get(field);
        if (!element.isJsonPrimitive()) {
            return;
        }
        String oldValue = element.getAsString();
        if (MCPatcherUtils.isNullOrEmpty(oldValue)) {
            return;
        }
        for (DateFormat format : DATE_FORMATS) {
            try {
                String newValue = changeDate(format, oldValue);
                json.addProperty(field, newValue);
                return;
            } catch (ParseException e) {
                // continue
            }
        }
    }

    private static String changeDate(DateFormat format, String oldValue) throws ParseException {
        oldValue = oldValue.replaceFirst("(\\d\\d):(\\d\\d)$", "$1$2");
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(format.parse(oldValue));
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.SECOND, -1);
        return DATE_FORMATS[0].format(calendar.getTime()).replaceFirst("(\\d\\d)(\\d\\d)$", "$1:$2");
    }

    public void addToClassPath(File libDir, List<File> jars) {
        if (libraries != null) {
            for (Library l : libraries) {
                l.addToClassPath(libDir, jars);
            }
        }
    }

    public void unpackNatives(File libDir, File destDir) {
        if (libraries != null && !libraries.isEmpty()) {
            destDir.mkdirs();
            for (Library l : libraries) {
                l.unpackNatives(libDir, destDir);
            }
        }
    }

    public void fetchLibraries(File libDir) {
        if (!MCPatcherUtils.isNullOrEmpty(libraries)) {
            for (Library l : libraries) {
                if (!l.exclude()) {
                    l.fetch(libDir);
                }
            }
        }
    }

    public void setGameArguments(Map<String, String> args) {
        args.put("version_name", id);
        args.put("game_assets", MCPatcherUtils.getMinecraftPath("assets").getPath());
    }

    public void addGameArguments(Map<String, String> args, List<String> cmdLine) {
        String argTemplate;
        if (!minecraftArguments.isEmpty()) {
            argTemplate = minecraftArguments;
        } else if (LEGACY.equalsIgnoreCase(processArguments)) {
            argTemplate = LEGACY_VALUE;
        } else if (USERNAME_SESSION.equalsIgnoreCase(processArguments)) {
            argTemplate = USERNAME_SESSION_VALUE;
        } else {
            argTemplate = USERNAME_SESSION_VERSION_VALUE;
        }
        for (String s : argTemplate.split("\\s+")) {
            if (s.equals("")) {
                // nothing
            } else if (s.startsWith("${") && s.endsWith("}")) {
                String value = args.get(s.substring(2, s.length() - 1));
                cmdLine.add(value == null ? "" : value);
            } else {
                cmdLine.add(s);
            }
        }
    }

    public String getMainClass() {
        return mainClass;
    }

    @Override
    public int compareTo(Version o) {
        MinecraftVersion v1 = MinecraftVersion.parseVersion(getId());
        MinecraftVersion v2 = MinecraftVersion.parseVersion(o.getId());
        if (v1 != null && v2 != null) {
            return v1.compareTo(v2);
        } else if (v1 != null) {
            return 1;
        } else if (v2 != null) {
            return -1;
        } else {
            return getId().compareTo(o.getId());
        }
    }
}
