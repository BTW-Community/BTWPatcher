package com.prupe.mcpatcher.mod;

import com.prupe.mcpatcher.Config;
import com.prupe.mcpatcher.MCLogger;
import com.prupe.mcpatcher.MCPatcherUtils;
import org.lwjgl.opengl.PixelFormat;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

public class AAHelper {
    private static final MCLogger logger = MCLogger.getLogger(MCPatcherUtils.MIPMAP);

    private static final int BORDER_COLOR = 0;

    private static final int aaSamples = Config.getInt(MCPatcherUtils.EXTENDED_HD, "antiAliasing", 1);

    public static int border;
    static int maxBorder;

    static void reset() {
        maxBorder = 0;
    }

    public static PixelFormat setupPixelFormat(PixelFormat pixelFormat) {
        if (aaSamples > 1) {
            logger.config("setting AA samples to %d", aaSamples);
            return pixelFormat.withSamples(aaSamples);
        } else {
            return pixelFormat;
        }
    }

    public static BufferedImage addBorder(String name, BufferedImage input, boolean isAnimation) {
        if (input == null) {
            border = 0;
            return input;
        }
        int width = input.getWidth();
        int height = input.getHeight();
        int numFrames;
        if (isAnimation && height % width == 0) {
            numFrames = height / width;
            height = width;
        } else {
            numFrames = 1;
        }
        setupBorder(input, width, height);
        if (border <= 0) {
            logger.finer("no border around %s", name);
            return input;
        }
        logger.finer("adding %d pixel border around %s", border, name);
        int newWidth = width + 2 * border;
        int newHeight = height + 2 * border;
        BufferedImage output = new BufferedImage(newWidth, numFrames * newHeight, BufferedImage.TYPE_INT_ARGB);
        for (int frame = 0; frame < numFrames; frame++) {
            int sy = frame * height;
            int dy = frame * newHeight;

            copyRegion(input, 0, sy, output, 0, dy, border, border, true, true);
            copyRegion(input, 0, sy, output, border, dy, width, border, false, true);
            copyRegion(input, width - border, sy, output, width + border, dy, border, border, true, true);

            copyRegion(input, 0, sy, output, 0, dy + border, border, width, true, false);
            copyRegion(input, 0, sy, output, border, dy + border, width, height, false, false);
            copyRegion(input, width - border, sy, output, width + border, dy + border, border, width, true, false);

            copyRegion(input, 0, sy + height - border, output, 0, dy + height + border, border, border, true, true);
            copyRegion(input, 0, sy + height - border, output, border, dy + height + border, width, border, false, true);
            copyRegion(input, width - border, sy + height - border, output, width + border, dy + height + border, border, border, true, true);

            addDebugOutline(output, dy, width, height);
        }
        return output;
    }

    private static void setupBorder(BufferedImage input, int width, int height) {
        if (input == null) {
            border = 0;
        } else if (MipmapHelper.mipmapEnabled && MipmapHelper.maxMipmapLevel > 0) {
            border = 1 << Math.max(Math.min(MipmapHelper.maxMipmapLevel, 4), 0);
        } else if (aaSamples > 1 || MipmapHelper.anisoLevel > 1) {
            border = 2;
        } else {
            border = 0;
        }
        if (border > 0) {
            int edgeCount = 0;
            for (int i = 0; i < width; i++) {
                if ((input.getRGB(i, 0) & 0xff000000) != 0) {
                    edgeCount++;
                }
                if ((input.getRGB(i, height - 1) & 0xff000000) != 0) {
                    edgeCount++;
                }
            }
            for (int i = 1; i < height - 1; i++) {
                if ((input.getRGB(0, i) & 0xff000000) != 0) {
                    edgeCount++;
                }
                if ((input.getRGB(width - 1, i) & 0xff000000) != 0) {
                    edgeCount++;
                }
            }
            if ((float) edgeCount / (float) (2 * (width + height)) < 0.3f) {
                border = 0;
            }
        }
        maxBorder = Math.max(border, maxBorder);
    }

    private static void copyRegion(BufferedImage input, int sx, int sy, BufferedImage output, int dx, int dy, int w, int h, boolean flipX, boolean flipY) {
        int[] rgb = new int[w * h];
        input.getRGB(sx, sy, w, h, rgb, 0, w);
        if (flipX || flipY) {
            int[] rgbFlipped = new int[w * h];
            for (int i = 0; i < w; i++) {
                for (int j = 0; j < h; j++) {
                    rgbFlipped[w * j + i] = rgb[w * (flipY ? h - 1 - j : j) + (flipX ? w - 1 - i : i)];
                }
            }
            output.setRGB(dx, dy, w, h, rgbFlipped, 0, w);
        } else {
            output.setRGB(dx, dy, w, h, rgb, 0, w);
        }
    }

    private static void addDebugOutline(BufferedImage output, int dy, int width, int height) {
        if (BORDER_COLOR != 0) {
            for (int i = 0; i < width; i++) {
                output.setRGB(i + border, dy + border, BORDER_COLOR);
                output.setRGB(i + border, dy + height + border, BORDER_COLOR);
            }
            for (int i = 0; i < height; i++) {
                output.setRGB(border, dy + i + border, BORDER_COLOR);
                output.setRGB(height + border, dy + i + border, BORDER_COLOR);
            }
        }
    }
}
