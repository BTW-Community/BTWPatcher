package com.prupe.mcpatcher;

import javassist.bytecode.AccessFlag;

import static com.prupe.mcpatcher.BinaryRegex.*;
import static com.prupe.mcpatcher.BytecodeMatcher.*;
import static javassist.bytecode.Opcode.*;

public class BaseTilesheetMod extends Mod {
    public static final String NAME = "__TilesheetBase";

    protected BaseTilesheetMod() {
        name = NAME;
        author = "MCPatcher";
        description = "Internal mod required by the patcher.";
        version = "1.0";

        addDependency(BaseTexturePackMod.NAME);

        addClassMod(new BaseMod.IconMod());
        addClassMod(new RenderEngineMod());
        addClassMod(new TessellatorMod());
        addClassMod(new IconRegisterMod());
        addClassMod(new TextureMapMod());
        addClassMod(new TextureMod());
        addClassMod(new TextureManagerMod());
        addClassMod(new StitcherMod());
        addClassMod(new StitchHolderMod());

        addClassFile(MCPatcherUtils.TILE_LOADER_CLASS);
        addClassFile(MCPatcherUtils.TILE_LOADER_CLASS + "$1");
        addClassFile(MCPatcherUtils.TESSELLATOR_UTILS_CLASS);

        BaseTexturePackMod.earlyInitialize(1, MCPatcherUtils.TILE_LOADER_CLASS, "init");
    }

    @Override
    public String[] getLoggingCategories() {
        return new String[]{"Tilesheet"};
    }

    private class RenderEngineMod extends BaseMod.RenderEngineMod {
        RenderEngineMod() {
            final MethodRef updateAnimations = new MethodRef("TextureMap", "updateAnimations", "()V");

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "update ctm animations";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        reference(GETFIELD, textureMapBlocks),
                        reference(INVOKEVIRTUAL, updateAnimations)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.TILE_LOADER_CLASS, "updateAnimations", "()V"))
                    );
                }
            }
                .setInsertBefore(true)
                .targetMethod(updateDynamicTextures)
            );
        }
    }

    private class TessellatorMod extends BaseMod.TessellatorMod {
        TessellatorMod() {
            final MethodRef constructor = new MethodRef(getDeobfClass(), "<init>", "(I)V");
            final MethodRef reset = new MethodRef(getDeobfClass(), "reset", "()V");
            final FieldRef isDrawing = new FieldRef(getDeobfClass(), "isDrawing", "Z");
            final FieldRef drawMode = new FieldRef(getDeobfClass(), "drawMode", "I");
            final FieldRef textureMap = new FieldRef(getDeobfClass(), "textureMap", "LTextureMap;");
            final FieldRef bufferSize = new FieldRef(getDeobfClass(), "bufferSize", "I");
            final FieldRef addedVertices = new FieldRef(getDeobfClass(), "addedVertices", "I");
            final FieldRef vertexCount = new FieldRef(getDeobfClass(), "vertexCount", "I");
            final FieldRef rawBufferIndex = new FieldRef(getDeobfClass(), "rawBufferIndex", "I");
            final FieldRef children = new FieldRef(getDeobfClass(), "children", "Ljava/util/Map;");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        captureReference(GETFIELD),
                        push(4),
                        IREM,

                        any(0, 1000),

                        ALOAD_0,
                        DUP,
                        captureReference(GETFIELD),
                        ICONST_1,
                        IADD,
                        anyReference(PUTFIELD),

                        ALOAD_0,
                        DUP,
                        captureReference(GETFIELD),
                        push(8),
                        IADD,
                        anyReference(PUTFIELD)
                    );
                }
            }
                .setMethod(addVertex)
                .addXref(1, addedVertices)
                .addXref(2, vertexCount)
                .addXref(3, rawBufferIndex)
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

                        // GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureMap.getTexture().getGlTextureId());
                        push(3553), // GL11.GL_TEXTURE_2D
                        ALOAD_0,
                        reference(GETFIELD, textureMap),
                        reference(INVOKEVIRTUAL, new MethodRef("TextureMap", "getTexture", "()LTexture;")),
                        reference(INVOKEVIRTUAL, new MethodRef("Texture", "getGlTextureId", "()I")),
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

    private class TextureMapMod extends ClassMod {
        TextureMapMod() {
            setInterfaces("IconRegister");

            final FieldRef basePath = new FieldRef(getDeobfClass(), "basePath", "Ljava/lang/String;");
            final FieldRef textureExt = new FieldRef(getDeobfClass(), "textureExt", "Ljava/lang/String;");
            final FieldRef mapTexturesStitched = new FieldRef(getDeobfClass(), "mapTexturesStitched", "Ljava/util/HashMap;");
            final MethodRef refreshTextures = new MethodRef(getDeobfClass(), "refreshTextures", "()V");
            final MethodRef getTexture = new MethodRef(getDeobfClass(), "getTexture", "()LTexture;");
            final ClassRef sbClass = new ClassRef("java/lang/StringBuilder");
            final MethodRef strValueOf = new MethodRef("java/lang/String", "valueOf", "(Ljava/lang/Object;)Ljava/lang/String;");
            final MethodRef sbInit0 = new MethodRef("java/lang/StringBuilder", "<init>", "()V");
            final MethodRef sbInit1 = new MethodRef("java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V");
            final MethodRef sbAppend = new MethodRef("java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;");
            final MethodRef sbToString = new MethodRef("java/lang/StringBuilder", "toString", "()Ljava/lang/String;");
            final MethodRef addStitchHolder = new MethodRef("Stitcher", "addStitchHolder", "(LStitchHolder;)V");
            final MethodRef asList = new MethodRef("java/util/Arrays", "asList", "([Ljava/lang/Object;)Ljava/util/List;");
            final InterfaceMethodRef mapPut = new InterfaceMethodRef("java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
            final MethodRef hashMapPut = new MethodRef("java/util/HashMap", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
            final MethodRef doStitch = new MethodRef("Stitcher", "doStitch", "()V");

            addClassSignature(new ConstSignature("missingno"));
            addClassSignature(new ConstSignature(".png"));
            addClassSignature(new ConstSignature(".txt"));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        captureReference(INVOKESTATIC),
                        ALOAD_0,
                        anyReference(GETFIELD),
                        captureReference(INVOKEVIRTUAL),
                        anyASTORE
                    );
                }
            }
                .setMethod(refreshTextures)
                .addXref(1, new MethodRef("TextureManager", "getInstance", "()LTextureManager;"))
                .addXref(2, new MethodRef("TextureManager", "createStitcher", "(Ljava/lang/String;)Lnet/minecraft/src/Stitcher;"))
            );

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        ALOAD_2,
                        captureReference(PUTFIELD)
                    );
                }
            }
                .matchConstructorOnly(true)
                .addXref(1, basePath)
            );

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        ALOAD_3,
                        captureReference(PUTFIELD)
                    );
                }
            }
                .matchConstructorOnly(true)
                .addXref(1, textureExt)
            );

            addMemberMapper(new MethodMapper(getTexture));
            addMemberMapper(new FieldMapper(mapTexturesStitched));

            addPatch(new MakeMemberPublicPatch(mapTexturesStitched));

            addPatch(new BytecodePatch() {
                private int stitcherRegister;
                private int nameRegister;

                {
                    addPreMatchSignature(new BytecodeSignature() {
                        @Override
                        public String getMatchExpression() {
                            return buildExpression(
                                // stitcher.addStitchHolder(stitchHolder);
                                capture(anyALOAD),
                                anyALOAD,
                                reference(INVOKEVIRTUAL, addStitchHolder),

                                // map.put(stitchHolder, Arrays.asList(new Texture[]{texture}));
                                capture(anyALOAD),
                                anyALOAD,
                                push(1),
                                reference(ANEWARRAY, new ClassRef("Texture")),
                                DUP,
                                push(0),
                                anyALOAD,
                                AASTORE,
                                reference(INVOKESTATIC, asList),
                                or(
                                    build(reference(INVOKEINTERFACE, mapPut)),
                                    build(reference(INVOKEVIRTUAL, hashMapPut))
                                ),
                                POP
                            );
                        }

                        @Override
                        public boolean afterMatch() {
                            stitcherRegister = extractRegisterNum(getCaptureGroup(1));
                            nameRegister = extractRegisterNum(getCaptureGroup(2));
                            return true;
                        }
                    });
                }

                @Override
                public String getDescription() {
                    return "register additional tiles";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        anyALOAD,
                        reference(INVOKEVIRTUAL, doStitch)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // TileLoader.registerIcons(this, stitcher, mapName, map);
                        ALOAD_0,
                        ALOAD, stitcherRegister,
                        ALOAD_0,
                        reference(GETFIELD, basePath),
                        ALOAD, nameRegister,
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.TILE_LOADER_CLASS, "registerIcons", "(LTextureMap;LStitcher;Ljava/lang/String;Ljava/util/Map;)V"))
                    );
                }
            }
                .setInsertBefore(true)
                .targetMethod(refreshTextures)
            );

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "register ctm animation txt files";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // this.textureExt + name + extension
                        reference(NEW, sbClass),
                        DUP,
                        or(
                            build( // vanilla mc
                                reference(INVOKESPECIAL, sbInit0),
                                ALOAD_0,
                                reference(GETFIELD, textureExt),
                                reference(INVOKEVIRTUAL, sbAppend)
                            ),
                            build( // mcp
                                ALOAD_0,
                                reference(GETFIELD, textureExt),
                                optional(build(reference(INVOKESTATIC, strValueOf))), // useless, but added by mcp
                                reference(INVOKESPECIAL, sbInit1)
                            )
                        ),
                        capture(any(1, 5)),
                        reference(INVOKEVIRTUAL, sbAppend),
                        capture(build(push(".txt"))),
                        reference(INVOKEVIRTUAL, sbAppend),
                        reference(INVOKEVIRTUAL, sbToString)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // TileLoader.getOverridePath(this.textureExt, name, extension)
                        ALOAD_0,
                        reference(GETFIELD, textureExt),
                        getCaptureGroup(1),
                        getCaptureGroup(2),
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.TILE_LOADER_CLASS, "getOverridePath", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;"))
                    );
                }
            }
                .targetMethod(refreshTextures)
            );
        }
    }

    private class TextureMod extends BaseMod.TextureMod {
        TextureMod() {
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

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override texture name";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        ALOAD_1,
                        captureReference(PUTFIELD)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        ALOAD_0,
                        ALOAD_1,
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.TILE_LOADER_CLASS, "getOverrideTextureName", "(Ljava/lang/String;)Ljava/lang/String;")),
                        getCaptureGroup(1)
                    );
                }
            }.matchConstructorOnly(true));
        }
    }

    private class TextureManagerMod extends ClassMod {
        TextureManagerMod() {
            final MethodRef getInstance = new MethodRef(getDeobfClass(), "getInstance", "()LTextureManager;");
            final MethodRef createTextureFromImage = new MethodRef(getDeobfClass(), "createTextureFromImage", "(Ljava/lang/String;IIIIIIIZLjava/awt/image/BufferedImage;)LTexture;");
            final MethodRef createTextureFromFile = new MethodRef(getDeobfClass(), "createTextureFromFile", "(Ljava/lang/String;)Ljava/util/List;");

            addClassSignature(new ConstSignature("/"));
            addClassSignature(new ConstSignature(".txt"));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        captureReference(INVOKESTATIC),
                        ISTORE_2,
                        captureReference(NEW),
                        DUP,
                        ALOAD_1,
                        ILOAD_2,
                        ILOAD_2,
                        or(
                            build( // 13w02a
                                reference(INVOKESTATIC, new MethodRef("org/lwjgl/opengl/GLContext", "getCapabilities", "()Lorg/lwjgl/opengl/ContextCapabilities;")),
                                reference(GETFIELD, new FieldRef("org/lwjgl/opengl/ContextCapabilities", "GL_ARB_texture_non_power_of_two", "Z"))
                            ),
                            build(push(1)) // 13w02b
                        ),
                        anyReference(INVOKESPECIAL),
                        ARETURN
                    );
                }
            }
                .setMethod(new MethodRef(getDeobfClass(), "createStitcher", "(Ljava/lang/String;)LStitcher;"))
                .addXref(1, new MethodRef("Minecraft", "getMaxTextureSize", "()I"))
                .addXref(2, new ClassRef("Stitcher"))
            );

            addMemberMapper(new MethodMapper(getInstance)
                .accessFlag(AccessFlag.PUBLIC, true)
                .accessFlag(AccessFlag.STATIC, true)
            );
            addMemberMapper(new MethodMapper(createTextureFromImage));
            addMemberMapper(new MethodMapper(createTextureFromFile));
        }
    }

    private class StitcherMod extends ClassMod {
        StitcherMod() {
            final MethodRef ceilPowerOf2 = new MethodRef(getDeobfClass(), "ceilPowerOf2", "(I)I");
            final MethodRef addStitchHolder = new MethodRef(getDeobfClass(), "addStitchHolder", "(LStitchHolder;)V");
            final MethodRef getTexture = new MethodRef(getDeobfClass(), "getTexture", "()LTexture;");
            final MethodRef doStitch = new MethodRef(getDeobfClass(), "doStitch", "()V");
            final MethodRef arraySort = new MethodRef("java/util/Arrays", "sort", "([Ljava/lang/Object;)V");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 1; i <= 16; i <<= 1) {
                        sb.append(buildExpression(
                            ILOAD_2,
                            ILOAD_2,
                            push(i),
                            ISHR,
                            IOR,
                            ISTORE_2
                        ));
                    }
                    return sb.toString();
                }
            }.setMethod(ceilPowerOf2));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        reference(INVOKESTATIC, arraySort)
                    );
                }
            }.setMethod(doStitch));

            addMemberMapper(new MethodMapper(addStitchHolder));
            addMemberMapper(new MethodMapper(getTexture));
        }
    }

    private class StitchHolderMod extends ClassMod {
        StitchHolderMod() {
            setInterfaces("java/lang/Comparable");

            final MethodRef min = new MethodRef("java/lang/Math", "min", "(II)I");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // (float)par1 / (float)Math.min(this.width, this.height)
                        ILOAD_1,
                        I2F,
                        ALOAD_0,
                        anyReference(GETFIELD),
                        ALOAD_0,
                        anyReference(GETFIELD),
                        reference(INVOKESTATIC, min),
                        I2F,
                        FDIV
                    );
                }
            }.setMethod(new MethodRef(getDeobfClass(), "setNewDimension", "(I)V")));
        }
    }
}
