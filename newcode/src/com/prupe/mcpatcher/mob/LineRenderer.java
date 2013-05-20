package com.prupe.mcpatcher.mob;

import com.prupe.mcpatcher.Config;
import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.TexturePackAPI;

public class LineRenderer {
    private static final String[] TEXTURE_NAME = new String[]{
        MCPatcherUtils.TEXTURE_PACK_PREFIX + "item/fishing_line.png",
        MCPatcherUtils.TEXTURE_PACK_PREFIX + "item/lead.png",
    };

    private static final boolean enable = Config.getBoolean(MCPatcherUtils.RANDOM_MOBS, "leashLine", true);
    private static final boolean[] enableLine = new boolean[TEXTURE_NAME.length];

    public static boolean renderLine(int type, double x0, double y0, double z0, double x1, double y1, double z1) {
        if (!enableLine[type]) {
            return false;
        }
        return false;
    }

    static void reset() {
        for (int i = 0; i < TEXTURE_NAME.length; i++) {
            enableLine[i] = enable && TexturePackAPI.hasResource(TEXTURE_NAME[i]);
        }
    }
}
