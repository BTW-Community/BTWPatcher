package com.prupe.mcpatcher.mal.resource;

import com.prupe.mcpatcher.MCLogger;
import com.prupe.mcpatcher.MCPatcherUtils;
import net.minecraft.src.ResourceLocation;

import java.util.Properties;

public class PropertiesFile {
    private static final MCLogger staticLogger = MCLogger.getLogger("Texture Pack");

    private final MCLogger logger;
    private final String resource;
    private final String prefix;
    private final Properties properties;

    private int warningCount;
    private int errorCount;

    public static PropertiesFile get(ResourceLocation resource) {
        return get(staticLogger, resource);
    }

    public static PropertiesFile get(MCLogger logger, ResourceLocation resource) {
        Properties properties = TexturePackAPI.getProperties(resource);
        if (properties == null) {
            return null;
        } else {
            return new PropertiesFile(logger, resource, properties);
        }
    }

    private PropertiesFile(MCLogger logger, ResourceLocation resource, Properties properties) {
        this.logger = logger;
        this.resource = resource.toString();
        prefix = (this.resource + ": ").replace("%", "%%");
        this.properties = properties;
    }

    public String getString(String key, String defaultValue) {
        return MCPatcherUtils.getStringProperty(properties, key, defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        return MCPatcherUtils.getIntProperty(properties, key, defaultValue);
    }

    public float getFloat(String key, float defaultValue) {
        return MCPatcherUtils.getFloatProperty(properties, key, defaultValue);
    }

    public int[] getIntList(String key, int minValue, int maxValue) {
        return getIntList(key, minValue, maxValue, null);
    }

    public int[] getIntList(String key, int minValue, int maxValue, String defaultValue) {
        String value = getString(key, defaultValue);
        if (value == null) {
            return null;
        } else {
            return MCPatcherUtils.parseIntegerList(value, minValue, maxValue);
        }
    }

    public void info(String format, Object... params) {
        logger.info(prefix + format, params);
    }

    public void warning(String format, Object... params) {
        logger.warning(prefix + format, params);
        warningCount++;
    }

    public void error(String format, Object... params) {
        logger.error(prefix + format, params);
        errorCount++;
    }

    public int getWarningCount() {
        return warningCount;
    }

    public int getErrorCount() {
        return errorCount;
    }

    @Override
    public String toString() {
        return resource;
    }
}
