package com.prupe.mcpatcher.basemod;

import com.prupe.mcpatcher.FieldRef;
import com.prupe.mcpatcher.MethodRef;
import com.prupe.mcpatcher.Mod;
import javassist.bytecode.AccessFlag;

import static com.prupe.mcpatcher.BytecodeMatcher.captureReference;
import static javassist.bytecode.Opcode.*;

/**
 * Matches Tessellator class and instance and maps several commonly used rendering methods.
 */
public class TessellatorMod extends com.prupe.mcpatcher.ClassMod {
    public static MethodRef draw;
    public static final MethodRef addVertexWithUV = new MethodRef("Tessellator", "addVertexWithUV", "(DDDDD)V");
    public static final MethodRef addVertex = new MethodRef("Tessellator", "addVertex", "(DDD)V");
    public static final MethodRef setTextureUV = new MethodRef("Tessellator", "setTextureUV", "(DD)V");
    public static final MethodRef setColorOpaque_F = new MethodRef("Tessellator", "setColorOpaque_F", "(FFF)V");
    public static FieldRef instance;

    public static boolean drawReturnsInt() {
        return Mod.getMinecraftVersion().compareTo("1.8.2-pre1") < 0;
    }

    public TessellatorMod(Mod mod) {
        super(mod);

        draw = new MethodRef("Tessellator", "draw", "()" + (drawReturnsInt() ? "I" : "V"));

        addClassSignature(new BytecodeSignature() {
            @Override
            public String getMatchExpression() {
                return buildExpression(
                    push(TessellatorFactoryMod.haveClass() ? "Not building!" : "Not tesselating!")
                );
            }
        }.setMethod(draw));

        if (TessellatorFactoryMod.haveClass()) {
            instance = null;
        } else {
            instance = new FieldRef("Tessellator", "instance", "LTessellator;");
            addMemberMapper(new FieldMapper(instance).accessFlag(AccessFlag.STATIC, true));
        }

        if (drawReturnsInt()) {
            addMemberMapper(new MethodMapper(setColorOpaque_F));
        } else {
            addMemberMapper(new MethodMapper(null, setColorOpaque_F));
        }
    }
}
