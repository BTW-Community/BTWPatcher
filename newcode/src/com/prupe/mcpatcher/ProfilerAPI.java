package com.prupe.mcpatcher;

public class ProfilerAPI {
    public static void startSection(String name) {
        MCPatcherUtils.getMinecraft().mcProfiler.startSection(name);
    }

    public static void endStartSection(String name) {
        MCPatcherUtils.getMinecraft().mcProfiler.endStartSection(name);
    }

    public static void endSection() {
        MCPatcherUtils.getMinecraft().mcProfiler.endSection();
    }
}
