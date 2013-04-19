package com.prupe.mcpatcher;

import java.io.File;
import java.io.IOException;

class MinecraftJarV2 extends MinecraftJarBase {
    static MinecraftJarBase create(File file) throws IOException {
        return isThisVersion(file) ? new MinecraftJarV2(file) : null;
    }

    static boolean isThisVersion(File file) {
        try {
            return file.getParentFile().getParentFile().getName().equals("versions");
        } catch (Throwable e) {
            return false;
        }
    }

    private MinecraftJarV2(File file) throws IOException {
        super(file);
    }

    MinecraftVersion getVersionFromFilename(File file) {
        String versionString = file.getName().replaceFirst("(-original)?\\.jar$", "");
        return MinecraftVersion.parseShortVersion(versionString);
    }

    File getOutputJarPath(MinecraftVersion version) {
        String v = version.getVersionString();
        return MCPatcherUtils.getMinecraftPath("versions", v, v + ".jar");
    }

    File getInputJarPath(MinecraftVersion version) {
        String v = version.getVersionString();
        return MCPatcherUtils.getMinecraftPath("versions", v, v + "-original.jar");
    }
}
