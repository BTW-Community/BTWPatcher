package net.minecraft.src;

import java.io.File;
import java.io.InputStream;

public class TexturePackImplementation implements ITexturePack {
    public File texturePackFile;

    public void deleteTexturePack(TextureManager textureManager) {
    }

    public void bindThumbnailTexture(TextureManager textureManager) {
    }

    public InputStream getResourceAsStream(String resource) {
        return null;
    }

    public InputStream getResourceAsStream2(String resource, boolean useDefault) {
        return null;
    }

    public String getTexturePackID() {
        return null;
    }

    public String getTexturePackFileName() {
        return null;
    }

    public String getFirstDescriptionLine() {
        return null;
    }

    public String getSecondDescriptionLine() {
        return null;
    }

    public boolean hasResource(String resource, boolean useDefault) {
        return false;
    }

    public boolean isCompatible() {
        return false;
    }
}
