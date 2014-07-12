package com.prupe.mcpatcher.basemod;

import com.prupe.mcpatcher.InterfaceMethodRef;
import com.prupe.mcpatcher.Mod;

/**
 * Maps TextureObject interface (1.6+).
 */
public class TextureObjectMod extends com.prupe.mcpatcher.ClassMod {
    public TextureObjectMod(Mod mod) {
        super(mod);

        InterfaceMethodRef unknownMethod1;
        InterfaceMethodRef unknownMethod2;
        if (IconMod.haveClass()) {
            unknownMethod1 = unknownMethod2 = null;
        } else {
            unknownMethod1 = new InterfaceMethodRef(getDeobfClass(), "unknownMethod1", "(ZZ)V");
            unknownMethod2 = new InterfaceMethodRef(getDeobfClass(), "unknownMethod2", "()V");
        }

        addClassSignature(new InterfaceSignature(
            unknownMethod1,
            unknownMethod2,
            new InterfaceMethodRef(getDeobfClass(), "load", "(LResourceManager;)V"),
            new InterfaceMethodRef(getDeobfClass(), "getGLTexture", "()I")
        ).setInterfaceOnly(true));
    }
}
