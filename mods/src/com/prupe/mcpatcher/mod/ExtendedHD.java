package com.prupe.mcpatcher.mod;

import com.prupe.mcpatcher.*;
import com.prupe.mcpatcher.basemod.*;
import javassist.bytecode.AccessFlag;

import static com.prupe.mcpatcher.BinaryRegex.*;
import static com.prupe.mcpatcher.BytecodeMatcher.*;
import static javassist.bytecode.Opcode.*;

public class ExtendedHD extends Mod {
    private static final MethodRef copySubTexture1 = new MethodRef("TextureUtil", "copySubTexture1", "([IIIIIZZ)V");
    private static final MethodRef copySubTexture2 = new MethodRef("TextureUtil", "copySubTexture2", "(Ljava/awt/image/BufferedImage;IIZZ)V");
    private static final MethodRef setupTexture1 = new MethodRef("TextureUtil", "setupTexture1", "(ILjava/awt/image/BufferedImage;ZZ)I");
    private static final MethodRef setupTexture2 = new MethodRef("TextureUtil", "setupTexture2", "(III)V");

    private static final MethodRef setupTextureMipmaps1 = new MethodRef(MCPatcherUtils.MIPMAP_HELPER_CLASS, "setupTexture", "(ILjava/awt/image/BufferedImage;ZZLResourceLocation;)I");
    private static final MethodRef setupTextureMipmaps2 = new MethodRef(MCPatcherUtils.MIPMAP_HELPER_CLASS, "setupTexture", "(Ljava/lang/Object;Ljava/awt/image/BufferedImage;IZZLResourceLocation;)V");
    private static final MethodRef setupTextureMipmaps3 = new MethodRef(MCPatcherUtils.MIPMAP_HELPER_CLASS, "setupTexture", "(IIILjava/lang/String;)V");
    private static final MethodRef copySubTextureMipmaps1 = new MethodRef(MCPatcherUtils.MIPMAP_HELPER_CLASS, "copySubTexture", "([IIIIILjava/lang/String;)V");
    private static final MethodRef copySubTextureMipmaps2 = new MethodRef(MCPatcherUtils.MIPMAP_HELPER_CLASS, "copySubTexture", "(LTextureAtlasSprite;I)V");
    private static final MethodRef updateCustomAnimations = new MethodRef(MCPatcherUtils.CUSTOM_ANIMATION_CLASS, "updateAll", "()V");

    private static final MethodRef imageRead = new MethodRef("javax/imageio/ImageIO", "read", "(Ljava/io/InputStream;)Ljava/awt/image/BufferedImage;");

    private static final FieldRef textureBorder = new FieldRef("Texture", "border", "I");

    private final boolean haveMipmapping;

    public ExtendedHD() {
        name = MCPatcherUtils.EXTENDED_HD;
        author = "MCPatcher";
        description = "Provides support for custom animations, HD fonts, mipmapping, and other graphical features.";
        version = "3.2";

        haveMipmapping = getMinecraftVersion().compareTo("13w38a") >= 0;
        configPanel = new HDConfig(!haveMipmapping);

        addDependency(MCPatcherUtils.BASE_TEXTURE_PACK_MOD);

        if (getMinecraftVersion().compareTo("13w02a") < 0) {
            addError("Requires Minecraft 1.5 or newer");
            return;
        }

        if (ResourceLocationMod.setup(this)) {
            addClassMod(new ResourceMod(this));
            addClassMod(new TextureObjectMod(this));
            addClassMod(new AbstractTextureMod(this));
            addClassMod(new TextureMod(this));
            addClassMod(new TextureManagerMod());
            addClassMod(new TextureAtlasMod());
            if (haveMipmapping) {
                addClassMod(new com.prupe.mcpatcher.basemod.TextureAtlasSpriteMod(this));
            } else {
                addClassMod(new MinecraftMod());
                addClassMod(new TextureUtilMod());
                addClassMod(new TextureAtlasSpriteMod());
                addClassMod(new SimpleTextureMod());
            }
            addClassMod(new SimpleResourceMod());
        } else {
            addClassMod(new MinecraftMod());
            addClassMod(new RenderEngineMod());
            addClassMod(new TextureMod15());
            addClassMod(new TextureAtlasSpriteMod15());
        }
        addClassMod(new IconMod(this));
        addClassMod(new TextureCompassMod());
        addClassMod(new TextureClockMod());
        HDFont.setupMod(this, getMinecraftVersion(), false);

        addClassFiles("com.prupe.mcpatcher.hd.*");
        if (haveMipmapping) {
            removeAddedClassFile(MCPatcherUtils.AA_HELPER_CLASS);
            removeAddedClassFile(MCPatcherUtils.BORDERED_TEXTURE_CLASS);
        } else {
            getClassMap().addInheritance("TextureAtlasSprite", MCPatcherUtils.BORDERED_TEXTURE_CLASS);
        }
    }

    @Override
    public String[] getLoggingCategories() {
        return new String[]{
            MCPatcherUtils.CUSTOM_ANIMATIONS,
            MCPatcherUtils.MIPMAP,
            MCPatcherUtils.HD_FONT,
        };
    }

    private class MinecraftMod extends com.prupe.mcpatcher.basemod.MinecraftMod {
        MinecraftMod() {
            super(ExtendedHD.this);

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

    private class TextureUtilMod extends com.prupe.mcpatcher.basemod.TextureUtilMod {
        TextureUtilMod() {
            super(ExtendedHD.this);

            addMemberMapper(new MethodMapper(copySubTexture1));
            addMemberMapper(new MethodMapper(copySubTexture2));
            addMemberMapper(new MethodMapper(setupTexture1));
            addMemberMapper(new MethodMapper(setupTexture2));
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
                        reference(INVOKESTATIC, updateCustomAnimations)
                    );
                }
            }
                .setInsertBefore(true)
                .targetMethod(updateAnimations)
            );
        }
    }

    private class TextureAtlasMod extends com.prupe.mcpatcher.basemod.TextureAtlasMod {
        TextureAtlasMod() {
            super(ExtendedHD.this);

            final ClassRef textureStitched = new ClassRef("TextureAtlasSprite");
            final FieldRef animations = new FieldRef(getDeobfClass(), "animations", "Ljava/util/List;");
            final MethodRef textureStitchedConstructor = new MethodRef("TextureAtlasSprite", "<init>", "(Ljava/lang/String;)V");
            final MethodRef createTextureStitched = new MethodRef(MCPatcherUtils.BORDERED_TEXTURE_CLASS, "create", "(Ljava/lang/String;Ljava/lang/String;)LTextureAtlasSprite;");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // TextureUtil.setupTexture(t.getFrameRGB(0), t.getWidth(), t.getHeight(), t.getX0(), t.getY0(), false, false);
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
                .addXref(2, new MethodRef("TextureAtlasSprite", "getFrameRGB", "(I)" + (haveMipmapping ? "[" : "") + "[I"))
                .addXref(3, new MethodRef("TextureAtlasSprite", "getX0", "()I"))
                .addXref(4, new MethodRef("TextureAtlasSprite", "getY0", "()I"))
            );

            addMemberMapper(new FieldMapper(animations));

            addPatch(new MakeMemberPublicPatch(animations));

            if (haveMipmapping) {
                return;
            }

            addPatch(new TextureMipmapPatch(this, basePath));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "enable mipmapping for tilesheets";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        reference(INVOKESTATIC, setupTexture2)
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

    private class TextureAtlasSpriteMod extends com.prupe.mcpatcher.basemod.TextureAtlasSpriteMod {
        TextureAtlasSpriteMod() {
            super(ExtendedHD.this);

            final FieldRef mipmaps = new FieldRef(getDeobfClass(), "mipmaps", "Ljava/util/List;");
            final FieldRef animationFrames = new FieldRef(getDeobfClass(), "animationFrames", "Ljava/util/List;");
            final MethodRef constructor = new MethodRef(getDeobfClass(), "<init>", "(Ljava/lang/String;)V");
            final MethodRef init = new MethodRef(getDeobfClass(), "init", "(IIIIZ)V");
            final MethodRef copy = new MethodRef(getDeobfClass(), "copy", "(LTextureAtlasSprite;)V");
            final MethodRef updateAnimation = new MethodRef(getDeobfClass(), "updateAnimation", "()V");
            final MethodRef loadResource = new MethodRef(getDeobfClass(), "loadResource", "(LResource;)V");
            final MethodRef addBorder = new MethodRef(MCPatcherUtils.AA_HELPER_CLASS, "addBorder", "(LTextureAtlasSprite;LResource;Ljava/awt/image/BufferedImage;)Ljava/awt/image/BufferedImage;");
            final InterfaceMethodRef listGet = new InterfaceMethodRef("java/util/List", "get", "(I)Ljava/lang/Object;");
            final InterfaceMethodRef listClear = new InterfaceMethodRef("java/util/List", "clear", "()V");
            final ClassRef intArray = new ClassRef("[I");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // (... + 1) % ...
                        push(1),
                        IADD,
                        any(1, 6),
                        IREM
                    );
                }
            }.setMethod(updateAnimation));

            addMemberMapper(new MethodMapper(init));
            addMemberMapper(new MethodMapper(copy));
            addMemberMapper(new MethodMapper(loadResource));
            addMemberMapper(new FieldMapper(animationFrames));

            addPatch(new MakeMemberPublicPatch(constructor)); // constructor was made protected in 13w25c
            addPatch(new MakeMemberPublicPatch(animationFrames));
            addPatch(new AddFieldPatch(mipmaps));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "add tile border for aa";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // image = ImageIO.read(inputStream);
                        anyALOAD,
                        reference(INVOKESTATIC, imageRead)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // AAHelper.addBorder(stitched, resource, ...)
                        ALOAD_0,
                        ALOAD_1,
                        getMatch(),
                        reference(INVOKESTATIC, addBorder)
                    );
                }
            }.targetMethod(loadResource));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "update mipmaps (tile animations)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        reference(GETFIELD, animationFrames),
                        capture(anyILOAD),
                        reference(INVOKEINTERFACE, listGet),
                        repeat(build(reference(CHECKCAST, intArray)), 1, 2), // mcp adds a second (int[]) cast
                        ALOAD_0,
                        anyReference(GETFIELD),
                        ALOAD_0,
                        anyReference(GETFIELD),
                        ALOAD_0,
                        anyReference(GETFIELD),
                        ALOAD_0,
                        anyReference(GETFIELD),
                        push(0),
                        push(0),
                        reference(INVOKESTATIC, copySubTexture1)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        ALOAD_0,
                        getCaptureGroup(1),
                        reference(INVOKESTATIC, copySubTextureMipmaps2)
                    );
                }
            }.targetMethod(updateAnimation));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "update mipmaps with tile animation data";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(or(
                        build(
                            // this.animationFrames.clear();
                            ALOAD_0,
                            reference(GETFIELD, animationFrames),
                            reference(INVOKEINTERFACE, listClear)
                        ),
                        build(
                            // this.animationFrames = ...;
                            reference(PUTFIELD, animationFrames)
                        )
                    ));
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // this.mipmaps = null;
                        ALOAD_0,
                        ACONST_NULL,
                        reference(PUTFIELD, mipmaps)
                    );
                }
            }.setInsertAfter(true));
        }
    }

    private static class TextureMipmapPatch extends BytecodePatch {
        private final FieldRef textureNameField;

        TextureMipmapPatch(com.prupe.mcpatcher.ClassMod classMod, FieldRef textureNameField) {
            super(classMod);
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
                reference(INVOKESTATIC, copySubTextureMipmaps1)
            );
        }
    }

    private class SimpleTextureMod extends com.prupe.mcpatcher.basemod.SimpleTextureMod {
        SimpleTextureMod() {
            super(ExtendedHD.this);

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
                        reference(INVOKESTATIC, setupTextureMipmaps1)
                    );
                }
            });
        }
    }

    abstract private class TextureDialMod extends ClassMod {
        protected final FieldRef currentAngle = new FieldRef(getDeobfClass(), "currentAngle", "D");
        protected final MethodRef update = getUpdateMethod();

        TextureDialMod(final String name) {
            setParentClass("TextureAtlasSprite");

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
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.FANCY_DIAL_CLASS, "setup", "(LTextureAtlasSprite;)V"))
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
                        // if (FancyDial.update(this, renderItemFlag)) {
                        ALOAD_0,
                        getRenderItemFrameFlag(),
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.FANCY_DIAL_CLASS, "update", "(LTextureAtlasSprite;Z)Z")),
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

        protected byte[] getRenderItemFrameFlag() {
            return buildCode(
                push(0)
            );
        }
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

        @Override
        protected byte[] getRenderItemFrameFlag() {
            return buildCode(
                ILOAD, 9
            );
        }
    }

    private class TextureClockMod extends TextureDialMod {
        TextureClockMod() {
            super("clock");

            addClassSignature(new ConstSignature(0.8));
            addClassSignature(new ConstSignature(0.5));
            addClassSignature(new ConstSignature(new MethodRef("java/lang/Math", "random", "()D")));

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

    private class SimpleResourceMod extends ClassMod {
        SimpleResourceMod() {
            setInterfaces("Resource");

            addClassSignature(new ConstSignature(new ClassRef("java/io/BufferedReader")));
            addClassSignature(new ConstSignature(new ClassRef("java/io/InputStreamReader")));
            addClassSignature(new ConstSignature(new MethodRef("com/google/gson/JsonElement", "getAsJsonObject", "()Lcom/google/gson/JsonObject;")));
            addClassSignature(new ConstSignature("pack.mcmeta").negate(true));
        }
    }

    private class RenderEngineMod extends com.prupe.mcpatcher.basemod.RenderEngineMod {
        RenderEngineMod() {
            super(ExtendedHD.this);

            setupCustomAnimations();
            setupMipmaps();
        }

        private void setupCustomAnimations() {
            final MethodRef updateDynamicTextures = new MethodRef(getDeobfClass(), "updateDynamicTextures", "()V");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        begin(),
                        ALOAD_0,
                        anyReference(GETFIELD),
                        captureReference(INVOKEVIRTUAL),
                        ALOAD_0,
                        anyReference(GETFIELD),
                        backReference(1),
                        RETURN
                    );
                }
            }.setMethod(updateDynamicTextures));

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
                        // CustomAnimations.updateAll();
                        reference(INVOKESTATIC, updateCustomAnimations)
                    );
                }
            }
                .setInsertBefore(true)
                .targetMethod(updateDynamicTextures)
            );
        }

        private void setupMipmaps() {
            final MethodRef setupTextureExt = new MethodRef(getDeobfClass(), "setupTextureExt", "(Ljava/awt/image/BufferedImage;IZZ)V");
            final MethodRef startsWith = new MethodRef("java/lang/String", "startsWith", "(Ljava/lang/String;)Z");

            addMemberMapper(new MethodMapper(setupTextureExt));

            addPatch(new BytecodePatch() {
                private int nameRegister;

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
                            nameRegister = extractRegisterNum(getCaptureGroup(1));
                            return true;
                        }
                    });
                }

                @Override
                public String getDescription() {
                    return "generate mipmaps";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // this.setupTextureExt(...);
                        reference(INVOKEVIRTUAL, setupTextureExt)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // MipmapHelper.setupTexture(..., this.currentTextureName);
                        registerLoadStore(ALOAD, nameRegister),
                        ResourceLocationMod.wrap(this),
                        reference(INVOKESTATIC, setupTextureMipmaps2)
                    );
                }
            });
        }
    }

    private class TextureMod15 extends ClassMod {
        TextureMod15() {
            final FieldRef textureTarget = new FieldRef(getDeobfClass(), "textureTarget", "I");
            final FieldRef glTextureId = new FieldRef(getDeobfClass(), "glTextureId", "I");
            final FieldRef textureCreated = new FieldRef(getDeobfClass(), "textureCreated", "Z");
            final MethodRef getTextureId = new MethodRef(getDeobfClass(), "getTextureId", "()I");
            final MethodRef getGlTextureId = new MethodRef(getDeobfClass(), "getGlTextureId", "()I");
            final MethodRef getWidth = new MethodRef(getDeobfClass(), "getWidth", "()I");
            final MethodRef getHeight = new MethodRef(getDeobfClass(), "getHeight", "()I");
            final MethodRef getTextureData = new MethodRef(getDeobfClass(), "getTextureData", "()Ljava/nio/ByteBuffer;");
            final MethodRef getTextureName = new MethodRef(getDeobfClass(), "getTextureName", "()Ljava/lang/String;");
            final MethodRef createTexture = new MethodRef(getDeobfClass(), "createTexture", "()V");
            final MethodRef transferFromImage = new MethodRef(getDeobfClass(), "transferFromImage", "(Ljava/awt/image/BufferedImage;)V");
            final MethodRef glBindTexture = new MethodRef(MCPatcherUtils.GL11_CLASS, "glBindTexture", "(II)V");
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

            addClassSignature(new ConstSignature("png"));

            addClassSignature(new BytecodeSignature() {
                    @Override
                    public String getMatchExpression() {
                        return buildExpression(
                            ALOAD_0,
                            captureReference(GETFIELD),
                            ALOAD_0,
                            captureReference(GETFIELD),
                            reference(INVOKESTATIC, glBindTexture)
                        );
                    }
                }
                    .matchConstructorOnly(true)
                    .addXref(1, textureTarget)
                    .addXref(2, glTextureId)
            );

            addClassSignature(new BytecodeSignature() {
                    @Override
                    public String getMatchExpression() {
                        return buildExpression(
                            ALOAD_0,
                            push(1),
                            captureReference(PUTFIELD),
                            RETURN,
                            end()
                        );
                    }
                }
                    .setMethod(createTexture)
                    .addXref(1, textureCreated)
            );

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

            addMemberMapper(new MethodMapper(getTextureId, getGlTextureId, getWidth, getHeight));
            addMemberMapper(new MethodMapper(getTextureData));
            addMemberMapper(new MethodMapper(getTextureName));
            addMemberMapper(new MethodMapper(transferFromImage));
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

            patchBTW();
        }

        private void patchBTW() {
            final MethodRef uploadByteBufferToGPU = new MethodRef(getDeobfClass(), "UploadByteBufferToGPU", "(IILjava/nio/ByteBuffer;II)V");
            final MethodRef copySubTexture = new MethodRef(MCPatcherUtils.MIPMAP_HELPER_CLASS, "copySubTexture", "(LTexture;Ljava/nio/ByteBuffer;IIII)V");

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "replace UploadByteBufferToGPU (btw)";
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
                        ALOAD_3,
                        ILOAD_1,
                        ILOAD_2,
                        ILOAD, 4,
                        ILOAD, 5,
                        reference(INVOKESTATIC, copySubTexture),
                        RETURN
                    );
                }
            }.targetMethod(uploadByteBufferToGPU));
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
    
    private class TextureAtlasSpriteMod15 extends ClassMod {
        TextureAtlasSpriteMod15() {
            setInterfaces("Icon");

            final FieldRef texture = new FieldRef(getDeobfClass(), "texture", "LTexture;");
            final MethodRef constructor = new MethodRef(getDeobfClass(), "<init>", "(Ljava/lang/String;)V");
            final MethodRef init = new MethodRef(getDeobfClass(), "init", "(LTexture;Ljava/util/List;IIIIZ)V");
            final MethodRef updateAnimation = new MethodRef(getDeobfClass(), "updateAnimation", "()V");
            final MethodRef createSprite = new MethodRef(MCPatcherUtils.BORDERED_TEXTURE_CLASS, "create", "(Ljava/lang/String;)L" + getDeobfClass() + ";");

            addClassSignature(new ConstSignature("clock"));
            addClassSignature(new ConstSignature("compass"));
            addClassSignature(new ConstSignature(","));

            addMemberMapper(new FieldMapper(texture));
            addMemberMapper(new MethodMapper(init));
            addMemberMapper(new MethodMapper(updateAnimation));

            addPatch(new MakeMemberPublicPatch(constructor));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override " + getDeobfClass();
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // new TextureAtlasSprite(name)
                        reference(NEW, new ClassRef(getDeobfClass())),
                        DUP,
                        ALOAD_0,
                        reference(INVOKESPECIAL, new MethodRef(getDeobfClass(), "<init>", "(Ljava/lang/String;)V"))
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // BorderedTexture.create(name)
                        ALOAD_0,
                        reference(INVOKESTATIC, createSprite)
                    );
                }
            });
        }
    }
}
