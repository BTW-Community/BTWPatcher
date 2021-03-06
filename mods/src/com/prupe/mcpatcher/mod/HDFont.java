package com.prupe.mcpatcher.mod;

import com.prupe.mcpatcher.*;
import com.prupe.mcpatcher.basemod.ResourceLocationMod;
import com.prupe.mcpatcher.mal.TexturePackAPIMod;
import javassist.bytecode.AccessFlag;

import static com.prupe.mcpatcher.BinaryRegex.*;
import static com.prupe.mcpatcher.BytecodeMatcher.*;
import static javassist.bytecode.Opcode.*;

public class HDFont extends Mod {
    private static boolean haveReadFontData;
    private static boolean haveFontWidthHack1;
    private static boolean haveFontWidthHack2;
    private static boolean renderDefaultCharTakesChar;
    private static MethodRef renderDefaultChar;

    public HDFont() {
        name = MCPatcherUtils.HD_FONT;
        author = "MCPatcher";
        description = "Provides support for higher resolution fonts.";
        version = "1.6";

        addDependency(MCPatcherUtils.TEXTURE_PACK_API_MOD);

        setupMod(this, getMinecraftVersion(), true);
    }

    static void setupMod(Mod mod, MinecraftVersion minecraftVersion, boolean addFiles) {
        haveReadFontData = minecraftVersion.compareTo("1.6.2") < 0;
        haveFontWidthHack1 = minecraftVersion.compareTo("1.6.2") >= 0;
        haveFontWidthHack2 = haveFontWidthHack1 && minecraftVersion.compareTo("1.8.2-pre6") < 0;
        renderDefaultCharTakesChar = minecraftVersion.compareTo("1.8.2-pre5") >= 0 && minecraftVersion.compareTo("1.8.2-pre6") <= 0;
        renderDefaultChar = new MethodRef("FontRenderer", "renderDefaultChar", "(" + (renderDefaultCharTakesChar ? "C" : "I") + "Z)F");

        mod.addClassMod(new FontRendererMod(mod));

        if (addFiles) {
            mod.addClassFile(MCPatcherUtils.FONT_UTILS_CLASS);
            mod.addClassFile(MCPatcherUtils.FONT_UTILS_CLASS + "$1");
        }

        TexturePackAPIMod.earlyInitialize(3, MCPatcherUtils.FONT_UTILS_CLASS, "init");
    }

    private static class FontRendererMod extends com.prupe.mcpatcher.basemod.FontRendererMod {
        private final FieldRef fontAdj = new FieldRef(getDeobfClass(), "fontAdj", "F");

        FontRendererMod(Mod mod) {
            super(mod);

            String d = ResourceLocationMod.getDescriptor();
            final FieldRef fontResource = new FieldRef(getDeobfClass(), "fontResource", d);
            final FieldRef charWidth = new FieldRef(getDeobfClass(), "charWidth", "[I");
            final FieldRef fontHeight = new FieldRef(getDeobfClass(), "fontHeight", "I");
            final FieldRef charWidthf = new FieldRef(getDeobfClass(), "charWidthf", "[F");
            final FieldRef defaultFont = new FieldRef(getDeobfClass(), "defaultFont", d);
            final FieldRef hdFont = new FieldRef(getDeobfClass(), "hdFont", d);
            final FieldRef isHD = new FieldRef(getDeobfClass(), "isHD", "Z");
            final MethodRef readFontData = new MethodRef(getDeobfClass(), "readFontData", "()V");
            final MethodRef getStringWidth = new MethodRef(getDeobfClass(), "getStringWidth", "(Ljava/lang/String;)I");
            final MethodRef getCharWidth = new MethodRef(getDeobfClass(), "getCharWidth", "(C)I");
            String computeCharWidthsArg = ResourceLocationMod.select("Ljava/lang/String;", "");
            final MethodRef computeCharWidths = haveReadFontData ? new MethodRef(getDeobfClass(), "computeCharWidths", "(" + computeCharWidthsArg + ")V") : readFontData;
            final MethodRef getResourceAsStream = new MethodRef("java/lang/Class", "getResourceAsStream", "(Ljava/lang/String;)Ljava/io/InputStream;");
            final MethodRef readImage = new MethodRef("javax/imageio/ImageIO", "read", "(Ljava/io/InputStream;)Ljava/awt/image/BufferedImage;");
            final MethodRef getImageWidth = new MethodRef("java/awt/image/BufferedImage", "getWidth", "()I");
            final MethodRef getFontName = new MethodRef(MCPatcherUtils.FONT_UTILS_CLASS, "getFontName", "(LFontRenderer;LResourceLocation;F)LResourceLocation;");
            final MethodRef computeCharWidthsf = new MethodRef(MCPatcherUtils.FONT_UTILS_CLASS, "computeCharWidthsf", "(LFontRenderer;LResourceLocation;Ljava/awt/image/BufferedImage;[I[I)[F");
            final MethodRef getCharWidthf = new MethodRef(MCPatcherUtils.FONT_UTILS_CLASS, "getCharWidthf", "(LFontRenderer;[II)F");
            final MethodRef getStringWidthf = new MethodRef(MCPatcherUtils.FONT_UTILS_CLASS, "getStringWidthf", "(LFontRenderer;Ljava/lang/String;)F");
            final MethodRef getNewImage = new MethodRef(MCPatcherUtils.TEXTURE_PACK_API_CLASS, "getImage", "(LResourceLocation;)Ljava/awt/image/BufferedImage;");

            addClassSignature(new BytecodeSignature() {
                {
                    matchConstructorOnly(true);
                    addXref(1, fontHeight);
                    addXref(2, fontResource);
                }

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
            });

            if (haveReadFontData) {
                addClassSignature(new BytecodeSignature() {
                    @Override
                    public String getMatchExpression() {
                        return buildExpression(
                            begin(),
                            ALOAD_0,
                            anyReference(INVOKESPECIAL),
                            ALOAD_0,
                            ResourceLocationMod.haveClass() ? "" : build(ALOAD_0, anyReference(GETFIELD)),
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
            addPatch(new AddFieldPatch(defaultFont, AccessFlag.PRIVATE));
            addPatch(new AddFieldPatch(hdFont, AccessFlag.PRIVATE));
            addFontMethods(defaultFont, "DefaultFont");
            addFontMethods(hdFont, "HDFont");
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
                        // this.fontResource = FontUtils.getFontName(this, this.fontResource, hdFontAdj);
                        ALOAD_0,
                        ALOAD_0,
                        ALOAD_0,
                        reference(GETFIELD, fontResource),
                        ResourceLocationMod.wrap(this),
                        push(haveFontWidthHack2 ? 0.0f : 1.0f),
                        reference(INVOKESTATIC, getFontName),
                        ResourceLocationMod.unwrap(this),
                        reference(PUTFIELD, fontResource),
                        ResourceLocationMod.haveClass() ? new byte[0] :
                            buildCode(
                                ALOAD_0,
                                reference(GETFIELD, fontResource),
                                ASTORE_1
                            )
                    );
                }
            }.targetMethod(readFontData));

            if (!ResourceLocationMod.haveClass()) {
                addPatch(new BytecodePatch() {
                    @Override
                    public String getDescription() {
                        return "override font image";
                    }

                    @Override
                    public String getMatchExpression() {
                        return buildExpression(
                            // ImageIO.read(RenderEngine.class.getResourceAsStream(name));
                            anyLDC,
                            ALOAD_1,
                            reference(INVOKEVIRTUAL, getResourceAsStream),
                            reference(INVOKESTATIC, readImage)
                        );
                    }

                    @Override
                    public byte[] getReplacementBytes() {
                        return buildCode(
                            // TexturePackAPI.getImage(name)
                            ALOAD_1,
                            ResourceLocationMod.wrap(this),
                            reference(INVOKESTATIC, getNewImage)
                        );
                    }
                });
            }

            addPatch(new BytecodePatch() {
                private int imageRegister;
                private int rgbRegister;

                {
                    setInsertBefore(true);
                    targetMethod(computeCharWidths);

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
                        ALOAD_0,
                        reference(GETFIELD, fontResource),
                        ResourceLocationMod.wrap(this),
                        ALOAD, imageRegister,
                        ALOAD, rgbRegister,
                        ALOAD_0,
                        reference(GETFIELD, charWidth),
                        reference(INVOKESTATIC, computeCharWidthsf),
                        reference(PUTFIELD, charWidthf)
                    );
                }
            });

            if (getMinecraftVersion().compareTo("1.8.2-pre5") >= 0) {
                addPatch(new BytecodePatch() {
                    {
                        targetMethod(renderDefaultChar);
                    }

                    @Override
                    public String getDescription() {
                        return "use charWidthf instead of charWidth";
                    }

                    @Override
                    public String getMatchExpression() {
                        return buildExpression(
                            // (float) char1
                            ILOAD, 6,
                            I2F
                        );
                    }

                    @Override
                    public byte[] getReplacementBytes() {
                        return buildCode(
                            // FontUtils.getCharWidthf(this, this.charWidth, char1)
                            ALOAD_0,
                            ALOAD_0,
                            reference(GETFIELD, charWidth),
                            ILOAD_1,
                            reference(INVOKESTATIC, getCharWidthf)
                        );
                    }
                });
            } else {
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
            }

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

            if (ResourceLocationMod.haveClass()) {
                setupUnicode();
            }
            if (haveFontWidthHack1) {
                setupFontHack();
            }
        }

        private void addFontMethods(final FieldRef field, String name) {
            addPatch(new AddMethodPatch(new MethodRef(getDeobfClass(), "get" + name, "()LResourceLocation;")) {
                @Override
                public byte[] generateMethod() {
                    return buildCode(
                        ALOAD_0,
                        reference(GETFIELD, field),
                        ResourceLocationMod.wrap(this),
                        ARETURN
                    );
                }
            });

            addPatch(new AddMethodPatch(new MethodRef(getDeobfClass(), "set" + name, "(LResourceLocation;)V")) {
                @Override
                public byte[] generateMethod() {
                    return buildCode(
                        ALOAD_0,
                        ALOAD_1,
                        ResourceLocationMod.unwrap(this),
                        reference(PUTFIELD, field),
                        RETURN
                    );
                }
            });
        }

        private void setupUnicode() {
            final MethodRef getUnicodePage = new MethodRef(getDeobfClass(), "getUnicodePage", "(I)LResourceLocation;");
            final MethodRef getUnicodePage1 = new MethodRef(MCPatcherUtils.FONT_UTILS_CLASS, "getUnicodePage", "(LResourceLocation;)LResourceLocation;");

            addMemberMapper(new MethodMapper(getUnicodePage));

            addPatch(new BytecodePatch() {
                {
                    setInsertBefore(true);
                    targetMethod(getUnicodePage);
                }

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
            });
        }

        private void setupFontHack() {
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
