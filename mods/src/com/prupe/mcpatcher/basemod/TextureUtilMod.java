package com.prupe.mcpatcher.basemod;

import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.MethodRef;
import com.prupe.mcpatcher.Mod;

/**
 * Maps TextureUtilsClass (1.6).
 */
public class TextureUtilMod extends com.prupe.mcpatcher.ClassMod {
    protected final MethodRef glTexSubImage2D = new MethodRef(MCPatcherUtils.GL11_CLASS, "glTexSubImage2D", "(IIIIIIIILjava/nio/IntBuffer;)V");
    protected final MethodRef glTexParameteri = new MethodRef(MCPatcherUtils.GL11_CLASS, "glTexParameteri", "(III)V");
    protected final MethodRef glTexParameterf = new MethodRef(MCPatcherUtils.GL11_CLASS, "glTexParameterf", "(IIF)V");
    protected final MethodRef glTexImage2D = new MethodRef(MCPatcherUtils.GL11_CLASS, "glTexImage2D", "(IIIIIIIILjava/nio/IntBuffer;)V");

    public TextureUtilMod(Mod mod) {
        super(mod);

        addClassSignature(new ConstSignature(glTexImage2D));
        addClassSignature(new ConstSignature(glTexSubImage2D));
        addClassSignature(new OrSignature(
            new ConstSignature(glTexParameteri),
            new ConstSignature(glTexParameterf)
        ));
    }
}
