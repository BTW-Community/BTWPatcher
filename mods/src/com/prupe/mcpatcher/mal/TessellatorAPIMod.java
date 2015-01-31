package com.prupe.mcpatcher.mal;

import com.prupe.mcpatcher.*;
import com.prupe.mcpatcher.basemod.TessellatorFactoryMod;
import com.prupe.mcpatcher.basemod.ext18.VertexFormatMod;

import static com.prupe.mcpatcher.BinaryRegex.*;
import static com.prupe.mcpatcher.BytecodeMatcher.*;
import static javassist.bytecode.Opcode.*;

public class TessellatorAPIMod extends Mod {
    public TessellatorAPIMod() {
        name = MCPatcherUtils.TESSELLATOR_API_MOD;
        author = "MCPatcher";
        description = "Internal mod required by the patcher.";
        version = "1.1";

        addClassMod(new TessellatorMod());
        if (VertexFormatMod.haveClass()) {
            setMALVersion("tessellator", 4);
            addClassMod(new TessellatorFactoryMod(this));
            addClassMod(new VertexFormatMod(this));
        } else if (TessellatorFactoryMod.haveClass()) {
            addClassMod(new TessellatorFactoryMod(this));
            setMALVersion("tessellator", TessellatorMod.drawReturnsInt() ? 2 : 3);
        } else {
            setMALVersion("tessellator", 1);
        }

        addClassFiles("com.prupe.mcpatcher.mal.tessellator.*");
    }

    @Override
    public String[] getLoggingCategories() {
        return null;
    }

    private class TessellatorMod extends com.prupe.mcpatcher.basemod.TessellatorMod {
        TessellatorMod() {
            super(TessellatorAPIMod.this);

            if (VertexFormatMod.haveClass()) {
                setupV4();
            } else {
                setupV123();
            }
        }

        private void setupV123() {
            final MethodRef startDrawingQuads = new MethodRef("Tessellator", "startDrawingQuads", "()V");
            final MethodRef startDrawing = new MethodRef("Tessellator", "startDrawing", "(I)V");

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
        }

        private void setupV4() {
            final MethodRef startDrawing = new MethodRef(getDeobfClass(), "startDrawing", "(ILVertexFormat;)V");
            final MethodRef addXYZ = new MethodRef(getDeobfClass(), "addXYZ", "(DDD)LTessellator;");
            final MethodRef addUV = new MethodRef(getDeobfClass(), "addUV", "(DD)LTessellator;");
            final MethodRef setColorF = new MethodRef(getDeobfClass(), "setColorF", "(FFFF)LTessellator;");

            addClassSignature(new BytecodeSignature() {
                {
                    setMethod(startDrawing);
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push("Already building!")
                    );
                }
            });

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

            addReturnThisMethodSignature(addXYZ);
            addReturnThisMethodSignature(addUV);
        }

        private void addReturnThisMethodSignature(final MethodRef method) {
            addClassSignature(new BytecodeSignature() {
                {
                    setMethod(method);
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // return this;
                        ALOAD_0,
                        ARETURN,
                        end()
                    );
                }
            });
        }
    }
}
