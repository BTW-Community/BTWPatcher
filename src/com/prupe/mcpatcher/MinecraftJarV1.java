package com.prupe.mcpatcher;

import java.io.File;
import java.io.IOException;

class MinecraftJarV1 extends MinecraftJarBase {
    static boolean isThisVersion(File file) {
        return file.getParentFile().getName().equals("bin");
    }

    MinecraftJarV1(File file) throws IOException {
        super(file);

        info = new Info(file);
        if (!info.isOk()) {
            throw info.exception;
        }

        if (file.getName().equals("minecraft.jar")) {
            origFile = new File(file.getParent(), "minecraft-" + info.version.getVersionString() + ".jar");
            outputFile = file;
            Info origInfo = new Info(origFile);
            if (origInfo.result == Info.MODDED_JAR && info.result == Info.UNMODDED_JAR) {
                Logger.log(Logger.LOG_JAR, "copying unmodded %s over %s", outputFile.getName(), origFile.getName());
                origFile.delete();
            }
            if (!origFile.exists()) {
                createBackup();
            } else if (origInfo.isOk()) {
                info = origInfo;
            }
        } else {
            origFile = file;
            outputFile = new File(file.getParent(), "minecraft.jar");
        }
    }

    File getOutputJarPath(MinecraftVersion version) {
        return MCPatcherUtils.getMinecraftPath("bin", "minecraft.jar");
    }

    File getInputJarPath(MinecraftVersion version) {
        return MCPatcherUtils.getMinecraftPath("bin", "minecraft-" + version.getVersionString() + ".jar");
    }
}
