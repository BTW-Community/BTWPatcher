package net.minecraft.src;

public interface ITexture {
    void load(IResourceBundle resourceBundle);

    int getGLTexture();
}
