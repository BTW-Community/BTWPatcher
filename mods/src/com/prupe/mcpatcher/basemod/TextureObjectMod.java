package com.prupe.mcpatcher.basemod;

import com.prupe.mcpatcher.InterfaceMethodRef;
import com.prupe.mcpatcher.Mod;

/**
 * Maps TextureObject interface (1.6+).
 */
public class TextureObjectMod extends com.prupe.mcpatcher.ClassMod {
    public TextureObjectMod(Mod mod) {
        super(mod);

        addClassSignature(new InterfaceSignature(
            new InterfaceMethodRef(getDeobfClass(), "load", "(LResourceManager;)V"),
            new InterfaceMethodRef(getDeobfClass(), "getGLTexture", "()I")
        ).setInterfaceOnly(true));
    }
}
