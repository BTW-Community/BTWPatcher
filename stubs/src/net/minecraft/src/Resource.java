package net.minecraft.src;

import java.io.InputStream;

public interface Resource {
    InputStream getInputStream();

    boolean isPresent();

    MetadataSection getMCMeta(String path);
}
