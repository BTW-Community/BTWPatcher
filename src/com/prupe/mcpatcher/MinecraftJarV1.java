package com.prupe.mcpatcher;

import java.io.File;
import java.io.IOException;

class MinecraftJarV1 extends MinecraftJarBase {
    static MinecraftJarBase create(File file) throws IOException {
        return isThisVersion(file) ? new MinecraftJarV1(file) : null;
    }

    static boolean isThisVersion(File file) {
        try {
            return file.getParentFile().getName().equals("bin");
        } catch (Throwable e) {
            return false;
        }
    }

    private MinecraftJarV1(File file) throws IOException {
        super(file);
    }

    MinecraftVersion getVersionFromFilename(File file) {
        return null;
    }

    File getOutputJarPath(MinecraftVersion version) {
        return MCPatcherUtils.getMinecraftPath("bin", "minecraft.jar");
    }

    File getInputJarPath(MinecraftVersion version) {
        return MCPatcherUtils.getMinecraftPath("bin", "minecraft-" + version.getVersionString() + ".jar");
    }
}
