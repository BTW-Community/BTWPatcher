package com.prupe.mcpatcher.mod.cc;

import com.prupe.mcpatcher.*;
import com.prupe.mcpatcher.basemod.ext18.DirectionMod;
import com.prupe.mcpatcher.basemod.ext18.PositionMod;
import com.prupe.mcpatcher.basemod.ResourceLocationMod;
import com.prupe.mcpatcher.mal.BlockAPIMod;
import javassist.bytecode.AccessFlag;

import static com.prupe.mcpatcher.BinaryRegex.*;
import static com.prupe.mcpatcher.BytecodeMatcher.*;
import static com.prupe.mcpatcher.mod.cc.CustomColors.*;
import static javassist.bytecode.Opcode.*;

class CC_Block {
    private static final MethodRef setupBlockSmoothing1 = new MethodRef(MCPatcherUtils.COLORIZE_BLOCK_CLASS, "setupBlockSmoothing", "(LRenderBlocks;LBlock;LIBlockAccess;IIII)Z");
    private static final MethodRef setupBlockSmoothing2 = new MethodRef(MCPatcherUtils.COLORIZE_BLOCK_CLASS, "setupBlockSmoothing", "(LRenderBlocks;LBlock;LIBlockAccess;IIIIFFFF)Z");
    private static final MethodRef setupBlockSmoothing3 = new MethodRef(MCPatcherUtils.COLORIZE_BLOCK_CLASS, "setupBlockSmoothingGrassSide", "(LRenderBlocks;LBlock;LIBlockAccess;IIIIFFFF)Z");
    private static final MethodRef setupBlockSmoothing4 = new MethodRef(MCPatcherUtils.COLORIZE_BLOCK_CLASS, "setupBlockSmoothing", "(LBlock;LIBlockAccess;IIIIFFF)V");

    private static final MethodRef getBlockColor = new MethodRef("Block", "getBlockColor", "()I");
    private static final MethodRef getRenderColor = new MethodRef("Block", "getRenderColor", "(I)I");
    private static MethodRef colorMultiplier;
    private static final FieldRef enableAO = new FieldRef("RenderBlocks", "enableAO", "Z");
    private static final FieldRef tessellator = new FieldRef("Tessellator", "instance", "LTessellator;");
    private static final MethodRef setColorOpaque_F = new MethodRef("Tessellator", "setColorOpaque_F", "(FFF)V");
    private static final MethodRef addVertexWithUV = new MethodRef("Tessellator", "addVertexWithUV", "(DDDDD)V");

    private static final String[] vertexNames = new String[]{"TopLeft", "BottomLeft", "BottomRight", "TopRight"};
    private static final String[] colorNames = new String[]{"Red", "Green", "Blue"};

    private static final FieldRef[] vertexColorFields = new FieldRef[12];
    private static final MethodRef[] vertexColorMethods = new MethodRef[3];
    private static final FieldRef[] ccVertexColorFields = new FieldRef[12];
    private static final FieldRef[] brightnessFields = new FieldRef[4];
    private static final FieldRef[] mixedBrightness = new FieldRef[4];

    static void setup(Mod mod) {
        int i = 0;
        int j = 0;
        for (String v : vertexNames) {
            for (String c : colorNames) {
                vertexColorFields[i] = new FieldRef("RenderBlocks", "color" + c + v, "F");
                ccVertexColorFields[i] = new FieldRef(MCPatcherUtils.COLORIZE_BLOCK_CLASS, "color" + c + v, "F");
                i++;
            }
            brightnessFields[j] = new FieldRef("RenderBlocks", "brightness" + v, "I");
            mixedBrightness[j] = new FieldRef("RenderBlockHelper", "mixedBrightness" + v, "F");
            j++;
        }
        i = 0;
        for (String c : colorNames) {
            vertexColorMethods[i++] = new MethodRef("RenderBlockCustomHelper", "getVertex" + c, "(LRenderBlockCustomHelper;)[F");
        }

        colorMultiplier = new MethodRef("Block", "colorMultiplier", "(LIBlockAccess;" + PositionMod.getDescriptor() + ")I");

        mod.addClassMod(new BlockMod(mod));
        mod.addClassMod(new BlockSubclassMod(mod));
        mod.addClassMod(new BlockCauldronMod(mod));
        mod.addClassMod(new BlockRedstoneWireMod(mod));
        mod.addClassMod(new ItemBlockMod(mod));
        mod.addClassMod(new RenderBlocksMod(mod));
        mod.addClassMod(new ItemRendererMod(mod));

        /* TODO: move to CTM
        if (RenderBlocksMod.haveSubclasses()) {
            boolean haveRenderBlockCauldron = getMinecraftVersion().compareTo("14w10a") < 0;
            RenderBlockManagerMod renderBlockManagerMod = new RenderBlockManagerMod(this)
                .mapRenderType(4, "RenderBlockFluid")
                .mapRenderType(5, "RenderBlockRedstoneWire");
            if (haveRenderBlockCauldron) {
                renderBlockManagerMod.mapRenderType(24, "RenderBlockCauldron");
            }
            addClassMod(renderBlockManagerMod);
            if (RenderBlockHelperMod.haveClass()) {
                addClassMod(new RenderBlockHelperMod());
            }
            if (RenderBlockCustomMod.haveCustomModels()) {
                addClassMod(new RenderBlockCustomMod());
                if (getMinecraftVersion().compareTo("14w07a") >= 0) {
                    addClassMod(new RenderBlockCustomHelperMod());
                }
            }
            addClassMod(new RenderBlockFluidMod());
            addClassMod(new RenderBlockRedstoneWireMod());
            if (haveRenderBlockCauldron) {
                addClassMod(new RenderBlockCauldronMod());
            }
        }
        */

        mod.addClassFiles("com.prupe.mcpatcher.colormap.*");
    }

    private static class BlockMod extends com.prupe.mcpatcher.basemod.BlockMod {
        BlockMod(Mod mod) {
            super(mod);

            match0xffffff(getBlockColor);
            match0xffffff(getRenderColor);
            match0xffffff(colorMultiplier);
        }

        private void match0xffffff(MethodRef method) {
            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        begin(),
                        push(0xffffff),
                        IRETURN,
                        end()
                    );
                }
            }.setMethod(method));
        }
    }

    private static class BlockSubclassMod extends ClassMod {
        public BlockSubclassMod(Mod mod) {
            super(mod);

            final MethodRef colorizeBlock1 = new MethodRef(MCPatcherUtils.COLORIZE_BLOCK_CLASS, "colorizeBlock", "(LBlock;)Z");
            final MethodRef colorizeBlock2 = new MethodRef(MCPatcherUtils.COLORIZE_BLOCK_CLASS, "colorizeBlock", "(LBlock;I)Z");
            final MethodRef colorizeBlock3 = new MethodRef(MCPatcherUtils.COLORIZE_BLOCK_CLASS, "colorizeBlock", "(LBlock;LIBlockAccess;III)Z");
            final FieldRef blockColor = new FieldRef(MCPatcherUtils.COLORIZE_BLOCK_CLASS, "blockColor", "I");

            setMultipleMatchesAllowed(true);

            addClassSignature(new AncestorClassSignature("Block"));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override color multiplier";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        begin()
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    String desc = getMethodInfo().getDescriptor();
                    final MethodRef newMethod;
                    final byte[] args;
                    if (desc.contains(getClassMap().mapTypeString(PositionMod.getDescriptor()) + ")")) {
                        newMethod = colorizeBlock3;
                        args = buildCode(
                            ALOAD_1,
                            PositionMod.unpackArguments(this, 2)
                        );
                    } else if (desc.contains("I)")) {
                        newMethod = colorizeBlock2;
                        args = new byte[]{ILOAD_1};
                    } else {
                        newMethod = colorizeBlock1;
                        args = new byte[0];
                    }
                    return buildCode(
                        // if (ColorizeBlock.colorizeBlock(this, ...)) {
                        ALOAD_0,
                        args,
                        reference(INVOKESTATIC, newMethod),
                        IFEQ, branch("A"),

                        // return ColorizeBlock.blockColor;
                        reference(GETSTATIC, blockColor),
                        IRETURN,

                        // }
                        label("A")
                    );
                }
            }.targetMethod(getBlockColor, getRenderColor, colorMultiplier));
        }
    }

    private static class BlockCauldronMod extends ClassMod {
        BlockCauldronMod(Mod mod) {
            super(mod);
            setParentClass("Block");

            addTexSignature("inner");
            addTexSignature("top");
            addTexSignature("bottom");
            addTexSignature("side");
            addClassSignature(new ConstSignature(0.3125f));
        }

        private void addTexSignature(String s) {
            addClassSignature(new OrSignature(
                new ConstSignature(s),
                new ConstSignature("_" + s),
                new ConstSignature("cauldron_" + s)
            ));
        }
    }

    private static class ItemBlockMod extends ClassMod {
        ItemBlockMod(Mod mod) {
            super(mod);
            setParentClass("Item");

            final MethodRef onItemUse = new MethodRef(getDeobfClass(), "onItemUse", "(LItemStack;LEntityPlayer;LWorld;" + PositionMod.getDescriptor() + DirectionMod.getDescriptor() + "FFF)Z");

            addClassSignature(new ConstSignature(0.5f));
            addClassSignature(new ConstSignature(0.8f));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // (... & 7) < 1
                        push(7),
                        IAND,
                        push(1),
                        IF_ICMPGE, any(2)
                    );
                }
            }.setMethod(onItemUse));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // if (... == 255)
                        push(255),
                        IF_ICMPNE, any(2)
                    );
                }
            }.setMethod(onItemUse));

            if (PositionMod.havePositionClass()) {
                // TODO:
            } else {
                addClassSignature(new BytecodeSignature() {
                    @Override
                    public String getMatchExpression() {
                        return buildExpression(
                            // if (face == 2) z--;
                            ILOAD, 7,
                            push(2),
                            IF_ICMPNE, any(2),
                            IINC, 6, -1,

                            // if (face == 3) z++;
                            ILOAD, 7,
                            push(3),
                            IF_ICMPNE, any(2),
                            IINC, 6, 1
                        );
                    }
                }.setMethod(onItemUse));

                addClassSignature(new BytecodeSignature() {
                    @Override
                    public String getMatchExpression() {
                        return buildExpression(
                            // if (y == 255)
                            ILOAD, 5,
                            push(255),
                            IF_ICMPNE, any(2)
                        );
                    }
                }.setMethod(onItemUse));
            }

            if (getMinecraftVersion().compareTo("13w36a") < 0) {
                final FieldRef blocksList = new FieldRef("Block", "blocksList", "[LBlock;");
                final FieldRef blockID = new FieldRef(getDeobfClass(), "blockID", "I");

                addClassSignature(new BytecodeSignature() {
                    {
                        matchConstructorOnly(true);
                        addXref(1, blockID);
                    }

                    @Override
                    public String getMatchExpression() {
                        return buildExpression(
                            ALOAD_0,
                            ILOAD_1,
                            push(256),
                            IADD,
                            captureReference(PUTFIELD)
                        );
                    }
                });

                addPatch(new ItemBlockPatch(this) {
                    @Override
                    protected byte[] getBlockOnStack() {
                        return buildCode(
                            reference(GETSTATIC, blocksList),
                            ALOAD_0,
                            reference(GETFIELD, blockID),
                            AALOAD
                        );
                    }
                });
            } else {
                final FieldRef block = new FieldRef(getDeobfClass(), "block", "LBlock;");

                addMemberMapper(new FieldMapper(block));

                addPatch(new ItemBlockPatch(this) {
                    @Override
                    protected byte[] getBlockOnStack() {
                        return buildCode(
                            ALOAD_0,
                            reference(GETFIELD, block)
                        );
                    }
                });
            }
        }

        abstract private static class ItemBlockPatch extends com.prupe.mcpatcher.AddMethodPatch {
            public ItemBlockPatch(ClassMod classMod) {
                super(classMod, getColorFromDamage);
            }

            @Override
            public byte[] generateMethod() {
                return buildCode(
                    // Block block = ...;
                    getBlockOnStack(),
                    ASTORE_3,

                    // if (block != null) {
                    ALOAD_3,
                    IFNULL, getClassMod().branch("A"),

                    // return block.getRenderColor(damage);
                    ALOAD_3,
                    ILOAD_2,
                    reference(INVOKEVIRTUAL, getRenderColor),
                    IRETURN,

                    // } else {
                    getClassMod().label("A"),

                    // return super.getColorFromDamage(itemStack, damage);
                    ALOAD_0,
                    ALOAD_1,
                    ILOAD_2,
                    reference(INVOKESPECIAL, getColorFromDamage),
                    IRETURN

                    // }
                );
            }

            abstract protected byte[] getBlockOnStack();
        }
    }

    private static class ItemRendererMod extends ClassMod {
        ItemRendererMod(Mod mod) {
            super(mod);

            final MethodRef renderItem = new MethodRef(getDeobfClass(), "renderItem", "(LEntityLivingBase;LItemStack;I)V");
            final MethodRef glTranslatef = new MethodRef(MCPatcherUtils.GL11_CLASS, "glTranslatef", "(FFF)V");
            final boolean haveItemID = getMinecraftVersion().compareTo("13w36a") < 0;
            final FieldRef itemID = haveItemID ? new FieldRef("ItemStack", "itemID", "I") : null;
            final MethodRef colorizeWaterBlockGL = new MethodRef(MCPatcherUtils.COLORIZE_BLOCK_CLASS, "colorizeWaterBlockGL", "(LBlock;)V");

            if (ResourceLocationMod.haveClass()) {
                addClassSignature(new ConstSignature("textures/misc/underwater.png"));
                addClassSignature(new ConstSignature("textures/map/map_background.png"));
            } else {
                addClassSignature(new ConstSignature("%blur%/misc/glint.png"));
                addClassSignature(new ConstSignature("/misc/mapbg.png"));
            }

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // GL11.glTranslatef(-0.9375f, -0.0625f, 0.0f);
                        push(-0.9375f),
                        push(-0.0625f),
                        push(0.0f),
                        reference(INVOKESTATIC, glTranslatef)
                    );
                }
            }.setMethod(renderItem));

            if (haveItemID) {
                addClassSignature(new BytecodeSignature() {
                    {
                        addXref(1, itemID);
                        setMethod(renderItem);
                    }

                    @Override
                    public String getMatchExpression() {
                        return buildExpression(
                            // par2ItemStack.itemID
                            ALOAD_2,
                            captureReference(GETFIELD),
                            or(
                                build(push(256)),
                                build(AALOAD)
                            )
                        );
                    }
                });
            }

            addPatch(new BytecodePatch() {
                private int blockRegister;

                {
                    setInsertAfter(true);
                    targetMethod(renderItem);

                    if (!haveItemID) {
                        addPreMatchSignature(new BytecodeSignature() {
                            @Override
                            public String getMatchExpression() {
                                return buildExpression(
                                    // block = Block.getItemBlock(item);
                                    begin(),
                                    any(0, 20),
                                    anyALOAD,
                                    anyReference(INVOKESTATIC),
                                    capture(anyASTORE)
                                );
                            }

                            @Override
                            public boolean afterMatch() {
                                blockRegister = extractRegisterNum(getCaptureGroup(1));
                                return true;
                            }
                        });
                    }
                }

                @Override
                public String getDescription() {
                    return "override water block color in third person";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // GL11.glTranslatef(-0.9375f, -0.0625f, 0.0f);
                        push(-0.9375f),
                        push(-0.0625f),
                        push(0.0f),
                        reference(INVOKESTATIC, glTranslatef)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    if (haveItemID) {
                        return buildCode(
                            ALOAD_2,
                            reference(GETFIELD, itemID),
                            reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.BLOCK_API_CLASS, "getBlockById", "(I)LBlock;")),
                            reference(INVOKESTATIC, colorizeWaterBlockGL)
                        );
                    } else {
                        return buildCode(
                            ALOAD, blockRegister,
                            reference(INVOKESTATIC, colorizeWaterBlockGL)
                        );
                    }
                }
            });
        }
    }

    private static void setupRedstoneWire(com.prupe.mcpatcher.ClassMod classMod, final String description, final MethodRef method) {
        classMod.addClassSignature(new BytecodeSignature(classMod) {
            @Override
            public String getMatchExpression() {
                return buildExpression(
                    // f = (float) l / 15.0f;
                    anyILOAD,
                    I2F,
                    push(15.0f),
                    FDIV,
                    anyFSTORE,

                    // f1 = f * 0.6f + 0.4f;
                    anyFLOAD,
                    push(0.6f),
                    FMUL,
                    push(0.4f),
                    FADD,
                    anyFSTORE
                );
            }
        }.setMethod(method));

        classMod.addPatch(new BytecodePatch(classMod) {
            @Override
            public String getDescription() {
                return description;
            }

            @Override
            public String getMatchExpression() {
                return buildExpression(
                    ILOAD, capture(any()),
                    I2F,
                    push(15.0f),
                    FDIV,
                    FSTORE, capture(any()),

                    FLOAD, backReference(2),
                    push(0.6f),
                    FMUL,
                    push(0.4f),
                    FADD,
                    FSTORE, capture(any()),

                    any(0, 10),

                    FLOAD, backReference(2),
                    FLOAD, backReference(2),
                    FMUL,
                    push(0.7f),
                    FMUL,
                    push(0.5f),
                    FSUB,
                    FSTORE, capture(any()),

                    FLOAD, backReference(2),
                    FLOAD, backReference(2),
                    FMUL,
                    push(0.6f),
                    FMUL,
                    push(0.7f),
                    FSUB,
                    FSTORE, capture(any())
                );
            }

            @Override
            public byte[] getReplacementBytes() {
                return buildCode(
                    ILOAD, getCaptureGroup(1),
                    reference(INVOKESTATIC, computeRedstoneWireColor),
                    IFEQ, branch("A"),

                    reference(GETSTATIC, setColor),
                    ICONST_0,
                    FALOAD,
                    FSTORE, getCaptureGroup(3),
                    reference(GETSTATIC, setColor),
                    ICONST_1,
                    FALOAD,
                    FSTORE, getCaptureGroup(4),
                    reference(GETSTATIC, setColor),
                    ICONST_2,
                    FALOAD,
                    FSTORE, getCaptureGroup(5),
                    GOTO, branch("B"),

                    label("A"),
                    getMatch(),
                    label("B")
                );
            }
        }.targetMethod(method));
    }

    private static class BlockRedstoneWireMod extends ClassMod {
        BlockRedstoneWireMod(Mod mod) {
            super(mod);
            setParentClass("Block");

            final MethodRef randomDisplayTick = new MethodRef("BlockRedstoneWire", "randomDisplayTick", "(LWorld;" + PositionMod.getDescriptor() + "Ljava/util/Random;)V");
            final MethodRef colorizeRedstoneWire = new MethodRef(MCPatcherUtils.COLORIZE_BLOCK_CLASS, "colorizeRedstoneWire", "(LIBlockAccess;IIII)I");

            setupRedstoneWire(this, "override redstone wire particle color", randomDisplayTick);

            addClassSignature(new ConstSignature("reddust"));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override redstone color multiplier";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(0x800000)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        ALOAD_1,
                        PositionMod.unpackArguments(this, 2),
                        getMatch(),
                        reference(INVOKESTATIC, colorizeRedstoneWire)
                    );
                }
            }.targetMethod(colorMultiplier));
        }
    }

    abstract private static class TessellatorPatch extends BytecodePatch {
        protected int tessellatorRegister;

        TessellatorPatch(com.prupe.mcpatcher.ClassMod classMod) {
            super(classMod);

            addPreMatchSignature(new BytecodeSignature(classMod) {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        reference(GETSTATIC, tessellator),
                        capture(anyASTORE)
                    );
                }

                @Override
                public boolean afterMatch() {
                    tessellatorRegister = extractRegisterNum(getCaptureGroup(1));
                    return true;
                }
            });
        }
    }

    private static class RenderBlocksMod extends com.prupe.mcpatcher.basemod.RenderBlocksMod {
        private final MethodRef renderBlockFluids = new MethodRef(getDeobfClass(), "renderBlockFluids", "(LBlock;" + PositionMod.getDescriptor() + ")Z");
        private final MethodRef renderBlockFallingSand = new MethodRef(getDeobfClass(), "renderBlockFallingSand", "(LBlock;LWorld;" + PositionMod.getDescriptor() + "I)V");
        private final MethodRef renderBlockCauldron = new MethodRef(getDeobfClass(), "renderBlockCauldron", "(LBlockCauldron;" + PositionMod.getDescriptor() + ")Z");
        private final MethodRef renderBlockRedstoneWire = new MethodRef(getDeobfClass(), "renderBlockRedstoneWire", "(LBlock;" + PositionMod.getDescriptor() + ")Z");

        RenderBlocksMod(Mod mod) {
            super(mod);

            if (haveSubclasses()) {
                addPatch(new MakeMemberPublicPatch(enableAO));
            } else {
                mapRenderType(4, renderBlockFluids);
                setupFluids(this, renderBlockFluids);
                mapRenderType(5, renderBlockRedstoneWire);
                setupRedstoneWire(this, "override redstone wire color", renderBlockRedstoneWire);
                mapRenderType(24, renderBlockCauldron);
                setupCauldron(this, renderBlockCauldron);
            }
            setupBiomeSmoothing();
            setupFallingSand();
            if (!ResourceLocationMod.haveClass()) {
                setupBTW();
            }
        }

        private void setupFallingSand() {
            addMemberMapper(new MethodMapper(renderBlockFallingSand));

            addPatch(new TessellatorPatch(this) {
                @Override
                public String getDescription() {
                    return "colorize falling sand and gravel";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // tessellator.setColorOpaque_F(...);
                        ALOAD, tessellatorRegister,
                        any(0, 20),
                        reference(INVOKEVIRTUAL, setColorOpaque_F)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // if (!ColorizeBlock.setupBlockSmoothing(this, block, world, i, j, k, face)) {
                        ALOAD_0,
                        ALOAD_1,
                        ALOAD_2,
                        PositionMod.unpackArguments(this, 3),
                        push(getMethodMatchCount() % 6),
                        reference(INVOKESTATIC, setupBlockSmoothing1),
                        IFNE, branch("A"),

                        // ...
                        getMatch(),

                        // }
                        label("A")
                    );
                }
            }.targetMethod(renderBlockFallingSand));
        }

        private void setupBiomeSmoothing() {
            addClassSignature(new BytecodeSignature() {
                private final String vertexExpr;

                {
                    addXref(1, enableAO);

                    if (RenderBlockHelperMod.haveClass()) {
                        vertexExpr = buildExpression(
                            // this.doubleArray[intArray[0-3]]
                            ALOAD_0,
                            anyReference(GETFIELD),
                            anyALOAD,
                            any(1, 2),
                            IALOAD,
                            DALOAD
                        );
                    } else {
                        vertexExpr = anyDLOAD;
                    }

                    int i = 0;
                    int j = 0;
                    for (String v : vertexNames) {
                        for (String c : colorNames) {
                            vertexColorFields[i] = new FieldRef(getDeobfClass(), "color" + c + v, "F");
                            i++;
                        }
                        brightnessFields[j] = new FieldRef(getDeobfClass(), "brightness" + v, "I");
                        j++;
                    }
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // if (this.enableAO)
                        ALOAD_0,
                        captureReference(GETFIELD),
                        IFEQ, any(2),

                        // tessellator.setColorOpaque_F(...);
                        // tessellator.setBrightness(...);
                        // tessellator.addVertexWithUV(...);
                        // x4
                        getSubExpression(0),
                        getSubExpression(1),
                        getSubExpression(2),
                        getSubExpression(3)
                    );
                }

                private String getSubExpression(int index) {
                    addXref(7 * index + 2, vertexColorFields[3 * index]);
                    addXref(7 * index + 3, vertexColorFields[3 * index + 1]);
                    addXref(7 * index + 4, vertexColorFields[3 * index + 2]);
                    addXref(7 * index + 5, setColorOpaque_F);
                    addXref(7 * index + 6, brightnessFields[index]);
                    // tessellator.setBrightness
                    addXref(7 * index + 8, addVertexWithUV);

                    return build(
                        // tessellator.setColorOpaque_F(this.colorRedxxx, this.colorGreenxxx, this.colorBluexxx);
                        anyALOAD,
                        ALOAD_0,
                        captureReference(GETFIELD),
                        ALOAD_0,
                        captureReference(GETFIELD),
                        ALOAD_0,
                        captureReference(GETFIELD),
                        captureReference(INVOKEVIRTUAL),

                        // tessellator.setBrightness(this.brightnessxxx);
                        anyALOAD,
                        ALOAD_0,
                        captureReference(GETFIELD),
                        captureReference(INVOKEVIRTUAL),

                        // tessellator.addVertexWithUV(...);
                        anyALOAD,
                        repeat(vertexExpr, 5),
                        captureReference(INVOKEVIRTUAL)
                    );
                }
            });

            for (FieldRef field : vertexColorFields) {
                addPatch(new MakeMemberPublicPatch(field));
            }

            if (RenderBlockHelperMod.haveClass()) {
                setupBiomeSmoothing18();
            } else {
                setupBiomeSmoothing17();
            }
        }

        private void setupBiomeSmoothing17() {
            addPatch(new BytecodePatch() {
                {
                    addPreMatchSignature(grassTopSignature);
                }

                @Override
                public String getDescription() {
                    return "smooth biome colors (standard blocks)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        capture(repeat(build(
                            // this.brightnessxxx = this.get/mixAoBrightness(...); x4
                            ALOAD_0,
                            ALOAD_0,
                            nonGreedy(any(0, 100)),
                            anyReference(INVOKESPECIAL),
                            or(
                                build(reference(PUTFIELD, brightnessFields[0])),
                                build(reference(PUTFIELD, brightnessFields[1])),
                                build(reference(PUTFIELD, brightnessFields[2])),
                                build(reference(PUTFIELD, brightnessFields[3]))
                            )
                        ), 4)),

                        capture(build(
                            // ...
                            nonGreedy(any(0, 200)),

                            // this.colorRedTopLeft *= topLeft;
                            // this.colorGreenTopLeft *= topLeft;
                            // this.colorBlueTopLeft *= topLeft;
                            getColorSubExpression(0),
                            getColorSubExpression(1),
                            getColorSubExpression(2),

                            // this.colorRedBottomLeft *= bottomLeft;
                            // ...
                            getColorSubExpression(3),
                            getColorSubExpression(4),
                            getColorSubExpression(5),

                            // this.colorRedBottomRight *= bottomRight;
                            // ...
                            getColorSubExpression(6),
                            getColorSubExpression(7),
                            getColorSubExpression(8),

                            // this.colorRedTopRight *= topRight;
                            // ...
                            getColorSubExpression(9),
                            getColorSubExpression(10),
                            getColorSubExpression(11)
                        )),

                        lookAhead(build(
                            // this.getBlockIcon(block, this.blockAccess, i, j, k, ...);
                            nonGreedy(any(0, 30)),
                            ALOAD_0,
                            ALOAD_1,
                            ALOAD_0,
                            reference(GETFIELD, blockAccess),
                            PositionMod.passArguments(2),
                            capture(any(0, 3)),
                            anyReference(INVOKEVIRTUAL)
                        ), true)
                    );
                }

                private String getColorSubExpression(int index) {
                    return build(
                        // this.colorxxxyyy *= yyy;
                        ALOAD_0,
                        DUP,
                        reference(GETFIELD, vertexColorFields[index]),
                        index % 3 == 0 ? capture(anyFLOAD) : backReference(index / 3 + 3),
                        FMUL,
                        reference(PUTFIELD, vertexColorFields[index])
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // ...
                        getCaptureGroup(1),

                        // if (!ColorizeBlock.setupBlockSmoothing(this, block, this.blockAccess,
                        //                                        i, j, k, face,
                        //                                        topLeft, bottomLeft, bottomRight, topRight)) {
                        ALOAD_0,
                        ALOAD_1,
                        ALOAD_0,
                        reference(GETFIELD, blockAccess),

                        PositionMod.unpackArguments(this, 2),
                        getCaptureGroup(7),
                        DirectionMod.haveDirectionClass() ?
                            reference(INVOKEVIRTUAL, DirectionMod.ordinal) : new byte[0],

                        getCaptureGroup(3),
                        getCaptureGroup(4),
                        getCaptureGroup(5),
                        getCaptureGroup(6),

                        reference(INVOKESTATIC, setupBlockSmoothing2),
                        IFNE, branch("A"),

                        // ...
                        getCaptureGroup(2),

                        // }
                        label("A")
                    );
                }
            });
        }

        private void setupBiomeSmoothing18() {
            final FieldRef helper = new FieldRef(getDeobfClass(), "renderBlockHelper", "LRenderBlockHelper;");

            addMemberMapper(new FieldMapper(helper));

            addPatch(new BytecodePatch() {
                private int faceRegister;

                {
                    addPreMatchSignature(new BytecodeSignature() {
                        @Override
                        public String getMatchExpression() {
                            return buildExpression(
                                IINC, any(2)
                            );
                        }

                        @Override
                        public boolean afterMatch() {
                            faceRegister = getMatch()[1] & 0xff;
                            return true;
                        }
                    });
                }

                @Override
                public String getDescription() {
                    return "smooth biome colors (grass side)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // this.topLeft *= red;
                        // ...
                        getColorSubExpression(0),
                        getColorSubExpression(1),
                        getColorSubExpression(2),
                        getColorSubExpression(3),
                        getColorSubExpression(4),
                        getColorSubExpression(5),
                        getColorSubExpression(6),
                        getColorSubExpression(7),
                        getColorSubExpression(8),
                        getColorSubExpression(9),
                        getColorSubExpression(10),
                        getColorSubExpression(11)
                    );
                }

                private String getColorSubExpression(int index) {
                    // 0, 3, 6, 9, 1, 4, 7, 10, 2, 5, 8, 11
                    FieldRef vertex = vertexColorFields[(3 * index + index / 4) % vertexColorFields.length];
                    return build(
                        ALOAD_0,
                        DUP,
                        reference(GETFIELD, vertex),
                        index % 4 == 0 ? capture(anyFLOAD) : backReference(index / 4 + 1),
                        FMUL,
                        reference(PUTFIELD, vertex)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // if (!ColorizeBlock.setupBlockSmoothingGrassSide(this, block, this.blockAccess,
                        //                                                 i, j, k, face,
                        //                                                 this.helper.topLeft, this.helper.bottomLeft,
                        //                                                 this.helper.bottomRight, this.helper.topRight)) {
                        ALOAD_0,
                        ALOAD_1,
                        ALOAD_0,
                        reference(GETFIELD, blockAccess),
                        PositionMod.unpackArguments(this, 2),
                        registerLoadStore(ILOAD, faceRegister),
                        ALOAD_0,
                        reference(GETFIELD, helper),
                        reference(GETFIELD, mixedBrightness[0]),
                        ALOAD_0,
                        reference(GETFIELD, helper),
                        reference(GETFIELD, mixedBrightness[1]),
                        ALOAD_0,
                        reference(GETFIELD, helper),
                        reference(GETFIELD, mixedBrightness[2]),
                        ALOAD_0,
                        reference(GETFIELD, helper),
                        reference(GETFIELD, mixedBrightness[3]),
                        reference(INVOKESTATIC, setupBlockSmoothing3),
                        IFNE, branch("A"),

                        // ...
                        getMatch(),

                        // }
                        label("A")
                    );
                }
            }.targetMethod(renderStandardBlockWithAmbientOcclusion));
        }

        private void setupBTW() {
            final FieldRef blockAccess = new FieldRef(getDeobfClass(), "blockAccess", "LIBlockAccess;");
            final MethodRef renderBlockAO = new MethodRef(getDeobfClass(), "RenderStandardFullBlockWithAmbientOcclusion", "(LBlock;III)Z");
            final MethodRef renderBlockCM = new MethodRef(getDeobfClass(), "RenderStandardFullBlockWithColorMultiplier", "(LBlock;III)Z");
            final MethodRef renderGrassBlockAO = new MethodRef(getDeobfClass(), "renderGrassBlockWithAmbientOcclusion", "(LBlock;IIIFFFLIcon;)Z");
            final MethodRef renderGrassBlockCM = new MethodRef(getDeobfClass(), "renderGrassBlockWithColorMultiplier", "(LBlock;IIIFFFLIcon;)Z");

            addPatch(new BytecodePatch() {
                private int topLeftRegister;

                {
                    setInsertAfter(true);
                    targetMethod(renderBlockAO, renderBlockCM);

                    addPreMatchSignature(new BytecodeSignature() {
                        @Override
                        public String getMatchExpression() {
                            return buildExpression(
                                // topLeft = (...) / 4.0f
                                push(4.0f),
                                FDIV,
                                capture(anyFSTORE)
                            );
                        }

                        @Override
                        public boolean afterMatch() {
                            topLeftRegister = extractRegisterNum(getCaptureGroup(1));
                            Logger.log(Logger.LOG_CONST, "top left register %d", topLeftRegister);
                            return true;
                        }
                    });
                }

                @Override
                public String getDescription() {
                    return "use block color multiplier (btw)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(repeat(build(
                        // this.vertexColorAAA = this.vertexColorBBB = this.vertexColorCCC = this.vertexColorDDD = <value>;
                        // x3
                        ALOAD_0,
                        ALOAD_0,
                        ALOAD_0,
                        ALOAD_0,
                        or(anyLDC, build(push(1.0f))),
                        DUP_X1,
                        anyReference(PUTFIELD),
                        DUP_X1,
                        anyReference(PUTFIELD),
                        DUP_X1,
                        anyReference(PUTFIELD),
                        anyReference(PUTFIELD)
                    ), 3));
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // ColorizeBlock.setupBiomeSmoothing(this, block, this.blockAccess, i, j, k, face,
                        //                                   topLeft, bottomLeft, bottomRight, topRight)
                        ALOAD_0,
                        ALOAD_1,
                        ALOAD_0,
                        reference(GETFIELD, blockAccess),
                        ILOAD_2,
                        ILOAD_3,
                        ILOAD, 4,
                        push(getMethodMatchCount() % 6),
                        registerLoadStore(FLOAD, topLeftRegister),
                        registerLoadStore(FLOAD, topLeftRegister + 1),
                        registerLoadStore(FLOAD, topLeftRegister + 2),
                        registerLoadStore(FLOAD, topLeftRegister + 3),
                        reference(INVOKESTATIC, setupBlockSmoothing3),
                        POP
                    );
                }
            });

            addPatch(new BytecodePatch() {
                private int topLeftRegister;

                {
                    targetMethod(renderGrassBlockAO, renderGrassBlockCM);

                    addPreMatchSignature(new BytecodeSignature() {
                        @Override
                        public String getMatchExpression() {
                            return buildExpression(
                                // topLeft = (...) / 4.0f
                                push(4.0f),
                                FDIV,
                                capture(anyFSTORE)
                            );
                        }

                        @Override
                        public boolean afterMatch() {
                            topLeftRegister = extractRegisterNum(getCaptureGroup(1));
                            Logger.log(Logger.LOG_CONST, "top left register %d", topLeftRegister);
                            return true;
                        }
                    });
                }

                @Override
                public String getDescription() {
                    return "use grass color multiplier (btw)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(repeat(build(
                        // this.vertexColorXXX *= color;
                        // x9
                        ALOAD_0,
                        DUP,
                        anyReference(GETFIELD),
                        FLOAD, subset(true, 5, 6, 7),
                        FMUL,
                        anyReference(PUTFIELD)
                    ), 12));
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // if (!ColorizeBlock.setupBiomeSmoothing(this, block, this.blockAccess, i, j, k, face,
                        //                                        topLeft, bottomLeft, bottomRight, topRight)) {
                        ALOAD_0,
                        ALOAD_1,
                        ALOAD_0,
                        reference(GETFIELD, blockAccess),
                        ILOAD_2,
                        ILOAD_3,
                        ILOAD, 4,
                        push(2 + getMethodMatchCount() % 4),
                        registerLoadStore(FLOAD, topLeftRegister),
                        registerLoadStore(FLOAD, topLeftRegister + 1),
                        registerLoadStore(FLOAD, topLeftRegister + 2),
                        registerLoadStore(FLOAD, topLeftRegister + 3),
                        reference(INVOKESTATIC, setupBlockSmoothing3),
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

    private static class RenderBlockHelperMod extends com.prupe.mcpatcher.basemod.RenderBlockHelperMod {
        RenderBlockHelperMod(Mod mod) {
            super(mod);

            addMemberMapper(new FieldMapper(mixedBrightness));

            addPatch(new BytecodePatch() {
                {
                    setInsertBefore(true);
                    targetMethod(computeVertexColors);
                }

                @Override
                public String getDescription() {
                    return "smooth biome colors (standard blocks)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        or(
                            build(
                                // if (block.useColorMultiplierThisDirection(direction) ...)
                                ALOAD_1,
                                ALOAD_3,
                                anyReference(INVOKEVIRTUAL)
                            ),
                            build(
                                // if (RenderBlocksUtils.useColorMultiplier(direction.ordinal()) ...)
                                ALOAD_3,
                                reference(INVOKEVIRTUAL, DirectionMod.ordinal),
                                reference(INVOKESTATIC, BlockAPIMod.useColorMultiplier1)
                            )
                        ),
                        IFEQ, any(2)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // if (ColorizeBlock.setupBlockSmoothing(this.renderBlocks, block, this.renderBlocks.blockAccess,
                        //                                       i, j, k, face,
                        //                                       this.topLeft, this.bottomLeft, this.bottomRight, this.topRight)) {
                        ALOAD_0,
                        reference(GETFIELD, renderBlocks),
                        ALOAD_1,
                        ALOAD_0,
                        reference(GETFIELD, renderBlocks),
                        reference(GETFIELD, RenderBlocksMod.blockAccess),

                        PositionMod.unpackArguments(this, 2),
                        DirectionMod.unpackArguments(this, 3),

                        ALOAD_0,
                        reference(GETFIELD, mixedBrightness[0]),
                        ALOAD_0,
                        reference(GETFIELD, mixedBrightness[1]),
                        ALOAD_0,
                        reference(GETFIELD, mixedBrightness[2]),
                        ALOAD_0,
                        reference(GETFIELD, mixedBrightness[3]),

                        reference(INVOKESTATIC, setupBlockSmoothing2),
                        IFEQ, branch("A"),

                        // return;
                        RETURN,

                        // }
                        label("A")
                    );
                }
            });
        }
    }

    private static class RenderBlockCustomMod extends com.prupe.mcpatcher.basemod.RenderBlockCustomMod {
        RenderBlockCustomMod(Mod mod) {
            super(mod);
            mapHelper();

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "smooth biome colors (standard blocks)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        begin()
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    int directionParam = getDirectionParam();
                    return buildCode(
                        // ColorizeBlock.setupBlockSmoothing(block, this.blockAccess, i, j, k, face, r, g, b);
                        ALOAD_1,
                        ALOAD_0,
                        reference(GETFIELD, RenderBlocksMod.blockAccess),
                        PositionMod.unpackArguments(this, 2),
                        DirectionMod.unpackArgumentsSafe(this, directionParam),
                        registerLoadStore(FLOAD, directionParam + 1),
                        registerLoadStore(FLOAD, directionParam + 2),
                        registerLoadStore(FLOAD, directionParam + 3),
                        reference(INVOKESTATIC, setupBlockSmoothing4)
                    );
                }
            }.targetMethod(renderFaceAO));

            addPatch(new BytecodePatch() {
                private final boolean useHelper = getMinecraftVersion().compareTo("14w07a") >= 0;

                @Override
                public String getDescription() {
                    return "use vertex color multipliers";
                }

                @Override
                public String getMatchExpression() {
                    String[] tmp = new String[vertexColorFields.length];
                    for (int i = 0; i < vertexColorFields.length; i++) {
                        String subExpr = getVertexSubExpression(i);
                        tmp[i] = capture(subExpr);
                    }

                    return buildExpression(
                        or(tmp),
                        anyFLOAD,
                        FMUL
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    for (int i = 0; i < vertexColorFields.length; i++) {
                        byte[] subExpr = getCaptureGroup(i + 1);
                        if (subExpr != null) {
                            return buildCode(
                                // ... * ColorizeBlock.colorxxx
                                subExpr,
                                reference(GETSTATIC, ccVertexColorFields[i]),
                                FMUL
                            );
                        }
                    }
                    return null;
                }

                private String getVertexSubExpression(int index) {
                    if (useHelper) {
                        return build(
                            // RenderBlockCustomHelper.getVertexxxx(this.helper)[xxx]
                            ALOAD_0,
                            reference(GETFIELD, helper),
                            reference(INVOKESTATIC, vertexColorMethods[index % 3]),
                            push((index / 3 + 2) % 4), // why does +2 work???
                            FALOAD
                        );
                    } else {
                        return build(
                            // this.colorxxx
                            ALOAD_0,
                            reference(GETFIELD, remap(vertexColorFields[index]))
                        );
                    }
                }
            }.targetMethod(renderFaceAO));
        }
    }

    private static class RenderBlockCustomHelperMod extends ClassMod {
        RenderBlockCustomHelperMod(Mod mod) {
            super(mod);
            addPrerequisiteClass("RenderBlockCustom");

            addMemberMapper(new MethodMapper(vertexColorMethods)
                    .accessFlag(AccessFlag.SYNTHETIC, true)
                    .accessFlag(AccessFlag.STATIC, true)
            );
        }
    }

    private static class RenderBlockFluidMod extends ClassMod {
        RenderBlockFluidMod(Mod mod) {
            super(mod);

            final MethodRef renderBlockFluid = new MethodRef(getDeobfClass(), "renderBlockFluid", "(LBlockFluid;" + PositionMod.getDescriptor() + ")Z");

            addPrerequisiteClass("RenderBlockManager");
            setParentClass("RenderBlocks");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(-999.0f)
                    );
                }
            }.setMethod(renderBlockFluid));

            setupFluids(this, renderBlockFluid);
        }
    }

    private static class RenderBlockRedstoneWireMod extends ClassMod {
        RenderBlockRedstoneWireMod(Mod mod) {
            super(mod);

            final MethodRef renderBlockRedstoneWire = new MethodRef(getDeobfClass(), "renderBlockRedstoneWire", "(LBlockRedstoneWire;" + PositionMod.getDescriptor() + ")Z");

            addPrerequisiteClass("RenderBlockManager");
            setParentClass("RenderBlocks");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push("cross")
                    );
                }
            }.setMethod(renderBlockRedstoneWire));

            setupRedstoneWire(this, "override redstone wire color", renderBlockRedstoneWire);
        }
    }

    private static class RenderBlockCauldronMod extends ClassMod {
        RenderBlockCauldronMod(Mod mod) {
            super(mod);

            final MethodRef renderBlockCauldron = new MethodRef(getDeobfClass(), "renderBlockCauldron", "(LBlockCauldron;" + PositionMod.getDescriptor() + ")Z");

            addPrerequisiteClass("RenderBlockManager");
            setParentClass("RenderBlocks");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push("inner")
                    );
                }
            }.setMethod(renderBlockCauldron));

            setupCauldron(this, renderBlockCauldron);
        }
    }

    private static void setupFluids(final com.prupe.mcpatcher.ClassMod classMod, final MethodRef renderBlockFluids) {
        final FieldRef blockAccess = new FieldRef(classMod.getDeobfClass(), "blockAccess", "LIBlockAccess;");
        final FieldRef isSmooth = new FieldRef(MCPatcherUtils.COLORIZE_BLOCK_CLASS, "isSmooth", "Z");

        classMod.addPatch(new TessellatorPatch(classMod) {
            private int[] waterRegisters;

            {
                addPreMatchSignature(new BytecodeSignature(classMod) {
                    @Override
                    public String getMatchExpression() {
                        return buildExpression(
                            // f = (float)(l >> 16 & 0xff) / 255F;
                            capture(anyILOAD),
                            push(16),
                            ISHR,
                            push(255),
                            IAND,
                            I2F,
                            push(255.0f),
                            FDIV,
                            capture(anyFSTORE),

                            // f1 = (float)(l >> 8 & 0xff) / 255F;
                            backReference(1),
                            push(8),
                            ISHR,
                            push(255),
                            IAND,
                            I2F,
                            push(255.0f),
                            FDIV,
                            capture(anyFSTORE),

                            // f2 = (float)(l & 0xff) / 255F;
                            backReference(1),
                            push(255),
                            IAND,
                            I2F,
                            push(255.0f),
                            FDIV,
                            capture(anyFSTORE)
                        );
                    }

                    @Override
                    public boolean afterMatch() {
                        waterRegisters = new int[]{
                            extractRegisterNum(getCaptureGroup(2)),
                            extractRegisterNum(getCaptureGroup(3)),
                            extractRegisterNum(getCaptureGroup(4)),
                        };
                        Logger.log(Logger.LOG_CONST, "water color registers: %d %d %d",
                            waterRegisters[0], waterRegisters[1], waterRegisters[2]
                        );
                        return true;
                    }
                });
            }

            @Override
            public String getDescription() {
                return "colorize bottom of water block";
            }

            @Override
            public String getMatchExpression() {
                return buildExpression(
                    // tessellator.setColorOpaque_F(k * l, k * l, k * l);
                    // -or-
                    // tessellator.setColorOpaque_F(k, k, k);
                    registerLoadStore(ALOAD, tessellatorRegister),
                    capture(build(
                        anyFLOAD,
                        optional(build(anyFLOAD, FMUL))
                    )),
                    backReference(1),
                    backReference(1),
                    reference(INVOKEVIRTUAL, setColorOpaque_F)
                );
            }

            @Override
            public byte[] getReplacementBytes() {
                return buildCode(
                    // tessellator.setColorOpaque_F(k * l * r, k * l * g, k * l * b);
                    registerLoadStore(ALOAD, tessellatorRegister),
                    getCaptureGroup(1),
                    FLOAD, waterRegisters[0],
                    FMUL,
                    getCaptureGroup(1),
                    FLOAD, waterRegisters[1],
                    FMUL,
                    getCaptureGroup(1),
                    FLOAD, waterRegisters[2],
                    FMUL,
                    reference(INVOKEVIRTUAL, setColorOpaque_F)
                );
            }
        }.targetMethod(renderBlockFluids));

        classMod.addPatch(new BytecodePatch(classMod) {
            private int faceRegister;

            {
                addPreMatchSignature(new BytecodeSignature(classMod) {
                    @Override
                    public String getMatchExpression() {
                        return buildExpression(
                            // || renderFaces[face]
                            anyALOAD,
                            capture(anyILOAD),
                            BALOAD,
                            IFEQ, any(2)
                        );
                    }

                    @Override
                    public boolean afterMatch() {
                        faceRegister = extractRegisterNum(getCaptureGroup(1));
                        return true;
                    }

                    @Override
                    public boolean afterNonMatch() {
                        faceRegister = 0;
                        return true;
                    }
                });
            }

            @Override
            public String getDescription() {
                return "smooth biome colors (water part 1)";
            }

            @Override
            public String getMatchExpression() {
                return buildExpression(
                    // tessellator.setColorOpaque_F(k * l * r, k * l * g, k * l * b);
                    // -or-
                    // tessellator.setColorOpaque_F(k * r, k * g, k * b);
                    anyALOAD,
                    capture(build(
                        anyFLOAD,
                        optional(build(anyFLOAD, FMUL))
                    )),
                    anyFLOAD,
                    FMUL,

                    backReference(1),
                    anyFLOAD,
                    FMUL,

                    backReference(1),
                    anyFLOAD,
                    FMUL,

                    reference(INVOKEVIRTUAL, setColorOpaque_F)
                );
            }

            @Override
            public byte[] getReplacementBytes() {
                byte[] faceCode;
                switch (getMethodMatchCount()) {
                    case 0:
                        faceCode = new byte[]{ICONST_1}; // top face
                        break;

                    case 1:
                        faceCode = new byte[]{ICONST_0}; // bottom face
                        break;

                    default:
                        if (faceRegister > 0) {
                            if (getMethodMatchCount() == 2) {
                                faceCode = buildCode(
                                    registerLoadStore(ILOAD, faceRegister),
                                    push(2),
                                    IADD
                                ); // other faces
                            } else {
                                return null;
                            }
                        } else {
                            // btw uses 4 code blocks with fixed face values instead of a loop
                            faceCode = buildCode(push(getMethodMatchCount() % 6));
                        }
                        break;
                }
                return buildCode(
                    // ColorizeBlock.isSmooth = ColorizeBlock.setupBlockSmoothing(this, block, this,blockAccess,
                    //                                                            i, j, k, face + 6);
                    ALOAD_0,
                    ALOAD_1,
                    ALOAD_0,
                    reference(GETFIELD, blockAccess),

                    PositionMod.unpackArguments(this, 2),
                    faceCode,
                    push(6),
                    IADD,

                    reference(INVOKESTATIC, setupBlockSmoothing1),
                    reference(PUTSTATIC, isSmooth),

                    // if (!ColorizeBlock.isSmooth) {
                    reference(GETSTATIC, isSmooth),
                    IFNE, branch("A"),

                    // ...
                    getMatch(),

                    // }
                    label("A")
                );
            }
        }.targetMethod(renderBlockFluids));

        classMod.addPatch(new BytecodePatch(classMod) {
            private int tessellatorRegister;
            private final int[] vertexOrder = new int[]{0, 1, 2, 3, 3, 2, 1, 0};
            private final int firstPatchOffset;

            {
                firstPatchOffset = getMinecraftVersion().compareTo("13w48a") >= 0 ? 1 : 0;

                targetMethod(renderBlockFluids);

                addPreMatchSignature(new BytecodeSignature(classMod) {
                    @Override
                    public String getMatchExpression() {
                        return buildExpression(
                            reference(GETSTATIC, tessellator),
                            capture(anyASTORE)
                        );
                    }

                    @Override
                    public boolean afterMatch() {
                        tessellatorRegister = extractRegisterNum(getCaptureGroup(1));
                        return true;
                    }
                });
            }

            @Override
            public String getDescription() {
                return "smooth biome colors (water part 2)";
            }

            @Override
            public String getMatchExpression() {
                String expr = build(
                    registerLoadStore(ALOAD, tessellatorRegister),
                    nonGreedy(any(0, 20)),
                    reference(INVOKEVIRTUAL, addVertexWithUV)
                );
                return buildExpression(
                    // tessellator.addVertexWithUV(...); x4 or x8
                    capture(expr),
                    capture(expr),
                    capture(expr),
                    capture(expr),
                    optional(build(
                        capture(expr),
                        capture(expr),
                        capture(expr),
                        capture(expr)
                    ))
                );
            }

            @Override
            public byte[] getReplacementBytes() {
                byte[] code = new byte[0];
                for (int i = 0; i < 8; i++) {
                    byte[] orig = getCaptureGroup(i + 1);
                    if (orig == null) {
                        break;
                    }
                    // *sigh*
                    // 13w47e: draws top face in order 0 1 2 3 3 2 1 0
                    // 13w48a: draws top face in order 0 1 2 3 0 3 2 1
                    // side faces are drawn 0 1 2 3 3 2 1 0 regardless
                    int vertex = vertexOrder[i];
                    if (i >= 4 && getMethodMatchCount() == 0) {
                        vertex = (vertex + firstPatchOffset) % 4;
                    }
                    code = buildCode(
                        code,

                        // tessellator.setColorOpaque_F(this.colorRedxxx, this.colorGreenxxx, this.colorBluexxx);
                        registerLoadStore(ALOAD, tessellatorRegister),
                        getVertexColor(vertex, 0),
                        getVertexColor(vertex, 1),
                        getVertexColor(vertex, 2),
                        reference(INVOKEVIRTUAL, setColorOpaque_F),

                        // tessellator.addVertexWithUV(...);
                        orig
                    );
                }
                return buildCode(
                    // if (ColorizeBlock.isSmooth) {
                    reference(GETSTATIC, isSmooth),
                    IFEQ, branch("A"),

                    // tessellator.setColorOpaque_F(this.colorRedxxx, this.colorGreenxxx, this.colorBluexxx);
                    // tessellator.addVertexWithUV(...);
                    // x4 or x8
                    code,

                    GOTO, branch("B"),

                    // } else {
                    label("A"),

                    // ...
                    getMatch(),

                    // }
                    label("B")
                );
            }

            private byte[] getVertexColor(int vertex, int channel) {
                return buildCode(
                    ALOAD_0,
                    reference(GETFIELD, new FieldRef(classMod.getDeobfClass(), "color" + colorNames[channel] + vertexNames[vertex], "F"))
                );
            }
        });

        classMod.addPatch(new BytecodePatch(classMod) {
            @Override
            public String getDescription() {
                return "smooth biome colors (water part 3)";
            }

            @Override
            public String getMatchExpression() {
                return buildExpression(
                    ALOAD_0,
                    PositionMod.havePositionClass() ?
                        build(
                            // this.renderFace(x, y + var36, z, ..., Direction.DOWN);
                            anyDLOAD,
                            anyDLOAD,
                            anyDLOAD,
                            DADD,
                            anyDLOAD,
                            nonGreedy(any(0, 20)),
                            reference(GETSTATIC, DirectionMod.DOWN)
                        ) :
                        build(
                            // this.renderFaceYNeg(block, (double) i, (double) j + var32, (double) k, ...);
                            ALOAD_1,
                            ILOAD_2,
                            I2D,
                            ILOAD_3,
                            I2D,
                            anyDLOAD,
                            DADD,
                            ILOAD, 4,
                            I2D,
                            nonGreedy(any(0, 20))
                        ),
                    anyReference(INVOKEVIRTUAL),

                    optional(build(
                        // flag = true;
                        push(1),
                        anyISTORE
                    ))
                );
            }

            @Override
            public byte[] getReplacementBytes() {
                return buildCode(
                    // if (ColorizeBlock.isSmooth) {
                    reference(GETSTATIC, isSmooth),
                    IFEQ, branch("A"),

                    // this.enableAO = true;
                    ALOAD_0,
                    push(1),
                    reference(PUTFIELD, enableAO),

                    // }
                    label("A"),

                    // ...
                    getMatch(),

                    // this.enableAO = false;
                    ALOAD_0,
                    push(0),
                    reference(PUTFIELD, enableAO)
                );
            }
        }.targetMethod(renderBlockFluids));
    }

    private static void setupCauldron(com.prupe.mcpatcher.ClassMod classMod, final MethodRef renderBlockCauldron) {
        final MethodRef computeWaterColor2 = new MethodRef(MCPatcherUtils.COLORIZE_BLOCK_CLASS, "computeWaterColor", "()V");

        classMod.addPatch(new TessellatorPatch(classMod) {
            {
                setInsertAfter(true);
                targetMethod(renderBlockCauldron);
            }

            @Override
            public String getDescription() {
                return "colorize cauldron water";
            }

            @Override
            public String getMatchExpression() {
                return buildExpression(or(
                        build(
                            // vanilla: BlockLiquid.getBlockIconByName("water_still")
                            push("water_still"),
                            anyReference(INVOKESTATIC)
                        ),
                        build(
                            // btw: block.getBlockIconFromSide(2)
                            ALOAD_1,
                            push(2),
                            anyReference(INVOKEVIRTUAL)
                        )
                    ),
                    anyASTORE
                );
            }

            @Override
            public byte[] getReplacementBytes() {
                return buildCode(
                    // Colorizer.computeWaterColor();
                    reference(INVOKESTATIC, computeWaterColor2),

                    // tessellator.setColorOpaque(Colorizer.setColor[0], Colorizer.setColor[1], Colorizer.setColor[2]);
                    ALOAD, tessellatorRegister,
                    reference(GETSTATIC, setColor),
                    push(0),
                    FALOAD,
                    reference(GETSTATIC, setColor),
                    push(1),
                    FALOAD,
                    reference(GETSTATIC, setColor),
                    push(2),
                    FALOAD,
                    reference(INVOKEVIRTUAL, setColorOpaque_F)
                );
            }
        });
    }

}
