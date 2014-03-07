package com.prupe.mcpatcher.basemod;

import com.prupe.mcpatcher.ClassMod;
import com.prupe.mcpatcher.FieldRef;
import com.prupe.mcpatcher.MethodRef;
import com.prupe.mcpatcher.Mod;
import javassist.bytecode.AccessFlag;

import static javassist.bytecode.Opcode.INVOKESTATIC;

/**
 * Matches BlockModelFace class (14w06a+).
 */
public class BlockModelFaceMod extends ClassMod {
    public static final MethodRef getShadedIntBuffer = new MethodRef("BlockModelFace", "getShadedIntBuffer", "()[I");
    public static final MethodRef getUnshadedIntBuffer = new MethodRef("BlockModelFace", "getUnshadedIntBuffer", "()[I");

    public BlockModelFaceMod(Mod mod) {
        super(mod);

        final MethodRef floatToRawIntBits = new MethodRef("java/lang/Float", "floatToRawIntBits", "(F)I");
        final FieldRef vectorX = new FieldRef("javax/vecmath/Vector3f", "x", "F");

        addClassSignature(new ConstSignature(0.017453292f)); // 180.0 / pi
        addClassSignature(new ConstSignature(floatToRawIntBits));
        addClassSignature(new ConstSignature(vectorX));

        addClassSignature(new BytecodeSignature() {
            @Override
            public String getMatchExpression() {
                return buildExpression(
                    reference(INVOKESTATIC, floatToRawIntBits)
                );
            }
        }.matchConstructorOnly(true));
    }

    public BlockModelFaceMod mapIntBufferMethods() {
        addMemberMapper(new MethodMapper(getShadedIntBuffer, getUnshadedIntBuffer)
            .accessFlag(AccessFlag.PUBLIC, true)
            .accessFlag(AccessFlag.STATIC, false)
        );
        return this;
    }
}
