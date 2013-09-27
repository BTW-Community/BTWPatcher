package com.prupe.mcpatcher.mal;

import com.prupe.mcpatcher.*;
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
    protected final MethodRef beforeChange1 = new MethodRef(MCPatcherUtils.TEXTURE_PACK_CHANGE_HANDLER_CLASS, "beforeChange1", "(Z)V");
    protected final MethodRef afterChange1 = new MethodRef(MCPatcherUtils.TEXTURE_PACK_CHANGE_HANDLER_CLASS, "afterChange1", "(Z)V");

    public BaseTexturePackMod() {
        name = MCPatcherUtils.BASE_TEXTURE_PACK_MOD;
        author = "MCPatcher";
        description = "Internal mod required by the patcher.";
        version = "4.0";

        addClassMod(new MinecraftMod());
        addClassMod(new TextureManagerMod());
        addClassMod(new BaseMod.TextureUtilMod(this));
        addClassMod(new AbstractTextureMod());
        addClassMod(new BaseMod.SimpleTextureMod(this));
        addClassMod(new BaseMod.IconMod(this));
        addClassMod(new BaseMod.TextureAtlasMod(this));
        addClassMod(new BaseMod.DynamicTextureMod(this));
        addClassMod(new ResourcePackMod());
        addClassMod(new DefaultResourcePackMod());
        addClassMod(new AbstractResourcePackMod());
        addClassMod(new FileResourcePackMod());
        addClassMod(new FolderResourcePackMod());
        addClassMod(new ResourceManagerMod());
        addClassMod(new ReloadableResourceManagerMod());
        addClassMod(new SimpleReloadableResourceManagerMod());
        addClassMod(new FallbackResourceManagerMod());
        addClassMod(new BaseMod.ResourceMod(this));
        addClassMod(new BaseMod.ResourceLocationMod(this));

        addClassFile(MCPatcherUtils.TEXTURE_PACK_API_CLASS);
        addClassFile(MCPatcherUtils.TEXTURE_PACK_API_CLASS + "$1");
        addClassFile(MCPatcherUtils.TEXTURE_PACK_CHANGE_HANDLER_CLASS);
        addClassFile(MCPatcherUtils.TEXTURE_PACK_CHANGE_HANDLER_CLASS + "$1");
        addClassFile(MCPatcherUtils.WEIGHTED_INDEX_CLASS);
        addClassFile(MCPatcherUtils.WEIGHTED_INDEX_CLASS + "$1");
        addClassFile(MCPatcherUtils.WEIGHTED_INDEX_CLASS + "$2");
        addClassFile(MCPatcherUtils.BLEND_METHOD_CLASS);
        addClassFile(MCPatcherUtils.INPUT_HANDLER_CLASS);
    }

    @Override
    public String[] getLoggingCategories() {
        return new String[]{"Texture Pack"};
    }

    public static void preInitialize() {
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

            final ClassRef textureResourceManagerClass = new ClassRef("SimpleReloadableResourceManager");
            final MethodRef getTextureManager = new MethodRef(getDeobfClass(), "getTextureManager", "()LTextureManager;");
            final MethodRef getResourceManager = new MethodRef(getDeobfClass(), "getResourceManager", "()LResourceManager;");
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

            addMemberMapper(new MethodMapper(getResourceManager));
            addMemberMapper(new MethodMapper(getTextureManager));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "init texture pack handlers on startup";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        reference(NEW, textureResourceManagerClass),
                        any(0, 700),
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
                        push(1),
                        reference(INVOKESTATIC, beforeChange1),
                        getMatch(),
                        push(1),
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
            final MethodRef bindTexture = new MethodRef(getDeobfClass(), "bindTexture", "(LResourceLocation;)V");
            final MethodRef getTexture = new MethodRef(getDeobfClass(), "getTexture", "(LResourceLocation;)LTextureObject;");
            final MethodRef refreshTextures = new MethodRef(getDeobfClass(), "refreshTextures", "(LResourceManager;)V");

            addClassSignature(new ConstSignature("dynamic/%s_%d"));

            addMemberMapper(new FieldMapper(texturesByName));
            addMemberMapper(new MethodMapper(bindTexture).accessFlag(AccessFlag.STATIC, false));
            addMemberMapper(new MethodMapper(getTexture).accessFlag(AccessFlag.STATIC, false));
            addMemberMapper(new MethodMapper(refreshTextures));

            addPatch(new MakeMemberPublicPatch(texturesByName));
        }
    }

    private class AbstractTextureMod extends BaseMod.AbstractTextureMod {
        AbstractTextureMod() {
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

    private class ResourcePackMod extends ClassMod {
        ResourcePackMod() {
            String nsType = getMinecraftVersion().compareTo("13w25c") >= 0 ? "Set" : "List";
            boolean newMCMeta = getMinecraftVersion().compareTo("13w26a") >= 0;
            addClassSignature(new InterfaceSignature(
                new InterfaceMethodRef(getDeobfClass(), "getInputStream", "(LResourceLocation;)Ljava/io/InputStream;"),
                new InterfaceMethodRef(getDeobfClass(), "hasResource", "(LResourceLocation;)Z"),
                new InterfaceMethodRef(getDeobfClass(), "getNamespaces", "()Ljava/util/" + nsType + ";"),
                newMCMeta ?
                    new InterfaceMethodRef(getDeobfClass(), "getMCMeta", "(LMetadataSectionSerializer;Ljava/lang/String;)LMCMeta;") :
                    new InterfaceMethodRef(getDeobfClass(), "getPackInfo", "(LMetadataSectionSerializer;)LPackMetadataSection;"),
                new InterfaceMethodRef(getDeobfClass(), "getPackIcon", "()Ljava/awt/image/BufferedImage;"),
                newMCMeta ?
                    new InterfaceMethodRef(getDeobfClass(), "getName", "()Ljava/lang/String;") : null
            ).setInterfaceOnly(true));
        }
    }

    private class DefaultResourcePackMod extends ClassMod {
        DefaultResourcePackMod() {
            setInterfaces("ResourcePack");

            final FieldRef file = new FieldRef(getDeobfClass(), "file", "Ljava/io/File;");

            addClassSignature(new ConstSignature("minecraft"));
            addClassSignature(new ConstSignature("/assets/minecraft/"));

            addMemberMapper(new FieldMapper(file));

            addPatch(new MakeMemberPublicPatch(file));
        }
    }

    private class AbstractResourcePackMod extends ClassMod {
        AbstractResourcePackMod() {
            setInterfaces("ResourcePack");

            final FieldRef file = new FieldRef(getDeobfClass(), "file", "Ljava/io/File;");

            addClassSignature(new ConstSignature("assets"));
            addClassSignature(new ConstSignature("pack.mcmeta"));

            addMemberMapper(new FieldMapper(file));

            addPatch(new MakeMemberPublicPatch(file));
        }
    }

    private class FileResourcePackMod extends ClassMod {
        FileResourcePackMod() {
            setParentClass("AbstractResourcePack");

            final FieldRef zipFile = new FieldRef(getDeobfClass(), "zipFile", "Ljava/util/zip/ZipFile;");

            addClassSignature(new ConstSignature("assets/"));
            addClassSignature(new ConstSignature(new ClassRef("java/util/zip/ZipFile")));

            addMemberMapper(new FieldMapper(zipFile));

            addPatch(new MakeMemberPublicPatch(zipFile));
        }
    }

    private class FolderResourcePackMod extends ClassMod {
        FolderResourcePackMod() {
            setParentClass("AbstractResourcePack");

            addClassSignature(new ConstSignature("assets/"));
            addClassSignature(new ConstSignature(new ClassRef("java/io/FileInputStream")));
        }
    }

    private class ResourceManagerMod extends ClassMod {
        ResourceManagerMod() {
            boolean newMCMeta = getMinecraftVersion().compareTo("13w26a") >= 0;
            addClassSignature(new InterfaceSignature(
                newMCMeta ?
                    new InterfaceMethodRef(getDeobfClass(), "getNamespaces", "()Ljava/util/Set;") : null,
                new InterfaceMethodRef(getDeobfClass(), "getResource", "(LResourceLocation;)LResource;"),
                newMCMeta ?
                    new InterfaceMethodRef(getDeobfClass(), "getMCMeta", "(LResourceLocation;)Ljava/util/List;") : null
            ).setInterfaceOnly(true));
        }
    }

    private class ReloadableResourceManagerMod extends ClassMod {
        ReloadableResourceManagerMod() {
            setInterfaces("ResourceManager");

            addClassSignature(new InterfaceSignature(
                new InterfaceMethodRef(getDeobfClass(), "method1", "(Ljava/util/List;)V"),
                new InterfaceMethodRef(getDeobfClass(), "method2", "(LResourceManagerReloadListener;)V")
            ).setInterfaceOnly(true));
        }
    }

    private class SimpleReloadableResourceManagerMod extends ClassMod {
        SimpleReloadableResourceManagerMod() {
            setInterfaces("ReloadableResourceManager");

            final ClassRef fnfException = new ClassRef("java/io/FileNotFoundException");
            final FieldRef namespaceMap = new FieldRef(getDeobfClass(), "namespaceMap", "Ljava/util/Map;");
            final MethodRef fnfInit = new MethodRef("java/io/FileNotFoundException", "<init>", "(Ljava/lang/String;)V");
            final MethodRef getResource = new MethodRef(getDeobfClass(), "getResource", "(LResourceLocation;)LResource;");
            final MethodRef loadResources = new MethodRef(getDeobfClass(), "loadResources", "()V");
            final MethodRef addressToString = new MethodRef("ResourceLocation", "toString", "()Ljava/lang/String;");
            final InterfaceMethodRef mapClear = new InterfaceMethodRef("java/util/Map", "clear", "()V");
            final InterfaceMethodRef listIterator = new InterfaceMethodRef("java/util/List", "iterator", "()Ljava/util/Iterator;");

            addClassSignature(new ConstSignature(mapClear));

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

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        reference(INVOKEINTERFACE, listIterator)
                    );
                }
            }.setMethod(loadResources));

            addMemberMapper(new FieldMapper(namespaceMap));

            addPatch(new MakeMemberPublicPatch(namespaceMap));

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
                        push(0),
                        reference(INVOKESTATIC, beforeChange1)
                    );
                }
            }.targetMethod(loadResources));

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
                        push(0),
                        reference(INVOKESTATIC, afterChange1)
                    );
                }
            }
                .setInsertBefore(true)
                .targetMethod(loadResources)
            );
        }
    }

    private class FallbackResourceManagerMod extends ClassMod {
        FallbackResourceManagerMod() {
            setInterfaces("ResourceManager");

            final FieldRef resourcePacks = new FieldRef(getDeobfClass(), "resourcePacks", "Ljava/util/List;");

            addClassSignature(new ConstSignature(".mcmeta"));

            addMemberMapper(new FieldMapper(resourcePacks));

            addPatch(new MakeMemberPublicPatch(resourcePacks));
        }
    }
}
