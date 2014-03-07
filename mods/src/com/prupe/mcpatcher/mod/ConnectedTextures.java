package com.prupe.mcpatcher.mod;

import com.prupe.mcpatcher.*;
import com.prupe.mcpatcher.basemod.*;
import com.prupe.mcpatcher.mal.BaseTexturePackMod;
import javassist.bytecode.AccessFlag;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.prupe.mcpatcher.BinaryRegex.*;
import static com.prupe.mcpatcher.BytecodeMatcher.*;
import static javassist.bytecode.Opcode.*;

public class ConnectedTextures extends Mod {
    private final boolean haveBlockRegistry;
    private final boolean haveThickPanes;
    private final boolean haveGlassPaneRenderer;

    public static final MethodRef newBlockIconFromPosition = new MethodRef(MCPatcherUtils.CTM_UTILS_CLASS, "getBlockIcon", "(LIcon;LRenderBlocks;LBlock;LIBlockAccess;IIII)LIcon;");
    public static final MethodRef newBlockIconFromSideAndMetadata = new MethodRef(MCPatcherUtils.CTM_UTILS_CLASS, "getBlockIcon", "(LIcon;LRenderBlocks;LBlock;II)LIcon;");
    public static final MethodRef newBlockIconFromSide = new MethodRef(MCPatcherUtils.CTM_UTILS_CLASS, "getBlockIcon", "(LIcon;LRenderBlocks;LBlock;I)LIcon;");

    private static final MethodRef getGrassSideTexture = new MethodRef("BlockGrass", "getSideTexture", "()LIcon;");
    private static final MethodRef getSecondaryIcon = new MethodRef("RenderBlocks", "getSecondaryIcon", "(LBlock;LIBlockAccess;LPosition;LDirection;LRenderBlocks;)LIcon;");

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
        haveThickPanes = getMinecraftVersion().compareTo("13w41a") >= 0;
        haveGlassPaneRenderer = getMinecraftVersion().compareTo("14w10a") < 0;

        addClassMod(new IBlockAccessMod(this));
        addClassMod(new TessellatorMod(this));
        addClassMod(new IconMod(this));
        addClassMod(new ResourceLocationMod(this));
        PositionMod.setup(this);
        addClassMod(new BlockMod());
        addClassMod(new RenderBlocksMod());
        addClassMod(new RenderBlocksSubclassMod());
        if (RenderBlocksMod.haveSubclasses()) {
            RenderBlockManagerMod renderBlockManagerMod = new RenderBlockManagerMod(this);
            if (haveGlassPaneRenderer) {
                renderBlockManagerMod
                    .mapRenderType(18, "RenderBlockIronBars")
                    .mapRenderType(35, "RenderBlockAnvil")
                    .mapRenderType(40, "RenderBlockDoublePlant")
                    .mapRenderType(41, "RenderBlockGlassPane");
                addClassMod(new RenderBlockPaneMod("RenderBlockIronBars"));
                addClassMod(new RenderBlockPaneMod("RenderBlockGlassPane"));
            } else {
                addClassMod(new BlockModelFaceMod(this)
                    .mapDirectionMethods()
                    .mapIntBufferMethods()
                );
            }
            addClassMod(renderBlockManagerMod);
            if (RenderBlockCustomMod.haveCustomModels()) {
                addClassMod(new RenderBlockCustomMod());
            }
            addClassMod(new RenderBlockDoublePlantMod());
        } else {
            addClassMod(new BlockGrassMod());
        }

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
        addClassFile(MCPatcherUtils.BLOCK_ORIENTATION_CLASS);
        if (haveGlassPaneRenderer) {
            addClassFile(MCPatcherUtils.GLASS_PANE_RENDERER_CLASS);
        } else {
            addClassFile(MCPatcherUtils.CTM_UTILS_CLASS + "$Ext18");
        }

        BaseTexturePackMod.earlyInitialize(2, MCPatcherUtils.CTM_UTILS_CLASS, "reset");
    }

    private class ConfigPanel extends ModConfigPanel {
        private JPanel panel;
        private JCheckBox glassPaneCheckBox;
        private JCheckBox grassCheckBox;
        private JCheckBox standardCheckBox;
        private JCheckBox nonStandardCheckBox;
        private JCheckBox debugCheckBox;

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

            Config.remove(MCPatcherUtils.CONNECTED_TEXTURES, "splitTextures");
        }

        @Override
        public void save() {
        }
    }

    private class BlockMod extends com.prupe.mcpatcher.basemod.BlockMod {
        BlockMod() {
            super(ConnectedTextures.this);

            final MethodRef constructor = new MethodRef(getDeobfClass(), "<init>", "(" + (haveBlockRegistry ? "" : "I") + "LMaterial;)V");

            mapBlockIconMethods();

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
                .addXref(1, IBlockAccessMod.getBlockMetadata)
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

    private class BlockGrassMod extends ClassMod {
        BlockGrassMod() {
            addClassSignature(new ConstSignature("_side"));
            addClassSignature(new ConstSignature("_top"));
            addClassSignature(new ConstSignature("_side_snowed"));
            addClassSignature(new ConstSignature("_side_overlay"));

            addMemberMapper(new MethodMapper(getGrassSideTexture)
                .accessFlag(AccessFlag.PUBLIC, true)
                .accessFlag(AccessFlag.STATIC, true)
            );
        }
    }

    private class RenderBlocksMod extends com.prupe.mcpatcher.basemod.RenderBlocksMod {
        RenderBlocksMod() {
            super(ConnectedTextures.this);

            mapHasOverrideTexture();

            if (!haveSubclasses()) {
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
                    .addXref(1, BlockMod.getRenderType)
                );
            }

            addMemberMapper(new FieldMapper(blockAccess));

            if (!haveSubclasses()) {
                setupGlassPanes();
                if (getMinecraftVersion().compareTo("1.7") >= 0) {
                    mapRenderType(40, renderBlockDoublePlant);
                    addDoublePlantPatches(this, renderBlockDoublePlant);
                }
            }
            setupTileOverrides();
            if (BlockMod.getSecondaryBlockIcon == null) {
                setupSecondaryTexture17();
            } else {
                setupSecondaryTexture18();
            }
        }

        private void setupTileOverrides() {
            addMemberMapper(new MethodMapper(getBlockIconFromPosition));
            addMemberMapper(new MethodMapper(getBlockIconFromSideAndMetadata));
            addMemberMapper(new MethodMapper(getBlockIconFromSide));

            addPatch(new OverrideIconPatch(getBlockIconFromPosition, newBlockIconFromPosition, "position, side") {
                @Override
                byte[] getCTMUtilsArgs() {
                    return buildCode(
                        // blockAccess, i, j, k, face
                        ALOAD_2,
                        PositionMod.unpackArguments(this, 3),
                        DirectionMod.unpackArgumentsSafe(this, 3 + PositionMod.getDescriptorLength())
                    );
                }
            });

            addPatch(new OverrideIconPatch(getBlockIconFromSideAndMetadata, newBlockIconFromSideAndMetadata, "side, metadata") {
                @Override
                byte[] getCTMUtilsArgs() {
                    return buildCode(
                        // face, metadata
                        DirectionMod.unpackArgumentsSafe(this, 2),
                        ILOAD_3
                    );
                }
            });

            addPatch(new OverrideIconPatch(getBlockIconFromSide, newBlockIconFromSide, "side") {
                @Override
                byte[] getCTMUtilsArgs() {
                    return buildCode(
                        // face
                        DirectionMod.unpackArgumentsSafe(this, 2)
                    );
                }
            });
        }

        abstract private class OverrideIconPatch extends BytecodePatch {
            private final MethodRef to;
            private final String desc;

            {
                skipMethod(renderBlockGenericPane, renderBlockGlassPane17);
            }

            OverrideIconPatch(MethodRef from, MethodRef to, String desc) {
                this.to = to;
                this.desc = desc;
                setInsertBefore(true);
                targetMethod(from);
            }

            @Override
            public String getDescription() {
                return "override texture (by " + desc + ")";
            }

            @Override
            public String getMatchExpression() {
                return buildExpression(
                    // return icon;
                    ARETURN
                );
            }

            @Override
            public byte[] getReplacementBytes() {
                return buildCode(
                    // return CTMUtils.getBlockIcon(icon, this, block, ...);
                    ALOAD_0,
                    ALOAD_1,
                    getCTMUtilsArgs(),
                    reference(INVOKESTATIC, to)
                );
            }

            abstract byte[] getCTMUtilsArgs();
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

        private void setupGlassPanes() {
            mapRenderTypeMethod(18, renderBlockGenericPane);
            if (haveThickPanes) {
                mapRenderTypeMethod(41, renderBlockGlassPane17);
            }

            addGlassPanePatches(this, renderBlockGenericPane, renderBlockGlassPane17);
        }

        private void setupSecondaryTexture17() {
            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override texture (grass side texture)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // BlockGrass.getSideTexture()
                        reference(INVOKESTATIC, getGrassSideTexture)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // CTMUtils.getBlockIcon(..., this, block, this.blockAccess, i, j, k, face)
                        ALOAD_0,
                        ALOAD_1,
                        ALOAD_0,
                        reference(GETFIELD, blockAccess),
                        PositionMod.unpackArguments(this, 2),
                        push((getMethodMatchCount() % 4) + 2),
                        reference(INVOKESTATIC, newBlockIconFromPosition)
                    );
                }
            }
                .setInsertAfter(true)
            );
        }

        private void setupSecondaryTexture18() {
            addPatch(new AddMethodPatch(getSecondaryIcon, AccessFlag.PROTECTED | AccessFlag.STATIC) {
                @Override
                public byte[] generateMethod() {
                    int icon = 5;
                    return buildCode(
                        // icon = block.getSecondaryIcon(blockAccess, position, direction)
                        ALOAD_0,
                        ALOAD_1,
                        ALOAD_2,
                        ALOAD_3,
                        reference(INVOKEVIRTUAL, BlockMod.getSecondaryBlockIcon),
                        ASTORE, icon,

                        // if (icon != null) {
                        ALOAD, icon,
                        IFNULL, branch("A"),

                        // icon = CTMUtils.getBlockIcon(icon, renderBlocks, block, renderBlocks.blockAccess, i, j, k, face);
                        ALOAD, icon,
                        ALOAD, 4,
                        ALOAD_0,
                        ALOAD, 4,
                        reference(GETFIELD, RenderBlocksMod.blockAccess),
                        PositionMod.unpackArguments(this, 2),
                        DirectionMod.unpackArgumentsSafe(this, 3),
                        reference(INVOKESTATIC, newBlockIconFromPosition),
                        ASTORE, icon,

                        // }
                        label("A"),

                        // return icon;
                        ALOAD, icon,
                        ARETURN
                    );
                }
            });
        }
    }

    private class RenderBlocksSubclassMod extends ClassMod {
        RenderBlocksSubclassMod() {
            setMultipleMatchesAllowed(true);
            addClassSignature(new AncestorClassSignature("RenderBlocks"));

            if (BlockMod.getSecondaryBlockIcon != null) {
                setupSecondaryTexture();
            }

            addPatch(new GetBlockIconPatch(RenderBlocksMod.getBlockIconFromSideAndMetadata, "side, metadata") {
                @Override
                protected String getMoreArgs() {
                    return buildExpression(
                        // this.getBlockIconFromSideAndMetadata(block, face, metadata)
                        any(0, 20)
                    );
                }
            });

            addPatch(new GetBlockIconPatch(RenderBlocksMod.getBlockIconFromSide, "side"));
        }

        private void setupSecondaryTexture() {
            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override secondary block texture";
                }

                @Override
                public boolean filterMethod() {
                    return (getMethodInfo().getAccessFlags() & AccessFlag.STATIC) == 0;
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // block.getSecondaryBlockIcon(...)
                        reference(INVOKEVIRTUAL, BlockMod.getSecondaryBlockIcon)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // RenderBlocks.getSecondaryIcon(..., this)
                        ALOAD_0,
                        reference(INVOKESTATIC, getSecondaryIcon)
                    );
                }
            });
        }

        private class GetBlockIconPatch extends BytecodePatch {
            private final MethodRef from;
            private final String description;
            private Pattern matchPrefix;
            private Set<String> excludedClasses;

            GetBlockIconPatch(MethodRef from, String description) {
                this.from = from;
                this.description = description;
            }

            private FieldRef remap(FieldRef field) {
                return new FieldRef(getClassFile().getName(), field.getName(), field.getType());
            }

            private MethodRef remap(MethodRef method) {
                return new MethodRef(getClassFile().getName(), method.getName(), method.getType());
            }

            @Override
            public boolean filterMethod() {
                if (excludedClasses == null) {
                    excludedClasses = new HashSet<String>();
                    excludedClasses.add(getClassMap().map("RenderBlockIronBars"));
                    excludedClasses.add(getClassMap().map("RenderBlockGlassPane"));
                    excludedClasses.add(getClassMap().map("RenderBlockAnvil"));
                }
                if (excludedClasses.contains(getClassFile().getName())) {
                    return false;
                }
                if (matchPrefix == null) {
                    matchPrefix = Pattern.compile("^\\(L([a-z]+);" + getClassMap().mapTypeString(Pattern.quote(PositionMod.getDescriptor())) + ".*");
                }
                Matcher matcher = matchPrefix.matcher(getMethodInfo().getDescriptor());
                return matcher.matches() && isInstanceOf(matcher.group(1), "Block");
            }

            @Override
            public String getDescription() {
                return "use block coordinates where possible (" + description + ")";
            }

            @Override
            public String getMatchExpression() {
                return buildExpression(
                    // this.getBlockIconFromSide...(block, face, ...)
                    ALOAD_0,
                    ALOAD_1,
                    capture(DirectionMod.haveDirectionClass() ?
                        or(
                            // Direction.get(...)
                            build(any(0, 5), anyReference(INVOKESTATIC)),
                            // Direction.DOWN, etc.
                            build(anyReference(GETSTATIC)),
                            // variable direction
                            anyALOAD
                        ) :
                        or(
                            // fixed direction
                            build(push(0)),
                            build(push(1)),
                            build(push(2)),
                            build(push(3)),
                            build(push(4)),
                            build(push(5)),
                            // variable direction
                            build(anyILOAD, optional(build(any(1, 4), IADD)))
                        )),
                    getMoreArgs(),
                    reference(INVOKEVIRTUAL, remap(from))
                );
            }

            @Override
            public byte[] getReplacementBytes() {
                return buildCode(
                    // (this.blockAccess == null ?
                    ALOAD_0,
                    reference(GETFIELD, remap(RenderBlocksMod.blockAccess)),
                    IFNONNULL, branch("A"),

                    // ...
                    getMatch(),
                    GOTO, branch("B"),

                    // :
                    label("A"),

                    // this.getBlockIconFromPosition(block, this.blockAccess, i, j, k, face)
                    ALOAD_0,
                    ALOAD_1,
                    ALOAD_0,
                    reference(GETFIELD, remap(RenderBlocksMod.blockAccess)),
                    PositionMod.passArguments(2),
                    getCaptureGroup(1),
                    reference(INVOKEVIRTUAL, RenderBlocksMod.getBlockIconFromPosition),

                    // )
                    label("B")
                );
            }

            protected String getMoreArgs() {
                return "";
            }
        }
    }

    private void addGlassPanePatches(final com.prupe.mcpatcher.ClassMod classMod, final MethodRef... methods) {
        final MethodRef newRenderPaneThin = new MethodRef(MCPatcherUtils.GLASS_PANE_RENDERER_CLASS, "renderThin", "(LRenderBlocks;LBlock;LIcon;IIIZZZZ)V");
        final MethodRef newRenderPaneThick = new MethodRef(MCPatcherUtils.GLASS_PANE_RENDERER_CLASS, "renderThick", "(LRenderBlocks;LBlock;LIcon;IIIZZZZ)V");
        final FieldRef skipPaneRendering = new FieldRef(MCPatcherUtils.GLASS_PANE_RENDERER_CLASS, "skipPaneRendering", "Z");
        final FieldRef skipTopEdgeRendering = new FieldRef(MCPatcherUtils.GLASS_PANE_RENDERER_CLASS, "skipTopEdgeRendering", "Z");
        final FieldRef skipBottomEdgeRendering = new FieldRef(MCPatcherUtils.GLASS_PANE_RENDERER_CLASS, "skipBottomEdgeRendering", "Z");
        final InterfaceMethodRef getMinU = new InterfaceMethodRef("Icon", "getMinU", "()F");

        classMod.addPatch(new BytecodePatch(classMod) {
            private int iconRegister;
            private int connectNorthRegister;

            {
                setInsertBefore(true);
                if (methods.length > 0) {
                    targetMethod(methods);
                }

                addPreMatchSignature(new BytecodeSignature(classMod) {
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
                        Logger.log(Logger.LOG_BYTECODE, "icon register %d", iconRegister);
                        return true;
                    }
                });

                addPreMatchSignature(new BytecodeSignature(classMod) {
                    @Override
                    public String getMatchExpression() {
                        return buildExpression(
                            getSubExpression(false, DSUB),
                            getSubExpression(false, DADD),
                            getSubExpression(false, DSUB),
                            getSubExpression(true, DADD)
                        );
                    }

                    private String getSubExpression(boolean capture, int opcode) {
                        return build(
                            // d = d1 + 0.5 +/- 0.0625;
                            or(
                                anyDLOAD,
                                build(anyILOAD, I2D)
                            ),
                            push(0.5),
                            DADD,
                            push(0.0625),
                            opcode,
                            capture ? capture(anyDSTORE) : anyDSTORE
                        );
                    }

                    @Override
                    public boolean afterMatch() {
                        connectNorthRegister = extractRegisterNum(getCaptureGroup(1)) + 2;
                        Logger.log(Logger.LOG_BYTECODE, "glass pane side connect registers %d %d %d %d",
                            connectNorthRegister, connectNorthRegister + 1, connectNorthRegister + 2, connectNorthRegister + 3
                        );
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
                    or(
                        build(
                            // 1.6 panes and 1.7+ thin panes
                            push(0.01),
                            anyDSTORE,
                            push(0.005),
                            anyDSTORE
                        ),
                        build(
                            // 1.7+ thick panes
                            push(0.001),
                            anyDSTORE,
                            push(0.999),
                            anyDSTORE,
                            push(0.001),
                            anyDSTORE
                        )
                    )
                );
            }

            @Override
            public byte[] getReplacementBytes() {
                int index = extractConstPoolIndex(getMatch());
                double width = getMethodInfo().getConstPool().getDoubleInfo(index);
                return buildCode(
                    // GlassPaneRenderer.render(renderBlocks, blockPane, i, j, k, connectNorth, ...);
                    ALOAD_0,
                    ALOAD_1,
                    ALOAD, iconRegister,
                    PositionMod.unpackArguments(this, 2),
                    ILOAD, connectNorthRegister,
                    ILOAD, connectNorthRegister + 1,
                    ILOAD, connectNorthRegister + 2,
                    ILOAD, connectNorthRegister + 3,
                    reference(INVOKESTATIC, width == 0.001 ? newRenderPaneThick : newRenderPaneThin)
                );
            }
        });

        classMod.addPatch(new RenderPanePatch(classMod, methods) {
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
                    reference(INVOKEVIRTUAL, TessellatorMod.addVertexWithUV)
                ), 4, 8));
            }

            @Override
            public byte[] getReplacementBytes() {
                return buildCode(
                    // if (!GlassPaneRenderer.skipPaneRendering) {
                    reference(GETSTATIC, skipPaneRendering),
                    IFNE, branch("A"),

                    // ...
                    getMatch(),

                    // }
                    label("A")
                );
            }
        });

        classMod.addPatch(new RenderPanePatch(classMod, methods) {
            protected final int[] ijkRegisters = new int[3];

            {
                if (PositionMod.havePositionClass()) {
                    addPreMatchSignature(new BytecodeSignature(classMod) {
                        @Override
                        public String getMatchExpression() {
                            String s = buildExpression(
                                // varXX = (double) position.getI/J/K();
                                ALOAD_2,
                                anyReference(INVOKEVIRTUAL),
                                I2D,
                                capture(anyDSTORE)
                            );
                            return s + s + s;
                        }

                        @Override
                        public boolean afterMatch() {
                            ijkRegisters[0] = extractRegisterNum(getCaptureGroup(1));
                            ijkRegisters[1] = extractRegisterNum(getCaptureGroup(2));
                            ijkRegisters[2] = extractRegisterNum(getCaptureGroup(3));
                            Logger.log(Logger.LOG_CONST, "glass pane i j k registers %d %d %d", ijkRegisters[0], ijkRegisters[1], ijkRegisters[2]);
                            return true;
                        }
                    });
                }
            }

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
                    first ? capture(getJExpression()) : backReference(1),
                    DLOAD, any(),
                    DLOAD, subset(sideUVRegisters, true),
                    DLOAD, subset(sideUVRegisters, true),
                    reference(INVOKEVIRTUAL, TessellatorMod.addVertexWithUV)
                );
            }

            private String getJExpression() {
                if (PositionMod.havePositionClass()) {
                    return build(
                        DLOAD, ijkRegisters[1],
                        or(
                            // (double) (j + 1) + 0.005
                            build(
                                push(1.0),
                                DADD,
                                anyLDC,
                                DADD
                            ),
                            // (double) j - 0.005
                            build(
                                anyLDC,
                                DSUB
                            ),
                            // (double) j + <0.001|0.999>
                            build(
                                anyLDC,
                                DADD
                            )
                        )
                    );
                } else {
                    return build(
                        ILOAD_3,
                        or(
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
                        )
                    );
                }
            }

            @Override
            public byte[] getReplacementBytes() {
                boolean top;
                byte[] group = getCaptureGroup(1);
                int length = group.length;
                if (group[1] == ICONST_1 || group[1] == DCONST_1) {
                    // if j-expression contains +1 or +1.0, it is always the top edge
                    top = true;
                } else if (group[length - 1] == DSUB) {
                    // if j-expression is a subtraction, it is always the bottom edge
                    top = false;
                } else if (length >= 4 && group[length - 4] == LDC2_W) {
                    // otherwise determine based on the value of the double constant
                    byte[] tmp = new byte[3];
                    tmp[0] = group[length - 4];
                    tmp[1] = group[length - 3];
                    tmp[2] = group[length - 2];
                    int index = extractConstPoolIndex(tmp);
                    double value = getMethodInfo().getConstPool().getDoubleInfo(index);
                    top = value > 0.5;
                } else {
                    Logger.log(Logger.LOG_CONST, "WARNING: no j-expression match in %s(%s)@%d",
                        getMethodInfo().getName(), getMethodInfo().getDescriptor(), matcher.getStart()
                    );
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

        RenderPanePatch(com.prupe.mcpatcher.ClassMod classMod, MethodRef... methods) {
            super(classMod);

            if (methods.length > 0) {
                targetMethod(methods);
            }

            addPreMatchSignature(new BytecodeSignature(classMod) {
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

                        // older: var34 = (double) i;
                        // 14w02a+: var34 = (double) position.getI();
                        PositionMod.havePositionClass() ?
                            build(
                                ALOAD_2,
                                anyReference(INVOKEVIRTUAL)
                            ) :
                            build(
                                ILOAD_2
                            ),
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
                    Logger.log(Logger.LOG_CONST, "glass pane side texture uv registers (%s)", sb.toString());
                    return true;
                }
            });
        }
    }

    private class RenderBlockPaneMod extends ClassMod {
        private final String className;

        RenderBlockPaneMod(String className) {
            this.className = className;

            addPrerequisiteClass("RenderBlockManager");
            setParentClass("RenderBlocks");

            addGlassPanePatches(this);
        }

        @Override
        public String getDeobfClass() {
            return className;
        }
    }

    private class RenderBlockCustomMod extends com.prupe.mcpatcher.basemod.RenderBlockCustomMod {
        RenderBlockCustomMod() {
            super(ConnectedTextures.this);

            final InterfaceMethodRef listIterator = new InterfaceMethodRef("java/util/Iterator", "next", "()Ljava/lang/Object;");
            final ClassRef blockModelFaceClass = new ClassRef("BlockModelFace");
            final MethodRef setBlockFace = new MethodRef(MCPatcherUtils.CTM_UTILS_CLASS + "$Ext18", "setBlockFace", "(LIBlockAccess;LBlock;LPosition;LDirection;LDirection;LDirection;)V");

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override texture (custom models)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // face = (BlockModelFace) iterator.next();
                        anyALOAD,
                        reference(INVOKEINTERFACE, listIterator),
                        reference(CHECKCAST, blockModelFaceClass),
                        capture(anyASTORE)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    int blockFace = extractRegisterNum(getCaptureGroup(1));
                    return buildCode(
                        // CTMUtils.Ext18.setBlockFace(this.blockAccess, block, position, direction, face.getTextureFacing(), face.getBlockFacing());
                        ALOAD_0,
                        reference(GETFIELD, RenderBlocksMod.blockAccess),
                        ALOAD_1,
                        ALOAD_2,
                        registerLoadStore(ALOAD, getDirectionParam()),
                        registerLoadStore(ALOAD, blockFace),
                        reference(INVOKEVIRTUAL, BlockModelFaceMod.getTextureFacing),
                        BlockModelFaceMod.getBlockFacing == null ?
                            ACONST_NULL :
                            buildCode(
                                registerLoadStore(ALOAD, blockFace),
                                reference(INVOKEVIRTUAL, BlockModelFaceMod.getBlockFacing)
                            ),
                        reference(INVOKESTATIC, setBlockFace)
                    );
                }
            }
                .setInsertAfter(true)
                .targetMethod(renderFaceAO, renderFaceNonAO)
            );
        }
    }

    private class RenderBlockDoublePlantMod extends ClassMod {
        RenderBlockDoublePlantMod() {
            setParentClass("RenderBlocks");
            addPrerequisiteClass("RenderBlocks");

            final MethodRef renderDoublePlant = new MethodRef(getDeobfClass(), "renderDoublePlant", "(LBlockDoublePlant;LPosition;)Z");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(3129871)
                    );
                }
            }.setMethod(renderDoublePlant));

            addDoublePlantPatches(this, renderDoublePlant);
        }
    }

    private void addDoublePlantPatches(com.prupe.mcpatcher.ClassMod classMod, MethodRef method) {
        classMod.addPatch(new BytecodePatch(classMod) {
            @Override
            public String getDescription() {
                return "use block coordinates where possible (double plants)";
            }

            @Override
            public String getMatchExpression() {
                return buildExpression(
                    // icon = blockDoublePlant.getIcon(isTop, plantType);
                    capture(build(
                        ALOAD_1,
                        anyILOAD,
                        anyILOAD,
                        anyReference(INVOKEVIRTUAL),
                        ASTORE, capture(any())
                    )),

                    // this.drawCrossedSquares(icon, x, y, z, 1.0f);
                    capture(build(
                        ALOAD_0,
                        ALOAD, backReference(2),
                        anyDLOAD,
                        anyDLOAD,
                        anyDLOAD,
                        push(1.0f),
                        anyReference(INVOKEVIRTUAL)
                    ))
                );
            }

            @Override
            public byte[] getReplacementBytes() {
                return buildCode(
                    // ...
                    getCaptureGroup(1),

                    // icon = CTMUtils.getBlockIcon(icon, this, blockDoublePlant, this.blockAccess, i, j, k, -1)
                    ALOAD, getCaptureGroup(2),
                    ALOAD_0,
                    ALOAD_1,
                    ALOAD_0,
                    reference(GETFIELD, RenderBlocksMod.blockAccess),
                    PositionMod.unpackArguments(this, 2),
                    push(-1),
                    reference(INVOKESTATIC, newBlockIconFromPosition),
                    ASTORE, getCaptureGroup(2),

                    // ...
                    getCaptureGroup(3)
                );
            }
        }.targetMethod(method));
    }
}
