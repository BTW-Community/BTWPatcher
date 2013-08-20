package com.prupe.mcpatcher;

import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class LegacyVersionList {
    static final String VERSIONS_JSON = "mcpatcher-legacy.json";
    static final URL VERSIONS_URL = Util.newURL("https://bitbucket.org/prupe/mcpatcher-legacy/raw/master/" + VERSIONS_JSON);
    static final String DEFAULT_BASE_URL = "https://bitbucket.org/prupe/mcpatcher-legacy/downloads/";

    int format = 1;
    LinkedHashMap<String, Version> versions = new LinkedHashMap<String, Version>();

    void add(Version version) {
        versions.put(version.getKey(), version);
    }

    List<Version> find(String api) {
        List<Version> list = new ArrayList<Version>();
        for (Map.Entry<String, Version> entry : versions.entrySet()) {
            if (entry.getValue().api.equals(api)) {
                list.add(entry.getValue());
            }
        }
        return list;
    }

    static class Version {
        String id;
        String api;
        String maxMinecraftVersion;
        String libraryVersion;
        String md5;
        String baseURL;
        List<Mod> mods = new ArrayList<Mod>();

        private Version() {
        }

        Version(String id, String api, String maxMinecraftVersion, String libraryVersion) {
            this.id = id;
            this.api = api;
            this.maxMinecraftVersion = maxMinecraftVersion;
            this.libraryVersion = libraryVersion;
        }

        String getBaseURL() {
            return MCPatcherUtils.isNullOrEmpty(baseURL) ? DEFAULT_BASE_URL : baseURL;
        }

        URL getURL() {
            return Util.newURL(getBaseURL() + "mcpatcher-legacy-" + libraryVersion + ".jar");
        }

        String getKey() {
            return id + "." + api;
        }

        String getResource() {
            return "/" + id + ".jar";
        }

        void add(Mod mod) {
            mods.add(mod);
        }
    }

    static class Mod {
        String name;
        String className;
        private Boolean internal;
        private Boolean experimental;

        private Mod() {
        }

        Mod(String name, String className) {
            this.name = name;
            this.className = className;
        }

        Mod setInternal(boolean internal) {
            this.internal = internal ? true : null;
            return this;
        }

        Mod setExperimental(boolean experimental) {
            this.experimental = experimental ? true : null;
            return this;
        }

        boolean isInternal() {
            return internal != null && internal;
        }

        boolean isExperimental() {
            return experimental != null && experimental;
        }
    }
}
