package com.prupe.mcpatcher.basemod.ext18;

import com.prupe.mcpatcher.ClassMod;
import com.prupe.mcpatcher.FieldRef;
import com.prupe.mcpatcher.MethodRef;
import com.prupe.mcpatcher.Mod;

import static com.prupe.mcpatcher.BinaryRegex.begin;
import static javassist.bytecode.Opcode.*;

public class ModelFaceSpriteMod extends ClassMod {
    public static final FieldRef sprite = new FieldRef("ModelFaceSprite", "sprite", "LTextureAtlasSprite;");

    public ModelFaceSpriteMod(Mod mod) {
        super(mod);
        setParentClass("ModelFace");

        addClassSignature(new ConstSignature(new MethodRef("java/util/Arrays", "copyOf", "([II)[I")));
        addClassSignature(new ConstSignature(new MethodRef("java/lang/Float", "intBitsToFloat", "(I)F")));
        addClassSignature(new ConstSignature(16.0f));

        addClassSignature(new BytecodeSignature() {
            @Override
            public String getMatchExpression() {
                return buildExpression(
                    // j = 7 * i;
                    begin(),
                    push(7),
                    ILOAD_1,
                    IMUL,
                    ISTORE_2
                );
            }
        });

        addMemberMapper(new FieldMapper(sprite));
    }
}
