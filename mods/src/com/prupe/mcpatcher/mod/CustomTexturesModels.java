package com.prupe.mcpatcher.mod;

import com.prupe.mcpatcher.*;
import com.prupe.mcpatcher.basemod.*;
import com.prupe.mcpatcher.basemod.ext18.*;
import com.prupe.mcpatcher.mal.TexturePackAPIMod;
import javassist.bytecode.AccessFlag;

import static com.prupe.mcpatcher.BinaryRegex.*;
import static com.prupe.mcpatcher.BytecodeMatcher.*;
import static javassist.bytecode.Opcode.*;

public class CustomTexturesModels extends Mod {
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
        addClassMod(new IBlockAccessMod(this));
        addClassMod(new IBlockStateMod(this));
        addClassMod(new TessellatorMod(this));
        addClassMod(new TessellatorFactoryMod(this));
        addClassMod(new BiomeGenBaseMod(this));
        addClassMod(new DirectionWithAOMod(this));
        addClassMod(new IModelMod(this));
        addClassMod(new ModelFaceMod(this));

        addClassMod(new BlockMod());
        addClassMod(new RenderBlockCustomMod());
        addClassMod(new RenderBlockCustomInnerMod());
        addClassMod(new ModelFaceSpriteMod());

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

            final MethodRef colorMultiplier = new MethodRef(getDeobfClass(), "colorMultiplier", "(LIBlockAccess;LPosition;I)I");

            addMemberMapper(new MethodMapper(colorMultiplier));
        }
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
        }

        private FieldRef addInitializedField(final String name, final String type) {
            final FieldRef field = new FieldRef(getDeobfClass(), name, "L" + type + ";");

            addPatch(new AddFieldPatch(field, AccessFlag.PUBLIC | AccessFlag.FINAL));

            addPatch(new BytecodePatch() {
                {
                    setInsertBefore(true);
                    matchConstructorOnly(true);
                }

                @Override
                public String getDescription() {
                    return "initialize " + name;
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
                        // this.field = new ClassName(this);
                        ALOAD_0,
                        reference(NEW, new ClassRef(type)),
                        DUP,
                        ALOAD_0,
                        reference(INVOKESPECIAL, new MethodRef(type, "<init>", "(L" + getDeobfClass() + ";)V")),
                        reference(PUTFIELD, field)
                    );
                }
            });

            addPatch(new BytecodePatch() {
                {
                    setInsertAfter(true);
                    targetMethod(renderBlock);
                }

                @Override
                public String getDescription() {
                    return "set up " + name + " for render";
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
                        // if (!this.field.preRender(blockAccess, model, blockState, useAO)) {
                        ALOAD_0,
                        reference(GETFIELD, field),
                        ALOAD_1,
                        ALOAD_2,
                        ALOAD_3,
                        registerLoadStore(ILOAD, register - 1),
                        reference(INVOKEVIRTUAL, new MethodRef(type, "preRender", "(LIBlockAccess;LIModel;LIBlockState;Z)Z")),
                        IFNE, branch("A"),

                        // return false;
                        push(false),
                        IRETURN,

                        // }
                        label("A")
                    );
                }
            });

            return field;
        }

        private void setupColorMaps() {
            final MethodRef useColormap = new MethodRef("ModelFace", "useColormap", "()Z");
            final MethodRef newUseColormap = new MethodRef(MCPatcherUtils.COLORIZE_BLOCK18_CLASS, "useColormap", "(ZLBlock;)Z");

            addInitializedField("ccInfo", MCPatcherUtils.COLORIZE_BLOCK18_CLASS);

            addPatch(new BytecodePatch() {
                {
                    setInsertAfter(true);
                    targetMethod(renderFaceAO, renderFaceNonAO);
                }

                @Override
                public String getDescription() {
                    return "override useColormap";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        reference(INVOKEVIRTUAL, useColormap)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        ALOAD_2,
                        reference(INVOKESTATIC, newUseColormap)
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

    private class ModelFaceSpriteMod extends com.prupe.mcpatcher.basemod.ext18.ModelFaceSpriteMod {
        ModelFaceSpriteMod() {
            super(CustomTexturesModels.this);

            addPatch(new MakeMemberPublicPatch(sprite));
        }
    }
}
