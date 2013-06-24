package net.minecraft.src;

import java.io.InputStream;

public interface IResource {
    InputStream getInputStream();

    boolean isPresent();

    IMCMeta getMCMeta(String path);
}
