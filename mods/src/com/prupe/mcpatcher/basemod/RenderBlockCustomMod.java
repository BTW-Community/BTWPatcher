package com.prupe.mcpatcher.basemod;

import com.prupe.mcpatcher.ClassMod;
import com.prupe.mcpatcher.FieldRef;
import com.prupe.mcpatcher.MethodRef;
import com.prupe.mcpatcher.Mod;

import static com.prupe.mcpatcher.BytecodeMatcher.*;
import static javassist.bytecode.Opcode.*;

public class RenderBlockCustomMod extends ClassMod {
    public static MethodRef renderFaceAO;
    public static MethodRef renderFaceNonAO;
    public static final MethodRef renderBlockHeld = new MethodRef("RenderBlockCustom", "renderBlockHeld", "(LBlock;IF)V");
    public static final FieldRef helper = new FieldRef("RenderBlockCustom", "helper", "LRenderBlockCustomHelper;");

    public static boolean haveCustomModels() {
        return Mod.getMinecraftVersion().compareTo("14w06a") >= 0;
    }

    public static int getDirectionParam() {
        return Mod.getMinecraftVersion().compareTo("14w10a") >= 0 ? 3 : 4;
    }

    public RenderBlockCustomMod(Mod mod) {
        super(mod);
        setParentClass("RenderBlocks");
        addPrerequisiteClass("RenderBlocks");

        String returnTypeAO;
        if (Mod.getMinecraftVersion().compareTo("14w07a") < 0) {
            returnTypeAO = ")I";
        } else if (Mod.getMinecraftVersion().compareTo("14w11a") < 0) {
            returnTypeAO = "Z)V";
        } else {
            returnTypeAO = ")V";
        }
        String returnTypeNonAO = Mod.getMinecraftVersion().compareTo("14w11a") < 0 ? "I)V" : "IZ)V";
        String iconParam = Mod.getMinecraftVersion().compareTo("14w10a") < 0 ? "LIcon;" : "";
        renderFaceAO = new MethodRef("RenderBlockCustom", "renderFaceAO", "(LBlock;LPosition;" + iconParam + "LDirection;FFF" + returnTypeAO);
        renderFaceNonAO = new MethodRef("RenderBlockCustom", "renderFaceNonAO", "(LBlock;LPosition;" + iconParam + "LDirection;FFF" + returnTypeNonAO);

        addClassSignature(new ConstSignature(0xf000f));
        addSeedSignature(renderFaceNonAO);
        addSeedSignature(renderFaceAO);

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
            {
                matchConstructorOnly(true);
                addXref(1, helper);
            }

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
        });

        return this;
    }
}
