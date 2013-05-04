package net.minecraft.src;

public interface ILoadableTexture {
    void load(ITexturePack texturePack);

    int getGLTexture();
}
