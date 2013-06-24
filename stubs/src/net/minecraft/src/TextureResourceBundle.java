package net.minecraft.src;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TextureResourceBundle implements ITextureResourceBundle {
    public Map<String, ResourceBundle> namespaceMap; // made public by __TexturePackBase

    public void refreshResourcePacks(List<IResourcePack> resourcePacks) {
    }

    public void registerLoadableResource(ILoadableResource resource) {
    }

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
