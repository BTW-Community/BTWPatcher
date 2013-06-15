package net.minecraft.src;

import java.io.Closeable;
import java.io.IOException;
import java.util.zip.ZipFile;

public class ResourcePackZip extends ResourcePackBase implements Closeable {
    public ZipFile zipFile; // made public by __TexturePackBase

    public void close() throws IOException {
    }
}
