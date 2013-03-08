package com.prupe.mcpatcher;

import javassist.bytecode.AccessFlag;

import java.util.HashMap;
import java.util.Map;

import static com.prupe.mcpatcher.BinaryRegex.*;
import static com.prupe.mcpatcher.BytecodeMatcher.*;
import static javassist.bytecode.Opcode.*;

public class BaseTexturePackMod extends Mod {
    public static final String NAME = "__TexturePackBase";

    private static final Map<String, String> earlyInitMethods = new HashMap<String, String>();

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
        addClassMod(new RenderEngineMod());
        addClassMod(new TexturePackListMod());
        addClassMod(new ITexturePackMod());
        addClassMod(new TexturePackImplementationMod());
        addClassMod(new TexturePackDefaultMod());
        addClassMod(new TexturePackCustomMod());
        addClassMod(new TexturePackFolderMod());
        addClassMod(new GetResourceMod());

        addClassFile(MCPatcherUtils.TEXTURE_PACK_API_CLASS);
        addClassFile(MCPatcherUtils.TEXTURE_PACK_CHANGE_HANDLER_CLASS);
        addClassFile(MCPatcherUtils.TEXTURE_PACK_CHANGE_HANDLER_CLASS + "$1");
        addClassFile(MCPatcherUtils.WEIGHTED_INDEX_CLASS);
        addClassFile(MCPatcherUtils.WEIGHTED_INDEX_CLASS + "$1");
        addClassFile(MCPatcherUtils.WEIGHTED_INDEX_CLASS + "$2");
        addClassFile(MCPatcherUtils.BLEND_METHOD_CLASS);
    }

    @Override
    public String[] getLoggingCategories() {
        if (name.equals(NAME)) {
            return new String[]{"Texture Pack"};
        } else {
            return super.getLoggingCategories();
        }
    }

    public static void earlyInitialize(String className, String methodName) {
        earlyInitMethods.put(className, methodName);
    }

    private class MinecraftMod extends BaseMod.MinecraftMod {
        MinecraftMod() {
            final FieldRef texturePackList = new FieldRef(getDeobfClass(), "texturePackList", "LTexturePackList;");
            final FieldRef renderEngine = new FieldRef(getDeobfClass(), "renderEngine", "LRenderEngine;");
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
            addMemberMapper(new FieldMapper(renderEngine));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "init texture pack handlers on startup";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        reference(GETFIELD, renderEngine),
                        reference(INVOKEVIRTUAL, refreshTextureMaps)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    byte[] earlyInitCode = new byte[0];
                    for (Map.Entry<String, String> entry : earlyInitMethods.entrySet()) {
                        earlyInitCode = buildCode(
                            earlyInitCode,
                            push(entry.getKey()),
                            push(entry.getValue()),
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

    private class RenderEngineMod extends BaseMod.RenderEngineMod {
        RenderEngineMod() {
            final MethodRef deleteTexture = new MethodRef(getDeobfClass(), "deleteTexture", "(I)V");
            final MethodRef setupTexture = new MethodRef(getDeobfClass(), "setupTexture", "(Ljava/awt/image/BufferedImage;I)V");
            final MethodRef getImageContents = new MethodRef(getDeobfClass(), "getImageContents", "(Ljava/awt/image/BufferedImage;[I)[I");
            final MethodRef readTextureImage = new MethodRef(getDeobfClass(), "readTextureImage", "(Ljava/io/InputStream;)Ljava/awt/image/BufferedImage;");
            final MethodRef bindTexture = new MethodRef(getDeobfClass(), "bindTexture", "(Ljava/lang/String;)V");
            final MethodRef clearBoundTexture = new MethodRef(getDeobfClass(), "clearBoundTexture", "()V");
            final MethodRef clear = new MethodRef("java/nio/IntBuffer", "clear", "()Ljava/nio/Buffer;");
            final MethodRef put = new MethodRef("java/nio/IntBuffer", "put", "([I)Ljava/nio/IntBuffer;");
            final MethodRef position = new MethodRef("java/nio/IntBuffer", "position", "(I)Ljava/nio/Buffer;");
            final MethodRef limit = new MethodRef("java/nio/Buffer", "limit", "(I)Ljava/nio/Buffer;");
            final MethodRef getIntBuffer = new MethodRef(MCPatcherUtils.TEXTURE_PACK_API_CLASS, "getIntBuffer", "(Ljava/nio/IntBuffer;[I)Ljava/nio/IntBuffer;");

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
                        begin(),
                        ALOAD_0,
                        push(-1)
                    );
                }
            }.setMethod(clearBoundTexture));

            addMemberMapper(new MethodMapper(setupTexture));
            addMemberMapper(new MethodMapper(getImageContents));
            addMemberMapper(new MethodMapper(readTextureImage));
            addMemberMapper(new MethodMapper(bindTexture));

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
            }.targetMethod(setupTexture));

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
            interfaces = new String[]{"ITexturePack"};

            final FieldRef texturePackFile = new FieldRef(getDeobfClass(), "texturePackFile", "Ljava/io/File;");

            addClassSignature(new ConstSignature("/pack.txt"));

            addMemberMapper(new FieldMapper(texturePackFile));

            addPatch(new MakeMemberPublicPatch(texturePackFile));
        }
    }

    private class TexturePackDefaultMod extends ClassMod {
        TexturePackDefaultMod() {
            parentClass = "TexturePackImplementation";

            addClassSignature(new ConstSignature("The default look of Minecraft"));
        }
    }

    private class TexturePackCustomMod extends ClassMod {
        TexturePackCustomMod() {
            parentClass = "TexturePackImplementation";

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
            parentClass = "TexturePackImplementation";

            final MethodRef substring = new MethodRef("java/lang/String", "substring", "(I)Ljava/lang/String;");

            addClassSignature(new ConstSignature(new ClassRef("java.io.FileInputStream")));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_1,
                        push(1),
                        reference(INVOKEVIRTUAL, substring)
                    );
                }
            });
        }
    }

    private class GetResourceMod extends ClassMod {
        GetResourceMod() {
            global = true;

            final MethodRef getResource = new MethodRef("java.lang.Class", "getResource", "(Ljava/lang/String;)Ljava/net/URL;");
            final MethodRef readURL = new MethodRef("javax.imageio.ImageIO", "read", "(Ljava/net/URL;)Ljava/awt/image/BufferedImage;");
            final MethodRef getResourceAsStream = new MethodRef("java.lang.Class", "getResourceAsStream", "(Ljava/lang/String;)Ljava/io/InputStream;");
            final MethodRef readStream = new MethodRef("javax.imageio.ImageIO", "read", "(Ljava/io/InputStream;)Ljava/awt/image/BufferedImage;");

            addClassSignature(new OrSignature(
                new ConstSignature(getResource),
                new ConstSignature(getResourceAsStream)
            ));
            addClassSignature(new OrSignature(
                new ConstSignature(readURL),
                new ConstSignature(readStream)
            ));

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