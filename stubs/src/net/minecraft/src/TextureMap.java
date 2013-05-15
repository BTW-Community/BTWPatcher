package net.minecraft.src;

import java.util.HashMap;

public class TextureMap extends TextureBase implements IStitchedTexture, IconRegister {
    public HashMap<String, TextureStitched> mapTexturesStitched;

    public TextureMap(int type, String basePath) {
    }

    public Icon registerIcon(String name) {
        return null;
    }

    public void refreshTextures1(ITexturePack texturePack) {
    }

    public void refreshTextures2(ITexturePack texturePack) {
    }

    public void updateAnimations() {
    }
}
