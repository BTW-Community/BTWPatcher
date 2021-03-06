package com.prupe.mcpatcher;

import com.prupe.mcpatcher.launcher.version.Library;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Pack200;
import java.util.zip.Adler32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

class ForgeAdapter extends Mod {
    private static final String FORGE_LIB_NAME = "forge";
    private static final int FORGE_MIN_VERSION = 804;
    private static final String FORGE_MIN_VERSION_STR = "9.10.0.804";

    private static final String FML_LIB_NAME = "fml";
    private static final int FML_MIN_VERSION = 787;
    private static final String FML_MIN_VERSION_STR = "6.4.41.787";

    private static final String[] GDIFF_CLASSES = new String[]{
        "cpw.mods.fml.repackage.com.nothome.delta.GDiffPatcher",
        "net.minecraftforge.fml.repackage.com.nothome.delta.GDiffPatcher", // 1.8+
    };
    private static final String GDIFF_PATCH_METHOD = "patch";

    private static final String LZMA_PACKAGE = "lzma";
    private static final String LZMA_NAME = "lzma";
    private static final String LZMA_VERSION = "0.0.1";
    private static final String LZMA_MAVEN = LZMA_PACKAGE + ":" + LZMA_NAME + ":" + LZMA_VERSION;
    private static final String LZMA_CLASS = "LZMA.LzmaInputStream";

    private static final String BINPATCHES_PACK = "binpatches.pack.lzma";
    private static final String BINPATCH_PREFIX = "binpatch/client/";
    private static final String BINPATCH_SUFFIX = ".binpatch";

    private final File mcLibDir;
    private final File forgeJarPath;
    private final Map<String, Binpatch> patches = new HashMap<String, Binpatch>();
    private int buildNumber;

    private Constructor<?> gdiffConstructor1;
    private Constructor<?> gdiffConstructor2;
    private Method gdiffPatchMethod;

    private Constructor<? extends InputStream> lzmaConstructor;

    ForgeAdapter(UserInterface ui, Library forgeLibrary) throws Exception {
        mcLibDir = MCPatcherUtils.getMinecraftPath("libraries");
        forgeJarPath = forgeLibrary.getPath(mcLibDir);
        version = forgeLibrary.getVersion();
        website = "http://minecraftforge.net/";
        clearDependencies();

        int minVersion;
        String minVersionStr;
        String libName = forgeLibrary.getName().toLowerCase();
        if (libName.contains(FORGE_LIB_NAME)) {
            minVersion = FORGE_MIN_VERSION;
            minVersionStr = FORGE_MIN_VERSION_STR;
            name = "Minecraft Forge";
            author = "Minecraft Forge team";
            description = "Minecraft Forge";
        } else if (libName.equals(FML_LIB_NAME)) {
            minVersion = FML_MIN_VERSION;
            minVersionStr = FML_MIN_VERSION_STR;
            name = "Forge Modloader";
            author = "FML team";
            description = "Minecraft Forge Modloader";
        } else {
            throw new IOException("Invalid filename " + forgeJarPath.getName());
        }

        String temp = version.replaceAll(".*[^0-9](\\d+)$", "$1");
        if (!MCPatcherUtils.isNullOrEmpty(temp)) {
            try {
                buildNumber = Integer.parseInt(temp);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        if (buildNumber != 0 && buildNumber < minVersion) {
            addError("Requires " + name + " " + minVersionStr + " or newer");
            return;
        }

        ui.setStatusText("Analyzing %s...", forgeJarPath.getName());
        ui.updateProgress(0, 3);
        setupClassLoader();

        ZipFile forgeZip = null;
        try {
            ui.updateProgress(1, 3);
            forgeZip = new ZipFile(forgeJarPath);
            ui.updateProgress(2, 3);
            loadBinPatches(forgeZip, BINPATCHES_PACK);
            ui.updateProgress(3, 3);
        } finally {
            MCPatcherUtils.close(forgeZip);
            ui.updateProgress(0, 0);
            ui.setStatusText("");
        }

        description = String.format("%d classes modified", patches.size());
    }

    static boolean isForgeLibrary(Library library) {
        if (library == null || library.exclude()) {
            return false;
        }
        String name = library.getName().toLowerCase();
        return name.contains(FORGE_LIB_NAME) || name.equals(FML_LIB_NAME);
    }

    @Override
    public String[] getLoggingCategories() {
        return null;
    }

    @Override
    public InputStream openFile(String name) throws IOException {
        name = name.replaceFirst("^/", "");
        Binpatch binpatch = patches.get(name);
        if (binpatch == null) {
            throw new IOException("No patch for " + name);
        }
        byte[] input = binpatch.getInput(MCPatcher.minecraft.getInputJar());
        byte[] output = binpatch.apply(input);
        ByteArrayOutputStream tmp = new ByteArrayOutputStream();
        tmp.write(output);
        return new ByteArrayInputStream(tmp.toByteArray());
    }

    @Override
    void addExtraJavaArguments(List<String> cmdLine) {
        cmdLine.add("-Dfml.ignorePatchDiscrepancies=true");
        cmdLine.add("-Dfml.ignoreInvalidMinecraftCertificates=true");
    }

    private void setupClassLoader() throws Exception {
        Library lzmaLib = new Library(LZMA_MAVEN, null);
        lzmaLib.fetch(mcLibDir);
        ClassLoader classLoader = URLClassLoader.newInstance(new URL[]{
            lzmaLib.getPath(mcLibDir).toURI().toURL(),
            forgeJarPath.toURI().toURL()
        }, getClass().getClassLoader());

        Class<?> gdiffClass = null;
        Exception savedException = null;
        for (String gdiffClassName : GDIFF_CLASSES) {
            try {
                gdiffClass = classLoader.loadClass(gdiffClassName);
                break;
            } catch (ClassNotFoundException e) {
                savedException = e;
            }
        }
        if (gdiffClass == null) {
            throw savedException;
        }
        try {
            gdiffConstructor1 = gdiffClass.getDeclaredConstructor();
            gdiffPatchMethod = gdiffClass.getDeclaredMethod(GDIFF_PATCH_METHOD, byte[].class, InputStream.class, OutputStream.class);
        } catch (NoSuchMethodException e) {
            gdiffConstructor2 = gdiffClass.getDeclaredConstructor(byte[].class, InputStream.class, OutputStream.class);
        }

        Class<? extends InputStream> lzmaClass = classLoader.loadClass(LZMA_CLASS).asSubclass(InputStream.class);
        lzmaConstructor = lzmaClass.getConstructor(InputStream.class);
    }

    private void loadBinPatches(ZipFile zip, String name) throws Exception {
        ZipEntry entry = zip.getEntry(name);
        if (entry == null) {
            throw new IOException("Could not load " + name + " from " + forgeJarPath.getName());
        }
        InputStream raw = null;
        InputStream lzma = null;
        ZipInputStream zipStream = null;
        JarOutputStream jar = null;
        try {
            raw = zip.getInputStream(entry);
            lzma = lzmaConstructor.newInstance(raw);
            ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
            jar = new JarOutputStream(byteOutput);
            Pack200.Unpacker pack = Pack200.newUnpacker();
            pack.unpack(lzma, jar);
            jar.close();
            ByteArrayInputStream byteInput = new ByteArrayInputStream(byteOutput.toByteArray());
            zipStream = new ZipInputStream(byteInput);
            while ((entry = zipStream.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                name = entry.getName();
                if (name.startsWith(BINPATCH_PREFIX) && name.endsWith(BINPATCH_SUFFIX)) {
                    Binpatch binpatch = new Binpatch(zipStream);
                    addClassFile(binpatch.obfClass);
                    patches.put(binpatch.filename, binpatch);
                }
            }
        } finally {
            MCPatcherUtils.close(jar);
            MCPatcherUtils.close(zipStream);
            MCPatcherUtils.close(lzma);
            MCPatcherUtils.close(raw);
        }
    }

    private class Binpatch {
        final String filename;
        final String obfClass;
        final String deobfClass;
        final boolean hasInput;
        final int checksum;
        final byte[] patchData;

        Binpatch(InputStream input) throws IOException {
            DataInputStream data = new DataInputStream(input);
            filename = data.readUTF() + ".class";
            obfClass = data.readUTF();
            deobfClass = data.readUTF();
            hasInput = data.readBoolean();
            checksum = hasInput ? data.readInt() : 0;
            int expLen = data.readInt();
            patchData = new byte[expLen];
            int actualLen = 0;
            while (actualLen < expLen) {
                int count = data.read(patchData, actualLen, expLen - actualLen);
                if (count <= 0) {
                    break;
                }
                actualLen += count;
            }
            if (actualLen != expLen) {
                throw new IOException(String.format("Patch %s: EOF at %d/%d bytes", filename, actualLen, expLen));
            }
        }

        byte[] getInput(JarFile jar) throws IOException {
            if (hasInput) {
                JarEntry entry = jar.getJarEntry(filename);
                if (entry == null) {
                    throw new IOException(String.format("Patch %s: input file is missing", filename));
                }
                InputStream inputStream = jar.getInputStream(entry);
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                Util.copyStream(inputStream, outputStream);
                MCPatcherUtils.close(inputStream);
                MCPatcherUtils.close(outputStream);
                byte[] output = outputStream.toByteArray();
                Adler32 adler = new Adler32();
                adler.update(output);
                int actual = (int) adler.getValue();
                if (actual != checksum) {
                    throw new IOException(String.format("Patch %s: invalid checksum: expected=%08x, actual=%08x", filename, checksum, actual));
                }
                return output;
            } else {
                return new byte[0];
            }
        }

        private byte[] apply(byte[] input) throws IOException {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ByteArrayInputStream diff = new ByteArrayInputStream(patchData);
            try {
                if (gdiffConstructor1 != null) {
                    Object patcher = gdiffConstructor1.newInstance();
                    gdiffPatchMethod.invoke(patcher, input, diff, output);
                } else {
                    gdiffConstructor2.newInstance(input, diff, output);
                }
                return output.toByteArray();
            } catch (Throwable e) {
                throw new IOException(e);
            }
        }
    }
}
