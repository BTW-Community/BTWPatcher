package com.prupe.mcpatcher.mal;

import com.prupe.mcpatcher.*;
import com.prupe.mcpatcher.basemod.*;
import javassist.bytecode.AccessFlag;

import static com.prupe.mcpatcher.BinaryRegex.*;
import static com.prupe.mcpatcher.BytecodeMatcher.*;
import static javassist.bytecode.Opcode.*;

public class TilesheetAPIMod extends Mod {
    public TilesheetAPIMod() {
        name = MCPatcherUtils.TILESHEET_API_MOD;
        author = "MCPatcher";
        description = "Internal mod required by the patcher.";
        version = "2.4";

        addDependency(MCPatcherUtils.TEXTURE_PACK_API_MOD);
        addDependency(MCPatcherUtils.TESSELLATOR_API_MOD);

        addClassMod(new MinecraftMod(this));
        addClassMod(new IconMod(this));
        if (ResourceLocationMod.setup(this)) {
            addClassMod(new AbstractTextureMod(this));
            addClassMod(new TextureMod(this));
        }
        addClassMod(new TextureManagerMod());
        addClassMod(new TessellatorMod(this));
        addClassMod(new TextureAtlasMod());
        addClassMod(new TextureAtlasSpriteMod());

        addClassFiles("com.prupe.mcpatcher.mal.tile.*");

        TexturePackAPIMod.earlyInitialize(1, MCPatcherUtils.TILE_LOADER_CLASS, "init");
    }

    @Override
    public String[] getLoggingCategories() {
        return new String[]{"Tilesheet"};
    }

    private class TextureAtlasMod extends com.prupe.mcpatcher.basemod.TextureAtlasMod {
        TextureAtlasMod() {
            super(TilesheetAPIMod.this);

            final FieldRef blocksAtlas = new FieldRef(getDeobfClass(), "blocksAtlas", "LResourceLocation;");
            final FieldRef itemsAtlas = new FieldRef(getDeobfClass(), "itemssAtlas", "LResourceLocation;");
            final MethodRef registerTiles = new MethodRef(getDeobfClass(), "registerTiles", IconMod.haveClass() ? "()V" : "(LResourceManager;LUnknownInterface1;)V");
            final InterfaceMethodRef mapClear = new InterfaceMethodRef("java/util/Map", "clear", "()V");
            final ClassRef sbClass = new ClassRef("java/lang/StringBuilder");
            final ClassRef objClass = new ClassRef("java/lang/Object");
            final MethodRef stringValueOf = new MethodRef("java/lang/String", "valueOf", "(Ljava/lang/Object;)Ljava/lang/String;");
            final MethodRef stringFormat = new MethodRef("java/lang/String", "format", "(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;");
            final MethodRef stringIndexOf = new MethodRef("java/lang/String", "indexOf", "(I)I");
            final MethodRef stringEquals = new MethodRef("java/lang/String", "equals", "(Ljava/lang/Object;)Z");
            final MethodRef sbInit0 = new MethodRef("java/lang/StringBuilder", "<init>", "()V");
            final MethodRef sbInit1 = new MethodRef("java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V");
            final MethodRef sbAppend = new MethodRef("java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;");
            final MethodRef sbToString = new MethodRef("java/lang/StringBuilder", "toString", "()Ljava/lang/String;");
            final MethodRef getOverridePath = new MethodRef(MCPatcherUtils.TILE_LOADER_CLASS, "getOverridePath", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;");

            if (ResourceLocationMod.haveClass()) {
                addClassSignature(new ResourceLocationSignature(this, blocksAtlas, "textures/atlas/blocks.png"));
                // In 14w25a, blocks and items are stored on a single atlas.
                // Let itemsAtlas be an alias for blocksAtlas in this case.
                addClassSignature(new ResourceLocationSignature(this, itemsAtlas, "textures/atlas/" + (IconMod.haveClass() ? "items" : "blocks") + ".png"));

                addClassSignature(new BytecodeSignature() {
                    {
                        setMethod(refreshTextures2);
                        addXref(1, new MethodRef("Minecraft", "getMaxTextureSize", "()I"));
                        addXref(2, new ClassRef("Stitcher"));
                    }

                    @Override
                    public String getMatchExpression() {
                        return buildExpression(
                            // i = Minecraft.getMaxTextureSize();
                            begin(),
                            captureReference(INVOKESTATIC),
                            ISTORE_2,

                            // stitcher = new Stitcher(i, i, true, ...);
                            captureReference(NEW),
                            DUP,
                            ILOAD_2,
                            ILOAD_2,
                            push(1),
                            any(0, 6),
                            anyReference(INVOKESPECIAL),
                            ASTORE_3
                        );
                    }
                });
            }

            addMemberMapper(new MethodMapper(registerIcon));

            addClassSignature(new BytecodeSignature() {
                {
                    setMethod(registerTiles);
                    addXref(1, texturesByName);
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        begin(),
                        ALOAD_0,
                        captureReference(GETFIELD),
                        reference(INVOKEINTERFACE, mapClear)
                    );
                }
            });

            addPatch(new BytecodePatch() {
                {
                    setInsertAfter(true);
                    targetMethod(ResourceLocationMod.select(refreshTextures1, refreshTextures2));
                }

                @Override
                public String getDescription() {
                    return "register additional tiles";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // this.list.clear();
                        ALOAD_0,
                        anyReference(GETFIELD),
                        reference(INVOKEINTERFACE, new InterfaceMethodRef("java/util/List", "clear", "()V"))
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // this.registerTiles();
                        ResourceLocationMod.haveClass() && IconMod.haveClass() ?
                            buildCode(
                                ALOAD_0,
                                reference(INVOKESPECIAL, registerTiles)
                            ) : new byte[0],

                        // registerIcons(this, this.mapName, this.mapTextures);
                        ALOAD_0,
                        ALOAD_0,
                        reference(GETFIELD, basePath),
                        ALOAD_0,
                        reference(GETFIELD, texturesByName),
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.TILE_LOADER_CLASS, "registerIcons", "(LTextureAtlas;Ljava/lang/String;Ljava/util/Map;)V"))
                    );
                }
            });

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override tile texture paths";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(or(
                        getMatchExpressionSB(),
                        getMatchExpressionSprintf()
                    ));
                }

                private String getMatchExpressionSB() {
                    return build(
                        // ("/" +) this.basePath + name + extension
                        reference(NEW, sbClass),
                        DUP,
                        or(
                            getSBExpression(
                                ResourceLocationMod.select(capture(build(push("/"))), null),
                                build(ALOAD_0, reference(GETFIELD, basePath)),
                                capture(any(1, 5)),
                                capture(any(1, 5))
                            ),
                            getSBExpression(
                                build(ALOAD_0, reference(GETFIELD, basePath)),
                                capture(any(1, 5)),
                                capture(any(1, 5))
                            )
                        ),
                        reference(INVOKEVIRTUAL, sbToString)
                    );
                }

                private String getSBExpression(String... subexprs) {
                    StringBuilder sb = new StringBuilder();
                    boolean first = true;
                    for (String subexpr : subexprs) {
                        if (subexpr == null) {
                            continue;
                        }
                        if (first) {
                            sb.append(or(
                                build(
                                    // vanilla mc:
                                    // sb = new StringBuilder();
                                    // sb.append(value);
                                    reference(INVOKESPECIAL, sbInit0),
                                    subexpr,
                                    reference(INVOKEVIRTUAL, sbAppend)
                                ),
                                build(
                                    // mcp:
                                    // sb = new StringBuilder(value);
                                    subexpr,
                                    optional(build(reference(INVOKESTATIC, stringValueOf))), // useless, but added by mcp
                                    reference(INVOKESPECIAL, sbInit1)
                                )
                            ));
                        } else {
                            sb.append(build(
                                // sb.append(value);
                                subexpr,
                                reference(INVOKEVIRTUAL, sbAppend)
                            ));
                        }
                        first = false;
                    }
                    return sb.toString();
                }

                private String getMatchExpressionSprintf() {
                    return build(
                        // String.format("%s/%s%s", this.basePath, name, extension)
                        push("%s/%s%s"),
                        push(3),
                        reference(ANEWARRAY, objClass),
                        DUP,
                        push(0),
                        ALOAD_0,
                        reference(GETFIELD, basePath),
                        AASTORE,
                        DUP,
                        push(1),
                        capture(any(1, 5)),
                        AASTORE,
                        DUP,
                        push(2),
                        capture(anyLDC),
                        AASTORE,
                        reference(INVOKESTATIC, stringFormat)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    int group = 1;
                    byte[] prefix = null;
                    if (!ResourceLocationMod.haveClass()) {
                        prefix = getCaptureGroup(group);
                        group++;
                    }
                    if (prefix == null) {
                        prefix = buildCode(push(""));
                    }
                    byte[] name = null;
                    byte[] extension = null;
                    for (; name == null && group < 6; group += 2) {
                        name = getCaptureGroup(group);
                        extension = getCaptureGroup(group + 1);
                    }
                    return buildCode(
                        // TileLoader.getOverridePath(prefix, this.basePath, name, extension)
                        prefix,
                        ALOAD_0,
                        reference(GETFIELD, basePath),
                        name,
                        extension,
                        reference(INVOKESTATIC, getOverridePath)
                    );
                }
            });

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "check for clock/compass textures";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        capture(or(
                            build(push("clock")),
                            build(push("compass"))
                        )),
                        ALOAD_1,
                        reference(INVOKEVIRTUAL, stringEquals)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        ALOAD_0,
                        ALOAD_1,
                        getCaptureGroup(1),
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.TILE_LOADER_CLASS, "isSpecialTexture", "(LTextureAtlas;Ljava/lang/String;Ljava/lang/String;)Z"))
                    );
                }
            }.targetMethod(registerIcon));

            if (getMinecraftVersion().compareTo("13w41a") >= 0 && getMinecraftVersion().compareTo("14w02a") < 0) {
                addPatch(new BytecodePatch() {
                    @Override
                    public String getDescription() {
                        return "allow slashes in texture names";
                    }

                    @Override
                    public String getMatchExpression() {
                        return buildExpression(
                            // if (name.indexOf('/') != -1 ...)
                            ALOAD_1,
                            push(47),
                            reference(INVOKEVIRTUAL, stringIndexOf),
                            push(-1),
                            IF_ICMPNE, any(2)
                        );
                    }

                    @Override
                    public byte[] getReplacementBytes() {
                        return buildCode(
                            // *burp*
                        );
                    }
                });
            }
        }
    }

    private class TextureManagerMod extends ClassMod {
        TextureManagerMod() {
            if (ResourceLocationMod.haveClass()) {
                setup16();
            } else {
                setup15();
            }
        }

        private void setup15() {
            final MethodRef getBasename = new MethodRef(getDeobfClass(), "getBasename", "(Ljava/lang/String;)Ljava/lang/String;");
            final MethodRef lastIndexOf = new MethodRef("java/lang/String", "lastIndexOf", "(I)I");
            final MethodRef getOverrideBasename = new MethodRef(MCPatcherUtils.TILE_LOADER_CLASS, "getOverrideBasename", "(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/String;");

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
            }.setMethod(getBasename));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override texture name";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        reference(INVOKESPECIAL, getBasename)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        reference(INVOKESTATIC, getOverrideBasename)
                    );
                }
            });
        }

        private void setup16() {
            final MethodRef updateAnimations = new MethodRef(getDeobfClass(), "updateAnimations", "()V");
            final InterfaceMethodRef listIterator = new InterfaceMethodRef("java/util/List", "iterator", "()Ljava/util/Iterator;");

            addClassSignature(new ConstSignature("dynamic/%s_%d"));

            addClassSignature(new BytecodeSignature() {
                {
                    setMethod(updateAnimations);
                    addXref(1, new FieldRef(getDeobfClass(), "animations", "Ljava/util/List;"));
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        begin(),
                        ALOAD_0,
                        captureReference(GETFIELD),
                        reference(INVOKEINTERFACE, listIterator)

                    );
                }
            });
        }
    }

    private class TextureAtlasSpriteMod extends com.prupe.mcpatcher.basemod.TextureAtlasSpriteMod {
        TextureAtlasSpriteMod() {
            super(TilesheetAPIMod.this);

            if (ResourceLocationMod.haveClass()) {
                setup16();
            }
        }

        private void setup16() {
            final InterfaceMethodRef listSize = new InterfaceMethodRef("java/util/List", "size", "()I");

            addMemberMapper(new MethodMapper(getX0, getY0)
                    .accessFlag(AccessFlag.PUBLIC, true)
                    .accessFlag(AccessFlag.STATIC, false)
            );

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "check for null animation data";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        begin(),
                        ALOAD_0,
                        captureReference(GETFIELD),
                        reference(INVOKEINTERFACE, listSize),
                        IRETURN
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // if (this.animationData != null)
                        ALOAD_0,
                        getCaptureGroup(1),
                        ASTORE_1,
                        ALOAD_1,
                        IFNULL, branch("A"),

                        // return this.animationData.size();
                        ALOAD_1,
                        reference(INVOKEINTERFACE, listSize),
                        IRETURN,

                        // }
                        label("A"),

                        // return 1;
                        push(1),
                        IRETURN
                    );
                }
            });
        }
    }
}
