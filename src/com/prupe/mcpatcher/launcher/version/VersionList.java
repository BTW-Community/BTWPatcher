package com.prupe.mcpatcher.launcher.version;

import com.google.gson.Gson;
import com.prupe.mcpatcher.Config;
import com.prupe.mcpatcher.JsonUtils;
import com.prupe.mcpatcher.Logger;
import com.prupe.mcpatcher.MCPatcherUtils;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VersionList {
    public static final URL VERSION_LIST = JsonUtils.newURL("https://s3.amazonaws.com/Minecraft.Download/versions/versions.json");

    private static final URL[] VERSIONS_URLS = new URL[]{
        VERSION_LIST,
        JsonUtils.newURL(VERSION_LIST.toString().replaceFirst("^https", "http")),
    };
    private static final byte[] JSON_SIGNATURE = "{".getBytes();

    List<Version> versions = new ArrayList<Version>();
    LatestVersion latest;

    public static VersionList getVersionList(boolean remote) {
        File local = MCPatcherUtils.getMinecraftPath("versions", Config.VERSIONS_JSON);
        for (URL url : VERSIONS_URLS) {
            if (JsonUtils.fetchURL(url, local, remote, JSON_SIGNATURE)) {
                break;
            }
        }
        if (!local.isFile()) {
            InputStream inputStream = null;
            OutputStream outputStream = null;
            try {
                Logger.log(Logger.LOG_MAIN, "WARNING: using included copy of versions.json, may be out of date");
                inputStream = VersionList.class.getResourceAsStream("/resources/versions.json");
                outputStream = new FileOutputStream(local);
                JsonUtils.copyStream(inputStream, outputStream);
            } catch (IOException e) {
                Logger.log(e);
            } finally {
                MCPatcherUtils.close(inputStream);
                MCPatcherUtils.close(outputStream);
            }
        }
        return JsonUtils.parseJson(local, VersionList.class);
    }

    private VersionList() {
    }

    @Override
    public String toString() {
        return String.format("VersionList{latest=%s, %d versions}", latest, versions.size());
    }

    public Version getLatestVersion(boolean release, boolean snapshot, boolean local) {
        String relVersion = latest == null ? null : latest.release;
        String ssVersion = latest == null ? null : latest.snapshot;
        List<Version> tmpList = new ArrayList<Version>();
        tmpList.addAll(versions);
        Collections.reverse(tmpList);
        for (Version v : tmpList) {
            if (local && !v.getJsonPath().isFile()) {
                continue;
            }
            if (release && v.getId().equals(relVersion)) {
                return v;
            }
            if (snapshot && v.getId().equals(ssVersion)) {
                return v;
            }
        }
        return null;
    }

    public List<Version> getVersions() {
        return versions;
    }

    public void dump(PrintStream output) {
        output.println(toString());
        for (Version v : versions) {
            output.println(v.toString());
        }
        Gson gson = JsonUtils.newGson();
        gson.toJson(this, VersionList.class, output);
        output.println();
    }
}
