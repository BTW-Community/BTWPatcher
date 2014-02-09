package com.prupe.mcpatcher.basemod;

import com.prupe.mcpatcher.ClassMod;
import com.prupe.mcpatcher.MethodRef;
import com.prupe.mcpatcher.Mod;

import static com.prupe.mcpatcher.BytecodeMatcher.*;
import static javassist.bytecode.Opcode.*;

public class RenderBlockCustomMod extends ClassMod {
    public static final MethodRef renderModelFace = new MethodRef("RenderBlockCustom", "renderModelFace", "(LBlock;LPosition;LIcon;LDirection;FFF)I");

    public static RenderBlockCustomMod setup(Mod mod) {
        if (haveCustomModels()) {
            RenderBlockCustomMod renderBlockCustomMod = new RenderBlockCustomMod(mod);
            mod.addClassMod(renderBlockCustomMod);
            return renderBlockCustomMod;
        } else {
            return null;
        }
    }

    public static boolean haveCustomModels() {
        return Mod.getMinecraftVersion().compareTo("14w06a") >= 0;
    }

    public RenderBlockCustomMod(Mod mod) {
        super(mod);
        setParentClass("RenderBlocks");
        addPrerequisiteClass("RenderBlocks");

        addClassSignature(new BytecodeSignature() {
            @Override
            public String getMatchExpression() {
                return buildExpression(
                    // seed = (long)(i * 3129871) ^ (long)j * 116129781L ^ (long)k;
                    anyILOAD,
                    push(3129871),
                    IMUL,
                    I2L,
                    anyILOAD,
                    I2L,
                    push(116129781L),
                    LMUL,
                    LXOR,
                    anyILOAD,
                    I2L,
                    LXOR,
                    anyLSTORE
                );
            }
        }.setMethod(renderModelFace));

        addMemberMapper(new MethodMapper(renderModelFace));
    }
}
