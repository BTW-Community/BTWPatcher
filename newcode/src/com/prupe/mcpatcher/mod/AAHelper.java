package com.prupe.mcpatcher.mod;

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.PixelFormat;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

public class AAHelper {
    private static final int BORDER_COLOR = 0xffff0000;

    private static final int border = 8;
    private static final int aaSamples = 4;

    public static int lastBorder;

    public static void main(String[] args) {
        try {
            File inputFile = new File(args[0]);
            BufferedImage input = ImageIO.read(inputFile);
            System.out.printf("Read %s %dx%d\n", inputFile, input.getWidth(), input.getHeight());
            BufferedImage output = addBorder(input, true);
            File outputFile = new File(args[1]);
            if (output == input) {
                outputFile.delete();
                System.out.println("Image is identical");
            } else {
                ImageIO.write(output, "png", outputFile);
                System.out.printf("Wrote %s %dx%d\n", outputFile, output.getWidth(), output.getHeight());
            }
        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static PixelFormat setupPixelFormat(PixelFormat pixelFormat) throws LWJGLException {
        if (aaSamples > 1) {
            return pixelFormat.withSamples(aaSamples);
        } else {
            return pixelFormat;
        }
    }

    public static BufferedImage addBorder(BufferedImage input, boolean isAnimation) {
        if (border <= 0 || input == null) {
            lastBorder = 0;
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
        lastBorder = border;
        return output;
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
}
