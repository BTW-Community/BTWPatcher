package net.minecraft.src;

import java.io.InputStream;

public class StreamedResource implements IResource {
    public InputStream getInputStream() {
        return null;
    }

    public boolean isPresent() {
        return false;
    }

    public IMCMeta getMCMeta(String var1) {
        return null;
    }
}
