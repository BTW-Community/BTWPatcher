package net.minecraft.src;

import java.io.Closeable;
import java.io.IOException;
import java.util.zip.ZipFile;

public class FileResourcePack extends AbstractResourcePack implements Closeable {
    public ZipFile zipFile; // made public by __TexturePackBase

    public void close() throws IOException {
    }
}
