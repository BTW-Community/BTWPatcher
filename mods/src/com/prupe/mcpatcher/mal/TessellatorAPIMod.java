package com.prupe.mcpatcher.mal;

import com.prupe.mcpatcher.*;
import com.prupe.mcpatcher.basemod.TessellatorFactoryMod;

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
        if (TessellatorMod.haveVertexFormatClass()) {
            setMALVersion("tessellator", 4);
            addClassMod(new TessellatorFactoryMod(this));
            addClassMod(new VertexFormatMod());
            addClassMod(new MinecraftMod());
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

            if (TessellatorMod.haveVertexFormatClass()) {
                setupV4();
            } else {
                setupV123();
            }
        }

        private void setupV123() {
            addClassSignature(new BytecodeSignature() {
                {
                    setMethod(startDrawingQuads);
                    addXref(1, startDrawing1);
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
            addClassSignature(new BytecodeSignature() {
                {
                    setMethod(startDrawing2);
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

    private class VertexFormatMod extends ClassMod {
        VertexFormatMod() {
            final MethodRef addElement = new MethodRef("VertexFormat", "addElement", "(LVertexFormatElement;)LVertexFormat;");

            addClassSignature(new ConstSignature("format: "));
            addClassSignature(new ConstSignature(" elements: "));

            addClassSignature(new BytecodeSignature() {
                {
                    setMethod(addElement);
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // switch (...[...])
                        anyReference(GETSTATIC),
                        ALOAD_1,
                        anyReference(INVOKEVIRTUAL),
                        anyReference(INVOKEVIRTUAL),
                        IALOAD,
                        TABLESWITCH
                    );
                }
            });
        }
    }

    private class MinecraftMod extends com.prupe.mcpatcher.basemod.MinecraftMod {
        MinecraftMod() {
            super(TessellatorAPIMod.this);

            final MethodRef drawLogo = new MethodRef(getDeobfClass(), "drawLogo", "(LTextureManager;)V");
            final FieldRef standardQuadFormat = new FieldRef("VertexFormats", "standardQuadFormat", "LVertexFormat;");

            addClassSignature(new BytecodeSignature() {
                {
                    setMethod(drawLogo);
                    addXref(1, standardQuadFormat);
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // tessellator.startDrawing(7, VertexFormats.standardQuadFormat);
                        anyALOAD,
                        push(7),
                        captureReference(GETSTATIC),
                        anyReference(INVOKEVIRTUAL)
                    );
                }
            });
        }
    }
}
