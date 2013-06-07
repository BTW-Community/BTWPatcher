package com.prupe.mcpatcher;

import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;

import java.io.*;
import java.util.*;
import java.util.jar.JarException;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;

abstract class MinecraftInstallation {
    static MinecraftInstallation v1;
    static MinecraftInstallation v2;

    protected final File baseDir;

    static void setBaseDir(File baseDir) {
        v1 = new MinecraftInstallationV1(baseDir);
        v2 = new MinecraftInstallationV2(baseDir);
    }

    static MinecraftJar openMinecraftJar(File file) throws IOException {
        boolean v1Present = v1.isPresent();
        boolean v2Present = v2.isPresent();
        boolean haveV1 = v1Present && v1.canHaveThisFile(file);
        boolean haveV2 = v2Present && v2.canHaveThisFile(file);
        if (haveV1 && !haveV2) {
            return v1.openMinecraftJarImpl(file);
        } else if (!haveV1 && haveV2) {
            return v2.openMinecraftJarImpl(file);
        } else if (v1Present || v2Present) {
            MinecraftVersion version = null;
            if (v1Present) {
                version = v1.getVersionFromFilename(file);
            }
            if (version == null && v2Present) {
                version = v2.getVersionFromFilename(file);
            }
            if (version != null) {
                if (v1Present && v1.prefersThisVersion(version)) {
                    return v1.openMinecraftJarImpl(file);
                } else if (v2Present && v2.prefersThisVersion(version)) {
                    return v2.openMinecraftJarImpl(file);
                }
            }
        }
        return null;
    }

    static File getJarPathForVersion(String versionString) {
        MinecraftVersion version = MinecraftVersion.parseVersion(versionString);
        if (version == null) {
            version = MinecraftVersion.parseShortVersion(versionString);
        }
        if (version == null) {
            return null;
        }
        File jar1 = v1.isPresent() ? v1.getJarPathForVersionImpl(version) : null;
        File jar2 = v2.isPresent() ? v2.getJarPathForVersionImpl(version) : null;
        if (jar1 != null && jar1.isFile() && jar2 != null && jar2.isFile()) {
            return v1.prefersThisVersion(version) ? jar1 : jar2;
        } else if (jar1 != null && jar1.isFile()) {
            return jar1;
        } else if (jar2 != null && jar2.isFile()) {
            return jar2;
        } else {
            return null;
        }
    }

    static File getDefaultJarPath() {
        File jar;
        if (v2.isPresent()) {
            jar = v2.getDefaultJarPathImpl();
            if (jar != null && jar.isFile()) {
                return jar;
            }
        }
        if (v1.isPresent()) {
            jar = v1.getDefaultJarPathImpl();
            if (jar != null && jar.isFile()) {
                return jar;
            }
        }
        return v2.baseDir;
    }

    static File getDefaultJarDirectory() {
        File dir = getDefaultJarPath();
        return dir.isDirectory() ? dir : dir.getParentFile();
    }

    static boolean isGarbageFile(String filename) {
        return filename.startsWith("META-INF") || filename.startsWith("__MACOSX") || filename.endsWith(".DS_Store") || filename.equals("mod.properties");
    }

    static boolean isClassFile(String filename) {
        return filename.endsWith(".class") && !isGarbageFile(filename);
    }

    static void setDefaultTexturePack() {
        File input = MCPatcherUtils.getMinecraftPath("options.txt");
        if (!input.exists()) {
            return;
        }
        File output = MCPatcherUtils.getMinecraftPath("options.txt.tmp");
        BufferedReader br = null;
        PrintWriter pw = null;
        try {
            br = new BufferedReader(new FileReader(input));
            pw = new PrintWriter(new FileWriter(output));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("skin:")) {
                    line = "skin:Default";
                }
                pw.println(line);
            }
        } catch (IOException e) {
            Logger.log(e);
        } finally {
            MCPatcherUtils.close(br);
            MCPatcherUtils.close(pw);
        }
        try {
            Util.copyFile(output, input);
            output.delete();
        } catch (IOException e) {
            Logger.log(e);
        }
    }

    protected MinecraftInstallation(File baseDir) {
        this.baseDir = baseDir.getAbsoluteFile();
    }

    abstract boolean isPresent();

    abstract boolean canHaveThisFile(File file);

    abstract boolean prefersThisVersion(MinecraftVersion version);

    abstract File getDefaultJarPathImpl();

    abstract File getPatchedInstallation(MinecraftVersion version);

    abstract boolean deletePatchedInstallation(MinecraftVersion version);

    abstract MinecraftVersion getVersionFromFilename(File file);

    abstract File getJarPathForVersionImpl(MinecraftVersion version);

    abstract MinecraftJar openMinecraftJarImpl(File file) throws IOException;

    static class Info {
        static final int MISSING_JAR = 0;
        static final int IO_ERROR = 1;
        static final int CORRUPT_JAR = 2;
        static final int MODDED_JAR = 3;
        static final int MODDED_OR_UNMODDED_JAR = 4;
        static final int UNMODDED_JAR = 5;

        MinecraftVersion version;
        String md5;
        String origMD5;
        int result;
        IOException exception;

        Info(File minecraftJar, MinecraftVersion version) {
            this.version = version;
            result = initialize(minecraftJar);
            if (!isOk() && exception == null) {
                exception = new IOException("unexpected error opening " + minecraftJar.getPath());
            }
        }

        private int initialize(File minecraftJar) {
            if (!minecraftJar.exists()) {
                exception = new FileNotFoundException(minecraftJar.getPath() + " does not exist");
                return MISSING_JAR;
            }

            md5 = Util.computeMD5(minecraftJar);
            if (md5 == null) {
                exception = new IOException("could not open " + minecraftJar.getPath());
                return IO_ERROR;
            }

            boolean haveMetaInf = false;
            boolean havePatcherProperties = false;
            JarFile jar = null;
            try {
                HashSet<String> entries = new HashSet<String>();
                jar = new JarFile(minecraftJar);
                for (ZipEntry entry : Collections.list(jar.entries())) {
                    String name = entry.getName();
                    if (entries.contains(name)) {
                        exception = new ZipException("duplicate zip entry " + name);
                        return CORRUPT_JAR;
                    }
                    if (name.startsWith("META-INF")) {
                        haveMetaInf = true;
                    } else if (name.equals("mcpatcher.properties")) {
                        havePatcherProperties = true;
                    }
                    entries.add(name);
                }
                if (version == null) {
                    version = extractVersion(jar, md5);
                }
            } catch (ZipException e) {
                exception = e;
                return CORRUPT_JAR;
            } catch (IOException e) {
                exception = e;
                return IO_ERROR;
            } finally {
                MCPatcherUtils.close(jar);
            }

            if (version == null) {
                exception = new JarException("Could not determine version of " + minecraftJar.getPath());
                return CORRUPT_JAR;
            }
            origMD5 = getOrigMD5(minecraftJar.getParentFile(), version);
            if (!haveMetaInf || havePatcherProperties) {
                return MODDED_JAR;
            }
            if (origMD5 == null) {
                return MODDED_OR_UNMODDED_JAR;
            }
            if (MinecraftVersion.isKnownMD5(md5)) {
                return UNMODDED_JAR;
            }
            if (version.isNewerThanAnyKnownVersion()) {
                return MODDED_OR_UNMODDED_JAR;
            }
            if (origMD5.equals(md5)) {
                return UNMODDED_JAR;
            }
            return MODDED_JAR;
        }

        static MinecraftVersion extractVersion(File file, String md5) {
            if (!file.exists()) {
                return null;
            }
            JarFile jar = null;
            try {
                jar = new JarFile(file);
                return extractVersion(jar, md5);
            } catch (Exception e) {
                Logger.log(e);
            } finally {
                MCPatcherUtils.close(jar);
            }
            return null;
        }

        private static MinecraftVersion extractVersion(JarFile jar, String md5) {
            MinecraftVersion version = extractVersion(jar);
            if (version != null) {
                return version;
            }
            InputStream inputStream = null;
            try {
                ZipEntry entry = jar.getEntry("net/minecraft/client/Minecraft.class");
                if (entry == null) {
                    return null;
                }
                inputStream = jar.getInputStream(entry);
                ClassFile classFile = new ClassFile(new DataInputStream(inputStream));
                ConstPool constPool = classFile.getConstPool();
                for (int i = 1; i < constPool.getSize(); i++) {
                    if (constPool.getTag(i) == ConstPool.CONST_String) {
                        String value = constPool.getStringInfo(i);
                        version = MinecraftVersion.parseVersion(value);
                        if (version != null) {
                            version = version.getOverrideVersion(md5);
                            break;
                        }
                    }
                }
            } catch (IOException e) {
                Logger.log(e);
            } finally {
                MCPatcherUtils.close(inputStream);
            }
            return version;
        }

        private static MinecraftVersion extractVersion(JarFile jar) {
            MinecraftVersion version = null;
            InputStream inputStream = null;
            try {
                ZipEntry entry = jar.getEntry("mcpatcher.properties");
                if (entry != null) {
                    inputStream = jar.getInputStream(entry);
                    Properties properties = new Properties();
                    properties.load(inputStream);
                    String value = properties.getProperty("minecraftVersion", "");
                    if (!value.equals("")) {
                        return MinecraftVersion.parseShortVersion(value);
                    }
                }
            } catch (IOException e) {
                Logger.log(e);
            } finally {
                MCPatcherUtils.close(inputStream);
            }
            return version;
        }

        private static String getOrigMD5(File binDir, MinecraftVersion version) {
            String md5 = MinecraftVersion.knownMD5s.get(version.getVersionString());
            if (md5 != null) {
                return md5;
            }
            File md5File = new File(binDir, "md5s");
            if (!version.isPrerelease() && md5File.exists()) {
                FileInputStream inputStream = null;
                try {
                    Properties properties = new Properties();
                    inputStream = new FileInputStream(md5File);
                    properties.load(inputStream);
                    return properties.getProperty("minecraft.jar");
                } catch (IOException e) {
                    Logger.log(e);
                } finally {
                    MCPatcherUtils.close(inputStream);
                }
            }
            return null;
        }

        boolean isOk() {
            return result > CORRUPT_JAR;
        }
    }

    abstract class MinecraftJar {
        protected File origFile;
        protected File outputFile;
        protected Info info;
        private JarFile origJar;
        private JarOutputStream outputJar;

        abstract File getInputJarDirectory();

        abstract File getOutputJarDirectory();

        abstract File getInputJarPath(MinecraftVersion version);

        abstract File getOutputJarPath(MinecraftVersion version);

        abstract File getNativesDirectory();

        abstract void addToClassPath(List<File> classPath);

        abstract void addGameParams(List<String> params);

        @Override
        protected void finalize() throws Throwable {
            closeStreams();
            super.finalize();
        }

        MinecraftVersion getVersion() {
            return info.version;
        }

        boolean isModded() {
            return info.result == Info.MODDED_JAR;
        }

        void logVersion() {
            Logger.log(Logger.LOG_MAIN, "Minecraft version is %s (md5 %s)", info.version, info.md5);
            if (info.origMD5 == null) {
                Logger.log(Logger.LOG_MAIN, "WARNING: could not determine original md5 sum");
            } else if (info.result == Info.MODDED_JAR) {
                Logger.log(Logger.LOG_MAIN, "WARNING: possibly modded minecraft.jar (orig md5 %s)", info.origMD5);
            }
        }

        String getMainClass() {
            return getVersion().compareTo("13w16a") < 0 ? "net.minecraft.client.Minecraft" : "net.minecraft.client.main.Main";
        }

        void createBackup() throws IOException {
            closeStreams();
        }

        void restoreBackup() throws IOException {
            closeStreams();
            if (origFile.exists()) {
                Util.copyFile(origFile, outputFile);
            }
        }

        void setOutputFile(File file) {
            outputFile = file;
            closeStreams();
        }

        JarFile getInputJar() throws IOException {
            if (origJar == null) {
                origJar = new JarFile(origFile, false);
            }
            return origJar;
        }

        JarOutputStream getOutputJar() throws IOException {
            if (outputJar == null) {
                outputJar = new JarOutputStream(new FileOutputStream(outputFile));
            }
            return outputJar;
        }

        File getInputFile() {
            return origFile;
        }

        File getOutputFile() {
            return outputFile;
        }

        void writeProperties(Properties properties) throws IOException {
            switch (info.result) {
                case Info.UNMODDED_JAR:
                    properties.setProperty(Config.TAG_PRE_PATCH_STATE, "unmodded");
                    break;

                case Info.MODDED_JAR:
                    properties.setProperty(Config.TAG_PRE_PATCH_STATE, "modded");
                    break;

                default:
                    break;
            }
            try {
                outputJar.putNextEntry(new ZipEntry(Config.MCPATCHER_PROPERTIES));
                properties.store(outputJar, null);
            } catch (IOException e) {
                if (!e.toString().contains("duplicate entry")) {
                    throw e;
                }
            }
        }

        void checkOutput() throws Exception {
            closeStreams();
            JarFile jar = null;
            try {
                jar = new JarFile(outputFile);
            } finally {
                MCPatcherUtils.close(jar);
            }
        }

        void closeStreams() {
            MCPatcherUtils.close(origJar);
            MCPatcherUtils.close(outputJar);
            origJar = null;
            outputJar = null;
        }

        void run() {
            File file = getOutputFile();
            StringBuilder sb = new StringBuilder();
            List<File> classPath = new ArrayList<File>();
            addToClassPath(classPath);
            classPath.add(outputFile);
            for (File f : classPath) {
                sb.append(f.getPath()).append(File.pathSeparatorChar);
            }

            List<String> params = new ArrayList<String>();
            params.add("java");
            params.add("-cp");
            params.add(sb.toString());
            params.add("-Djava.library.path=" + getNativesDirectory().getPath());
            int heapSize = Config.getInt(Config.TAG_JAVA_HEAP_SIZE, 1024);
            if (heapSize > 0) {
                params.add("-Xmx" + heapSize + "M");
                params.add("-Xms" + Math.min(heapSize, 512) + "M");
            }
            int directSize = Config.getInt(Config.TAG_DIRECT_MEMORY_SIZE, 0);
            if (directSize > 0) {
                params.add("-XX:MaxDirectMemorySize=" + directSize + "M");
            }
            params.add(getMainClass());
            addGameParams(params);

            ProcessBuilder pb = new ProcessBuilder(params.toArray(new String[params.size()]));
            pb.redirectErrorStream(true);
            pb.directory(MCPatcherUtils.getMinecraftPath());

            Logger.log(Logger.LOG_MAIN);
            Logger.log(Logger.LOG_MAIN, "Launching %s", file.getPath());
            sb = new StringBuilder();
            for (String s : pb.command()) {
                if (sb.length() > 0) {
                    sb.append(' ');
                }
                if (s.contains(" ")) {
                    sb.append('"');
                    sb.append(s);
                    sb.append('"');
                } else {
                    sb.append(s);
                }
            }
            Logger.log(Logger.LOG_MAIN, "%s", sb.toString());

            try {
                Process p = pb.start();
                if (p != null) {
                    BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
                    Pattern stackDump = Pattern.compile("^\tat ([a-z]+)\\.\\w+\\(\\S+:\\d+\\)$");
                    HashMap<String, String> reverseMap = MCPatcher.modList.getReverseMap();
                    String line;
                    while ((line = input.readLine()) != null) {
                        MCPatcher.checkInterrupt();
                        Matcher matcher = stackDump.matcher(line);
                        if (matcher.find()) {
                            String obfName = matcher.group(1);
                            String deobfName = reverseMap.get(obfName);
                            if (deobfName != null && !deobfName.equals(obfName)) {
                                line += " [" + deobfName + "]";
                            }
                        }
                        Logger.log(Logger.LOG_MAIN, "%s", line);
                    }
                    p.waitFor();
                    if (p.exitValue() != 0) {
                        Logger.log(Logger.LOG_MAIN, "Minecraft exited with status %d", p.exitValue());
                    }
                }
            } catch (InterruptedException e) {
            } catch (IOException e) {
                Logger.log(e);
            }
        }
    }
}
