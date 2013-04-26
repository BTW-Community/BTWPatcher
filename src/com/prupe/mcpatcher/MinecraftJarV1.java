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

    static File getInstallation(MinecraftVersion version) {
        if (version == null) {
            return null;
        }
        File jar = MCPatcherUtils.getMinecraftPath("bin", "minecraft-" + version.getVersionString() + ".jar");
        return jar.isFile() ? jar : null;
    }

    static boolean deleteInstallation(MinecraftVersion version) {
        File jar = getInstallation(version);
        return jar != null && jar.delete();
    }

    private MinecraftJarV1(File file) throws IOException {
        MinecraftVersion version = getVersionFromFilename(file);
        Info tmpInfo = new Info(file, version);
        if (!tmpInfo.isOk()) {
            throw tmpInfo.exception;
        }
        version = tmpInfo.version;

        outputFile = getOutputJarPath(version);
        origFile = getInputJarPath(version);

        if (!origFile.exists()) {
            createBackup();
        }

        info = file.equals(origFile) ? tmpInfo : new Info(origFile, version);
        if (!info.isOk()) {
            throw info.exception;
        }

        Info outputInfo = file.equals(outputFile) ? tmpInfo : new Info(outputFile, version);
        if (!outputInfo.isOk()) {
            throw outputInfo.exception;
        }

        if (info.result == Info.MODDED_JAR && outputInfo.result == Info.UNMODDED_JAR) {
            Logger.log(Logger.LOG_JAR, "copying unmodded %s over %s", outputFile.getName(), origFile.getName());
            origFile.delete();
            createBackup();
            info = outputInfo;
        }
    }

    @Override
    MinecraftVersion getVersionFromFilename(File file) {
        return null;
    }

    @Override
    File getInputJarDirectory() {
        return MCPatcherUtils.getMinecraftPath("bin");
    }

    @Override
    File getOutputJarDirectory() {
        return MCPatcherUtils.getMinecraftPath("bin");
    }

    @Override
    File getInputJarPath(MinecraftVersion version) {
        return new File(getInputJarDirectory(), "minecraft-" + version.getVersionString() + ".jar");
    }

    @Override
    File getOutputJarPath(MinecraftVersion version) {
        return new File(getOutputJarDirectory(), "minecraft.jar");
    }

    @Override
    File getNativesDirectory() {
        return new File(getOutputJarDirectory(), "natives");
    }

    @Override
    void addToClassPath(List<File> classPath) {
        classPath.add(new File(getInputJarDirectory(), "lwjgl.jar"));
        classPath.add(new File(getInputJarDirectory(), "lwjgl_util.jar"));
        classPath.add(new File(getInputJarDirectory(), "jinput.jar"));
    }

    @Override
    void createBackup() throws IOException {
        super.createBackup();
        if (outputFile.exists() && !origFile.exists()) {
            Util.copyFile(outputFile, origFile);
        }
    }

    @Override
    void restoreBackup() throws IOException {
        super.restoreBackup();
        if (origFile.exists()) {
            Util.copyFile(origFile, outputFile);
        }
    }
}
