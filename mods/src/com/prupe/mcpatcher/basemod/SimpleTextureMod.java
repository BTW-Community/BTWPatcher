package com.prupe.mcpatcher.basemod;

import com.prupe.mcpatcher.FieldRef;
import com.prupe.mcpatcher.MethodRef;
import com.prupe.mcpatcher.Mod;

import static com.prupe.mcpatcher.BytecodeMatcher.*;
import static javassist.bytecode.Opcode.ALOAD_2;
import static javassist.bytecode.Opcode.INVOKESTATIC;

/**
 * Maps SimpleTexture class (1.6+).
 */
public class SimpleTextureMod extends com.prupe.mcpatcher.ClassMod {
    protected final FieldRef textureName = new FieldRef(getDeobfClass(), "address", "LResourceLocation;");
    protected final MethodRef load = new MethodRef(getDeobfClass(), "load", "(LResourceManager;)V");

    public SimpleTextureMod(Mod mod) {
        super(mod);
        setParentClass("AbstractTexture");

        addClassSignature(new ConstSignature("texture"));

        addClassSignature(new BytecodeSignature() {
            @Override
            public String getMatchExpression() {
                return buildExpression(
                    // 14w26a+: image = ImageUtils.read(path);
                    // older: image = ImageIO.read(path);
                    ALOAD_2,
                    anyReference(INVOKESTATIC),
                    anyASTORE,

                    // blur = false;
                    push(0),
                    anyISTORE,

                    // clamp = false;
                    push(0),
                    anyISTORE
                );
            }
        }.setMethod(load));

        addMemberMapper(new FieldMapper(textureName));
    }
}
