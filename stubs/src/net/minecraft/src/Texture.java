package net.minecraft.src;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

public class Texture {
    public int glMinFilter;
    public int glMagFilter;
    public boolean useMipmaps;
    public ByteBuffer byteBuffer;

    public int getGLTexture() {
        return 0;
    }

    public int getWidth() {
        return 0;
    }

    public int getHeight() {
        return 0;
    }

    public String getName() {
        return "";
    }

    public ByteBuffer getByteBuffer() {
        return null;
    }

    public void transferFromImage(BufferedImage image) {
    }

    public void unloadGLTexture() {
    }
}
