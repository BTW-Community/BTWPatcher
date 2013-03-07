package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.*;
import javassist.bytecode.AccessFlag;

import static com.pclewis.mcpatcher.BinaryRegex.*;
import static com.pclewis.mcpatcher.BytecodeMatcher.*;
import static javassist.bytecode.Opcode.*;

public class ExtendedHD extends BaseTexturePackMod {
    public ExtendedHD() {
        clearPatches();

        name = MCPatcherUtils.EXTENDED_HD;
        author = "MCPatcher";
        description = "Provides extended support for custom animations, mipmapping, and other graphical features.";
        version = "2.1";

        configPanel = new HDConfig();

        addDependency(BaseTexturePackMod.NAME);

        if (getMinecraftVersion().compareTo("13w09b") < 0) {
            addError("Requires Minecraft 13w09b or newer");
            return;
        }

        addClassMod(new MinecraftMod());
        addClassMod(new RenderEngineMod());
        addClassMod(new ColorizerMod("ColorizerGrass"));
        addClassMod(new ColorizerMod("ColorizerFoliage"));
        addClassMod(new BaseMod.IconMod());
        addClassMod(new TextureMod());
        addClassMod(new TextureStitchedMod());
        addClassMod(new TextureCompassMod());
        addClassMod(new TextureClockMod());

        addClassFile(MCPatcherUtils.CUSTOM_ANIMATION_CLASS);
        addClassFile(MCPatcherUtils.CUSTOM_ANIMATION_CLASS + "$1");
        addClassFile(MCPatcherUtils.CUSTOM_ANIMATION_CLASS + "$1$1");
        addClassFile(MCPatcherUtils.MIPMAP_HELPER_CLASS);
        addClassFile(MCPatcherUtils.FANCY_DIAL_CLASS);
        addClassFile(MCPatcherUtils.FANCY_DIAL_CLASS + "$Layer");
    }

    @Override
    public String[] getLoggingCategories() {
        return new String[]{
            MCPatcherUtils.CUSTOM_ANIMATIONS,
            MCPatcherUtils.MIPMAP,
        };
    }

    private class MinecraftMod extends BaseMod.MinecraftMod {
        MinecraftMod() {
            final FieldRef renderEngine = new FieldRef(getDeobfClass(), "renderEngine", "LRenderEngine;");

            addColorizerSignature("Grass");
            addColorizerSignature("Foliage");

            addMemberMapper(new FieldMapper(renderEngine));
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
        private final MethodRef readTextureImage = new MethodRef(getDeobfClass(), "readTextureImage", "(Ljava/io/InputStream;)Ljava/awt/image/BufferedImage;");
        private final MethodRef setupTexture1 = new MethodRef(getDeobfClass(), "setupTexture1", "(Ljava/awt/image/BufferedImage;I)V");
        private final MethodRef setupTexture2 = new MethodRef(getDeobfClass(), "setupTexture2", "(Ljava/awt/image/BufferedImage;IZZ)V");
        private final MethodRef setupTextureMipmaps = new MethodRef(MCPatcherUtils.MIPMAP_HELPER_CLASS, "setupTexture", "(LRenderEngine;Ljava/awt/image/BufferedImage;IZZLjava/lang/String;)V");
        private final MethodRef getImageRGB = new MethodRef(getDeobfClass(), "getImageRGB", "(Ljava/awt/image/BufferedImage;[I)[I");
        private final MethodRef readTextureImageData = new MethodRef(getDeobfClass(), "readTextureImageData", "(Ljava/lang/String;)[I");
        private final MethodRef allocateAndSetupTexture = new MethodRef(getDeobfClass(), "allocateAndSetupTexture", "(Ljava/awt/image/BufferedImage;)I");
        private final InterfaceMethodRef getResourceAsStream = new InterfaceMethodRef("ITexturePack", "getResourceAsStream", "(Ljava/lang/String;)Ljava/io/InputStream;");

        RenderEngineMod() {
            addMemberMapper(new MethodMapper(readTextureImage));
            addMemberMapper(new MethodMapper(setupTexture1));
            addMemberMapper(new MethodMapper(setupTexture2));
            addMemberMapper(new MethodMapper(getImageRGB));
            addMemberMapper(new MethodMapper(readTextureImageData));
            addMemberMapper(new MethodMapper(allocateAndSetupTexture));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "readTextureImage(getResourceAsStream(...)) -> getImage(...)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        reference(INVOKEINTERFACE, getResourceAsStream),
                        reference(INVOKESPECIAL, readTextureImage)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.TEXTURE_PACK_API_CLASS, "getImage", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/String;)Ljava/awt/image/BufferedImage;"))
                    );
                }
            });

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "getResourceAsStream(...), readTextureImage -> getImage(...)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_2,
                        any(0, 6), // some mods have two useless CHECKCASTS here
                        ALOAD_1,
                        reference(INVOKEINTERFACE, getResourceAsStream),
                        anyASTORE,
                        anyALOAD,
                        IFNONNULL, any(2),
                        ALOAD_0,
                        ALOAD_0,
                        GETFIELD, any(2),
                        anyILOAD,
                        or(
                            build(reference(INVOKEVIRTUAL, setupTexture1)),
                            build(any(0, 6), reference(INVOKEVIRTUAL, setupTexture2))
                        ),
                        GOTO, any(2),
                        ALOAD_0,
                        ALOAD_0,
                        anyALOAD,
                        reference(INVOKESPECIAL, readTextureImage)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        ALOAD_0,
                        ALOAD_1,
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.TEXTURE_PACK_API_CLASS, "getImage", "(Ljava/lang/String;)Ljava/awt/image/BufferedImage;"))
                    );
                }
            });

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "null check in setupTexture";
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
                        ALOAD_1,
                        IFNONNULL, branch("A"),
                        RETURN,
                        label("A")
                    );
                }
            }.targetMethod(setupTexture2));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "null check in getImageRGB";
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
                        ALOAD_1,
                        IFNONNULL, branch("A"),
                        ALOAD_2,
                        ARETURN,
                        label("A")
                    );
                }
            }.targetMethod(getImageRGB));

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
                            push(6408),
                            anyILOAD,
                            anyILOAD,
                            push(0),
                            or(build(push(6408)), build(push(32993 /* GL11.GL_BGRA */))),
                            or(build(push(5121)), build(push(33639 /* GL_UNSIGNED_INT_8_8_8_8_REV */))),
                            ALOAD_0,
                            reference(GETFIELD, imageData),
                            reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.GL11_CLASS, "glTexImage2D", "(IIIIIIII" + imageData.getType() + ")V"))
                        ), true)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // GL11.glTexImage2D(..., MipmapHelper.currentLevel, ...);
                        reference(GETSTATIC, new FieldRef(MCPatcherUtils.MIPMAP_HELPER_CLASS, "currentLevel", "I"))
                    );
                }
            }.targetMethod(setupTexture2));

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
                            reference(INVOKEVIRTUAL, new MethodRef("java/awt/image/BufferedImage", "getWidth", "()I"))
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
            }.targetMethod(setupTexture2));

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
                                reference(INVOKEVIRTUAL, new MethodRef("java/lang/String", "startsWith", "(Ljava/lang/String;)Z"))
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
                        reference(INVOKEVIRTUAL, setupTexture2)
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

    private class TextureMod extends BaseMod.TextureMod {
        TextureMod() {
            final FieldRef glMinFilter = mapIntField(7, "glMinFilter");
            final FieldRef glMagFilter = mapIntField(8, "glMagFilter");
            final FieldRef useMipmaps = new FieldRef(getDeobfClass(), "useMipmaps", "Z");
            final MethodRef copySubTexture = new MethodRef(getDeobfClass(), "copySubTexture", "(IILTexture;Z)V");
            final MethodRef glTexImage2D = new MethodRef(MCPatcherUtils.GL11_CLASS, "glTexImage2D", "(IIIIIIIILjava/nio/ByteBuffer;)V");
            final FieldRef byteBuffer = new FieldRef(getDeobfClass(), "byteBuffer", "Ljava/nio/ByteBuffer;");
            final MethodRef allocateDirect = new MethodRef("java/nio/ByteBuffer", "allocateDirect", "(I)Ljava/nio/ByteBuffer;");
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
                .addXref(1, useMipmaps)
            );

            addMemberMapper(new MethodMapper(copySubTexture));
            addMemberMapper(new FieldMapper(byteBuffer));

            addPatch(new MakeMemberPublicPatch(glMinFilter) {
                @Override
                public int getNewFlags(int oldFlags) {
                    return super.getNewFlags(oldFlags) & ~AccessFlag.FINAL;
                }
            });

            addPatch(new MakeMemberPublicPatch(glMagFilter) {
                @Override
                public int getNewFlags(int oldFlags) {
                    return super.getNewFlags(oldFlags) & ~AccessFlag.FINAL;
                }
            });

            addPatch(new MakeMemberPublicPatch(useMipmaps) {
                @Override
                public int getNewFlags(int oldFlags) {
                    return super.getNewFlags(oldFlags) & ~AccessFlag.FINAL;
                }
            });

            addPatch(new MakeMemberPublicPatch(byteBuffer));

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
                    return "replace copySubTexture";
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
                        reference(GETFIELD, loaded),
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
            }.targetMethod(copySubTexture));
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

    private class TextureStitchedMod extends ClassMod {
        TextureStitchedMod() {
            setInterfaces("Icon");

            final MethodRef update = new MethodRef(getDeobfClass(), "update", "()V");

            addClassSignature(new ConstSignature("clock"));
            addClassSignature(new ConstSignature("compass"));
            addClassSignature(new ConstSignature(","));

            addMemberMapper(new MethodMapper(update));
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

            addClassSignature(new ConstSignature("compass"));
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

            addClassSignature(new OrSignature(
                new ConstSignature("compass"), // [sic]
                new ConstSignature("clock") // in case mojang fixes it
            ));
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

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "fix icon name";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push("compass")
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        push("clock")
                    );
                }
            }.matchConstructorOnly(true));
        }

        @Override
        protected MethodRef getUpdateMethod() {
            return new MethodRef(getDeobfClass(), "update", "()V");
        }
    }
}
