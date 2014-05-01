package com.prupe.mcpatcher.basemod;

import com.prupe.mcpatcher.FieldRef;
import com.prupe.mcpatcher.MethodRef;
import com.prupe.mcpatcher.Mod;

import static com.prupe.mcpatcher.BinaryRegex.*;
import static com.prupe.mcpatcher.BytecodeMatcher.anyReference;
import static com.prupe.mcpatcher.BytecodeMatcher.captureReference;
import static javassist.bytecode.Opcode.*;

/**
 * Maps AbstractTexture class (1.6+).
 */
public class AbstractTextureMod extends com.prupe.mcpatcher.ClassMod {
    protected final FieldRef glTextureId = new FieldRef(getDeobfClass(), "glTextureId", "I");
    protected final MethodRef getGLTextureId = new MethodRef(getDeobfClass(), "getGlTextureId", "()I");

    public AbstractTextureMod(Mod mod) {
        super(mod);
        setInterfaces("TextureObject");

        addClassSignature(new BytecodeSignature() {
            @Override
            public String getMatchExpression() {
                return buildExpression(
                    ALOAD_0,
                    push(-1),
                    anyReference(PUTFIELD)
                );
            }
        }.matchConstructorOnly(true));

        addClassSignature(new BytecodeSignature() {
            {
                setMethod(getGLTextureId);
                addXref(2, new MethodRef("TextureUtil", "newGLTexture", "()I"));
            }

            @Override
            public String getMatchExpression() {
                return buildExpression(
                    begin(),
                    ALOAD_0,
                    GETFIELD, capture(any(2)),
                    push(-1),
                    IF_ICMPNE, any(2),

                    ALOAD_0,
                    captureReference(INVOKESTATIC),
                    PUTFIELD, backReference(1)
                );
            }
        });

        addMemberMapper(new FieldMapper(glTextureId));
    }
}
