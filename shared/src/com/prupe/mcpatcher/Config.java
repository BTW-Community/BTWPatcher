package com.prupe.mcpatcher;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.logging.Level;

public class Config {
    private static Config instance = new Config();

    private static File jsonFile;
    private static boolean readOnly;

    public static final String MCPATCHER_PROPERTIES = "mcpatcher.properties";
    public static final String MCPATCHER_JSON = "mcpatcher.json";
    public static final String LAUNCHER_JSON = "launcher_profiles.json";
    public static final String VERSIONS_JSON = "versions.json";

    static final String TAG_MINECRAFT_VERSION = "minecraftVersion";
    static final String TAG_PATCHER_VERSION = "patcherVersion";
    static final String TAG_PRE_PATCH_STATE = "prePatchState";
    static final String TAG_MODIFIED_CLASSES = "modifiedClasses";
    static final String TAG_ADDED_CLASSES = "addedClasses";

    static final String VAL_BUILTIN = "builtIn";
    static final String VAL_EXTERNAL_ZIP = "externalZip";
    static final String VAL_EXTERNAL_JAR = "externalJar";
    static final String VAL_EXTERNAL_FORGE = "externalForge";

    private static final String TAG_SELECTED_PROFILE = "selectedProfile";

    public static final String MCPATCHER_PROFILE_NAME = "MCPatcher";

    private static final int VAL_FORMAT_CURRENT = 1;
    private static final int VAL_FORMAT_MIN = 1;
    private static final int VAL_FORMAT_MAX = 1;

    private static final String HEX_CHARS = "0123456789abcdef";
    private static final String PROXY_CIPHER = "Blowfish";
    private static final String PROXY_ENC_PREFIX = "enc:";

    transient String selectedProfile = MCPATCHER_PROFILE_NAME;
    transient SecretKeySpec key;
    transient Cipher cipher;

    int format = VAL_FORMAT_CURRENT;
    String patcherVersion;
    String proxyHost;
    Integer proxyPort;
    String proxyUser;
    private String proxyPassword;
    boolean betaWarningShown;
    boolean selectPatchedProfile = true;
    boolean fetchRemoteVersionList = true;
    boolean extraProfiling;
    String lastModDirectory;
    LinkedHashMap<String, String> logging = new LinkedHashMap<String, String>();
    LinkedHashMap<String, ProfileEntry> profiles = new LinkedHashMap<String, ProfileEntry>();

    static boolean load(File minecraftDir) {
        jsonFile = new File(minecraftDir, MCPATCHER_JSON);
        instance = JsonUtils.parseJson(jsonFile, Config.class);
        if (instance == null || instance.format <= 0) {
            instance = new Config();
            save();
        } else if (instance.format < VAL_FORMAT_MIN) {
            instance.format = VAL_FORMAT_CURRENT;
            save();
        } else if (instance.format > VAL_FORMAT_MAX) {
            setReadOnly(true); // don't overwrite newer file
        }
        instance.selectedProfile = getSelectedLauncherProfile(minecraftDir);
        return true;
    }

    static boolean save() {
        boolean success = false;
        if (jsonFile != null && !readOnly) {
            JsonUtils.writeJson(instance, jsonFile);
        }
        return success;
    }

    void setProxy() {
        setProxy(proxyHost, proxyPort == null ? null : proxyPort.toString(), proxyUser, getProxyPassword());
    }

    void setProxy(String host, String port, final String user, final String pw) {
        int portNum = 0;
        if (!MCPatcherUtils.isNullOrEmpty(port)) {
            try {
                portNum = Integer.parseInt(port);
            } catch (NumberFormatException e) {
            }
        }
        if (!MCPatcherUtils.isNullOrEmpty(host) && portNum > 0 && portNum < 65536) {
            try {
                JsonUtils.proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(host, portNum));
                proxyHost = host;
                proxyPort = portNum;
                if (MCPatcherUtils.isNullOrEmpty(user) || MCPatcherUtils.isNullOrEmpty(pw)) {
                    proxyUser = null;
                    setProxyPassword(null);
                    Authenticator.setDefault(null);
                } else {
                    proxyUser = user;
                    setProxyPassword(pw);
                    Authenticator.setDefault(new Authenticator() {
                        @Override
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(user, pw.toCharArray());
                        }
                    });
                }
                return;
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        JsonUtils.proxy = Proxy.NO_PROXY;
        proxyHost = null;
        proxyPort = null;
        proxyUser = null;
        setProxyPassword(null);
        Authenticator.setDefault(null);
    }

    private Cipher initCipher(int mode) throws InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException {
        if (cipher == null) {
            cipher = Cipher.getInstance(PROXY_CIPHER);
        }
        if (key == null) {
            key = new SecretKeySpec("mOwRtHYFtdxHBcbq".getBytes(), PROXY_CIPHER);
        }
        cipher.init(mode, key);
        return cipher;
    }

    String getProxyPassword() {
        if (MCPatcherUtils.isNullOrEmpty(proxyPassword)) {
            setProxyPassword(null);
            return null;
        }
        if (!proxyPassword.startsWith(PROXY_ENC_PREFIX)) {
            setProxyPassword(proxyPassword);
            return getProxyPassword();
        }
        try {
            Cipher cipher = initCipher(Cipher.DECRYPT_MODE);
            String enc = proxyPassword.substring(PROXY_ENC_PREFIX.length());
            byte[] data = new byte[enc.length() / 2];
            for (int i = 0; i < data.length; i++) {
                String tmp = enc.substring(2 * i, 2 * i + 2);
                data[i] = (byte) Integer.parseInt(tmp, 16);
            }
            return new String(cipher.doFinal(data));
        } catch (Throwable e) {
            e.printStackTrace();
            setProxyPassword(null);
            return null;
        }
    }

    void setProxyPassword(String proxyPassword) {
        if (MCPatcherUtils.isNullOrEmpty(proxyPassword)) {
            this.proxyPassword = null;
            return;
        }
        try {
            Cipher cipher = initCipher(Cipher.ENCRYPT_MODE);
            byte[] in = cipher.doFinal(proxyPassword.getBytes());
            char[] out = new char[2 * in.length];
            for (int i = 0; i < out.length; i += 2) {
                out[i] = HEX_CHARS.charAt((in[i / 2] >> 4) & 0xf);
                out[i + 1] = HEX_CHARS.charAt(in[i / 2] & 0xf);
            }
            this.proxyPassword = PROXY_ENC_PREFIX + new String(out);
        } catch (Throwable e) {
            e.printStackTrace();
            this.proxyPassword = null;
        }
    }

    private static String getSelectedLauncherProfile(File minecraftDir) {
        String value = null;
        File path = new File(minecraftDir, LAUNCHER_JSON);
        JsonObject json = JsonUtils.parseJson(path);
        if (json != null) {
            JsonElement element = json.get(TAG_SELECTED_PROFILE);
            if (element != null && element.isJsonPrimitive()) {
                value = element.getAsString();
            }
        }
        if (MCPatcherUtils.isNullOrEmpty(value)) {
            return MCPATCHER_PROFILE_NAME;
        } else {
            return value;
        }
    }

    public static Config getInstance() {
        return instance;
    }

    public static void setReadOnly(boolean readOnly) {
        Config.readOnly = readOnly;
    }

    static Level getLogLevel(String category) {
        Level level = Level.INFO;
        String value = instance.logging.get(category);
        if (value != null) {
            try {
                level = Level.parse(value.trim().toUpperCase());
            } catch (Throwable e) {
            }
        }
        setLogLevel(category, level);
        return level;
    }

    static void setLogLevel(String category, Level level) {
        instance.logging.put(category, level.toString().toUpperCase());
    }

    /**
     * Gets a value from mcpatcher.json.
     *
     * @param tag          property name
     * @param defaultValue default value if not found in profile
     * @return String value
     */
    public static String getString(String mod, String tag, Object defaultValue) {
        LinkedHashMap<String, String> modConfig = instance.getModConfig(mod);
        String value = modConfig.get(tag);
        if (value == null) {
            modConfig.put(tag, defaultValue.toString());
            return defaultValue.toString();
        } else {
            return value;
        }
    }

    /**
     * Gets a value from mcpatcher.json.
     *
     * @param mod          name of mod
     * @param tag          property name
     * @param defaultValue default value if not found in profile
     * @return int value or 0
     */
    public static int getInt(String mod, String tag, int defaultValue) {
        int value;
        try {
            value = Integer.parseInt(getString(mod, tag, defaultValue));
        } catch (NumberFormatException e) {
            value = defaultValue;
        }
        return value;
    }

    /**
     * Gets a value from mcpatcher.json.
     *
     * @param mod          name of mod
     * @param tag          property name
     * @param defaultValue default value if not found in profile
     * @return boolean value
     */
    public static boolean getBoolean(String mod, String tag, boolean defaultValue) {
        String value = getString(mod, tag, defaultValue).toLowerCase();
        if (value.equals("false")) {
            return false;
        } else if (value.equals("true")) {
            return true;
        } else {
            return defaultValue;
        }
    }

    /**
     * Sets a value in mcpatcher.json.
     *
     * @param mod   name of mod
     * @param tag   property name
     * @param value property value (must support toString())
     */
    public static void set(String mod, String tag, Object value) {
        if (value == null) {
            remove(mod, tag);
            return;
        }
        instance.getModConfig(mod).put(tag, value.toString());
    }

    /**
     * Remove a value from mcpatcher.json.
     *
     * @param mod name of mod
     * @param tag property name
     */
    public static void remove(String mod, String tag) {
        instance.getModConfig(mod).remove(tag);
    }

    String getSelectedProfileName() {
        if (MCPatcherUtils.isNullOrEmpty(selectedProfile)) {
            selectedProfile = MCPATCHER_PROFILE_NAME;
        }
        return selectedProfile;
    }

    ProfileEntry getSelectedProfile() {
        ProfileEntry profile = profiles.get(getSelectedProfileName());
        if (profile == null) {
            profile = new ProfileEntry();
            profiles.put(selectedProfile, profile);
        }
        return profile;
    }

    VersionEntry getSelectedVersion() {
        ProfileEntry profile = getSelectedProfile();
        VersionEntry version = profile.versions.get(profile.version);
        if (version == null) {
            version = new VersionEntry();
            profile.versions.put(profile.version, version);
        }
        return version;
    }

    ModEntry getModEntry(String mod) {
        return getSelectedVersion().mods.get(mod);
    }

    Collection<ModEntry> getModEntries() {
        return getSelectedVersion().mods.values();
    }

    private LinkedHashMap<String, String> getModConfig(String mod) {
        return getSelectedProfile().getModConfig(mod);
    }

    void removeMod(String mod) {
        getSelectedProfile().config.remove(mod);
        getSelectedVersion().mods.remove(mod);
    }

    void removeProfile(String name) {
        if (!name.equals(selectedProfile)) {
            profiles.remove(name);
        }
    }

    void removeVersion(String name) {
        if (!name.equals(getSelectedProfile().version)) {
            getSelectedProfile().versions.remove(name);
        }
    }

    Map<String, String> getPatchedVersionMap() {
        Map<String, String> map = new HashMap<String, String>();
        for (ProfileEntry profile : profiles.values()) {
            profile.versions.remove(null);
            profile.versions.remove("");
            for (Map.Entry<String, VersionEntry> entry : profile.versions.entrySet()) {
                String patchedVersion = entry.getKey();
                String unpatchedVersion = entry.getValue().original;
                map.put(patchedVersion, unpatchedVersion);
            }
        }
        return map;
    }

    static class ProfileEntry {
        String original;
        String version;
        LinkedHashMap<String, LinkedHashMap<String, String>> config = new LinkedHashMap<String, LinkedHashMap<String, String>>();
        LinkedHashMap<String, VersionEntry> versions = new LinkedHashMap<String, VersionEntry>();

        private LinkedHashMap<String, String> getModConfig(String mod) {
            LinkedHashMap<String, String> map = config.get(mod);
            if (map == null) {
                map = new LinkedHashMap<String, String>();
                config.put(mod, map);
            }
            return map;
        }
    }

    static class VersionEntry {
        String original;
        LinkedHashMap<String, ModEntry> mods = new LinkedHashMap<String, ModEntry>();
    }

    static class ModEntry {
        String type;
        boolean enabled;
        String path;
        String className;
        List<FileEntry> files;
    }

    static class FileEntry {
        String from;
        String to;

        private FileEntry() {
        }

        FileEntry(String from, String to) {
            this.from = from;
            this.to = to;
        }
    }
}
