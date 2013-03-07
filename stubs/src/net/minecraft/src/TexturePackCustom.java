package net.minecraft.src;

import java.io.File;
import java.util.zip.ZipFile;

public class TexturePackCustom extends TexturePackImplementation {
    public ZipFile zipFile;

    // added by TexturePackBase
    public File tmpFile;
    public ZipFile origZip;
    public long lastModified;
}
