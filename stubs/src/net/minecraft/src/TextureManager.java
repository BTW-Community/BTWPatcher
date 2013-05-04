package net.minecraft.src;

import java.awt.image.BufferedImage;
import java.util.List;

public class TextureManager {
    public void bindTexture(String texture) {
    }

    public ILoadableTexture getTexture(String texture) {
        return null;
    }

    public void unloadTexture(String texture) {
    }

    // TODO: 1.5 stuff - remove
    public static TextureManager getInstance() {
        return null;
    }

    public Texture createTextureFromImage(String name, int slot, int width, int height, int wrapST, int pixelFormat, int minFilter, int magFilter, boolean flag, BufferedImage image) {
        return null;
    }

    public List<Texture> createTextureFromFile(String filename) {
        return null;
    }
}
