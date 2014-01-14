package com.prupe.mcpatcher.basemod;

import com.prupe.mcpatcher.InterfaceMethodRef;
import com.prupe.mcpatcher.Mod;

/**
 * Matches Resource class.
 */
public class ResourceMod extends com.prupe.mcpatcher.ClassMod {
    public ResourceMod(Mod mod) {
        super(mod);

        addClassSignature(new InterfaceSignature(
            Mod.getMinecraftVersion().compareTo("13w25a") >= 0 ? null : new InterfaceMethodRef(getDeobfClass(), "getAddress", "()LResourceLocation;"),
            new InterfaceMethodRef(getDeobfClass(), "getInputStream", "()Ljava/io/InputStream;"),
            new InterfaceMethodRef(getDeobfClass(), "isPresent", "()Z"),
            new InterfaceMethodRef(getDeobfClass(), "getMCMeta", "(Ljava/lang/String;)LMetadataSection;")
        ));
    }
}
