package net.minecraft.src;

public class TextureBase implements ITexture {
    public void load(ITexturePack texturePack) {
    }

    public int getGLTexture() {
        return -1;
    }

    public void unloadGLTexture() { // added by __TexturePackBase
    }
}
