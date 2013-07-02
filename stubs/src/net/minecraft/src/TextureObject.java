package net.minecraft.src;

public interface TextureObject {
    void load(ResourceManager resourceManager);

    int getGLTexture();
}
