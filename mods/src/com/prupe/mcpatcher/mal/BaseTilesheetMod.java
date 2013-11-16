package com.prupe.mcpatcher.mal;

import com.prupe.mcpatcher.*;
import javassist.bytecode.AccessFlag;

import static com.prupe.mcpatcher.BinaryRegex.*;
import static com.prupe.mcpatcher.BytecodeMatcher.*;
import static javassist.bytecode.Opcode.*;

public class BaseTilesheetMod extends Mod {
    public BaseTilesheetMod() {
        name = MCPatcherUtils.BASE_TILESHEET_MOD;
        author = "MCPatcher";
        description = "Internal mod required by the patcher.";
        version = "2.2";

        addDependency(MCPatcherUtils.BASE_TEXTURE_PACK_MOD);

        addClassMod(new BaseMod.IconMod(this));
        addClassMod(new BaseMod.ResourceLocationMod(this));
        addClassMod(new BaseMod.AbstractTextureMod(this));
        addClassMod(new BaseMod.TextureMod(this));
        addClassMod(new TessellatorMod());
        addClassMod(new IconRegisterMod());
        addClassMod(new TextureAtlasMod());
        addClassMod(new TextureManagerMod());
        addClassMod(new TextureAtlasSpriteMod());

        addClassFile(MCPatcherUtils.TILE_LOADER_CLASS);
        addClassFile(MCPatcherUtils.TILE_LOADER_CLASS + "$1");
        addClassFile(MCPatcherUtils.TESSELLATOR_UTILS_CLASS);

        BaseTexturePackMod.earlyInitialize(1, MCPatcherUtils.TILE_LOADER_CLASS, "init");
    }

    @Override
    public String[] getLoggingCategories() {
        return new String[]{"Tilesheet"};
    }

    private class TessellatorMod extends BaseMod.TessellatorMod {
        TessellatorMod() {
            super(BaseTilesheetMod.this);

            final MethodRef constructor = new MethodRef(getDeobfClass(), "<init>", "(I)V");
            final MethodRef reset = new MethodRef(getDeobfClass(), "reset", "()V");
            final FieldRef isDrawing = new FieldRef(getDeobfClass(), "isDrawing", "Z");
            final FieldRef drawMode = new FieldRef(getDeobfClass(), "drawMode", "I");
            final FieldRef textureMap = new FieldRef(getDeobfClass(), "textureMap", "LTextureAtlas;");
            final FieldRef bufferSize = new FieldRef(getDeobfClass(), "bufferSize", "I");
            final FieldRef addedVertices = new FieldRef(getDeobfClass(), "addedVertices", "I");
            final FieldRef vertexCount = new FieldRef(getDeobfClass(), "vertexCount", "I");
            final FieldRef rawBufferIndex = new FieldRef(getDeobfClass(), "rawBufferIndex", "I");
            final FieldRef children = new FieldRef(getDeobfClass(), "children", "Ljava/util/Map;");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // if (this.addedVertices % 4 == 0 ...)
                        ALOAD_0,
                        captureReference(GETFIELD),
                        push(4),
                        IREM,
                        IFNE_or_IFEQ, any(2)
                    );
                }
            }
                .setMethod(addVertex)
                .addXref(1, addedVertices)
            );

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // this.vertexCount++;
                        ALOAD_0,
                        DUP,
                        capture(build(GETFIELD, capture(any(2)))),
                        push(1),
                        IADD,
                        PUTFIELD, backReference(2)
                    );
                }
            }
                .setMethod(addVertex)
                .addXref(1, vertexCount)
            );

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // this.rawBufferIndex += 8;
                        ALOAD_0,
                        DUP,
                        capture(build(GETFIELD, capture(any(2)))),
                        push(8),
                        IADD,
                        PUTFIELD, backReference(2)
                    );
                }
            }
                .setMethod(addVertex)
                .addXref(1, rawBufferIndex)
            );

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        begin(),
                        ALOAD_0,
                        captureReference(GETFIELD),

                        any(0, 50),

                        push("Already tesselating!"),
                        any(0, 100),

                        ALOAD_0,
                        captureReference(INVOKESPECIAL),

                        ALOAD_0,
                        ILOAD_1,
                        captureReference(PUTFIELD)
                    );
                }
            }
                .setMethod(startDrawing)
                .addXref(1, isDrawing)
                .addXref(2, reset)
                .addXref(3, drawMode)
            );

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        begin(),
                        ALOAD_0,
                        push(7),
                        captureReference(INVOKEVIRTUAL),
                        RETURN,
                        end()
                    );
                }
            }
                .setMethod(startDrawingQuads)
                .addXref(1, startDrawing)
            );

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        ILOAD_1,
                        captureReference(PUTFIELD)
                    );
                }
            }
                .matchConstructorOnly(true)
                .addXref(1, bufferSize)
            );

            addMemberMapper(new FieldMapper(instance).accessFlag(AccessFlag.STATIC, true));

            for (JavaRef ref : new JavaRef[]{constructor, startDrawing, isDrawing, drawMode, draw, reset, bufferSize,
                addedVertices, vertexCount, rawBufferIndex}) {
                addPatch(new MakeMemberPublicPatch(ref));
            }

            addPatch(new AddFieldPatch(textureMap));
            addPatch(new AddFieldPatch(children));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "initialize new fields";
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
                        // children = new WeakHashMap();
                        ALOAD_0,
                        reference(NEW, new ClassRef("java/util/WeakHashMap")),
                        DUP,
                        reference(INVOKESPECIAL, new MethodRef("java/util/WeakHashMap", "<init>", "()V")),
                        reference(PUTFIELD, children),

                        // TessellatorUtils.haveBufferSize = true / false;
                        push(getMethodInfo().getDescriptor().contains("()") ? 0 : 1),
                        reference(PUTSTATIC, new FieldRef(MCPatcherUtils.TESSELLATOR_UTILS_CLASS, "haveBufferSize", "Z"))
                    );
                }
            }
                .setInsertBefore(true)
                .matchConstructorOnly(true)
            );

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "bind texture before drawing";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        or(
                            build(ALOAD_0, anyReference(GETFIELD)),
                            anyReference(GETSTATIC)
                        ),
                        reference(INVOKEVIRTUAL, new MethodRef("java/nio/IntBuffer", "clear", "()Ljava/nio/Buffer;")),
                        POP
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // if (textureMap != null) {
                        ALOAD_0,
                        reference(GETFIELD, textureMap),
                        IFNULL, branch("A"),

                        // GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureMap.getGlTextureId());
                        push(3553), // GL11.GL_TEXTURE_2D
                        ALOAD_0,
                        reference(GETFIELD, textureMap),
                        reference(INVOKEVIRTUAL, new MethodRef("TextureAtlas", "getGlTextureId", "()I")),
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.GL11_CLASS, "glBindTexture", "(II)V")),

                        // }
                        label("A")
                    );
                }
            }
                .setInsertBefore(true)
                .targetMethod(draw)
            );

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "reset children";
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
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.TESSELLATOR_UTILS_CLASS, "resetChildren", "(LTessellator;)V"))
                    );
                }
            }
                .setInsertBefore(true)
                .targetMethod(reset)
            );

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "draw children";
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
                        ALOAD_0,
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.TESSELLATOR_UTILS_CLASS, "drawChildren", "(ILTessellator;)I")),
                        ISTORE_1
                    );
                }
            }.targetMethod(draw));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "startDrawing children";
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
                        ILOAD_1,
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.TESSELLATOR_UTILS_CLASS, "startDrawingChildren", "(LTessellator;I)V"))
                    );
                }
            }
                .setInsertBefore(true)
                .targetMethod(startDrawing)
            );
        }
    }

    private class IconRegisterMod extends ClassMod {
        IconRegisterMod() {
            final InterfaceMethodRef registerIcon = new InterfaceMethodRef(getDeobfClass(), "registerIcon", "(Ljava/lang/String;)LIcon;");

            addClassSignature(new InterfaceSignature(
                registerIcon
            ).setInterfaceOnly(true));
        }
    }

    private class TextureAtlasMod extends BaseMod.TextureAtlasMod {
        TextureAtlasMod() {
            super(BaseTilesheetMod.this);

            final FieldRef blocksAtlas = new FieldRef(getDeobfClass(), "blocksAtlas", "LResourceLocation;");
            final FieldRef itemsAtlas = new FieldRef(getDeobfClass(), "itemssAtlas", "LResourceLocation;");
            final MethodRef registerTiles = new MethodRef(getDeobfClass(), "registerTiles", "()V");
            final InterfaceMethodRef mapClear = new InterfaceMethodRef("java/util/Map", "clear", "()V");
            final InterfaceMethodRef mapEntrySet = new InterfaceMethodRef("java/util/Map", "entrySet", "()Ljava/util/Set;");
            final InterfaceMethodRef setIterator = new InterfaceMethodRef("java/util/Set", "iterator", "()Ljava/util/Iterator;");
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
            final MethodRef readImage = new MethodRef("javax/imageio/ImageIO", "read", "(Ljava/io/InputStream;)Ljava/awt/image/BufferedImage;");

            addClassSignature(new BaseMod.ResourceLocationSignature(this, blocksAtlas, "textures/atlas/blocks.png"));
            addClassSignature(new BaseMod.ResourceLocationSignature(this, itemsAtlas, "textures/atlas/items.png"));

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
                        // this.mapTextures.entrySet().iterator()
                        ALOAD_0,
                        reference(GETFIELD, texturesByName),
                        reference(INVOKEINTERFACE, mapEntrySet),
                        reference(INVOKEINTERFACE, setIterator),
                        anyASTORE
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
                .setInsertBefore(true)
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

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override tile image";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // ImageIO.read(texturePack.getResourceAsStream(path))
                        anyALOAD,
                        capture(anyALOAD),
                        anyReference(INVOKEINTERFACE),
                        reference(INVOKESTATIC, readImage)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // TileLoader.getOverrideImage(path)
                        getCaptureGroup(1),
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.TILE_LOADER_CLASS, "getOverrideImage", "(Ljava/lang/String;)Ljava/awt/image/BufferedImage;"))
                    );
                }
            });

            if (getMinecraftVersion().compareTo("13w41a") >= 0) {
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

    private class TextureAtlasSpriteMod extends BaseMod.TextureAtlasSpriteMod {
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
