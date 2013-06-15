package net.minecraft.src;

import java.io.InputStream;

public interface IResource {
    ResourceAddress getAddress();

    InputStream getInputStream();

    boolean isPresent();

    IMCMeta getMCMeta(String var1);
}
