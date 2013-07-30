package com.prupe.mcpatcher.launcher.profile;

import com.google.gson.Gson;
import com.prupe.mcpatcher.Config;
import com.prupe.mcpatcher.JsonUtils;
import com.prupe.mcpatcher.MCPatcherUtils;

import java.io.File;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

public class ProfileList {
    Map<String, Profile> profiles = new HashMap<String, Profile>();
    String selectedProfile;
    Map<String, Authentication> authenticationDatabase = new HashMap<String, Authentication>();

    public static ProfileList getProfileList() {
        return JsonUtils.parseJson(getProfilesPath(), ProfileList.class);
    }

    public static File getProfilesPath() {
        return MCPatcherUtils.getMinecraftPath(Config.LAUNCHER_JSON);
    }

    private ProfileList() {
    }

    @Override
    public String toString() {
        return String.format("ProfileList{%d profiles, selectedProfile=%s}", profiles.size(), selectedProfile);
    }

    public String getSelectedProfile() {
        return selectedProfile;
    }

    public Profile getProfile(String name) {
        return profiles.get(name);
    }

    public Map<String, Profile> getProfiles() {
        return profiles;
    }

    public void dump(PrintStream output) {
        output.println(toString());
        Gson gson = JsonUtils.newGson();
        gson.toJson(this, ProfileList.class, output);
        output.println();
    }
}
