package net.minecraft.src;

import java.util.List;

public interface ITextureResourceBundle extends IResourceBundle {
    void refreshResourcePacks(List<IResourcePack> resourcePacks);

    void registerLoadableResource(ILoadableResource resource);
}
