package com.prupe.mcpatcher.mod;

import com.prupe.mcpatcher.*;

import static com.prupe.mcpatcher.BinaryRegex.*;
import static com.prupe.mcpatcher.BytecodeMatcher.*;
import static javassist.bytecode.Opcode.*;

public class HDFont extends Mod {
    public HDFont() {
        name = MCPatcherUtils.HD_FONT;
        author = "MCPatcher";
        description = "Provides support for higher resolution fonts.";
        version = "1.6";

        addDependency(BaseTexturePackMod.NAME);

        addClassMod(new FontRendererMod());

        addClassFile(MCPatcherUtils.FONT_UTILS_CLASS);
    }

    private class FontRendererMod extends BaseMod.FontRendererMod {
        FontRendererMod() {
            final FieldRef charWidth = new FieldRef(getDeobfClass(), "charWidth", "[I");
            final FieldRef fontHeight = new FieldRef(getDeobfClass(), "fontHeight", "I");
            final FieldRef charWidthf = new FieldRef(getDeobfClass(), "charWidthf", "[F");
            final MethodRef getStringWidth = new MethodRef(getDeobfClass(), "getStringWidth", "(Ljava/lang/String;)I");
            final MethodRef getCharWidth = new MethodRef(getDeobfClass(), "getCharWidth", "(C)I");
            final MethodRef computeCharWidths = new MethodRef(getDeobfClass(), "computeCharWidths", "(Ljava/lang/String;)V");
            final MethodRef getStringWidthf = new MethodRef(MCPatcherUtils.FONT_UTILS_CLASS, "getStringWidthf", "(LFontRenderer;Ljava/lang/String;)F");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        or(
                            build(push(8)),
                            build(push(9))
                        ),
                        captureReference(PUTFIELD)
                    );
                }
            }
                .matchConstructorOnly(true)
                .addXref(1, fontHeight)
            );

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        capture(anyILOAD),
                        push(16),
                        IREM,
                        anyISTORE,
                        backReference(1),
                        push(16),
                        IDIV,
                        anyISTORE
                    );
                }
            }.setMethod(computeCharWidths));

            addMemberMapper(new MethodMapper(getStringWidth));
            addMemberMapper(new MethodMapper(getCharWidth));

            addPatch(new AddFieldPatch(charWidthf));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override font name";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        ALOAD_2,
                        anyReference(PUTFIELD)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        ALOAD_2,
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.FONT_UTILS_CLASS, "getFontName", "(Ljava/lang/String;)Ljava/lang/String;")),
                        ASTORE_2
                    );
                }
            }
                .setInsertBefore(true)
                .matchConstructorOnly(true)
            );

            addPatch(new BytecodePatch() {
                private int imageRegister;
                private int rgbRegister;

                {
                    addPreMatchSignature(new BytecodeSignature() {
                        @Override
                        public String getMatchExpression() {
                            return buildExpression(
                                capture(anyALOAD),
                                reference(INVOKEVIRTUAL, new MethodRef("java/awt/image/BufferedImage", "getWidth", "()I")),
                                any(0, 10),
                                anyILOAD,
                                anyILOAD,
                                IMUL,
                                NEWARRAY, T_INT,
                                capture(anyASTORE)
                            );
                        }

                        @Override
                        public boolean afterMatch() {
                            imageRegister = extractRegisterNum(getCaptureGroup(1));
                            rgbRegister = extractRegisterNum(getCaptureGroup(2));
                            return true;
                        }
                    });
                }

                @Override
                public String getDescription() {
                    return "FontUtils.computeCharWidths on init";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ICONST_0,
                        ISTORE, capture(any()),
                        ILOAD, backReference(1),
                        push(256),
                        IF_ICMPGE, any(2),
                        any(1, 180),
                        IINC, backReference(1), 1,
                        GOTO, any(2)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        ALOAD_0,
                        ALOAD_0,
                        ALOAD_1,
                        ALOAD, imageRegister,
                        ALOAD, rgbRegister,
                        ALOAD_0,
                        reference(GETFIELD, charWidth),
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.FONT_UTILS_CLASS, "computeCharWidths", "(LFontRenderer;Ljava/lang/String;Ljava/awt/image/BufferedImage;[I[I)[F")),
                        reference(PUTFIELD, charWidthf)
                    );
                }
            }.targetMethod(computeCharWidths));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "use charWidthf instead of charWidth";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        reference(GETFIELD, charWidth),
                        capture(any(1, 4)),
                        IALOAD,
                        I2F,
                        FRETURN
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        ALOAD_0,
                        reference(GETFIELD, charWidthf),
                        getCaptureGroup(1),
                        FALOAD,
                        ALOAD_0,
                        reference(GETFIELD, fontHeight),
                        I2F,
                        FMUL,
                        push(8.0f),
                        FDIV,
                        FRETURN
                    );
                }
            });

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "replace getStringWidth";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        begin(),
                        any(0, 1000),
                        end()
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        ALOAD_0,
                        ALOAD_1,
                        reference(INVOKESTATIC, getStringWidthf),
                        F2I,
                        IRETURN
                    );
                }
            }.targetMethod(getStringWidth));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "4.0f -> charWidthf[32]";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(4.0f),
                        FRETURN
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        ALOAD_0,
                        reference(GETFIELD, charWidthf),
                        push(32),
                        FALOAD,
                        FRETURN
                    );
                }
            });

            addGetResourcePatch();
        }

        private void addGetResourcePatch() {
            final MethodRef getResource = new MethodRef("java.lang.Class", "getResource", "(Ljava/lang/String;)Ljava/net/URL;");
            final MethodRef readURL = new MethodRef("javax.imageio.ImageIO", "read", "(Ljava/net/URL;)Ljava/awt/image/BufferedImage;");
            final MethodRef getResourceAsStream = new MethodRef("java.lang.Class", "getResourceAsStream", "(Ljava/lang/String;)Ljava/io/InputStream;");
            final MethodRef readStream = new MethodRef("javax.imageio.ImageIO", "read", "(Ljava/io/InputStream;)Ljava/awt/image/BufferedImage;");

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "ImageIO.read(getResource(...)) -> getImage(...)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        BinaryRegex.or(
                            buildExpression(
                                reference(INVOKEVIRTUAL, getResource),
                                reference(INVOKESTATIC, readURL)
                            ),
                            buildExpression(
                                reference(INVOKEVIRTUAL, getResourceAsStream),
                                reference(INVOKESTATIC, readStream)
                            )
                        )
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.TEXTURE_PACK_API_CLASS, "getImage", "(Ljava/lang/Object;Ljava/lang/String;)Ljava/awt/image/BufferedImage;"))
                    );
                }
            });
        }
    }
}
