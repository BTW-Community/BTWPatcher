package com.prupe.mcpatcher.ctm;

import com.prupe.mcpatcher.MAL;
import net.minecraft.src.Block;

abstract public class RenderPassMap {
    static final RenderPassMap instance = MAL.newInstance(RenderPassMap.class, "renderpass");

    public static int map18To17(int pass) {
        return pass > 1 ? instance.MCPatcherToVanilla(pass) : pass;
    }

    public static int map17To18(int pass) {
        return pass <= 1 ? instance.vanillaToMCPatcher(pass) : pass;
    }

    abstract protected int vanillaToMCPatcher(int pass);

    abstract protected int MCPatcherToVanilla(int pass);

    abstract protected int getDefaultRenderPass(Block block);

    // pre-14w03a
    private static class V1 extends RenderPassMap {
        private static final int[] MAP = new int[]{0, 0, 0, 1, 2, 3};

        @Override
        protected int vanillaToMCPatcher(int pass) {
            for (int i = 0; i < MAP.length; i++) {
                if (MAP[i] == pass) {
                    return i;
                }
            }
            return -1;
        }

        @Override
        protected int MCPatcherToVanilla(int pass) {
            return pass >= 0 && pass < MAP.length ? MAP[pass] : -1;
        }

        @Override
        protected int getDefaultRenderPass(Block block) {
            return vanillaToMCPatcher(block.getRenderBlockPass());
        }
    }

    // 14w03a+
    private static class V2 extends RenderPassMap {
        @Override
        protected int vanillaToMCPatcher(int pass) {
            return pass;
        }

        @Override
        protected int MCPatcherToVanilla(int pass) {
            return pass;
        }

        @Override
        protected int getDefaultRenderPass(Block block) {
            return block.getRenderBlockPassEnum().ordinal();
        }
    }
}
