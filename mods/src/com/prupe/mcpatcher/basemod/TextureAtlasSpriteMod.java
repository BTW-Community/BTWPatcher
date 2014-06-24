package com.prupe.mcpatcher.basemod;

import com.prupe.mcpatcher.FieldRef;
import com.prupe.mcpatcher.MethodRef;
import com.prupe.mcpatcher.Mod;

import static com.prupe.mcpatcher.BinaryRegex.build;
import static com.prupe.mcpatcher.BinaryRegex.repeat;
import static com.prupe.mcpatcher.BytecodeMatcher.anyFSTORE;
import static com.prupe.mcpatcher.BytecodeMatcher.anyILOAD;
import static javassist.bytecode.Opcode.*;

/**
 * Maps TextureAtlasSprite class.
 */
public class TextureAtlasSpriteMod extends com.prupe.mcpatcher.ClassMod {
    public static final FieldRef textureName = new FieldRef("TextureAtlasSprite", "textureName", "Ljava/lang/String;");
    public static final MethodRef getX0 = new MethodRef("TextureAtlasSprite", "getX0", "()I");
    public static final MethodRef getY0 = new MethodRef("TextureAtlasSprite", "getY0", "()I");

    public TextureAtlasSpriteMod(Mod mod) {
        super(mod);
        setInterfaces("Icon");

        if (ResourceLocationMod.haveClass()) {
            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(repeat(build(
                        push(0.009999999776482582),
                        anyILOAD,
                        I2D,
                        DDIV,
                        D2F,
                        anyFSTORE
                    ), 2));
                }
            });
        } else {
            addClassSignature(new ConstSignature("clock"));
            addClassSignature(new ConstSignature("compass"));
            addClassSignature(new ConstSignature(","));
        }

        addMemberMapper(new FieldMapper(textureName));
    }
}
