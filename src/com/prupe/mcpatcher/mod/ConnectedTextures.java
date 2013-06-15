package com.prupe.mcpatcher.mod;

import com.prupe.mcpatcher.*;
import javassist.bytecode.AccessFlag;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static com.prupe.mcpatcher.BinaryRegex.*;
import static com.prupe.mcpatcher.BytecodeMatcher.*;
import static javassist.bytecode.Opcode.*;

public class ConnectedTextures extends Mod {
    private final MethodRef startCTM = new MethodRef(MCPatcherUtils.CTM_UTILS_CLASS, "start", "()V");
    private final MethodRef finishCTM = new MethodRef(MCPatcherUtils.CTM_UTILS_CLASS, "finish", "()V");

    public ConnectedTextures() {
        name = MCPatcherUtils.CONNECTED_TEXTURES;
        author = "MCPatcher";
        description = "Enables support for connected, randomized, and other custom terrain textures.";
        version = "2.4";

        addDependency(MCPatcherUtils.BASE_TEXTURE_PACK_MOD);
        addDependency(MCPatcherUtils.BASE_TILESHEET_MOD);

        configPanel = new ConfigPanel();

        addClassMod(new BaseMod.IBlockAccessMod(this));
        addClassMod(new BaseMod.TessellatorMod(this));
        addClassMod(new BaseMod.IconMod(this));
        addClassMod(new BaseMod.ResourceAddressMod(this));
        addClassMod(new BlockMod());
        addClassMod(new RenderBlocksMod());
        addClassMod(new WorldRendererMod());

        addClassFile(MCPatcherUtils.CTM_UTILS_CLASS);
        addClassFile(MCPatcherUtils.CTM_UTILS_CLASS + "$1");
        addClassFile(MCPatcherUtils.CTM_UTILS_CLASS + "$2");
        addClassFile(MCPatcherUtils.CTM_UTILS_CLASS + "$3");
        addClassFile(MCPatcherUtils.CTM_UTILS_CLASS + "$TileOverrideIterator");
        addClassFile(MCPatcherUtils.TILE_OVERRIDE_INTERFACE);
        addClassFile(MCPatcherUtils.TILE_OVERRIDE_CLASS);
        addClassFile(MCPatcherUtils.TILE_OVERRIDE_IMPL_CLASS);
        addClassFile(MCPatcherUtils.TILE_OVERRIDE_IMPL_CLASS + "$CTM");
        addClassFile(MCPatcherUtils.TILE_OVERRIDE_IMPL_CLASS + "$Random1");
        addClassFile(MCPatcherUtils.TILE_OVERRIDE_IMPL_CLASS + "$Fixed");
        addClassFile(MCPatcherUtils.TILE_OVERRIDE_IMPL_CLASS + "$Horizontal");
        addClassFile(MCPatcherUtils.TILE_OVERRIDE_IMPL_CLASS + "$HorizontalVertical");
        addClassFile(MCPatcherUtils.TILE_OVERRIDE_IMPL_CLASS + "$Vertical");
        addClassFile(MCPatcherUtils.TILE_OVERRIDE_IMPL_CLASS + "$VerticalHorizontal");
        addClassFile(MCPatcherUtils.TILE_OVERRIDE_IMPL_CLASS + "$Top");
        addClassFile(MCPatcherUtils.TILE_OVERRIDE_IMPL_CLASS + "$Repeat");
        addClassFile(MCPatcherUtils.TILE_OVERRIDE_IMPL_CLASS + "$BetterGrass");
        addClassFile(MCPatcherUtils.GLASS_PANE_RENDERER_CLASS);
        addClassFile(MCPatcherUtils.RENDER_PASS_API_CLASS);

        BaseTexturePackMod.earlyInitialize(2, MCPatcherUtils.CTM_UTILS_CLASS, "reset");
    }

    private class ConfigPanel extends ModConfigPanel {
        private JPanel panel;
        private JCheckBox glassPaneCheckBox;
        private JCheckBox grassCheckBox;
        private JCheckBox standardCheckBox;
        private JCheckBox nonStandardCheckBox;
        private JCheckBox debugCheckBox;
        private JComboBox splitComboBox;

        public ConfigPanel() {
            standardCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Config.set(MCPatcherUtils.CONNECTED_TEXTURES, "standard", standardCheckBox.isSelected());
                }
            });

            nonStandardCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Config.set(MCPatcherUtils.CONNECTED_TEXTURES, "nonStandard", nonStandardCheckBox.isSelected());
                }
            });

            glassPaneCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Config.set(MCPatcherUtils.CONNECTED_TEXTURES, "glassPane", glassPaneCheckBox.isSelected());
                }
            });

            grassCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Config.set(MCPatcherUtils.CONNECTED_TEXTURES, "grass", grassCheckBox.isSelected());
                }
            });

            debugCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Config.set(MCPatcherUtils.CONNECTED_TEXTURES, "debugTextures", debugCheckBox.isSelected());
                }
            });

            splitComboBox.addItem("Never");
            splitComboBox.addItem("As needed");
            splitComboBox.addItem("Always");
            splitComboBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Config.set(MCPatcherUtils.CONNECTED_TEXTURES, "splitTextures", splitComboBox.getSelectedIndex());
                }
            });
        }

        @Override
        public JPanel getPanel() {
            return panel;
        }

        @Override
        public void load() {
            standardCheckBox.setSelected(Config.getBoolean(MCPatcherUtils.CONNECTED_TEXTURES, "standard", true));
            nonStandardCheckBox.setSelected(Config.getBoolean(MCPatcherUtils.CONNECTED_TEXTURES, "nonStandard", true));
            glassPaneCheckBox.setSelected(Config.getBoolean(MCPatcherUtils.CONNECTED_TEXTURES, "glassPane", true));
            grassCheckBox.setSelected(Config.getBoolean(MCPatcherUtils.CONNECTED_TEXTURES, "grass", false));
            debugCheckBox.setSelected(Config.getBoolean(MCPatcherUtils.CONNECTED_TEXTURES, "debugTextures", false));

            showAdvancedOption(debugCheckBox);

            switch (Config.getInt(MCPatcherUtils.CONNECTED_TEXTURES, "splitTextures", 1)) {
                case 0:
                    splitComboBox.setSelectedIndex(0);
                    break;

                default:
                    splitComboBox.setSelectedIndex(1);
                    break;

                case 2:
                    splitComboBox.setSelectedIndex(2);
                    break;
            }
        }

        @Override
        public void save() {
        }
    }

    private class BlockMod extends BaseMod.BlockMod {
        BlockMod() {
            super(ConnectedTextures.this);

            final FieldRef blockMaterial = new FieldRef(getDeobfClass(), "blockMaterial", "LMaterial;");
            final MethodRef getBlockIcon = new MethodRef(getDeobfClass(), "getBlockIcon", "(LIBlockAccess;IIII)LIcon;");
            final InterfaceMethodRef getBlockMetadata = new InterfaceMethodRef("IBlockAccess", "getBlockMetadata", "(III)I");
            final MethodRef getBlockIconFromSideAndMetadata = new MethodRef(getDeobfClass(), "getBlockIconFromSideAndMetadata", "(II)LIcon;");
            final MethodRef getShortName = new MethodRef(getDeobfClass(), "getShortName", "()Ljava/lang/String;");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        begin(),
                        ALOAD_0,
                        ILOAD, 5,
                        ALOAD_1,
                        ILOAD_2,
                        ILOAD_3,
                        ILOAD, 4,
                        captureReference(INVOKEINTERFACE),
                        captureReference(INVOKEVIRTUAL),
                        ARETURN,
                        end()
                    );
                }
            }
                .setMethod(getBlockIcon)
                .addXref(1, getBlockMetadata)
                .addXref(2, getBlockIconFromSideAndMetadata)
            );

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(" is already occupied by ")
                    );
                }
            }
                .matchConstructorOnly(true)
                .setMethod(new MethodRef(getDeobfClass(), "<init>", "(ILMaterial;)V"))
            );

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push("MISSING_ICON_TILE_")
                    );
                }
            }.setMethod(getShortName));

            addMemberMapper(new FieldMapper(blockMaterial).accessFlag(AccessFlag.PUBLIC, true));

            addPatch(new MakeMemberPublicPatch(getShortName));
        }
    }

    private class RenderBlocksMod extends BaseMod.RenderBlocksMod {
        private final MethodRef[] faceMethods = new MethodRef[6];
        private final FieldRef overrideBlockTexture = new FieldRef(getDeobfClass(), "overrideBlockTexture", "LIcon;");
        private final FieldRef blockAccess = new FieldRef(getDeobfClass(), "blockAccess", "LIBlockAccess;");
        private final FieldRef fancyGrass = new FieldRef(getDeobfClass(), "fancyGrass", "Z");
        private final FieldRef instance = new FieldRef("Tessellator", "instance", "LTessellator;");
        private final MethodRef renderBlockByRenderType = new MethodRef(getDeobfClass(), "renderBlockByRenderType", "(LBlock;III)Z");
        private final MethodRef renderStandardBlock = new MethodRef(getDeobfClass(), "renderStandardBlock", "(LBlock;III)Z");
        private final MethodRef renderStandardBlockWithColorMultiplier = new MethodRef(getDeobfClass(), "renderStandardBlockWithColorMultiplier", "(LBlock;IIIFFF)Z");
        private final MethodRef hasOverrideTexture = new MethodRef(getDeobfClass(), "hasOverrideTexture", "()Z");
        private final MethodRef drawCrossedSquares = new MethodRef(getDeobfClass(), "drawCrossedSquares", "(LBlock;IDDDF)V");
        private final MethodRef renderBlockPane = new MethodRef(getDeobfClass(), "renderBlockPane", "(LBlockPane;III)Z");
        private final MethodRef renderBlockBrewingStand = new MethodRef(getDeobfClass(), "renderBlockBrewingStand", "(LBlockBrewingStand;III)Z");
        private final MethodRef addVertexWithUV = new MethodRef("Tessellator", "addVertexWithUV", "(DDDDD)V");
        private final MethodRef setColorOpaque_F = new MethodRef("Tessellator", "setColorOpaque_F", "(FFF)V");
        private final MethodRef renderBlockAsItem = new MethodRef(getDeobfClass(), "renderBlockAsItem", "(LBlock;IF)V");
        private final MethodRef renderBlockAsItemVanilla = new MethodRef(getDeobfClass(), "renderBlockAsItemVanilla", "(LBlock;IF)V"); // added by BTW 4.68
        private final MethodRef getIconBySideAndMetadata = new MethodRef(getDeobfClass(), "getIconBySideAndMetadata", "(LBlock;II)LIcon;");
        private final MethodRef getIconBySide = new MethodRef(getDeobfClass(), "getIconBySide", "(LBlock;I)LIcon;");
        private final InterfaceMethodRef getMinU = new InterfaceMethodRef("Icon", "getMinU", "()F");
        private final InterfaceMethodRef getMinV = new InterfaceMethodRef("Icon", "getMinV", "()F");
        private final InterfaceMethodRef getInterpolatedU = new InterfaceMethodRef("Icon", "getInterpolatedU", "(D)F");
        private final MethodRef getRenderType = new MethodRef("Block", "getRenderType", "()I");
        private final MethodRef getTile = new MethodRef(MCPatcherUtils.CTM_UTILS_CLASS, "getTile", "(LRenderBlocks;LBlock;IIIILIcon;LTessellator;)LIcon;");
        private final MethodRef getTileNoFace = new MethodRef(MCPatcherUtils.CTM_UTILS_CLASS, "getTile", "(LRenderBlocks;LBlock;IIILIcon;LTessellator;)LIcon;");
        private final MethodRef getTileBySideAndMetadata = new MethodRef(MCPatcherUtils.CTM_UTILS_CLASS, "getTile", "(LRenderBlocks;LBlock;IILTessellator;)LIcon;");
        private final MethodRef getTileBySide = new MethodRef(MCPatcherUtils.CTM_UTILS_CLASS, "getTile", "(LRenderBlocks;LBlock;ILTessellator;)LIcon;");
        private final MethodRef getTessellator = new MethodRef(MCPatcherUtils.TESSELLATOR_UTILS_CLASS, "getTessellator", "(LTessellator;LIcon;)LTessellator;");

        RenderBlocksMod() {
            super(ConnectedTextures.this);

            mapRenderTypeMethod(0, renderStandardBlock);

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        begin(),
                        ALOAD_0,
                        captureReference(GETFIELD),
                        or(
                            build(IFNULL, any(2), push(1)),
                            build(IFNONNULL, any(2), push(0))
                        )
                    );
                }
            }
                .setMethod(hasOverrideTexture)
                .addXref(1, overrideBlockTexture)
            );

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_1,
                        captureReference(INVOKEVIRTUAL),
                        ISTORE, 5
                    );
                }
            }
                .setMethod(renderBlockByRenderType)
                .addXref(1, getRenderType)
            );

            addMemberMapper(new FieldMapper(overrideBlockTexture));
            addMemberMapper(new FieldMapper(blockAccess));

            setupFastGrass();
            setupStandardBlocks();
            setupNonStandardBlocks();
            setupGlassPanes();
            setupHeldBlocks();
        }

        abstract private class RenderBlocksPatch extends BytecodePatch {
            protected int tessellatorRegister;
            protected int iconRegister;

            {
                addPreMatchSignature(new BytecodeSignature() {
                    @Override
                    public String getMatchExpression() {
                        return buildExpression(
                            reference(GETSTATIC, instance),
                            capture(anyASTORE)
                        );
                    }

                    @Override
                    public boolean afterMatch() {
                        tessellatorRegister = extractRegisterNum(getCaptureGroup(1));
                        return true;
                    }
                });

                setInsertAfter(true);
            }

            @Override
            public String getDescription() {
                return "override texture (" + getTextureType() + ")";
            }

            @Override
            public String getMatchExpression() {
                return buildExpression(
                    ALOAD_0,
                    or(
                        build(reference(INVOKEVIRTUAL, hasOverrideTexture), IFEQ, any(2)),
                        build(reference(GETFIELD, overrideBlockTexture), IFNULL, any(2))
                    ),

                    ALOAD_0,
                    reference(GETFIELD, overrideBlockTexture),
                    capture(anyASTORE)
                );
            }

            @Override
            public byte[] getReplacementBytes() {
                iconRegister = getIconRegister();
                final byte[] returnCode;
                if (getMethodInfo().getDescriptor().endsWith("V")) {
                    returnCode = new byte[]{(byte) RETURN};
                } else {
                    returnCode = new byte[]{ICONST_0, (byte) IRETURN};
                }
                return buildCode(
                    // icon = CTMUtils.getTile(this, block, ..., icon, tessellator);
                    ALOAD_0,
                    ALOAD_1,
                    getCTMUtilsArgs(),
                    registerLoadStore(ALOAD, iconRegister),
                    registerLoadStore(ALOAD, tessellatorRegister),
                    reference(INVOKESTATIC, getCTMUtilsMethod()),
                    registerLoadStore(ASTORE, iconRegister),

                    // if (icon == null) {
                    registerLoadStore(ALOAD, iconRegister),
                    IFNONNULL, branch("A"),

                    // return ...;
                    returnCode,

                    // }
                    label("A"),

                    // tessellator = CTMUtils.getTessellator(tessellator, icon);
                    registerLoadStore(ALOAD, tessellatorRegister),
                    registerLoadStore(ALOAD, iconRegister),
                    reference(INVOKESTATIC, getTessellator),
                    registerLoadStore(ASTORE, tessellatorRegister)
                );
            }

            protected int getIconRegister() {
                return extractRegisterNum(getCaptureGroup(1));
            }

            abstract protected String getTextureType();

            abstract protected byte[] getCTMUtilsArgs();

            abstract protected MethodRef getCTMUtilsMethod();
        }

        private void mapRenderTypeMethod(final int type, MethodRef renderMethod) {
            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ILOAD, 5,
                        (type == 0 ?
                            build(IFNE) :
                            build(push(type), IF_ICMPNE)
                        ),
                        any(2),
                        ALOAD_0,
                        ALOAD_1,
                        optional(anyReference(CHECKCAST)),
                        ILOAD_2,
                        ILOAD_3,
                        ILOAD, 4,
                        capture(build(subset(new int[]{INVOKEVIRTUAL, INVOKESPECIAL}, true), any(2)))
                    );
                }
            }
                .setMethod(renderBlockByRenderType)
                .addXref(1, renderMethod)
            );
        }

        private void setupFastGrass() {
            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(0.5f),
                        anyFSTORE,
                        push(1.0f),
                        anyFSTORE,
                        push(0.8f),
                        anyFSTORE,
                        push(0.6f),
                        anyFSTORE
                    );
                }
            }.setMethod(renderStandardBlockWithColorMultiplier));

            addMemberMapper(new FieldMapper(fancyGrass).accessFlag(AccessFlag.STATIC, true));

            addPatch(new BytecodePatch() {
                private int face;

                @Override
                public String getDescription() {
                    return "apply color multiplier to side grass texture (non-AO, fast graphics)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // tessellator.setColorOpaque_F(r, g, b);
                        ALOAD, capture(any()),
                        FLOAD, capture(any()),
                        FLOAD, capture(any()),
                        FLOAD, capture(any()),
                        reference(INVOKEVIRTUAL, setColorOpaque_F)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    if (face++ < 2) {
                        return null;
                    }
                    return buildCode(
                        // if (CTMUtils.isBetterGrass(this.blockAccess, block, i, j, k, face)) {
                        ALOAD_0,
                        reference(GETFIELD, blockAccess),
                        ALOAD_1,
                        ILOAD_2,
                        ILOAD_3,
                        ILOAD, 4,
                        push(face - 1),
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.CTM_UTILS_CLASS, "isBetterGrass", "(LIBlockAccess;LBlock;IIII)Z")),
                        IFEQ, branch("A"),

                        // tessellator.setColorOpaque_F(r * par5, g * par6, b * par7);
                        ALOAD, getCaptureGroup(1),
                        FLOAD, getCaptureGroup(2),
                        FLOAD, 5,
                        FMUL,
                        FLOAD, getCaptureGroup(3),
                        FLOAD, 6,
                        FMUL,
                        FLOAD, getCaptureGroup(4),
                        FLOAD, 7,
                        FMUL,
                        reference(INVOKEVIRTUAL, setColorOpaque_F),
                        GOTO, branch("B"),

                        // } else {
                        label("A"),

                        // tessellator.setColorOpaque_F(r, g, b);
                        getMatch(),

                        // }
                        label("B")
                    );
                }
            }.targetMethod(renderStandardBlockWithColorMultiplier));

            if (getMinecraftVersion().compareTo("1.5.1") >= 0 && !getMinecraftVersion().getVersionString().matches("2\\.0_(blue|purple)")) {
                setupFastGrassPost151();
            } else {
                setupFastGrassPre151();
            }
        }

        private void setupFastGrassPre151() {
            addPatch(new BytecodePatch() {
                private final int[] faces = new int[6];
                private boolean matched;

                {
                    addPreMatchSignature(new BytecodeSignature() {
                        @Override
                        public String getMatchExpression() {
                            return buildExpression(
                                push(1),
                                capture(anyISTORE),
                                push(1),
                                capture(anyISTORE),
                                push(1),
                                capture(anyISTORE),
                                push(1),
                                capture(anyISTORE),
                                push(1),
                                capture(anyISTORE),
                                push(1),
                                capture(anyISTORE)
                            );
                        }

                        @Override
                        public boolean afterMatch() {
                            for (int i = 0; i < faces.length; i++) {
                                faces[i] = extractRegisterNum(getCaptureGroup(i + 1));
                            }
                            return true;
                        }
                    });
                }

                @Override
                public String getDescription() {
                    return "apply color multiplier to side grass texture (AO, fast graphics)";
                }

                @Override
                public String getMatchExpression() {
                    String istore = build(ISTORE, subset(faces, true));
                    return buildExpression(or(
                        repeat(build(push(0), istore), 5),
                        build(
                            push(0),
                            repeat(build(DUP, istore), 4),
                            istore
                        )
                    ));
                }

                @Override
                public byte[] getReplacementBytes() {
                    if (matched) {
                        return null;
                    }
                    matched = true;
                    return buildCode(
                        // if (RenderBlocks.fancyGrass) {
                        reference(GETSTATIC, fancyGrass),
                        IFEQ, branch("A"),

                        // ...
                        getMatch(),
                        GOTO, branch("B"),

                        // } else {
                        label("A"),
                        getCodeForFace(5),
                        getCodeForFace(4),
                        getCodeForFace(3),
                        getCodeForFace(2),
                        push(0),
                        ISTORE, faces[0],

                        // }
                        label("B")
                    );
                }

                private byte[] getCodeForFace(int face) {
                    return buildCode(
                        ALOAD_0,
                        reference(GETFIELD, blockAccess),
                        ALOAD_1,
                        ILOAD_2,
                        ILOAD_3,
                        ILOAD, 4,
                        push(face),
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.CTM_UTILS_CLASS, "isBetterGrass", "(LIBlockAccess;LBlock;IIII)Z")),
                        ISTORE, faces[face]
                    );
                }
            }.targetMethod(renderStandardBlockWithAmbientOcclusion));
        }

        private void setupFastGrassPost151() {
            addPatch(new BytecodePatch() {
                private int flagRegister;
                private int face;

                {
                    final MethodRef stringEquals = new MethodRef("java/lang/String", "equals", "(Ljava/lang/Object;)Z");

                    addPreMatchSignature(new BytecodeSignature() {
                        @Override
                        public String getMatchExpression() {
                            return buildExpression(
                                push("grass_top"),
                                reference(INVOKEVIRTUAL, stringEquals),
                                IFEQ_or_IFNE, any(2),
                                nonGreedy(any(0, 8)),
                                push(0),
                                capture(anyISTORE)
                            );
                        }

                        @Override
                        public boolean afterMatch() {
                            flagRegister = extractRegisterNum(getCaptureGroup(1));
                            return true;
                        }
                    });
                }

                @Override
                public String getDescription() {
                    return "apply color multiplier to side grass texture (AO, fast graphics, post-1.5.1)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ILOAD, flagRegister
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    switch (face) {
                        case 0:
                            face++;
                            return null;

                        case 1:
                            // NOTE: Mojang's code does not check flag for bottom face
                            face++;

                        case 2:
                        case 3:
                        case 4:
                        case 5:
                            break;

                        default:
                            return null;
                    }
                    return buildCode(
                        // flag || (!RenderBlocks.fancyGrass && CTMUtils.isBetterGrass(...))
                        IFNE, branch("A"),

                        reference(GETSTATIC, fancyGrass),
                        IFNE, branch("B"),

                        ALOAD_0,
                        reference(GETFIELD, blockAccess),
                        ALOAD_1,
                        ILOAD_2,
                        ILOAD_3,
                        ILOAD, 4,
                        push(face++),
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.CTM_UTILS_CLASS, "isBetterGrass", "(LIBlockAccess;LBlock;IIII)Z")),
                        GOTO, branch("C"),

                        label("A"),
                        push(1),
                        GOTO, branch("C"),

                        label("B"),
                        push(0),

                        label("C")
                    );
                }
            }
                .setInsertAfter(true)
                .targetMethod(renderStandardBlockWithAmbientOcclusion)
            );
        }

        private void setupStandardBlocks() {
            setupBlockFace(0, "Bottom", "Bottom");
            setupBlockFace(1, "Top", "Top");
            setupBlockFace(2, "North", "East");
            setupBlockFace(3, "South", "West");
            setupBlockFace(4, "West", "North");
            setupBlockFace(5, "East", "South");

            addMemberMapper(new MethodMapper(faceMethods));
        }

        private void setupBlockFace(final int face, final String direction, String altDirection) {
            final MethodRef altMethod = new MethodRef(getDeobfClass(), "RenderFull" + altDirection + "Face", "(LBlock;DDDLIcon;)V");
            faceMethods[face] = new MethodRef(getDeobfClass(), "render" + direction + "Face", "(LBlock;DDDLIcon;)V");

            addPatch(new RenderBlocksPatch() {
                @Override
                protected int getIconRegister() {
                    return 8;
                }

                @Override
                public String getMatchExpression() {
                    if (isAlternateMethod()) {
                        return buildExpression(
                            reference(GETSTATIC, instance),
                            anyASTORE
                        );
                    } else {
                        return super.getMatchExpression();
                    }
                }

                @Override
                protected String getTextureType() {
                    return direction.toLowerCase() + " face";
                }

                @Override
                protected byte[] getCTMUtilsArgs() {
                    return buildCode(
                        DLOAD_2,
                        D2I,
                        DLOAD, 4,
                        D2I,
                        DLOAD, 6,
                        D2I,
                        push(face)
                    );
                }

                @Override
                protected MethodRef getCTMUtilsMethod() {
                    return getTile;
                }

                private boolean isAlternateMethod() {
                    return getMethodInfo().getName().startsWith("RenderFull");
                }
            }.targetMethod(faceMethods[face], altMethod));
        }

        private void setupNonStandardBlocks() {
            addMemberMapper(new MethodMapper(drawCrossedSquares));
            mapRenderTypeMethod(25, renderBlockBrewingStand);

            addPatch(new RenderBlocksPatch() {
                {
                    setInsertBefore(true);
                    skipMethod(renderBlockPane);
                }

                @Override
                public boolean filterMethod() {
                    return getMethodInfo().getDescriptor().matches("\\(L[a-z]+;III.*[IVZ]");
                }

                @Override
                protected String getTextureType() {
                    return "other blocks";
                }

                @Override
                public String getMatchExpression() {
                    final InterfaceMethodRef method;
                    if (getMethodInfo().getDescriptor().equals(map(renderBlockBrewingStand).getType())) {
                        method = getMinV;
                    } else {
                        method = getMinU;
                    }
                    return buildExpression(
                        capture(anyALOAD),
                        reference(INVOKEINTERFACE, method),
                        F2D,
                        anyDSTORE
                    );
                }

                @Override
                protected byte[] getCTMUtilsArgs() {
                    return buildCode(
                        ILOAD_2,
                        ILOAD_3,
                        ILOAD, 4
                    );
                }

                @Override
                protected MethodRef getCTMUtilsMethod() {
                    return getTileNoFace;
                }
            });

            addPatch(new RenderBlocksPatch() {
                @Override
                protected String getTextureType() {
                    return "crossed squares";
                }

                @Override
                protected byte[] getCTMUtilsArgs() {
                    return buildCode(
                        DLOAD_3,
                        D2I,
                        DLOAD, 5,
                        D2I,
                        DLOAD, 7,
                        D2I
                    );
                }

                @Override
                protected MethodRef getCTMUtilsMethod() {
                    return getTileNoFace;
                }
            }.targetMethod(drawCrossedSquares));
        }

        private void setupGlassPanes() {
            mapRenderTypeMethod(18, renderBlockPane);

            addPatch(new BytecodePatch() {
                private int iconRegister;

                {
                    addPreMatchSignature(new BytecodeSignature() {
                        @Override
                        public String getMatchExpression() {
                            return buildExpression(
                                ALOAD, capture(any()),
                                reference(INVOKEINTERFACE, getMinU)
                            );
                        }

                        @Override
                        public boolean afterMatch() {
                            iconRegister = getCaptureGroup(1)[0] & 0xff;
                            return true;
                        }
                    });
                }

                @Override
                public String getDescription() {
                    return "override texture (glass pane)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // connectEast = par1BlockPane.canThisPaneConnectToThisBlockID(this.blockAccess.getBlockId(i + 1, j, k));
                        ALOAD_1,
                        ALOAD_0,
                        reference(GETFIELD, blockAccess),
                        ILOAD_2,
                        push(1),
                        IADD,
                        ILOAD_3,
                        ILOAD, 4,
                        anyReference(INVOKEINTERFACE),
                        anyReference(INVOKEVIRTUAL),
                        ISTORE, capture(any())
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    int reg = getCaptureGroup(1)[0] & 0xff;
                    Logger.log(Logger.LOG_BYTECODE, "glass side connect flags (%d %d %d %d)",
                        reg - 3, reg - 2, reg - 1, reg
                    );
                    return buildCode(
                        // GlassPaneRenderer.render(renderBlocks, blockPane, i, j, k, connectNorth, ...);
                        ALOAD_0,
                        ALOAD_1,
                        ALOAD, iconRegister,
                        ILOAD_2,
                        ILOAD_3,
                        ILOAD, 4,
                        ILOAD, reg - 3,
                        ILOAD, reg - 2,
                        ILOAD, reg - 1,
                        ILOAD, reg,
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.GLASS_PANE_RENDERER_CLASS, "render", "(LRenderBlocks;LBlock;LIcon;IIIZZZZ)V"))
                    );
                }
            }
                .setInsertAfter(true)
                .targetMethod(renderBlockPane)
            );

            addPatch(new BytecodePatch() {
                private int[] sideUVRegisters;

                {
                    addPreMatchSignature(new BytecodeSignature() {
                        @Override
                        public String getMatchExpression() {
                            return buildExpression(
                                // var28 = (double) var65.interpolateX(7.0);
                                ALOAD, any(),
                                push(7.0),
                                reference(INVOKEINTERFACE, getInterpolatedU),
                                F2D,
                                DSTORE, capture(any())
                            );
                        }

                        @Override
                        public boolean afterMatch() {
                            int reg = getCaptureGroup(1)[0] & 0xff;
                            sideUVRegisters = new int[]{reg, reg + 2, reg + 4, reg + 6, reg + 8};
                            Logger.log(Logger.LOG_CONST, "glass side texture uv registers (%d %d %d %d %d)",
                                reg, reg + 2, reg + 4, reg + 6, reg + 8
                            );
                            return true;
                        }
                    });
                }

                @Override
                public String getDescription() {
                    return "disable default rendering (glass pane faces)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(repeat(build(
                        ALOAD, any(),
                        nonGreedy(any(0, 15)),
                        DLOAD, subset(sideUVRegisters, false),
                        DLOAD, subset(sideUVRegisters, false),
                        reference(INVOKEVIRTUAL, addVertexWithUV)
                    ), 8));
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // if (!GlassPaneRenderer.active) {
                        reference(GETSTATIC, new FieldRef(MCPatcherUtils.GLASS_PANE_RENDERER_CLASS, "active", "Z")),
                        IFNE, branch("A"),

                        // ...
                        getMatch(),

                        // }
                        label("A")
                    );
                }
            }.targetMethod(renderBlockPane));
        }

        private void setupHeldBlocks() {
            addMemberMapper(new MethodMapper(renderBlockAsItem));
            addMemberMapper(new MethodMapper(getIconBySideAndMetadata));
            addMemberMapper(new MethodMapper(getIconBySide));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "setup held items (finish)";
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
                        reference(INVOKESTATIC, finishCTM)
                    );
                }
            }
                .setInsertBefore(true)
                .targetMethod(renderBlockAsItem, renderBlockAsItemVanilla)
            );

            setupHeldBlocks(getIconBySide, getTileBySide, "held blocks");
            setupHeldBlocks(getIconBySideAndMetadata, getTileBySideAndMetadata, "held blocks with metadata");
        }

        private void setupHeldBlocks(final MethodRef from, final MethodRef to, final String name) {
            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override texture (" + name + ")";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        reference(INVOKEVIRTUAL, from)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        reference(INVOKESTATIC, startCTM),
                        ALOAD, 4,
                        reference(INVOKESTATIC, to)
                    );
                }
            }.targetMethod(renderBlockAsItem, renderBlockAsItemVanilla));
        }
    }

    private class WorldRendererMod extends ClassMod {
        WorldRendererMod() {
            final MethodRef updateRenderer = new MethodRef(getDeobfClass(), "updateRenderer", "()V");

            addClassSignature(new ConstSignature(new MethodRef(MCPatcherUtils.GL11_CLASS, "glNewList", "(II)V")));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(1.000001F)
                    );
                }
            }.setMethod(updateRenderer));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "pre render world";
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
                        reference(INVOKESTATIC, startCTM)
                    );
                }
            }.targetMethod(updateRenderer));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "post render world";
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
                        reference(INVOKESTATIC, finishCTM)
                    );
                }
            }
                .setInsertBefore(true)
                .targetMethod(updateRenderer)
            );
        }
    }
}
