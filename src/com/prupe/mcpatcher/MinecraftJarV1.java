package com.prupe.mcpatcher;

import java.io.File;
import java.io.IOException;
import java.util.List;

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

    @Override
    MinecraftVersion getVersionFromFilename(File file) {
        return null;
    }

    @Override
    File getOutputJarPath(MinecraftVersion version) {
        return MCPatcherUtils.getMinecraftPath("bin", "minecraft.jar");
    }

    @Override
    File getInputJarPath(MinecraftVersion version) {
        return MCPatcherUtils.getMinecraftPath("bin", "minecraft-" + version.getVersionString() + ".jar");
    }

    @Override
    File getNativesDir() {
        return MCPatcherUtils.getMinecraftPath("bin", "natives");
    }

    @Override
    void addToClassPath(List<File> classPath) {
        classPath.add(MCPatcherUtils.getMinecraftPath("bin", "lwjgl.jar"));
        classPath.add(MCPatcherUtils.getMinecraftPath("bin", "lwjgl_util.jar"));
        classPath.add(MCPatcherUtils.getMinecraftPath("bin", "jinput.jar"));
    }

    @Override
    String getMainClass() {
        return "net.minecraft.client.Minecraft";
    }
}
