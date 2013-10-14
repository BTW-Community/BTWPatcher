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
    private static double u1; // 7/16 point
    private static double u2; // 9/16 point
    private static double u3; // right edge
    private static double v0; // top edge
    private static double v1; // bottom edge

    private static double u1Scaled;
    private static double u2Scaled;

    public static void renderThin(RenderBlocks renderBlocks, Block blockPane, Icon origIcon, int i, int j, int k,
                                  boolean connectNorth, boolean connectSouth, boolean connectWest, boolean connectEast) {
        if (setupIcons(renderBlocks, blockPane, origIcon, i, j, k)) {
            render(i, j, k, connectNorth, connectSouth, connectWest, connectEast, 0.0, 0.0, 0.0);
        }
    }

    public static void renderThick(RenderBlocks renderBlocks, Block blockPane, Icon origIcon, int i, int j, int k,
                                   boolean connectNorth, boolean connectSouth, boolean connectWest, boolean connectEast) {
        if (setupIcons(renderBlocks, blockPane, origIcon, i, j, k)) {
            render(i, j, k, connectNorth, connectSouth, connectWest, connectEast, 0.0625, 1.0, 0.001);
        }
    }

    private static boolean setupIcons(RenderBlocks renderBlocks, Block blockPane, Icon origIcon, int i, int j, int k) {
        if (!enable) {
            active = false;
            return false;
        }
        active = false;
        for (int face = TileOverride.NORTH_FACE; face <= TileOverride.EAST_FACE; face++) {
            icons[face] = CTMUtils.getTile(renderBlocks, blockPane, i, j, k, face, origIcon, Tessellator.instance);
            if (icons[face] == null) {
                active = RenderPassAPI.instance.skipDefaultRendering(blockPane);
                return false;
            } else if (icons[face] != origIcon) {
                active = true;
            }
        }
        return active;
    }

    private static void render(int i, int j, int k,
                               boolean connectNorth, boolean connectSouth, boolean connectWest, boolean connectEast,
                               double thickness, double uOffset, double yOffset) {
        final double i0 = i;
        final double i1 = i0 + 0.5 - thickness;
        final double i2 = i0 + 0.5 + thickness;
        final double i3 = i0 + 1.0;
        final double j0 = j + yOffset;
        final double j1 = j + 1.0 - yOffset;
        final double k0 = k;
        final double k1 = k0 + 0.5 - thickness;
        final double k2 = k0 + 0.5 + thickness;
        final double k3 = k0 + 1.0;

        u1Scaled = 8.0 - uOffset;
        u2Scaled = 8.0 + uOffset;

        if (!connectNorth && !connectSouth && !connectWest && !connectEast) {
            connectNorth = connectSouth = connectWest = connectEast = true;
        }

        if (connectEast && connectWest) {
            // full west-east pane
            setupTileCoords(TileOverride.SOUTH_FACE);
            tessellator.addVertexWithUV(i0, j1, k2, u0, v0);
            tessellator.addVertexWithUV(i0, j0, k2, u0, v1);
            tessellator.addVertexWithUV(i3, j0, k2, u3, v1);
            tessellator.addVertexWithUV(i3, j1, k2, u3, v0);

            setupTileCoords(TileOverride.NORTH_FACE);
            tessellator.addVertexWithUV(i3, j1, k1, u0, v0);
            tessellator.addVertexWithUV(i3, j0, k1, u0, v1);
            tessellator.addVertexWithUV(i0, j0, k1, u3, v1);
            tessellator.addVertexWithUV(i0, j1, k1, u3, v0);
        } else if (connectWest) {
            // west half-pane
            setupTileCoords(TileOverride.SOUTH_FACE);
            if (connectSouth) {
                tessellator.addVertexWithUV(i0, j1, k2, u2, v0);
                tessellator.addVertexWithUV(i0, j0, k2, u2, v1);
                tessellator.addVertexWithUV(i1, j0, k2, u3, v1);
                tessellator.addVertexWithUV(i1, j1, k2, u3, v0);
            } else {
                tessellator.addVertexWithUV(i0, j1, k2, u1, v0);
                tessellator.addVertexWithUV(i0, j0, k2, u1, v1);
                tessellator.addVertexWithUV(i2, j0, k2, u3, v1);
                tessellator.addVertexWithUV(i2, j1, k2, u3, v0);
            }

            setupTileCoords(TileOverride.NORTH_FACE);
            if (connectNorth) {
                tessellator.addVertexWithUV(i1, j1, k1, u0, v0);
                tessellator.addVertexWithUV(i1, j0, k1, u0, v1);
                tessellator.addVertexWithUV(i0, j0, k1, u1, v1);
                tessellator.addVertexWithUV(i0, j1, k1, u1, v0);
            } else {
                tessellator.addVertexWithUV(i2, j1, k1, u0, v0);
                tessellator.addVertexWithUV(i2, j0, k1, u0, v1);
                tessellator.addVertexWithUV(i0, j0, k1, u2, v1);
                tessellator.addVertexWithUV(i0, j1, k1, u2, v0);
            }
        } else if (connectEast) {
            // east half-pane
            setupTileCoords(TileOverride.SOUTH_FACE);
            if (connectSouth) {
                tessellator.addVertexWithUV(i2, j1, k2, u0, v0);
                tessellator.addVertexWithUV(i2, j0, k2, u0, v1);
                tessellator.addVertexWithUV(i3, j0, k2, u1, v1);
                tessellator.addVertexWithUV(i3, j1, k2, u1, v0);
            } else {
                tessellator.addVertexWithUV(i1, j1, k2, u0, v0);
                tessellator.addVertexWithUV(i1, j0, k2, u0, v1);
                tessellator.addVertexWithUV(i3, j0, k2, u2, v1);
                tessellator.addVertexWithUV(i3, j1, k2, u2, v0);
            }

            setupTileCoords(TileOverride.NORTH_FACE);
            if (connectNorth) {
                tessellator.addVertexWithUV(i3, j1, k1, u2, v0);
                tessellator.addVertexWithUV(i3, j0, k1, u2, v1);
                tessellator.addVertexWithUV(i2, j0, k1, u3, v1);
                tessellator.addVertexWithUV(i2, j1, k1, u3, v0);
            } else {
                tessellator.addVertexWithUV(i3, j1, k1, u1, v0);
                tessellator.addVertexWithUV(i3, j0, k1, u1, v1);
                tessellator.addVertexWithUV(i1, j0, k1, u3, v1);
                tessellator.addVertexWithUV(i1, j1, k1, u3, v0);
            }
        }

        if (connectNorth && connectSouth) {
            // full north-south pane
            setupTileCoords(TileOverride.WEST_FACE);
            tessellator.addVertexWithUV(i1, j1, k0, u0, v0);
            tessellator.addVertexWithUV(i1, j0, k0, u0, v1);
            tessellator.addVertexWithUV(i1, j0, k3, u3, v1);
            tessellator.addVertexWithUV(i1, j1, k3, u3, v0);

            setupTileCoords(TileOverride.EAST_FACE);
            tessellator.addVertexWithUV(i2, j1, k3, u0, v0);
            tessellator.addVertexWithUV(i2, j0, k3, u0, v1);
            tessellator.addVertexWithUV(i2, j0, k0, u3, v1);
            tessellator.addVertexWithUV(i2, j1, k0, u3, v0);
        } else if (connectNorth) {
            // north half-pane
            setupTileCoords(TileOverride.WEST_FACE);
            if (connectWest) {
                tessellator.addVertexWithUV(i1, j1, k0, u2, v0);
                tessellator.addVertexWithUV(i1, j0, k0, u2, v1);
                tessellator.addVertexWithUV(i1, j0, k1, u3, v1);
                tessellator.addVertexWithUV(i1, j1, k1, u3, v0);
            } else {
                tessellator.addVertexWithUV(i1, j1, k0, u1, v0);
                tessellator.addVertexWithUV(i1, j0, k0, u1, v1);
                tessellator.addVertexWithUV(i1, j0, k2, u3, v1);
                tessellator.addVertexWithUV(i1, j1, k2, u3, v0);
            }

            setupTileCoords(TileOverride.EAST_FACE);
            if (connectEast) {
                tessellator.addVertexWithUV(i2, j1, k1, u0, v0);
                tessellator.addVertexWithUV(i2, j0, k1, u0, v1);
                tessellator.addVertexWithUV(i2, j0, k0, u1, v1);
                tessellator.addVertexWithUV(i2, j1, k0, u1, v0);
            } else {
                tessellator.addVertexWithUV(i2, j1, k2, u0, v0);
                tessellator.addVertexWithUV(i2, j0, k2, u0, v1);
                tessellator.addVertexWithUV(i2, j0, k0, u2, v1);
                tessellator.addVertexWithUV(i2, j1, k0, u2, v0);
            }
        } else if (connectSouth) {
            // south half-pane
            setupTileCoords(TileOverride.WEST_FACE);
            if (connectWest) {
                tessellator.addVertexWithUV(i1, j1, k2, u0, v0);
                tessellator.addVertexWithUV(i1, j0, k2, u0, v1);
                tessellator.addVertexWithUV(i1, j0, k3, u1, v1);
                tessellator.addVertexWithUV(i1, j1, k3, u1, v0);
            } else {
                tessellator.addVertexWithUV(i1, j1, k1, u0, v0);
                tessellator.addVertexWithUV(i1, j0, k1, u0, v1);
                tessellator.addVertexWithUV(i1, j0, k3, u2, v1);
                tessellator.addVertexWithUV(i1, j1, k3, u2, v0);
            }

            setupTileCoords(TileOverride.EAST_FACE);
            if (connectEast) {
                tessellator.addVertexWithUV(i2, j1, k3, u2, v0);
                tessellator.addVertexWithUV(i2, j0, k3, u2, v1);
                tessellator.addVertexWithUV(i2, j0, k2, u3, v1);
                tessellator.addVertexWithUV(i2, j1, k2, u3, v0);
            } else {
                tessellator.addVertexWithUV(i2, j1, k3, u1, v0);
                tessellator.addVertexWithUV(i2, j0, k3, u1, v1);
                tessellator.addVertexWithUV(i2, j0, k1, u3, v1);
                tessellator.addVertexWithUV(i2, j1, k1, u3, v0);
            }
        }
    }


    private static void setupTileCoords(int face) {
        Icon icon = icons[face];
        tessellator = TessellatorUtils.getTessellator(Tessellator.instance, icons[face]);
        u0 = icon.getMinU();
        u1 = icon.getInterpolatedU(u1Scaled);
        u2 = icon.getInterpolatedU(u2Scaled);
        u3 = icon.getMaxU();
        v0 = icon.getMinV();
        v1 = icon.getMaxV();
    }

    static void clear() {
        Arrays.fill(icons, null);
        tessellator = null;
        active = false;
    }
}
