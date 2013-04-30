package com.prupe.mcpatcher;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

class MinecraftInstallationV2 extends MinecraftInstallation {
    static final String MCPATCHER_SUFFIX = "-mcpatcher";
    private static final String ORIGINAL_SUFFIX = "-original";
    private static final String NATIVES_SUFFIX = "-natives";

    private final File versionsDir;
    private final File librariesDir;

    MinecraftInstallationV2(File baseDir) {
        super(baseDir);
        versionsDir = new File(baseDir, "versions");
        librariesDir = new File(baseDir, "libraries");
    }

    @Override
    boolean isPresent() {
        return versionsDir.isDirectory() && librariesDir.isDirectory();
    }

    @Override
    boolean canHaveThisFile(File file) {
        try {
            return file.getParentFile().getParentFile().getName().equals("versions");
        } catch (Throwable e) {
            return false;
        }
    }

    @Override
    boolean prefersThisVersion(MinecraftVersion version) {
        return version.compareTo("1.5.2") > 0;
    }

    @Override
    File getDefaultJarPathImpl() {
        MinecraftVersion version = getCurrentVersion();
        if (version == null) {
            return null;
        }
        String v = version.getVersionString();
        return new File(new File(versionsDir, v), v + ".jar");
    }

    @Override
    File getPatchedInstallation(MinecraftVersion version) {
        return new File(versionsDir, version.getVersionString() + MCPATCHER_SUFFIX);
    }

    @Override
    boolean deletePatchedInstallation(MinecraftVersion version) {
        File dir = getPatchedInstallation(version);
        String v = version.getVersionString() + MCPATCHER_SUFFIX;
        File natives = new File(dir, v + NATIVES_SUFFIX);
        File[] files = natives.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isFile()) {
                    f.delete();
                }
            }
        }
        natives.delete();
        new File(dir, v + ".jar").delete();
        new File(dir, v + ".json").delete();
        return dir.delete();
    }

    private MinecraftVersion getCurrentVersion() {
        File[] versions = versionsDir.listFiles();
        if (versions != null) {
            List<MinecraftVersion> availableVersions = new ArrayList<MinecraftVersion>();
            for (File d : versions) {
                if (!d.getName().endsWith(MCPATCHER_SUFFIX)) {
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
                return availableVersions.get(0);
            }
        }
        return null;
    }

    @Override
    MinecraftVersion getVersionFromFilename(File file) {
        String versionString = file.getName().replaceFirst("(" + MCPATCHER_SUFFIX + "|" + ORIGINAL_SUFFIX + ")*\\.jar$", "");
        return MinecraftVersion.parseShortVersion(versionString);
    }

    @Override
    File getJarPathForVersionImpl(MinecraftVersion version) {
        String v = version.getVersionString();
        return new File(new File(versionsDir, v), v + ".jar");
    }

    @Override
    MinecraftJar openMinecraftJarImpl(File file) throws IOException {
        return new MinecraftJarV2(file);
    }

    private void createVersionDirectory(File oldDir, File newDir) throws IOException {
        String oldVersion = oldDir.getName();
        String newVersion = newDir.getName();
        newDir.mkdirs();
        BufferedReader reader = null;
        PrintWriter writer = null;
        try {
            reader = new BufferedReader(new FileReader(new File(oldDir, oldVersion + ".json")));
            writer = new PrintWriter(new FileWriter(new File(newDir, newVersion + ".json")));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("\"id\":")) {
                    line = line.replace(oldVersion, newVersion);
                }
                writer.println(line);
            }
        } finally {
            MCPatcherUtils.close(reader);
            MCPatcherUtils.close(writer);
        }
        oldDir = new File(oldDir, oldVersion + NATIVES_SUFFIX);
        newDir = new File(newDir, newVersion + NATIVES_SUFFIX);
        File[] natives = oldDir.listFiles();
        if (natives != null) {
            newDir.mkdirs();
            for (File f : natives) {
                Util.copyFile(f, new File(newDir, f.getName()));
            }
        }
    }

    class MinecraftJarV2 extends MinecraftJar {
        MinecraftJarV2(File file) throws IOException {
            MinecraftVersion version = getVersionFromFilename(file);
            info = new Info(file, version);
            if (!info.isOk()) {
                throw info.exception;
            }

            outputFile = getOutputJarPath(version);
            origFile = getInputJarPath(version);
            if (!origFile.equals(file)) {
                info = new Info(origFile, version);
                if (!info.isOk()) {
                    throw info.exception;
                }
            }
            if (info.result == Info.MODDED_JAR) {
                File tmp = new File(origFile.getParent(), origFile.getName().replaceFirst("\\.jar$", ORIGINAL_SUFFIX + ".jar"));
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
                createVersionDirectory(origFile.getParentFile(), outputFile.getParentFile());
            }
        }

        @Override
        File getInputJarDirectory() {
            return new File(versionsDir, getVersion().getVersionString());
        }

        @Override
        File getOutputJarDirectory() {
            return new File(versionsDir, getVersion().getVersionString() + MCPATCHER_SUFFIX);
        }

        @Override
        File getInputJarPath(MinecraftVersion version) {
            return new File(getInputJarDirectory(), version.getVersionString() + ".jar");
        }

        @Override
        File getOutputJarPath(MinecraftVersion version) {
            return new File(getOutputJarDirectory(), version.getVersionString() + MCPATCHER_SUFFIX + ".jar");
        }

        @Override
        File getNativesDirectory() {
            return new File(getOutputJarDirectory(), getOutputJarDirectory().getName() + NATIVES_SUFFIX);
        }

        @Override
        void addToClassPath(List<File> classPath) {
            if (!getClassPathFromJSON(classPath)) {
                if (librariesDir.isDirectory()) {
                    addAllToClassPath(librariesDir, classPath);
                }
            }
        }

        private boolean getClassPathFromJSON(List<File> classPath) {
            BufferedReader reader = null;
            try {
                File json = new File(getOutputJarDirectory(), getVersion().getVersionString() + MCPATCHER_SUFFIX + ".json");
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
                        // org.lwjgl.lwjgl:lwjgl:2.9.0 ->
                        //   libraries/        (base library dir)
                        //   org/lwjgl/lwjgl/  (package)
                        //   lwjgl/            (library name)
                        //   2.9.0/            (library version)
                        //   lwjgl-2.9.0.jar   (<library>-<version>.jar)
                        String subdir = tokens[2].replace('.', '/');
                        String base = tokens[3];
                        String version = tokens[4];
                        File jar = new File(new File(new File(new File(librariesDir, subdir), base), version), base + '-' + version + ".jar");
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

        private void addAllToClassPath(File dir, List<File> classPath) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isFile() && f.getName().endsWith(".jar")) {
                        classPath.add(f);
                    } else if (f.isDirectory()) {
                        addAllToClassPath(f, classPath);
                    }
                }
            }
        }
    }
}
