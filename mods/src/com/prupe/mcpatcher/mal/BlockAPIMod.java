package com.prupe.mcpatcher.mal;

import com.prupe.mcpatcher.*;
import com.prupe.mcpatcher.basemod.*;
import javassist.bytecode.MethodInfo;

import static com.prupe.mcpatcher.BinaryRegex.*;
import static com.prupe.mcpatcher.BytecodeMatcher.*;
import static javassist.bytecode.Opcode.*;

public class BlockAPIMod extends Mod {
    private final int malVersion;

    public static final MethodRef useColorMultiplier1 = new MethodRef(MCPatcherUtils.RENDER_BLOCKS_UTILS_CLASS, "useColorMultiplier", "(I)Z");
    public static final MethodRef useColorMultiplier2 = new MethodRef(MCPatcherUtils.RENDER_BLOCKS_UTILS_CLASS, "useColorMultiplier", "(ZI)Z");
    public static final FieldRef layerIndex = new FieldRef(MCPatcherUtils.RENDER_BLOCKS_UTILS_CLASS, "layerIndex", "I");

    public BlockAPIMod() {
        name = MCPatcherUtils.BLOCK_API_MOD;
        author = "MCPatcher";
        description = "Internal mod required by the patcher.";

        if (PositionMod.havePositionClass()) {
            malVersion = 3;
        } else if (BlockMod.haveBlockRegistry()) {
            malVersion = 2;
        } else {
            malVersion = 1;
        }
        version = String.valueOf(malVersion) + ".0";
        setMALVersion("block", malVersion);

        addClassMod(new BlockMod());
        addClassMod(new BlockGrassMod());
        addClassMod(new BlockMyceliumMod());
        addClassMod(new IBlockAccessMod(this));
        addClassMod(new IconMod(this));
        addClassMod(new TessellatorMod(this));
        addClassMod(new ResourceLocationMod(this));
        addClassMod(new RenderBlocksMod());
        if (RenderBlocksMod.haveSubclasses()) {
            addClassMod(new RenderBlockManagerMod(this));
        }
        if (RenderBlockHelperMod.haveClass() && !RenderBlockCustomMod.haveCustomModels()) {
            addClassMod(new RenderBlockHelperMod());
        }
        if (RenderBlockCustomMod.haveCustomModels()) {
            addClassMod(new RenderBlockCustomMod());
        }
        if (malVersion >= 2) {
            addClassMod(new Shared.RegistryBaseMod(this));
            addClassMod(new Shared.RegistryMod(this));
        }
        if (malVersion >= 3) {
            PositionMod.setup(this);
        }

        addClassFile(MCPatcherUtils.BLOCK_API_CLASS);
        addClassFile(MCPatcherUtils.BLOCK_API_CLASS + "$1");
        addClassFile(MCPatcherUtils.BLOCK_API_CLASS + "$V" + malVersion);
        if (malVersion == 3) {
            addClassFile(MCPatcherUtils.BLOCK_API_CLASS + "$V2");
        }
        addClassFile(MCPatcherUtils.BLOCK_AND_METADATA_CLASS);
        addClassFile(MCPatcherUtils.RENDER_PASS_API_MAL_CLASS);
        addClassFile(MCPatcherUtils.RENDER_BLOCKS_UTILS_CLASS);
    }

    @Override
    public String[] getLoggingCategories() {
        return null;
    }

    private class BlockMod extends com.prupe.mcpatcher.basemod.BlockMod {
        BlockMod() {
            super(BlockAPIMod.this);

            mapBlockIconMethods();
            addMemberMapper(new MethodMapper(shouldSideBeRendered));

            if (malVersion >= 2) {
                addMemberMapper(new FieldMapper(blockRegistry));
            }

            if (RenderBlocksMod.haveSubclasses()) {
                addMemberMapper(new MethodMapper(useColorMultiplierOnFace));
            }
        }
    }

    abstract private class BetterGrassMod extends ClassMod {
        protected final MethodRef getGrassTexture = new MethodRef(MCPatcherUtils.RENDER_BLOCKS_UTILS_CLASS, "getGrassTexture", "(LBlock;LIBlockAccess;IIIILIcon;)LIcon;");
        private final FieldRef topIcon = new FieldRef(getDeobfClass(), "topIcon", "LIcon;");

        BetterGrassMod() {
            setParentClass("Block");

            addClassSignature(new ConstSignature("_side"));
            addClassSignature(new ConstSignature("_top"));

            addClassSignature(new BytecodeSignature() {
                final ClassRef stringBuilderClass = new ClassRef("java/lang/StringBuilder");

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // this.field = iconRegister.registerIcon(this.getTextureName() + name);
                        ALOAD_0,
                        ALOAD_1,
                        // new StringBuilder(this.getTextureName()).append(suffix).toString();
                        // -or-
                        // new StringBuilder().append(this.getTextureName()).append(suffix).toString();
                        reference(NEW, stringBuilderClass),
                        DUP,
                        nonGreedy(any(0, 12)),
                        push("_top"),
                        nonGreedy(any(0, 8)),
                        anyReference(INVOKEINTERFACE),
                        captureReference(PUTFIELD)
                    );
                }
            }.addXref(1, topIcon));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override side texture";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        begin()
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    int iconRegister = getMethodInfo().getCodeAttribute().getMaxLocals();
                    return buildCode(
                        // Icon newIcon = RenderBlocksUtils.getGrassTexture(...);
                        getGrassTexture(this, iconRegister),

                        // if (newIcon != null) {
                        registerLoadStore(ALOAD, iconRegister),
                        IFNULL, branch("A"),

                        // return newIcon;
                        registerLoadStore(ALOAD, iconRegister),
                        ARETURN,

                        // }
                        label("A")
                    );
                }
            }.targetMethod(BlockMod.getBlockIcon));
        }

        protected byte[] getGrassTexture(BytecodePatch patch, int iconRegister) {
            return patch.buildCode(
                // Icon newIcon = RenderBlocksUtils.getGrassTexture(this, blockAccess, i, j, k, face, this.topIcon);
                ALOAD_0,
                ALOAD_1,
                PositionMod.unpackArguments(this, 2),
                DirectionMod.unpackArguments(this, 2 + PositionMod.getDescriptorLength()),
                ALOAD_0,
                reference(GETFIELD, topIcon),
                reference(INVOKESTATIC, getGrassTexture),
                registerLoadStore(ASTORE, iconRegister)
            );
        }
    }

    private class BlockGrassMod extends BetterGrassMod {
        BlockGrassMod() {
            addClassSignature(new ConstSignature("_side_snowed"));
            addClassSignature(new ConstSignature("_side_overlay"));

            if (BlockMod.getSecondaryBlockIcon != null) {
                addPatch(new BytecodePatch() {
                    @Override
                    public String getDescription() {
                        return "disable overlay texture if better grass";
                    }

                    @Override
                    public String getMatchExpression() {
                        return buildExpression(
                            begin()
                        );
                    }

                    @Override
                    public byte[] getReplacementBytes() {
                        int iconRegister = getMethodInfo().getCodeAttribute().getMaxLocals();
                        return buildCode(
                            // Icon newIcon = RenderBlocksUtils.getGrassTexture(...);
                            getGrassTexture(this, iconRegister),

                            // if (newIcon != null) {
                            ALOAD, iconRegister,
                            IFNULL, branch("A"),

                            // return null;
                            ACONST_NULL,
                            ARETURN,

                            // }
                            label("A")
                        );
                    }
                }.targetMethod(BlockMod.getSecondaryBlockIcon));
            }
        }
    }

    private class BlockMyceliumMod extends BetterGrassMod {
        BlockMyceliumMod() {
            addClassSignature(new ConstSignature("grass_side_snowed"));
            addClassSignature(new ConstSignature("townaura"));
        }
    }

    private class RenderBlocksMod extends com.prupe.mcpatcher.basemod.RenderBlocksMod {
        private final MethodRef getColorMultiplierRed = new MethodRef(MCPatcherUtils.RENDER_BLOCKS_UTILS_CLASS, "getColorMultiplierRed", "(I)F");
        private final MethodRef getColorMultiplierGreen = new MethodRef(MCPatcherUtils.RENDER_BLOCKS_UTILS_CLASS, "getColorMultiplierGreen", "(I)F");
        private final MethodRef getColorMultiplierBlue = new MethodRef(MCPatcherUtils.RENDER_BLOCKS_UTILS_CLASS, "getColorMultiplierBlue", "(I)F");

        RenderBlocksMod() {
            super(BlockAPIMod.this);

            mapRenderStandardBlock();
            mapHasOverrideTexture();

            if (!RenderBlockCustomMod.haveCustomModels()) {
                presetupColorMultipliers(this);

                if (haveSubclasses()) {
                    setupColorMultipliers18(this);
                } else {
                    setupColorMultipliers17();
                }
            }
        }

        private void setupColorMultipliers17() {
            addPatch(new BytecodePatch() {
                private MethodInfo lastMethod;
                private int patchCount;

                {
                    addPreMatchSignature(grassTopSignature);
                }

                @Override
                public String getDescription() {
                    return "use per-face color multiplier flags";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // useColor
                        ILOAD, useColorRegister
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // RenderBlocksUtils.useColorMultiplier(face)
                        push(getPatchCount()),
                        reference(INVOKESTATIC, useColorMultiplier1)
                    );
                }

                private int getPatchCount() {
                    if (lastMethod != getMethodInfo()) {
                        lastMethod = getMethodInfo();
                        patchCount = 0;
                    }
                    int oldPatchCount = patchCount;
                    patchCount++;
                    if (patchCount == 1) {
                        patchCount++;
                    }
                    patchCount %= 6;
                    return oldPatchCount;
                }
            });

            addPatch(new BytecodePatch() {
                private int patchCount;

                {
                    addPreMatchSignature(new BytecodeSignature() {
                        @Override
                        public String getMatchExpression() {
                            return buildExpression(
                                // if (block != Block.grass) {
                                ALOAD_1,
                                anyReference(GETSTATIC),
                                IF_ACMPEQ, any(2),

                                // floatValue *= red/green/blue;
                                // x9
                                getSubExpression(0),
                                getSubExpression(1),
                                getSubExpression(2),
                                getSubExpression(3),
                                getSubExpression(4),
                                getSubExpression(5),
                                getSubExpression(6),
                                getSubExpression(7),
                                getSubExpression(8)
                            );
                        }

                        private String getSubExpression(int index) {
                            return build(
                                FLOAD, capture(any()),
                                registerLoadStore(FLOAD, 2 + PositionMod.getDescriptorLength() + index / 3),
                                FMUL,
                                FSTORE, backReference(index + 1)
                            );
                        }
                    });
                }

                @Override
                public String getDescription() {
                    return "use per-face color multiplier flags (non-AO)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // tessellator.setColorOpaque_F(...);
                        capture(anyALOAD),
                        anyFLOAD,
                        anyFLOAD,
                        anyFLOAD,
                        reference(INVOKEVIRTUAL, TessellatorMod.setColorOpaque_F)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    byte[] code = buildCode(
                        // tessellator.setColorOpaque_F(RenderBlocksUtils.getColorMultiplierRed(face), ...);
                        getCaptureGroup(1),
                        push(patchCount),
                        reference(INVOKESTATIC, getColorMultiplierRed),
                        push(patchCount),
                        reference(INVOKESTATIC, getColorMultiplierGreen),
                        push(patchCount),
                        reference(INVOKESTATIC, getColorMultiplierBlue),
                        reference(INVOKEVIRTUAL, TessellatorMod.setColorOpaque_F)
                    );
                    patchCount++;
                    return code;
                }
            });
        }
    }

    private class RenderBlockHelperMod extends com.prupe.mcpatcher.basemod.RenderBlockHelperMod {
        RenderBlockHelperMod() {
            super(BlockAPIMod.this);

            setupColorMultipliers18(this);
        }
    }

    private void setupColorMultipliers18(com.prupe.mcpatcher.ClassMod classMod) {
        classMod.addPatch(new com.prupe.mcpatcher.BytecodePatch(classMod) {
            @Override
            public String getDescription() {
                return "use per-face color multiplier flags";
            }

            @Override
            public String getMatchExpression() {
                return buildExpression(
                    // block.useColorMultiplierOnFace(Direction.xxx)
                    ALOAD_1,
                    capture(or(anyReference(GETSTATIC), anyALOAD)),
                    reference(INVOKEVIRTUAL, BlockMod.useColorMultiplierOnFace)
                );
            }

            @Override
            public byte[] getReplacementBytes() {
                return buildCode(
                    // RenderBlocksUtils.useColorMultiplier(direction)
                    getCaptureGroup(1),
                    reference(INVOKEVIRTUAL, DirectionMod.ordinal),
                    reference(INVOKESTATIC, useColorMultiplier1)
                );
            }
        });
    }

    private void presetupColorMultipliers(com.prupe.mcpatcher.ClassMod classMod) {
        classMod.addPatch(new com.prupe.mcpatcher.BytecodePatch(classMod) {
            private final MethodRef setupColorMultiplier = new MethodRef(MCPatcherUtils.RENDER_BLOCKS_UTILS_CLASS, "setupColorMultiplier", "(LBlock;LIBlockAccess;IIIZFFF)V");

            @Override
            public String getDescription() {
                return "set per-face color multipliers";
            }

            @Override
            public String getMatchExpression() {
                return buildExpression(
                    reference(INVOKESTATIC, MinecraftMod.isAmbientOcclusionEnabled)
                );
            }

            @Override
            public byte[] getReplacementBytes() {
                int baseRegister = 3 + PositionMod.getDescriptorLength();
                return buildCode(
                    // RenderBlocksUtils.setupColorMultiplier(block, this.blockAccess, i, j, k, this.hasOverrideTexture(), r, g, b);
                    ALOAD_1,
                    ALOAD_0,
                    reference(GETFIELD, RenderBlocksMod.blockAccess),
                    PositionMod.unpackArguments(this, 2),
                    ALOAD_0,
                    reference(INVOKEVIRTUAL, RenderBlocksMod.hasOverrideBlockTexture),
                    FLOAD, baseRegister,
                    FLOAD, baseRegister + 1,
                    FLOAD, baseRegister + 2,
                    reference(INVOKESTATIC, setupColorMultiplier)
                );
            }
        }
            .setInsertBefore(true)
            .targetMethod(RenderBlocksMod.renderStandardBlock)
        );
    }

    private class RenderBlockCustomMod extends com.prupe.mcpatcher.basemod.RenderBlockCustomMod {
        RenderBlockCustomMod() {
            super(BlockAPIMod.this);

            final MethodRef useTint = new MethodRef("BlockModelFace", "useTint", "()Z");
            final InterfaceMethodRef listIterator = new InterfaceMethodRef("java/util/List", "iterator", "()Ljava/util/Iterator;");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // older: if (faces[index].useTint()) {
                        // 14w07a+: if (face.useTint()) {
                        anyALOAD,
                        captureReference(INVOKEVIRTUAL),
                        IFEQ, any(2),

                        // older: tessellator.setVertexColor(this.colorxxx * r, ...);
                        // 14w07a+: tessellator.setVertexColor(RenderBlockCustomHelper.getxxx(this.helper)[0] * r, ...);
                        anyALOAD,
                        ALOAD_0,
                        anyReference(GETFIELD),
                        anyReference(INVOKESTATIC),
                        push(0),
                        FALOAD,
                        FLOAD, 5,
                        FMUL
                    );
                }
            }
                .setMethod(renderFaceAO)
                .addXref(1, useTint)
            );

            presetupColorMultipliers(this);

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "initialize layer index";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // iterator = faces.iterator();
                        anyALOAD,
                        reference(INVOKEINTERFACE, listIterator),
                        anyASTORE
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // RenderBlocksUtils.layerIndex = 0;
                        push(0),
                        reference(PUTSTATIC, layerIndex)
                    );
                }
            }
                .setInsertAfter(true)
                .targetMethod(renderFaceAO, renderFaceNonAO, renderBlockHeld)
            );

            addPatch(new BytecodePatch() {
                private boolean isMetadataMethod;
                private int direction;

                {
                    addPreMatchSignature(new BytecodeSignature() {
                        @Override
                        public String getMatchExpression() {
                            if ((isMetadataMethod = getMethodInfo().getDescriptor().endsWith("IF)V"))) {
                                return buildExpression(
                                    // direction = face.getUVDirection()
                                    anyALOAD,
                                    anyReference(INVOKEVIRTUAL),
                                    ASTORE, capture(any()),

                                    // icon = this.getBlockTexture(block, direction, metadata);
                                    ALOAD_0,
                                    ALOAD_1,
                                    ALOAD, backReference(1),
                                    ILOAD_2,
                                    anyReference(INVOKEVIRTUAL),
                                    anyASTORE
                                );
                            } else {
                                return "";
                            }
                        }

                        @Override
                        public boolean afterMatch() {
                            if (isMetadataMethod) {
                                direction = getCaptureGroup(1)[0] & 0xff;
                            } else {
                                direction = 4;
                            }
                            return true;
                        }
                    });
                }

                @Override
                public String getDescription() {
                    return "override tint flag";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // face.useTint()
                        reference(INVOKEVIRTUAL, useTint)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // RenderBlocksUtils.useColorMultiplier(..., direction.ordinal())
                        DirectionMod.unpackArgumentsSafe(this, direction),
                        reference(INVOKESTATIC, useColorMultiplier2)
                    );
                }
            }
                .setInsertAfter(true)
                .targetMethod(renderFaceAO, renderFaceNonAO, renderBlockHeld)
            );
        }
    }
}
