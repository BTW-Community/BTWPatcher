package com.prupe.mcpatcher.basemod;

import com.prupe.mcpatcher.FieldRef;
import com.prupe.mcpatcher.MethodRef;
import com.prupe.mcpatcher.Mod;
import javassist.bytecode.AccessFlag;

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
    public static final MethodRef startDrawingQuads = new MethodRef("Tessellator", "startDrawingQuads", "()V");
    public static final MethodRef startDrawing1 = new MethodRef("Tessellator", "startDrawing", "(I)V");

    public static FieldRef instance;

    // 1.8.2-pre5+ methods
    public static final MethodRef startDrawing2 = new MethodRef("Tessellator", "startDrawing", "(ILVertexFormat;)V");
    public static final MethodRef addXYZ = new MethodRef("Tessellator", "addXYZ", "(DDD)LTessellator;");
    public static final MethodRef addUV = new MethodRef("Tessellator", "addUV", "(DD)LTessellator;");
    public static final MethodRef setColorF = new MethodRef("Tessellator", "setColorF", "(FFFF)LTessellator;");

    public static boolean drawReturnsInt() {
        return Mod.getMinecraftVersion().compareTo("1.8.2-pre1") < 0;
    }

    public static boolean haveVertexFormatClass() {
        return Mod.getMinecraftVersion().compareTo("1.8.2-pre5") >= 0;
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

        if (haveVertexFormatClass()) {
            addClassSignature(new BytecodeSignature() {
                {
                    setMethod(setColorF);
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // (int) (r * 255.0f)
                        FLOAD_1,
                        push(255.0f),
                        FMUL,
                        F2I
                    );
                }
            });
        } else if (drawReturnsInt()) {
            addMemberMapper(new MethodMapper(setColorOpaque_F));
        } else {
            addMemberMapper(new MethodMapper(null, setColorOpaque_F));
        }

        if (haveVertexFormatClass()) {
            addMemberMapper(new MethodMapper(addXYZ));
            addMemberMapper(new MethodMapper(addUV));
        } else {
            addMemberMapper(new MethodMapper(addVertexWithUV));
        }
    }
}
