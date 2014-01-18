package com.prupe.mcpatcher.mal.block;

import com.prupe.mcpatcher.ctm.RenderPass;
import net.minecraft.src.Block;

public class RenderPassAPI {
    public static RenderPassAPI instance = new RenderPassAPI();

    public int parseRenderPass(String value) {
        if (value.matches("\\d+")) {
            return Integer.parseInt(value);
        } else if (value.equalsIgnoreCase("backface") || value.equalsIgnoreCase("overlay")) {
            return RenderPass.MAX_EXTRA_RENDER_PASS;
        } else {
            return -1;
        }
    }

    public boolean skipDefaultRendering(Block block) {
        return false;
    }

    public boolean skipThisRenderPass(Block block, int pass) {
        return pass > 2;
    }

    public boolean useColorMultiplierThisPass(Block block) {
        return true;
    }

    public boolean useLightmapThisPass() {
        return true;
    }

    public void clear() {
    }

    public void setRenderPassForBlock(Block block, int pass) {
    }

    public void finish() {
    }
}
