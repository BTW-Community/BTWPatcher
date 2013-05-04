package net.minecraft.src;

public interface ITexture {
    void load(ITexturePack texturePack);

    int getGLTexture();
}
