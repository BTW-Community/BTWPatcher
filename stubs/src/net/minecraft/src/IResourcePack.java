package net.minecraft.src;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface IResourcePack {
    InputStream getInputStream(ResourceAddress var1) throws IOException;

    boolean hasResource(ResourceAddress var1);

    List<String> getNamespaces();

    MCMetaResourcePackInfo getPackInfo(MCMetaParser var1) throws IOException;

    BufferedImage getPackIcon() throws IOException;
}
