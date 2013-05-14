package net.minecraft.src;

public class TextureBase implements ITexture {
    public int glTextureId; // made public by __TexturePackBase

    public void load(ITexturePack texturePack) {
    }

    public int getGLTexture() {
        return -1;
    }

    public void unloadGLTexture() { // added by __TexturePackBase
    }
}
