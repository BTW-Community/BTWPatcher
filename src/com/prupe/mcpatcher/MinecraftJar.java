package com.prupe.mcpatcher;

import com.prupe.mcpatcher.launcher.profile.Profile;
import com.prupe.mcpatcher.launcher.version.Library;
import com.prupe.mcpatcher.launcher.version.Version;
import javassist.bytecode.ClassFile;

import java.io.*;
import java.util.*;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

class MinecraftJar {
    private final ProfileManager profileManager;
    private final File inputFile;
    private final File outputFile;
    private final String inputMD5;
    private final String origMD5;
    private final MinecraftVersion inputVersion;

    private final Map<String, ClassFile> classes = new HashMap<String, ClassFile>();
    private final Map<String, ClassEntry> classHierarchy = new HashMap<String, ClassEntry>();

    private JarFile inputJar;
    private JarOutputStream outputJar;

    MinecraftJar(ProfileManager profileManager) throws PatcherException {
        this.profileManager = profileManager;
        inputFile = profileManager.getInputJar();
        outputFile = profileManager.getOutputJar();
        inputMD5 = Util.computeMD5(inputFile);
        inputVersion = MinecraftVersion.parseVersion(profileManager.getInputBaseVersion());
        if (inputVersion == null) {
            throw new PatcherException.CorruptJarFile(inputFile);
        }
        origMD5 = MinecraftVersion.getOriginalMD5(inputVersion);
    }

    static boolean isGarbageFile(String filename) {
        return filename.startsWith("META-INF") || filename.startsWith("__MACOSX") ||
            filename.endsWith(".DS_Store") || filename.equals("mod.properties");
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
        BufferedReader reader = null;
        PrintWriter writer = null;
        try {
            reader = new BufferedReader(new FileReader(input));
            writer = new PrintWriter(new FileWriter(output));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("skin:")) {
                    line = "skin:Default";
                }
                writer.println(line);
            }
        } catch (IOException e) {
            Logger.log(e);
        } finally {
            MCPatcherUtils.close(reader);
            MCPatcherUtils.close(writer);
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
        return inputVersion;
    }

    boolean isModded() {
        return !MinecraftVersion.isKnownMD5(inputMD5) && origMD5 != null && !origMD5.equals(inputMD5);
    }

    void logVersion() {
        Logger.log(Logger.LOG_MAIN, "Minecraft version is %s (md5 %s)", inputVersion, inputMD5);
        if (inputVersion.isNewerThanAnyKnownVersion()) {
            Logger.log(Logger.LOG_MAIN, "WARNING: version is newer than any known version");
        } else if (origMD5 == null) {
            Logger.log(Logger.LOG_MAIN, "WARNING: could not determine original md5 sum");
        } else if (isModded()) {
            Logger.log(Logger.LOG_MAIN, "WARNING: possibly modded minecraft.jar (orig md5 %s)", origMD5);
        }
    }

    JarFile getInputJar() throws IOException {
        if (inputJar == null) {
            inputJar = new JarFile(inputFile, false);
        }
        return inputJar;
    }

    JarOutputStream getOutputJar() throws IOException {
        if (outputJar == null) {
            outputJar = new JarOutputStream(new FileOutputStream(outputFile));
        }
        return outputJar;
    }

    File getInputFile() {
        return inputFile;
    }

    File getOutputFile() {
        return outputFile;
    }

    ClassFile getClassFile(ZipEntry entry) throws IOException {
        String name = entry.getName();
        ClassFile classFile = classes.get(name);
        if (classFile == null) {
            InputStream input = null;
            try {
                input = getInputJar().getInputStream(entry);
                classFile = new ClassFile(new DataInputStream(input));
                classes.put(name, classFile);
                registerClassFile(classFile);
            } finally {
                MCPatcherUtils.close(input);
            }
        }
        return classFile;
    }

    void clearClassFileCache() {
        classes.clear();
    }

    private void registerClassFile(ClassFile classFile) {
        String name = classFile.getName();
        ClassEntry entry = classHierarchy.get(name);
        if (entry == null) {
            entry = new ClassEntry();
            classHierarchy.put(name, entry);
        }
        entry.load(classFile);
    }

    boolean isInstanceOf(String child, String parent) {
        if (parent == null) {
            return false;
        }
        if (parent.equals(child)) {
            return true;
        }
        ClassEntry entry = classHierarchy.get(child);
        if (entry == null) {
            return false;
        }
        if (isInstanceOf(entry.parent, parent)) {
            return true;
        }
        for (String i : entry.interfaces) {
            if (isInstanceOf(i, parent)) {
                return true;
            }
        }
        return false;
    }

    void writeProperties(Properties properties) throws IOException {
        if (origMD5 != null) {
            properties.setProperty(Config.TAG_PRE_PATCH_STATE, isModded() ? "modded" : "unmodded");
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
        MCPatcherUtils.close(inputJar);
        MCPatcherUtils.close(outputJar);
        inputJar = null;
        outputJar = null;
    }

    void run() throws PatcherException, IOException {
        File jarFile = getOutputFile();
        Profile profile = profileManager.getOutputProfileData();
        if (profile == null) {
            throw new IllegalStateException(String.format(
                "Output profile '%s' unexpectedly missing", profileManager.getOutputProfile()
            ));
        }
        Version version = profileManager.getOutputVersionData();
        if (version == null || !version.isComplete()) {
            throw new IllegalStateException(String.format(
                "Output version '%s' unexpectedly missing", profileManager.getOutputVersion()
            ));
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
        cmdLine.add("-XX:+UseConcMarkSweepGC");
        cmdLine.add("-XX:+CMSIncrementalMode");
        cmdLine.add("-XX:-UseAdaptiveSizePolicy");

        profile.setGameArguments(gameArgs, profileManager.getProfileList());
        version.setGameArguments(gameArgs);

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

        version.fetchLibraries(libDir);

        cmdLine.add("-cp");
        StringBuilder sb = new StringBuilder();
        List<File> classPath = new ArrayList<File>();
        version.addToClassPath(libDir, classPath);
        classPath.add(jarFile);
        for (File f : classPath) {
            if (sb.length() > 0) {
                sb.append(File.pathSeparatorChar);
            }
            sb.append(f.getAbsolutePath());
        }
        cmdLine.add(sb.toString());
        version.unpackNatives(libDir, nativesDir);
        cmdLine.add("-Djava.library.path=" + nativesDir.getPath());

        cmdLine.add(version.getMainClass());
        profile.addGameArguments(gameArgs, cmdLine);
        version.addGameArguments(gameArgs, cmdLine);

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

    private static class ClassEntry {
        String parent;
        final List<String> interfaces = new ArrayList<String>();

        void load(ClassFile classFile) {
            parent = classFile.getSuperclass();
            for (String i : classFile.getInterfaces()) {
                interfaces.add(i);
            }
        }
    }
}
