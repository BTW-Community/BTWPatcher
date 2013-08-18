package com.prupe.mcpatcher;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

class LegacyVersionList {
    static final String VERSIONS_JSON = "mcpatcher-legacy.json";
    static final URL VERSIONS_URL = Util.newURL("https://bitbucket.org/prupe/mcpatcher-legacy/raw/master/" + VERSIONS_JSON);
    static final String DEFAULT_BASE_URL = "https://bitbucket.org/prupe/mcpatcher-legacy/downloads/";

    int format = 1;
    List<Entry> versions = new ArrayList<Entry>();

    void add(Entry entry) {
        versions.add(entry);
    }

    static class Entry {
        String id;
        String maxMinecraftVersion;
        String libraryVersion;
        String md5;
        String baseURL;
        List<Mod> mods = new ArrayList<Mod>();

        private Entry() {
        }

        Entry(String id, String maxMinecraftVersion, String libraryVersion) {
            this.id = id;
            this.maxMinecraftVersion = maxMinecraftVersion;
            this.libraryVersion = libraryVersion;
        }

        String getBaseURL() {
            return MCPatcherUtils.isNullOrEmpty(baseURL) ? DEFAULT_BASE_URL : baseURL;
        }

        URL getURL() {
            return Util.newURL(getBaseURL() + "mcpatcher-legacy-" + libraryVersion + ".jar");
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
