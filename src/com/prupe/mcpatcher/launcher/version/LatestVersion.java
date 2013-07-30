package com.prupe.mcpatcher.launcher.version;

class LatestVersion {
    String snapshot;
    String release;

    private LatestVersion() {
    }

    @Override
    public String toString() {
        return "LatestVersion{snapshot=" + snapshot + ", release=" + release + "}";
    }
}
