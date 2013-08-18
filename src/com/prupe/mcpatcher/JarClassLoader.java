package com.prupe.mcpatcher;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

class JarClassLoader extends ClassLoader {
    private static final String FAKE_URL_PROTOCOL = "jarjar";

    private final Map<String, byte[]> cache = new HashMap<String, byte[]>();
    private final String resourcePath;

    JarClassLoader(String resourcePath) throws IOException {
        this.resourcePath = resourcePath;
        InputStream input = null;
        JarInputStream jar = null;
        ByteArrayOutputStream output = null;
        try {
            input = openInputStream();
            jar = new JarInputStream(input);
            JarEntry entry;
            while ((entry = jar.getNextJarEntry()) != null) {
                if (!entry.isDirectory()) {
                    String name = entry.getName();
                    output = new ByteArrayOutputStream();
                    int count;
                    byte[] buffer = new byte[1024];
                    while ((count = jar.read(buffer)) > 0) {
                        output.write(buffer, 0, count);
                    }
                    output.close();
                    cache.put(name, output.toByteArray());
                }
            }
        } finally {
            MCPatcherUtils.close(jar);
            MCPatcherUtils.close(input);
            MCPatcherUtils.close(output);
        }
    }

    @Override
    public String toString() {
        return "JarClassLoader{" + resourcePath + "}";
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        String filename = ClassMap.classNameToFilename(name);
        byte[] data = getEntry(filename);
        if (data == null) {
            throw new ClassNotFoundException(name);
        } else {
            Class<?> cl = defineClass(name, data, 0, data.length);
            resolveClass(cl);
            return cl;
        }
    }

    @Override
    protected URL findResource(final String name) {
        try {
            return new URL(FAKE_URL_PROTOCOL, resourcePath, 0, "/" + name, new URLStreamHandler() {
                @Override
                protected URLConnection openConnection(URL u) throws IOException {
                    return new URLConnection(u) {
                        @Override
                        public void connect() throws IOException {
                        }

                        @Override
                        public InputStream getInputStream() throws IOException {
                            byte[] data = getEntry(name);
                            if (data == null) {
                                throw new FileNotFoundException(name);
                            }
                            return new ByteArrayInputStream(data);
                        }
                    };
                }
            });
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    private InputStream openInputStream() throws IOException {
        InputStream input = getParent().getResourceAsStream(resourcePath);
        if (input == null) {
            input = getParent().getResourceAsStream(resourcePath.replaceFirst("^/", ""));
            if (input == null) {
                throw new FileNotFoundException(resourcePath);
            }
        }
        return input;
    }

    private byte[] getEntry(String filename) {
        return cache.get(filename);
    }
}
