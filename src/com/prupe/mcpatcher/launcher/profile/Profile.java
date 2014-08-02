package com.prupe.mcpatcher.launcher.profile;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.prupe.mcpatcher.JsonUtils;
import com.prupe.mcpatcher.MCPatcherUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Profile {
    private static final String TAG_PROFILES = "profiles";
    private static final String TAG_NAME = "name";
    private static final String TAG_LAST_VERSION_ID = "lastVersionId";
    private static final String TAG_SELECTED_PROFILE = "selectedProfile";
    private static final String TAG_JAVA_ARGS = "javaArgs";

    private static final String ALLOW_RELEASE = "release";
    private static final String ALLOW_SNAPSHOT = "snapshot";

    private static final boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");

    String name;
    String lastVersionId;
    String gameDir;
    String javaDir;
    String javaArgs;
    Resolution resolution;
    List<String> allowedReleaseTypes = new ArrayList<String>();
    String playerUUID;

    public Profile() {
    }

    public Profile copyToNewProfile(String newName, String version, boolean selectProfile, String javaArgs) {
        JsonObject json = JsonUtils.parseJson(ProfileList.getProfilesPath());
        if (json == null) {
            return null;
        }
        JsonObject profiles = json.getAsJsonObject(TAG_PROFILES);
        if (profiles == null) {
            return null;
        }
        JsonObject profile = profiles.getAsJsonObject(name);
        if (profile == null) {
            profile = new JsonObject();
        }
        JsonObject newProfile = JsonUtils.cloneJson(profile);
        newProfile.addProperty(TAG_NAME, newName);
        if (version == null) {
            newProfile.remove(TAG_LAST_VERSION_ID);
        } else {
            newProfile.addProperty(TAG_LAST_VERSION_ID, version);
        }
        profiles.add(newName, newProfile);
        if (selectProfile) {
            json.addProperty(TAG_SELECTED_PROFILE, newName);
        } else {
            JsonPrimitive prim = json.getAsJsonPrimitive(TAG_SELECTED_PROFILE);
            if (prim != null && prim.isString() && newName.equals(prim.getAsString())) {
                json.addProperty(TAG_SELECTED_PROFILE, name);
            }
        }
        if (!MCPatcherUtils.isNullOrEmpty(javaArgs)) {
            StringBuilder sb = new StringBuilder();
            JsonElement elem = newProfile.get(TAG_JAVA_ARGS);
            if (elem != null && elem.isJsonPrimitive()) {
                sb.append(elem.getAsString());
                sb.append(' ');
            }
            sb.append(javaArgs);
            newProfile.addProperty(TAG_JAVA_ARGS, sb.toString());
        }
        JsonUtils.writeJson(json, ProfileList.getProfilesPath());
        return JsonUtils.newGson().fromJson(newProfile, Profile.class);
    }

    public static Profile newUnmoddedProfile(String newName) {
        JsonObject json = JsonUtils.parseJson(ProfileList.getProfilesPath());
        if (json == null) {
            return null;
        }
        JsonObject profiles = json.getAsJsonObject(TAG_PROFILES);
        if (profiles == null) {
            return null;
        }
        Profile profile = new Profile();
        profile.name = newName;
        profile.allowedReleaseTypes.add(ALLOW_SNAPSHOT);
        profile.allowedReleaseTypes.add(ALLOW_RELEASE);
        profiles.add(newName, JsonUtils.newGson().toJsonTree(profile, Profile.class));
        JsonUtils.writeJson(json, ProfileList.getProfilesPath());
        return profile;
    }

    public void delete(String newProfile) {
        JsonObject json = JsonUtils.parseJson(ProfileList.getProfilesPath());
        if (json == null) {
            return;
        }
        JsonObject profiles = json.getAsJsonObject(TAG_PROFILES);
        if (profiles == null) {
            return;
        }
        profiles.remove(name);
        JsonPrimitive prim = json.getAsJsonPrimitive(TAG_SELECTED_PROFILE);
        if (prim != null && prim.isString() && name.equals(prim.getAsString())) {
            json.addProperty(TAG_SELECTED_PROFILE, newProfile);
        }
        JsonUtils.writeJson(json, ProfileList.getProfilesPath());
    }

    public File getJavaExe() {
        if (javaDir == null) {
            return null;
        }
        return new File(new File(javaDir, "bin"), isWindows ? "javaw.exe" : "java");
    }

    public boolean getJavaArguments(List<String> cmdLine) {
        boolean added = false;
        if (!MCPatcherUtils.isNullOrEmpty(javaArgs)) {
            for (String s : javaArgs.split("\\s+")) {
                if (!s.isEmpty()) {
                    cmdLine.add(s);
                    added = true;
                }
            }
        }
        return added;
    }

    public void setGameArguments(Map<String, String> args, ProfileList profileList) {
        args.put("profile_name", name);
        if (gameDir == null || gameDir.isEmpty()) {
            args.put("game_directory", MCPatcherUtils.getMinecraftPath().getAbsolutePath());
        } else {
            args.put("game_directory", gameDir);
        }
        Authentication authentication = profileList.authenticationDatabase.get(playerUUID);
        if (authentication == null) {
            authentication = profileList.authenticationDatabase.get(profileList.selectedUser);
        }
        if (authentication != null) {
            if (authentication.username != null) {
                args.put("auth_player_name", authentication.username);
            }
            if (authentication.accessToken != null && authentication.uuid != null) {
                args.put("auth_session", "token:" + authentication.accessToken + ':' + authentication.uuid);
            }
            if (authentication.uuid != null) {
                args.put("auth_uuid", authentication.uuid);
            }
            if (authentication.accessToken != null) {
                args.put("auth_access_token", authentication.accessToken);
            }
        }
        args.put("user_properties", "{}");
    }

    public void addGameArguments(Map<String, String> args, List<String> cmdLine) {
        if (resolution != null && resolution.width > 0 && resolution.height > 0) {
            cmdLine.add("--width");
            cmdLine.add(String.valueOf(resolution.width));
            cmdLine.add("--height");
            cmdLine.add(String.valueOf(resolution.height));
        }
    }

    public File getGameDir() {
        if (gameDir != null) {
            File dir = new File(gameDir);
            if (dir.isDirectory()) {
                return dir;
            }
        }
        return MCPatcherUtils.getMinecraftPath();
    }

    private boolean isAllowed(String type) {
        for (String s : allowedReleaseTypes) {
            if (type.equalsIgnoreCase(s)) {
                return true;
            }
        }
        return false;
    }

    public String getLastVersionId() {
        return lastVersionId;
    }

    public boolean isReleaseAllowed() {
        return true;
    }

    public boolean isSnapshotAllowed() {
        return isAllowed(ALLOW_SNAPSHOT);
    }
}
