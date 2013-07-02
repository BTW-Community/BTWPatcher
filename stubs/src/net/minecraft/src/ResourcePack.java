package net.minecraft.src;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

public interface ResourcePack {
    InputStream getInputStream(ResourceLocation var1) throws IOException;

    boolean hasResource(ResourceLocation var1);

    Set<String> getNamespaces(); // pre-13w25c: uses List<String> instead

    PackMetadataSection getMCMeta(MetadataSectionSerializer var1, String path) throws IOException; // pre-13w26a: only one param

    BufferedImage getPackIcon() throws IOException;

    String getName(); // added in 13w26a
}
