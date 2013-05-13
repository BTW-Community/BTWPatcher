package net.minecraft.src;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;

public class TextureManager {
    public Map<String, ITexture> texturesByName;

    public void bindTexture(String texture) {
    }

    public ITexture getTexture(String texture) {
        return null;
    }

    public void unloadTexture(String texture) {
    }

    public void addTexture(String name, ITexture texture) {
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
