package com.prupe.mcpatcher;

import java.lang.reflect.Constructor;

/**
 * Minecraft Abstraction Layer.
 */
public class MAL {
    public static <T> T newInstance(Class<T> baseClass, String apiName, String prefix) {
        String propertyName = apiName + Config.TAG_MAL_VERSION;
        int apiVersion = MCPatcherUtils.getIntProperty(MCPatcherUtils.getPatcherProperties(), propertyName, 0);
        if (apiVersion <= 0) {
            System.out.printf("ERROR: could not get %s from %s\n", propertyName, Config.MCPATCHER_PROPERTIES);
            return null;
        }
        String className = baseClass.getCanonicalName() + prefix + apiVersion;
        try {
            Class<? extends T> apiClass = Class.forName(className).asSubclass(baseClass);
            Constructor<? extends T> constructor = apiClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    public static <T> T newInstance(Class<T> baseClass, String apiName) {
        return newInstance(baseClass, apiName, "$V");
    }

    public static int getVersion(String apiName) {
        String propertyName = apiName + Config.TAG_MAL_VERSION;
        return MCPatcherUtils.getIntProperty(MCPatcherUtils.getPatcherProperties(), propertyName, 0);
    }
}
