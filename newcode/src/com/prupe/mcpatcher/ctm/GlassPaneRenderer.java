package com.prupe.mcpatcher.ctm;

import com.prupe.mcpatcher.Config;
import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.TessellatorUtils;
import com.prupe.mcpatcher.mal.block.RenderPassAPI;
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
    private static double u7; // 7/16 point
    private static double uM; // left-right midpoint
    private static double u9; // 9/16 point
    private static double u1; // right edge
    private static double v0; // top edge
    private static double v1; // bottom edge

    public static void renderThin(RenderBlocks renderBlocks, Block blockPane, Icon origIcon, int i, int j, int k,
                                  boolean connectNorth, boolean connectSouth, boolean connectWest, boolean connectEast) {
        if (!setupIcons(renderBlocks, blockPane, origIcon, i, j, k)) {
            return;
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
            setupTileCoordsThin(TileOverride.SOUTH_FACE);
            tessellator.addVertexWithUV(i0, j1, kM, u0, v0);
            tessellator.addVertexWithUV(i0, j0, kM, u0, v1);
            tessellator.addVertexWithUV(i1, j0, kM, u1, v1);
            tessellator.addVertexWithUV(i1, j1, kM, u1, v0);

            setupTileCoordsThin(TileOverride.NORTH_FACE);
            tessellator.addVertexWithUV(i1, j1, kM, u0, v0);
            tessellator.addVertexWithUV(i1, j0, kM, u0, v1);
            tessellator.addVertexWithUV(i0, j0, kM, u1, v1);
            tessellator.addVertexWithUV(i0, j1, kM, u1, v0);
        } else if (connectWest && !connectEast) {
            // west half-pane
            setupTileCoordsThin(TileOverride.SOUTH_FACE);
            tessellator.addVertexWithUV(i0, j1, kM, uM, v0);
            tessellator.addVertexWithUV(i0, j0, kM, uM, v1);
            tessellator.addVertexWithUV(iM, j0, kM, u1, v1);
            tessellator.addVertexWithUV(iM, j1, kM, u1, v0);

            setupTileCoordsThin(TileOverride.NORTH_FACE);
            tessellator.addVertexWithUV(iM, j1, kM, u0, v0);
            tessellator.addVertexWithUV(iM, j0, kM, u0, v1);
            tessellator.addVertexWithUV(i0, j0, kM, uM, v1);
            tessellator.addVertexWithUV(i0, j1, kM, uM, v0);
        } else if (!connectWest && connectEast) {
            // east half-pane
            setupTileCoordsThin(TileOverride.SOUTH_FACE);
            tessellator.addVertexWithUV(iM, j1, kM, u0, v0);
            tessellator.addVertexWithUV(iM, j0, kM, u0, v1);
            tessellator.addVertexWithUV(i1, j0, kM, uM, v1);
            tessellator.addVertexWithUV(i1, j1, kM, uM, v0);

            setupTileCoordsThin(TileOverride.NORTH_FACE);
            tessellator.addVertexWithUV(i1, j1, kM, uM, v0);
            tessellator.addVertexWithUV(i1, j0, kM, uM, v1);
            tessellator.addVertexWithUV(iM, j0, kM, u1, v1);
            tessellator.addVertexWithUV(iM, j1, kM, u1, v0);
        }

        if ((connectNorth && connectSouth) || !connectAny) {
            // full north-south pane
            setupTileCoordsThin(TileOverride.WEST_FACE);
            tessellator.addVertexWithUV(iM, j1, k0, u0, v0);
            tessellator.addVertexWithUV(iM, j0, k0, u0, v1);
            tessellator.addVertexWithUV(iM, j0, k1, u1, v1);
            tessellator.addVertexWithUV(iM, j1, k1, u1, v0);

            setupTileCoordsThin(TileOverride.EAST_FACE);
            tessellator.addVertexWithUV(iM, j1, k1, u0, v0);
            tessellator.addVertexWithUV(iM, j0, k1, u0, v1);
            tessellator.addVertexWithUV(iM, j0, k0, u1, v1);
            tessellator.addVertexWithUV(iM, j1, k0, u1, v0);
        } else if (connectNorth && !connectSouth) {
            // north half-pane
            setupTileCoordsThin(TileOverride.WEST_FACE);
            tessellator.addVertexWithUV(iM, j1, k0, uM, v0);
            tessellator.addVertexWithUV(iM, j0, k0, uM, v1);
            tessellator.addVertexWithUV(iM, j0, kM, u1, v1);
            tessellator.addVertexWithUV(iM, j1, kM, u1, v0);

            setupTileCoordsThin(TileOverride.EAST_FACE);
            tessellator.addVertexWithUV(iM, j1, kM, u0, v0);
            tessellator.addVertexWithUV(iM, j0, kM, u0, v1);
            tessellator.addVertexWithUV(iM, j0, k0, uM, v1);
            tessellator.addVertexWithUV(iM, j1, k0, uM, v0);
        } else if (!connectNorth && connectSouth) {
            // south half-pane
            setupTileCoordsThin(TileOverride.WEST_FACE);
            tessellator.addVertexWithUV(iM, j1, kM, u0, v0);
            tessellator.addVertexWithUV(iM, j0, kM, u0, v1);
            tessellator.addVertexWithUV(iM, j0, k1, uM, v1);
            tessellator.addVertexWithUV(iM, j1, k1, uM, v0);

            setupTileCoordsThin(TileOverride.EAST_FACE);
            tessellator.addVertexWithUV(iM, j1, k1, uM, v0);
            tessellator.addVertexWithUV(iM, j0, k1, uM, v1);
            tessellator.addVertexWithUV(iM, j0, kM, u1, v1);
            tessellator.addVertexWithUV(iM, j1, kM, u1, v0);
        }

        clear();
    }

    public static void renderThick(RenderBlocks renderBlocks, Block blockPane, Icon origIcon, int i, int j, int k,
                                   boolean connectNorth, boolean connectSouth, boolean connectWest, boolean connectEast) {
        if (!setupIcons(renderBlocks, blockPane, origIcon, i, j, k)) {
            return;
        }

        final double i0 = i;
        final double i7 = i0 + 0.5 - 0.0625;
        final double i9 = i0 + 0.5 + 0.0625;
        final double i1 = i0 + 1.0;
        final double j0 = j + 0.001;
        final double j1 = j + 0.999;
        final double k0 = k;
        final double k7 = k0 + 0.5 - 0.0625;
        final double k9 = k0 + 0.5 + 0.0625;
        final double k1 = k0 + 1.0;

        final boolean connectAny = connectWest || connectEast || connectNorth || connectSouth;

        if ((connectEast && connectWest) || !connectAny) {
            // full west-east pane
            setupTileCoordsThick(TileOverride.SOUTH_FACE);
            tessellator.addVertexWithUV(i0, j1, k7, u0, v0);
            tessellator.addVertexWithUV(i0, j0, k7, u0, v1);
            tessellator.addVertexWithUV(i1, j0, k7, u1, v1);
            tessellator.addVertexWithUV(i1, j1, k7, u1, v0);

            setupTileCoordsThick(TileOverride.NORTH_FACE);
            tessellator.addVertexWithUV(i1, j1, k9, u0, v0);
            tessellator.addVertexWithUV(i1, j0, k9, u0, v1);
            tessellator.addVertexWithUV(i0, j0, k9, u1, v1);
            tessellator.addVertexWithUV(i0, j1, k9, u1, v0);
        } else if (connectWest && !connectEast) {
            // west half-pane
            setupTileCoordsThick(TileOverride.SOUTH_FACE);
            tessellator.addVertexWithUV(i0, j1, k7, u9, v0);
            tessellator.addVertexWithUV(i0, j0, k7, u9, v1);
            tessellator.addVertexWithUV(i7, j0, k7, u1, v1);
            tessellator.addVertexWithUV(i7, j1, k7, u1, v0);

            setupTileCoordsThick(TileOverride.NORTH_FACE);
            tessellator.addVertexWithUV(i7, j1, k9, u0, v0);
            tessellator.addVertexWithUV(i7, j0, k9, u0, v1);
            tessellator.addVertexWithUV(i0, j0, k9, u7, v1);
            tessellator.addVertexWithUV(i0, j1, k9, u7, v0);
        } else if (!connectWest && connectEast) {
            // east half-pane
            setupTileCoordsThick(TileOverride.SOUTH_FACE);
            tessellator.addVertexWithUV(i9, j1, k7, u0, v0);
            tessellator.addVertexWithUV(i9, j0, k7, u0, v1);
            tessellator.addVertexWithUV(i1, j0, k7, u7, v1);
            tessellator.addVertexWithUV(i1, j1, k7, u7, v0);

            setupTileCoordsThick(TileOverride.NORTH_FACE);
            tessellator.addVertexWithUV(i1, j1, k9, u9, v0);
            tessellator.addVertexWithUV(i1, j0, k9, u9, v1);
            tessellator.addVertexWithUV(i9, j0, k9, u1, v1);
            tessellator.addVertexWithUV(i9, j1, k9, u1, v0);
        }

        if ((connectNorth && connectSouth) || !connectAny) {
            // full north-south pane
            setupTileCoordsThick(TileOverride.WEST_FACE);
            tessellator.addVertexWithUV(i7, j1, k0, u0, v0);
            tessellator.addVertexWithUV(i7, j0, k0, u0, v1);
            tessellator.addVertexWithUV(i7, j0, k1, u1, v1);
            tessellator.addVertexWithUV(i7, j1, k1, u1, v0);

            setupTileCoordsThick(TileOverride.EAST_FACE);
            tessellator.addVertexWithUV(i9, j1, k1, u0, v0);
            tessellator.addVertexWithUV(i9, j0, k1, u0, v1);
            tessellator.addVertexWithUV(i9, j0, k0, u1, v1);
            tessellator.addVertexWithUV(i9, j1, k0, u1, v0);
        } else if (connectNorth && !connectSouth) {
            // north half-pane
            setupTileCoordsThick(TileOverride.WEST_FACE);
            tessellator.addVertexWithUV(i7, j1, k0, u9, v0);
            tessellator.addVertexWithUV(i7, j0, k0, u9, v1);
            tessellator.addVertexWithUV(i7, j0, k7, u1, v1);
            tessellator.addVertexWithUV(i7, j1, k7, u1, v0);

            setupTileCoordsThick(TileOverride.EAST_FACE);
            tessellator.addVertexWithUV(i9, j1, k7, u0, v0);
            tessellator.addVertexWithUV(i9, j0, k7, u0, v1);
            tessellator.addVertexWithUV(i9, j0, k0, u7, v1);
            tessellator.addVertexWithUV(i9, j1, k0, u7, v0);
        } else if (!connectNorth && connectSouth) {
            // south half-pane
            setupTileCoordsThick(TileOverride.WEST_FACE);
            tessellator.addVertexWithUV(i7, j1, k9, u0, v0);
            tessellator.addVertexWithUV(i7, j0, k9, u0, v1);
            tessellator.addVertexWithUV(i7, j0, k1, u7, v1);
            tessellator.addVertexWithUV(i7, j1, k1, u7, v0);

            setupTileCoordsThick(TileOverride.EAST_FACE);
            tessellator.addVertexWithUV(i9, j1, k1, u9, v0);
            tessellator.addVertexWithUV(i9, j0, k1, u9, v1);
            tessellator.addVertexWithUV(i9, j0, k9, u1, v1);
            tessellator.addVertexWithUV(i9, j1, k9, u1, v0);
        }

        clear();
    }

    private static boolean setupIcons(RenderBlocks renderBlocks, Block blockPane, Icon origIcon, int i, int j, int k) {
        if (!enable) {
            active = false;
            return false;
        }
        active = true;
        for (int face = TileOverride.NORTH_FACE; face <= TileOverride.EAST_FACE; face++) {
            icons[face] = CTMUtils.getTile(renderBlocks, blockPane, i, j, k, face, origIcon, Tessellator.instance);
            if (icons[face] == null) {
                active = RenderPassAPI.instance.skipDefaultRendering(blockPane);
                return false;
            }
        }
        return true;
    }

    private static void setupTileCoordsThin(int face) {
        Icon icon = icons[face];
        tessellator = TessellatorUtils.getTessellator(Tessellator.instance, icons[face]);
        u0 = icon.getMinU();
        uM = icon.getInterpolatedU(8.0);
        u1 = icon.getMaxU();
        v0 = icon.getMinV();
        v1 = icon.getMaxV();
    }

    private static void setupTileCoordsThick(int face) {
        Icon icon = icons[face];
        tessellator = TessellatorUtils.getTessellator(Tessellator.instance, icons[face]);
        u0 = icon.getMinU();
        u7 = icon.getInterpolatedU(7.0);
        u9 = icon.getInterpolatedU(9.0);
        u1 = icon.getMaxU();
        v0 = icon.getMinV();
        v1 = icon.getMaxV();
    }

    private static void clear() {
        Arrays.fill(icons, null);
        tessellator = null;
    }
}
