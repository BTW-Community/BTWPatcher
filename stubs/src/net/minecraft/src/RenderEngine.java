package net.minecraft.src;

import java.awt.image.BufferedImage;

public class RenderEngine {
    public TexturePackList texturePackList;

    public void bindTexture(String s) {
    }

    public void clearBoundTexture() {
    }

    public void deleteTexture(int texture) {
    }

    public void createTextureFromBytes(int[] rgb, int width, int height, int texture) {
    }

    public int[] readTextureImageData(String s) {
        return null;
    }

    public int allocateAndSetupTexture(BufferedImage image) {
        return -1;
    }

    public void setupTexture(BufferedImage image, int texture) {
    }

    public void setupTextureWithFlags(BufferedImage image, int texture, boolean blurTexture, boolean clampTexture) { // 13w09a+
    }

    public void refreshTextures() {
    }
}
