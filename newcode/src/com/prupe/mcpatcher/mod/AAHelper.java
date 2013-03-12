package com.prupe.mcpatcher.mod;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class AAHelper {
    private static final int borderSize = 8;

    public static void main(String[] args) {
        try {
            File inputFile = new File(args[0]);
            BufferedImage input = ImageIO.read(inputFile);
            System.out.printf("Read %s %dx%d\n", inputFile, input.getWidth(), input.getHeight());
            BufferedImage output = addBorder(input);
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

    public static BufferedImage addBorder(BufferedImage input) {
        if (borderSize <= 0) {
            return input;
        }
        final int width = input.getWidth();
        int height = input.getHeight();
        final int numFrames;
        if (height % width == 0) {
            numFrames = height / width;
            height = width;
        } else {
            numFrames = 1;
        }
        final int newWidth = width + 2 * borderSize;
        final int newHeight = height + 2 * borderSize;
        final BufferedImage output = new BufferedImage(newWidth, numFrames * newHeight, BufferedImage.TYPE_INT_ARGB);
        for (int frame = 0; frame < numFrames; frame++) {
            int sy = frame * height;
            int dy = frame * newHeight;

            copyRegion(input, width - borderSize, sy + height - borderSize, output, 0, dy, borderSize, borderSize);
            copyRegion(input, 0, sy + height - borderSize, output, borderSize, dy, width, borderSize);
            copyRegion(input, 0, sy + height - borderSize, output, width + borderSize, dy, borderSize, borderSize);

            copyRegion(input, width - borderSize, sy, output, 0, dy + borderSize, borderSize, width);
            copyRegion(input, 0, sy, output, borderSize, dy + borderSize, width, height);
            copyRegion(input, 0, sy, output, width + borderSize, dy + borderSize, borderSize, width);

            copyRegion(input, width - borderSize, sy, output, 0, dy + height + borderSize, borderSize, borderSize);
            copyRegion(input, 0, sy, output, borderSize, dy + height + borderSize, width, borderSize);
            copyRegion(input, 0, sy, output, width + borderSize, dy + height + borderSize, borderSize, borderSize);
        }
        return output;
    }

    private static void copyRegion(BufferedImage input, int sx, int sy, BufferedImage output, int dx, int dy, int w, int h) {
        int[] rgb = new int[w * h];
        input.getRGB(sx, sy, w, h, rgb, 0, w);
        output.setRGB(dx, dy, w, h, rgb, 0, w);
    }
}
