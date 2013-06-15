package com.prupe.mcpatcher;

import javassist.bytecode.AccessFlag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.prupe.mcpatcher.BinaryRegex.*;
import static com.prupe.mcpatcher.BytecodeMatcher.*;
import static javassist.bytecode.Opcode.*;

public class BaseTexturePackMod extends Mod {
    private static final List<EarlyInitEntry> earlyInitMethods = new ArrayList<EarlyInitEntry>();

    protected final MethodRef earlyInitialize = new MethodRef(MCPatcherUtils.TEXTURE_PACK_CHANGE_HANDLER_CLASS, "earlyInitialize", "(Ljava/lang/String;Ljava/lang/String;)V");
    protected final MethodRef checkForTexturePackChange = new MethodRef(MCPatcherUtils.TEXTURE_PACK_CHANGE_HANDLER_CLASS, "checkForTexturePackChange", "()V");
    protected final MethodRef beforeChange1 = new MethodRef(MCPatcherUtils.TEXTURE_PACK_CHANGE_HANDLER_CLASS, "beforeChange1", "()V");
    protected final MethodRef afterChange1 = new MethodRef(MCPatcherUtils.TEXTURE_PACK_CHANGE_HANDLER_CLASS, "afterChange1", "()V");

    protected BaseTexturePackMod() {
        name = MCPatcherUtils.BASE_TEXTURE_PACK_MOD;
        author = "MCPatcher";
        description = "Internal mod required by the patcher.";
        version = "4.0";

        addClassMod(new MinecraftMod());
        addClassMod(new TextureManagerMod());
        addClassMod(new BaseMod.TextureUtilsMod(this));
        //addClassMod(new TexturePackListMod());
        //addClassMod(new BaseMod.ITexturePackMod(this));
        //addClassMod(new BaseMod.ITextureMod(this));
        addClassMod(new TextureBaseMod());
        addClassMod(new BaseMod.TextureNamedMod(this));
        addClassMod(new BaseMod.IconMod(this));
        addClassMod(new BaseMod.TextureMapMod(this));
        addClassMod(new BaseMod.TextureWithDataMod(this));
        addClassMod(new IResourcePackMod());
        addClassMod(new ResourcePackDefaultMod());
        addClassMod(new ResourcePackBaseMod());
        addClassMod(new ResourcePackZipMod());
        addClassMod(new ResourcePackFolderMod());
        addClassMod(new IResourceBundleMod());
        addClassMod(new ITextureResourceBundleMod());
        addClassMod(new TextureResourceBundleMod());
        addClassMod(new ResourceBundleMod());
        addClassMod(new IResourceMod());
        addClassMod(new BaseMod.ResourceAddressMod(this));

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

    static void clearEarlyInitializeMethods() {
        earlyInitMethods.clear();
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
            super(BaseTexturePackMod.this);

            final ClassRef textureManagerClass = new ClassRef("TextureManager");
            final MethodRef getTextureManager = new MethodRef(getDeobfClass(), "getTextureManager", "()LTextureManager;");
            final MethodRef getResourceBundle = new MethodRef(getDeobfClass(), "getResourceBundle", "()LIResourceBundle;");
            final MethodRef startGame = new MethodRef(getDeobfClass(), "startGame", "()V");
            final MethodRef runGameLoop = new MethodRef(getDeobfClass(), "runGameLoop", "()V");
            final MethodRef setTitle = new MethodRef("org/lwjgl/opengl/Display", "setTitle", "(Ljava/lang/String;)V");
            final MethodRef isCloseRequested = new MethodRef("org/lwjgl/opengl/Display", "isCloseRequested", "()Z");
            final MethodRef glViewport = new MethodRef(MCPatcherUtils.GL11_CLASS, "glViewport", "(IIII)V");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        reference(INVOKESTATIC, setTitle)
                    );
                }
            }.setMethod(startGame));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        reference(INVOKESTATIC, isCloseRequested)
                    );
                }
            }.setMethod(runGameLoop));

            addMemberMapper(new MethodMapper(getResourceBundle));
            addMemberMapper(new MethodMapper(getTextureManager));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "init texture pack handlers on startup";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        reference(NEW, textureManagerClass),
                        any(0, 500),
                        reference(INVOKESTATIC, glViewport)
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
            final FieldRef texturesByName = new FieldRef(getDeobfClass(), "texturesByName", "Ljava/util/Map;");
            final MethodRef bindTexture = new MethodRef(getDeobfClass(), "bindTexture", "(LResourceAddress;)V");
            final MethodRef getTexture = new MethodRef(getDeobfClass(), "getTexture", "(LResourceAddress;)LITexture;");
            final MethodRef addTexture = new MethodRef(getDeobfClass(), "addTexture", "(LResourceAddress;LITexture;)V");
            final MethodRef refreshTextures = new MethodRef(getDeobfClass(), "refreshTextures", "(LIResourceBundle;)V");

            addClassSignature(new ConstSignature("dynamic/%s_%d"));

            addMemberMapper(new FieldMapper(texturesByName));
            addMemberMapper(new MethodMapper(bindTexture).accessFlag(AccessFlag.STATIC, false));
            addMemberMapper(new MethodMapper(getTexture).accessFlag(AccessFlag.STATIC, false));
            addMemberMapper(new MethodMapper(addTexture));
            addMemberMapper(new MethodMapper(refreshTextures));

            addPatch(new MakeMemberPublicPatch(texturesByName));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "before texture pack change";
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
                    return "after texture pack change";
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

    private class TextureBaseMod extends BaseMod.TextureBaseMod {
        TextureBaseMod() {
            super(BaseTexturePackMod.this);

            final MethodRef unloadGLTexture = new MethodRef(getDeobfClass(), "unloadGLTexture", "()V");

            addPatch(new MakeMemberPublicPatch(glTextureId));

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

            addPatch(new AddMethodPatch(new MethodRef(getDeobfClass(), "finalize", "()V")) {
                @Override
                public byte[] generateMethod() {
                    return buildCode(
                        ALOAD_0,
                        reference(INVOKEVIRTUAL, unloadGLTexture),
                        RETURN
                    );
                }
            });
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

    private class IResourcePackMod extends ClassMod {
        IResourcePackMod() {
            addClassSignature(new InterfaceSignature(
                new InterfaceMethodRef(getDeobfClass(), "getInputStream", "(LResourceAddress;)Ljava/io/InputStream;"),
                new InterfaceMethodRef(getDeobfClass(), "hasResource", "(LResourceAddress;)Z"),
                new InterfaceMethodRef(getDeobfClass(), "getNamespaces", "()Ljava/util/List;"),
                new InterfaceMethodRef(getDeobfClass(), "getPackInfo", "(LMCMetaParser;)LMCMetaResourcePackInfo;"),
                new InterfaceMethodRef(getDeobfClass(), "getPackIcon", "()Ljava/awt/image/BufferedImage;")
            ).setInterfaceOnly(true));
        }
    }

    private class ResourcePackDefaultMod extends ClassMod {
        ResourcePackDefaultMod() {
            setInterfaces("IResourcePack");

            addClassSignature(new ConstSignature("minecraft"));
            addClassSignature(new ConstSignature("/assets/minecraft/"));
        }
    }

    private class ResourcePackBaseMod extends ClassMod {
        ResourcePackBaseMod() {
            setInterfaces("IResourcePack");

            final FieldRef file = new FieldRef(getDeobfClass(), "file", "Ljava/io/File;");

            addClassSignature(new ConstSignature("assets"));
            addClassSignature(new ConstSignature("pack.mcmeta"));

            addMemberMapper(new FieldMapper(file));

            addPatch(new MakeMemberPublicPatch(file));
        }
    }

    private class ResourcePackZipMod extends ClassMod {
        ResourcePackZipMod() {
            setParentClass("ResourcePackBase");

            final FieldRef zipFile = new FieldRef(getDeobfClass(), "zipFile", "Ljava/util/zip/ZipFile;");

            addClassSignature(new ConstSignature("assets/"));
            addClassSignature(new ConstSignature(new ClassRef("java/util/zip/ZipFile")));

            addMemberMapper(new FieldMapper(zipFile));

            addPatch(new MakeMemberPublicPatch(zipFile));
        }
    }

    private class ResourcePackFolderMod extends ClassMod {
        ResourcePackFolderMod() {
            setParentClass("ResourcePackBase");

            addClassSignature(new ConstSignature("assets/"));
            addClassSignature(new ConstSignature(new ClassRef("java/io/FileInputStream")));
        }
    }

    private class IResourceBundleMod extends ClassMod {
        IResourceBundleMod() {
            addClassSignature(new InterfaceSignature(
                new InterfaceMethodRef(getDeobfClass(), "getResource", "(LResourceAddress;)LIResource;")
            ).setInterfaceOnly(true));
        }
    }

    private class ITextureResourceBundleMod extends ClassMod {
        ITextureResourceBundleMod() {
            setInterfaces("IResourceBundle");

            addClassSignature(new InterfaceSignature(
                new InterfaceMethodRef(getDeobfClass(), "method1", "(Ljava/util/List;)V"),
                new InterfaceMethodRef(getDeobfClass(), "method2", "(LILoadableResource;)V")
            ).setInterfaceOnly(true));
        }
    }

    private class TextureResourceBundleMod extends ClassMod {
        TextureResourceBundleMod() {
            setInterfaces("ITextureResourceBundle");

            final ClassRef fnfException = new ClassRef("java/io/FileNotFoundException");
            final MethodRef fnfInit = new MethodRef("java/io/FileNotFoundException", "<init>", "(Ljava/lang/String;)V");
            final MethodRef getResource = new MethodRef(getDeobfClass(), "getResource", "(LResourceAddress;)LIResource;");
            final MethodRef addressToString = new MethodRef("ResourceAddress", "toString", "()Ljava/lang/String;");

            addClassSignature(new ConstSignature(new InterfaceMethodRef("java/util/Map", "clear", "()V")));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        reference(NEW, fnfException),
                        DUP,
                        ALOAD_1,
                        captureReference(INVOKEVIRTUAL),
                        reference(INVOKESPECIAL, fnfInit),
                        ATHROW
                    );
                }
            }
                .setMethod(getResource)
                .addXref(1, addressToString)
            );
        }
    }

    private class ResourceBundleMod extends ClassMod {
        ResourceBundleMod() {
            setInterfaces("IResourceBundle");

            final FieldRef resourcePacks = new FieldRef(getDeobfClass(), "resourcePacks", "Ljava/util/List;");

            addClassSignature(new ConstSignature(".mcmeta"));

            addMemberMapper(new FieldMapper(resourcePacks));

            addPatch(new MakeMemberPublicPatch(resourcePacks));
        }
    }

    private class IResourceMod extends ClassMod {
        IResourceMod() {
            addClassSignature(new InterfaceSignature(
                new InterfaceMethodRef(getDeobfClass(), "getAddress", "()LResourceAddress;"),
                new InterfaceMethodRef(getDeobfClass(), "getInputStream", "()Ljava/io/InputStream;"),
                new InterfaceMethodRef(getDeobfClass(), "isPresent", "()Z"),
                new InterfaceMethodRef(getDeobfClass(), "getMCMeta", "(Ljava/lang/String;)LIMCMeta;")
            ));
        }
    }
}
