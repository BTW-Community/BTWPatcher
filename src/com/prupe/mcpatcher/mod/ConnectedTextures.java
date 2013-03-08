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
        description = "Connects adjacent blocks of the same type.";
        version = "2.1";

        addDependency(BaseTexturePackMod.NAME);

        configPanel = new ConfigPanel();

        addClassMod(new MinecraftMod());
        addClassMod(new RenderEngineMod());
        addClassMod(new BaseMod.IBlockAccessMod());
        addClassMod(new BlockMod());
        addClassMod(new TessellatorMod());
        addClassMod(new RenderBlocksMod());
        addClassMod(new WorldRendererMod());
        addClassMod(new BaseMod.IconMod());
        addClassMod(new IconRegisterMod());
        addClassMod(new TextureMapMod());
        addClassMod(new TextureMod());
        addClassMod(new TextureManagerMod());
        addClassMod(new StitcherMod());
        addClassMod(new StitchHolderMod());

        addClassFile(MCPatcherUtils.CTM_UTILS_CLASS);
        addClassFile(MCPatcherUtils.CTM_UTILS_CLASS + "$1");
        addClassFile(MCPatcherUtils.CTM_UTILS_CLASS + "$1$1");
        addClassFile(MCPatcherUtils.CTM_UTILS_CLASS + "$2");
        addClassFile(MCPatcherUtils.CTM_UTILS_CLASS + "$3");
        addClassFile(MCPatcherUtils.CTM_UTILS_CLASS + "$TileOverrideIterator");
        addClassFile(MCPatcherUtils.TESSELLATOR_UTILS_CLASS);
        addClassFile(MCPatcherUtils.TILE_OVERRIDE_INTERFACE);
        addClassFile(MCPatcherUtils.TILE_OVERRIDE_CLASS);
        addClassFile(MCPatcherUtils.TILE_OVERRIDE_IMPL_CLASS);
        addClassFile(MCPatcherUtils.TILE_OVERRIDE_IMPL_CLASS + "$CTM");
        addClassFile(MCPatcherUtils.TILE_OVERRIDE_IMPL_CLASS + "$Random1");
        addClassFile(MCPatcherUtils.TILE_OVERRIDE_IMPL_CLASS + "$Fixed");
        addClassFile(MCPatcherUtils.TILE_OVERRIDE_IMPL_CLASS + "$Horizontal");
        addClassFile(MCPatcherUtils.TILE_OVERRIDE_IMPL_CLASS + "$Vertical");
        addClassFile(MCPatcherUtils.TILE_OVERRIDE_IMPL_CLASS + "$Top");
        addClassFile(MCPatcherUtils.TILE_OVERRIDE_IMPL_CLASS + "$Repeat");
        addClassFile(MCPatcherUtils.TILE_OVERRIDE_IMPL_CLASS + "$BetterGrass");
        addClassFile(MCPatcherUtils.GLASS_PANE_RENDERER_CLASS);
        addClassFile(MCPatcherUtils.RENDER_PASS_API_CLASS);

        BaseTexturePackMod.earlyInitialize(MCPatcherUtils.CTM_UTILS_CLASS, "reset");
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

    private class MinecraftMod extends BaseMod.MinecraftMod {
        MinecraftMod() {
            final FieldRef renderEngine = new FieldRef(getDeobfClass(), "renderEngine", "LRenderEngine;");

            addMemberMapper(new FieldMapper(renderEngine));
        }
    }

    private class RenderEngineMod extends BaseMod.RenderEngineMod {
        RenderEngineMod() {
            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "update ctm animations";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        reference(GETFIELD, terrain),
                        reference(INVOKEVIRTUAL, new MethodRef("TextureMap", "updateAnimations", "()V"))
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.CTM_UTILS_CLASS, "updateAnimations", "()V"))
                    );
                }
            }
                .setInsertBefore(true)
                .targetMethod(updateDynamicTextures)
            );
        }
    }

    private class BlockMod extends BaseMod.BlockMod {
        BlockMod() {
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
                        begin(),
                        ALOAD_0,
                        anyReference(GETFIELD),
                        ARETURN,
                        end()
                    );
                }
            }.setMethod(getShortName));

            addMemberMapper(new FieldMapper(blockMaterial).accessFlag(AccessFlag.PUBLIC, true));
        }
    }

    private class TessellatorMod extends BaseMod.TessellatorMod {
        TessellatorMod() {
            final MethodRef constructor = new MethodRef(getDeobfClass(), "<init>", "(I)V");
            final MethodRef constructor0 = new MethodRef(getDeobfClass(), "<init>", "()V");
            final MethodRef reset = new MethodRef(getDeobfClass(), "reset", "()V");
            final FieldRef isDrawing = new FieldRef(getDeobfClass(), "isDrawing", "Z");
            final FieldRef drawMode = new FieldRef(getDeobfClass(), "drawMode", "I");
            final FieldRef textureMap = new FieldRef(getDeobfClass(), "textureMap", "LTextureMap;");
            final FieldRef bufferSize = new FieldRef(getDeobfClass(), "bufferSize", "I");
            final FieldRef addedVertices = new FieldRef(getDeobfClass(), "addedVertices", "I");
            final FieldRef vertexCount = new FieldRef(getDeobfClass(), "vertexCount", "I");
            final FieldRef rawBufferIndex = new FieldRef(getDeobfClass(), "rawBufferIndex", "I");
            final FieldRef children = new FieldRef(getDeobfClass(), "children", "Ljava/util/Map;");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        captureReference(GETFIELD),
                        push(4),
                        IREM,

                        any(0, 1000),

                        ALOAD_0,
                        DUP,
                        captureReference(GETFIELD),
                        ICONST_1,
                        IADD,
                        anyReference(PUTFIELD),

                        ALOAD_0,
                        DUP,
                        captureReference(GETFIELD),
                        push(8),
                        IADD,
                        anyReference(PUTFIELD)
                    );
                }
            }
                .setMethod(addVertex)
                .addXref(1, addedVertices)
                .addXref(2, vertexCount)
                .addXref(3, rawBufferIndex)
            );

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        begin(),
                        ALOAD_0,
                        captureReference(GETFIELD),

                        any(0, 50),

                        push("Already tesselating!"),
                        any(0, 100),

                        ALOAD_0,
                        captureReference(INVOKESPECIAL),

                        ALOAD_0,
                        ILOAD_1,
                        captureReference(PUTFIELD)
                    );
                }
            }
                .setMethod(startDrawing)
                .addXref(1, isDrawing)
                .addXref(2, reset)
                .addXref(3, drawMode)
            );

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        begin(),
                        ALOAD_0,
                        push(7),
                        captureReference(INVOKEVIRTUAL),
                        RETURN,
                        end()
                    );
                }
            }
                .setMethod(startDrawingQuads)
                .addXref(1, startDrawing)
            );

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        ILOAD_1,
                        captureReference(PUTFIELD)
                    );
                }
            }
                .matchConstructorOnly(true)
                .addXref(1, bufferSize)
            );

            addMemberMapper(new FieldMapper(instance).accessFlag(AccessFlag.STATIC, true));

            for (JavaRef ref : new JavaRef[]{constructor, startDrawing, isDrawing, drawMode, draw, reset, bufferSize,
                addedVertices, vertexCount, rawBufferIndex}) {
                addPatch(new MakeMemberPublicPatch(ref));
            }

            addPatch(new AddFieldPatch(textureMap));
            addPatch(new AddFieldPatch(children));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "initialize new fields";
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
                        // children = new WeakHashMap();
                        ALOAD_0,
                        reference(NEW, new ClassRef("java/util/WeakHashMap")),
                        DUP,
                        reference(INVOKESPECIAL, new MethodRef("java/util/WeakHashMap", "<init>", "()V")),
                        reference(PUTFIELD, children),

                        // TessellatorUtils.haveBufferSize = true / false;
                        push(getMethodInfo().getDescriptor().contains("()") ? 0 : 1),
                        reference(PUTSTATIC, new FieldRef(MCPatcherUtils.TESSELLATOR_UTILS_CLASS, "haveBufferSize", "Z"))
                    );
                }
            }
                .setInsertBefore(true)
                .matchConstructorOnly(true)
            );

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "bind texture before drawing";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        or(
                            build(ALOAD_0, anyReference(GETFIELD)),
                            anyReference(GETSTATIC)
                        ),
                        reference(INVOKEVIRTUAL, new MethodRef("java/nio/IntBuffer", "clear", "()Ljava/nio/Buffer;")),
                        POP
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // if (textureMap != null) {
                        ALOAD_0,
                        reference(GETFIELD, textureMap),
                        IFNULL, branch("A"),

                        // GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureMap.getTexture().getGLTexture());
                        push(3553), // GL11.GL_TEXTURE_2D
                        ALOAD_0,
                        reference(GETFIELD, textureMap),
                        reference(INVOKEVIRTUAL, new MethodRef("TextureMap", "getTexture", "()LTexture;")),
                        reference(INVOKEVIRTUAL, new MethodRef("Texture", "getGLTexture", "()I")),
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.GL11_CLASS, "glBindTexture", "(II)V")),

                        // }
                        label("A")
                    );
                }
            }
                .setInsertBefore(true)
                .targetMethod(draw)
            );

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "reset children";
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
                        ALOAD_0,
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.TESSELLATOR_UTILS_CLASS, "resetChildren", "(LTessellator;)V"))
                    );
                }
            }
                .setInsertBefore(true)
                .targetMethod(reset)
            );

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "draw children";
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
                        push(0),
                        ALOAD_0,
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.TESSELLATOR_UTILS_CLASS, "drawChildren", "(ILTessellator;)I")),
                        ISTORE_1
                    );
                }
            }.targetMethod(draw));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "startDrawing children";
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
                        ALOAD_0,
                        ILOAD_1,
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.TESSELLATOR_UTILS_CLASS, "startDrawingChildren", "(LTessellator;I)V"))
                    );
                }
            }
                .setInsertBefore(true)
                .targetMethod(startDrawing)
            );
        }
    }

    private class RenderBlocksMod extends BaseMod.RenderBlocksMod {
        private final MethodRef[] faceMethods = new MethodRef[6];
        private final FieldRef overrideBlockTexture = new FieldRef(getDeobfClass(), "overrideBlockTexture", "LIcon;");
        private final FieldRef blockAccess = new FieldRef(getDeobfClass(), "blockAccess", "LIBlockAccess;");
        private final FieldRef instance = new FieldRef("Tessellator", "instance", "LTessellator;");
        private final MethodRef renderBlockByRenderType = new MethodRef(getDeobfClass(), "renderBlockByRenderType", "(LBlock;III)Z");
        private final MethodRef renderStandardBlock = new MethodRef(getDeobfClass(), "renderStandardBlock", "(LBlock;III)Z");
        private final MethodRef hasOverrideTexture = new MethodRef(getDeobfClass(), "hasOverrideTexture", "()Z");
        private final MethodRef drawCrossedSquares = new MethodRef(getDeobfClass(), "drawCrossedSquares", "(LBlock;IDDDF)V");
        private final MethodRef renderBlockPane = new MethodRef(getDeobfClass(), "renderBlockPane", "(LBlockPane;III)Z");
        private final MethodRef addVertexWithUV = new MethodRef("Tessellator", "addVertexWithUV", "(DDDDD)V");
        private final MethodRef renderBlockAsItem = new MethodRef(getDeobfClass(), "renderBlockAsItem", "(LBlock;IF)V");
        private final MethodRef getIconBySideAndMetadata = new MethodRef(getDeobfClass(), "getIconBySideAndMetadata", "(LBlock;II)LIcon;");
        private final MethodRef getIconBySide = new MethodRef(getDeobfClass(), "getIconBySide", "(LBlock;I)LIcon;");
        private final MethodRef getTile = new MethodRef(MCPatcherUtils.CTM_UTILS_CLASS, "getTile", "(LRenderBlocks;LBlock;IIIILIcon;LTessellator;)LIcon;");
        private final MethodRef getTileNoFace = new MethodRef(MCPatcherUtils.CTM_UTILS_CLASS, "getTile", "(LRenderBlocks;LBlock;IIILIcon;LTessellator;)LIcon;");
        private final MethodRef getTileBySideAndMetadata = new MethodRef(MCPatcherUtils.CTM_UTILS_CLASS, "getTile", "(LRenderBlocks;LBlock;IILTessellator;)LIcon;");
        private final MethodRef getTileBySide = new MethodRef(MCPatcherUtils.CTM_UTILS_CLASS, "getTile", "(LRenderBlocks;LBlock;ILTessellator;)LIcon;");
        private final MethodRef getTessellator = new MethodRef(MCPatcherUtils.CTM_UTILS_CLASS, "getTessellator", "(LIcon;)LTessellator;");

        RenderBlocksMod() {
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

            addMemberMapper(new FieldMapper(overrideBlockTexture));
            addMemberMapper(new FieldMapper(blockAccess));

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
                iconRegister = extractRegisterNum(getCaptureGroup(1));
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

                    // tessellator = CTMUtils.getTessellator(icon);
                    registerLoadStore(ALOAD, iconRegister),
                    reference(INVOKESTATIC, getTessellator),
                    registerLoadStore(ASTORE, tessellatorRegister)
                );
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
                        captureReference(INVOKEVIRTUAL)
                    );
                }
            }
                .setMethod(renderBlockByRenderType)
                .addXref(1, renderMethod)
            );
        }

        private void setupStandardBlocks() {
            setupBlockFace(0, "Bottom");
            setupBlockFace(1, "Top");
            setupBlockFace(2, "North");
            setupBlockFace(3, "South");
            setupBlockFace(4, "West");
            setupBlockFace(5, "East");

            addMemberMapper(new MethodMapper(faceMethods));
        }

        private void setupBlockFace(final int face, final String direction) {
            faceMethods[face] = new MethodRef(getDeobfClass(), "render" + direction + "Face", "(LBlock;DDDLIcon;)V");

            addPatch(new RenderBlocksPatch() {
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
            }.targetMethod(faceMethods[face]));
        }

        private void setupNonStandardBlocks() {
            addMemberMapper(new MethodMapper(drawCrossedSquares));

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
                    return buildExpression(
                        capture(anyALOAD),
                        reference(INVOKEINTERFACE, new InterfaceMethodRef("Icon", "getNormalizedX0", "()F")),
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
                                reference(INVOKEINTERFACE, new InterfaceMethodRef("Icon", "getNormalizedX0", "()F"))
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
                                reference(INVOKEINTERFACE, new InterfaceMethodRef("Icon", "interpolateX", "(D)F")),
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
                .targetMethod(renderBlockAsItem)
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
            }.targetMethod(renderBlockAsItem));
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

    private class IconRegisterMod extends ClassMod {
        IconRegisterMod() {
            final InterfaceMethodRef getIcon = new InterfaceMethodRef(getDeobfClass(), "getIcon", "(Ljava/lang/String;)LIcon;");

            addClassSignature(new InterfaceSignature(
                getIcon
            ).setInterfaceOnly(true));
        }
    }

    private class TextureMapMod extends ClassMod {
        TextureMapMod() {
            setInterfaces("IconRegister");

            final FieldRef dstPrefix = new FieldRef(getDeobfClass(), "dstPrefix", "Ljava/lang/String;");
            final FieldRef srcPrefix = new FieldRef(getDeobfClass(), "srcPrefix", "Ljava/lang/String;");
            final MethodRef refresh = new MethodRef(getDeobfClass(), "refresh", "()V");
            final MethodRef getTexture = new MethodRef(getDeobfClass(), "getTexture", "()LTexture;");
            final ClassRef sbClass = new ClassRef("java/lang/StringBuilder");
            final MethodRef strValueOf = new MethodRef("java/lang/String", "valueOf", "(Ljava/lang/Object;)Ljava/lang/String;");
            final MethodRef sbInit0 = new MethodRef("java/lang/StringBuilder", "<init>", "()V");
            final MethodRef sbInit1 = new MethodRef("java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V");
            final MethodRef sbAppend = new MethodRef("java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;");
            final MethodRef sbToString = new MethodRef("java/lang/StringBuilder", "toString", "()Ljava/lang/String;");

            addClassSignature(new ConstSignature("missingno"));
            addClassSignature(new ConstSignature(".png"));
            addClassSignature(new ConstSignature(".txt"));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        captureReference(INVOKESTATIC),
                        ALOAD_0,
                        anyReference(GETFIELD),
                        captureReference(INVOKEVIRTUAL),
                        anyASTORE
                    );
                }
            }
                .setMethod(refresh)
                .addXref(1, new MethodRef("TextureManager", "getInstance", "()LTextureManager;"))
                .addXref(2, new MethodRef("TextureManager", "newStitcher", "(Ljava/lang/String;)Lnet/minecraft/src/Stitcher;"))
            );

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        ALOAD_2,
                        captureReference(PUTFIELD)
                    );
                }
            }
                .matchConstructorOnly(true)
                .addXref(1, dstPrefix)
            );

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        ALOAD_3,
                        captureReference(PUTFIELD)
                    );
                }
            }
                .matchConstructorOnly(true)
                .addXref(1, srcPrefix)
            );

            addMemberMapper(new MethodMapper(getTexture));

            addPatch(new BytecodePatch() {
                private int stitcherRegister;
                private int nameRegister;

                {
                    addPreMatchSignature(new BytecodeSignature() {
                        @Override
                        public String getMatchExpression() {
                            return buildExpression(
                                capture(anyALOAD),
                                anyALOAD,
                                reference(INVOKEVIRTUAL, new MethodRef("Stitcher", "registerStitchHolder", "(LStitchHolder;)V")),

                                capture(anyALOAD),
                                anyALOAD,
                                push(1),
                                reference(ANEWARRAY, new ClassRef("Texture")),
                                DUP,
                                push(0),
                                anyALOAD,
                                AASTORE,
                                reference(INVOKESTATIC, new MethodRef("java/util/Arrays", "asList", "([Ljava/lang/Object;)Ljava/util/List;")),
                                reference(INVOKEINTERFACE, new InterfaceMethodRef("java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")),
                                POP
                            );
                        }

                        @Override
                        public boolean afterMatch() {
                            stitcherRegister = extractRegisterNum(getCaptureGroup(1));
                            nameRegister = extractRegisterNum(getCaptureGroup(2));
                            return true;
                        }
                    });
                }

                @Override
                public String getDescription() {
                    return "register ctm textures";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        anyALOAD,
                        reference(INVOKEVIRTUAL, new MethodRef("Stitcher", "stitch", "()V"))
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        ALOAD_0,
                        ALOAD, stitcherRegister,
                        ALOAD_0,
                        reference(GETFIELD, dstPrefix),
                        ALOAD, nameRegister,
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.CTM_UTILS_CLASS, "registerIcons", "(LTextureMap;LStitcher;Ljava/lang/String;Ljava/util/Map;)V"))
                    );
                }
            }
                .setInsertBefore(true)
                .targetMethod(refresh)
            );

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "register ctm animation txt files";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // this.srcPrefix + name + extension
                        reference(NEW, sbClass),
                        DUP,
                        or(
                            build( // vanilla mc
                                reference(INVOKESPECIAL, sbInit0),
                                ALOAD_0,
                                reference(GETFIELD, srcPrefix),
                                reference(INVOKEVIRTUAL, sbAppend)
                            ),
                            build( // mcp
                                ALOAD_0,
                                reference(GETFIELD, srcPrefix),
                                optional(build(reference(INVOKESTATIC, strValueOf))), // useless, but added by mcp
                                reference(INVOKESPECIAL, sbInit1)
                            )
                        ),
                        capture(any(1, 5)),
                        reference(INVOKEVIRTUAL, sbAppend),
                        capture(build(push(".txt"))),
                        reference(INVOKEVIRTUAL, sbAppend),
                        reference(INVOKEVIRTUAL, sbToString)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // CTMUtils.getOverridePath(this.srcPrefix, name, extension)
                        ALOAD_0,
                        reference(GETFIELD, srcPrefix),
                        getCaptureGroup(1),
                        getCaptureGroup(2),
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.CTM_UTILS_CLASS, "getOverridePath", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;"))
                    );
                }
            }
                .targetMethod(refresh)
            );
        }
    }

    private class TextureMod extends BaseMod.TextureMod {
        TextureMod() {
            final MethodRef unloadGLTexture = new MethodRef(getDeobfClass(), "unloadGLTexture", "()V");

            addPatch(new AddMethodPatch(unloadGLTexture) {
                @Override
                public byte[] generateMethod() {
                    return buildCode(
                        // if (this.glTexture >= 0) {
                        ALOAD_0,
                        reference(GETFIELD, glTexture),
                        IFLT, branch("A"),

                        // GL11.glDeleteTextures(this.glTexture);
                        ALOAD_0,
                        reference(GETFIELD, glTexture),
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.GL11_CLASS, "glDeleteTextures", "(I)V")),

                        // this.glTexture = -1;
                        ALOAD_0,
                        push(-1),
                        reference(PUTFIELD, glTexture),

                        // }
                        label("A"),
                        RETURN
                    );
                }
            });

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override texture name";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        ALOAD_1,
                        captureReference(PUTFIELD)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        ALOAD_0,
                        ALOAD_1,
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.CTM_UTILS_CLASS, "getOverrideTextureName", "(Ljava/lang/String;)Ljava/lang/String;")),
                        getCaptureGroup(1)
                    );
                }
            }.matchConstructorOnly(true));
        }
    }

    private class TextureManagerMod extends ClassMod {
        TextureManagerMod() {
            final MethodRef getInstance = new MethodRef(getDeobfClass(), "getInstance", "()LTextureManager;");
            final MethodRef setupTexture = new MethodRef(getDeobfClass(), "setupTexture", "(Ljava/lang/String;IIIIIIIZLjava/awt/image/BufferedImage;)LTexture;");
            final MethodRef createTexture = new MethodRef(getDeobfClass(), "createTexture", "(Ljava/lang/String;)Ljava/util/List;");

            addClassSignature(new ConstSignature("/"));
            addClassSignature(new ConstSignature(".txt"));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        captureReference(INVOKESTATIC),
                        ISTORE_2,
                        captureReference(NEW),
                        DUP,
                        ALOAD_1,
                        ILOAD_2,
                        ILOAD_2,
                        or(
                            build( // 13w02a
                                reference(INVOKESTATIC, new MethodRef("org/lwjgl/opengl/GLContext", "getCapabilities", "()Lorg/lwjgl/opengl/ContextCapabilities;")),
                                reference(GETFIELD, new FieldRef("org/lwjgl/opengl/ContextCapabilities", "GL_ARB_texture_non_power_of_two", "Z"))
                            ),
                            build(push(1)) // 13w02b
                        ),
                        anyReference(INVOKESPECIAL),
                        ARETURN
                    );
                }
            }
                .setMethod(new MethodRef(getDeobfClass(), "newStitcher", "(Ljava/lang/String;)LStitcher;"))
                .addXref(1, new MethodRef("Minecraft", "getMaxTextureSize", "()I"))
                .addXref(2, new ClassRef("Stitcher"))
            );

            addMemberMapper(new MethodMapper(getInstance)
                .accessFlag(AccessFlag.PUBLIC, true)
                .accessFlag(AccessFlag.STATIC, true)
            );
            addMemberMapper(new MethodMapper(setupTexture));
            addMemberMapper(new MethodMapper(createTexture));
        }
    }

    private class StitcherMod extends ClassMod {
        StitcherMod() {
            final MethodRef ceilPowerOf2 = new MethodRef(getDeobfClass(), "ceilPowerOf2", "(I)I");
            final MethodRef registerStitchHolder = new MethodRef(getDeobfClass(), "registerStitchHolder", "(LStitchHolder;)V");
            final MethodRef getTexture = new MethodRef(getDeobfClass(), "getTexture", "()LTexture;");
            final MethodRef stitch = new MethodRef(getDeobfClass(), "stitch", "()V");
            final MethodRef arraySort = new MethodRef("java/util/Arrays", "sort", "([Ljava/lang/Object;)V");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 1; i <= 16; i <<= 1) {
                        sb.append(buildExpression(
                            ILOAD_2,
                            ILOAD_2,
                            push(i),
                            ISHR,
                            IOR,
                            ISTORE_2
                        ));
                    }
                    return sb.toString();
                }
            }.setMethod(ceilPowerOf2));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        reference(INVOKESTATIC, arraySort)
                    );
                }
            }.setMethod(stitch));

            addMemberMapper(new MethodMapper(registerStitchHolder));
            addMemberMapper(new MethodMapper(getTexture));
        }
    }

    private class StitchHolderMod extends ClassMod {
        StitchHolderMod() {
            setInterfaces("java/lang/Comparable");

            final MethodRef min = new MethodRef("java/lang/Math", "min", "(II)I");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // (float)par1 / (float)Math.min(this.width, this.height)
                        ILOAD_1,
                        I2F,
                        ALOAD_0,
                        anyReference(GETFIELD),
                        ALOAD_0,
                        anyReference(GETFIELD),
                        reference(INVOKESTATIC, min),
                        I2F,
                        FDIV
                    );
                }
            }.setMethod(new MethodRef(getDeobfClass(), "rescale", "(I)V")));
        }
    }
}