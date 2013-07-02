package net.minecraft.src;

import java.io.InputStream;

public class SimpleResource implements Resource {
    public InputStream getInputStream() {
        return null;
    }

    public boolean isPresent() {
        return false;
    }

    public MetadataSection getMCMeta(String var1) {
        return null;
    }
}
