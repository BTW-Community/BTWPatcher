package com.prupe.mcpatcher;

import com.google.gson.JsonObject;
import com.prupe.mcpatcher.launcher.profile.Profile;
import com.prupe.mcpatcher.launcher.profile.ProfileList;
import com.prupe.mcpatcher.launcher.version.Library;
import com.prupe.mcpatcher.launcher.version.Version;
import com.prupe.mcpatcher.launcher.version.VersionList;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;

class ProfileManager {
    private final Config config;
    private boolean remote = true;
    private boolean ready;

    private VersionList remoteVersions;
    private final List<String> unmoddedVersions = new ArrayList<String>();
    private final List<String> releaseVersions = new ArrayList<String>();
    private String inputVersion;
    private String outputVersion;

    private ProfileList profiles;
    private final List<String> unmoddedProfiles = new ArrayList<String>();
    private final List<String> moddedProfiles = new ArrayList<String>();
    private String inputProfile;
    private String outputProfile;

    ProfileManager(Config config) {
        this.config = config;
    }

    void setRemote(boolean remote) {
        this.remote = remote;
    }

    boolean isRemote() {
        return remote;
    }

    boolean setAsActive() {
        return config.selectPatchedProfile;
    }

    void refresh() throws IOException {
        refresh(false);
    }

    void refresh(boolean forceRemote) throws IOException {
        ready = false;

        rebuildRemoteVersionList(forceRemote);
        rebuildLocalVersionList();
        addUnmoddedCustomVersions();
        rebuildProfileList();

        String profile = profiles.getSelectedProfile();
        if (moddedProfiles.contains(profile)) {
            selectOutputProfile(profile);
        } else if (unmoddedProfiles.contains(profile)) {
            selectInputProfile(profile);
        } else {
            selectDefaultProfile();
        }

        ready = true;
    }

    private void rebuildRemoteVersionList(boolean forceRemote) throws IOException {
        int timeout = forceRemote ? JsonUtils.LONG_TIMEOUT / 2 : JsonUtils.SHORT_TIMEOUT;
        remoteVersions = VersionList.getRemoteVersionList(forceRemote || remote, timeout);
        if (remoteVersions != null && !remoteVersions.getVersions().isEmpty()) {
            return;
        }
        if (!forceRemote) {
            Logger.log(Logger.LOG_MAIN, "WARNING: using included copy of versions.json, may be out of date");
            remoteVersions = VersionList.getBuiltInVersionList();
            config.fetchRemoteVersionList = false;
        }
        if (remoteVersions == null || remoteVersions.getVersions().isEmpty()) {
            throw new IOException("Could not get list of Minecraft versions");
        }
    }

    private void rebuildLocalVersionList() throws IOException {
        unmoddedVersions.clear();
        releaseVersions.clear();
        OriginalVersion.clear();

        for (Version remote : remoteVersions.getVersions()) {
            Version local = Version.getLocalVersion(remote.getId());
            if (local != null && local.isComplete()) {
                unmoddedVersions.add(0, local.getId());
                if (!local.isSnapshot()) {
                    releaseVersions.add(0, local.getId());
                }
                OriginalVersion.register(local);
            }
        }
        if (unmoddedVersions.isEmpty()) {
            throw new IOException("No installed unmodded versions found");
        }
    }

    private void addUnmoddedCustomVersions() {
        File[] files = MCPatcherUtils.getMinecraftPath("versions").listFiles();
        if (files == null) {
            return;
        }
        for (File subdir : files) {
            if (!subdir.isDirectory()) {
                continue;
            }
            String id = subdir.getName();
            if (unmoddedVersions.contains(id)) {
                continue;
            }
            Version local = Version.getLocalVersion(id);
            if (local == null || !local.isComplete()) {
                continue;
            }
            String md5sum = Util.computeMD5(local.getJarPath());
            OriginalVersion orig = OriginalVersion.get(md5sum);
            if (orig != null) {
                unmoddedVersions.add(id);
                OriginalVersion.addVersionMap(id, orig);
            }
        }
    }

    private void rebuildProfileList() throws IOException {
        unmoddedProfiles.clear();
        moddedProfiles.clear();
        moddedProfiles.add(Config.MCPATCHER_PROFILE_NAME);

        profiles = ProfileList.getProfileList();
        if (profiles == null || profiles.getProfiles().isEmpty()) {
            throw new IOException("Could not get list of launcher profiles");
        }

        pruneConfigProfiles();
        for (String profile : config.profiles.keySet()) {
            if (!MCPatcherUtils.isNullOrEmpty(profile) && !moddedProfiles.contains(profile)) {
                moddedProfiles.add(profile);
            }
        }

        for (Map.Entry<String, Profile> entry : profiles.getProfiles().entrySet()) {
            if (moddedProfiles.contains(entry.getKey())) {
                continue;
            }
            String version = entry.getValue().getLastVersionId();
            if (version == null || unmoddedVersions.contains(version)) {
                unmoddedProfiles.add(entry.getKey());
            }
        }
    }

    private void pruneConfigProfiles() {
        Set<String> patchedBefore = config.getPatchedVersionMap().keySet();
        Set<String> rmProfile = new HashSet<String>();
        for (Map.Entry<String, Config.ProfileEntry> entry : config.profiles.entrySet()) {
            String name = entry.getKey();
            Config.ProfileEntry profile = entry.getValue();
            if ((!name.equals(Config.MCPATCHER_PROFILE_NAME) && !profiles.getProfiles().containsKey(name)) ||
                profile == null ||
                MCPatcherUtils.isNullOrEmpty(profile.versions) ||
                MCPatcherUtils.isNullOrEmpty(profile.config)) {
                rmProfile.add(name);
                continue;
            }
            Set<String> rmVersion = new HashSet<String>();
            for (Map.Entry<String, Config.VersionEntry> entry1 : profile.versions.entrySet()) {
                name = entry1.getKey();
                if (name == null) {
                    rmVersion.add(name);
                    continue;
                }
                if (name.equals(profile.version)) {
                    continue;
                }
                Version version = Version.getLocalVersion(name);
                if (version == null || !version.isComplete()) {
                    rmVersion.add(name);
                }
            }
            for (String s : rmVersion) {
                Logger.log(Logger.LOG_MAIN, "removing version %s from config since local version is no longer installed", name);
                profile.versions.remove(s);
            }
        }
        for (String s : rmProfile) {
            Logger.log(Logger.LOG_MAIN, "removing profile %s no longer in launcher_profiles", s);
            String version = config.profiles.get(s).version;
            if (!MCPatcherUtils.isNullOrEmpty(version)) {
                Version.deleteLocalFiles(version);
            }
            config.profiles.remove(s);
        }

        if (config.profiles.get(Config.MCPATCHER_PROFILE_NAME) == null) {
            config.profiles.put(Config.MCPATCHER_PROFILE_NAME, new Config.ProfileEntry());
        }
        patchedBefore.removeAll(config.getPatchedVersionMap().keySet());
        for (Profile profile : profiles.getProfiles().values()) {
            patchedBefore.remove(profile.getLastVersionId());
        }
        for (String s : patchedBefore) {
            Logger.log(Logger.LOG_MAIN, "removing orphaned version %s", s);
            Version.deleteLocalFiles(s);
        }
    }

    private static String makePatchedVersionString(String inputVersion, String outputProfile) {
        return inputVersion + '-' + outputProfile.toLowerCase().replaceAll("[^a-zA-Z0-9_.]", "");
    }

    private String makePatchedVersionString() {
        return makePatchedVersionString(inputVersion, outputProfile);
    }

    private List<String> getProfileVersions(Profile profile) {
        if (profile == null || profile.isSnapshotAllowed()) {
            return unmoddedVersions;
        } else {
            return releaseVersions;
        }
    }

    private String getProfileVersion(Profile profile) {
        if (profile != null && !MCPatcherUtils.isNullOrEmpty(profile.getLastVersionId())) {
            String version = profile.getLastVersionId();
            if (unmoddedVersions.contains(version)) {
                return version;
            }
            version = config.getPatchedVersionMap().get(version);
            if (unmoddedVersions.contains(version)) {
                return version;
            }
            version = version.replaceFirst("-.*", "");
            if (unmoddedVersions.contains(version)) {
                return version;
            }
        }
        return getProfileVersions(profile).get(0);
    }

    boolean isReady() {
        return ready;
    }

    List<String> getInputProfiles() {
        return unmoddedProfiles;
    }

    List<String> getInputVersions() {
        return unmoddedVersions;
    }

    List<String> getOutputProfiles() {
        return moddedProfiles;
    }

    int getSelectedInputVersionIndex() {
        return getInputVersions().indexOf(inputVersion);
    }

    int getSelectedOutputProfileIndex() {
        return getOutputProfiles().indexOf(outputProfile);
    }

    String getInputProfile() {
        return inputProfile;
    }

    String getOutputProfile() {
        return outputProfile;
    }

    String getInputVersion() {
        return inputVersion;
    }

    String getInputBaseVersion() {
        return OriginalVersion.getBaseVersion(inputVersion);
    }

    String getOutputVersion() {
        return outputVersion;
    }

    ProfileList getProfileList() {
        return profiles;
    }

    void selectInputProfile(String name) {
        inputProfile = null;
        outputProfile = null;

        if (!unmoddedProfiles.contains(name)) {
            name = unmoddedProfiles.get(0);
        }
        inputProfile = name;
        for (Map.Entry<String, Config.ProfileEntry> entry : config.profiles.entrySet()) {
            if (name.equals(entry.getValue().original)) {
                outputProfile = entry.getKey();
                break;
            }
        }
        if (MCPatcherUtils.isNullOrEmpty(outputProfile)) {
            outputProfile = Config.MCPATCHER_PROFILE_NAME;
        }

        config.selectedProfile = outputProfile;
        config.getSelectedProfile().original = inputProfile;
        selectInputVersion(getProfileVersion(getInputProfileData()));
    }

    void selectOutputProfile(String name) {
        outputProfile = name;
        config.selectedProfile = name;
        Config.ProfileEntry profileEntry = config.getSelectedProfile();
        if (unmoddedProfiles.contains(profileEntry.original)) {
            inputProfile = profileEntry.original;
        } else {
            inputProfile = null;
        }
        Config.VersionEntry versionEntry = profileEntry.versions.get(profileEntry.version);
        if (versionEntry != null && unmoddedVersions.contains(versionEntry.original)) {
            inputVersion = versionEntry.original;
            outputVersion = makePatchedVersionString();
        } else {
            inputVersion = unmoddedVersions.get(0);
            versionEntry = new Config.VersionEntry();
            versionEntry.original = inputVersion;
            outputVersion = makePatchedVersionString();
            profileEntry.versions.put(outputVersion, versionEntry);
        }
    }

    void selectInputVersion(String name) {
        inputVersion = null;
        outputVersion = null;

        if (MCPatcherUtils.isNullOrEmpty(name)) {
            name = getProfileVersion(getInputProfileData());
        }
        if (unmoddedVersions.contains(name)) {
            inputVersion = name;
        } else if (config.getSelectedProfile().versions.get(name) != null) {
            inputVersion = config.getSelectedProfile().versions.get(name).original;
            outputVersion = name;
        }

        if (!unmoddedVersions.contains(name)) {
            inputVersion = unmoddedVersions.get(0);
        }
        if (MCPatcherUtils.isNullOrEmpty(outputVersion)) {
            outputVersion = makePatchedVersionString();
        }

        config.getSelectedProfile().version = outputVersion;
        config.getSelectedVersion().original = inputVersion;
    }

    private void selectDefaultProfile() {
        inputProfile = null;
        outputProfile = Config.MCPATCHER_PROFILE_NAME;
        config.selectedProfile = outputProfile;
        selectInputVersion(null);
    }

    File getInputJar() {
        if (MCPatcherUtils.isNullOrEmpty(inputVersion)) {
            return null;
        } else {
            return Version.getJarPath(inputVersion);
        }
    }

    File getOutputJar() {
        if (MCPatcherUtils.isNullOrEmpty(outputVersion)) {
            return null;
        } else {
            return Version.getJarPath(outputVersion);
        }
    }

    private Profile getInputProfileData() {
        return profiles.getProfile(inputProfile);
    }

    Profile getOutputProfileData() {
        if (MCPatcherUtils.isNullOrEmpty(outputProfile)) {
            return null;
        } else {
            ProfileList profiles = ProfileList.getProfileList();
            return profiles == null ? null : profiles.getProfile(outputProfile);
        }
    }

    Version getOutputVersionData() {
        if (MCPatcherUtils.isNullOrEmpty(outputVersion)) {
            return null;
        } else {
            return Version.getLocalVersion(outputVersion);
        }
    }

    Library getForgeLibrary() {
        Version local = Version.getLocalVersion(getInputVersion());
        if (local == null) {
            return null;
        }
        for (Library library : local.getLibraries()) {
            if (!library.exclude() && library.getName().equalsIgnoreCase("minecraftforge")) {
                return library;
            }
        }
        return null;
    }

    private void addLauncherProfile(String profile, String version, String javaArgs) {
        Profile oldProfile = getInputProfileData();
        if (oldProfile == null) {
            oldProfile = new Profile();
        }
        boolean setActive = setAsActive();
        if (!Version.getJarPath(version).isFile()) {
            setActive = false;
        }
        Profile newProfile = oldProfile.copyToNewProfile(profile, version, setActive, javaArgs);
        if (newProfile != null) {
            profiles.getProfiles().put(profile, newProfile);
            if (!moddedProfiles.contains(profile)) {
                moddedProfiles.add(profile);
            }
        }
    }

    void createOutputProfile(JsonObject baseVersion, String javaArgs) {
        if (!MCPatcherUtils.isNullOrEmpty(inputVersion) && !MCPatcherUtils.isNullOrEmpty(outputVersion)) {
            Version version = Version.getLocalVersion(inputVersion);
            if (version != null && version.isComplete()) {
                version.copyToNewVersion(baseVersion, outputVersion);
            }
        }
        addLauncherProfile(outputProfile, outputVersion, javaArgs);
    }

    void copyCurrentProfile(String newName) {
        String newVersion = makePatchedVersionString(inputVersion, newName);
        addLauncherProfile(newName, newVersion, null);
        Config.ProfileEntry entry = config.profiles.get(outputProfile);
        Config.ProfileEntry newEntry = JsonUtils.cloneJson(entry, Config.ProfileEntry.class);
        newEntry.version = newVersion;
        config.profiles.put(newName, newEntry);
        if (!moddedProfiles.contains(newName)) {
            moddedProfiles.add(newName);
        }
        Config.VersionEntry version = JsonUtils.cloneJson(entry.versions.get(entry.version), Config.VersionEntry.class);
        version.original = inputVersion;
        newEntry.versions.clear();
        newEntry.versions.put(newVersion, version);
        selectOutputProfile(newName);
    }

    void deleteLocalVersion(String version) {
        if (!MCPatcherUtils.isNullOrEmpty(version) && !unmoddedVersions.contains(version)) {
            Version.deleteLocalFiles(version);
        }
    }

    void deleteProfile(String name, boolean deleteVersions) {
        if (MCPatcherUtils.isNullOrEmpty(name) || name.equals(Config.MCPATCHER_PROFILE_NAME)) {
            return;
        }
        Config.ProfileEntry entry = config.profiles.get(name);
        if (deleteVersions && entry != null) {
            deleteLocalVersion(entry.version);
        }
        Profile profile = profiles.getProfile(name);
        if (profile != null) {
            profile.delete(inputProfile);
        }
        if (name.equals(config.selectedProfile)) {
            config.selectedProfile = Config.MCPATCHER_PROFILE_NAME;
        }
        profiles.getProfiles().remove(name);
        if (!name.equals(outputProfile)) {
            config.profiles.remove(name);
        }
        moddedProfiles.remove(name);
    }

    File getLocalVersionPath(String name) {
        return Version.getJarPath(name).getParentFile();
    }

    void dumpJson(PrintStream out) {
        if (profiles == null) {
            out.printf("%s: (error)\n", Config.LAUNCHER_JSON);
        } else {
            out.printf("%s: (%d profiles)\n", Config.LAUNCHER_JSON, profiles.getProfiles().size());
            for (Map.Entry<String, Profile> entry : profiles.getProfiles().entrySet()) {
                String name = entry.getKey();
                Profile profile = entry.getValue();
                boolean selected = name != null && name.equals(profiles.getSelectedProfile());
                out.printf("  %1s %s version=%s\n", selected ? "*" : "", name, profile.getLastVersionId());
            }
        }

        out.printf("%s: (%d profiles)\n", Config.MCPATCHER_JSON, config.profiles.size());
        for (Map.Entry<String, Config.ProfileEntry> entry : config.profiles.entrySet()) {
            String name = entry.getKey();
            Config.ProfileEntry profile = entry.getValue();
            boolean selected = name != null && name.equals(config.selectedProfile);
            out.printf("  %1s %s original=%s (%d versions)\n", selected ? "*" : "", name, profile.original, profile.versions.size());
            for (Map.Entry<String, Config.VersionEntry> entry1 : profile.versions.entrySet()) {
                name = entry1.getKey();
                Config.VersionEntry version = entry1.getValue();
                selected = name != null && name.equals(profile.version);
                out.printf("    %1s %s original=%s (%d mods)\n", selected ? "*" : "", name, version.original, version.mods.size());
            }
        }

        out.printf("%s%s: (%d versions)\n", Config.VERSIONS_JSON, remote ? "" : " (local copy)", unmoddedVersions.size());
        for (Version version : remoteVersions.getVersions()) {
            out.printf("  %1s %s%s\n", version.isComplete() ? "*" : "", version.getId(), version.isSnapshot() ? " (snapshot)" : "");
        }
    }

    private static class OriginalVersion {
        private static final Map<String, OriginalVersion> md5Map = new HashMap<String, OriginalVersion>();
        private static final Map<String, String> baseVersionMap = new HashMap<String, String>();

        final String version;
        final String md5sum;

        static void register(Version version) throws IOException {
            OriginalVersion o = new OriginalVersion(version);
            md5Map.put(o.md5sum, o);
        }

        static OriginalVersion get(String md5sum) {
            return md5Map.get(md5sum);
        }

        static void addVersionMap(String customVersion, OriginalVersion baseVersion) {
            baseVersionMap.put(customVersion, baseVersion.version);
        }

        static String getBaseVersion(String inputVersion) {
            String baseVersion = baseVersionMap.get(inputVersion);
            return MCPatcherUtils.isNullOrEmpty(baseVersion) ? inputVersion : baseVersion;
        }

        static void clear() {
            md5Map.clear();
            baseVersionMap.clear();
        }

        private OriginalVersion(Version version) throws IOException {
            this.version = version.getId();
            md5sum = Util.computeMD5(version.getJarPath());
            if (MCPatcherUtils.isNullOrEmpty(md5sum)) {
                throw new IOException("Could not determine md5sum of " + version.getJarPath());
            }
        }
    }
}
