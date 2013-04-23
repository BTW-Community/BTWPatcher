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
    File getJarDirectory() {
        return MCPatcherUtils.getMinecraftPath("bin");
    }

    @Override
    File getOutputJarPath(MinecraftVersion version) {
        return new File(getJarDirectory(), "minecraft.jar");
    }

    @Override
    File getInputJarPath(MinecraftVersion version) {
        return new File(getJarDirectory(), "minecraft-" + version.getVersionString() + ".jar");
    }

    @Override
    File getNativesDirectory() {
        return new File(getJarDirectory(), "natives");
    }

    @Override
    void addToClassPath(List<File> classPath) {
        classPath.add(new File(getJarDirectory(), "lwjgl.jar"));
        classPath.add(new File(getJarDirectory(), "lwjgl_util.jar"));
        classPath.add(new File(getJarDirectory(), "jinput.jar"));
    }

    @Override
    String getMainClass() {
        return "net.minecraft.client.Minecraft";
    }
}
