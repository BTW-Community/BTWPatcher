package com.prupe.mcpatcher;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

class MinecraftJarV2 extends MinecraftJarBase {
    private static final String MCPATCHER_VERSION = "-mcpatcher";

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

    static File getLatestVersion() {
        File[] versions = MCPatcherUtils.getMinecraftPath("versions").listFiles();
        if (versions != null) {
            List<MinecraftVersion> availableVersions = new ArrayList<MinecraftVersion>();
            for (File d : versions) {
                if (!d.getName().endsWith(MCPATCHER_VERSION)) {
                    MinecraftVersion version = MinecraftVersion.parseShortVersion(d.getName());
                    if (version != null) {
                        File f = new File(d, d.getName() + ".jar");
                        if (f.isFile()) {
                            availableVersions.add(version);
                        }
                    }
                }
            }
            if (!availableVersions.isEmpty()) {
                Collections.sort(availableVersions, new Comparator<MinecraftVersion>() {
                    public int compare(MinecraftVersion o1, MinecraftVersion o2) {
                        return o2.compareTo(o1);
                    }
                });
                String v = availableVersions.get(0).getVersionString();
                return MCPatcherUtils.getMinecraftPath("versions", v, v + ".jar");
            }
        }
        return null;
    }

    private MinecraftJarV2(File file) throws IOException {
        MinecraftVersion version = getVersionFromFilename(file);

        outputFile = getOutputJarPath(version);
        origFile = getInputJarPath(version);
        info = new Info(origFile, version);
        if (!info.isOk()) {
            throw info.exception;
        }
        if (info.result == Info.MODDED_JAR) {
            File tmp = new File(origFile.getParent(), version + "-original.jar");
            if (tmp.isFile()) {
                Util.copyFile(tmp, origFile);
                info = new Info(origFile, version);
                if (!info.isOk()) {
                    throw info.exception;
                }
                tmp.delete();
            }
        }

        if (!outputFile.getParentFile().isDirectory()) {
            createVersionDirectory(origFile.getParentFile(), outputFile.getParentFile(), version.getVersionString());
        }
    }

    @Override
    MinecraftVersion getVersionFromFilename(File file) {
        String versionString = file.getName().replaceFirst("(" + MCPATCHER_VERSION + "|-original)*\\.jar$", "");
        return MinecraftVersion.parseShortVersion(versionString);
    }

    @Override
    File getInputJarDirectory() {
        String v = getVersion().getVersionString();
        return MCPatcherUtils.getMinecraftPath("versions", v);
    }

    @Override
    File getOutputJarDirectory() {
        String v = getVersion().getVersionString() + MCPATCHER_VERSION;
        return MCPatcherUtils.getMinecraftPath("versions", v);
    }

    @Override
    File getInputJarPath(MinecraftVersion version) {
        String v = version.getVersionString();
        return MCPatcherUtils.getMinecraftPath("versions", v, v + ".jar");
    }

    @Override
    File getOutputJarPath(MinecraftVersion version) {
        String v = version.getVersionString() + MCPATCHER_VERSION;
        return MCPatcherUtils.getMinecraftPath("versions", v, v + ".jar");
    }

    @Override
    File getNativesDirectory() {
        return new File(getOutputJarDirectory(), getVersion().getVersionString() + MCPATCHER_VERSION + "-natives");
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
            File json = new File(getOutputJarDirectory(), getVersion().getVersionString() + MCPATCHER_VERSION + ".json");
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
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isFile() && f.getName().endsWith(".jar")) {
                    classPath.add(f);
                } else if (f.isDirectory()) {
                    addToClassPath(f, classPath);
                }
            }
        }
    }

    private void createVersionDirectory(File fromDir, File toDir, String version) throws IOException {
        toDir.mkdirs();
        BufferedReader reader = null;
        PrintWriter writer = null;
        try {
            reader = new BufferedReader(new FileReader(new File(fromDir, version + ".json")));
            writer = new PrintWriter(new FileWriter(new File(toDir, version + MCPATCHER_VERSION + ".json")));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("\"id\":")) {
                    line = line.replace(version, version + MCPATCHER_VERSION);
                }
                writer.println(line);
            }
        } finally {
            MCPatcherUtils.close(reader);
            MCPatcherUtils.close(writer);
        }
        fromDir = new File(fromDir, version + "-natives");
        toDir = new File(toDir, version + MCPATCHER_VERSION + "-natives");
        File[] natives = fromDir.listFiles();
        if (natives != null) {
            toDir.mkdirs();
            for (File f : natives) {
                Util.copyFile(f, new File(toDir, f.getName()));
            }
        }
    }
}
