package com.prupe.mcpatcher.mal.block;

import net.minecraft.src.Block;

public class RenderPassAPI {
    public static RenderPassAPI instance = new RenderPassAPI();

    public boolean skipDefaultRendering(Block block) {
        return false;
    }

    public boolean skipThisRenderPass(Block block, int pass) {
        return pass > 2;
    }

    public boolean useColorMultiplierThisPass(Block block) {
        return true;
    }

    public void clear() {
    }

    public void setRenderPassForBlock(Block block, int pass) {
    }

    public void finish() {
    }
}
