package net.minecraft.src;

import java.io.IOException;
import java.util.List;

public class ResourceBundle implements IResourceBundle {
    public List<IResourcePack> resourcePacks; // made public by __TexturePackBase

    public IResource getResource(ResourceAddress address) throws IOException {
        return null;
    }
}
