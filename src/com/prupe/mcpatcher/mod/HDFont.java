package com.prupe.mcpatcher.mod;

import com.prupe.mcpatcher.*;
import javassist.bytecode.AccessFlag;

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

        setupMod(this);
    }

    static void setupMod(Mod mod) {
        mod.addClassMod(new FontRendererMod());

        mod.addClassFile(MCPatcherUtils.FONT_UTILS_CLASS);
    }

    private static class FontRendererMod extends BaseMod.FontRendererMod {
        FontRendererMod() {
            final FieldRef fontTextureName = new FieldRef(getDeobfClass(), "fontTextureName", "Ljava/lang/String;");
            final FieldRef charWidth = new FieldRef(getDeobfClass(), "charWidth", "[I");
            final FieldRef fontHeight = new FieldRef(getDeobfClass(), "fontHeight", "I");
            final FieldRef charWidthf = new FieldRef(getDeobfClass(), "charWidthf", "[F");
            final MethodRef readFontData = new MethodRef(getDeobfClass(), "readFontData", "()V");
            final MethodRef getStringWidth = new MethodRef(getDeobfClass(), "getStringWidth", "(Ljava/lang/String;)I");
            final MethodRef getCharWidth = new MethodRef(getDeobfClass(), "getCharWidth", "(C)I");
            final MethodRef computeCharWidths = new MethodRef(getDeobfClass(), "computeCharWidths", "(Ljava/lang/String;)V");
            final MethodRef getImageWidth = new MethodRef("java/awt/image/BufferedImage", "getWidth", "()I");
            final MethodRef getFontName = new MethodRef(MCPatcherUtils.FONT_UTILS_CLASS, "getFontName", "(Ljava/lang/String;)Ljava/lang/String;");
            final MethodRef computeCharWidthsf = new MethodRef(MCPatcherUtils.FONT_UTILS_CLASS, "computeCharWidthsf", "(LFontRenderer;Ljava/lang/String;Ljava/awt/image/BufferedImage;[I[I)[F");
            final MethodRef getCharWidthf = new MethodRef(MCPatcherUtils.FONT_UTILS_CLASS, "getCharWidthf", "(LFontRenderer;[II)F");
            final MethodRef getStringWidthf = new MethodRef(MCPatcherUtils.FONT_UTILS_CLASS, "getStringWidthf", "(LFontRenderer;Ljava/lang/String;)F");
            final FieldRef enableFont = new FieldRef(MCPatcherUtils.FONT_UTILS_CLASS, "enable", "Z");

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
                        ALOAD_0,
                        ALOAD_0,
                        captureReference(GETFIELD),
                        captureReference(INVOKESPECIAL)
                    );
                }
            }
                .setMethod(readFontData)
                .addXref(1, fontTextureName)
                .addXref(2, computeCharWidths)
            );

            addMemberMapper(new MethodMapper(getStringWidth));
            addMemberMapper(new MethodMapper(getCharWidth));

            addPatch(new AddFieldPatch(charWidthf));

            addPatch(new MakeMemberPublicPatch(fontTextureName) {
                @Override
                public int getNewFlags(int oldFlags) {
                    return oldFlags & ~AccessFlag.FINAL;
                }
            });

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override font name";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        begin()
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        ALOAD_0,
                        ALOAD_0,
                        reference(GETFIELD, fontTextureName),
                        reference(INVOKESTATIC, getFontName),
                        reference(PUTFIELD, fontTextureName)
                    );
                }
            }.targetMethod(readFontData));

            addPatch(new BytecodePatch() {
                private int imageRegister;
                private int rgbRegister;

                {
                    addPreMatchSignature(new BytecodeSignature() {
                        @Override
                        public String getMatchExpression() {
                            return buildExpression(
                                capture(anyALOAD),
                                reference(INVOKEVIRTUAL, getImageWidth),
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
                    return "FontUtils.computeCharWidthsf on init";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        RETURN
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // this.charWidthf = FontUtils.computeCharWidthsf(this, filename, image, rgb, this.charWidth);
                        ALOAD_0,
                        ALOAD_0,
                        ALOAD_1,
                        ALOAD, imageRegister,
                        ALOAD, rgbRegister,
                        ALOAD_0,
                        reference(GETFIELD, charWidth),
                        reference(INVOKESTATIC, computeCharWidthsf),
                        reference(PUTFIELD, charWidthf)
                    );
                }
            }
                .setInsertBefore(true)
                .targetMethod(computeCharWidths)
            );

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "use charWidthf instead of charWidth";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // return (float) this.charWidth[...];
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
                        // return FontUtils.getCharWidthf(this, this.charWidth, ...);
                        ALOAD_0,
                        ALOAD_0,
                        reference(GETFIELD, charWidth),
                        getCaptureGroup(1),
                        reference(INVOKESTATIC, getCharWidthf),
                        FRETURN
                    );
                }
            });

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override getStringWidth";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        begin()
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // if (FontUtils.enable) {
                        reference(GETSTATIC, enableFont),
                        IFEQ, branch("A"),

                        // return FontUtils.getStringWidthf(this, string);
                        ALOAD_0,
                        ALOAD_1,
                        reference(INVOKESTATIC, getStringWidthf),
                        F2I,
                        IRETURN,

                        // }
                        label("A")
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
            final MethodRef getImage = new MethodRef(MCPatcherUtils.FONT_UTILS_CLASS, "getImage", "(Ljava/lang/Object;Ljava/lang/String;)Ljava/awt/image/BufferedImage;");

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
                        reference(INVOKESTATIC, getImage)
                    );
                }
            });
        }
    }
}
