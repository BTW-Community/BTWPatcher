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
        new LineRenderer(0, "fishing_line", 0.0075f, 16);
        new LineRenderer(1, "lead", 0.025f, 24);
    }

    public static boolean renderLine(int type, double x, double y, double z, double dx, double dy, double dz) {
        return renderers[type].render(x, y, z, dx, dy, dz);
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
    }

    void reset1() {
        active = enable && TexturePackAPI.hasResource(texture);
        if (active) {
            logger.fine("using %s", this);
        } else {
            logger.fine("%s not found", this);
        }
    }

    boolean render(double x, double y, double z, double dx, double dy, double dz) {
        if (!active) {
            return false;
        }
        TexturePackAPI.bindTexture(texture);
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(x, y, z, 0.0, 0.0);
        tessellator.addVertexWithUV(x + dx, y + dy, z + dz, 1.0, 0.0);
        tessellator.addVertexWithUV(x + dx, y + dy + width, z + dz, 1.0, 1.0);
        tessellator.addVertexWithUV(x, y + width, z, 0.0, 1.0);
        tessellator.draw();
        return true;
    }

    @Override
    public String toString() {
        return "LineRenderer{" + texture + ", " + width + "}";
    }
}
