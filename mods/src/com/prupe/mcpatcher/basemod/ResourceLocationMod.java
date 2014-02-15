package com.prupe.mcpatcher.basemod;

import com.prupe.mcpatcher.MethodRef;
import com.prupe.mcpatcher.Mod;
import javassist.bytecode.AccessFlag;

import static javassist.bytecode.Opcode.INVOKEVIRTUAL;

/**
 * Matches ResourceLocation class.
 */
public class ResourceLocationMod extends com.prupe.mcpatcher.ClassMod {
    protected final MethodRef getNamespace = new MethodRef(getDeobfClass(), "getNamespace", "()Ljava/lang/String;");
    protected final MethodRef getPath = new MethodRef(getDeobfClass(), "getPath", "()Ljava/lang/String;");

    public ResourceLocationMod(Mod mod) {
        super(mod);

        final MethodRef indexOf = new MethodRef("java/lang/String", "indexOf", "(I)I");

        addClassSignature(new ConstSignature("minecraft"));

        addClassSignature(new BytecodeSignature() {
            @Override
            public String getMatchExpression() {
                return buildExpression(
                    push(58),
                    reference(INVOKEVIRTUAL, indexOf)
                );
            }
        });

        addMemberMapper(new MethodMapper(getPath, getNamespace)
            .accessFlag(AccessFlag.PUBLIC, true)
            .accessFlag(AccessFlag.STATIC, false)
        );
    }
}
