package com.prupe.mcpatcher.mod;

import com.prupe.mcpatcher.*;
import com.prupe.mcpatcher.basemod.*;
import com.prupe.mcpatcher.basemod.ext18.*;
import com.prupe.mcpatcher.mal.TexturePackAPIMod;
import javassist.bytecode.AccessFlag;

import java.util.HashMap;
import java.util.Map;

import static com.prupe.mcpatcher.BinaryRegex.*;
import static com.prupe.mcpatcher.BytecodeMatcher.*;
import static javassist.bytecode.Opcode.*;

public class CustomTexturesModels extends Mod {
    static final MethodRef blockColorMultiplier = new MethodRef("Block", "colorMultiplier", "(LIBlockAccess;LPosition;I)I");
    static final InterfaceMethodRef iteratorNext = new InterfaceMethodRef("java/util/Iterator", "next", "()Ljava/lang/Object;");
    static final InterfaceMethodRef listGet = new InterfaceMethodRef("java/util/List", "get", "(I)Ljava/lang/Object;");
    static final ClassRef modelFaceClass = new ClassRef("ModelFace");

    static final MethodRef getCTMInstance = new MethodRef(MCPatcherUtils.CTM_UTILS18_CLASS, "getInstance", "()L" + MCPatcherUtils.CTM_UTILS18_CLASS + ";");
    static final MethodRef setDirection = new MethodRef(MCPatcherUtils.CTM_UTILS18_CLASS, "setDirection", "(LDirection;)V");
    static final MethodRef newUseColormap = new MethodRef(MCPatcherUtils.CTM_UTILS18_CLASS, "useColormap", "(LModelFace;)Z");
    static final MethodRef newColorMultiplier = new MethodRef(MCPatcherUtils.CTM_UTILS18_CLASS, "colorMultiplier", "(I)I");
    static final MethodRef newVertexColor = new MethodRef(MCPatcherUtils.CTM_UTILS18_CLASS, "getVertexColor", "(FII)F");
    static final MethodRef newBlockFace = new MethodRef(MCPatcherUtils.CTM_UTILS18_CLASS, "getModelFace", "(LModelFace;)LModelFace;");

    static final MethodRef preRenderItem = new MethodRef(MCPatcherUtils.CIT_UTILS18_CLASS, "preRender", "(LItemStack;)V");
    static final MethodRef postRenderItem = new MethodRef(MCPatcherUtils.CIT_UTILS18_CLASS, "postRender", "()V");
    static final MethodRef newItemFace = new MethodRef(MCPatcherUtils.CIT_UTILS18_CLASS, "getModelFace", "(LModelFace;)LModelFace;");
    static final MethodRef newRenderEnchantments3D = new MethodRef(MCPatcherUtils.CIT_UTILS18_CLASS, "renderEnchantments3D", "(LRenderItemCustom;LIModel;)Z");
    static final MethodRef newArmorTexture = new MethodRef(MCPatcherUtils.CIT_UTILS18_CLASS, "getArmorTexture", "(LResourceLocation;LItemStack;I)LResourceLocation;");
    static final MethodRef newRenderArmorEnchantments = new MethodRef(MCPatcherUtils.CIT_UTILS18_CLASS, "renderArmorEnchantments", "(LEntityLivingBase;LModelBase;LItemStack;IFFFFFF)Z");

    private static final Map<String, Integer> ctmInfoMap = new HashMap<String, Integer>();

    public CustomTexturesModels() {
        name = MCPatcherUtils.CUSTOM_TEXTURES_MODELS;
        author = "MCPatcher";
        description = "Allows custom block and item rendering.";
        version = "1.1";

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
        addClassMod(new IBlockStatePropertyMod(this));
        addClassMod(new TessellatorMod(this));
        addClassMod(new TessellatorFactoryMod(this));
        addClassMod(new BiomeGenBaseMod(this));
        PositionMod.setup(this);
        addClassMod(new DirectionWithAOMod(this));
        addClassMod(new IModelMod(this));
        addClassMod(new ModelFaceMod(this));
        addClassMod(new ModelFaceSpriteMod(this));

        addClassMod(new BlockMod());
        //addClassMod(new BetterGrassMod("BlockGrass"));
        //addClassMod(new BetterGrassMod("BlockMycel"));
        addClassMod(new MinecraftMod());
        addClassMod(new RenderBlockDispatcherMod());
        addClassMod(new RenderBlockCustomMod());
        addClassMod(new RenderBlockCustomInnerMod());
        addClassMod(new RenderBlockFluidMod());
        addClassMod(new RenderGlobalMod());
        addClassMod(new EntityDiggingFXMod());

        addClassMod(new ItemMod(this));
        addClassMod(new ItemStackMod(this));
        addClassMod(new NBTTagCompoundMod(this));
        addClassMod(new NBTTagListMod(this));
        addClassMod(new PotionMod(this));
        addClassMod(new PotionHelperMod(this));
        addClassMod(new EntityLivingBaseMod(this));
        addClassMod(new RenderItemCustom());
        addClassMod(new RenderArmorMod());
        addClassMod(new ItemBlockMod());
        addClassMod(new EntityPotionMod());
        addClassMod(new RenderItemFrameMod());

        addClassFiles("com.prupe.mcpatcher.ctm.*");
        addClassFiles("com.prupe.mcpatcher.cit.*");
        addClassFiles(MCPatcherUtils.COLORIZE_BLOCK_CLASS + "*");
        addClassFiles(MCPatcherUtils.COLORIZE_BLOCK18_CLASS + "*");

        TexturePackAPIMod.earlyInitialize(2, MCPatcherUtils.CTM_UTILS_CLASS, "clear");
        TexturePackAPIMod.earlyInitialize(2, MCPatcherUtils.COLORIZE_BLOCK18_CLASS, "reset");
        TexturePackAPIMod.earlyInitialize(2, MCPatcherUtils.CIT_UTILS_CLASS, "init");
    }

    @Override
    public String[] getLoggingCategories() {
        return new String[]{
            MCPatcherUtils.CONNECTED_TEXTURES,
            MCPatcherUtils.CUSTOM_ITEM_TEXTURES
        };
    }

    private class MinecraftMod extends com.prupe.mcpatcher.basemod.MinecraftMod {
        MinecraftMod() {
            super(CustomTexturesModels.this);

            final MethodRef getRenderBlockDispatcher = new MethodRef(getDeobfClass(), "getRenderBlockDispatcher", "()LRenderBlockDispatcher;");

            addMemberMapper(new MethodMapper(getRenderBlockDispatcher));
        }
    }

    private class BlockMod extends com.prupe.mcpatcher.basemod.BlockMod {
        private final MethodRef registerBlocks = new MethodRef(getDeobfClass(), "registerBlocks", "()V");

        BlockMod() {
            super(CustomTexturesModels.this);

            mapBlockMaterial();
            mapBlockState();

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

            mapBlockClass(2, "grass", "BlockGrass");
            mapBlockClass(110, "mycelium", "BlockMycel");

            addMemberMapper(new MethodMapper(blockColorMultiplier));

            addPatch(new MakeMemberPublicPatch(blockMaterial));
        }

        private void mapBlockClass(final int blockId, final String blockName, final String className) {
            addClassSignature(new BytecodeSignature() {
                {
                    setMethod(registerBlocks);
                    addXref(1, new ClassRef(className));
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // registerBlock(blockId, blockName, new BlockSubclass(...)
                        push(blockId),
                        push(blockName),
                        captureReference(NEW),
                        DUP
                    );
                }
            });
        }
    }

    private class BetterGrassMod extends ClassMod {
        private final MethodRef createBoolProperty = new MethodRef("PropertyBool", "create", "(Ljava/lang/String;)LPropertyBool;");
        private final String blockName;

        BetterGrassMod(String blockName) {
            setParentClass("Block");
            addPrerequisiteClass("Block");
            this.blockName = blockName;

            final FieldRef snowyProperty = new FieldRef(getDeobfClass(), "SNOWY", "LPropertyBool;");
            final FieldRef northProperty = addPropertyField("north");
            final FieldRef southProperty = addPropertyField("south");
            final FieldRef westProperty = addPropertyField("west");
            final FieldRef eastProperty = addPropertyField("east");

            addClassSignature(new BytecodeSignature() {
                {
                    matchStaticInitializerOnly(true);
                    addXref(1, createBoolProperty);
                    addXref(2, snowyProperty);
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // SNOWY = PropertyBool.create("snowy");
                        push("snowy"),
                        captureReference(INVOKESTATIC),
                        captureReference(PUTSTATIC)
                    );
                }
            });

            addPatch(new BytecodePatch() {
                private final MethodRef setBlockState = new MethodRef(getDeobfClass(), BlockMod.setBlockState.getName(), BlockMod.setBlockState.getType());
                private final MethodRef booleanValueOf = new MethodRef("java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;");

                {
                    setInsertBefore(true);
                    matchConstructorOnly(true);
                }

                @Override
                public String getDescription() {
                    return "extend default block state";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // this.setBlockState(...)
                        reference(INVOKEVIRTUAL, setBlockState)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        setProperty(northProperty),
                        setProperty(southProperty),
                        setProperty(westProperty),
                        setProperty(eastProperty)
                    );
                }

                private byte[] setProperty(FieldRef field) {
                    return buildCode(
                        // .setProperty(field, false)
                        reference(GETSTATIC, field),
                        push(false),
                        reference(INVOKESTATIC, booleanValueOf),
                        reference(INVOKEINTERFACE, IBlockStateMod.setProperty)
                    );
                }
            });

            addPatch(new BytecodePatch() {
                private final ClassRef propertyClass = new ClassRef("IBlockStateProperty");

                @Override
                public String getDescription() {
                    return "extend block properties array";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // new IBlockStateProperty[]{ SNOWY }
                        push(1),
                        reference(ANEWARRAY, propertyClass),
                        DUP,
                        push(0),
                        reference(GETSTATIC, snowyProperty),
                        AASTORE
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // new IBlockStateProperty[]{ SNOWY, NORTH, SOUTH, WEST, EAST }
                        push(5),
                        reference(ANEWARRAY, propertyClass),
                        setArrayElement(0, snowyProperty),
                        setArrayElement(1, northProperty),
                        setArrayElement(2, southProperty),
                        setArrayElement(3, westProperty),
                        setArrayElement(4, eastProperty)
                    );
                }

                private byte[] setArrayElement(int index, FieldRef field) {
                    return buildCode(
                        DUP,
                        push(index),
                        reference(GETSTATIC, field),
                        AASTORE
                    );
                }
            });

            addPatch(new BytecodePatch() {
                private final MethodRef getBlockStateInWorld = new MethodRef(getDeobfClass(), BlockMod.getBlockStateInWorld.getName(), BlockMod.getBlockStateInWorld.getType());
                private final MethodRef setBetterGrassProperty = new MethodRef(MCPatcherUtils.CTM_UTILS18_CLASS, "setBetterGrassProperty", "(LIBlockState;LBlock;LIBlockAccess;LPosition;LIBlockStateProperty;I)LIBlockState;");

                {
                    setInsertBefore(true);
                    targetMethod(getBlockStateInWorld);
                }

                @Override
                public String getDescription() {
                    return "extend block state";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ARETURN
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        setProperty(northProperty, 2),
                        setProperty(southProperty, 3),
                        setProperty(westProperty, 4),
                        setProperty(eastProperty, 5)
                    );
                }

                private byte[] setProperty(FieldRef field, int direction) {
                    return buildCode(
                        // CTMUtils18.setBetterGrassProperty(..., this, blockAccess, position, field, direction)
                        ALOAD_0,
                        ALOAD_2,
                        ALOAD_3,
                        reference(GETSTATIC, field),
                        push(direction),
                        reference(INVOKESTATIC, setBetterGrassProperty)
                    );
                }
            });
        }

        private FieldRef addPropertyField(final String name) {
            final FieldRef field = new FieldRef(getDeobfClass(), name.toUpperCase(), "LPropertyBool;");

            addPatch(new AddFieldPatch(field, AccessFlag.PUBLIC | AccessFlag.STATIC | AccessFlag.FINAL));

            addPatch(new BytecodePatch() {
                {
                    matchStaticInitializerOnly(true);
                    setInsertBefore(true);
                }

                @Override
                public String getDescription() {
                    return "initialize field for " + name + " property";
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
                        // FIELD = PropertyBool.create("field");
                        push(name),
                        reference(INVOKESTATIC, createBoolProperty),
                        reference(PUTSTATIC, field)
                    );
                }
            });

            return field;
        }

        @Override
        public String getDeobfClass() {
            return blockName;
        }
    }

    private class RenderBlockDispatcherMod extends ClassMod {
        RenderBlockDispatcherMod() {
            final MethodRef getModel = new MethodRef(getDeobfClass(), "getModel", "(LIBlockState;LIBlockAccess;LPosition;)LIModel;");

            addClassSignature(new ConstSignature("Tesselating block in world"));
            addClassSignature(new ConstSignature(0xf000f).negate(true));

            addMemberMapper(new MethodMapper(getModel));
        }
    }

    private static String getCTMKey(PatchComponent patchComponent) {
        return patchComponent.getClassFile().getName() + ":" + patchComponent.getMethodInfo().toString();
    }

    private static void initCTMInfo(ClassMod classMod, final MethodRef... methods) {
        ctmInfoMap.clear();
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
                ctmInfoMap.put(getCTMKey(this), register);
                return buildCode(
                    reference(INVOKESTATIC, getCTMInstance),
                    registerLoadStore(ASTORE, register)
                );
            }
        });
    }

    private static byte[] getCTMInfo(PatchComponent patchComponent) {
        String key = getCTMKey(patchComponent);
        Integer register = ctmInfoMap.get(key);
        if (register == null) {
            for (Map.Entry<String, Integer> entry : ctmInfoMap.entrySet()) {
                Logger.log(Logger.LOG_MAIN, "  %s -> %s", entry.getKey(), entry.getValue());
            }
            throw new IllegalStateException("no ctmInfo for [" + key + "]");
        }
        return registerLoadStore(ALOAD, register);
    }

    private class RenderBlockCustomMod extends ClassMod {
        private final MethodRef renderBlock = new MethodRef(getDeobfClass(), "renderBlock", "(LIBlockAccess;LIModel;LIBlockState;LPosition;LTessellator;Z)Z");
        private final MethodRef renderBlockAO = new MethodRef(getDeobfClass(), "renderBlockAO", "(LIBlockAccess;LIModel;LBlock;LPosition;LTessellator;Z)Z");
        private final MethodRef renderBlockNonAO = new MethodRef(getDeobfClass(), "renderBlockNonAO", "(LIBlockAccess;LIModel;LBlock;LPosition;LTessellator;Z)Z");
        private final MethodRef renderFaceAO = new MethodRef(getDeobfClass(), "renderFaceAO", "(LIBlockAccess;LBlock;LPosition;LTessellator;Ljava/util/List;[FLjava/util/BitSet;LRenderBlockCustomInner;)V");
        private final MethodRef renderFaceNonAO = new MethodRef(getDeobfClass(), "renderFaceNonAO", "(LIBlockAccess;LBlock;LPosition;LDirection;IZLTessellator;Ljava/util/List;Ljava/util/BitSet;)V");
        private final MethodRef renderBlockHeld = new MethodRef(getDeobfClass(), "renderBlockHeld", "(LIModel;LIBlockState;FZ)V");
        private final MethodRef renderFaceHeld = new MethodRef(getDeobfClass(), "renderFaceHeld", "(FFFFLjava/util/List;)V");

        RenderBlockCustomMod() {
            if (getMinecraftVersion().compareTo("1.8.2-pre5") < 0) {
                addClassSignature(new ConstSignature(0xf000f));
            }

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
                        // array = new float[Direction.values().length * 2];
                        anyReference(INVOKESTATIC),
                        ARRAYLENGTH,
                        push(2),
                        IMUL,
                        NEWARRAY, T_FLOAT,
                        anyASTORE,

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

            initCTMInfo(this, renderBlockAO, renderBlockNonAO, renderFaceAO, renderFaceNonAO, renderBlockHeld, renderFaceHeld);

            setupColorMaps();
            setupCTM();
            setupHeldCTM();
        }

        private void setupPreRender(final MethodRef method) {
            addPatch(new BytecodePatch() {
                {
                    setInsertAfter(true);
                    targetMethod(renderBlock);
                }

                @Override
                public String getDescription() {
                    return "pre render";
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

            addPatch(new BytecodePatch() {
                {
                    setInsertBefore(true);
                    targetMethod(renderBlock);
                }

                @Override
                public String getDescription() {
                    return "post render";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // return ...;
                        IRETURN
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // return CTMUtils18.postRender(...);
                        reference(INVOKESTATIC, new MethodRef(method.getClassName(), "postRender", "(Z)Z"))
                    );
                }
            });
        }

        private void setupColorMaps() {
            final MethodRef useColormap = new MethodRef("ModelFace", "useColormap", "()Z");

            setupPreRender(getCTMInstance);

            addPatch(new BytecodePatch() {
                {
                    setInsertBefore(true);
                    targetMethod(renderBlockAO, renderBlockNonAO, renderBlockHeld);
                }

                @Override
                public String getDescription() {
                    return "set render direction";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // model.getFaces(direction)
                        anyALOAD,
                        capture(anyALOAD),
                        reference(INVOKEINTERFACE, IModelMod.getFaces)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // ctm.setDirection(direction);
                        getCTMInfo(this),
                        getCaptureGroup(1),
                        reference(INVOKEVIRTUAL, setDirection)
                    );
                }
            });

            addPatch(new BytecodePatch() {
                {
                    setInsertBefore(true);
                    targetMethod(renderBlockAO, renderBlockNonAO, renderBlockHeld);
                }

                @Override
                public String getDescription() {
                    return "clear render direction";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // model.getDefaultFaces()
                        anyALOAD,
                        reference(INVOKEINTERFACE, IModelMod.getDefaultFaces)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // ctm.setDirection(null);
                        getCTMInfo(this),
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
                        // ctm.useColormap(face)
                        getCTMInfo(this),
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
                        // color = ctm.colorMultiplier(color)
                        getCTMInfo(this),
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
                    // RenderBlockCustomInner.getVertexColor(inner)[0,1,2,3] * ctm.getVertexColor(color, count / 3, count % 3)
                    return buildCode(
                        getCTMInfo(this),
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
                        // face = ctm.newModelFace(face);
                        getCTMInfo(this),
                        flipLoadStore(getCaptureGroup(1)),
                        reference(INVOKEVIRTUAL, newBlockFace),
                        getCaptureGroup(1)
                    );
                }
            });
        }

        private void setupHeldCTM() {
            final MethodRef preRenderHeld = new MethodRef(MCPatcherUtils.CTM_UTILS18_CLASS, "preRenderHeld", "(LIModel;LIBlockState;LBlock;)Z");

            addMemberMapper(new MethodMapper(renderBlockHeld));
            addMemberMapper(new MethodMapper(renderFaceHeld));

            addPatch(new BytecodePatch() {
                {
                    setInsertAfter(true);
                    targetMethod(renderBlockHeld);
                }

                @Override
                public String getDescription() {
                    return "pre render (held blocks)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // rgb = block.getRenderColor(block.getStateForEntityRender(blockState));
                        capture(anyALOAD),
                        backReference(1),
                        ALOAD_2,
                        anyReference(INVOKEVIRTUAL),
                        anyReference(INVOKEVIRTUAL),
                        capture(anyISTORE)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // if (!ctm.preRenderHeld(model, blockState, block)) {
                        getCTMInfo(this),
                        ALOAD_1,
                        ALOAD_2,
                        getCaptureGroup(1),
                        reference(INVOKEVIRTUAL, preRenderHeld),
                        IFNE, branch("A"),

                        // return;
                        RETURN,

                        // }
                        label("A"),

                        // color = ctm.colorMultiplier(color);
                        getCTMInfo(this),
                        flipLoadStore(getCaptureGroup(2)),
                        reference(INVOKEVIRTUAL, newColorMultiplier),
                        getCaptureGroup(2)
                    );
                }
            });

            addPatch(new BytecodePatch() {
                {
                    setInsertAfter(true);
                    targetMethod(renderFaceHeld);
                }

                @Override
                public String getDescription() {
                    return "override texture (held blocks)";
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
                        // face = CITUtils18.newModelFace(face);
                        flipLoadStore(getCaptureGroup(1)),
                        reference(INVOKESTATIC, newItemFace),
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
            final MethodRef preRender = new MethodRef(MCPatcherUtils.CTM_UTILS18_CLASS, "preRender", "(LIBlockAccess;LIModel;LIBlockState;LPosition;LBlock;Z)Z");
            final MethodRef setDirection = new MethodRef(MCPatcherUtils.CTM_UTILS18_CLASS, "setDirectionWater", "(LDirection;)V");
            final MethodRef applyVertexColor1 = new MethodRef(MCPatcherUtils.CTM_UTILS18_CLASS, "applyVertexColor", "(LTessellator;FI)V");
            final MethodRef applyVertexColor2 = new MethodRef(MCPatcherUtils.CTM_UTILS18_CLASS, "applyVertexColor", "(FIFFF)F");
            final MethodRef getVertexColor = new MethodRef(MCPatcherUtils.CTM_UTILS18_CLASS, "getVertexColor", "(I)F");

            initCTMInfo(this, renderBlock);

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
                        // if (!ctm.preRender(blockAccess, null, blockState, position, block, true)) {
                        getCTMInfo(this),
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

                        // color = ctm.colorMultiplier(color);
                        getCTMInfo(this),
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
                        // 1.8.2-pre5+: .setColor(f, f, f, 1.0f)
                        // older: tessellator.setColorOpaque_F(f, f, f);
                        capture(anyFLOAD),
                        backReference(1),
                        backReference(1),
                        lookAhead(TessellatorMod.haveVertexFormatClass() ?
                                build(
                                    push(1.0f),
                                    reference(INVOKEVIRTUAL, TessellatorMod.setColorF)
                                ) :
                                build(
                                    reference(INVOKEVIRTUAL, TessellatorMod.setColorOpaque_F)
                                ),
                            true)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // tessellator.setColorOpaque_F(f * r, f * g, f * b);
                        getCaptureGroup(1),
                        FLOAD, colorRegister[0],
                        FMUL,
                        getCaptureGroup(1),
                        FLOAD, colorRegister[1],
                        FMUL,
                        getCaptureGroup(1),
                        FLOAD, colorRegister[2],
                        FMUL
                    );
                }
            });

            addPatch(new BytecodePatch() {
                private int baseMultiplier;
                private int faceIndex;

                {
                    if (!TessellatorMod.haveVertexFormatClass()) {
                        setInsertBefore(true);
                    }
                    targetMethod(renderBlock);

                    addPreMatchSignature(new BytecodeSignature() {
                        @Override
                        public String getMatchExpression() {
                            if (TessellatorMod.haveVertexFormatClass()) {
                                return getMatchExpression2();
                            } else {
                                return getMatchExpression1();
                            }
                        }

                        private String getMatchExpression1() {
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

                        private String getMatchExpression2() {
                            return buildExpression(
                                // r1 = f * base * r;
                                capture(anyFLOAD),
                                capture(anyFLOAD),
                                FMUL,
                                FLOAD, colorRegister[0],
                                FMUL,
                                anyFSTORE,

                                // g1 = f * base * g;
                                backReference(1),
                                backReference(2),
                                FMUL,
                                FLOAD, colorRegister[1],
                                FMUL,
                                anyFSTORE,

                                // b1 = f * base * b;
                                backReference(1),
                                backReference(2),
                                FMUL,
                                FLOAD, colorRegister[2],
                                FMUL,
                                anyFSTORE
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
                    if (TessellatorMod.haveVertexFormatClass()) {
                        return getMatchExpression2();
                    } else {
                        return getMatchExpression1();
                    }
                }

                @Override
                public byte[] getReplacementBytes() {
                    if (TessellatorMod.haveVertexFormatClass()) {
                        return getReplacementBytes2();
                    } else {
                        return getReplacementBytes1();
                    }
                }

                private String getMatchExpression1() {
                    return buildExpression(
                        // tessellator.addVertexWithUV(...);
                        ALOAD, 4,
                        nonGreedy(any(0, 30)),
                        reference(INVOKEVIRTUAL, TessellatorMod.addVertexWithUV)
                    );
                }

                private byte[] getReplacementBytes1() {
                    return buildCode(
                        callSetDirection(),

                        // ctm.applyVertexColor(tessellator, base, vertex);
                        getCTMInfo(this),
                        ALOAD, 4,
                        getBase(),
                        push(getVertex()),
                        reference(INVOKEVIRTUAL, applyVertexColor1)
                    );
                }

                private String getMatchExpression2() {
                    return buildExpression(
                        // ...(r, g, b, 1.0f)
                        // -or-
                        // ...(f * r, f * g, f * b, 1.0f)
                        capture(repeat(build(
                            anyFLOAD,
                            optional(build(
                                anyFLOAD,
                                FMUL
                            ))
                        ), 3)),
                        push(1.0f)
                    );
                }

                private byte[] getReplacementBytes2() {
                    return buildCode(
                        callSetDirection(),

                        // ctm.applyVertexColor(base, vertex, r, g, b)
                        getCTMInfo(this),
                        getBase(),
                        push(getVertex()),
                        getCaptureGroup(1),
                        reference(INVOKEVIRTUAL, applyVertexColor2),

                        // ctm.getVertexColor(1)
                        getCTMInfo(this),
                        push(1),
                        reference(INVOKEVIRTUAL, getVertexColor),

                        // ctm.getVertexColor(2)
                        getCTMInfo(this),
                        push(2),
                        reference(INVOKEVIRTUAL, getVertexColor),

                        push(1.0f)
                    );
                }

                private byte[] callSetDirection() {
                    if (getMethodMatchCount() % 4 == 0) {
                        switch (getMethodMatchCount() / 4) {
                            case 0: // top face
                                return buildCode(
                                    // ctm.setDirectionWater(Direction.UP);
                                    getCTMInfo(this),
                                    reference(GETSTATIC, DirectionMod.UP),
                                    reference(INVOKEVIRTUAL, setDirection)
                                );

                            case 2: // bottom face
                                return buildCode(
                                    // ctm.setDirectionWater(Direction.DOWN);
                                    getCTMInfo(this),
                                    reference(GETSTATIC, DirectionMod.DOWN),
                                    reference(INVOKEVIRTUAL, setDirection)
                                );

                            case 3: // side faces
                                return buildCode(
                                    // ctm.setDirectionWater(Direction.values()[faceIndex + 2]);
                                    getCTMInfo(this),
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

    private class RenderGlobalMod extends ClassMod {
        RenderGlobalMod() {
            addClassSignature(new ConstSignature("textures/environment/clouds.png"));
            addClassSignature(new ConstSignature("textures/environment/moon_phases.png"));

            final MethodRef buildNear = new MethodRef(getDeobfClass(), "buildNear", "(LPosition;LUnknownClass_18_cop;)Z");

            addClassSignature(new BytecodeSignature() {
                {
                    setMethod(new MethodRef(getDeobfClass(), "updateRenderList", "(LEntity;DLUnknownInterface_18_cox;IZ)V"));
                    addXref(1, buildNear);
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // if (this.buildNear(position, chunk.something)) {
                        ALOAD_0,
                        anyALOAD,
                        anyALOAD,
                        anyReference(GETFIELD),
                        captureReference(INVOKESPECIAL),

                        // this.mc.profiler.startSection("build near")
                        IFEQ, any(2),
                        ALOAD_0,
                        anyReference(GETFIELD),
                        anyReference(GETFIELD),
                        push("build near"),
                        anyReference(INVOKEVIRTUAL)
                    );
                }
            });

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override build near check";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // ...
                        begin(),
                        any(0, 1000),
                        end()
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // *burp*
                        push(false),
                        IRETURN
                    );
                }
            }.targetMethod(buildNear));
        }
    }

    private class EntityDiggingFXMod extends ClassMod {
        EntityDiggingFXMod() {
            setParentClass("EntityFX");

            final MethodRef applyColorMultiplier = new MethodRef(getDeobfClass(), "applyColorMultiplier", "(LPosition;)LEntityDiggingFX;");
            final MethodRef getParticleColor = new MethodRef(MCPatcherUtils.CTM_UTILS18_CLASS, "getParticleColor", "(LIBlockAccess;LIBlockState;LPosition;I)I");

            addClassSignature(new ConstSignature(0.015609375f));
            addClassSignature(new ConstSignature(255.0f));
            addClassSignature(new ConstSignature(4.0f));

            addMemberMapper(new MethodMapper(applyColorMultiplier));

            addPatch(new BytecodePatch() {
                {
                    setInsertAfter(true);
                    targetMethod(applyColorMultiplier);
                }

                @Override
                public String getDescription() {
                    return "override digging particle color";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // color = this.blockState.getBlock().colorMultiplier(this.worldObj, position)
                        ALOAD_0,
                        captureReference(GETFIELD),
                        anyReference(INVOKEINTERFACE),
                        ALOAD_0,
                        captureReference(GETFIELD),
                        ALOAD_1,
                        anyReference(INVOKEVIRTUAL),
                        capture(anyISTORE)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // color = CTMUtils18.getInstance().getParticleColor(this.worldObj, this.blockState, position, color);
                        reference(INVOKESTATIC, getCTMInstance),
                        ALOAD_0,
                        getCaptureGroup(2),
                        ALOAD_0,
                        getCaptureGroup(1),
                        ALOAD_1,
                        flipLoadStore(getCaptureGroup(3)),
                        reference(INVOKEVIRTUAL, getParticleColor),
                        getCaptureGroup(3)
                    );
                }
            });
        }
    }

    private class RenderItemCustom extends ClassMod {
        RenderItemCustom() {
            addClassSignature(new ConstSignature("textures/misc/enchanted_item_glint.png"));
            addClassSignature(new ConstSignature("inventory"));

            final MethodRef renderItem1 = new MethodRef(getDeobfClass(), "renderItem1", "(LIModel;ILItemStack;)V");
            final MethodRef renderItem2 = new MethodRef(getDeobfClass(), "renderItem2", "(LItemStack;LIModel;)V");
            final MethodRef renderItemFace = new MethodRef(getDeobfClass(), "renderItemFace", "(LIModel;ILItemStack;)V");
            final MethodRef renderEnchantment = new MethodRef(getDeobfClass(), "renderEnchantment", "(LIModel;)V");
            final MethodRef renderFace = new MethodRef(getDeobfClass(), "renderFace", "(LTessellator;Ljava/util/List;ILItemStack;)V");
            final MethodRef renderItemOverlayIntoGUI = new MethodRef(getDeobfClass(), "renderItemOverlayIntoGUI", "(LFontRenderer;LItemStack;IILjava/lang/String;)V");
            final MethodRef hasEffect = new MethodRef("ItemStack", "hasEffectVanilla", "()Z");
            final MethodRef getMaxDamage = new MethodRef("ItemStack", "getMaxDamage", "()I");

            addClassSignature(new BytecodeSignature() {
                {
                    setMethod(renderItem2);
                    addXref(1, hasEffect);
                    addXref(2, renderEnchantment);
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // GL11.glTranslatef(-0.5f, -0.5f, -0.5f);
                        push(-0.5f),
                        push(-0.5f),
                        push(-0.5f),
                        anyReference(INVOKESTATIC),

                        // this.xxx(model, itemStack);
                        ALOAD_0,
                        ALOAD_2,
                        ALOAD_1,
                        anyReference(INVOKESPECIAL),

                        // if (itemStack.hasEffect())
                        ALOAD_1,
                        captureReference(INVOKEVIRTUAL),
                        IFEQ, any(2),

                        // this.renderEnchantment(model)
                        ALOAD_0,
                        ALOAD_2,
                        captureReference(INVOKESPECIAL)
                    );
                }
            });

            addClassSignature(new BytecodeSignature() {
                {
                    setMethod(renderItemOverlayIntoGUI);
                    addXref(1, getMaxDamage);
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // 13.0 - (double) itemStack.getItemDamage() * 13.0 / (double) itemStack.getMaxDamage()
                        push(13.0),
                        ALOAD_2,
                        anyReference(INVOKEVIRTUAL),
                        I2D,
                        push(13.0),
                        DMUL,
                        ALOAD_2,
                        captureReference(INVOKEVIRTUAL)
                    );
                }
            });

            addMemberMapper(new MethodMapper(renderItem1));
            addMemberMapper(new MethodMapper(renderFace));
            addMemberMapper(new MethodMapper(renderItemFace));

            addPatch(new MakeMemberPublicPatch(renderItem1));

            addPatch(new BytecodePatch() {
                {
                    targetMethod(renderItem1);
                }

                @Override
                public String getDescription() {
                    return "pre render item";
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
                        // CITUtils18.preRenderItem(itemStack);
                        ALOAD_3,
                        reference(INVOKESTATIC, preRenderItem)
                    );
                }
            });

            addPatch(new BytecodePatch() {
                {
                    setInsertBefore(true);
                    targetMethod(renderItem2);
                }

                @Override
                public String getDescription() {
                    return "post render item";
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
                        // CITUtils18.postRender();
                        reference(INVOKESTATIC, postRenderItem)
                    );
                }
            });

            addPatch(new BytecodePatch() {
                {
                    setInsertAfter(true);
                    targetMethod(renderFace);
                }

                @Override
                public String getDescription() {
                    return "override texture";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // 1.8.2-pre2+: face = (ModelFace) list.get(i);
                        // older: face = (ModelFace) iterator.next();
                        anyALOAD,
                        or(
                            build(
                                reference(INVOKEINTERFACE, iteratorNext)
                            ),
                            build(
                                anyILOAD,
                                reference(INVOKEINTERFACE, listGet)
                            )
                        ),
                        reference(CHECKCAST, modelFaceClass),
                        capture(anyASTORE)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // face = CITUtils18.newModelFace(face);
                        flipLoadStore(getCaptureGroup(1)),
                        reference(INVOKESTATIC, newItemFace),
                        getCaptureGroup(1)
                    );
                }
            });

            addPatch(new BytecodePatch() {
                {
                    targetMethod(renderItem2);
                }

                @Override
                public String getDescription() {
                    return "render custom enchantments";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // this.renderEnchantment(model);
                        ALOAD_0,
                        ALOAD_2,
                        reference(INVOKESPECIAL, renderEnchantment)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // if (!CITUtils18.renderEnchantments3D(this, model)) {
                        ALOAD_0,
                        ALOAD_2,
                        reference(INVOKESTATIC, newRenderEnchantments3D),
                        IFNE, branch("A"),

                        // ...
                        getMatch(),

                        // }
                        label("A")
                    );
                }
            });
        }
    }

    private class RenderArmorMod extends ClassMod {
        RenderArmorMod() {
            final MethodRef renderArmor = new MethodRef(getDeobfClass(), "renderArmor", "(LEntityLivingBase;FFFFFFFI)V");
            final MethodRef renderEnchantment = new MethodRef(getDeobfClass(), "renderEnchantment", "(LEntityLivingBase;LModelBase;FFFFFFF)V");
            final MethodRef getArmorTexture2 = new MethodRef(getDeobfClass(), "getArmorTexture2", "(LItemArmor;Z)LResourceLocation;");
            final MethodRef getArmorTexture3 = new MethodRef(getDeobfClass(), "getArmorTexture3", "(LItemArmor;ZLjava/lang/String;)LResourceLocation;");
            final MethodRef renderModel = new MethodRef("ModelBase", "render", "(LEntity;FFFFFF)V");

            addClassSignature(new ConstSignature("textures/misc/enchanted_item_glint.png"));
            addClassSignature(new ConstSignature("textures/models/armor/%s_layer_%d%s.png"));

            addClassSignature(new BytecodeSignature() {
                {
                    setMethod(renderEnchantment);
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(5890) // GL_TEXTURE
                    );
                }
            });

            addClassSignature(new BytecodeSignature() {
                {
                    setMethod(getArmorTexture2);
                    addXref(1, getArmorTexture3);
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // return this.getArmorTexture(item, overlay, null);
                        begin(),
                        ALOAD_0,
                        ALOAD_1,
                        ILOAD_2,
                        ACONST_NULL,
                        captureReference(INVOKESPECIAL),
                        ARETURN,
                        end()
                    );
                }
            });

            addClassSignature(new BytecodeSignature() {
                {
                    setMethod(renderEnchantment);
                    addXref(1, renderModel);
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_2,
                        ALOAD_1,
                        FLOAD_3,
                        FLOAD, 4,
                        FLOAD, 6,
                        FLOAD, 7,
                        FLOAD, 8,
                        FLOAD, 9,
                        captureReference(INVOKEVIRTUAL)
                    );
                }
            });

            addMemberMapper(new MethodMapper(renderArmor));

            addPatch(new BytecodePatch() {
                {
                    setInsertAfter(true);
                    targetMethod(renderArmor);
                }

                @Override
                public String getDescription() {
                    return "override armor texture";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // this.getArmorTexture(item, overlay);
                        ALOAD_0,
                        anyALOAD,
                        anyILOAD,
                        reference(INVOKESPECIAL, getArmorTexture2)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // CITUtils18.getArmorTexture(..., itemStack, slot)
                        ALOAD, 10,
                        ILOAD, 9,
                        reference(INVOKESTATIC, newArmorTexture)
                    );
                }
            });

            addPatch(new BytecodePatch() {
                {
                    targetMethod(renderArmor);
                }

                @Override
                public String getDescription() {
                    return "render armor enchantments";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // this.renderEnchantment(...)
                        ALOAD_0,
                        ALOAD_1,
                        capture(anyALOAD),
                        FLOAD_2,
                        FLOAD_3,
                        FLOAD, 4,
                        FLOAD, 5,
                        FLOAD, 6,
                        FLOAD, 7,
                        FLOAD, 8,
                        reference(INVOKESPECIAL, renderEnchantment)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // if (!CITUtils18.renderArmorEnchantments(entity, model, itemStack, slot, ...,) {
                        ALOAD_1,
                        getCaptureGroup(1),
                        ALOAD, 10,
                        ILOAD, 9,
                        FLOAD_2,
                        FLOAD_3,
                        FLOAD, 5,
                        FLOAD, 6,
                        FLOAD, 7,
                        FLOAD, 8,

                        reference(INVOKESTATIC, newRenderArmorEnchantments),
                        IFNE, branch("A"),

                        // ...
                        getMatch(),

                        // }
                        label("A")
                    );
                }
            });
        }
    }

    private class ItemBlockMod extends ClassMod {
        ItemBlockMod() {
            final MethodRef getBlock = new MethodRef(getDeobfClass(), "getBlock", "()LBlock;");

            setParentClass("Item");

            addClassSignature(new ConstSignature("BlockEntityTag"));
            addClassSignature(new ConstSignature("x"));
            addClassSignature(new ConstSignature(0.5f));
            addClassSignature(new ConstSignature(0.8f));

            addMemberMapper(new MethodMapper(getBlock));
        }
    }

    private class EntityPotionMod extends ClassMod {
        EntityPotionMod() {
            setParentClass("EntityThrowable");

            addClassSignature(new ConstSignature("Potion"));
            addClassSignature(new ConstSignature("potionValue"));
        }
    }

    private class RenderItemFrameMod extends ClassMod {
        RenderItemFrameMod() {
            final MethodRef renderContents = new MethodRef(getDeobfClass(), "renderContents", "(LEntityItemFrame;)V");
            final MethodRef getDisplayedItem = new MethodRef("EntityItemFrame", "getDisplayedItem", "()LItemStack;");

            setParentClass("Render");

            addClassSignature(new ConstSignature("textures/map/map_background.png"));
            addClassSignature(new ConstSignature("item_frame"));
            addClassSignature(new ConstSignature("normal"));

            addClassSignature(new BytecodeSignature() {
                {
                    setMethod(renderContents);
                    addXref(1, getDisplayedItem);
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // itemStack = entity.getDisplayedItem();
                        begin(),
                        ALOAD_1,
                        captureReference(INVOKEVIRTUAL),
                        ASTORE_2
                    );
                }
            });

            addPatch(new BytecodePatch() {
                {
                    setInsertAfter(true);
                    targetMethod(renderContents);
                }

                @Override
                public String getDescription() {
                    return "pre render";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // itemStack = entity.getDisplayedItem();
                        begin(),
                        ALOAD_1,
                        captureReference(INVOKEVIRTUAL),
                        ASTORE_2
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // CITUtils18.preRender(itemStack);
                        ALOAD_2,
                        reference(INVOKESTATIC, preRenderItem)
                    );
                }
            });

            addPatch(new BytecodePatch() {
                {
                    setInsertBefore(true);
                    targetMethod(renderContents);
                }

                @Override
                public String getDescription() {
                    return "post render";
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
                        // CITUtils18.postRender();
                        reference(INVOKESTATIC, postRenderItem)
                    );
                }
            });
        }
    }
}
