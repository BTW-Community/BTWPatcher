package com.prupe.mcpatcher.basemod;

import com.prupe.mcpatcher.MethodRef;
import com.prupe.mcpatcher.Mod;

import static com.prupe.mcpatcher.BytecodeMatcher.anyReference;
import static javassist.bytecode.Opcode.*;

/**
 * Maps DynamicTexture class.
 */
public class DynamicTextureMod extends com.prupe.mcpatcher.ClassMod {
    protected final MethodRef getRGB = new MethodRef(getDeobfClass(), "getRGB", "()[I");

    public DynamicTextureMod(Mod mod) {
        super(mod);
        setParentClass("AbstractTexture");

        addClassSignature(new BytecodeSignature() {
            {
                matchConstructorOnly(true);
                setMethod(new MethodRef(getDeobfClass(), "<init>", "(II)V"));
            }

            @Override
            public String getMatchExpression() {
                return buildExpression(
                    ALOAD_0,
                    ILOAD_1,
                    ILOAD_2,
                    IMUL,
                    NEWARRAY, T_INT,
                    anyReference(PUTFIELD)
                );
            }
        });

        addMemberMapper(new MethodMapper(getRGB));
    }
}
