package com.prupe.mcpatcher.mod;

import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.MethodRef;
import com.prupe.mcpatcher.Mod;
import com.prupe.mcpatcher.basemod.*;
import com.prupe.mcpatcher.mal.TexturePackAPIMod;

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
        addClassMod(new TessellatorMod(this));
        addClassMod(new TessellatorFactoryMod(this));
        addClassMod(new BiomeGenBaseMod(this));

        addClassMod(new BlockMod(this));
        addClassMod(new RenderBlocks18Mod());
        addClassMod(new ModelFaceMod());
        addClassMod(new ModelFaceSpriteMod());

        addClassMod(new ItemMod(this));

        addClassFiles("com.prupe.mcpatcher.colormap.*");
        addClassFiles("com.prupe.mcpatcher.ctm.*");
        addClassFiles("com.prupe.mcpatcher.cit.*");

        TexturePackAPIMod.earlyInitialize(2, MCPatcherUtils.CTM_UTILS18_CLASS, "reset");
    }

    @Override
    public String[] getLoggingCategories() {
        return new String[]{
            MCPatcherUtils.CONNECTED_TEXTURES,
            MCPatcherUtils.CUSTOM_ITEM_TEXTURES
        };
    }

    private class RenderBlocks18Mod extends ClassMod {
        RenderBlocks18Mod() {
            final MethodRef renderBlock = new MethodRef(getDeobfClass(), "renderBlock", "(LIBlockAccess;LIModel;LIBlockState;LPosition;LTessellator;Z)Z");
            final MethodRef renderBlockAO = new MethodRef(getDeobfClass(), "renderBlockAO", "(LIBlockAccess;LIModel;LBlock;LPosition;LTessellator;Z)Z");
            final MethodRef renderBlockNonAO = new MethodRef(getDeobfClass(), "renderBlockNonAO", "(LIBlockAccess;LIModel;LBlock;LPosition;LTessellator;Z)Z");
            final MethodRef renderFaceAO = new MethodRef(getDeobfClass(), "renderFaceAO", "(LIBlockAccess;LBlock;LPosition;LTessellator;Ljava/util/List;[FLjava/util/BitSet;LRenderBlocks18Inner;)V");
            final MethodRef renderFaceNonAO = new MethodRef(getDeobfClass(), "renderFaceNonAO", "(LIBlockAccess;LBlock;LPosition;LDirection;IZLTessellator;Ljava/util/List;Ljava/util/BitSet;)V");

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
        }
    }

    private class ModelFaceMod extends ClassMod {
        ModelFaceMod() {
            addPrerequisiteClass("ModelFaceSprite");

            addClassSignature(new InterfaceSignature(
                new MethodRef(getDeobfClass(), "<init>", "([IILDirection;)V"),
                new MethodRef(getDeobfClass(), "getIntBuffer", "()[I"),
                new MethodRef(getDeobfClass(), "useColormap", "()Z"),
                new MethodRef(getDeobfClass(), "getColor", "()I"),
                new MethodRef(getDeobfClass(), "getDirection", "()LDirection;")
            ).setInterfaceOnly(false));
        }
    }

    private class ModelFaceSpriteMod extends ClassMod {
        ModelFaceSpriteMod() {
            setParentClass("ModelFace");

            addClassSignature(new ConstSignature(new MethodRef("java/util/Arrays", "copyOf", "([II)[I")));
            addClassSignature(new ConstSignature(new MethodRef("java/lang/Float", "intBitsToFloat", "(I)F")));
            addClassSignature(new ConstSignature(16.0f));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // j = 7 * i;
                        begin(),
                        push(7),
                        ILOAD_1,
                        IMUL,
                        ISTORE_2
                    );
                }
            });
        }
    }
}
