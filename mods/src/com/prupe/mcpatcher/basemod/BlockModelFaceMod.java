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
    public static MethodRef getUnshadedIntBuffer;
    public static final MethodRef getTextureFacing = new MethodRef("BlockModelFace", "getTextureFacing", "()LDirection;");
    public static MethodRef getBlockFacing;

    public BlockModelFaceMod(Mod mod) {
        super(mod);

        final MethodRef floatToRawIntBits = new MethodRef("java/lang/Float", "floatToRawIntBits", "(F)I");
        final FieldRef vector3fX = new FieldRef("javax/vecmath/Vector3f", "x", "F");
        final FieldRef vector3dX = new FieldRef("javax/vecmath/Vector3d", "x", "D");
        final MethodRef matrixSetRotation = new MethodRef("javax/vecmath/Matrix4d", "setRotation", "(Ljavax/vecmath/AxisAngle4d;)V");

        if (Mod.getMinecraftVersion().compareTo("14w10a") < 0) {
            getBlockFacing = null;
        } else {
            getBlockFacing = new MethodRef("BlockModelFace", "getBlockFacing", "()LDirection;");
        }

        addClassSignature(new ConstSignature(floatToRawIntBits));
        if (Mod.getMinecraftVersion().compareTo("14w11a") < 0) {
            getUnshadedIntBuffer = new MethodRef("BlockModelFace", "getUnshadedIntBuffer", "()[I");
            addClassSignature(new ConstSignature(vector3fX));
            addClassSignature(new ConstSignature(0.017453292f)); // 180.0 / pi

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        reference(INVOKESTATIC, floatToRawIntBits)
                    );
                }
            }.matchConstructorOnly(true));
        } else {
            getUnshadedIntBuffer = null;
            addClassSignature(new ConstSignature(vector3dX));
            addClassSignature(new ConstSignature(0.017453292519943295)); // 180.0 / pi
            addClassSignature(new ConstSignature(matrixSetRotation));
        }
    }

    public BlockModelFaceMod mapIntBufferMethods() {
        MethodMapper mapper;
        if (getUnshadedIntBuffer == null) {
            mapper = new MethodMapper(getShadedIntBuffer);
        } else {
            mapper = new MethodMapper(getShadedIntBuffer, getUnshadedIntBuffer);
        }
        mapper
            .accessFlag(AccessFlag.PUBLIC, true)
            .accessFlag(AccessFlag.STATIC, false);
        addMemberMapper(mapper);
        return this;
    }

    public BlockModelFaceMod mapDirectionMethods() {
        MethodMapper mapper;
        if (getBlockFacing == null) {
            mapper = new MethodMapper(getTextureFacing);
        } else {
            mapper = new MethodMapper(getTextureFacing, getBlockFacing);
        }
        mapper
            .accessFlag(AccessFlag.PUBLIC, true)
            .accessFlag(AccessFlag.STATIC, false);
        addMemberMapper(mapper);
        return this;
    }
}
