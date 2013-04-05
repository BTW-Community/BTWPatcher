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

class MinecraftJar {
    private File origFile;
    private File outputFile;
    private Info info;
    private JarFile origJar;
    private JarOutputStream outputJar;

    MinecraftJar(File file) throws IOException {
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

    void createBackup() throws IOException {
        closeStreams();
        if (outputFile.exists() && !origFile.exists()) {
            Util.copyFile(outputFile, origFile);
        }
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
                properties.setProperty("prePatchState", "unmodded");
                break;

            case Info.MODDED_JAR:
                properties.setProperty("prePatchState", "modded");
                break;

            default:
                break;
        }
        try {
            outputJar.putNextEntry(new ZipEntry("mcpatcher.properties"));
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
        File directory = file.getParentFile();
        StringBuilder cp = new StringBuilder();
        for (String p : new String[]{file.getName(), "lwjgl.jar", "lwjgl_util.jar", "jinput.jar"}) {
            cp.append(directory.getPath());
            cp.append("/");
            cp.append(p);
            cp.append(File.pathSeparatorChar);
        }

        List<String> params = new ArrayList<String>();
        params.add("java");
        params.add("-cp");
        params.add(cp.toString());
        params.add("-Djava.library.path=" + new File(directory, "natives").getPath());
        int heapSize = Config.getInt(Config.TAG_JAVA_HEAP_SIZE, 1024);
        if (heapSize > 0) {
            params.add("-Xmx" + heapSize + "M");
            params.add("-Xms" + Math.min(heapSize, 512) + "M");
        }
        int directSize = Config.getInt(Config.TAG_DIRECT_MEMORY_SIZE, 0);
        if (directSize > 0) {
            params.add("-XX:MaxDirectMemorySize=" + directSize + "M");
        }
        params.add("net.minecraft.client.Minecraft");

        ProcessBuilder pb = new ProcessBuilder(params.toArray(new String[params.size()]));
        pb.redirectErrorStream(true);
        pb.directory(MCPatcherUtils.getMinecraftPath());

        Logger.log(Logger.LOG_MAIN);
        Logger.log(Logger.LOG_MAIN, "Launching %s", file.getPath());
        StringBuilder sb = new StringBuilder();
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

    private static class Info {
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

        Info(File minecraftJar) {
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
                version = extractVersion(jar, md5);
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

        static MinecraftVersion extractVersion(File file) {
            return extractVersion(file, Util.computeMD5(file));
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
}
