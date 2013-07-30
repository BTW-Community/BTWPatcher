package com.prupe.mcpatcher.launcher.version;

class OS {
    private String name;
    private String version;

    boolean match() {
        if (name == null || name.isEmpty()) {
            return true;
        }
        if (!name.equals(Library.OS_TYPE)) {
            return false;
        }
        if (version == null || version.isEmpty()) {
            return true;
        }
        return Library.OS_VERSION.matches(version);
    }
}
