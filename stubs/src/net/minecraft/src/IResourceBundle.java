package net.minecraft.src;

import java.io.IOException;

public interface IResourceBundle {
    IResource getResource(ResourceAddress address) throws IOException;
}
