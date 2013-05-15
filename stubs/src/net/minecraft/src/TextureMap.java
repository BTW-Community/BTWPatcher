package net.minecraft.src;

import java.util.HashMap;

public class TextureMap implements IconRegister {
    public HashMap<String, TextureStitched> mapTexturesStitched;

    public TextureMap(int type, String basePath) {
    }

    public Icon registerIcon(String name) {
        return null;
    }

    public Texture getTexture() {
        return null;
    }

    public void refreshTextures(ITexturePack texturePack) {
    }

    public void updateAnimations() {
    }
}
