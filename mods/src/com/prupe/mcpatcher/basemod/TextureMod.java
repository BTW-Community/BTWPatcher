package com.prupe.mcpatcher.basemod;

import com.prupe.mcpatcher.FieldRef;
import com.prupe.mcpatcher.MethodRef;
import com.prupe.mcpatcher.Mod;

import static com.prupe.mcpatcher.BytecodeMatcher.captureReference;
import static javassist.bytecode.Opcode.*;

/**
 * Maps Texture class and various fields and methods.
 */
public class TextureMod extends com.prupe.mcpatcher.ClassMod {
    protected final FieldRef glTextureId = new FieldRef(getDeobfClass(), "glTextureId", "I");
    protected final FieldRef rgb = new FieldRef(getDeobfClass(), "rgb", "[I");
    protected final MethodRef getGlTextureId = new MethodRef(getDeobfClass(), "getGlTextureId", "()I");
    protected final MethodRef getWidth = new MethodRef(getDeobfClass(), "getWidth", "()I");
    protected final MethodRef getHeight = new MethodRef(getDeobfClass(), "getHeight", "()I");
    protected final MethodRef getRGB = new MethodRef(getDeobfClass(), "getRGB", "()[I");

    public TextureMod(Mod mod) {
        super(mod);
        setParentClass("AbstractTexture");

        addClassSignature(new BytecodeSignature() {
            @Override
            public String getMatchExpression() {
                return buildExpression(
                    ALOAD_0,
                    ILOAD_1,
                    ILOAD_2,
                    IMUL,
                    NEWARRAY, T_INT,
                    captureReference(PUTFIELD)
                );
            }
        }
            .matchConstructorOnly(true)
            .addXref(1, rgb)
        );

        addMemberMapper(new MethodMapper(getRGB));
    }
}
