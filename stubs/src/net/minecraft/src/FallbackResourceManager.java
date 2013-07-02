package net.minecraft.src;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public class FallbackResourceManager implements ResourceManager {
    public List<ResourcePack> resourcePacks; // made public by __TexturePackBase

    @Override
    public Set<String> getNamespaces() {
        return null;
    }

    public Resource getResource(ResourceLocation address) throws IOException {
        return null;
    }

    @Override
    public List<Resource> getMCMeta(ResourceLocation address) throws IOException {
        return null;
    }
}
