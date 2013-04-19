package com.prupe.mcpatcher;

import java.io.File;
import java.io.IOException;

class MinecraftJarV2 extends MinecraftJarBase {
    static MinecraftJarV2 create(File file) throws IOException {
        if (!isThisVersion(file)) {
            return null;
        }
        String versionString = file.getName().replaceFirst("(-original)?\\.jar$", "");
        MinecraftVersion version = MinecraftVersion.parseShortVersion(versionString);
        if (version == null) {
            return null;
        }
        return new MinecraftJarV2(file, version);
    }

    static boolean isThisVersion(File file) {
        try {
            return file.getParentFile().getParentFile().getName().equals("versions");
        } catch (Throwable e) {
            return false;
        }
    }

    protected MinecraftJarV2(File file, MinecraftVersion version) throws IOException {
        super(file);

        Info tmpInfo = new Info(file, version);
        if (!tmpInfo.isOk()) {
            throw tmpInfo.exception;
        }

        origFile = getInputJarPath(version);
        info = file.equals(origFile) ? tmpInfo : new Info(origFile, version);
        if (!info.isOk()) {
            throw info.exception;
        }

        outputFile = getOutputJarPath(version);
        Info outputInfo = file.equals(outputFile) ? tmpInfo : new Info(outputFile, version);
        if (!outputInfo.isOk()) {
            throw outputInfo.exception;
        }

        if (tmpInfo.result == Info.MODDED_JAR && outputInfo.result == Info.UNMODDED_JAR) {
            Logger.log(Logger.LOG_JAR, "copying unmodded %s over %s", outputFile.getName(), origFile.getName());
            origFile.delete();
        }
        if (!origFile.exists()) {
            createBackup();
        } else if (outputInfo.isOk()) {
            info = outputInfo;
        }
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
