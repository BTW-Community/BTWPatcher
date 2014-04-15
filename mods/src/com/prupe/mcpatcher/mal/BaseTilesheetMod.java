package com.prupe.mcpatcher.mal;

import com.prupe.mcpatcher.*;
import com.prupe.mcpatcher.basemod.*;

import static com.prupe.mcpatcher.BinaryRegex.*;
import static com.prupe.mcpatcher.BytecodeMatcher.*;
import static javassist.bytecode.Opcode.*;

public class BaseTilesheetMod extends Mod {
    public BaseTilesheetMod() {
        name = MCPatcherUtils.BASE_TILESHEET_MOD;
        author = "MCPatcher";
        description = "Internal mod required by the patcher.";
        version = "2.3";

        addDependency(MCPatcherUtils.BASE_TEXTURE_PACK_MOD);

        addClassMod(new IconMod(this));
        ResourceLocationMod.setup(this);
        addClassMod(new AbstractTextureMod(this));
        addClassMod(new TextureMod(this));
        addClassMod(new TessellatorMod(this));
        addClassMod(new IconRegisterMod());
        addClassMod(new TextureAtlasMod());
        addClassMod(new TextureManagerMod());
        addClassMod(new TextureAtlasSpriteMod());

        addClassFiles("com.prupe.mcpatcher.mal.tile.*");

        BaseTexturePackMod.earlyInitialize(1, MCPatcherUtils.TILE_LOADER_CLASS, "init");
    }

    @Override
    public String[] getLoggingCategories() {
        return new String[]{"Tilesheet"};
    }

    private class IconRegisterMod extends ClassMod {
        IconRegisterMod() {
            final InterfaceMethodRef registerIcon = new InterfaceMethodRef(getDeobfClass(), "registerIcon", "(Ljava/lang/String;)LIcon;");

            addClassSignature(new InterfaceSignature(
                registerIcon
            ).setInterfaceOnly(true));
        }
    }

    private class TextureAtlasMod extends com.prupe.mcpatcher.basemod.TextureAtlasMod {
        TextureAtlasMod() {
            super(BaseTilesheetMod.this);

            final FieldRef blocksAtlas = new FieldRef(getDeobfClass(), "blocksAtlas", "LResourceLocation;");
            final FieldRef itemsAtlas = new FieldRef(getDeobfClass(), "itemssAtlas", "LResourceLocation;");
            final MethodRef registerTiles = new MethodRef(getDeobfClass(), "registerTiles", "()V");
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

            addClassSignature(new ResourceLocationSignature(this, blocksAtlas, "textures/atlas/blocks.png"));
            addClassSignature(new ResourceLocationSignature(this, itemsAtlas, "textures/atlas/items.png"));

            addClassSignature(new BytecodeSignature() {
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
            }
                .setMethod(refreshTextures2)
                .addXref(1, new MethodRef("Minecraft", "getMaxTextureSize", "()I"))
                .addXref(2, new ClassRef("Stitcher"))
            );

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        begin(),
                        ALOAD_0,
                        captureReference(GETFIELD),
                        reference(INVOKEINTERFACE, mapClear)
                    );
                }
            }
                .setMethod(registerTiles)
                .addXref(1, texturesByName)
            );

            addPatch(new BytecodePatch() {
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
                        ALOAD_0,
                        reference(INVOKESPECIAL, registerTiles),

                        // registerIcons(this, this.mapName, this.mapTextures);
                        ALOAD_0,
                        ALOAD_0,
                        reference(GETFIELD, basePath),
                        ALOAD_0,
                        reference(GETFIELD, texturesByName),
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.TILE_LOADER_CLASS, "registerIcons", "(LTextureAtlas;Ljava/lang/String;Ljava/util/Map;)V"))
                    );
                }
            }
                .setInsertAfter(true)
                .targetMethod(refreshTextures2)
            );

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
                        // this.basePath + name + extension
                        reference(NEW, sbClass),
                        DUP,
                        or(
                            build( // vanilla mc
                                reference(INVOKESPECIAL, sbInit0),
                                ALOAD_0,
                                reference(GETFIELD, basePath),
                                reference(INVOKEVIRTUAL, sbAppend)
                            ),
                            build( // mcp
                                ALOAD_0,
                                reference(GETFIELD, basePath),
                                optional(build(reference(INVOKESTATIC, stringValueOf))), // useless, but added by mcp
                                reference(INVOKESPECIAL, sbInit1)
                            )
                        ),
                        capture(any(1, 5)),
                        reference(INVOKEVIRTUAL, sbAppend),
                        capture(anyLDC),
                        reference(INVOKEVIRTUAL, sbAppend),
                        reference(INVOKEVIRTUAL, sbToString)
                    );
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
                    byte[] name = getCaptureGroup(1);
                    byte[] extension = getCaptureGroup(2);
                    if (name == null) {
                        name = getCaptureGroup(3);
                    }
                    if (extension == null) {
                        extension = getCaptureGroup(4);
                    }
                    return buildCode(
                        // TileLoader.getOverridePath(this.basePath, name, extension)
                        ALOAD_0,
                        reference(GETFIELD, basePath),
                        name,
                        extension,
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.TILE_LOADER_CLASS, "getOverridePath", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;"))
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
            final MethodRef updateAnimations = new MethodRef(getDeobfClass(), "updateAnimations", "()V");
            final InterfaceMethodRef listIterator = new InterfaceMethodRef("java/util/List", "iterator", "()Ljava/util/Iterator;");

            addClassSignature(new ConstSignature("dynamic/%s_%d"));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        begin(),
                        ALOAD_0,
                        captureReference(GETFIELD),
                        reference(INVOKEINTERFACE, listIterator)

                    );
                }
            }
                .setMethod(updateAnimations)
                .addXref(1, new FieldRef(getDeobfClass(), "animations", "Ljava/util/List;"))
            );
        }
    }

    private class TextureAtlasSpriteMod extends com.prupe.mcpatcher.basemod.TextureAtlasSpriteMod {
        TextureAtlasSpriteMod() {
            super(BaseTilesheetMod.this);

            final InterfaceMethodRef listSize = new InterfaceMethodRef("java/util/List", "size", "()I");

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
