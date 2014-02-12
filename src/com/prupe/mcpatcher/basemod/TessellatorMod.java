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
    public static final MethodRef draw = new MethodRef("Tessellator", "draw", "()I");
    public static final MethodRef startDrawingQuads = new MethodRef("Tessellator", "startDrawingQuads", "()V");
    public static final MethodRef startDrawing = new MethodRef("Tessellator", "startDrawing", "(I)V");
    public static final MethodRef addVertexWithUV = new MethodRef("Tessellator", "addVertexWithUV", "(DDDDD)V");
    public static final MethodRef addVertex = new MethodRef("Tessellator", "addVertex", "(DDD)V");
    public static final MethodRef setTextureUV = new MethodRef("Tessellator", "setTextureUV", "(DD)V");
    public static final MethodRef setColorOpaque_F = new MethodRef("Tessellator", "setColorOpaque_F", "(FFF)V");
    public static final FieldRef instance = new FieldRef("Tessellator", "instance", "LTessellator;");

    public TessellatorMod(Mod mod) {
        super(mod);

        addClassSignature(new BytecodeSignature() {
            @Override
            public String getMatchExpression() {
                return buildExpression(
                    push("Not tesselating!")
                );
            }
        }.setMethod(draw));

        addClassSignature(new BytecodeSignature() {
            @Override
            public String getMatchExpression() {
                return buildExpression(
                    ALOAD_0,
                    push(7),
                    captureReference(INVOKEVIRTUAL),
                    RETURN
                );
            }
        }
            .setMethod(startDrawingQuads)
            .addXref(1, startDrawing)
        );

        addClassSignature(new BytecodeSignature() {
            @Override
            public String getMatchExpression() {
                return buildExpression(
                    ALOAD_0,
                    DLOAD, 7,
                    DLOAD, 9,
                    captureReference(INVOKEVIRTUAL),

                    ALOAD_0,
                    DLOAD_1,
                    DLOAD_3,
                    DLOAD, 5,
                    captureReference(INVOKEVIRTUAL),

                    RETURN
                );
            }
        }
            .setMethod(addVertexWithUV)
            .addXref(1, setTextureUV)
            .addXref(2, addVertex)
        );

        addMemberMapper(new FieldMapper(instance).accessFlag(AccessFlag.STATIC, true));
        addMemberMapper(new MethodMapper(setColorOpaque_F));
    }
}
