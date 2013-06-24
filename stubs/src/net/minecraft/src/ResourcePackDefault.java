package net.minecraft.src;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

public class ResourcePackDefault implements IResourcePack {
    public File file; // made public by __TexturePackBase

    public InputStream getInputStream(ResourceAddress var1) throws IOException {
        return null;
    }

    public boolean hasResource(ResourceAddress var1) {
        return false;
    }

    public Set<String> getNamespaces() {
        return null;
    }

    public MCMetaResourcePackInfo getMCMeta(MCMetaParser var1, String path) throws IOException {
        return null;
    }

    public BufferedImage getPackIcon() throws IOException {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }
}
