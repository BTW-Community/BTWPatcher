package com.prupe.mcpatcher.mal.resource;

import com.prupe.mcpatcher.MCLogger;
import com.prupe.mcpatcher.MCPatcherUtils;
import net.minecraft.src.ResourceLocation;

import java.util.Properties;

final public class PropertiesFile {
    private static final MCLogger staticLogger = MCLogger.getLogger("Texture Pack");

    private final MCLogger logger;
    private final ResourceLocation resource;
    private final String prefix;
    private final Properties properties;

    private int warningCount;
    private int errorCount;

    public static PropertiesFile get(ResourceLocation resource) {
        return get(staticLogger, resource);
    }

    public static PropertiesFile getNonNull(ResourceLocation resource) {
        return getNonNull(staticLogger, resource);
    }

    public static PropertiesFile get(MCLogger logger, ResourceLocation resource) {
        Properties properties = TexturePackAPI.getProperties(resource);
        if (properties == null) {
            return null;
        } else {
            return new PropertiesFile(logger, resource, properties);
        }
    }

    public static PropertiesFile getNonNull(MCLogger logger, ResourceLocation resource) {
        PropertiesFile propertiesFile = get(logger, resource);
        if (propertiesFile == null) {
            return new PropertiesFile(logger, resource, new Properties());
        } else {
            return propertiesFile;
        }
    }

    private PropertiesFile(MCLogger logger, ResourceLocation resource, Properties properties) {
        this.logger = logger;
        this.resource = resource;
        prefix = (resource.toString() + ": ").replace("%", "%%");
        this.properties = properties;
    }

    public String getString(String key, String defaultValue) {
        return MCPatcherUtils.getStringProperty(properties, key, defaultValue);
    }

    public ResourceLocation getResourceLocation(String key, String defaultValue) {
        String value = getString(key, defaultValue);
        return TexturePackAPI.parseResourceLocation(resource, value);
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

    public boolean getBooleanProperty(String key, boolean defaultValue) {
        return MCPatcherUtils.getBooleanProperty(properties, key, defaultValue);
    }

    public void setProperty(String key, String value) {
        properties.setProperty(key, value);
    }

    public void info(String format, Object... params) {
        logger.info(prefix + format, params);
    }

    public void config(String format, Object... params) {
        logger.config(format, params);
    }

    public void warning(String format, Object... params) {
        logger.warning(prefix + format, params);
        warningCount++;
    }

    public boolean error(String format, Object... params) {
        logger.error(prefix + format, params);
        errorCount++;
        return false;
    }

    public int getWarningCount() {
        return warningCount;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public boolean valid() {
        return getErrorCount() == 0;
    }

    @Override
    public String toString() {
        return resource.toString();
    }
}
