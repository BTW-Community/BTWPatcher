package net.minecraft.src;

public class AbstractTexture implements TextureObject {
    public int glTextureId; // made public by __TexturePackBase

    public void load(ResourceManager resourceManager) {
    }

    public int getGLTexture() {
        return -1;
    }

    public void unloadGLTexture() { // added by __TexturePackBase
    }
}
