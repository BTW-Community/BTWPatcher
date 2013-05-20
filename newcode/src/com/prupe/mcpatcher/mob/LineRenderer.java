package com.prupe.mcpatcher.mob;

import com.prupe.mcpatcher.Config;
import com.prupe.mcpatcher.MCLogger;
import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.TexturePackAPI;
import net.minecraft.src.Tessellator;

public class LineRenderer {
    private static final MCLogger logger = MCLogger.getLogger(MCPatcherUtils.RANDOM_MOBS);

    private static final boolean enable = Config.getBoolean(MCPatcherUtils.RANDOM_MOBS, "leashLine", true);
    private static final LineRenderer[] renderers = new LineRenderer[2];

    private final String texture;
    private final float width;
    private final int segments;
    private boolean active;

    static {
        new LineRenderer(0, "fishing_line", 0.01f, 16);
        new LineRenderer(1, "lead", 0.025f, 24);
    }

    public static boolean renderLine(int type, double x0, double y0, double z0, double x1, double y1, double z1) {
        return renderers[type].render(x0, y0, z0, x1, y1, z1);
    }

    static void reset() {
        for (LineRenderer renderer : renderers) {
            renderer.reset1();
        }
    }

    LineRenderer(int type, String name, float width, int segments) {
        texture = MCPatcherUtils.TEXTURE_PACK_PREFIX + "item/" + name + ".png";
        this.width = width;
        this.segments = segments;
        renderers[type] = this;
        logger.fine("%d: new %s", type, this);
    }

    void reset1() {
        active = enable && TexturePackAPI.hasResource(texture);
    }

    boolean render(double x0, double y0, double z0, double x1, double y1, double z1) {
        if (!active) {
            return false;
        }
        TexturePackAPI.bindTexture(texture);
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(x0, y0, z0, 0.0, 0.0);
        tessellator.addVertexWithUV(x1, y1, z1, 1.0, 0.0);
        tessellator.addVertexWithUV(x1, y1 + width, z1, 1.0, 1.0);
        tessellator.addVertexWithUV(x0, y0 + width, z0, 0.0, 1.0);
        tessellator.draw();
        return true;
    }

    @Override
    public String toString() {
        return "LineRenderer{" + texture + ", " + width + "}";
    }
}
