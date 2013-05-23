package com.prupe.mcpatcher.mod;

import com.prupe.mcpatcher.*;

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
        version = "3.0";

        configPanel = new HDConfig();

        addDependency(MCPatcherUtils.BASE_TEXTURE_PACK_MOD);

        if (getMinecraftVersion().compareTo("13w18a") < 0) {
            addError("Requires Minecraft 13w18a or newer");
            return;
        }

        addClassMod(new MinecraftMod());
        addClassMod(new BaseMod.IconMod());
        addClassMod(new BaseMod.ITextureMod());
        addClassMod(new BaseMod.TextureBaseMod());
        addClassMod(new BaseMod.TextureMod());
        addClassMod(new TextureUtilsMod());
        addClassMod(new TextureManagerMod());
        addClassMod(new TextureMapMod());
        addClassMod(new TextureStitchedMod());
        addClassMod(new TextureNamedMod());
        addClassMod(new TextureCompassMod());
        addClassMod(new TextureClockMod());
        HDFont.setupMod(this);

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
    }

    private class TextureUtilsMod extends BaseMod.TextureUtilsMod {
        TextureUtilsMod() {
            addMemberMapper(new MethodMapper(copySubTexture1));
            addMemberMapper(new MethodMapper(copySubTexture2));
            addMemberMapper(new MethodMapper(setupTexture1));
            addMemberMapper(new MethodMapper(setupTexture3));
        }
    }

    private class TextureManagerMod extends ClassMod {
        TextureManagerMod() {
            final MethodRef updateAnimations = new MethodRef(getDeobfClass(), "updateAnimations", "()V");
            final FieldRef animations = new FieldRef(getDeobfClass(), "animations", "Ljava/util/List;");
            final InterfaceMethodRef listIterator = new InterfaceMethodRef("java/util/List", "iterator", "()Ljava/util/Iterator;");

            addClassSignature(new ConstSignature("dynamic/%s_%d"));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        captureReference(GETFIELD),
                        reference(INVOKEINTERFACE, listIterator),
                        ASTORE_1

                    );
                }
            }
                .setMethod(updateAnimations)
                .addXref(1, animations)
            );

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
                .targetMethod(updateAnimations)
            );
        }
    }

    private class TextureMapMod extends BaseMod.TextureMapMod {
        TextureMapMod() {
            final MethodRef readTile = new MethodRef(getDeobfClass(), "readTile", "(LTextureStitched;LIResourceBundle;Ljava/lang/String;)Z");
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
            final MethodRef updateAnimation = new MethodRef(getDeobfClass(), "updateAnimation", "()V");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // (... + 1) % ...
                        push(1),
                        IADD,
                        ALOAD_0,
                        anyReference(GETFIELD),
                        anyReference(INVOKEVIRTUAL),
                        IREM
                    );
                }
            }.setMethod(updateAnimation));

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

    private class TextureNamedMod extends BaseMod.TextureNamedMod {
        TextureNamedMod() {
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
