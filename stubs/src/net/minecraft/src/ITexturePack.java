package net.minecraft.src;

import java.io.InputStream;

public interface ITexturePack {
    void deleteTexturePack(TextureManager textureManager);

    void bindThumbnailTexture(TextureManager textureManager);

    InputStream getResourceAsStream(String resource);

    InputStream getResourceAsStream2(String resource, boolean useDefault);

    String getTexturePackID();

    String getTexturePackFileName();

    String getFirstDescriptionLine();

    String getSecondDescriptionLine();

    boolean hasResource(String resource, boolean useDefault);

    boolean isCompatible();
}
