package net.minecraft.src;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SimpleReloadableResourceManager implements ReloadableResourceManager {
    public Map<String, FallbackResourceManager> namespaceMap; // made public by __TexturePackBase

    public void refreshResourcePacks(List<ResourcePack> resourcePacks) {
    }

    public void registerLoadableResource(ResourceManagerReloadListener resource) {
    }

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
