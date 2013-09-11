package com.prupe.mcpatcher.mal;

import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.Mod;

public class BiomeAPIMod extends Mod {
    private final int malVersion;

    public BiomeAPIMod() {
        name = MCPatcherUtils.BIOME_API_MOD;
        author = "MCPatcher";
        description = "Internal mod required by the patcher.";

        if (getMinecraftVersion().compareTo("13w36a") < 0) {
            malVersion = 1;
        } else {
            malVersion = 2;
        }
        version = String.valueOf(malVersion) + ".0";
        setMALVersion("biome", malVersion);

        addClassFile(MCPatcherUtils.BIOME_API_CLASS);
        addClassFile(MCPatcherUtils.BIOME_API_CLASS + "$V" + malVersion);
    }
}
