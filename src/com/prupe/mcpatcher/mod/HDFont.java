package com.prupe.mcpatcher.mod;

import com.prupe.mcpatcher.*;
import javassist.bytecode.AccessFlag;

import static com.prupe.mcpatcher.BinaryRegex.*;
import static com.prupe.mcpatcher.BytecodeMatcher.*;
import static javassist.bytecode.Opcode.*;

public class HDFont extends Mod {
    private static boolean haveReadFontData;
    private static boolean haveFontWidthHack;

    public HDFont() {
        name = MCPatcherUtils.HD_FONT;
        author = "MCPatcher";
        description = "Provides support for higher resolution fonts.";
        version = "1.6";

        addDependency(MCPatcherUtils.BASE_TEXTURE_PACK_MOD);

        setupMod(this, getMinecraftVersion());
    }

    static void setupMod(Mod mod, MinecraftVersion minecraftVersion) {
        haveReadFontData = minecraftVersion.compareTo("1.6.2") < 0;
        haveFontWidthHack = minecraftVersion.compareTo("1.6.2") >= 0;

        mod.addClassMod(new FontRendererMod(mod));

        mod.addClassFile(MCPatcherUtils.FONT_UTILS_CLASS);
        mod.addClassFile(MCPatcherUtils.FONT_UTILS_CLASS + "$1");

        BaseTexturePackMod.earlyInitialize(3, MCPatcherUtils.FONT_UTILS_CLASS, "init");
    }

    private static class FontRendererMod extends BaseMod.FontRendererMod {
        private final FieldRef fontAdj = new FieldRef(getDeobfClass(), "fontAdj", "F");

        FontRendererMod(Mod mod) {
            super(mod);

            final FieldRef fontResource = new FieldRef(getDeobfClass(), "fontResource", "LResourceLocation;");
            final FieldRef charWidth = new FieldRef(getDeobfClass(), "charWidth", "[I");
            final FieldRef fontHeight = new FieldRef(getDeobfClass(), "fontHeight", "I");
            final FieldRef charWidthf = new FieldRef(getDeobfClass(), "charWidthf", "[F");
            final FieldRef defaultFont = new FieldRef(getDeobfClass(), "defaultFont", "LResourceLocation;");
            final FieldRef hdFont = new FieldRef(getDeobfClass(), "hdFont", "LResourceLocation;");
            final FieldRef isHD = new FieldRef(getDeobfClass(), "isHD", "Z");
            final MethodRef readFontData = new MethodRef(getDeobfClass(), "readFontData", "()V");
            final MethodRef getStringWidth = new MethodRef(getDeobfClass(), "getStringWidth", "(Ljava/lang/String;)I");
            final MethodRef getCharWidth = new MethodRef(getDeobfClass(), "getCharWidth", "(C)I");
            final MethodRef computeCharWidths = haveReadFontData ? new MethodRef(getDeobfClass(), "computeCharWidths", "()V") : readFontData;
            final MethodRef getImageWidth = new MethodRef("java/awt/image/BufferedImage", "getWidth", "()I");
            final MethodRef getFontName = new MethodRef(MCPatcherUtils.FONT_UTILS_CLASS, "getFontName", "(LFontRenderer;LResourceLocation;)LResourceLocation;");
            final MethodRef computeCharWidthsf = new MethodRef(MCPatcherUtils.FONT_UTILS_CLASS, "computeCharWidthsf", "(LFontRenderer;LResourceLocation;Ljava/awt/image/BufferedImage;[I[IF)[F");
            final MethodRef getCharWidthf = new MethodRef(MCPatcherUtils.FONT_UTILS_CLASS, "getCharWidthf", "(LFontRenderer;[II)F");
            final MethodRef getStringWidthf = new MethodRef(MCPatcherUtils.FONT_UTILS_CLASS, "getStringWidthf", "(LFontRenderer;Ljava/lang/String;)F");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        push(9),
                        captureReference(PUTFIELD),

                        any(0, 100),

                        ALOAD_0,
                        ALOAD_2,
                        captureReference(PUTFIELD)
                    );
                }
            }
                .matchConstructorOnly(true)
                .addXref(1, fontHeight)
                .addXref(2, fontResource)
            );

            if (haveReadFontData) {
                addClassSignature(new BytecodeSignature() {
                    @Override
                    public String getMatchExpression() {
                        return buildExpression(
                            begin(),
                            ALOAD_0,
                            anyReference(INVOKESPECIAL),
                            ALOAD_0,
                            anyReference(INVOKESPECIAL)
                        );
                    }
                }.setMethod(readFontData));
            }

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        reference(INVOKEVIRTUAL, getImageWidth)
                    );
                }
            }.setMethod(computeCharWidths));

            addMemberMapper(new MethodMapper(getStringWidth));
            addMemberMapper(new MethodMapper(getCharWidth));

            addPatch(new AddFieldPatch(charWidthf));
            addPatch(new AddFieldPatch(defaultFont));
            addPatch(new AddFieldPatch(hdFont));
            addPatch(new AddFieldPatch(isHD));
            addPatch(new AddFieldPatch(fontAdj));

            addPatch(new MakeMemberPublicPatch(readFontData));
            addPatch(new MakeMemberPublicPatch(fontResource) {
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
                        // this.fontResource = FontUtils.getFontName(this, this.fontResource);
                        ALOAD_0,
                        ALOAD_0,
                        ALOAD_0,
                        reference(GETFIELD, fontResource),
                        reference(INVOKESTATIC, getFontName),
                        reference(PUTFIELD, fontResource)
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
                        // this.charWidthf = FontUtils.computeCharWidthsf(this, filename, image, rgb, this.charWidth, fontAdj);
                        ALOAD_0,
                        ALOAD_0,
                        ALOAD_0,
                        reference(GETFIELD, fontResource),
                        ALOAD, imageRegister,
                        ALOAD, rgbRegister,
                        ALOAD_0,
                        reference(GETFIELD, charWidth),
                        push(haveFontWidthHack ? 1.0f : 0.0f),
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
                        // if (this.isHD) {
                        ALOAD_0,
                        reference(GETFIELD, isHD),
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

            setupUnicode();
            if (haveFontWidthHack) {
                setupFontHack();
            }
        }

        private void setupUnicode() {
            final MethodRef getUnicodePage = new MethodRef(getDeobfClass(), "getUnicodePage", "(I)LResourceLocation;");
            final MethodRef getUnicodePage1 = new MethodRef(MCPatcherUtils.FONT_UTILS_CLASS, "getUnicodePage", "(LResourceLocation;)LResourceLocation;");

            addMemberMapper(new MethodMapper(getUnicodePage));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override unicode font name";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ARETURN
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        reference(INVOKESTATIC, getUnicodePage1)
                    );
                }
            }
                .setInsertBefore(true)
                .targetMethod(getUnicodePage)
            );
        }

        private void setupFontHack() {
            final MethodRef renderDefaultChar = new MethodRef(getDeobfClass(), "renderDefaultChar", "(IZ)F");
            final MethodRef glTexCoord2f = new MethodRef(MCPatcherUtils.GL11_CLASS, "glTexCoord2f", "(FF)V");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        reference(INVOKESTATIC, glTexCoord2f)
                    );
                }
            }.setMethod(renderDefaultChar));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "undo font adjustment";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(1.0f),
                        FSUB
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        ALOAD_0,
                        reference(GETFIELD, fontAdj),
                        FSUB
                    );
                }
            }.targetMethod(renderDefaultChar));

        }
    }
}
