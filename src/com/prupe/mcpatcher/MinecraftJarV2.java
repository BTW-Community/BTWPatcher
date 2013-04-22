package com.prupe.mcpatcher;

import java.io.File;
import java.io.IOException;
import java.util.List;

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

    @Override
    MinecraftVersion getVersionFromFilename(File file) {
        String versionString = file.getName().replaceFirst("(-original)?\\.jar$", "");
        return MinecraftVersion.parseShortVersion(versionString);
    }

    @Override
    File getOutputJarPath(MinecraftVersion version) {
        String v = version.getVersionString();
        return MCPatcherUtils.getMinecraftPath("versions", v, v + ".jar");
    }

    @Override
    File getInputJarPath(MinecraftVersion version) {
        String v = version.getVersionString();
        return MCPatcherUtils.getMinecraftPath("versions", v, v + "-original.jar");
    }

    @Override
    File getNativesDir() {
        String v = getVersion().getVersionString();
        return MCPatcherUtils.getMinecraftPath("versions", v, v + "-natives");
    }

    @Override
    void addToClassPath(List<File> classPath) {
        File libDir = MCPatcherUtils.getMinecraftPath("libraries");
        if (libDir.isDirectory()) {
            addToClassPath(libDir, classPath);
        }
    }

    private static void addToClassPath(File dir, List<File> classPath) {
        for (File f : dir.listFiles()) {
            if (f.isFile() && f.getName().endsWith(".jar")) {
                classPath.add(f);
            } else if (f.isDirectory()) {
                addToClassPath(f, classPath);
            }
        }
    }

    @Override
    String getMainClass() {
        return "net.minecraft.client.main.Main";
    }
}
