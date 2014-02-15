package com.prupe.mcpatcher.basemod;

import com.prupe.mcpatcher.FieldRef;
import com.prupe.mcpatcher.MethodRef;
import com.prupe.mcpatcher.Mod;

import static com.prupe.mcpatcher.BytecodeMatcher.anyASTORE;
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

        final MethodRef imageRead = new MethodRef("javax/imageio/ImageIO", "read", "(Ljava/io/InputStream;)Ljava/awt/image/BufferedImage;");

        addClassSignature(new ConstSignature("texture"));

        addClassSignature(new BytecodeSignature() {
            @Override
            public String getMatchExpression() {
                return buildExpression(
                    reference(INVOKESTATIC, imageRead),
                    anyASTORE
                );
            }
        }.setMethod(load));

        addMemberMapper(new FieldMapper(textureName));
    }
}
