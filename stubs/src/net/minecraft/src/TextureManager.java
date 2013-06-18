package net.minecraft.src;

import java.util.Map;

public class TextureManager {
    public Map<ResourceAddress, ITexture> texturesByName;

    public void bindTexture(ResourceAddress texture) {
    }

    public ITexture getTexture(ResourceAddress texture) {
        return null;
    }

    public void unloadTexture(ResourceAddress texture) {
    }
}
