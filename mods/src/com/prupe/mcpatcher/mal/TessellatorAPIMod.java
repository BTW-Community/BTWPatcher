package com.prupe.mcpatcher.mal;

import com.prupe.mcpatcher.FieldRef;
import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.MethodRef;
import com.prupe.mcpatcher.Mod;
import com.prupe.mcpatcher.basemod.TessellatorFactoryMod;

import static com.prupe.mcpatcher.BinaryRegex.backReference;
import static com.prupe.mcpatcher.BinaryRegex.capture;
import static com.prupe.mcpatcher.BytecodeMatcher.*;
import static javassist.bytecode.Opcode.*;

public class TessellatorAPIMod extends Mod {
    public TessellatorAPIMod() {
        name = MCPatcherUtils.TESSELLATOR_API_MOD;
        author = "MCPatcher";
        description = "Internal mod required by the patcher.";
        version = "1.2";

        addClassMod(new TessellatorMod());
        if (TessellatorMod.haveVertexFormatClass()) {
            addClassMod(new TessellatorFactoryMod(this));
            addClassMod(new VertexFormatMod());
            addClassMod(new RenderGlobalMod());
            setMALVersion("tessellator", 4);
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

    private class RenderGlobalMod extends ClassMod {
        RenderGlobalMod() {
            addPrerequisiteClass("Tessellator");

            final MethodRef renderSky = new MethodRef(getDeobfClass(), "renderSky", "(FI)V");
            final FieldRef standardQuadFormat = new FieldRef("VertexFormats", "standardQuadFormat", "LVertexFormat;");
            final MethodRef next = new MethodRef("Tessellator", "next", "()V");

            addClassSignature(new ConstSignature("textures/environment/clouds.png"));

            addClassSignature(new BytecodeSignature() {
                {
                    setMethod(renderSky);
                    addXref(2, standardQuadFormat);
                    addXref(3, TessellatorMod.startDrawing2);
                    addXref(4, TessellatorMod.addXYZ);
                    addXref(5, TessellatorMod.addUV);
                    addXref(6, next);
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // tessellator.startDrawing(7, VertexFormats.standardQuadFormat);
                        capture(anyALOAD),
                        push(7),
                        captureReference(GETSTATIC),
                        captureReference(INVOKEVIRTUAL),

                        // tessellator.addXYZ((double) -f, 100.0, (double) -f).addUV(0.0, 0.0).next();
                        backReference(1),
                        anyFLOAD,
                        FNEG,
                        F2D,
                        push(100.0),
                        anyFLOAD,
                        FNEG,
                        F2D,
                        captureReference(INVOKEVIRTUAL),
                        push(0.0),
                        push(0.0),
                        captureReference(INVOKEVIRTUAL),
                        captureReference(INVOKEVIRTUAL)
                    );
                }
            });
        }
    }
}
