package com.prupe.mcpatcher.mod;

import com.prupe.mcpatcher.*;
import javassist.bytecode.AccessFlag;

import static com.prupe.mcpatcher.BinaryRegex.*;
import static com.prupe.mcpatcher.BytecodeMatcher.*;
import static javassist.bytecode.Opcode.*;

public class ExtendedHD extends Mod {
    private static final MethodRef copySubTexture1 = new MethodRef("TextureUtils", "copySubTexture1", "([IIIIIZZ)V");
    private static final MethodRef copySubTexture2 = new MethodRef("TextureUtils", "copySubTexture2", "(Ljava/awt/image/BufferedImage;IIZZ)V");
    private static final MethodRef setupTexture1 = new MethodRef("TextureUtils", "setupTexture1", "(ILjava/awt/image/BufferedImage;ZZ)I");
    private static final MethodRef setupTexture3 = new MethodRef("TextureUtils", "setupTexture3", "(III)V");

    private static final MethodRef setupTextureMipmaps1 = new MethodRef(MCPatcherUtils.MIPMAP_HELPER_CLASS, "setupTexture", "([IIIIILjava/lang/String;)V");
    private static final MethodRef setupTextureMipmaps2 = new MethodRef(MCPatcherUtils.MIPMAP_HELPER_CLASS, "setupTexture", "(ILjava/awt/image/BufferedImage;ZZLjava/lang/String;)I");
    private static final MethodRef setupTextureMipmaps3 = new MethodRef(MCPatcherUtils.MIPMAP_HELPER_CLASS, "setupTexture", "(IIILjava/lang/String;)V");
    private static final MethodRef copySubTextureMipmaps = new MethodRef(MCPatcherUtils.MIPMAP_HELPER_CLASS, "copySubTexture", "([IIIIILjava/lang/String;)V");
    private static final FieldRef textureBorder = new FieldRef("Texture", "border", "I");

    private static final MethodRef imageRead = new MethodRef("javax/imageio/ImageIO", "read", "(Ljava/io/InputStream;)Ljava/awt/image/BufferedImage;");

    public ExtendedHD() {
        name = MCPatcherUtils.EXTENDED_HD;
        author = "MCPatcher";
        description = "Provides support for custom animations, HD fonts, mipmapping, and other graphical features.";
        version = "2.2";

        configPanel = new HDConfig();

        addDependency(BaseTexturePackMod.NAME);

        if (getMinecraftVersion().compareTo("13w09b") < 0) {
            addError("Requires Minecraft 13w09b or newer");
            return;
        }

        addClassMod(new MinecraftMod());
        //addClassMod(new RenderEngineMod());
        //addClassMod(new ColorizerMod("ColorizerGrass"));
        //addClassMod(new ColorizerMod("ColorizerFoliage"));
        addClassMod(new BaseMod.IconMod());
        addClassMod(new BaseMod.ITextureMod());
        addClassMod(new BaseMod.TextureBaseMod());
        addClassMod(new BaseMod.TextureMod());
        addClassMod(new TextureUtilsMod());
        //addClassMod(new TextureManagerMod());
        addClassMod(new TextureMapMod());
        addClassMod(new TextureStitchedMod());
        addClassMod(new TextureNamedMod());
        addClassMod(new TextureCompassMod());
        addClassMod(new TextureClockMod());
        //HDFont.setupMod(this);

        addClassFile(MCPatcherUtils.CUSTOM_ANIMATION_CLASS);
        addClassFile(MCPatcherUtils.CUSTOM_ANIMATION_CLASS + "$1");
        addClassFile(MCPatcherUtils.CUSTOM_ANIMATION_CLASS + "$1$1");
        addClassFile(MCPatcherUtils.MIPMAP_HELPER_CLASS);
        addClassFile(MCPatcherUtils.AA_HELPER_CLASS);
        addClassFile(MCPatcherUtils.BORDERED_TEXTURE_CLASS);
        addClassFile(MCPatcherUtils.FANCY_DIAL_CLASS);
        addClassFile(MCPatcherUtils.FANCY_DIAL_CLASS + "$Layer");

        getClassMap().addInheritance("TextureStitched", MCPatcherUtils.BORDERED_TEXTURE_CLASS);
    }

    @Override
    public String[] getLoggingCategories() {
        return new String[]{
            MCPatcherUtils.CUSTOM_ANIMATIONS,
            MCPatcherUtils.MIPMAP,
            MCPatcherUtils.HD_FONT,
        };
    }

    private class MinecraftMod extends BaseMod.MinecraftMod {
        MinecraftMod() {
            //final FieldRef renderEngine = new FieldRef(getDeobfClass(), "renderEngine", "LRenderEngine;");

            //addColorizerSignature("Grass");
            //addColorizerSignature("Foliage");

            //addMemberMapper(new FieldMapper(renderEngine));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "enable anti-aliasing";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        reference(INVOKESTATIC, new MethodRef("org/lwjgl/opengl/Display", "create", "(Lorg/lwjgl/opengl/PixelFormat;)V"))
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.AA_HELPER_CLASS, "setupPixelFormat", "(Lorg/lwjgl/opengl/PixelFormat;)Lorg/lwjgl/opengl/PixelFormat;"))
                    );
                }
            }.setInsertBefore(true));
        }

        private void addColorizerSignature(final String name) {
            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push("/misc/" + name.toLowerCase() + "color.png"),
                        anyReference(INVOKEVIRTUAL),
                        captureReference(INVOKESTATIC)
                    );
                }
            }.addXref(1, new MethodRef("Colorizer" + name, "loadColorBuffer", "([I)V")));
        }
    }

    private class RenderEngineMod extends BaseMod.RenderEngineMod {
        RenderEngineMod() {
            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "update custom animations";
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
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.CUSTOM_ANIMATION_CLASS, "updateAll", "()V"))
                    );
                }
            }
                .setInsertBefore(true)
                .targetMethod(updateDynamicTextures)
            );

            addMipmappingPatches();
        }

        private void addMipmappingPatches() {
            final MethodRef setupTextureExt = new MethodRef(getDeobfClass(), "setupTextureExt", "(Ljava/awt/image/BufferedImage;IZZ)V");
            final MethodRef setupTextureMipmaps = new MethodRef(MCPatcherUtils.MIPMAP_HELPER_CLASS, "setupTexture", "(LRenderEngine;Ljava/awt/image/BufferedImage;IZZLjava/lang/String;)V");
            final MethodRef glTexImage2D = new MethodRef(MCPatcherUtils.GL11_CLASS, "glTexImage2D", "(IIIIIIII" + imageData.getType() + ")V");
            final FieldRef currentMipmapLevel = new FieldRef(MCPatcherUtils.MIPMAP_HELPER_CLASS, "currentLevel", "I");
            final FieldRef enableTextureBorder = new FieldRef(MCPatcherUtils.TEXTURE_PACK_API_CLASS, "enableTextureBorder", "Z");
            final MethodRef getImageWidth = new MethodRef("java/awt/image/BufferedImage", "getWidth", "()I");
            final MethodRef startsWith = new MethodRef("java/lang/String", "startsWith", "(Ljava/lang/String;)Z");

            addMemberMapper(new MethodMapper(setupTextureExt));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override mipmap level in setupTexture";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, var3, var4, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, this.imageData);
                        lookBehind(build(
                            push(3553)
                        ), true),
                        push(0),
                        lookAhead(build(
                            any(0, 24),
                            anyILOAD,
                            anyILOAD,
                            push(0),
                            or(build(push(6408)), build(push(32993 /* GL11.GL_BGRA */))),
                            or(build(push(5121)), build(push(33639 /* GL_UNSIGNED_INT_8_8_8_8_REV */))),
                            ALOAD_0,
                            reference(GETFIELD, imageData),
                            reference(INVOKESTATIC, glTexImage2D)
                        ), true)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // GL11.glTexImage2D(..., MipmapHelper.currentLevel, ...);
                        reference(GETSTATIC, currentMipmapLevel)
                    );
                }
            }.targetMethod(setupTextureExt));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "preserve texture parameters during mipmap creation";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        begin(),
                        capture(nonGreedy(any(0, 300))),
                        capture(build(
                            ALOAD_1,
                            reference(INVOKEVIRTUAL, getImageWidth)
                        ))
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        reference(GETSTATIC, new FieldRef(MCPatcherUtils.MIPMAP_HELPER_CLASS, "currentLevel", "I")),
                        IFNE, branch("A"),
                        getCaptureGroup(1),
                        label("A"),
                        getCaptureGroup(2)
                    );
                }
            }.targetMethod(setupTextureExt));

            addPatch(new BytecodePatch() {
                private byte[] pushTextureName;
                private int position;

                {
                    addPreMatchSignature(new BytecodeSignature() {
                        @Override
                        public String getMatchExpression() {
                            return buildExpression(
                                capture(anyALOAD),
                                push("%blur%"),
                                reference(INVOKEVIRTUAL, startsWith)
                            );
                        }

                        @Override
                        public boolean afterMatch() {
                            pushTextureName = getCaptureGroup(1);
                            position = matcher.getStart();
                            return true;
                        }
                    });
                }

                @Override
                public String getDescription() {
                    return "generate mipmaps during texture setup";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        reference(INVOKEVIRTUAL, setupTextureExt)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    if (matcher.getStart() < position) {
                        return null;
                    } else {
                        return buildCode(
                            pushTextureName,
                            reference(INVOKESTATIC, setupTextureMipmaps)
                        );
                    }
                }
            });

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "enable texture border on terrain";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // this.terrain.refresh();
                        ALOAD_0,
                        reference(GETFIELD, textureMapBlocks),
                        anyReference(INVOKEVIRTUAL)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // TexturePackAPI.enableTextureBorder = true;
                        push(1),
                        reference(PUTSTATIC, enableTextureBorder),

                        // ...
                        getMatch(),

                        // TexturePackAPI.enableTextureBorder = false;
                        push(0),
                        reference(PUTSTATIC, enableTextureBorder)
                    );
                }
            }.targetMethod(refreshTextureMaps));
        }
    }

    private class ColorizerMod extends ClassMod {
        private final String name;

        ColorizerMod(String name) {
            this.name = name;

            final FieldRef colorBuffer = new FieldRef(getDeobfClass(), "colorBuffer", "[I");

            addMemberMapper(new FieldMapper(colorBuffer));

            addPatch(new MakeMemberPublicPatch(colorBuffer));

            addPrerequisiteClass("Minecraft");
        }

        @Override
        public String getDeobfClass() {
            return name;
        }
    }

    private class TextureUtilsMod extends BaseMod.TextureUtilsMod {
        TextureUtilsMod() {
            addMemberMapper(new MethodMapper(copySubTexture1));
            addMemberMapper(new MethodMapper(copySubTexture2));
            addMemberMapper(new MethodMapper(setupTexture1));
            addMemberMapper(new MethodMapper(setupTexture3));
        }
    }

    private class TextureMod extends BaseMod.TextureMod {
        TextureMod() {
            final FieldRef textureMinFilter = mapIntField(7, "textureMinFilter");
            final FieldRef textureMagFilter = mapIntField(8, "textureMagFilter");
            final FieldRef mipmapActive = new FieldRef(getDeobfClass(), "mipmapActive", "Z");
            final MethodRef copyFrom = new MethodRef(getDeobfClass(), "copyFrom", "(IILTexture;Z)V");
            final MethodRef copyFromSub = new MethodRef(getDeobfClass(), "copyFromSub", "(IILTexture;)V");
            final FieldRef textureData = new FieldRef(getDeobfClass(), "textureData", "Ljava/nio/ByteBuffer;");
            final MethodRef allocateDirect = new MethodRef("java/nio/ByteBuffer", "allocateDirect", "(I)Ljava/nio/ByteBuffer;");
            final MethodRef glTexImage2D = new MethodRef(MCPatcherUtils.GL11_CLASS, "glTexImage2D", "(IIIIIIIILjava/nio/ByteBuffer;)V");
            final MethodRef glTexSubImage2D = new MethodRef(MCPatcherUtils.GL11_CLASS, "glTexSubImage2D", "(IIIIIIIILjava/nio/ByteBuffer;)V");
            final MethodRef allocateByteBuffer = new MethodRef(MCPatcherUtils.MIPMAP_HELPER_CLASS, "allocateByteBuffer", "(I)Ljava/nio/ByteBuffer;");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        nonGreedy(any(0, 20)),
                        push(9728), // GL_NEAREST
                        nonGreedy(any(0, 20)),
                        captureReference(PUTFIELD)
                    );
                }
            }
                .matchConstructorOnly(true)
                .addXref(1, mipmapActive)
            );

            addMemberMapper(new MethodMapper(copyFrom));
            addMemberMapper(new FieldMapper(textureData));

            addPatch(new MakeMemberPublicPatch(textureMinFilter) {
                @Override
                public int getNewFlags(int oldFlags) {
                    return super.getNewFlags(oldFlags) & ~AccessFlag.FINAL;
                }
            });

            addPatch(new MakeMemberPublicPatch(textureMagFilter) {
                @Override
                public int getNewFlags(int oldFlags) {
                    return super.getNewFlags(oldFlags) & ~AccessFlag.FINAL;
                }
            });

            addPatch(new MakeMemberPublicPatch(mipmapActive) {
                @Override
                public int getNewFlags(int oldFlags) {
                    return super.getNewFlags(oldFlags) & ~AccessFlag.FINAL;
                }
            });

            addPatch(new MakeMemberPublicPatch(textureData));
            addPatch(new AddFieldPatch(textureBorder));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "generate mipmaps";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        reference(INVOKESTATIC, glTexImage2D)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        ALOAD_0,
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.MIPMAP_HELPER_CLASS, "setupTexture", "(IIIIIIIILjava/nio/ByteBuffer;LTexture;)V"))
                    );
                }
            });

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override byte buffer allocation";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        reference(INVOKESTATIC, allocateDirect)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        reference(INVOKESTATIC, allocateByteBuffer)
                    );
                }
            });

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "replace copyFrom";
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
                        // if (this.loaded) {
                        ALOAD_0,
                        reference(GETFIELD, textureCreated),
                        IFEQ, branch("A"),

                        // MipmapHelper.copySubTexture(this, src, x, y, flipped);
                        ALOAD_0,
                        ALOAD_3,
                        ILOAD_1,
                        ILOAD_2,
                        ILOAD, 4,
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.MIPMAP_HELPER_CLASS, "copySubTexture", "(LTexture;LTexture;IIZ)V")),
                        RETURN,

                        // }
                        label("A")
                    );
                }
            }.targetMethod(copyFrom));

            // 1.5.2 (.3, .4, ...) or 13w17a+
            if ((getMinecraftVersion().compareTo("1.5.2") >= 0 && getMinecraftVersion().compareTo("13w16a") < 0) ||
                getMinecraftVersion().compareTo("13w17a") >= 0) {
                addClassSignature(new BytecodeSignature() {
                    @Override
                    public String getMatchExpression() {
                        return buildExpression(
                            reference(INVOKESTATIC, glTexSubImage2D)
                        );
                    }
                }.setMethod(copyFromSub));

                addPatch(new BytecodePatch() {
                    @Override
                    public String getDescription() {
                        return "replace copyFromSub";
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
                            // if (this.loaded) {
                            ALOAD_0,
                            reference(GETFIELD, textureCreated),
                            IFEQ, branch("A"),

                            // MipmapHelper.copySubTexture(this, src, x, y, false);
                            ALOAD_0,
                            ALOAD_3,
                            ILOAD_1,
                            ILOAD_2,
                            push(0),
                            reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.MIPMAP_HELPER_CLASS, "copySubTexture", "(LTexture;LTexture;IIZ)V")),
                            RETURN,

                            // }
                            label("A")
                        );
                    }
                }.targetMethod(copyFromSub));
            }
        }

        private FieldRef mapIntField(final int register, String name) {
            final FieldRef field = new FieldRef(getDeobfClass(), name, "I");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        registerLoadStore(ILOAD, register),
                        captureReference(PUTFIELD)
                    );
                }
            }
                .matchConstructorOnly(true)
                .addXref(1, field)
            );

            return field;
        }
    }

    private class TextureManagerMod extends ClassMod {
        TextureManagerMod() {
            final MethodRef createTextureFromImage = new MethodRef(getDeobfClass(), "createTextureFromImage", "(Ljava/lang/String;IIIIIIIZLjava/awt/image/BufferedImage;)LTexture;");
            final MethodRef lastIndexOf = new MethodRef("java/lang/String", "lastIndexOf", "(I)I");

            addClassSignature(new ConstSignature("/"));
            addClassSignature(new ConstSignature(".txt"));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(46),
                        reference(INVOKEVIRTUAL, lastIndexOf)
                    );
                }
            });

            addMemberMapper(new MethodMapper(createTextureFromImage));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "add texture border";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        reference(NEW, new ClassRef("Texture")),
                        DUP,
                        any(0, 30),
                        anyReference(INVOKESPECIAL),
                        ASTORE, capture(any())
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // image = AAHelper.addBorder(name, image, false);
                        ALOAD_1,
                        ALOAD, 10,
                        push(0),
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.AA_HELPER_CLASS, "addBorder", "(Ljava/lang/String;Ljava/awt/image/BufferedImage;Z)Ljava/awt/image/BufferedImage;")),
                        ASTORE, 10,

                        // if (image != null) {
                        ALOAD, 10,
                        IFNULL, branch("A"),

                        // width = image.getWidth();
                        ALOAD, 10,
                        reference(INVOKEVIRTUAL, new MethodRef("java/awt/image/BufferedImage", "getWidth", "()I")),
                        ISTORE_3,

                        // height = image.getHeight();
                        ALOAD, 10,
                        reference(INVOKEVIRTUAL, new MethodRef("java/awt/image/BufferedImage", "getHeight", "()I")),
                        ISTORE, 4,

                        // }
                        label("A"),

                        // ...
                        getMatch(),

                        // texture.border = AAHelper.border;
                        ALOAD, getCaptureGroup(1),
                        reference(GETSTATIC, new FieldRef(MCPatcherUtils.AA_HELPER_CLASS, "border", "I")),
                        reference(PUTFIELD, textureBorder)
                    );
                }
            }.targetMethod(createTextureFromImage));
        }
    }

    private class TextureMapMod extends BaseMod.TextureMapMod {
        TextureMapMod() {
            final MethodRef readTile = new MethodRef(getDeobfClass(), "readTile", "(LTextureStitched;LITexturePack;Ljava/lang/String;)Z");
            final ClassRef textureStitched = new ClassRef("TextureStitched");
            final MethodRef textureStitchedConstructor = new MethodRef("TextureStitched", "<init>", "(Ljava/lang/String;)V");
            final MethodRef addBorder = new MethodRef(MCPatcherUtils.AA_HELPER_CLASS, "addBorder", "(LTextureStitched;Ljava/lang/String;Ljava/awt/image/BufferedImage;)Ljava/awt/image/BufferedImage;");
            final MethodRef createTextureStitched = new MethodRef(MCPatcherUtils.BORDERED_TEXTURE_CLASS, "create", "(Ljava/lang/String;Ljava/lang/String;)LTextureStitched;");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // TextureUtils.setupTexture(t.getFrameRGB(0), t.getWidth(), t.getHeight(), t.getX0(), t.getY0(), false, false);
                        capture(anyALOAD),
                        push(0),
                        captureReference(INVOKEVIRTUAL),
                        backReference(1),
                        anyReference(INVOKEVIRTUAL),
                        backReference(1),
                        anyReference(INVOKEVIRTUAL),
                        backReference(1),
                        captureReference(INVOKEVIRTUAL),
                        backReference(1),
                        captureReference(INVOKEVIRTUAL),
                        push(0),
                        push(0),
                        anyReference(INVOKESTATIC)
                    );
                }
            }
                .addXref(2, new MethodRef("TextureStitched", "getFrameRGB", "(I)[I"))
                .addXref(3, new MethodRef("TextureStitched", "getX0", "()I"))
                .addXref(4, new MethodRef("TextureStitched", "getY0", "()I"))
            );

            addMemberMapper(new MethodMapper(readTile));

            addPatch(new TextureMipmapPatch(basePath));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "enable mipmapping for tilesheets";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        reference(INVOKESTATIC, setupTexture3)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        ALOAD_0,
                        reference(GETFIELD, basePath),
                        reference(INVOKESTATIC, setupTextureMipmaps3)
                    );
                }
            });

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "add tile border for aa";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_2,
                        capture(anyALOAD),
                        anyReference(INVOKEINTERFACE),
                        reference(INVOKESTATIC, imageRead)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        ALOAD_1,
                        getCaptureGroup(1),
                        getMatch(),
                        reference(INVOKESTATIC, addBorder)
                    );
                }
            }.targetMethod(readTile));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override texture coordinates for aa";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        reference(NEW, textureStitched),
                        DUP,
                        ALOAD_1,
                        reference(INVOKESPECIAL, textureStitchedConstructor)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        ALOAD_0,
                        reference(GETFIELD, basePath),
                        ALOAD_1,
                        reference(INVOKESTATIC, createTextureStitched)
                    );
                }
            });
        }
    }

    private class TextureStitchedMod extends BaseMod.TextureStitchedMod {
        TextureStitchedMod() {
            final MethodRef init = new MethodRef(getDeobfClass(), "init", "(IIIIZ)V");
            final MethodRef copy = new MethodRef(getDeobfClass(), "copy", "(LTextureStitched;)V");

            addMemberMapper(new MethodMapper(init));
            addMemberMapper(new MethodMapper(copy));

            addPatch(new TextureMipmapPatch(textureName));
        }
    }

    private static class TextureMipmapPatch extends BytecodePatch {
        private final FieldRef textureNameField;

        TextureMipmapPatch(FieldRef textureNameField) {
            this.textureNameField = textureNameField;
        }

        @Override
        public String getDescription() {
            return "generate mipmaps";
        }

        @Override
        public String getMatchExpression() {
            return buildExpression(
                reference(INVOKESTATIC, copySubTexture1)
            );
        }

        @Override
        public byte[] getReplacementBytes() {
            return buildCode(
                POP,
                POP,
                ALOAD_0,
                reference(GETFIELD, textureNameField),
                reference(INVOKESTATIC, copySubTextureMipmaps)
            );
        }
    }

    private class TextureNamedMod extends ClassMod {
        TextureNamedMod() {
            setParentClass("TextureBase");

            final FieldRef textureName = new FieldRef(getDeobfClass(), "textureName", "Ljava/lang/String;");
            final MethodRef load = new MethodRef(getDeobfClass(), "load", "(LITexturePack;)V");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_2,
                        reference(INVOKESTATIC, imageRead),
                        ASTORE_3
                    );
                }
            }.setMethod(load));

            addMemberMapper(new FieldMapper(textureName));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "generate mipmaps";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        reference(INVOKESTATIC, setupTexture1)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        ALOAD_0,
                        reference(GETFIELD, textureName),
                        reference(INVOKESTATIC, setupTextureMipmaps2)
                    );
                }
            });
        }
    }

    abstract private class TextureDialMod extends ClassMod {
        protected final FieldRef currentAngle = new FieldRef(getDeobfClass(), "currentAngle", "D");
        protected final MethodRef update = getUpdateMethod();

        TextureDialMod(final String name) {
            setParentClass("TextureStitched");

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "setup custom " + name;
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
                        ALOAD_0,
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.FANCY_DIAL_CLASS, "setup", "(LTextureStitched;)V"))
                    );
                }
            }
                .setInsertBefore(true)
                .matchConstructorOnly(true)
            );

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "render custom " + name;
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        reference(GETFIELD, currentAngle),
                        optional(build(push(2.0 * Math.PI), DDIV)),
                        push(1.0),
                        DADD
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // if (FancyDial.update(this)) {
                        ALOAD_0,
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.FANCY_DIAL_CLASS, "update", "(LTextureStitched;)Z")),
                        IFEQ, branch("A"),

                        // return;
                        RETURN,

                        // }
                        label("A")
                    );
                }
            }
                .setInsertBefore(true)
                .targetMethod(update)
            );
        }

        abstract protected MethodRef getUpdateMethod();
    }

    private class TextureCompassMod extends TextureDialMod {
        TextureCompassMod() {
            super("compass");

            addClassSignature(new ConstSignature(180.0));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        captureReference(GETFIELD),
                        push(2.0 * Math.PI),
                        DDIV,
                        push(1.0),
                        DADD
                    );
                }
            }
                .setMethod(update)
                .addXref(1, currentAngle)
            );
        }

        @Override
        protected MethodRef getUpdateMethod() {
            return new MethodRef(getDeobfClass(), "updateNeedle", "(LWorld;DDDZZ)V");
        }
    }

    private class TextureClockMod extends TextureDialMod {
        TextureClockMod() {
            super("clock");

            addClassSignature(new ConstSignature(0.8));
            addClassSignature(new ConstSignature(0.5));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        DUP,
                        capture(build(GETFIELD, capture(any(2)))),
                        ALOAD_0,
                        anyReference(GETFIELD),
                        DADD,
                        PUTFIELD, backReference(2)
                    );
                }
            }
                .setMethod(update)
                .addXref(1, currentAngle)
            );

            addPatch(new MakeMemberPublicPatch(currentAngle));
        }

        @Override
        protected MethodRef getUpdateMethod() {
            return new MethodRef(getDeobfClass(), "updateAnimation", "()V");
        }
    }
}
