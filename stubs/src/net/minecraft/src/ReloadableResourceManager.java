package net.minecraft.src;

import java.util.List;

public interface ReloadableResourceManager extends ResourceManager {
    void refreshResourcePacks(List<ResourcePack> resourcePacks);

    void registerLoadableResource(ResourceManagerReloadListener resource);
}
