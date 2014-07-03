package com.prupe.mcpatcher.basemod;

import com.prupe.mcpatcher.InterfaceMethodRef;
import com.prupe.mcpatcher.Mod;

/**
 * Matches Resource class.
 */
public class ResourceMod extends com.prupe.mcpatcher.ClassMod {
    public static InterfaceMethodRef getAddress;
    public static final InterfaceMethodRef getInputStream = new InterfaceMethodRef("Resource", "getInputStream", "()Ljava/io/InputStream;");
    public static final InterfaceMethodRef isPresent = new InterfaceMethodRef("Resource", "isPresent", "()Z");
    public static final InterfaceMethodRef getMCMeta = new InterfaceMethodRef("Resource", "getMCMeta", "(Ljava/lang/String;)LMetadataSection;");
    private static InterfaceMethodRef unknownMethodD;

    public ResourceMod(Mod mod) {
        super(mod);

        if (Mod.getMinecraftVersion().compareTo("13w25a") < 0 || Mod.getMinecraftVersion().compareTo("14w25a") >= 0) {
            getAddress = new InterfaceMethodRef("Resource", "getAddress", "()LResourceLocation;");
        } else {
            getAddress = null;
        }
        if (Mod.getMinecraftVersion().compareTo("14w25a") >= 0) {
            unknownMethodD = new InterfaceMethodRef(getDeobfClass(), "d", "()Ljava/lang/String;");
        } else {
            unknownMethodD = null;
        }

        addClassSignature(new InterfaceSignature(
            getAddress,
            getInputStream,
            isPresent,
            getMCMeta,
            unknownMethodD
        ));
    }
}
