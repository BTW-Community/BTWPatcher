package com.prupe.mcpatcher;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
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
    File getJarDirectory() {
        String v = getVersion().getVersionString();
        return MCPatcherUtils.getMinecraftPath("versions", v);
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
    File getNativesDirectory() {
        return new File(getJarDirectory(), getVersion().getVersionString() + "-natives");
    }

    @Override
    void addToClassPath(List<File> classPath) {
        if (!getClassPathFromJSON(classPath)) {
            File libDir = MCPatcherUtils.getMinecraftPath("libraries");
            if (libDir.isDirectory()) {
                addToClassPath(libDir, classPath);
            }
        }
    }

    private boolean getClassPathFromJSON(List<File> classPath) {
        BufferedReader reader = null;
        try {
            File json = new File(getJarDirectory(), getVersion().getVersionString() + ".json");
            if (!json.isFile()) {
                Logger.log(Logger.LOG_JAR, "WARNING: %s not found", json);
                return false;
            }
            reader = new BufferedReader(new FileReader(json));
            boolean foundLibs = false;
            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split("[ \t:\"]+");
                if (tokens.length >= 2 && tokens[1].equals("libraries")) {
                    foundLibs = true;
                } else if (foundLibs && tokens.length >= 5 && tokens[1].equals("name")) {
                    String subdir = tokens[2].replace('.', '/');
                    String base = tokens[3];
                    String version = tokens[4];
                    File jar = MCPatcherUtils.getMinecraftPath("libraries", subdir, base, version, base + '-' + version + ".jar");
                    if (jar.isFile()) {
                        classPath.add(jar);
                    } else {
                        Logger.log(Logger.LOG_JAR, "WARNING: %s not found", jar);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            MCPatcherUtils.close(reader);
        }
        return true;
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
