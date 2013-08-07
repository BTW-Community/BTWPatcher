package com.prupe.mcpatcher;

import com.prupe.mcpatcher.launcher.profile.Profile;
import com.prupe.mcpatcher.launcher.version.Library;
import com.prupe.mcpatcher.launcher.version.Version;

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
    private final ProfileManager profileManager;
    private final File origFile;
    private final File outputFile;
    private final Info info;
    private JarFile origJar;
    private JarOutputStream outputJar;

    MinecraftJar(ProfileManager profileManager) throws IOException {
        this.profileManager = profileManager;
        origFile = profileManager.getInputJar();
        outputFile = profileManager.getOutputJar();
        info = new Info(origFile, profileManager.getInputBaseVersion());
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
        File jarFile = getOutputFile();
        Profile profile = profileManager.getOutputProfileData();
        if (profile == null) {
            Logger.log(Logger.LOG_MAIN, "Output profile '%s' unexpectedly missing", profileManager.getOutputProfile());
            return;
        }
        Version version1 = profileManager.getOutputVersionData();
        if (version1 == null || !version1.isComplete()) {
            Logger.log(Logger.LOG_MAIN, "Output version '%s' unexpectedly missing", profileManager.getOutputVersion());
            return;
        }
        List<String> cmdLine = new ArrayList<String>();
        Map<String, String> gameArgs = new HashMap<String, String>();

        File java = profile.getJavaExe();
        if (java != null && java.isFile()) {
            cmdLine.add(java.getAbsolutePath());
        } else {
            cmdLine.add("java");
        }

        if ("osx".equals(Library.getOSType())) {
            cmdLine.add("-Xdock:icon=" + MCPatcherUtils.getMinecraftPath("assets", "icons", "minecraft.icns").getPath());
            cmdLine.add("-Xdock:name=Minecraft");
        } else if ("windows".equals(Library.getOSType())) {
            cmdLine.add("-XX:HeapDumpPath=MojangTricksIntelDriversForPerformance_javaw.exe_minecraft.exe.heapdump");
        }
        if (!profile.getJavaArguments(cmdLine)) {
            String mem = "32".equals(System.getProperty("sun.arch.data.model")) ? "512M" : "1G";
            cmdLine.add("-Xmx" + mem);
        }

        profile.setGameArguments(gameArgs, profileManager.getProfileList());
        version1.setGameArguments(gameArgs);

        File libDir = MCPatcherUtils.getMinecraftPath("libraries");
        File nativesDir = new File(jarFile.getParentFile(), profileManager.getOutputVersion() + "-natives-1");
        File oldNativesDir = new File(jarFile.getParentFile(), profileManager.getOutputVersion() + "-natives");
        if (oldNativesDir.isDirectory()) {
            File[] list = oldNativesDir.listFiles();
            if (list != null) {
                for (File f : list) {
                    f.delete();
                }
            }
            oldNativesDir.delete();
        }

        version1.fetchLibraries(libDir);

        cmdLine.add("-cp");
        StringBuilder sb = new StringBuilder();
        List<File> classPath = new ArrayList<File>();
        version1.addToClassPath(libDir, classPath);
        classPath.add(jarFile);
        for (File f : classPath) {
            if (sb.length() > 0) {
                sb.append(File.pathSeparatorChar);
            }
            sb.append(f.getAbsolutePath());
        }
        cmdLine.add(sb.toString());
        version1.unpackNatives(libDir, nativesDir);
        cmdLine.add("-Djava.library.path=" + nativesDir.getPath());

        cmdLine.add(version1.getMainClass());
        profile.addGameArguments(gameArgs, cmdLine);
        version1.addGameArguments(gameArgs, cmdLine);

        ProcessBuilder pb = new ProcessBuilder(cmdLine.toArray(new String[cmdLine.size()]));
        pb.redirectErrorStream(true);
        pb.directory(profile.getGameDir());

        Logger.log(Logger.LOG_MAIN);
        Logger.log(Logger.LOG_MAIN, "Launching %s", jarFile.getPath());
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

    static class Info {
        static final int MODDED_JAR = 0;
        static final int MODDED_OR_UNMODDED_JAR = 1;
        static final int UNMODDED_JAR = 2;

        MinecraftVersion version;
        String md5;
        String origMD5;
        final int result;

        Info(File minecraftJar, String versionString) throws IOException {
            result = initialize(minecraftJar, versionString);
        }

        private int initialize(File minecraftJar, String versionString) throws IOException {
            if (!minecraftJar.isFile()) {
                throw new FileNotFoundException(minecraftJar.getPath() + " does not exist");
            }

            md5 = Util.computeMD5(minecraftJar);
            if (md5 == null) {
                throw new IOException("could not open " + minecraftJar.getPath());
            }

            if (!MCPatcherUtils.isNullOrEmpty(versionString)) {
                version = MinecraftVersion.parseVersion(versionString);
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
                        throw new ZipException("duplicate zip entry " + name);
                    }
                    if (name.startsWith("META-INF")) {
                        haveMetaInf = true;
                    } else if (name.equals("mcpatcher.properties")) {
                        havePatcherProperties = true;
                    }
                    entries.add(name);
                }
                if (version == null) {
                    version = extractVersion(minecraftJar, jar, md5);
                }
            } finally {
                MCPatcherUtils.close(jar);
            }

            if (version == null) {
                throw new JarException("Could not determine version of " + minecraftJar.getPath());
            }
            origMD5 = getOrigMD5(version);
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
            return MODDED_OR_UNMODDED_JAR;
        }

        private static MinecraftVersion extractVersion(File path, JarFile jar, String md5) {
            MinecraftVersion version = extractVersion(jar);
            if (version != null) {
                return version;
            }
            return extractVersion(path);
        }

        private static MinecraftVersion extractVersion(File path) {
            return MinecraftVersion.parseVersion(path.getParentFile().getName());
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
                        return MinecraftVersion.parseVersion(value);
                    }
                }
            } catch (IOException e) {
                Logger.log(e);
            } finally {
                MCPatcherUtils.close(inputStream);
            }
            return version;
        }

        private static String getOrigMD5(MinecraftVersion version) {
            return MinecraftVersion.knownMD5s.get(version.getVersionString());
        }
    }
}
