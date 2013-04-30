package net.minecraft.src;

import java.awt.image.BufferedImage;
import java.util.HashMap;

public class TextureMap implements IconRegister {
    public HashMap<String, TextureStitched> mapTexturesStitched;

    public TextureMap(int type, String basePath, String textureExt, BufferedImage missingImage) {
    }

    public Icon registerIcon(String name) {
        return null;
    }

    public Texture getTexture() {
        return null;
    }

    public void refreshTextures() {
    }

    public void updateAnimations() {
    }
}
