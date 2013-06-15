package net.minecraft.src;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class TextureResourceBundle implements ITextureResourceBundle {
    public Map<String, ResourceBundle> namespaceMap; // made public by __TexturePackBase

    public void refreshResourcePacks(List<IResourcePack> resourcePacks) {
    }

    public void registerLoadableResource(ILoadableResource resource) {
    }

    public IResource getResource(ResourceAddress address) throws IOException {
        return null;
    }
}