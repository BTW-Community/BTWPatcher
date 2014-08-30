package com.prupe.mcpatcher.mod;

import com.prupe.mcpatcher.*;
import com.prupe.mcpatcher.basemod.*;
import com.prupe.mcpatcher.basemod.ext18.*;
import com.prupe.mcpatcher.mal.TexturePackAPIMod;

import java.util.HashMap;
import java.util.Map;

import static com.prupe.mcpatcher.BinaryRegex.*;
import static com.prupe.mcpatcher.BytecodeMatcher.*;
import static javassist.bytecode.Opcode.*;

public class CustomTexturesModels extends Mod {
    static final MethodRef blockColorMultiplier = new MethodRef("Block", "colorMultiplier", "(LIBlockAccess;LPosition;I)I");

    static final MethodRef getCCInstance = new MethodRef(MCPatcherUtils.COLORIZE_BLOCK18_CLASS, "getInstance", "()L" + MCPatcherUtils.COLORIZE_BLOCK18_CLASS + ";");
    static final MethodRef newUseColormap = new MethodRef(MCPatcherUtils.COLORIZE_BLOCK18_CLASS, "useColormap", "(LModelFace;)Z");
    static final MethodRef newColorMultiplier = new MethodRef(MCPatcherUtils.COLORIZE_BLOCK18_CLASS, "colorMultiplier", "(I)I");
    static final MethodRef newVertexColor = new MethodRef(MCPatcherUtils.COLORIZE_BLOCK18_CLASS, "getVertexColor", "(FII)F");
    static final MethodRef newModelFace = new MethodRef(MCPatcherUtils.COLORIZE_BLOCK18_CLASS, "getModelFace", "(LModelFace;)LModelFace;");

    private static final Map<String, Integer> ccInfoMap = new HashMap<String, Integer>();

    public CustomTexturesModels() {
        name = MCPatcherUtils.CUSTOM_TEXTURES_MODELS;
        author = "MCPatcher";
        description = "Allows custom block and item rendering.";
        version = "1.0";

        addDependency(MCPatcherUtils.TEXTURE_PACK_API_MOD);
        addDependency(MCPatcherUtils.TILESHEET_API_MOD);
        addDependency(MCPatcherUtils.BLOCK_API_MOD);
        addDependency(MCPatcherUtils.ITEM_API_MOD);
        addDependency(MCPatcherUtils.NBT_API_MOD);
        addDependency(MCPatcherUtils.BIOME_API_MOD);

        ResourceLocationMod.setup(this);
        addClassMod(new TextureAtlasSpriteMod(this));
        addClassMod(new IconMod(this));
        addClassMod(new IBlockAccessMod(this));
        addClassMod(new IBlockStateMod(this));
        addClassMod(new TessellatorMod(this));
        addClassMod(new TessellatorFactoryMod(this));
        addClassMod(new BiomeGenBaseMod(this));
        PositionMod.setup(this);
        addClassMod(new DirectionWithAOMod(this));
        addClassMod(new IModelMod(this));
        addClassMod(new ModelFaceMod(this));

        addClassMod(new BlockMod());
        addClassMod(new ModelFaceSpriteMod());
        addClassMod(new ModelFaceFactoryMod());
        addClassMod(new RenderBlockCustomMod());
        addClassMod(new RenderBlockCustomInnerMod());
        addClassMod(new RenderBlockFluidMod());

        addClassMod(new ItemMod(this));

        addClassFiles("com.prupe.mcpatcher.colormap.*");
        addClassFiles("com.prupe.mcpatcher.ctm.*");
        addClassFiles("com.prupe.mcpatcher.cit.*");
        addClassFiles(MCPatcherUtils.COLORIZE_BLOCK_CLASS + "*");
        addClassFiles(MCPatcherUtils.COLORIZE_BLOCK18_CLASS + "*");

        TexturePackAPIMod.earlyInitialize(2, MCPatcherUtils.CTM_UTILS18_CLASS, "reset");
        TexturePackAPIMod.earlyInitialize(2, MCPatcherUtils.COLORIZE_BLOCK18_CLASS, "reset");
    }

    @Override
    public String[] getLoggingCategories() {
        return new String[]{
            MCPatcherUtils.CONNECTED_TEXTURES,
            MCPatcherUtils.CUSTOM_ITEM_TEXTURES
        };
    }

    private class BlockMod extends com.prupe.mcpatcher.basemod.BlockMod {
        BlockMod() {
            super(CustomTexturesModels.this);

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // return 3;
                        begin(),
                        push(3),
                        IRETURN,
                        end()
                    );
                }
            }.setMethod(getRenderType));

            addMemberMapper(new MethodMapper(blockColorMultiplier));
        }
    }

    private class ModelFaceSpriteMod extends com.prupe.mcpatcher.basemod.ext18.ModelFaceSpriteMod {
        ModelFaceSpriteMod() {
            super(CustomTexturesModels.this);

            addPatch(new MakeMemberPublicPatch(sprite));
        }
    }

    private class ModelFaceFactoryMod extends ClassMod {
        ModelFaceFactoryMod() {
            addClassSignature(new ConstSignature(0.39269908169872414));
            addClassSignature(new ConstSignature(0.7853981633974483));
            addClassSignature(new ConstSignature(0.017453292519943295));

            final MethodRef createFace = new MethodRef(getDeobfClass(), "createFace", "(Ljavax/vecmath/Vector3f;Ljavax/vecmath/Vector3f;LUnknownClass_clt;LTextureAtlasSprite;LDirection;LUnknownEnum_cxa;LUnknownClass_clv;ZZ)LModelFace;");
            final MethodRef registerModelFaceSprite = new MethodRef(MCPatcherUtils.COLORIZE_BLOCK18_CLASS, "registerModelFaceSprite", "(LModelFace;LTextureAtlasSprite;)LModelFace;");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // return new ModelFace(...);
                        anyReference(NEW),
                        DUP,
                        anyALOAD,
                        ALOAD_3,
                        anyReference(GETFIELD),
                        anyALOAD,
                        anyReference(INVOKESPECIAL),
                        ARETURN,
                        end()
                    );
                }
            }.setMethod(createFace));

            addPatch(new BytecodePatch() {
                {
                    setInsertBefore(true);
                    targetMethod(createFace);
                }

                @Override
                public String getDescription() {
                    return "register model face sprites";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // return ...;
                        ARETURN
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // return ColorizeBlock18.registerModelFaceSprite(..., sprite);
                        ALOAD, 4,
                        reference(INVOKESTATIC, registerModelFaceSprite)
                    );
                }
            });
        }
    }

    private String getCCKey(PatchComponent patchComponent) {
        return patchComponent.getClassFile().getName() + ":" + patchComponent.getMethodInfo().toString();
    }

    private void initCCInfo(ClassMod classMod, final MethodRef... methods) {
        ccInfoMap.clear();
        classMod.addPatch(new BytecodePatch(classMod) {
            {
                targetMethod(methods);
            }

            @Override
            public String getDescription() {
                return "set render info";
            }

            @Override
            public String getMatchExpression() {
                return buildExpression(
                    begin()
                );
            }

            @Override
            public byte[] getReplacementBytes() {
                int register = getMethodInfo().getCodeAttribute().getMaxLocals();
                ccInfoMap.put(getCCKey(this), register);
                return buildCode(
                    reference(INVOKESTATIC, getCCInstance),
                    registerLoadStore(ASTORE, register)
                );
            }
        });
    }

    private byte[] getCCInfo(PatchComponent patchComponent) {
        String key = getCCKey(patchComponent);
        Integer register = ccInfoMap.get(key);
        if (register == null) {
            for (Map.Entry<String, Integer> entry : ccInfoMap.entrySet()) {
                Logger.log(Logger.LOG_MAIN, "  %s -> %s", entry.getKey(), entry.getValue());
            }
            throw new IllegalStateException("no ccInfo for [" + key + "]");
        }
        return registerLoadStore(ALOAD, register);
    }

    private class RenderBlockCustomMod extends ClassMod {
        private final MethodRef renderBlock = new MethodRef(getDeobfClass(), "renderBlock", "(LIBlockAccess;LIModel;LIBlockState;LPosition;LTessellator;Z)Z");
        private final MethodRef renderBlockAO = new MethodRef(getDeobfClass(), "renderBlockAO", "(LIBlockAccess;LIModel;LBlock;LPosition;LTessellator;Z)Z");
        private final MethodRef renderBlockNonAO = new MethodRef(getDeobfClass(), "renderBlockNonAO", "(LIBlockAccess;LIModel;LBlock;LPosition;LTessellator;Z)Z");
        private final MethodRef renderFaceAO = new MethodRef(getDeobfClass(), "renderFaceAO", "(LIBlockAccess;LBlock;LPosition;LTessellator;Ljava/util/List;[FLjava/util/BitSet;LRenderBlockCustomInner;)V");
        private final MethodRef renderFaceNonAO = new MethodRef(getDeobfClass(), "renderFaceNonAO", "(LIBlockAccess;LBlock;LPosition;LDirection;IZLTessellator;Ljava/util/List;Ljava/util/BitSet;)V");

        RenderBlockCustomMod() {
            addClassSignature(new ConstSignature(0xf000f));

            addClassSignature(new BytecodeSignature() {
                {
                    setMethod(renderBlock);
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push("Using AO")
                    );
                }
            });

            addClassSignature(new BytecodeSignature() {
                {
                    setMethod(renderBlockAO);
                    addXref(1, renderFaceAO);
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(0xf000f),

                        any(0, 500),

                        // this.renderFaceAO(...);
                        ALOAD_0,
                        ALOAD_1,
                        ALOAD_3,
                        ALOAD, 4,
                        ALOAD, 5,
                        anyALOAD,
                        anyALOAD,
                        anyALOAD,
                        anyALOAD,
                        captureReference(INVOKESPECIAL)
                    );
                }
            });

            addClassSignature(new BytecodeSignature() {
                {
                    setMethod(renderBlockNonAO);
                    addXref(1, renderFaceNonAO);
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // this.renderFaceNonAO(...)
                        ALOAD_0,
                        ALOAD_1,
                        ALOAD_3,
                        ALOAD, 4,
                        ACONST_NULL,
                        push(-1),
                        push(1),
                        ALOAD, 5,
                        anyALOAD,
                        anyALOAD,
                        captureReference(INVOKESPECIAL)
                    );
                }
            });

            setupColorMaps();
            setupCTM();
        }

        private void setupPreRender(final MethodRef method) {
            addPatch(new BytecodePatch() {
                {
                    setInsertAfter(true);
                    targetMethod(renderBlock);
                }

                @Override
                public String getDescription() {
                    return "set up " + method.getClassName().replaceFirst("^.*\\.", "") + " for render";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_3,
                        reference(INVOKEINTERFACE, IBlockStateMod.getBlock),
                        capture(anyASTORE)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    int register = extractRegisterNum(getCaptureGroup(1));
                    return buildCode(
                        // if (!ClassName.getInstance().preRender(blockAccess, model, blockState, position, block, useAO)) {
                        reference(INVOKESTATIC, method),
                        ALOAD_1,
                        ALOAD_2,
                        ALOAD_3,
                        ALOAD, 4,
                        registerLoadStore(ALOAD, register),
                        registerLoadStore(ILOAD, register - 1),
                        reference(INVOKEVIRTUAL, new MethodRef(method.getClassName(), "preRender", "(LIBlockAccess;LIModel;LIBlockState;LPosition;LBlock;Z)Z")),
                        IFNE, branch("A"),

                        // return false;
                        push(false),
                        IRETURN,

                        // }
                        label("A")
                    );
                }
            });
        }

        private void setupColorMaps() {
            final MethodRef useColormap = new MethodRef("ModelFace", "useColormap", "()Z");
            final MethodRef setDirection = new MethodRef(MCPatcherUtils.COLORIZE_BLOCK18_CLASS, "setDirection", "(LDirection;)V");

            setupPreRender(getCCInstance);

            initCCInfo(this, renderBlockAO, renderBlockNonAO, renderFaceAO, renderFaceNonAO);

            addPatch(new BytecodePatch() {
                {
                    setInsertBefore(true);
                    targetMethod(renderBlockAO, renderBlockNonAO);
                }

                @Override
                public String getDescription() {
                    return "set render direction";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // model.getFaces(direction)
                        ALOAD_2,
                        capture(anyALOAD),
                        reference(INVOKEINTERFACE, IModelMod.getFaces)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // colorizeBlock18.setDirection(direction);
                        getCCInfo(this),
                        getCaptureGroup(1),
                        reference(INVOKEVIRTUAL, setDirection)
                    );
                }
            });

            addPatch(new BytecodePatch() {
                {
                    setInsertBefore(true);
                    targetMethod(renderBlockAO, renderBlockNonAO);
                }

                @Override
                public String getDescription() {
                    return "clear render direction";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // model.getDefaultFaces()
                        ALOAD_2,
                        reference(INVOKEINTERFACE, IModelMod.getDefaultFaces)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // colorizeBlock18.setDirection(null);
                        getCCInfo(this),
                        push(null),
                        reference(INVOKEVIRTUAL, setDirection)
                    );
                }
            });

            addPatch(new BytecodePatch() {
                {
                    targetMethod(renderFaceAO, renderFaceNonAO);
                }

                @Override
                public String getDescription() {
                    return "set colormap flag";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // face.useColormap();
                        capture(anyALOAD),
                        reference(INVOKEVIRTUAL, useColormap)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // colorizeBlock18.useColormap(face)
                        getCCInfo(this),
                        getCaptureGroup(1),
                        reference(INVOKEVIRTUAL, newUseColormap)
                    );
                }
            });

            addPatch(new BytecodePatch() {
                {
                    setInsertAfter(true);
                    targetMethod(renderFaceAO, renderFaceNonAO);
                }

                @Override
                public String getDescription() {
                    return "override color multiplier";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // color = block.colorMultiplier(blockAccess, position, index);
                        reference(INVOKEVIRTUAL, blockColorMultiplier),
                        capture(anyISTORE)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // color = colorizeBlock18.colorMultiplier(color)
                        getCCInfo(this),
                        flipLoadStore(getCaptureGroup(1)),
                        reference(INVOKEVIRTUAL, newColorMultiplier),
                        getCaptureGroup(1)
                    );
                }
            });

            addPatch(new BytecodePatch() {
                {
                    targetMethod(renderFaceAO);
                }

                @Override
                public String getDescription() {
                    return "set up smooth vertex colors";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // RenderBlockCustomInner.getVertexColor(inner)[0,1,2,3] * color
                        lookBehind(build(
                            ALOAD, 8,
                            anyReference(INVOKESTATIC),
                            any(),
                            FALOAD
                        ), true),
                        capture(anyFLOAD),
                        FMUL
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    // RenderBlockCustomInner.getVertexColor(inner)[0,1,2,3] * colorizeBlock18.getVertexColor(color, count / 3, count % 3)
                    return buildCode(
                        getCCInfo(this),
                        getCaptureGroup(1),
                        push(getMethodMatchCount() / 3),
                        push(getMethodMatchCount() % 3),
                        reference(INVOKEVIRTUAL, newVertexColor),
                        FMUL
                    );
                }
            });
        }

        private void setupCTM() {
            addPatch(new BytecodePatch() {
                private final InterfaceMethodRef iteratorNext = new InterfaceMethodRef("java/util/Iterator", "next", "()Ljava/lang/Object;");
                private final ClassRef modelFaceClass = new ClassRef("ModelFace");

                {
                    setInsertAfter(true);
                    targetMethod(renderFaceAO, renderFaceNonAO);
                }

                @Override
                public String getDescription() {
                    return "override texture";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // face = (ModelFace) iterator.next();
                        anyALOAD,
                        reference(INVOKEINTERFACE, iteratorNext),
                        reference(CHECKCAST, modelFaceClass),
                        capture(anyASTORE)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // face = colorizeBlock18.newModelFace(face);
                        getCCInfo(this),
                        flipLoadStore(getCaptureGroup(1)),
                        reference(INVOKEVIRTUAL, newModelFace),
                        getCaptureGroup(1)
                    );
                }
            });
        }
    }

    private class RenderBlockCustomInnerMod extends ClassMod {
        public RenderBlockCustomInnerMod() {
            final MethodRef mixAOBrightness = new MethodRef(getDeobfClass(), "mixAOBrightness", "(IIII)I");
            final MethodRef computeVertexColors = new MethodRef(getDeobfClass(), "computeVertexColors", "(LIBlockAccess;LBlock;LPosition;LDirection;[FLjava/util/BitSet;)V");
            final FieldRef renderBlocks = new FieldRef(getDeobfClass(), "renderBlocks", "LRenderBlockCustom;");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // (var24 + var21 + var30 + var38) / 4.0f
                        anyFLOAD,
                        anyFLOAD,
                        FADD,
                        anyFLOAD,
                        FADD,
                        anyFLOAD,
                        FADD,
                        push(0.25f),
                        FMUL
                    );
                }
            }.setMethod(computeVertexColors));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // return ((var1 + var2 + var3 + var4) >> 2) & 0xff00ff;
                        ILOAD_1,
                        ILOAD_2,
                        IADD,
                        ILOAD_3,
                        IADD,
                        ILOAD, 4,
                        IADD,
                        push(2),
                        ISHR,
                        push(0xff00ff),
                        IAND,
                        IRETURN,
                        end()
                    );
                }
            }.setMethod(mixAOBrightness));

            addMemberMapper(new FieldMapper(renderBlocks));
        }
    }

    private class RenderBlockFluidMod extends ClassMod {
        private final MethodRef renderBlock = new MethodRef(getDeobfClass(), "renderBlock", "(LIBlockAccess;LIBlockState;LPosition;LTessellator;)Z");
        private final int[] colorRegister = new int[3];

        RenderBlockFluidMod() {
            addClassSignature(new ConstSignature("minecraft:blocks/lava_still"));
            addClassSignature(new ConstSignature("minecraft:blocks/lava_flow"));
            addClassSignature(new ConstSignature("minecraft:blocks/water_still"));
            addClassSignature(new ConstSignature("minecraft:blocks/water_flow"));

            addMemberMapper(new MethodMapper(renderBlock));

            setupColorMaps();
        }

        private void setupColorMaps() {
            final MethodRef preRender = new MethodRef(MCPatcherUtils.COLORIZE_BLOCK18_CLASS, "preRender", "(LIBlockAccess;LIModel;LIBlockState;LPosition;LBlock;Z)Z");
            final MethodRef setDirection = new MethodRef(MCPatcherUtils.COLORIZE_BLOCK18_CLASS, "setDirectionWater", "(LDirection;)V");
            final MethodRef applyVertexColor = new MethodRef(MCPatcherUtils.COLORIZE_BLOCK18_CLASS, "applyVertexColor", "(LTessellator;FI)V");

            initCCInfo(this, renderBlock);

            addPatch(new BytecodePatch() {
                {
                    setInsertBefore(true);
                    targetMethod(renderBlock);
                }

                @Override
                public String getDescription() {
                    return "pre render block";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // r = (float) (color >> 16 & 255) / 255.0f;
                        capture(anyILOAD),
                        push(16),
                        ISHR,
                        push(255),
                        IAND,
                        I2F,
                        push(255.0f),
                        FDIV,
                        capture(anyFSTORE)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    colorRegister[0] = extractRegisterNum(getCaptureGroup(2));
                    colorRegister[1] = colorRegister[0] + 1;
                    colorRegister[2] = colorRegister[0] + 2;
                    return buildCode(
                        // if (!colorizeBlock18.preRender(blockAccess, null, blockState, position, block, true)) {
                        getCCInfo(this),
                        ALOAD_1,
                        push(null),
                        ALOAD_2,
                        ALOAD_3,
                        ALOAD, 5,
                        push(true),
                        reference(INVOKEVIRTUAL, preRender),
                        IFNE, branch("A"),

                        // return false;
                        push(false),
                        IRETURN,

                        // }
                        label("A"),

                        // color = colorizeBlock18.colorMultiplier(color);
                        getCCInfo(this),
                        getCaptureGroup(1),
                        reference(INVOKEVIRTUAL, newColorMultiplier),
                        flipLoadStore(getCaptureGroup(1))
                    );
                }
            });

            addPatch(new BytecodePatch() {
                {
                    targetMethod(renderBlock);
                }

                @Override
                public boolean filterMethod() {
                    return colorRegister[0] > 0;
                }

                @Override
                public String getDescription() {
                    return "colorize bottom of water block";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // tessellator.setColorOpaque_F(f, f, f);
                        ALOAD, 4,
                        capture(anyFLOAD),
                        backReference(1),
                        backReference(1),
                        reference(INVOKEVIRTUAL, TessellatorMod.setColorOpaque_F)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // tessellator.setColorOpaque_F(f * r, f * g, f * b);
                        ALOAD, 4,
                        getCaptureGroup(1),
                        FLOAD, colorRegister[0],
                        FMUL,
                        getCaptureGroup(1),
                        FLOAD, colorRegister[1],
                        FMUL,
                        getCaptureGroup(1),
                        FLOAD, colorRegister[2],
                        FMUL,
                        reference(INVOKEVIRTUAL, TessellatorMod.setColorOpaque_F)
                    );
                }
            });

            addPatch(new BytecodePatch() {
                private int baseMultiplier;
                private int faceIndex;

                {
                    setInsertBefore(true);
                    targetMethod(renderBlock);

                    addPreMatchSignature(new BytecodeSignature() {
                        @Override
                        public String getMatchExpression() {
                            return buildExpression(
                                // tessellator.setColorOpaque(f * base * r, f * base * g, f * base * b);
                                ALOAD, 4,
                                capture(anyFLOAD),
                                capture(anyFLOAD),
                                FMUL,
                                FLOAD, colorRegister[0],
                                FMUL,
                                backReference(1),
                                backReference(2),
                                FMUL,
                                FLOAD, colorRegister[1],
                                FMUL,
                                backReference(1),
                                backReference(2),
                                FMUL,
                                FLOAD, colorRegister[2],
                                FMUL,
                                reference(INVOKEVIRTUAL, TessellatorMod.setColorOpaque_F)
                            );
                        }

                        @Override
                        public boolean afterMatch() {
                            baseMultiplier = extractRegisterNum(getCaptureGroup(2));
                            return true;
                        }
                    });

                    addPreMatchSignature(new BytecodeSignature() {
                        @Override
                        public String getMatchExpression() {
                            return buildExpression(
                                // for (...; faceIndex < 4; ...)
                                capture(anyILOAD),
                                push(4),
                                IF_ICMPLT_or_IF_ICMPGE
                            );
                        }

                        @Override
                        public boolean afterMatch() {
                            faceIndex = extractRegisterNum(getCaptureGroup(1));
                            return true;
                        }
                    });
                }

                @Override
                public boolean filterMethod() {
                    return colorRegister[0] > 0;
                }

                @Override
                public String getDescription() {
                    return "smooth biome colors";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // tessellator.addVertexWithUV(...);
                        ALOAD, 4,
                        nonGreedy(any(0, 30)),
                        reference(INVOKEVIRTUAL, TessellatorMod.addVertexWithUV)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        callSetDirection(),

                        // colorizeBlock18.applyVertexColor(tessellator, base, vertex);
                        getCCInfo(this),
                        ALOAD, 4,
                        getBase(),
                        push(getVertex()),
                        reference(INVOKEVIRTUAL, applyVertexColor)
                    );
                }

                private byte[] callSetDirection() {
                    if (getMethodMatchCount() % 4 == 0) {
                        switch (getMethodMatchCount() / 4) {
                            case 0: // top face
                                return buildCode(
                                    // colorizeBlock18.setDirectionWater(Direction.UP);
                                    getCCInfo(this),
                                    reference(GETSTATIC, DirectionMod.UP),
                                    reference(INVOKEVIRTUAL, setDirection)
                                );

                            case 2: // bottom face
                                return buildCode(
                                    // colorizeBlock18.setDirectionWater(Direction.DOWN);
                                    getCCInfo(this),
                                    reference(GETSTATIC, DirectionMod.DOWN),
                                    reference(INVOKEVIRTUAL, setDirection)
                                );

                            case 3: // side faces
                                return buildCode(
                                    // colorizeBlock18.setDirectionWater(Direction.values()[faceIndex + 2]);
                                    getCCInfo(this),
                                    reference(INVOKESTATIC, DirectionMod.values),
                                    ILOAD, faceIndex,
                                    push(2),
                                    IADD,
                                    AALOAD,
                                    reference(INVOKEVIRTUAL, setDirection)
                                );

                            default:
                                break;
                        }
                    }
                    return new byte[0];
                }

                private Object getBase() {
                    switch (getMethodMatchCount() / 4) {
                        case 0: // top face
                        case 1: // top face (reverse)
                        default:
                            return push(1.0f);

                        case 2: // bottom face
                            return push(0.5f);

                        case 3: // side faces
                        case 4: // side faces (reverse)
                            return buildCode(
                                FLOAD, baseMultiplier
                            );
                    }
                }

                private int getVertex() {
                    int vertex = getMethodMatchCount() % 4;
                    switch (getMethodMatchCount() / 4) {
                        case 1: // top face (reverse): 0, 3, 2, 1
                            return (4 - vertex) % 4;

                        case 4: // side faces (reverse): 3, 2, 1, 0
                            return 3 - vertex;

                        default: // 0, 1, 2, 3
                            return vertex;
                    }
                }
            });
        }
    }
}
