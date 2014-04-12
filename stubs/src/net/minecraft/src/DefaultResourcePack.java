package net.minecraft.src;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;

public class DefaultResourcePack implements ResourcePack {
    public Map<String, File> map; // made public by __TexturePackBase

    public InputStream getInputStream(ResourceLocation var1) throws IOException {
        return null;
    }

    public boolean hasResource(ResourceLocation var1) {
        return false;
    }

    public Set<String> getNamespaces() {
        return null;
    }

    public PackMetadataSection getMCMeta(MetadataSectionSerializer var1, String path) throws IOException {
        return null;
    }

    public BufferedImage getPackIcon() throws IOException {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public InputStream getInputStream(String resource) throws IOException {
        return null;
    }
}
