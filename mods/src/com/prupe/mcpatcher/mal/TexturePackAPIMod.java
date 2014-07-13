package com.prupe.mcpatcher.mal;

import com.prupe.mcpatcher.*;
import com.prupe.mcpatcher.basemod.*;
import javassist.bytecode.AccessFlag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.prupe.mcpatcher.BinaryRegex.*;
import static com.prupe.mcpatcher.BytecodeMatcher.*;
import static javassist.bytecode.Opcode.*;

public class TexturePackAPIMod extends Mod {
    private static final List<EarlyInitEntry> earlyInitMethods = new ArrayList<EarlyInitEntry>();

    private final int malVersion;

    protected final MethodRef earlyInitialize = new MethodRef(MCPatcherUtils.TEXTURE_PACK_CHANGE_HANDLER_CLASS, "earlyInitialize", "(Ljava/lang/String;Ljava/lang/String;)V");
    protected final MethodRef checkForTexturePackChange = new MethodRef(MCPatcherUtils.TEXTURE_PACK_CHANGE_HANDLER_CLASS, "checkForTexturePackChange", "()V");
    protected final MethodRef beforeChange1 = new MethodRef(MCPatcherUtils.TEXTURE_PACK_CHANGE_HANDLER_CLASS, "beforeChange1", "()V");
    protected final MethodRef afterChange1 = new MethodRef(MCPatcherUtils.TEXTURE_PACK_CHANGE_HANDLER_CLASS, "afterChange1", "()V");

    public TexturePackAPIMod() {
        name = MCPatcherUtils.TEXTURE_PACK_API_MOD;
        author = "MCPatcher";
        description = "Internal mod required by the patcher.";
        version = "4.3";

        malVersion = RenderUtilsMod.select(ResourceLocationMod.select(1, 2), 3);
        setMALVersion("texturepack", malVersion);

        addClassMod(new MinecraftMod());
        if (malVersion == 1) {
            addClassMod(new RenderEngineMod());
            addClassMod(new ResourcePackRepositoryMod());
            if (getMinecraftVersion().compareTo("1.5.2") == 0) {
                addClassMod(new RunnableTitleScreenMod());
            }
        } else {
            addClassMod(new TextureManagerMod());
            addClassMod(new TextureUtilMod(this));
            addClassMod(new AbstractTextureMod());
            addClassMod(new ThreadDownloadImageDataMod());
            addClassMod(new SimpleTextureMod(this));
            addClassMod(new TextureAtlasMod(this));
            addClassMod(new DynamicTextureMod(this));
            addClassMod(new ResourceManagerMod());
            addClassMod(new ReloadableResourceManagerMod());
            addClassMod(new SimpleReloadableResourceManagerMod());
            addClassMod(new FallbackResourceManagerMod());
            addClassMod(new ResourceMod(this));
        }
        addClassMod(new ResourcePackMod());
        addClassMod(new DefaultResourcePackMod());
        addClassMod(new AbstractResourcePackMod());
        addClassMod(new FileResourcePackMod());
        addClassMod(new FolderResourcePackMod());
        ResourceLocationMod.setup(this);
        RenderUtilsMod.setup(this);

        addClassFiles("com.prupe.mcpatcher.mal.resource.*");
        addClassFiles("com.prupe.mcpatcher.mal.util.*");

        getClassMap().addInheritance("ResourceLocation", MCPatcherUtils.RESOURCE_LOCATION_WITH_SOURCE_CLASS);
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

    private class MinecraftMod extends com.prupe.mcpatcher.basemod.MinecraftMod {
        MinecraftMod() {
            super(TexturePackAPIMod.this);

            final ClassRef textureResourceManagerClass = new ClassRef("SimpleReloadableResourceManager");
            final MethodRef getTextureManager = new MethodRef(getDeobfClass(), "getTextureManager", "()LTextureManager;");
            final MethodRef getResourceManager = new MethodRef(getDeobfClass(), "getResourceManager", "()LResourceManager;");
            final FieldRef texturePackList = new FieldRef(getDeobfClass(), "texturePackList", "LResourcePackRepository;");
            final FieldRef renderEngine = new FieldRef(getDeobfClass(), "renderEngine", "LRenderEngine;");
            final MethodRef refreshTextureMaps = new MethodRef("RenderEngine", "refreshTextureMaps", "()V");
            final MethodRef startGame = new MethodRef(getDeobfClass(), "startGame", "()V");
            final MethodRef runGameLoop = new MethodRef(getDeobfClass(), "runGameLoop", "()V");
            final MethodRef setTitle = new MethodRef("org/lwjgl/opengl/Display", "setTitle", "(Ljava/lang/String;)V");
            final MethodRef isCloseRequested = new MethodRef("org/lwjgl/opengl/Display", "isCloseRequested", "()Z");
            final MethodRef glViewport = new MethodRef(MCPatcherUtils.GL11_CLASS, "glViewport", "(IIII)V");
            final MethodRef imageIORead = new MethodRef("javax/imageio/ImageIO", "read", "(Ljava/io/InputStream;)Ljava/awt/image/BufferedImage;");
            final InterfaceMethodRef getResource = new InterfaceMethodRef("ResourceManager", "getResource", "(LResourceLocation;)LResource;");
            final InterfaceMethodRef getResourceInputStream = new InterfaceMethodRef("Resource", "getInputStream", "()Ljava/io/InputStream;");

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

            if (malVersion == 1) {
                addMemberMapper(new FieldMapper(texturePackList));
                addMemberMapper(new FieldMapper(renderEngine));
            } else {
                addMemberMapper(new MethodMapper(getResourceManager));
                addMemberMapper(new MethodMapper(getTextureManager));
            }

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "init texture pack handlers on startup";
                }

                @Override
                public String getMatchExpression() {
                    return malVersion == 1 ? getMatchExpression1() : getMatchExpression2();
                }

                private String getMatchExpression1() {
                    return buildExpression(
                        // this.renderEngine.refreshTextureMaps();
                        ALOAD_0,
                        reference(GETFIELD, renderEngine),
                        reference(INVOKEVIRTUAL, refreshTextureMaps)
                    );
                }

                private String getMatchExpression2() {
                    return buildExpression(
                        capture(build(
                            // this.resourceManager = new TextureResourceManager(...);
                            ALOAD_0,
                            reference(NEW, textureResourceManagerClass),
                            DUP,
                            nonGreedy(any(0, 12)),
                            anyReference(PUTFIELD),

                            // ...
                            nonGreedy(any(0, 60)),

                            // this.refreshResources();
                            ALOAD_0,
                            anyReference(INVOKEVIRTUAL)
                        )),

                        capture(build(
                            // ...
                            any(0, 700),

                            // GL11.glViewport(...);
                            reference(INVOKESTATIC, glViewport)
                        ))
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
                        malVersion == 1 ? getReplacementBytes1() : getReplacementBytes2()
                    );
                }

                private byte[] getReplacementBytes1() {
                    return buildCode(
                        reference(INVOKESTATIC, beforeChange1),
                        getMatch(),
                        reference(INVOKESTATIC, afterChange1)
                    );
                }

                private byte[] getReplacementBytes2() {
                    return buildCode(
                        getCaptureGroup(1),
                        reference(INVOKESTATIC, beforeChange1),
                        getCaptureGroup(2),
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

            if (getMinecraftVersion().compareTo("1.7.2") > 0) {
                final FieldRef mojangPng = new FieldRef(getDeobfClass(), "mojangPng", "LResourceLocation;");

                addClassSignature(new ResourceLocationSignature(this, mojangPng, "textures/gui/title/mojang.png"));

                addPatch(new BytecodePatch() {
                    @Override
                    public String getDescription() {
                        return "read startup logo from texture pack";
                    }

                    @Override
                    public String getMatchExpression() {
                        return buildExpression(
                            // ImageIO.read(this.defaultTexturePack.getInputStream(Minecraft.mojangPng));
                            ALOAD_0,
                            anyReference(GETFIELD),
                            reference(GETSTATIC, mojangPng),
                            anyReference(INVOKEVIRTUAL),
                            lookAhead(build(
                                any(0, 20),
                                reference(INVOKESTATIC, imageIORead)
                            ), true)
                        );
                    }

                    @Override
                    public byte[] getReplacementBytes() {
                        return buildCode(
                            // ImageIO.read(this.getResourceManager().getResource(Minecraft.mojangPng).getInputStream());
                            ALOAD_0,
                            reference(INVOKEVIRTUAL, getResourceManager),
                            reference(GETSTATIC, mojangPng),
                            reference(INVOKEINTERFACE, getResource),
                            reference(INVOKEINTERFACE, getResourceInputStream)
                        );
                    }
                });
            }
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

    private class AbstractTextureMod extends com.prupe.mcpatcher.basemod.AbstractTextureMod {
        AbstractTextureMod() {
            super(TexturePackAPIMod.this);

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

    private class ThreadDownloadImageDataMod extends ClassMod {
        ThreadDownloadImageDataMod() {
            setParentClass(getMinecraftVersion().compareTo("13w41a") < 0 ? "AbstractTexture" : "SimpleTexture");

            final MethodRef setDaemon = new MethodRef("java/lang/Thread", "setDaemon", "(Z)V");
            final MethodRef startThread = new MethodRef("java/lang/Thread", "start", "()V");

            addClassSignature(new ConstSignature(setDaemon));
            addClassSignature(new ConstSignature(startThread));
            addClassSignature(new OrSignature(
                new ConstSignature("Skin downloader: "),
                new ConstSignature("Texture Downloader #")
            ));
        }
    }

    private class ResourcePackMod extends ClassMod {
        ResourcePackMod() {
            String d = ResourceLocationMod.getDescriptor();
            List<InterfaceMethodRef> tmp = new ArrayList<InterfaceMethodRef>();
            if (malVersion == 1) {
                tmp.add(new InterfaceMethodRef(getDeobfClass(), "deleteTexturePack", "(LRenderEngine;)V"));
                tmp.add(new InterfaceMethodRef(getDeobfClass(), "bindThumbnailTexture", "(LRenderEngine;)V"));
                tmp.add(new InterfaceMethodRef(getDeobfClass(), "getInputStream2", "(" + d + "Z)Ljava/io/InputStream;"));
                tmp.add(new InterfaceMethodRef(getDeobfClass(), "getInputStream", "(" + d + ")Ljava/io/InputStream;"));
                tmp.add(new InterfaceMethodRef(getDeobfClass(), "getTexturePackID", "()Ljava/lang/String;"));
                tmp.add(new InterfaceMethodRef(getDeobfClass(), "getName", "()Ljava/lang/String;"));
                tmp.add(new InterfaceMethodRef(getDeobfClass(), "getFirstDescriptionLine", "()Ljava/lang/String;"));
                tmp.add(new InterfaceMethodRef(getDeobfClass(), "getSecondDescriptionLine", "()Ljava/lang/String;"));
                tmp.add(new InterfaceMethodRef(getDeobfClass(), "hasResource", "(Ljava/lang/String;Z)Z"));
                if (getMinecraftVersion().compareTo("1.5") < 0) {
                    tmp.add(new InterfaceMethodRef(getDeobfClass(), "getTexturePackResolution", "()I"));
                }
                tmp.add(new InterfaceMethodRef(getDeobfClass(), "isCompatible", "()Z"));
            } else {
                String nsType = getMinecraftVersion().compareTo("13w25c") >= 0 ? "Set" : "List";
                boolean newMCMeta = getMinecraftVersion().compareTo("13w26a") >= 0;
                tmp.add(new InterfaceMethodRef(getDeobfClass(), "getInputStream", "(" + d + ")Ljava/io/InputStream;"));
                tmp.add(new InterfaceMethodRef(getDeobfClass(), "hasResource", "(" + d + ")Z"));
                tmp.add(new InterfaceMethodRef(getDeobfClass(), "getNamespaces", "()Ljava/util/" + nsType + ";"));
                if (newMCMeta) {
                    tmp.add(new InterfaceMethodRef(getDeobfClass(), "getMCMeta", "(LMetadataSectionSerializer;Ljava/lang/String;)LMCMeta;"));
                } else {
                    tmp.add(new InterfaceMethodRef(getDeobfClass(), "getPackInfo", "(LMetadataSectionSerializer;)LPackMetadataSection;"));
                }
                tmp.add(new InterfaceMethodRef(getDeobfClass(), "getPackIcon", "()Ljava/awt/image/BufferedImage;"));
                if (newMCMeta) {
                    tmp.add(new InterfaceMethodRef(getDeobfClass(), "getName", "()Ljava/lang/String;"));
                }
            }
            addClassSignature(new InterfaceSignature(tmp.toArray(new InterfaceMethodRef[tmp.size()])).setInterfaceOnly(true));
        }
    }

    private class DefaultResourcePackMod extends ClassMod {
        DefaultResourcePackMod() {
            if (malVersion == 1) {
                setup15();
            } else {
                setup16();
            }
        }

        private void setup15() {
            setParentClass("AbstractResourcePack");

            addClassSignature(new ConstSignature("The default look of Minecraft"));
        }

        private void setup16() {
            final FieldRef map = new FieldRef(getDeobfClass(), "map", "Ljava/util/Map;");

            addClassSignature(new ConstSignature("minecraft"));
            addClassSignature(new OrSignature(
                new ConstSignature("/assets/minecraft/"), // pre-realms
                new ConstSignature("/assets/")            // 1.7.10-pre4+
            ));

            addMemberMapper(new FieldMapper(map));

            addPatch(new MakeMemberPublicPatch(map));
        }
    }

    private class AbstractResourcePackMod extends ClassMod {
        AbstractResourcePackMod() {
            setInterfaces("ResourcePack");

            final FieldRef file = new FieldRef(getDeobfClass(), "file", "Ljava/io/File;");

            if (malVersion == 1) {
                addClassSignature(new ConstSignature("/pack.txt"));
            } else {
                addClassSignature(new ConstSignature("assets"));
                addClassSignature(new ConstSignature("pack.mcmeta"));
            }

            addMemberMapper(new FieldMapper(file));

            addPatch(new MakeMemberPublicPatch(file));
        }
    }

    private class FileResourcePackMod extends ClassMod {
        FileResourcePackMod() {
            setParentClass("AbstractResourcePack");

            final FieldRef zipFile = new FieldRef(getDeobfClass(), "zipFile", "Ljava/util/zip/ZipFile;");

            if (malVersion == 1) {
                addClassSignature(new ConstSignature("textures/"));
            } else {
                addClassSignature(new ConstSignature("assets/"));
            }
            addClassSignature(new ConstSignature(new MethodRef("java/util/zip/ZipFile", "getEntry", "(Ljava/lang/String;)Ljava/util/zip/ZipEntry;")));
            addClassSignature(new ConstSignature(new MethodRef("java/util/zip/ZipFile", "close", "()V")));

            addMemberMapper(new FieldMapper(zipFile));

            addPatch(new MakeMemberPublicPatch(zipFile));
        }
    }

    private class FolderResourcePackMod extends ClassMod {
        FolderResourcePackMod() {
            setParentClass("AbstractResourcePack");

            if (malVersion == 1) {
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

            } else {
                addClassSignature(new ConstSignature("assets/"));
                addClassSignature(new ConstSignature(new ClassRef("java/io/FileInputStream")));
            }
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
                {
                    setMethod(getResource);
                    addXref(1, addressToString);
                }

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
            });

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
                        reference(INVOKESTATIC, beforeChange1)
                    );
                }
            }.targetMethod(loadResources));

            addPatch(new BytecodePatch() {
                {
                    setInsertBefore(true);
                    targetMethod(loadResources);
                }

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
            });
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

    private class RenderEngineMod extends com.prupe.mcpatcher.basemod.RenderEngineMod {
        public RenderEngineMod() {
            super(TexturePackAPIMod.this);

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
            final MethodRef getIntBuffer = new MethodRef(MCPatcherUtils.FAKE_RESOURCE_LOCATION_CLASS, "getIntBuffer", "(Ljava/nio/IntBuffer;[I)Ljava/nio/IntBuffer;");
            final MethodRef glDeleteTextures1 = new MethodRef(MCPatcherUtils.GL11_CLASS, "glDeleteTextures", "(Ljava/nio/IntBuffer;)V");
            final MethodRef glDeleteTextures2 = new MethodRef(MCPatcherUtils.GL11_CLASS, "glDeleteTextures", "(I)V");
            final MethodRef glBindTexture = new MethodRef(MCPatcherUtils.GL11_CLASS, "glBindTexture", "(II)V");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(or(
                        build(reference(INVOKESTATIC, glDeleteTextures1)),
                        build(reference(INVOKESTATIC, glDeleteTextures2))
                    ));
                }
            }.setMethod(deleteTexture));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        reference(INVOKESTATIC, glBindTexture)
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
            addPatch(new MakeMemberPublicPatch(getTexture));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "imageData.clear(), .put(), .limit() -> imageData = TexturePackAPI.getIntBuffer()";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // this.imageData.clear();
                        ALOAD_0,
                        reference(GETFIELD, imageData),
                        reference(INVOKEVIRTUAL, clear),
                        POP,

                        // this.imageData.put($1);
                        ALOAD_0,
                        reference(GETFIELD, imageData),
                        capture(any(1, 5)),
                        reference(INVOKEVIRTUAL, put),
                        POP,

                        // this.imageData.position(0).limit($1.length);
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
                        // this.imageData = TexturePackAPI.getIntBuffer(this.imageData, $1);
                        ALOAD_0,
                        ALOAD_0,
                        reference(GETFIELD, imageData),
                        getCaptureGroup(1),
                        reference(INVOKESTATIC, getIntBuffer),
                        reference(PUTFIELD, imageData)
                    );
                }
            });

            addNullCheckPatch(setupTextureExt, 1, new int[]{RETURN});
            addNullCheckPatch(getImageContents, 1, new int[]{ALOAD_2, ARETURN});
            addNullCheckPatch(readTextureImage, 1, new int[]{ACONST_NULL, ARETURN});

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
                {
                    setInsertBefore(true);
                    targetMethod(refreshTextures);
                }

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
            });
        }

        private void addNullCheckPatch(final MethodRef method, final int register, final int[] returnValue) {
            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "null check in " + method.getName();
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
                        registerLoadStore(ALOAD, register),
                        IFNONNULL, branch("A"),
                        returnValue,
                        label("A")
                    );
                }
            }.targetMethod(method));
        }
    }

    private class ResourcePackRepositoryMod extends ClassMod {
        ResourcePackRepositoryMod() {
            final FieldRef selectedTexturePack = new FieldRef(getDeobfClass(), "selectedTexturePack", "LResourcePack;");
            final FieldRef mc = new FieldRef(getDeobfClass(), "mc", "LMinecraft;");
            final MethodRef getSelectedTexturePack = new MethodRef(getDeobfClass(), "getSelectedTexturePack", "()LResourcePack;");
            final MethodRef setTexturePack = new MethodRef(getDeobfClass(), "setTexturePack", "(LResourcePack;)Z");
            final MethodRef updateAvailableTexturePacks = new MethodRef(getDeobfClass(), "updateAvailableTexturePacks", "()V");
            final MethodRef onDownloadFinished = new MethodRef(getDeobfClass(), "onDownloadFinished", "()V");
            final MethodRef scheduleTexturePackRefresh = new MethodRef("Minecraft", "scheduleTexturePackRefresh", "()V");

            addClassSignature(new ConstSignature(".zip"));
            addClassSignature(new ConstSignature("texturepacks"));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // this.availableTexturePacks.removeAll();
                        ALOAD_0,
                        captureReference(GETFIELD),
                        ALOAD_1,
                        reference(INVOKEINTERFACE, new InterfaceMethodRef("java/util/List", "removeAll", "(Ljava/util/Collection;)Z")),
                        POP
                    );
                }
            }.setMethod(updateAvailableTexturePacks));

            addClassSignature(new BytecodeSignature() {
                {
                    setMethod(onDownloadFinished);
                    addXref(1, mc);
                    addXref(2, scheduleTexturePackRefresh);
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // this.mc.scheduleTexturePackRefresh();
                        ALOAD_0,
                        captureReference(GETFIELD),
                        captureReference(INVOKEVIRTUAL)
                    );
                }
            });

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

    private class RunnableTitleScreenMod extends ClassMod {
        RunnableTitleScreenMod() {
            setInterfaces("java/lang/Runnable");

            addClassSignature(new ConstSignature("http://assets.minecraft.net/1_6_has_been_released.flag"));

            final MethodRef run = new MethodRef(getDeobfClass(), "run", "()V");

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "disable 1.6 notification message";
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
                        RETURN
                    );
                }
            }.targetMethod(run));
        }
    }
}
