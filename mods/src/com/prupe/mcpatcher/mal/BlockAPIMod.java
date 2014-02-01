package com.prupe.mcpatcher.mal;

import com.prupe.mcpatcher.FieldRef;
import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.MethodRef;
import com.prupe.mcpatcher.Mod;
import com.prupe.mcpatcher.basemod.*;
import javassist.bytecode.MethodInfo;

import static com.prupe.mcpatcher.BinaryRegex.*;
import static com.prupe.mcpatcher.BytecodeMatcher.*;
import static javassist.bytecode.Opcode.*;

public class BlockAPIMod extends Mod {
    private final int malVersion;
    private final MethodRef getBlockIcon = new MethodRef("Block", "getBlockIcon", "(LIBlockAccess;" + PositionMod.getDescriptor() + DirectionMod.getDescriptor() + ")LIcon;");
    private final MethodRef getSecondaryBlockIcon = RenderBlocksMod.haveSubclasses() ? new MethodRef("Block", "getSecondaryBlockIcon", "(LIBlockAccess;" + PositionMod.getDescriptor() + DirectionMod.getDescriptor() + ")LIcon;") : null;
    private final MethodRef useColorMultiplierOnFace = new MethodRef("Block", "useColorMultiplierOnFace", "(" + DirectionMod.getDescriptor() + ")Z");

    private static final MethodRef useColorMultiplier = new MethodRef(MCPatcherUtils.RENDER_BLOCKS_UTILS_CLASS, "useColorMultiplier", "(I)Z");

    public BlockAPIMod() {
        name = MCPatcherUtils.BLOCK_API_MOD;
        author = "MCPatcher";
        description = "Internal mod required by the patcher.";

        if (PositionMod.havePositionClass()) {
            malVersion = 3;
        } else if (getMinecraftVersion().compareTo("13w36a") >= 0) {
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
        if (getMinecraftVersion().compareTo("14w05a") >= 0) {
            addClassMod(new RenderBlockHelperMod());
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

            final MethodRef shouldSideBeRendered = new MethodRef(getDeobfClass(), "shouldSideBeRendered", "(LIBlockAccess;" + PositionMod.getDescriptor() + DirectionMod.getDescriptor() + ")Z");

            if (getSecondaryBlockIcon == null) {
                addMemberMapper(new MethodMapper(getBlockIcon));
            } else {
                addMemberMapper(new MethodMapper(getSecondaryBlockIcon, getBlockIcon));
            }
            addMemberMapper(new MethodMapper(shouldSideBeRendered));

            if (malVersion >= 2) {
                final FieldRef blockRegistry = new FieldRef(getDeobfClass(), "blockRegistry", "LRegistry;");

                addMemberMapper(new FieldMapper(blockRegistry));
            }

            if (RenderBlocksMod.haveSubclasses()) {
                addMemberMapper(new MethodMapper(useColorMultiplierOnFace));
            }
        }
    }

    abstract private class BetterGrassMod extends ClassMod {
        protected final MethodRef isBetterGrass = new MethodRef(MCPatcherUtils.RENDER_BLOCKS_UTILS_CLASS, "isBetterGrass", "(LBlock;LIBlockAccess;IIII)Z");

        BetterGrassMod() {
            setParentClass("Block");

            addClassSignature(new ConstSignature("_side"));
            addClassSignature(new ConstSignature("_top"));

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
                    return buildCode(
                        // if (BlockAPI.isBetterGrass(this, blockAccess, i, j, k, face)) {
                        ALOAD_0,
                        ALOAD_1,
                        PositionMod.unpackArguments(this, 2),
                        DirectionMod.unpackArguments(this, 2 + PositionMod.getDescriptorLength()),
                        reference(INVOKESTATIC, isBetterGrass),
                        IFEQ, branch("A"),

                        // face = 1;
                        DirectionMod.getFixedDirection(this, 1),
                        registerLoadStore(DirectionMod.getStoreOpcode(), 2 + PositionMod.getDescriptorLength()),

                        // }
                        label("A")
                    );
                }
            }.targetMethod(getBlockIcon));
        }
    }

    private class BlockGrassMod extends BetterGrassMod {
        BlockGrassMod() {
            addClassSignature(new ConstSignature("_side_snowed"));
            addClassSignature(new ConstSignature("_side_overlay"));

            if (getSecondaryBlockIcon != null) {
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
                        return buildCode(
                            // if (BlockAPI.isBetterGrass(this, blockAccess, i, j, k, face)) {
                            ALOAD_0,
                            ALOAD_1,
                            PositionMod.unpackArguments(this, 2),
                            DirectionMod.unpackArguments(this, 2 + PositionMod.getDescriptorLength()),
                            reference(INVOKESTATIC, isBetterGrass),
                            IFEQ, branch("A"),

                            // return null;
                            ACONST_NULL,
                            ARETURN,

                            // }
                            label("A")
                        );
                    }
                }.targetMethod(getSecondaryBlockIcon));
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
        private final MethodRef setColorOpaque_F = new MethodRef("Tessellator", "setColorOpaque_F", "(FFF)V");
        private final MethodRef setupColorMultiplier = new MethodRef(MCPatcherUtils.RENDER_BLOCKS_UTILS_CLASS, "setupColorMultiplier", "(LBlock;LIBlockAccess;IIIZFFF)V");
        private final MethodRef getColorMultiplierRed = new MethodRef(MCPatcherUtils.RENDER_BLOCKS_UTILS_CLASS, "getColorMultiplierRed", "(I)F");
        private final MethodRef getColorMultiplierGreen = new MethodRef(MCPatcherUtils.RENDER_BLOCKS_UTILS_CLASS, "getColorMultiplierGreen", "(I)F");
        private final MethodRef getColorMultiplierBlue = new MethodRef(MCPatcherUtils.RENDER_BLOCKS_UTILS_CLASS, "getColorMultiplierBlue", "(I)F");

        RenderBlocksMod() {
            super(BlockAPIMod.this);

            mapRenderStandardBlock();
            mapHasOverrideTexture();

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "set per-face color multipliers";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        reference(INVOKESTATIC, isAmbientOcclusionEnabled)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    int baseRegister = 3 + PositionMod.getDescriptorLength();
                    return buildCode(
                        // RenderBlocksUtils.setupColorMultiplier(block, this.blockAccess, i, j, k, this.hasOverrideTexture(), r, g, b);
                        ALOAD_1,
                        ALOAD_0,
                        reference(GETFIELD, blockAccess),
                        PositionMod.unpackArguments(this, 2),
                        ALOAD_0,
                        reference(INVOKEVIRTUAL, hasOverrideBlockTexture),
                        FLOAD, baseRegister,
                        FLOAD, baseRegister + 1,
                        FLOAD, baseRegister + 2,
                        reference(INVOKESTATIC, setupColorMultiplier)
                    );
                }
            }
                .setInsertBefore(true)
                .targetMethod(renderStandardBlock)
            );

            if (haveSubclasses()) {
                setupColorMultipliers18(this);
            } else {
                setupColorMultipliers17();
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
                        reference(INVOKESTATIC, useColorMultiplier)
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
                        reference(INVOKEVIRTUAL, setColorOpaque_F)
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
                        reference(INVOKEVIRTUAL, setColorOpaque_F)
                    );
                    patchCount++;
                    return code;
                }
            });
        }
    }

    private class RenderBlockHelperMod extends ClassMod {
        RenderBlockHelperMod() {
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
            });

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
                    reference(INVOKEVIRTUAL, useColorMultiplierOnFace)
                );
            }

            @Override
            public byte[] getReplacementBytes() {
                return buildCode(
                    // RenderBlocksUtils.useColorMultiplier(direction)
                    getCaptureGroup(1),
                    reference(INVOKEVIRTUAL, DirectionMod.ordinal),
                    reference(INVOKESTATIC, useColorMultiplier)
                );
            }
        });
    }
}
