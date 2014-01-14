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
    protected final MethodRef draw = new MethodRef(getDeobfClass(), "draw", "()I");
    protected final MethodRef startDrawingQuads = new MethodRef(getDeobfClass(), "startDrawingQuads", "()V");
    protected final MethodRef startDrawing = new MethodRef(getDeobfClass(), "startDrawing", "(I)V");
    protected final MethodRef addVertexWithUV = new MethodRef(getDeobfClass(), "addVertexWithUV", "(DDDDD)V");
    protected final MethodRef addVertex = new MethodRef(getDeobfClass(), "addVertex", "(DDD)V");
    protected final MethodRef setTextureUV = new MethodRef(getDeobfClass(), "setTextureUV", "(DD)V");
    protected final MethodRef setColorOpaque_F = new MethodRef(getDeobfClass(), "setColorOpaque_F", "(FFF)V");
    protected final FieldRef instance = new FieldRef(getDeobfClass(), "instance", "LTessellator;");

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
