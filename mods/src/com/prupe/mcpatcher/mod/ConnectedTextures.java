package com.prupe.mcpatcher.mod;

import com.prupe.mcpatcher.*;
import com.prupe.mcpatcher.basemod.DirectionMod;
import com.prupe.mcpatcher.basemod.PositionMod;
import com.prupe.mcpatcher.mal.BaseTexturePackMod;
import javassist.bytecode.AccessFlag;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import static com.prupe.mcpatcher.BinaryRegex.*;
import static com.prupe.mcpatcher.BytecodeMatcher.*;
import static javassist.bytecode.Opcode.*;

public class ConnectedTextures extends Mod {
    private final MethodRef startCTM = new MethodRef(MCPatcherUtils.CTM_UTILS_CLASS, "start", "(Z)V");
    private final MethodRef finishCTM = new MethodRef(MCPatcherUtils.CTM_UTILS_CLASS, "finish", "()V");

    private final boolean haveBlockRegistry;

    public ConnectedTextures() {
        name = MCPatcherUtils.CONNECTED_TEXTURES;
        author = "MCPatcher";
        description = "Enables support for connected, randomized, and other custom terrain textures.";
        version = "2.6";

        addDependency(MCPatcherUtils.BASE_TEXTURE_PACK_MOD);
        addDependency(MCPatcherUtils.BASE_TILESHEET_MOD);
        addDependency(MCPatcherUtils.BLOCK_API_MOD);
        addDependency(MCPatcherUtils.BIOME_API_MOD);

        configPanel = new ConfigPanel();

        haveBlockRegistry = getMinecraftVersion().compareTo("13w36a") >= 0;

        addClassMod(new BaseMod.IBlockAccessMod(this));
        addClassMod(new BaseMod.TessellatorMod(this));
        addClassMod(new BaseMod.IconMod(this));
        addClassMod(new BaseMod.ResourceLocationMod(this));
        if (PositionMod.havePositionClass()) {
            addClassMod(new PositionMod(this));
            addClassMod(new DirectionMod(this));
        }
        addClassMod(new BlockMod());
        addClassMod(new RenderBlocksMod());
        addClassMod(new WorldRendererMod());

        addClassFile(MCPatcherUtils.CTM_UTILS_CLASS);
        addClassFile(MCPatcherUtils.CTM_UTILS_CLASS + "$1");
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
        addClassFile(MCPatcherUtils.TILE_OVERRIDE_ITERATOR_CLASS);
        addClassFile(MCPatcherUtils.TILE_OVERRIDE_ITERATOR_CLASS + "$IJK");
        addClassFile(MCPatcherUtils.TILE_OVERRIDE_ITERATOR_CLASS + "$Metadata");
        addClassFile(MCPatcherUtils.GLASS_PANE_RENDERER_CLASS);

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
            final MethodRef getBlockIcon = new MethodRef(getDeobfClass(), "getBlockIcon", "(LIBlockAccess;" + PositionMod.getDescriptor() + DirectionMod.getDescriptor() + ")LIcon;");
            final InterfaceMethodRef getBlockMetadata = new InterfaceMethodRef("IBlockAccess", "getBlockMetadata", "(" + PositionMod.getDescriptor() + ")I");
            final MethodRef getBlockIconFromSideAndMetadata = new MethodRef(getDeobfClass(), "getBlockIconFromSideAndMetadata", "(" + DirectionMod.getDescriptor() + "I)LIcon;");
            final MethodRef getShortName = new MethodRef(getDeobfClass(), "getShortName", "()Ljava/lang/String;");
            final MethodRef constructor = new MethodRef(getDeobfClass(), "<init>", "(" + (haveBlockRegistry ? "" : "I") + "LMaterial;)V");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        begin(),
                        ALOAD_0,
                        DirectionMod.passArguments(2 + PositionMod.getDescriptorLength()),
                        ALOAD_1,
                        PositionMod.passArguments(2),
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
                        ALOAD_0,
                        haveBlockRegistry ? ALOAD_1 : ALOAD_2,
                        captureReference(PUTFIELD)
                    );
                }
            }
                .setMethod(constructor)
                .addXref(1, blockMaterial)
            );

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push("tile.")
                    );
                }
            }.setMethod(getShortName));

            addPatch(new MakeMemberPublicPatch(blockMaterial));
            addPatch(new MakeMemberPublicPatch(getShortName));
        }
    }

    private class RenderBlocksMod extends BaseMod.RenderBlocksMod {
        private final MethodRef[] faceMethods = new MethodRef[6];
        private final FieldRef overrideBlockTexture = new FieldRef(getDeobfClass(), "overrideBlockTexture", "LIcon;");
        private final FieldRef instance = new FieldRef("Tessellator", "instance", "LTessellator;");
        private final MethodRef renderBlockByRenderType = new MethodRef(getDeobfClass(), "renderBlockByRenderType", "(LBlock;" + PositionMod.getDescriptor() + ")Z");
        private final MethodRef renderStandardBlock = new MethodRef(getDeobfClass(), "renderStandardBlock", "(LBlock;" + PositionMod.getDescriptor() + ")Z");
        private final MethodRef drawCrossedSquares = new MethodRef(getDeobfClass(), "drawCrossedSquares", "(LIcon;DDDF)V");
        private final MethodRef hasOverrideTexture = new MethodRef(getDeobfClass(), "hasOverrideTexture", "()Z");
        private final MethodRef renderBlockPane1 = new MethodRef(getDeobfClass(), "renderBlockPane1", "(LBlockPane;" + PositionMod.getDescriptor() + ")Z");
        private final MethodRef renderBlockPane2 = new MethodRef(getDeobfClass(), "renderBlockPane2", "(LBlock;" + PositionMod.getDescriptor() + ")Z");
        private final MethodRef renderBlockBrewingStand = new MethodRef(getDeobfClass(), "renderBlockBrewingStand", "(LBlockBrewingStand;" + PositionMod.getDescriptor() + ")Z");
        private final MethodRef addVertexWithUV = new MethodRef("Tessellator", "addVertexWithUV", "(DDDDD)V");
        private final MethodRef renderBlockAsItem = new MethodRef(getDeobfClass(), "renderBlockAsItem", "(LBlock;IF)V");
        private final MethodRef renderBlockAsItemVanilla = new MethodRef(getDeobfClass(), "renderBlockAsItemVanilla", "(LBlock;IF)V"); // added by BTW 4.68
        private final MethodRef getIconBySideAndMetadata = new MethodRef(getDeobfClass(), "getIconBySideAndMetadata", "(LBlock;" + DirectionMod.getDescriptor() + "I)LIcon;");
        private final MethodRef getIconBySide = new MethodRef(getDeobfClass(), "getIconBySide", "(LBlock;" + DirectionMod.getDescriptor() + ")LIcon;");
        private final InterfaceMethodRef getMinU = new InterfaceMethodRef("Icon", "getMinU", "()F");
        private final InterfaceMethodRef getMinV = new InterfaceMethodRef("Icon", "getMinV", "()F");
        private final MethodRef getRenderType = new MethodRef("Block", "getRenderType", "()I");
        private final MethodRef getTile = new MethodRef(MCPatcherUtils.CTM_UTILS_CLASS, "getTile", "(LRenderBlocks;LBlock;IIIILIcon;LTessellator;)LIcon;");
        private final MethodRef getTileNoFace = new MethodRef(MCPatcherUtils.CTM_UTILS_CLASS, "getTile", "(LRenderBlocks;LBlock;IIILIcon;LTessellator;)LIcon;");
        private final MethodRef getTileNoFaceOrBlock = new MethodRef(MCPatcherUtils.CTM_UTILS_CLASS, "getTile", "(LRenderBlocks;IIILIcon;LTessellator;)LIcon;");
        private final MethodRef getTileBySideAndMetadata = new MethodRef(MCPatcherUtils.CTM_UTILS_CLASS, "getTile", "(LRenderBlocks;LBlock;IILTessellator;)LIcon;");
        private final MethodRef getTileBySide = new MethodRef(MCPatcherUtils.CTM_UTILS_CLASS, "getTile", "(LRenderBlocks;LBlock;ILTessellator;)LIcon;");
        private final MethodRef getTileByDirectionAndMetadata = new MethodRef(MCPatcherUtils.CTM_UTILS_CLASS, "getTile", "(LRenderBlocks;LBlock;" + DirectionMod.getDescriptor() + "ILTessellator;)LIcon;");
        private final MethodRef getTileByDirection = new MethodRef(MCPatcherUtils.CTM_UTILS_CLASS, "getTile", "(LRenderBlocks;LBlock;" + DirectionMod.getDescriptor() + "LTessellator;)LIcon;");
        private final MethodRef getTessellator = new MethodRef(MCPatcherUtils.TESSELLATOR_UTILS_CLASS, "getTessellator", "(LTessellator;LIcon;)LTessellator;");
        private final MethodRef round = new MethodRef("java/lang/Math", "round", "(D)J");

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
                        registerLoadStore(ISTORE, 2 + PositionMod.getDescriptorLength())
                    );
                }
            }
                .setMethod(renderBlockByRenderType)
                .addXref(1, getRenderType)
            );

            addMemberMapper(new FieldMapper(overrideBlockTexture));
            addMemberMapper(new FieldMapper(blockAccess));

            setupStandardBlocks();
            setupGlassPanes();
            setupHeldBlocks();
            if (haveBlockRegistry) {
                setupCrossedSquares17();
            }
            setupNonStandardBlocks();
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

                addPreMatchSignature((BytecodeSignature) new BytecodeSignature() {
                    @Override
                    public String getMatchExpression() {
                        return buildExpression(or(
                            build(reference(INVOKESTATIC, getTile)),
                            build(reference(INVOKESTATIC, getTileNoFace)),
                            build(reference(INVOKESTATIC, getTileNoFaceOrBlock)),
                            build(reference(INVOKESTATIC, getTileBySide)),
                            build(reference(INVOKESTATIC, getTileBySideAndMetadata)),
                            build(reference(INVOKESTATIC, getTileByDirection)),
                            build(reference(INVOKESTATIC, getTileByDirectionAndMetadata))
                        ));
                    }
                }.negate(true));

                setInsertAfter(true);
            }

            @Override
            public boolean filterMethod() {
                return (getMethodInfo().getAccessFlags() & AccessFlag.STATIC) == 0 &&
                    getMethodInfo().getDescriptor().matches("\\(L[a-z]+;.*");
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
                    getBlockCode(),
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

            protected byte[] getBlockCode() {
                return new byte[]{ALOAD_1};
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
                        registerLoadStore(ILOAD, 2 + PositionMod.getDescriptorLength()),
                        (type == 0 ?
                            build(IFNE) :
                            build(push(type), IF_ICMPNE)
                        ),
                        any(2),
                        ALOAD_0,
                        ALOAD_1,
                        optional(anyReference(CHECKCAST)),
                        PositionMod.passArguments(2),
                        capture(build(subset(new int[]{INVOKEVIRTUAL, INVOKESPECIAL}, true), any(2)))
                    );
                }
            }
                .setMethod(renderBlockByRenderType)
                .addXref(1, renderMethod)
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
            mapRenderTypeMethod(25, renderBlockBrewingStand);

            addPatch(new RenderBlocksPatch() {
                private String matchDescriptor;

                {
                    setInsertBefore(true);
                    skipMethod(renderBlockPane1);
                    skipMethod(renderBlockPane2);
                }

                @Override
                public boolean filterMethod() {
                    if (!super.filterMethod()) {
                        return false;
                    }
                    if (matchDescriptor == null) {
                        matchDescriptor = "\\(L[a-z]+;" + getClassMap().mapTypeString(PositionMod.getDescriptor()) + ".*[IVZ]";
                    }
                    return super.filterMethod() && getMethodInfo().getDescriptor().matches(matchDescriptor);
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
                        PositionMod.unpackArguments(this, 2)
                    );
                }

                @Override
                protected MethodRef getCTMUtilsMethod() {
                    return getTileNoFace;
                }
            });

            addPatch(new RenderBlocksPatch() {
                private final int[] coordRegisters = new int[3];

                {
                    skipMethod(drawCrossedSquares);
                }

                @Override
                public boolean filterMethod() {
                    if (!super.filterMethod()) {
                        return false;
                    }
                    String descriptor = getMethodInfo().getDescriptor();
                    List<String> types = new ArrayList<String>();
                    List<Integer> registers = new ArrayList<Integer>();
                    parseDescriptor(descriptor, false, types, registers);
                    // renderBlockStemBig/Small have an extra parameter renderMaxY before the x,y,z coords
                    boolean skipOne = descriptor.contains("IDDDD");
                    for (int i = 0; i + 2 < types.size(); i++) {
                        String threeTypes = types.get(i) + types.get(i + 1) + types.get(i + 2);
                        if (threeTypes.equals("III")) {
                            return false;
                        } else if (threeTypes.equals("DDD")) {
                            if (skipOne) {
                                skipOne = false;
                            } else {
                                coordRegisters[0] = registers.get(i);
                                coordRegisters[1] = registers.get(i + 1);
                                coordRegisters[2] = registers.get(i + 2);
                                return true;
                            }
                        }
                    }
                    return false;
                }

                @Override
                protected String getTextureType() {
                    return "other blocks (double)";
                }

                @Override
                protected byte[] getCTMUtilsArgs() {
                    Logger.log(Logger.LOG_CONST, "coord double registers: %d %d %d",
                        coordRegisters[0], coordRegisters[1], coordRegisters[2]
                    );
                    return buildCode(
                        registerLoadStore(DLOAD, coordRegisters[0]),
                        reference(INVOKESTATIC, round),
                        L2I,
                        registerLoadStore(DLOAD, coordRegisters[1]),
                        reference(INVOKESTATIC, round),
                        L2I,
                        registerLoadStore(DLOAD, coordRegisters[2]),
                        reference(INVOKESTATIC, round),
                        L2I
                    );
                }

                @Override
                protected MethodRef getCTMUtilsMethod() {
                    return getTileNoFace;
                }
            });
        }

        private void setupCrossedSquares17() {
            addMemberMapper(new MethodMapper(drawCrossedSquares));

            addPatch(new RenderBlocksPatch() {
                @Override
                protected String getTextureType() {
                    return "crossed squares";
                }

                @Override
                protected byte[] getCTMUtilsArgs() {
                    return buildCode(
                        // x,z coords are randomly offset by -0.15 to 0.15
                        DLOAD_2,
                        reference(INVOKESTATIC, round),
                        L2I,
                        DLOAD, 4,
                        reference(INVOKESTATIC, round),
                        L2I,
                        DLOAD, 6,
                        reference(INVOKESTATIC, round),
                        L2I
                    );
                }

                @Override
                protected MethodRef getCTMUtilsMethod() {
                    return getTileNoFaceOrBlock;
                }

                @Override
                protected int getIconRegister() {
                    return 1;
                }

                @Override
                protected byte[] getBlockCode() {
                    return new byte[0];
                }
            }.targetMethod(drawCrossedSquares));
        }

        private void setupGlassPanes() {
            final FieldRef forgeEast = new FieldRef("net/minecraftforge/common/ForgeDirection", "EAST", "Lnet/minecraftforge/common/ForgeDirection;");
            final MethodRef canPaneConnectToForge = new MethodRef("BlockPane", "canPaneConnectTo", "(LIBlockAccess;IIILnet/minecraftforge/common/ForgeDirection;)Z");
            final boolean haveThickPanes = getMinecraftVersion().compareTo("13w41a") >= 0;
            final MethodRef newRenderPaneThin = new MethodRef(MCPatcherUtils.GLASS_PANE_RENDERER_CLASS, "renderThin", "(LRenderBlocks;LBlock;LIcon;IIIZZZZ)V");
            final MethodRef newRenderPaneThick = new MethodRef(MCPatcherUtils.GLASS_PANE_RENDERER_CLASS, "renderThick", "(LRenderBlocks;LBlock;LIcon;IIIZZZZ)V");
            final MethodRef newRenderPane = haveThickPanes ? newRenderPaneThick : newRenderPaneThin;
            final FieldRef skipPaneRendering = new FieldRef(MCPatcherUtils.GLASS_PANE_RENDERER_CLASS, "skipPaneRendering", "Z");
            final FieldRef skipAllRendering = new FieldRef(MCPatcherUtils.GLASS_PANE_RENDERER_CLASS, "skipAllRendering", "Z");
            final FieldRef skipTopEdgeRendering = new FieldRef(MCPatcherUtils.GLASS_PANE_RENDERER_CLASS, "skipTopEdgeRendering", "Z");
            final FieldRef skipBottomEdgeRendering = new FieldRef(MCPatcherUtils.GLASS_PANE_RENDERER_CLASS, "skipBottomEdgeRendering", "Z");

            mapRenderTypeMethod(18, renderBlockPane1);
            if (haveThickPanes) {
                mapRenderTypeMethod(41, renderBlockPane2);
            }

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
                    return buildExpression(lookBehind(build(
                        ALOAD_1,
                        optional(anyReference(CHECKCAST)),
                        ALOAD_0,
                        reference(GETFIELD, blockAccess),
                        ILOAD_2,
                        or(
                            build(
                                // vanilla:
                                // connectEast = par1BlockPane.canThisPaneConnectToThisBlockID(this.blockAccess.getBlockId(i + 1, j, k));
                                push(1),
                                IADD,
                                ILOAD_3,
                                ILOAD, 4,
                                anyReference(INVOKEINTERFACE),
                                anyReference(INVOKEVIRTUAL)
                            ),
                            build(
                                // forge:
                                // connectEast = par1BlockPane.canPaneConnectTo(this.blockAccess, i, j, k, ForgeDirection.EAST);
                                ILOAD_3,
                                ILOAD, 4,
                                reference(GETSTATIC, forgeEast),
                                reference(INVOKEVIRTUAL, canPaneConnectToForge)
                            )
                        ),
                        capture(anyISTORE)
                    ), true));
                }

                @Override
                public byte[] getReplacementBytes() {
                    int reg = extractRegisterNum(getCaptureGroup(1));
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
                        reference(INVOKESTATIC, newRenderPane),

                        // if (GlassPaneRenderer.skipAllRendering) {
                        reference(GETSTATIC, skipAllRendering),
                        IFEQ, branch("A"),

                        // return true;
                        push(1),
                        IRETURN,

                        // }
                        label("A")
                    );
                }
            });

            addPatch(new RenderPanePatch() {
                @Override
                public String getDescription() {
                    return "disable default rendering (glass pane faces)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(repeat(build(
                        // tessellator.addVertexWithUV(..., paneU, paneV);
                        // x4 or x8
                        ALOAD, any(),
                        nonGreedy(any(0, 15)),
                        DLOAD, subset(sideUVRegisters, false),
                        DLOAD, subset(sideUVRegisters, false),
                        reference(INVOKEVIRTUAL, addVertexWithUV)
                    ), 4, 8));
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // if (!GlassPaneRenderer.active) {
                        reference(GETSTATIC, skipPaneRendering),
                        IFNE, branch("A"),

                        // ...
                        getMatch(),

                        // }
                        label("A")
                    );
                }
            });

            addPatch(new RenderPanePatch() {
                @Override
                public String getDescription() {
                    return "disable glass pane top and bottom edges";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // tesselator.addVertexWithUV(.., <same j expression>, ..., sideU, sideV);
                        // x4 or x8
                        getSubExpression(true),
                        repeat(getSubExpression(false), 3, 7)
                    );
                }

                private String getSubExpression(boolean first) {
                    return build(
                        ALOAD, any(),
                        DLOAD, any(),
                        ILOAD_3,
                        first ? capture(getJExpression()) : backReference(1),
                        DLOAD, any(),
                        DLOAD, subset(sideUVRegisters, true),
                        DLOAD, subset(sideUVRegisters, true),
                        reference(INVOKEVIRTUAL, addVertexWithUV)
                    );
                }

                private String getJExpression() {
                    return or(
                        // (double) (j + 1) + 0.005
                        build(
                            push(1),
                            IADD,
                            I2D,
                            anyLDC,
                            DADD
                        ),
                        // (double) j - 0.005
                        build(
                            I2D,
                            anyLDC,
                            DSUB
                        ),
                        // (double) j + <0.001|0.999>
                        build(
                            I2D,
                            anyLDC,
                            DADD
                        )
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    boolean top;
                    byte[] group = getCaptureGroup(1);
                    if (group[0] == ICONST_1) {
                        top = true;
                    } else if (group[group.length - 1] == DSUB) {
                        top = false;
                    } else if (group[1] == LDC2_W && group.length >= 4) {
                        byte[] tmp = new byte[3];
                        tmp[0] = group[1];
                        tmp[1] = group[2];
                        tmp[2] = group[3];
                        int index = extractConstPoolIndex(tmp);
                        double value = getMethodInfo().getConstPool().getDoubleInfo(index);
                        top = value > 0.5;
                    } else {
                        return null;
                    }
                    return buildCode(
                        // if (!GlassPaneRenderer.skip<Top|Bottom>EdgeRendering) {
                        reference(GETSTATIC, top ? skipTopEdgeRendering : skipBottomEdgeRendering),
                        IFNE, branch("A"),

                        // ...
                        getMatch(),

                        // }
                        label("A")
                    );
                }
            });
        }

        private abstract class RenderPanePatch extends BytecodePatch {
            protected int[] sideUVRegisters;

            {
                targetMethod(renderBlockPane1, renderBlockPane2);

                addPreMatchSignature(new BytecodeSignature() {
                    @Override
                    public String getMatchExpression() {
                        return buildExpression(
                            // var26 = (double) sideIcon.getInterpolatedU(7.0);
                            capture(anyALOAD),
                            push(7.0),
                            anyReference(INVOKEINTERFACE),
                            F2D,
                            capture(anyDSTORE),

                            // var28 = (double) sideIcon.getXXX(...);
                            // x4 or x5
                            repeat(build(
                                backReference(1),
                                optional(anyLDC),
                                anyReference(INVOKEINTERFACE),
                                F2D,
                                anyDSTORE
                            ), 4, 5),

                            // var34 = (double) i;
                            ILOAD_2,
                            I2D,
                            capture(anyDSTORE)
                        );
                    }

                    @Override
                    public boolean afterMatch() {
                        int firstReg = extractRegisterNum(getCaptureGroup(2));
                        int lastReg = extractRegisterNum(getCaptureGroup(3));
                        List<Integer> tmp = new ArrayList<Integer>();
                        for (int i = firstReg; i < lastReg; i += 2) {
                            tmp.add(i);
                        }
                        sideUVRegisters = new int[tmp.size()];
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < tmp.size(); i++) {
                            sideUVRegisters[i] = tmp.get(i);
                            if (sb.length() > 0) {
                                sb.append(' ');
                            }
                            sb.append(sideUVRegisters[i]);
                        }
                        Logger.log(Logger.LOG_CONST, "glass side texture uv registers (%s)", sb.toString());
                        return true;
                    }
                });
            }
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

            setupHeldBlocks(getIconBySide, getTileByDirection, "held blocks");
            setupHeldBlocks(getIconBySideAndMetadata, getTileByDirectionAndMetadata, "held blocks with metadata");
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
                        // CTMUtils.startCTM(false);
                        push(0),
                        reference(INVOKESTATIC, startCTM),
                        ALOAD, 4,
                        reference(INVOKESTATIC, to)
                    );
                }
            }.targetMethod(renderBlockAsItem, renderBlockAsItemVanilla));
        }
    }

    private class WorldRendererMod extends BaseMod.WorldRendererMod {
        WorldRendererMod() {
            super(ConnectedTextures.this);

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
                        // CTMUtils.startCTM(true);
                        push(1),
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
