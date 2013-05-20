package com.prupe.mcpatcher.mob;

import com.prupe.mcpatcher.Config;
import com.prupe.mcpatcher.MCLogger;
import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.TexturePackAPI;
import net.minecraft.src.Tessellator;

public class LineRenderer {
    private static final MCLogger logger = MCLogger.getLogger(MCPatcherUtils.RANDOM_MOBS);

    private static final boolean enable = Config.getBoolean(MCPatcherUtils.RANDOM_MOBS, "leashLine", true);
    private static final LineRenderer[] renderers = new LineRenderer[] {
        new LineRenderer("fishing_line", 0.0075, 0.0, 0.0, 16),
        new LineRenderer("lead", 0.025, 4.0 / 3.0, 0.125, 24),
    };

    private final String texture;
    private final double width;
    private final double a;
    private final double b;
    private final int segments;
    private boolean active;

    public static boolean renderLine(int type, double x, double y, double z, double dx, double dy, double dz) {
        return renderers[type].render(x, y, z, dx, dy, dz);
    }

    static void reset() {
        for (LineRenderer renderer : renderers) {
            renderer.reset1();
        }
    }

    LineRenderer(String name, double width, double a, double b, int segments) {
        texture = MCPatcherUtils.TEXTURE_PACK_PREFIX + "item/" + name + ".png";
        this.width = width;
        this.a = a;
        this.b = b;
        this.segments = segments;
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
        double x0 = x;
        double y0 = y;
        double z0 = z;
        for (int i = 1; i <= segments; i++) {
            double s = i / (double) segments;
            double x1 = x + s * dx;
            double y1 = y + (s * s + s) * dy * 0.5 + a * (1.0 - s) + b;
            double z1 = z + s * dz;

            tessellator.addVertexWithUV(x0, y0, z0, 0.0, 0.0);
            tessellator.addVertexWithUV(x1, y1, z1, 1.0, 0.0);
            tessellator.addVertexWithUV(x1, y1 + width, z1, 1.0, 1.0);
            tessellator.addVertexWithUV(x0, y0 + width, z0, 0.0, 1.0);

            tessellator.addVertexWithUV(x0, y0 + width, z0, 0.0, 1.0);
            tessellator.addVertexWithUV(x1, y1 + width, z1, 1.0, 1.0);
            tessellator.addVertexWithUV(x1, y1, z1, 1.0, 0.0);
            tessellator.addVertexWithUV(x0, y0, z0, 0.0, 0.0);

            x0 = x1;
            y0 = y1;
            z0 = z1;

        }
        tessellator.draw();
        return true;
    }

    @Override
    public String toString() {
        return "LineRenderer{" + texture + ", " + width + "}";
    }
}
