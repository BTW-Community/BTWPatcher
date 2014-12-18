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
    public static final MethodRef startDrawingQuads = new MethodRef("Tessellator", "startDrawingQuads", "()V");
    public static final MethodRef startDrawing = new MethodRef("Tessellator", "startDrawing", "(I)V");
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

        addClassSignature(new BytecodeSignature() {
            {
                setMethod(startDrawingQuads);
                addXref(1, startDrawing);
            }

            @Override
            public String getMatchExpression() {
                return buildExpression(
                    ALOAD_0,
                    push(7),
                    captureReference(INVOKEVIRTUAL),
                    RETURN
                );
            }
        });

        addClassSignature(new BytecodeSignature() {
            {
                setMethod(addVertexWithUV);
                addXref(1, setTextureUV);
                addXref(2, addVertex);
            }

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
        });

        if (TessellatorFactoryMod.haveClass()) {
            instance = null;
        } else {
            instance = new FieldRef("Tessellator", "instance", "LTessellator;");
            addMemberMapper(new FieldMapper(instance).accessFlag(AccessFlag.STATIC, true));
        }

        addMemberMapper(new MethodMapper(setColorOpaque_F));
    }
}
