package com.prupe.mcpatcher.launcher.version;

import com.prupe.mcpatcher.JsonUtils;
import com.prupe.mcpatcher.MCPatcherUtils;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Library {
    private static final String DEFAULT_URL = "https://s3.amazonaws.com/Minecraft.Download/libraries/";

    static final String NATIVES_TYPE;
    static final String OS_TYPE;
    static final String OS_VERSION = System.getProperty("os.version");

    String name;
    List<Rule> rules = new ArrayList<Rule>();
    Map<String, String> natives;
    Extract extract;
    String url;
    boolean serverreq;

    static {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("linux") || os.contains("unix")) {
            NATIVES_TYPE = "linux";
            OS_TYPE = "linux";
        } else if (os.contains("win")) {
            NATIVES_TYPE = "windows";
            OS_TYPE = "windows";
        } else if (os.contains("mac") || os.contains("osx")) {
            NATIVES_TYPE = "macosx";
            OS_TYPE = "osx";
        } else if (os.contains("solaris") || os.contains("sunos")) {
            NATIVES_TYPE = "solaris";
            OS_TYPE = "solaris";
        } else {
            NATIVES_TYPE = null;
            OS_TYPE = "unknown";
        }
    }

    public static String getOSType() {
        return OS_TYPE;
    }

    private Library() {
    }

    public Library(String name, String url) {
        this.name = name;
        this.url = url;
    }

    private boolean isNative() {
        return !MCPatcherUtils.isNullOrEmpty(natives);
    }

    public boolean isServer() {
        return serverreq;
    }

    private static File getPath(File libDir, String packageName, String library, String version, String suffix) {
        File jar = new File(libDir, packageName.replace(".", File.separator));
        jar = new File(jar, library);
        jar = new File(jar, version);
        StringBuilder name = new StringBuilder();
        name.append(library);
        name.append('-');
        name.append(version);
        if (!MCPatcherUtils.isNullOrEmpty(suffix)) {
            name.append('-');
            name.append(suffix);
        }
        name.append(".jar");
        return new File(jar, name.toString());
    }

    private static String[] splitName(String name, String suffix) {
        // org.lwjgl.lwjgl:lwjgl:2.9.0 ->
        //   libraries/        (base library dir)
        //   org/lwjgl/lwjgl/  (package)
        //   lwjgl/            (library name)
        //   2.9.0/            (library version)
        //   lwjgl-2.9.0.jar   (<library>-<version>(-<suffix>).jar)
        String[] tokens = name.split(":");
        if (tokens.length < 3) {
            return null;
        } else {
            return new String[]{
                tokens[0].replace('.', '/'),
                tokens[1],
                tokens[2],
                tokens[1] + "-" + tokens[2] + (MCPatcherUtils.isNullOrEmpty(suffix) ? "" : "-" + suffix) + ".jar"
            };
        }
    }

    boolean exclude() {
        boolean allow = rules.isEmpty();
        for (Rule rule : rules) {
            allow = rule.evaluate(allow);
        }
        return !allow;
    }

    public File getPath(File libDir) {
        String[] elem = splitName(name, isNative() ? natives.get(NATIVES_TYPE) : null);
        if (elem == null) {
            return null;
        } else {
            File path = libDir;
            for (String s : elem) {
                path = new File(path, s);
            }
            return path;
        }
    }

    public URL getURL() {
        String[] elem = splitName(name, isNative() ? natives.get(NATIVES_TYPE) : null);
        if (elem == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(MCPatcherUtils.isNullOrEmpty(url) ? DEFAULT_URL : url);
        for (String s : elem) {
            if (!sb.toString().endsWith("/")) {
                sb.append("/");
            }
            sb.append(s);
        }
        try {
            return new URL(sb.toString());
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean fetch(File libDir) {
        File local = getPath(libDir);
        URL remote = getURL();
        if (local == null || remote == null) {
            return false;
        }
        if (local.isFile()) {
            return true;
        }
        local.getParentFile().mkdirs();
        return JsonUtils.fetchURL(remote, local, false, "PK".getBytes());
    }

    public void addToClassPath(File libDir, List<File> jars) {
        if (isNative() || MCPatcherUtils.isNullOrEmpty(name) || exclude()) {
            return;
        }
        jars.add(getPath(libDir));
    }

    public void unpackNatives(File libDir, File destDir) {
        if (!isNative() || MCPatcherUtils.isNullOrEmpty(name) || exclude()) {
            return;
        }
        File jar = getPath(libDir);
        ZipFile zip = null;
        InputStream input = null;
        OutputStream output = null;
        try {
            if (!jar.isFile()) {
                throw new FileNotFoundException(jar.getAbsolutePath());
            }
            zip = new ZipFile(jar);
            entry:
            for (ZipEntry entry : Collections.list(zip.entries())) {
                String name = entry.getName();
                if (extract != null && extract.exclude != null) {
                    for (String s : extract.exclude) {
                        if (name.startsWith(s)) {
                            continue entry;
                        }
                    }
                }
                File dest = new File(destDir, name);
                if (!dest.isFile() || dest.length() != entry.getSize()) {
                    input = zip.getInputStream(entry);
                    output = new FileOutputStream(dest);
                    JsonUtils.copyStream(input, output);
                    MCPatcherUtils.close(input);
                    MCPatcherUtils.close(output);
                    input = null;
                    output = null;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            MCPatcherUtils.close(zip);
            MCPatcherUtils.close(input);
            MCPatcherUtils.close(output);
        }
    }
}
