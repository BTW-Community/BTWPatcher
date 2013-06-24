package net.minecraft.src;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public interface IResourceBundle {
    Set<String> getNamespaces(); // added in 13w26a

    IResource getResource(ResourceAddress address) throws IOException;

    List<IResource> getMCMeta(ResourceAddress address) throws IOException; // added in 13w26a
}
