package com.prupe.mcpatcher.mal;

import com.prupe.mcpatcher.*;
import javassist.bytecode.MethodInfo;

import static com.prupe.mcpatcher.BinaryRegex.*;
import static com.prupe.mcpatcher.BytecodeMatcher.*;
import static javassist.bytecode.Opcode.*;

public class BlockAPIMod extends Mod {
    private static final MethodRef getBlockIcon = new MethodRef("Block", "getBlockIcon", "(LIBlockAccess;IIII)LIcon;");

    private final int malVersion;

    public BlockAPIMod() {
        name = MCPatcherUtils.BLOCK_API_MOD;
        author = "MCPatcher";
        description = "Internal mod required by the patcher.";

        if (getMinecraftVersion().compareTo("13w36a") < 0) {
            malVersion = 1;
        } else {
            malVersion = 2;
        }
        version = String.valueOf(malVersion) + ".0";
        setMALVersion("block", malVersion);

        addClassMod(new BlockMod());
        addClassMod(new BlockGrassMod());
        addClassMod(new BlockMyceliumMod());
        addClassMod(new BaseMod.IBlockAccessMod(this));
        addClassMod(new BaseMod.IconMod(this));
        addClassMod(new BaseMod.TessellatorMod(this));
        addClassMod(new RenderBlocksMod());
        if (malVersion >= 2) {
            addClassMod(new Shared.RegistryBaseMod(this));
            addClassMod(new Shared.RegistryMod(this));
        }

        addClassFile(MCPatcherUtils.BLOCK_API_CLASS);
        addClassFile(MCPatcherUtils.BLOCK_API_CLASS + "$V" + malVersion);
        addClassFile(MCPatcherUtils.RENDER_PASS_API_MAL_CLASS);
        addClassFile(MCPatcherUtils.RENDER_BLOCKS_UTILS_CLASS);
    }

    @Override
    public String[] getLoggingCategories() {
        return null;
    }

    private class BlockMod extends BaseMod.BlockMod {
        BlockMod() {
            super(BlockAPIMod.this);

            final MethodRef getShortName = new MethodRef(getDeobfClass(), "getShortName", "()Ljava/lang/String;");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push("tile.")
                    );
                }
            }.setMethod(getShortName));

            addMemberMapper(new MethodMapper(getBlockIcon));

            if (malVersion == 2) {
                final FieldRef blockRegistry = new FieldRef(getDeobfClass(), "blockRegistry", "LRegistry;");

                addMemberMapper(new FieldMapper(blockRegistry));
            }
        }
    }

    abstract private class BetterGrassMod extends ClassMod {
        BetterGrassMod() {
            setParentClass("Block");

            addClassSignature(new ConstSignature("_side"));
            addClassSignature(new ConstSignature("_top"));

            final MethodRef isBetterGrass = new MethodRef(MCPatcherUtils.RENDER_BLOCKS_UTILS_CLASS, "isBetterGrass", "(LBlock;LIBlockAccess;IIII)Z");

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
                        ILOAD_2,
                        ILOAD_3,
                        ILOAD, 4,
                        ILOAD, 5,
                        reference(INVOKESTATIC, isBetterGrass),
                        IFEQ, branch("A"),

                        // face = 1;
                        push(1),
                        ISTORE, 5,

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
        }
    }

    private class BlockMyceliumMod extends BetterGrassMod {
        BlockMyceliumMod() {
            addClassSignature(new ConstSignature("grass_side_snowed"));
            addClassSignature(new ConstSignature("townaura"));
        }
    }

    private class RenderBlocksMod extends BaseMod.RenderBlocksMod {
        private int useColorRegister;

        RenderBlocksMod() {
            super(BlockAPIMod.this);

            final MethodRef hasOverrideBlockTexture = new MethodRef(getDeobfClass(), "hasOverrideBlockTexture", "Z");
            final MethodRef renderStandardBlock = new MethodRef(getDeobfClass(), "renderStandardBlock", "(LBlock;III)Z");
            final MethodRef isAmbientOcclusionEnabled = new MethodRef("Minecraft", "isAmbientOcclusionEnabled", "()Z");
            final MethodRef setColorOpaque_F = new MethodRef("Tessellator", "setColorOpaque_F", "(FFF)V");
            final MethodRef strEquals = new MethodRef("java/lang/String", "equals", "(Ljava/lang/Object;)Z");
            final MethodRef setupColorMultiplier = new MethodRef(MCPatcherUtils.RENDER_BLOCKS_UTILS_CLASS, "setupColorMultiplier", "(LBlock;LIBlockAccess;IIIZFFF)V");
            final MethodRef useColorMultiplier = new MethodRef(MCPatcherUtils.RENDER_BLOCKS_UTILS_CLASS, "useColorMultiplier", "(I)Z");
            final MethodRef getColorMultiplierRed = new MethodRef(MCPatcherUtils.RENDER_BLOCKS_UTILS_CLASS, "getColorMultiplierRed", "(I)F");
            final MethodRef getColorMultiplierGreen = new MethodRef(MCPatcherUtils.RENDER_BLOCKS_UTILS_CLASS, "getColorMultiplierGreen", "(I)F");
            final MethodRef getColorMultiplierBlue = new MethodRef(MCPatcherUtils.RENDER_BLOCKS_UTILS_CLASS, "getColorMultiplierBlue", "(I)F");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        captureReference(INVOKESTATIC),
                        IFEQ, any(2)
                    );
                }
            }
                .setMethod(renderStandardBlock)
                .addXref(1, isAmbientOcclusionEnabled)
            );

            final BytecodeSignature grassTopSignature = new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push("grass_top"),
                        reference(INVOKEVIRTUAL, strEquals),
                        IFEQ, any(2),
                        // useColor = false;
                        push(0),
                        capture(anyISTORE)
                    );
                }

                @Override
                public boolean afterMatch() {
                    useColorRegister = extractRegisterNum(getCaptureGroup(1));
                    return true;
                }
            };

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
                    return buildCode(
                        ALOAD_1,
                        ALOAD_0,
                        reference(GETFIELD, blockAccess),
                        ILOAD_2,
                        ILOAD_3,
                        ILOAD, 4,
                        ALOAD_0,
                        reference(INVOKEVIRTUAL, hasOverrideBlockTexture),
                        FLOAD, 6,
                        FLOAD, 7,
                        FLOAD, 8,
                        reference(INVOKESTATIC, setupColorMultiplier)
                    );
                }
            }
                .setInsertBefore(true)
                .targetMethod(renderStandardBlock)
            );

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
                        ILOAD, useColorRegister
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
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
                                ALOAD_1,
                                anyReference(GETSTATIC),
                                IF_ACMPEQ, any(2),

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
                                FLOAD, 5 + index / 3,
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
                        getCaptureGroup(1),
                        push(patchCount),
                        reference(INVOKESTATIC, getColorMultiplierRed),
                        push(patchCount),
                        reference(INVOKESTATIC, getColorMultiplierGreen),
                        push(patchCount),
                        reference(INVOKESTATIC, getColorMultiplierBlue)
                    );
                    patchCount++;
                    return code;
                }
            });
        }
    }
}
