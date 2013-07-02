package net.minecraft.src;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public interface ResourceManager {
    Set<String> getNamespaces(); // added in 13w26a

    Resource getResource(ResourceLocation address) throws IOException;

    List<Resource> getMCMeta(ResourceLocation address) throws IOException; // added in 13w26a
}
