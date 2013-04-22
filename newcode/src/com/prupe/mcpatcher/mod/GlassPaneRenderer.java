package com.prupe.mcpatcher.mod;

import com.prupe.mcpatcher.Config;
import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.TessellatorUtils;
import net.minecraft.src.Block;
import net.minecraft.src.Icon;
import net.minecraft.src.RenderBlocks;
import net.minecraft.src.Tessellator;

import java.util.Arrays;

public class GlassPaneRenderer {
    private static final boolean enable = Config.getBoolean(MCPatcherUtils.CONNECTED_TEXTURES, "glassPane", true);

    public static boolean active;

    private static final Icon[] icons = new Icon[6];
    private static Tessellator tessellator;

    private static double u0; // left edge
    private static double uM; // left-right midpoint 
    private static double u1; // right edge
    private static double v0; // top edge
    private static double v1; // bottom edge

    public static void render(RenderBlocks renderBlocks, Block blockPane, Icon origIcon, int i, int j, int k,
                              boolean connectNorth, boolean connectSouth, boolean connectWest, boolean connectEast) {
        if (!enable) {
            active = false;
            return;
        }
        active = true;
        for (int face = TileOverride.NORTH_FACE; face <= TileOverride.EAST_FACE; face++) {
            icons[face] = CTMUtils.getTile(renderBlocks, blockPane, i, j, k, face, origIcon, Tessellator.instance);
            if (icons[face] == null) {
                active = RenderPassAPI.instance.skipDefaultRendering(blockPane);
                return;
            }
        }

        final double i0 = i;
        final double iM = i0 + 0.5;
        final double i1 = i0 + 1.0;
        final double j0 = j;
        final double j1 = j0 + 1.0;
        final double k0 = k;
        final double kM = k0 + 0.5;
        final double k1 = k0 + 1.0;

        final boolean connectAny = connectWest || connectEast || connectNorth || connectSouth;

        if ((connectEast && connectWest) || !connectAny) {
            // full west-east pane
            setupTileCoords(TileOverride.SOUTH_FACE);
            tessellator.addVertexWithUV(i0, j1, kM, u0, v0);
            tessellator.addVertexWithUV(i0, j0, kM, u0, v1);
            tessellator.addVertexWithUV(i1, j0, kM, u1, v1);
            tessellator.addVertexWithUV(i1, j1, kM, u1, v0);

            setupTileCoords(TileOverride.NORTH_FACE);
            tessellator.addVertexWithUV(i1, j1, kM, u0, v0);
            tessellator.addVertexWithUV(i1, j0, kM, u0, v1);
            tessellator.addVertexWithUV(i0, j0, kM, u1, v1);
            tessellator.addVertexWithUV(i0, j1, kM, u1, v0);
        } else if (connectWest && !connectEast) {
            // west half-pane
            setupTileCoords(TileOverride.SOUTH_FACE);
            tessellator.addVertexWithUV(i0, j1, kM, uM, v0);
            tessellator.addVertexWithUV(i0, j0, kM, uM, v1);
            tessellator.addVertexWithUV(iM, j0, kM, u1, v1);
            tessellator.addVertexWithUV(iM, j1, kM, u1, v0);

            setupTileCoords(TileOverride.NORTH_FACE);
            tessellator.addVertexWithUV(iM, j1, kM, u0, v0);
            tessellator.addVertexWithUV(iM, j0, kM, u0, v1);
            tessellator.addVertexWithUV(i0, j0, kM, uM, v1);
            tessellator.addVertexWithUV(i0, j1, kM, uM, v0);
        } else if (!connectWest && connectEast) {
            // east half-pane
            setupTileCoords(TileOverride.SOUTH_FACE);
            tessellator.addVertexWithUV(iM, j1, kM, u0, v0);
            tessellator.addVertexWithUV(iM, j0, kM, u0, v1);
            tessellator.addVertexWithUV(i1, j0, kM, uM, v1);
            tessellator.addVertexWithUV(i1, j1, kM, uM, v0);

            setupTileCoords(TileOverride.NORTH_FACE);
            tessellator.addVertexWithUV(i1, j1, kM, uM, v0);
            tessellator.addVertexWithUV(i1, j0, kM, uM, v1);
            tessellator.addVertexWithUV(iM, j0, kM, u1, v1);
            tessellator.addVertexWithUV(iM, j1, kM, u1, v0);
        }

        if ((connectNorth && connectSouth) || !connectAny) {
            // full north-south pane
            setupTileCoords(TileOverride.WEST_FACE);
            tessellator.addVertexWithUV(iM, j1, k0, u0, v0);
            tessellator.addVertexWithUV(iM, j0, k0, u0, v1);
            tessellator.addVertexWithUV(iM, j0, k1, u1, v1);
            tessellator.addVertexWithUV(iM, j1, k1, u1, v0);

            setupTileCoords(TileOverride.EAST_FACE);
            tessellator.addVertexWithUV(iM, j1, k1, u0, v0);
            tessellator.addVertexWithUV(iM, j0, k1, u0, v1);
            tessellator.addVertexWithUV(iM, j0, k0, u1, v1);
            tessellator.addVertexWithUV(iM, j1, k0, u1, v0);
        } else if (connectNorth && !connectSouth) {
            // north half-pane
            setupTileCoords(TileOverride.WEST_FACE);
            tessellator.addVertexWithUV(iM, j1, k0, uM, v0);
            tessellator.addVertexWithUV(iM, j0, k0, uM, v1);
            tessellator.addVertexWithUV(iM, j0, kM, u1, v1);
            tessellator.addVertexWithUV(iM, j1, kM, u1, v0);

            setupTileCoords(TileOverride.EAST_FACE);
            tessellator.addVertexWithUV(iM, j1, kM, u0, v0);
            tessellator.addVertexWithUV(iM, j0, kM, u0, v1);
            tessellator.addVertexWithUV(iM, j0, k0, uM, v1);
            tessellator.addVertexWithUV(iM, j1, k0, uM, v0);
        } else if (!connectNorth && connectSouth) {
            // south half-pane
            setupTileCoords(TileOverride.WEST_FACE);
            tessellator.addVertexWithUV(iM, j1, kM, u0, v0);
            tessellator.addVertexWithUV(iM, j0, kM, u0, v1);
            tessellator.addVertexWithUV(iM, j0, k1, uM, v1);
            tessellator.addVertexWithUV(iM, j1, k1, uM, v0);

            setupTileCoords(TileOverride.EAST_FACE);
            tessellator.addVertexWithUV(iM, j1, k1, uM, v0);
            tessellator.addVertexWithUV(iM, j0, k1, uM, v1);
            tessellator.addVertexWithUV(iM, j0, kM, u1, v1);
            tessellator.addVertexWithUV(iM, j1, kM, u1, v0);
        }

        Arrays.fill(icons, null);
        tessellator = null;
    }

    private static void setupTileCoords(int face) {
        Icon icon = icons[face];
        tessellator = TessellatorUtils.getTessellator(Tessellator.instance, icons[face]);
        u0 = icon.getMinU();
        uM = icon.getInterpolatedU(8.0);
        u1 = icon.getMaxU();
        v0 = icon.getMinV();
        v1 = icon.getMaxV();
    }
}
