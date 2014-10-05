package com.prupe.mcpatcher.mod;

import com.prupe.mcpatcher.*;
import com.prupe.mcpatcher.basemod.ResourceLocationMod;
import javassist.bytecode.AccessFlag;

import static com.prupe.mcpatcher.BinaryRegex.*;
import static com.prupe.mcpatcher.BytecodeMatcher.*;
import static javassist.bytecode.Opcode.*;

class ExtendedHD15 {
    static final String WRAPPER_15_CLASS = "com.prupe.mcpatcher.hd.Wrapper15";

    private static final MethodRef setupTextureMipmaps1 = new MethodRef(WRAPPER_15_CLASS, "setupTexture", "(LRenderEngine;Ljava/awt/image/BufferedImage;IZZLResourceLocation;)V");
    private static final MethodRef setupTextureMipmaps2 = new MethodRef(WRAPPER_15_CLASS, "setupTexture", "(LTexture;Ljava/lang/String;)V");
    private static final MethodRef copySubTexture1 = new MethodRef(WRAPPER_15_CLASS, "copySubTexture", "(LTexture;LTexture;IIZ)V");
    private static final MethodRef copySubTexture2 = new MethodRef(WRAPPER_15_CLASS, "copySubTexture", "(LTexture;Ljava/nio/ByteBuffer;IIII)V");

    private static final FieldRef textureBorder = new FieldRef("Texture", "border", "I");

    static void setup(Mod mod) {
        mod.addClassMod(new RenderEngineMod(mod));
        mod.addClassMod(new TextureMod(mod));
        mod.addClassMod(new TextureManagerMod(mod));
        mod.addClassMod(new TextureAtlasMod(mod));
        mod.addClassMod(new TextureAtlasSpriteMod(mod));
        mod.addClassMod(new TileEntityBeaconRendererMod(mod));
    }

    private static class RenderEngineMod extends com.prupe.mcpatcher.basemod.RenderEngineMod {
        RenderEngineMod(Mod mod) {
            super(mod);

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
                            reference(INVOKESTATIC, ExtendedHD.updateCustomAnimations)
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
                        // Wrapper15.setupTexture(..., this.currentTextureName);
                        registerLoadStore(ALOAD, nameRegister),
                        ResourceLocationMod.wrap(this),
                        reference(INVOKESTATIC, setupTextureMipmaps1)
                    );
                }
            });
        }
    }

    private static class TextureMod extends ClassMod {
        TextureMod(Mod mod) {
            super(mod);

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
            final FieldRef mipmapData = new FieldRef(getDeobfClass(), "mipmapData", "[Ljava/nio/IntBuffer;");
            final MethodRef glTexImage2D = new MethodRef(MCPatcherUtils.GL11_CLASS, "glTexImage2D", "(IIIIIIIILjava/nio/ByteBuffer;)V");
            final MethodRef glTexSubImage2D = new MethodRef(MCPatcherUtils.GL11_CLASS, "glTexSubImage2D", "(IIIIIIIILjava/nio/ByteBuffer;)V");

            addClassSignature(new ConstSignature("png"));

            addClassSignature(new BytecodeSignature() {
                {
                    matchConstructorOnly(true);
                    addXref(1, textureTarget);
                    addXref(2, glTextureId);
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // GL11.glBindTexture(this,textureTarget, this.glTextureId);
                        ALOAD_0,
                        captureReference(GETFIELD),
                        ALOAD_0,
                        captureReference(GETFIELD),
                        reference(INVOKESTATIC, glBindTexture)
                    );
                }
            });

            addClassSignature(new BytecodeSignature() {
                {
                    setMethod(createTexture);
                    addXref(1, textureCreated);
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // this.textureCreated = true;
                        ALOAD_0,
                        push(1),
                        captureReference(PUTFIELD),
                        RETURN,
                        end()
                    );
                }
            });

            addClassSignature(new BytecodeSignature() {
                {
                    matchConstructorOnly(true);
                    addXref(1, mipmapActive);
                }

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
            });

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
            addPatch(new AddFieldPatch(mipmapData));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "generate mipmaps";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // GL11.glTexImage2D(...);
                        ALOAD_0,
                        reference(GETFIELD, textureTarget),
                        nonGreedy(any(0, 40)),
                        reference(INVOKESTATIC, glTexImage2D)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // Wrapper15.setupTexture(this, this.getTextureName());
                        ALOAD_0,
                        ALOAD_0,
                        reference(INVOKEVIRTUAL, getTextureName),
                        reference(INVOKESTATIC, setupTextureMipmaps2)
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
                        // if (this.textureCreated) {
                        ALOAD_0,
                        reference(GETFIELD, textureCreated),
                        IFEQ, branch("A"),

                        // Wrapper15.copySubTexture(this, src, x, y, flipped);
                        ALOAD_0,
                        ALOAD_3,
                        ILOAD_1,
                        ILOAD_2,
                        ILOAD, 4,
                        reference(INVOKESTATIC, copySubTexture1),
                        RETURN,

                        // }
                        label("A")
                    );
                }
            }.targetMethod(copyFrom));

            // 1.5.2 (.3, .4, ...) or 13w17a+
            if ((Mod.getMinecraftVersion().compareTo("1.5.2") >= 0 && Mod.getMinecraftVersion().compareTo("13w16a") < 0) ||
                Mod.getMinecraftVersion().compareTo("13w17a") >= 0) {
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

                            // Wrapper15.copySubTexture(this, src, x, y, false);
                            ALOAD_0,
                            ALOAD_3,
                            ILOAD_1,
                            ILOAD_2,
                            push(0),
                            reference(INVOKESTATIC, copySubTexture1),
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
                        reference(INVOKESTATIC, copySubTexture2),
                        RETURN
                    );
                }
            }.targetMethod(uploadByteBufferToGPU));
        }

        private FieldRef mapIntField(final int register, String name) {
            final FieldRef field = new FieldRef(getDeobfClass(), name, "I");

            addClassSignature(new BytecodeSignature() {
                {
                    matchConstructorOnly(true);
                    addXref(1, field);
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        registerLoadStore(ILOAD, register),
                        captureReference(PUTFIELD)
                    );
                }
            });

            return field;
        }
    }

    private static class TextureManagerMod extends ClassMod {
        TextureManagerMod(Mod mod) {
            super(mod);

            final MethodRef createTextureFromImage = new MethodRef(getDeobfClass(), "createTextureFromImage", "(Ljava/lang/String;IIIIIIIZLjava/awt/image/BufferedImage;)LTexture;");
            final MethodRef addAABorder = new MethodRef(WRAPPER_15_CLASS, "addAABorder", "(Ljava/lang/String;Ljava/awt/image/BufferedImage;)Ljava/awt/image/BufferedImage;");
            final FieldRef lastBorder = new FieldRef(MCPatcherUtils.AA_HELPER_CLASS, "lastBorder", "I");
            final MethodRef getImageWidth = new MethodRef("java/awt/image/BufferedImage", "getWidth", "()I");
            final MethodRef getImageHeight = new MethodRef("java/awt/image/BufferedImage", "getHeight", "()I");
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
                        // image = Wrapper15.addAABorder(name, image);
                        ALOAD_1,
                        ALOAD, 10,
                        reference(INVOKESTATIC, addAABorder),
                        ASTORE, 10,

                        // if (image != null) {
                        ALOAD, 10,
                        IFNULL, branch("A"),

                        // width = image.getWidth();
                        ALOAD, 10,
                        reference(INVOKEVIRTUAL, getImageWidth),
                        ISTORE_3,

                        // height = image.getHeight();
                        ALOAD, 10,
                        reference(INVOKEVIRTUAL, getImageHeight),
                        ISTORE, 4,

                        // }
                        label("A"),

                        // ...
                        getMatch(),

                        // texture.border = AAHelper.border;
                        ALOAD, getCaptureGroup(1),
                        reference(GETSTATIC, lastBorder),
                        reference(PUTFIELD, textureBorder)
                    );
                }
            }.targetMethod(createTextureFromImage));
        }
    }

    private static class TextureAtlasMod extends com.prupe.mcpatcher.basemod.TextureAtlasMod {
        public TextureAtlasMod(Mod mod) {
            super(mod);

            addPatch(new MakeMemberPublicPatch(TextureAtlasMod.basePath));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "set current tilesheet";
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
                        reference(PUTSTATIC, new FieldRef(ExtendedHD15.WRAPPER_15_CLASS, "currentAtlas", "L" + getDeobfClass() + ";"))
                    );
                }
            }.targetMethod(TextureAtlasMod.refreshTextures1));
        }
    }

    private static class TextureAtlasSpriteMod extends ClassMod {
        TextureAtlasSpriteMod(Mod mod) {
            super(mod);

            setInterfaces("Icon");

            final FieldRef texture = new FieldRef(getDeobfClass(), "texture", "LTexture;");
            final MethodRef constructor = new MethodRef(getDeobfClass(), "<init>", "(Ljava/lang/String;)V");
            final MethodRef init = new MethodRef(getDeobfClass(), "init", "(LTexture;Ljava/util/List;IIIIZ)V");
            final MethodRef updateAnimation = new MethodRef(getDeobfClass(), "updateAnimation", "()V");
            final MethodRef createSprite = new MethodRef(WRAPPER_15_CLASS, "createSprite", "(Ljava/lang/String;)L" + getDeobfClass() + ";");

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
                        reference(INVOKESPECIAL, constructor)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // Wrapper15.createSprite(name)
                        ALOAD_0,
                        reference(INVOKESTATIC, createSprite)
                    );
                }
            });
        }
    }

    private static class TileEntityBeaconRendererMod extends ClassMod {
        private static final long MODULUS = 0x7fff77L;

        TileEntityBeaconRendererMod(Mod mod) {
            super(mod);

            addClassSignature(new ConstSignature("/misc/beam.png"));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "fix beacon beam rendering when time > 49.7 days";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // var11 = (float) tileEntityBeacon.getWorldObj().getWorldTime() + partialTick;
                        capture(build(
                            ALOAD_1,
                            anyReference(INVOKEVIRTUAL),
                            anyReference(INVOKEVIRTUAL)
                        )),
                        capture(build(
                            L2F,
                            FLOAD, 8,
                            FADD,
                            anyFSTORE
                        ))
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // ... = (float) (... % MODULUS) + ...;
                        getCaptureGroup(1),
                        push(MODULUS),
                        LREM,
                        getCaptureGroup(2)
                    );
                }
            });
        }
    }
}
