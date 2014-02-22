package com.prupe.mcpatcher.basemod;

import com.prupe.mcpatcher.ClassMod;
import com.prupe.mcpatcher.FieldRef;
import com.prupe.mcpatcher.MethodRef;
import com.prupe.mcpatcher.Mod;

import static com.prupe.mcpatcher.BytecodeMatcher.*;
import static javassist.bytecode.Opcode.*;

public class RenderBlockCustomMod extends ClassMod {
    public static MethodRef renderFaceAO;
    public static final MethodRef renderFaceNonAO = new MethodRef("RenderBlockCustom", "renderFaceNonAO", "(LBlock;LPosition;LIcon;LDirection;FFFI)V");
    public static final MethodRef renderBlockHeld = new MethodRef("RenderBlockCustom", "renderBlockHeld", "(LBlock;IF)V");
    public static final FieldRef helper = new FieldRef("RenderBlockCustom", "helper", "LRenderBlockCustomHelper;");

    public static boolean haveCustomModels() {
        return Mod.getMinecraftVersion().compareTo("14w06a") >= 0;
    }

    public RenderBlockCustomMod(Mod mod) {
        super(mod);
        setParentClass("RenderBlocks");
        addPrerequisiteClass("RenderBlocks");

        renderFaceAO = new MethodRef("RenderBlockCustom", "renderFaceAO", "(LBlock;LPosition;LIcon;LDirection;FFF" + (Mod.getMinecraftVersion().compareTo("14w07a") >= 0 ? "Z)V" : ")I"));

        addSeedSignature(renderFaceAO);
        addSeedSignature(renderFaceNonAO);

        addMemberMapper(new MethodMapper(renderBlockHeld));
    }

    private void addSeedSignature(MethodRef method) {
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
        }.setMethod(method));
    }

    public RenderBlockCustomMod mapHelper() {
        addClassSignature(new BytecodeSignature() {
            @Override
            public String getMatchExpression() {
                return buildExpression(
                    // this.helper = new RenderBlockCustomHelper(this);
                    ALOAD_0,
                    anyReference(NEW),
                    DUP,
                    ALOAD_0,
                    anyReference(INVOKESPECIAL),
                    captureReference(PUTFIELD)
                );
            }
        }
            .matchConstructorOnly(true)
            .addXref(1, helper)
        );

        addMemberMapper(new FieldMapper(helper));

        return this;
    }
}
