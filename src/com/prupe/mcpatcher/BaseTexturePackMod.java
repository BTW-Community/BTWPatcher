package com.prupe.mcpatcher;

import javassist.bytecode.AccessFlag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.prupe.mcpatcher.BinaryRegex.*;
import static com.prupe.mcpatcher.BytecodeMatcher.*;
import static javassist.bytecode.Opcode.*;

public class BaseTexturePackMod extends Mod {
    public static final String NAME = "__TexturePackBase";

    private static final List<EarlyInitEntry> earlyInitMethods = new ArrayList<EarlyInitEntry>();

    protected final MethodRef earlyInitialize = new MethodRef(MCPatcherUtils.TEXTURE_PACK_CHANGE_HANDLER_CLASS, "earlyInitialize", "(Ljava/lang/String;Ljava/lang/String;)V");
    protected final MethodRef checkForTexturePackChange = new MethodRef(MCPatcherUtils.TEXTURE_PACK_CHANGE_HANDLER_CLASS, "checkForTexturePackChange", "()V");
    protected final MethodRef beforeChange1 = new MethodRef(MCPatcherUtils.TEXTURE_PACK_CHANGE_HANDLER_CLASS, "beforeChange1", "()V");
    protected final MethodRef afterChange1 = new MethodRef(MCPatcherUtils.TEXTURE_PACK_CHANGE_HANDLER_CLASS, "afterChange1", "()V");

    protected BaseTexturePackMod() {
        name = NAME;
        author = "MCPatcher";
        description = "Internal mod required by the patcher.";
        version = "2.1";

        earlyInitMethods.clear();

        addClassMod(new MinecraftMod());
        addClassMod(new TextureManagerMod());
        addClassMod(new TextureUtilsMod());
        addClassMod(new TexturePackListMod());
        addClassMod(new ITexturePackMod());
        addClassMod(new BaseMod.ITextureMod());
        addClassMod(new TextureBaseMod());
        addClassMod(new TexturePackImplementationMod());
        addClassMod(new TexturePackDefaultMod());
        addClassMod(new TexturePackCustomMod());
        addClassMod(new TexturePackFolderMod());

        addClassFile(MCPatcherUtils.TEXTURE_PACK_API_CLASS);
        addClassFile(MCPatcherUtils.TEXTURE_PACK_API_CLASS + "$1");
        addClassFile(MCPatcherUtils.TEXTURE_PACK_CHANGE_HANDLER_CLASS);
        addClassFile(MCPatcherUtils.TEXTURE_PACK_CHANGE_HANDLER_CLASS + "$1");
        addClassFile(MCPatcherUtils.WEIGHTED_INDEX_CLASS);
        addClassFile(MCPatcherUtils.WEIGHTED_INDEX_CLASS + "$1");
        addClassFile(MCPatcherUtils.WEIGHTED_INDEX_CLASS + "$2");
        addClassFile(MCPatcherUtils.BLEND_METHOD_CLASS);
    }

    @Override
    public String[] getLoggingCategories() {
        return new String[]{"Texture Pack"};
    }

    public static void earlyInitialize(int order, String className, String methodName) {
        earlyInitMethods.add(new EarlyInitEntry(order, className, methodName));
    }

    private static class EarlyInitEntry implements Comparable<EarlyInitEntry> {
        private final int order;
        private final String className;
        private final String methodName;

        EarlyInitEntry(int order, String className, String methodName) {
            this.order = order;
            this.className = className;
            this.methodName = methodName;
        }

        public int compareTo(EarlyInitEntry o) {
            return order - o.order;
        }
    }

    private class MinecraftMod extends BaseMod.MinecraftMod {
        MinecraftMod() {
            final MethodRef getTextureManager = new MethodRef(getDeobfClass(), "getTextureManager", "()LTextureManager;");
            final FieldRef renderEngine = new FieldRef(getDeobfClass(), "renderEngine", "LRenderEngine;");
            final FieldRef texturePackList = new FieldRef(getDeobfClass(), "texturePackList", "LTexturePackList;");
            final MethodRef startGame = new MethodRef(getDeobfClass(), "startGame", "()V");
            final MethodRef runGameLoop = new MethodRef(getDeobfClass(), "runGameLoop", "()V");
            final MethodRef refreshTextureMaps = new MethodRef("RenderEngine", "refreshTextureMaps", "()V");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        reference(INVOKESTATIC, new MethodRef("org/lwjgl/opengl/Display", "setTitle", "(Ljava/lang/String;)V"))
                    );
                }
            }.setMethod(startGame));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        reference(INVOKESTATIC, new MethodRef("org/lwjgl/opengl/Display", "isCloseRequested", "()Z"))
                    );
                }
            }.setMethod(runGameLoop));

            addMemberMapper(new FieldMapper(texturePackList));
            addMemberMapper(new MethodMapper(getTextureManager));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "init texture pack handlers on startup";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        reference(NEW, new ClassRef("TextureManager")),
                        any(0, 500),
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.GL11_CLASS, "glViewport", "(IIII)V"))
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    byte[] earlyInitCode = new byte[0];
                    Collections.sort(earlyInitMethods);
                    for (EarlyInitEntry entry : earlyInitMethods) {
                        earlyInitCode = buildCode(
                            earlyInitCode,
                            push(entry.className),
                            push(entry.methodName),
                            reference(INVOKESTATIC, earlyInitialize)
                        );
                    }
                    return buildCode(
                        earlyInitCode,
                        reference(INVOKESTATIC, beforeChange1),
                        getMatch(),
                        reference(INVOKESTATIC, afterChange1)
                    );
                }
            }.targetMethod(startGame));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "check for texture pack change";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        BinaryRegex.begin()
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        reference(INVOKESTATIC, checkForTexturePackChange)
                    );
                }
            }.targetMethod(runGameLoop));
        }
    }

    private class TextureManagerMod extends ClassMod {
        TextureManagerMod() {
            final MethodRef bindTexture = new MethodRef(getDeobfClass(), "bindTexture", "(Ljava/lang/String;)V");
            final MethodRef getTexture = new MethodRef(getDeobfClass(), "getTexture", "(Ljava/lang/String;)LITexture;");
            final MethodRef unloadTexture = new MethodRef(getDeobfClass(), "unloadTexture", "(Ljava/lang/String;)V");

            addClassSignature(new ConstSignature("dynamic/%s_%d"));

            addMemberMapper(new MethodMapper(bindTexture, unloadTexture).accessFlag(AccessFlag.STATIC, false));
            addMemberMapper(new MethodMapper(getTexture).accessFlag(AccessFlag.STATIC, false));
        }
    }

    private class TextureUtilsMod extends ClassMod {
        TextureUtilsMod() {
            final MethodRef glTexSubImage2D = new MethodRef(MCPatcherUtils.GL11_CLASS, "glTexSubImage2D", "(IIIIIIIILjava/nio/IntBuffer;)V");
            final MethodRef glTexParameteri = new MethodRef(MCPatcherUtils.GL11_CLASS, "glTexParameteri", "(III)V");
            final MethodRef glTexImage2D = new MethodRef(MCPatcherUtils.GL11_CLASS, "glTexImage2D", "(IIIIIIIILjava/nio/IntBuffer;)V");
            final MethodRef glGenTextures = new MethodRef(MCPatcherUtils.GL11_CLASS, "glGenTextures", "()I");
            final MethodRef glDeleteTextures = new MethodRef(MCPatcherUtils.GL11_CLASS, "glDeleteTextures", "(I)V");

            addClassSignature(new ConstSignature(glTexSubImage2D));
            addClassSignature(new ConstSignature(glTexParameteri));
            addClassSignature(new ConstSignature(glTexImage2D));
            addClassSignature(new ConstSignature(glGenTextures));
            addClassSignature(new ConstSignature(glDeleteTextures));
        }
    }

    private class TextureBaseMod extends BaseMod.TextureBaseMod {
        TextureBaseMod() {
            final MethodRef unloadGLTexture = new MethodRef(getDeobfClass(), "unloadGLTexture", "()V");

            addPatch(new AddMethodPatch(unloadGLTexture) {
                @Override
                public byte[] generateMethod() {
                    return buildCode(
                        // if (this.glTexture >= 0) {
                        ALOAD_0,
                        reference(GETFIELD, glTextureId),
                        IFLT, branch("A"),

                        // GL11.glDeleteTextures(this.glTexture);
                        ALOAD_0,
                        reference(GETFIELD, glTextureId),
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.GL11_CLASS, "glDeleteTextures", "(I)V")),

                        // this.glTexture = -1;
                        ALOAD_0,
                        push(-1),
                        reference(PUTFIELD, glTextureId),

                        // }
                        label("A"),
                        RETURN
                    );
                }
            });
        }
    }

    private class RenderEngineMod extends BaseMod.RenderEngineMod {
        RenderEngineMod() {
            final FieldRef missingTextureImage = new FieldRef(getDeobfClass(), "missingTextureImage", "Ljava/awt/image/BufferedImage;");
            final MethodRef deleteTexture = new MethodRef(getDeobfClass(), "deleteTexture", "(I)V");
            final MethodRef setupTexture = new MethodRef(getDeobfClass(), "setupTexture", "(Ljava/awt/image/BufferedImage;I)V");
            final MethodRef setupTextureExt = new MethodRef(getDeobfClass(), "setupTextureExt", "(Ljava/awt/image/BufferedImage;IZZ)V");
            final MethodRef getImageContents = new MethodRef(getDeobfClass(), "getImageContents", "(Ljava/awt/image/BufferedImage;[I)[I");
            final MethodRef readTextureImage = new MethodRef(getDeobfClass(), "readTextureImage", "(Ljava/io/InputStream;)Ljava/awt/image/BufferedImage;");
            final MethodRef bindTextureByName = new MethodRef(getDeobfClass(), "bindTextureByName", "(Ljava/lang/String;)V");
            final MethodRef bindTexture = new MethodRef(getDeobfClass(), "bindTexture", "(I)V");
            final MethodRef resetBoundTexture = new MethodRef(getDeobfClass(), "resetBoundTexture", "()V");
            final MethodRef clear = new MethodRef("java/nio/IntBuffer", "clear", "()Ljava/nio/Buffer;");
            final MethodRef put = new MethodRef("java/nio/IntBuffer", "put", "([I)Ljava/nio/IntBuffer;");
            final MethodRef position = new MethodRef("java/nio/IntBuffer", "position", "(I)Ljava/nio/Buffer;");
            final MethodRef limit = new MethodRef("java/nio/Buffer", "limit", "(I)Ljava/nio/Buffer;");
            final MethodRef getIntBuffer = new MethodRef(MCPatcherUtils.TEXTURE_PACK_API_CLASS, "getIntBuffer", "(Ljava/nio/IntBuffer;[I)Ljava/nio/IntBuffer;");
            final MethodRef getSelectedTexturePack = new MethodRef("TexturePackList", "getSelectedTexturePack", "()LITexturePack;");
            final InterfaceMethodRef getResourceAsStream = new InterfaceMethodRef("ITexturePack", "getResourceAsStream", "(Ljava/lang/String;)Ljava/io/InputStream;");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(or(
                        build(reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.GL11_CLASS, "glDeleteTextures", "(Ljava/nio/IntBuffer;)V"))),
                        build(reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.GL11_CLASS, "glDeleteTextures", "(I)V")))
                    ));
                }
            }.setMethod(deleteTexture));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.GL11_CLASS, "glBindTexture", "(II)V"))
                    );
                }
            }.setMethod(bindTexture));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        begin(),
                        ALOAD_0,
                        push(-1)
                    );
                }
            }.setMethod(resetBoundTexture));

            addMemberMapper(new FieldMapper(missingTextureImage));
            addMemberMapper(new MethodMapper(setupTexture));
            addMemberMapper(new MethodMapper(setupTextureExt));
            addMemberMapper(new MethodMapper(getImageContents));
            addMemberMapper(new MethodMapper(readTextureImage));
            addMemberMapper(new MethodMapper(bindTextureByName));

            addPatch(new MakeMemberPublicPatch(bindTexture));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "imageData.clear(), .put(), .limit() -> imageData = TexturePackAPI.getIntBuffer()";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // imageData.clear();
                        ALOAD_0,
                        reference(GETFIELD, imageData),
                        reference(INVOKEVIRTUAL, clear),
                        POP,

                        // imageData.put($1);
                        ALOAD_0,
                        reference(GETFIELD, imageData),
                        capture(any(1, 5)),
                        reference(INVOKEVIRTUAL, put),
                        POP,

                        // imageData.position(0).limit($1.length);
                        ALOAD_0,
                        reference(GETFIELD, imageData),
                        ICONST_0,
                        reference(INVOKEVIRTUAL, position),
                        backReference(1),
                        ARRAYLENGTH,
                        reference(INVOKEVIRTUAL, limit),
                        POP
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // imageData = TexturePackAPI.getByteBuffer(imageData, $1);
                        ALOAD_0,
                        ALOAD_0,
                        reference(GETFIELD, imageData),
                        getCaptureGroup(1),
                        reference(INVOKESTATIC, getIntBuffer),
                        reference(PUTFIELD, imageData)
                    );
                }
            });

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
                        // InputStream inputStream = this.texturePackList.getSelectedTexturePack().getResourceAsStream(var1);
                        ALOAD_0,
                        anyReference(GETFIELD),
                        reference(INVOKEVIRTUAL, getSelectedTexturePack),
                        ALOAD_1,
                        reference(INVOKEINTERFACE, getResourceAsStream),
                        ASTORE, capture(any()),

                        // if (inputStream == null) {
                        ALOAD, backReference(1),
                        IFNONNULL, any(2),

                        // this.setupTexture(this.missingTextureImage, texture, blur, clamp);
                        ALOAD_0,
                        ALOAD_0,
                        reference(GETFIELD, missingTextureImage),
                        capture(build(
                            anyILOAD,
                            anyILOAD,
                            anyILOAD,
                            reference(INVOKEVIRTUAL, setupTextureExt)
                        )),

                        // } else {
                        GOTO, any(2),

                        // this.setupTexture(this.readTextureImage(inputStream, texture, blur, clamp);
                        // -or-
                        // image = this.readTextureImage(inputStream, texture, blur, clamp);
                        // ...
                        // this.setupTexture(image, texture, blur, clamp);
                        optional(build(ALOAD_0)),
                        ALOAD_0,
                        ALOAD, backReference(1),
                        reference(INVOKESPECIAL, readTextureImage),
                        capture(any(0, 20)),
                        backReference(2)

                        // }
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    int reg = getMethodInfo().getCodeAttribute().getMaxLocals();
                    return buildCode(
                        // BufferedImage image = TexturePackAPI.getImage(var1);
                        ALOAD_1,
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.TEXTURE_PACK_API_CLASS, "getImage", "(Ljava/lang/String;)Ljava/awt/image/BufferedImage;")),
                        ASTORE, reg,

                        // if (image == null) {
                        ALOAD, reg,
                        IFNONNULL, branch("A"),

                        ALOAD_0,
                        reference(GETFIELD, missingTextureImage),
                        ASTORE, reg,

                        // }
                        label("A"),

                        // ...
                        // this.setupTextureExt(image, ...);
                        getCaptureGroup(3).length > 0 ? buildCode(ALOAD, reg, getCaptureGroup(3)) : new byte[0],
                        ALOAD_0,
                        ALOAD, reg,
                        getCaptureGroup(2)
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
            }.targetMethod(setupTextureExt));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "null check in getImageContents";
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
            }.targetMethod(getImageContents));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "null check in readTextureImage";
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
                        ACONST_NULL,
                        ARETURN,
                        label("A")
                    );
                }
            }.targetMethod(readTextureImage));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "before texture refresh";
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
                        reference(INVOKESTATIC, beforeChange1)
                    );
                }
            }.targetMethod(refreshTextures));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "after texture refresh";
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
                        reference(INVOKESTATIC, afterChange1)
                    );
                }
            }
                .setInsertBefore(true)
                .targetMethod(refreshTextures)
            );
        }
    }

    private class TexturePackListMod extends ClassMod {
        TexturePackListMod() {
            final FieldRef selectedTexturePack = new FieldRef(getDeobfClass(), "selectedTexturePack", "LITexturePack;");
            final FieldRef mc = new FieldRef(getDeobfClass(), "mc", "LMinecraft;");
            final MethodRef getSelectedTexturePack = new MethodRef(getDeobfClass(), "getSelectedTexturePack", "()LITexturePack;");
            final MethodRef setTexturePack = new MethodRef(getDeobfClass(), "setTexturePack", "(LITexturePack;)Z");
            final MethodRef updateAvailableTexturePacks = new MethodRef(getDeobfClass(), "updateAvailableTexturePacks", "()V");
            final MethodRef onDownloadFinished = new MethodRef(getDeobfClass(), "onDownloadFinished", "()V");
            final MethodRef scheduleTexturePackRefresh = new MethodRef("Minecraft", "scheduleTexturePackRefresh", "()V");

            addClassSignature(new ConstSignature(".zip"));
            addClassSignature(new ConstSignature("texturepacks"));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        captureReference(GETFIELD),
                        ALOAD_1,
                        reference(INVOKEINTERFACE, new InterfaceMethodRef("java/util/List", "removeAll", "(Ljava/util/Collection;)Z")),
                        POP
                    );
                }
            }.setMethod(updateAvailableTexturePacks));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        captureReference(GETFIELD),
                        captureReference(INVOKEVIRTUAL)
                    );
                }
            }
                .setMethod(onDownloadFinished)
                .addXref(1, mc)
                .addXref(2, scheduleTexturePackRefresh)
            );

            addMemberMapper(new MethodMapper(setTexturePack));

            addMemberMapper(new FieldMapper(selectedTexturePack)
                .accessFlag(AccessFlag.PRIVATE, true)
                .accessFlag(AccessFlag.STATIC, false)
                .accessFlag(AccessFlag.FINAL, false)
            );

            addMemberMapper(new MethodMapper(getSelectedTexturePack)
                .accessFlag(AccessFlag.PUBLIC, true)
                .accessFlag(AccessFlag.STATIC, false)
            );
        }
    }

    private class ITexturePackMod extends ClassMod {
        ITexturePackMod() {
            addClassSignature(new InterfaceSignature(
                new InterfaceMethodRef(getDeobfClass(), "deleteTexturePack", "(LRenderEngine;)V"),
                new InterfaceMethodRef(getDeobfClass(), "bindThumbnailTexture", "(LRenderEngine;)V"),
                new InterfaceMethodRef(getDeobfClass(), "getResourceAsStream2", "(Ljava/lang/String;Z)Ljava/io/InputStream;"),
                new InterfaceMethodRef(getDeobfClass(), "getResourceAsStream", "(Ljava/lang/String;)Ljava/io/InputStream;"),
                new InterfaceMethodRef(getDeobfClass(), "getTexturePackID", "()Ljava/lang/String;"),
                new InterfaceMethodRef(getDeobfClass(), "getTexturePackFileName", "()Ljava/lang/String;"),
                new InterfaceMethodRef(getDeobfClass(), "getFirstDescriptionLine", "()Ljava/lang/String;"),
                new InterfaceMethodRef(getDeobfClass(), "getSecondDescriptionLine", "()Ljava/lang/String;"),
                new InterfaceMethodRef(getDeobfClass(), "hasResource", "(Ljava/lang/String;Z)Z"),
                getMinecraftVersion().compareTo("1.5") >= 0 ?
                    null : new InterfaceMethodRef(getDeobfClass(), "getTexturePackResolution", "()I"),
                new InterfaceMethodRef(getDeobfClass(), "isCompatible", "()Z")
            ).setInterfaceOnly(true));
        }
    }

    private class TexturePackImplementationMod extends ClassMod {
        TexturePackImplementationMod() {
            setInterfaces("ITexturePack");

            final FieldRef texturePackFile = new FieldRef(getDeobfClass(), "texturePackFile", "Ljava/io/File;");

            addClassSignature(new ConstSignature("gui/unknown_pack.png"));
            addClassSignature(new ConstSignature("pack.txt"));

            addMemberMapper(new FieldMapper(texturePackFile));

            addPatch(new MakeMemberPublicPatch(texturePackFile));
        }
    }

    private class TexturePackDefaultMod extends ClassMod {
        TexturePackDefaultMod() {
            setParentClass("TexturePackImplementation");

            addClassSignature(new ConstSignature("The default look of Minecraft"));
        }
    }

    private class TexturePackCustomMod extends ClassMod {
        TexturePackCustomMod() {
            setParentClass("TexturePackImplementation");

            final FieldRef zipFile = new FieldRef(getDeobfClass(), "zipFile", "Ljava/util/zip/ZipFile;");

            addClassSignature(new ConstSignature(new MethodRef("java/util/zip/ZipFile", "getEntry", "(Ljava/lang/String;)Ljava/util/zip/ZipEntry;")));
            addClassSignature(new ConstSignature(new MethodRef("java/util/zip/ZipFile", "close", "()V")));
            addClassSignature(new ConstSignature("textures/"));

            addMemberMapper(new FieldMapper(zipFile));

            addPatch(new MakeMemberPublicPatch(zipFile));
            addPatch(new AddFieldPatch(new FieldRef(getDeobfClass(), "origZip", "Ljava/util/zip/ZipFile;")));
            addPatch(new AddFieldPatch(new FieldRef(getDeobfClass(), "tmpFile", "Ljava/io/File;")));
            addPatch(new AddFieldPatch(new FieldRef(getDeobfClass(), "lastModified", "J")));
        }
    }

    private class TexturePackFolderMod extends ClassMod {
        TexturePackFolderMod() {
            setParentClass("TexturePackImplementation");

            addClassSignature(new ConstSignature(new ClassRef("java.io.FileInputStream")));
            addClassSignature(new ConstSignature("textures/"));
        }
    }
}
