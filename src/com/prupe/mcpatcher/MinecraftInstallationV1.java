package com.prupe.mcpatcher;

import java.io.File;
import java.io.IOException;
import java.util.List;

class MinecraftInstallationV1 extends MinecraftInstallation {
    private final File binDir;

    MinecraftInstallationV1(File baseDir) {
        super(baseDir);
        this.binDir = new File(baseDir, "bin");
    }

    @Override
    boolean isPresent() {
        return false;
    }

    @Override
    boolean canHaveThisFile(File file) {
        try {
            return file.getParentFile().getName().equals("bin");
        } catch (Throwable e) {
            return false;
        }
    }

    @Override
    boolean prefersThisVersion(MinecraftVersion version) {
        return version.compareTo("1.5.2") <= 0;
    }

    @Override
    File getDefaultJarPathImpl() {
        return new File(binDir, "minecraft.jar");
    }

    @Override
    File getPatchedInstallation(MinecraftVersion version) {
        return getJarPathForVersionImpl(version);
    }

    @Override
    boolean deletePatchedInstallation(MinecraftVersion version) {
        return getPatchedInstallation(version).delete();
    }

    @Override
    MinecraftVersion getVersionFromFilename(File file) {
        Info info = new Info(file, null);
        return info.isOk() ? info.version : null;
    }

    @Override
    File getJarPathForVersionImpl(MinecraftVersion version) {
        return new File(binDir, "minecraft-" + version.getVersionString() + ".jar");
    }

    @Override
    MinecraftJar openMinecraftJarImpl(File file) throws IOException {
        return new MinecraftJarV1(file);
    }

    class MinecraftJarV1 extends MinecraftJar {
        MinecraftJarV1(File file) throws IOException {
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
        File getInputJarDirectory() {
            return binDir;
        }

        @Override
        File getOutputJarDirectory() {
            return binDir;
        }

        @Override
        File getInputJarPath(MinecraftVersion version) {
            return new File(binDir, "minecraft-" + version.getVersionString() + ".jar");
        }

        @Override
        File getOutputJarPath(MinecraftVersion version) {
            return new File(binDir, "minecraft.jar");
        }

        @Override
        File getNativesDirectory() {
            return new File(binDir, "natives");
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
}
