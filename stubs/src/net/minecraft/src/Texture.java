package net.minecraft.src;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

// 1.5 only
public class Texture {
    public int textureMinFilter;
    public int textureMagFilter;
    public boolean mipmapActive;
    public ByteBuffer textureData;

    public int border; // added by Extended HD

    public int getGlTextureId() {
        return 0;
    }

    public int getWidth() {
        return 0;
    }

    public int getHeight() {
        return 0;
    }

    public String getTextureName() {
        return "";
    }

    public ByteBuffer getTextureData() {
        return null;
    }

    public void transferFromImage(BufferedImage image) {
    }

    public void unloadGLTexture() {
    }
}
