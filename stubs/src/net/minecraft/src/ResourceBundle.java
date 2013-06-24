package net.minecraft.src;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public class ResourceBundle implements IResourceBundle {
    public List<IResourcePack> resourcePacks; // made public by __TexturePackBase

    @Override
    public Set<String> getNamespaces() {
        return null;
    }

    public IResource getResource(ResourceAddress address) throws IOException {
        return null;
    }

    @Override
    public List<IResource> getMCMeta(ResourceAddress address) throws IOException {
        return null;
    }
}
