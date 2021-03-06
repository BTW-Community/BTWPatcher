package com.prupe.mcpatcher.launcher.version;

import com.google.gson.Gson;
import com.prupe.mcpatcher.*;

import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VersionList {
    public static final URL VERSION_LIST = Util.newURL("https://s3.amazonaws.com/Minecraft.Download/versions/versions.json");
    private static final URL VERSION_LIST2 = Util.newURL(VERSION_LIST.toString().replaceFirst("^https", "http"));

    List<Version> versions = new ArrayList<Version>();
    LatestVersion latest;

    public static File getPath() {
        return MCPatcherUtils.getMinecraftPath("versions", Config.VERSIONS_JSON);
    }

    public static void fetchRemoteVersionList(int timeoutMS) throws PatcherException {
        File local = getPath();
        try {
            Util.fetchURL(VERSION_LIST, local, true, timeoutMS, Util.JSON_SIGNATURE);
        } catch (PatcherException e) {
            try {
                Util.fetchURL(VERSION_LIST2, local, true, timeoutMS, Util.JSON_SIGNATURE);
            } catch (PatcherException e1) {
                throw e;
            }
        }
    }

    public static VersionList getLocalVersionList() {
        VersionList list = JsonUtils.parseJson(getPath(), VersionList.class);
        if (list != null) {
            Collections.sort(list.versions);
        }
        return list;
    }

    public static VersionList getBuiltInVersionList() {
        VersionList list = null;
        InputStream inputStream = null;
        try {
            inputStream = VersionList.class.getResourceAsStream("/resources/versions.json");
            list = JsonUtils.parseJson(inputStream, VersionList.class);
            if (list != null) {
                Collections.sort(list.versions);
            }
        } finally {
            MCPatcherUtils.close(inputStream);
        }
        return list;
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
