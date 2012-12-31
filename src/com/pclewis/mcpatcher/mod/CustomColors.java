package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.*;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.BadBytecode;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import static com.pclewis.mcpatcher.BinaryRegex.*;
import static com.pclewis.mcpatcher.BytecodeMatcher.*;
import static javassist.bytecode.Opcode.*;

public class CustomColors extends Mod {
    private final boolean haveSpawnerEggs;
    private final boolean haveNewBiomes;
    private final boolean haveFontColor;
    private final boolean renderStringReturnsInt;
    private final boolean haveNewWorld;
    private final boolean renderBlockFallingSandTakes4Ints;
    private final boolean haveNightVision;
    private final MethodRef getColorFromDamage;
    private final int getColorFromDamageVer;
    private final boolean haveArmorDye;

    private static final FieldRef fleeceColorTable = new FieldRef("EntitySheep", "fleeceColorTable", "[[F");

    public CustomColors(MinecraftVersion minecraftVersion) {
        name = MCPatcherUtils.CUSTOM_COLORS;
        author = "MCPatcher";
        description = "Gives texture packs control over hardcoded colors in the game.";
        version = "1.4";

        addDependency(BaseTexturePackMod.NAME);

        haveSpawnerEggs = minecraftVersion.compareTo("12w01a") >= 0 || minecraftVersion.compareTo("1.0.1") >= 0;
        haveNewBiomes = minecraftVersion.compareTo("12w07a") >= 0;
        haveFontColor = minecraftVersion.compareTo("11w49a") >= 0;
        haveNewWorld = minecraftVersion.compareTo("12w18a") >= 0;
        renderStringReturnsInt = minecraftVersion.compareTo("1.2.4") >= 0;
        renderBlockFallingSandTakes4Ints = minecraftVersion.compareTo("12w22a") >= 0;
        haveNightVision = minecraftVersion.compareTo("12w32a") >= 0;
        if (minecraftVersion.compareTo("12w34a") >= 0) {
            getColorFromDamage = new MethodRef("Item", "getColorFromDamage", "(LItemStack;I)I");
            getColorFromDamageVer = 3;
        } else if (minecraftVersion.compareTo("1.1") >= 0) {
            getColorFromDamage = new MethodRef("Item", "getColorFromDamage", "(II)I");
            getColorFromDamageVer = 2;
        } else {
            getColorFromDamage = new MethodRef("Item", "getColorFromDamage", "(I)I");
            getColorFromDamageVer = 1;
        }
        haveArmorDye = minecraftVersion.compareTo("12w37a") >= 0;

        if (minecraftVersion.compareTo("Beta 1.9 Prerelease 4") < 0) {
            addError("Requires Minecraft Beta 1.9 or newer.");
            return;
        }

        configPanel = new ConfigPanel();

        addClassMod(new MinecraftMod(minecraftVersion));
        addClassMod(new IBlockAccessMod());
        addClassMod(new BlockMod());

        addClassMod(new BiomeGenBaseMod());
        addClassMod(new BiomeGenSwampMod());
        addClassMod(new BlockFluidMod());
        addClassMod(new BlockCauldronMod());
        addClassMod(new ItemMod());
        addClassMod(new ItemBlockMod());
        addClassMod(new ItemRendererMod());

        addClassMod(new PotionMod());
        addClassMod(new PotionHelperMod());

        addClassMod(new ColorizerFoliageMod());
        addClassMod(new BlockLeavesMod());

        addClassMod(new WorldMod());
        if (haveNewWorld) {
            addClassMod(new BaseMod.WorldServerMod(minecraftVersion));
        }
        addClassMod(new WorldProviderMod());
        addClassMod(new WorldProviderHellMod());
        addClassMod(new WorldProviderEndMod());
        addClassMod(new WorldChunkManagerMod());
        addClassMod(new EntityMod());
        addClassMod(new EntityFXMod());
        addClassMod(new EntityRainFXMod());
        addClassMod(new EntityDropParticleFXMod());
        addClassMod(new EntitySplashFXMod());
        addClassMod(new EntityBubbleFXMod());
        addClassMod(new EntitySuspendFXMod());
        addClassMod(new EntityPortalFXMod());
        addClassMod(new EntityAuraFXMod());

        addClassMod(new EntityLivingMod());
        addClassMod(new EntityRendererMod());

        addClassMod(new BlockLilyPadMod());

        addClassMod(new BlockRedstoneWireMod());
        addClassMod(new RenderBlocksMod());
        addClassMod(new EntityReddustFXMod());

        addClassMod(new RenderGlobalMod());

        addClassMod(new BlockStemMod());

        addClassMod(new MapColorMod());

        addClassMod(new ItemDyeMod());
        addClassMod(new EntitySheepMod());

        if (haveArmorDye) {
            addClassMod(new ItemArmorMod());
            addClassMod(new RenderWolfMod());
            addClassMod(new RecipesDyedArmorMod());
        }

        if (haveSpawnerEggs) {
            addClassMod(new EntityListMod());
            addClassMod(new ItemSpawnerEggMod());
        }

        if (haveFontColor) {
            addClassMod(new FontRendererMod());
            addClassMod(new TileEntitySignRendererMod());
        }

        addClassMod(new RenderXPOrbMod());

        addClassFile(MCPatcherUtils.COLORIZER_CLASS);
        addClassFile(MCPatcherUtils.COLORIZER_CLASS + "$1");
        addClassFile(MCPatcherUtils.COLOR_MAP_CLASS);
        addClassFile(MCPatcherUtils.LIGHTMAP_CLASS);
        addClassFile(MCPatcherUtils.BIOME_HELPER_CLASS);
        addClassFile(MCPatcherUtils.BIOME_HELPER_CLASS + "$Stub");
        addClassFile(MCPatcherUtils.BIOME_HELPER_CLASS + "$Old");
        addClassFile(MCPatcherUtils.BIOME_HELPER_CLASS + "$New");
    }

    private class ConfigPanel extends ModConfigPanel {
        private JCheckBox waterCheckBox;
        private JCheckBox swampCheckBox;
        private JCheckBox treeCheckBox;
        private JCheckBox potionCheckBox;
        private JCheckBox particleCheckBox;
        private JPanel panel;
        private JCheckBox lightmapCheckBox;
        private JCheckBox redstoneCheckBox;
        private JCheckBox stemCheckBox;
        private JCheckBox otherBlockCheckBox;
        private JCheckBox eggCheckBox;
        private JCheckBox fogCheckBox;
        private JCheckBox cloudsCheckBox;
        private JCheckBox mapCheckBox;
        private JCheckBox dyeCheckBox;
        private JSpinner fogBlendRadiusSpinner;
        private JSpinner blockBlendRadiusSpinner;
        private JCheckBox textCheckBox;
        private JCheckBox xpOrbCheckBox;

        ConfigPanel() {
            waterCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    MCPatcherUtils.set(MCPatcherUtils.CUSTOM_COLORS, "water", waterCheckBox.isSelected());
                }
            });

            swampCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    MCPatcherUtils.set(MCPatcherUtils.CUSTOM_COLORS, "swamp", swampCheckBox.isSelected());
                }
            });

            treeCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    MCPatcherUtils.set(MCPatcherUtils.CUSTOM_COLORS, "tree", treeCheckBox.isSelected());
                }
            });

            potionCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    MCPatcherUtils.set(MCPatcherUtils.CUSTOM_COLORS, "potion", potionCheckBox.isSelected());
                }
            });

            particleCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    MCPatcherUtils.set(MCPatcherUtils.CUSTOM_COLORS, "particle", particleCheckBox.isSelected());
                }
            });

            lightmapCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    MCPatcherUtils.set(MCPatcherUtils.CUSTOM_COLORS, "lightmaps", lightmapCheckBox.isSelected());
                }
            });

            cloudsCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    MCPatcherUtils.set(MCPatcherUtils.CUSTOM_COLORS, "clouds", cloudsCheckBox.isSelected());
                }
            });

            redstoneCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    MCPatcherUtils.set(MCPatcherUtils.CUSTOM_COLORS, "redstone", redstoneCheckBox.isSelected());
                }
            });

            stemCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    MCPatcherUtils.set(MCPatcherUtils.CUSTOM_COLORS, "stem", stemCheckBox.isSelected());
                }
            });

            eggCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    MCPatcherUtils.set(MCPatcherUtils.CUSTOM_COLORS, "egg", eggCheckBox.isSelected());
                }
            });

            mapCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    MCPatcherUtils.set(MCPatcherUtils.CUSTOM_COLORS, "map", mapCheckBox.isSelected());
                }
            });

            dyeCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    MCPatcherUtils.set(MCPatcherUtils.CUSTOM_COLORS, "dye", dyeCheckBox.isSelected());
                }
            });

            fogCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    MCPatcherUtils.set(MCPatcherUtils.CUSTOM_COLORS, "fog", fogCheckBox.isSelected());
                }
            });

            otherBlockCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    MCPatcherUtils.set(MCPatcherUtils.CUSTOM_COLORS, "otherBlocks", otherBlockCheckBox.isSelected());
                }
            });

            textCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    MCPatcherUtils.set(MCPatcherUtils.CUSTOM_COLORS, "text", textCheckBox.isSelected());
                }
            });

            xpOrbCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    MCPatcherUtils.set(MCPatcherUtils.CUSTOM_COLORS, "xporb", xpOrbCheckBox.isSelected());
                }
            });

            fogBlendRadiusSpinner.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    int value = 7;
                    try {
                        value = Integer.parseInt(fogBlendRadiusSpinner.getValue().toString());
                        value = Math.min(Math.max(0, value), 99);
                    } catch (NumberFormatException e1) {
                    }
                    MCPatcherUtils.set(MCPatcherUtils.CUSTOM_COLORS, "fogBlendRadius", value);
                    fogBlendRadiusSpinner.setValue(value);
                }
            });

            blockBlendRadiusSpinner.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    int value = 1;
                    try {
                        value = Integer.parseInt(blockBlendRadiusSpinner.getValue().toString());
                        value = Math.min(Math.max(0, value), 99);
                    } catch (NumberFormatException e1) {
                    }
                    MCPatcherUtils.set(MCPatcherUtils.CUSTOM_COLORS, "blockBlendRadius", value);
                    blockBlendRadiusSpinner.setValue(value);
                }
            });
        }

        @Override
        public JPanel getPanel() {
            return panel;
        }

        @Override
        public void load() {
            waterCheckBox.setSelected(MCPatcherUtils.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "water", true));
            swampCheckBox.setSelected(MCPatcherUtils.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "swamp", true));
            treeCheckBox.setSelected(MCPatcherUtils.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "tree", true));
            potionCheckBox.setSelected(MCPatcherUtils.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "potion", true));
            particleCheckBox.setSelected(MCPatcherUtils.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "particle", true));
            lightmapCheckBox.setSelected(MCPatcherUtils.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "lightmaps", true));
            cloudsCheckBox.setSelected(MCPatcherUtils.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "clouds", true));
            redstoneCheckBox.setSelected(MCPatcherUtils.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "redstone", true));
            stemCheckBox.setSelected(MCPatcherUtils.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "stem", true));
            eggCheckBox.setSelected(MCPatcherUtils.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "egg", true));
            mapCheckBox.setSelected(MCPatcherUtils.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "map", true));
            dyeCheckBox.setSelected(MCPatcherUtils.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "dye", true));
            fogCheckBox.setSelected(MCPatcherUtils.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "fog", true));
            otherBlockCheckBox.setSelected(MCPatcherUtils.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "otherBlocks", true));
            textCheckBox.setSelected(MCPatcherUtils.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "text", true));
            xpOrbCheckBox.setSelected(MCPatcherUtils.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "xporb", true));
            fogBlendRadiusSpinner.setValue(MCPatcherUtils.getInt(MCPatcherUtils.CUSTOM_COLORS, "fogBlendRadius", 7));
            blockBlendRadiusSpinner.setValue(MCPatcherUtils.getInt(MCPatcherUtils.CUSTOM_COLORS, "blockBlendRadius", 1));
            eggCheckBox.setVisible(haveSpawnerEggs);
        }

        @Override
        public void save() {
        }
    }

    private class MinecraftMod extends BaseMod.MinecraftMod {
        MinecraftMod(MinecraftVersion minecraftVersion) {
            final MethodRef runGameLoop = new MethodRef(getDeobfClass(), "runGameLoop", "()V");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(65),
                        reference(INVOKESTATIC, new MethodRef("org/lwjgl/input/Keyboard", "isKeyDown", "(I)Z")),
                        IFEQ, any(2),
                        reference(INVOKESTATIC, new MethodRef("org/lwjgl/opengl/Display", "update", "()V"))
                    );
                }
            }.setMethod(runGameLoop));

            addWorldGetter(minecraftVersion);

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "set up block access";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        begin()
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        ALOAD_0,
                        reference(INVOKEVIRTUAL, new MethodRef(getDeobfClass(), "getWorld", "()LWorld;")),
                        push(haveNewBiomes),
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.COLORIZER_CLASS, "setupBlockAccess", "(LIBlockAccess;Z)V"))
                    );
                }
            }.targetMethod(runGameLoop));
        }
    }

    private class BlockMod extends BaseMod.BlockMod {
        BlockMod() {
            final MethodRef getRenderColor = new MethodRef(getDeobfClass(), "getRenderColor", "(I)I");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    if (getMethodInfo().getDescriptor().equals(getRenderColor.getType())) {
                        return buildExpression(
                            begin(),
                            push(0xffffff),
                            IRETURN,
                            end()
                        );
                    } else {
                        return null;
                    }
                }
            }.setMethod(getRenderColor));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override color multiplier for all blocks";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(0xffffff)
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        ALOAD_0,
                        ILOAD_2,
                        ILOAD_3,
                        ILOAD, 4,
                        ALOAD_1,
                        ILOAD_2,
                        ILOAD_3,
                        ILOAD, 4,
                        reference(INVOKEINTERFACE, new InterfaceMethodRef("IBlockAccess", "getBlockMetadata", "(III)I")),
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.COLORIZER_CLASS, "colorizeBlock", "(LBlock;IIII)I"))
                    );
                }

            }.targetMethod(new MethodRef(getDeobfClass(), "colorMultiplier", "(LIBlockAccess;III)I")));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override default color for all blocks";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(0xffffff)
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        ALOAD_0,
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.COLORIZER_CLASS, "colorizeBlock", "(LBlock;)I"))
                    );
                }
            }.targetMethod(getRenderColor));
        }
    }

    private class IBlockAccessMod extends BaseMod.IBlockAccessMod {
        IBlockAccessMod() {
            if (haveNewBiomes) {
                addMemberMapper(new MethodMapper(new MethodRef(getDeobfClass(), "getBiomeGenAt", "(II)LBiomeGenBase;")));
            }
        }
    }

    private class BiomeGenBaseMod extends ClassMod {
        BiomeGenBaseMod() {
            addClassSignature(new ConstSignature("Ocean"));
            addClassSignature(new ConstSignature("Plains"));
            addClassSignature(new ConstSignature("Desert"));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        push(0xffffff),
                        captureReference(PUTFIELD)
                    );
                }
            }
                .matchConstructorOnly(true)
                .addXref(1, new FieldRef(getDeobfClass(), "waterColorMultiplier", "I"))
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
                .addXref(1, new FieldRef(getDeobfClass(), "biomeID", "I"))
            );

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        push(0.5f),
                        captureReference(PUTFIELD),
                        ALOAD_0,
                        push(0.5f),
                        captureReference(PUTFIELD)
                    );
                }
            }
                .matchConstructorOnly(true)
                .addXref(1, new FieldRef(getDeobfClass(), "temperature", "F"))
                .addXref(2, new FieldRef(getDeobfClass(), "rainfall", "F"))
            );

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    if (getMethodInfo().getDescriptor().startsWith("(Ljava/lang/String;)")) {
                        return buildExpression(
                            begin(),
                            ALOAD_0,
                            ALOAD_1,
                            captureReference(PUTFIELD),
                            ALOAD_0,
                            ARETURN,
                            end()
                        );
                    } else {
                        return null;
                    }
                }
            }
                .setMethodName("setBiomeName")
                .addXref(1, new FieldRef(getDeobfClass(), "biomeName", "Ljava/lang/String;"))
            );

            if (haveNewBiomes) {
                addClassSignature(new BytecodeSignature() {
                    @Override
                    public String getMatchExpression() {
                        return buildExpression(
                            // d = MathHelper.clampf(getTemperature(), 0.0f, 1.0f);
                            begin(),
                            ALOAD_0,
                            captureReference(INVOKEVIRTUAL),
                            FCONST_0,
                            FCONST_1,
                            anyReference(INVOKESTATIC),
                            F2D,
                            DSTORE_1,

                            // d1 = MathHelper.clampf(getRainfall(), 0.0f, 1.0f);
                            ALOAD_0,
                            captureReference(INVOKEVIRTUAL),
                            FCONST_0,
                            FCONST_1,
                            anyReference(INVOKESTATIC),
                            F2D,
                            DSTORE_3,

                            // return Colorizerxxx.yyy(d, d1);
                            DLOAD_1,
                            DLOAD_3,
                            anyReference(INVOKESTATIC),
                            IRETURN
                        );
                    }
                }
                    .addXref(1, new MethodRef(getDeobfClass(), "getTemperaturef", "()F"))
                    .addXref(2, new MethodRef(getDeobfClass(), "getRainfallf", "()F"))
                );

                addMemberMapper(new MethodMapper(new MethodRef(getDeobfClass(), "getGrassColor", "()I"), new MethodRef(getDeobfClass(), "getFoliageColor", "()I"))
                    .accessFlag(AccessFlag.PUBLIC, true)
                    .accessFlag(AccessFlag.STATIC, false)
                    .accessFlag(AccessFlag.FINAL, false)
                );
            } else {
                addClassSignature(new BytecodeSignature() {
                    @Override
                    public String getMatchExpression() {
                        return buildExpression(
                            // d = iblockaccess.getWorldChunkManager().getTemperature(i, j, k);
                            ALOAD_1,
                            anyReference(INVOKEINTERFACE),
                            ILOAD_2,
                            ILOAD_3,
                            ILOAD, 4,
                            captureReference(INVOKEVIRTUAL),
                            F2D,
                            DSTORE, 5,

                            // d1 = iblockaccess.getWorldChunkManager().getRainfall(i, k);
                            ALOAD_1,
                            anyReference(INVOKEINTERFACE),
                            ILOAD_2,
                            ILOAD, 4,
                            captureReference(INVOKEVIRTUAL),
                            F2D,
                            DSTORE, 7
                        );
                    }
                }
                    .addXref(1, new MethodRef("WorldChunkManager", "getTemperature", "(III)F"))
                    .addXref(2, new MethodRef("WorldChunkManager", "getRainfall", "(II)F"))
                );

                addMemberMapper(new MethodMapper(new MethodRef(getDeobfClass(), "getGrassColor", "(LIBlockAccess;III)I"), new MethodRef(getDeobfClass(), "getFoliageColor", "(LIBlockAccess;III)I")));
            }

            addMemberMapper(new FieldMapper(new FieldRef(getDeobfClass(), "color", "I")).accessFlag(AccessFlag.PUBLIC, true));

            addPatch(new BytecodePatch.InsertBefore() {
                @Override
                public String getDescription() {
                    return "map biomes by name";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        ARETURN,
                        end()
                    );
                }

                @Override
                public byte[] getInsertBytes() throws IOException {
                    return buildCode(
                        ALOAD_0,
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.COLORIZER_CLASS, "setupBiome", "(LBiomeGenBase;)V"))
                    );
                }
            }.targetMethod(new MethodRef(getDeobfClass(), "setBiomeName", "(Ljava/lang/String;)LBiomeGenBase;")));
        }
    }

    private class BiomeGenSwampMod extends ClassMod {
        private static final int MAGIC1 = 0xfefefe;
        private static final int MAGIC2 = 0x4e0e4e;
        private static final int MAGIC3_A = 0xe0ff70;
        private static final int MAGIC3_B = 0xe0ffae;

        BiomeGenSwampMod() {
            setParentClass("BiomeGenBase");

            addClassSignature(new ConstSignature(MAGIC1));
            addClassSignature(new ConstSignature(MAGIC2));
            addClassSignature(new OrSignature(
                new ConstSignature(MAGIC3_A),
                new ConstSignature(MAGIC3_B)
            ));

            addSwampColorPatch("SWAMP_GRASS", "Grass");
            addSwampColorPatch("SWAMP_FOLIAGE", "Foliage");
        }

        private void addSwampColorPatch(final String index, final String name) {
            if (haveNewBiomes) {
                addPatch(new BytecodePatch() {
                    @Override
                    public String getDescription() {
                        return "override swamp " + name.toLowerCase() + " color";
                    }

                    @Override
                    public String getMatchExpression() {
                        return buildExpression(
                            IRETURN
                        );
                    }

                    @Override
                    public byte[] getReplacementBytes() throws IOException {
                        return buildCode(
                            reference(GETSTATIC, new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "COLOR_MAP_" + index, "I")),
                            DLOAD_1,
                            DLOAD_3,
                            reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.COLORIZER_CLASS, "colorizeBiome", "(IIDD)I")),
                            IRETURN
                        );
                    }
                }.targetMethod(new MethodRef(getDeobfClass(), "get" + name + "Color", "()I")));
            } else {
                addPatch(new BytecodePatch.InsertAfter() {
                    @Override
                    public String getDescription() {
                        return "override swamp " + name.toLowerCase() + " color";
                    }

                    @Override
                    public String getMatchExpression() {
                        return buildExpression(
                            push(MAGIC1),
                            IAND,
                            push(MAGIC2),
                            IADD,
                            ICONST_2,
                            IDIV
                        );
                    }

                    @Override
                    public byte[] getInsertBytes() throws IOException {
                        return buildCode(
                            reference(GETSTATIC, new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "COLOR_MAP_" + index, "I")),
                            DLOAD, 5,
                            DLOAD, 7,
                            reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.COLORIZER_CLASS, "colorizeBiome", "(IIDD)I"))
                        );
                    }
                }.targetMethod(new MethodRef(getDeobfClass(), "get" + name + "Color", "(LIBlockAccess;III)I")));
            }
        }
    }

    private class BlockFluidMod extends ClassMod {
        BlockFluidMod() {
            addClassSignature(new ConstSignature("splash"));
            addClassSignature(new ConstSignature("liquid.water"));

            final MethodRef colorMultiplier = new MethodRef(getDeobfClass(), "colorMultiplier", "(LIBlockAccess;III)I");
            final FieldRef waterColorMultiplier = new FieldRef("BiomeGenBase", "waterColorMultiplier", "I");
            final MethodRef getWaterColorMultiplier = new MethodRef("BiomeGenBase", "getWaterColorMultiplier", "()I");

            if (haveNewBiomes) {
                addClassSignature(new BytecodeSignature() {
                    @Override
                    public String getMatchExpression() {
                        return buildExpression(
                            ALOAD_1,
                            ILOAD_2,
                            anyILOAD,
                            IADD,
                            ILOAD, 4,
                            anyILOAD,
                            IADD,
                            anyReference(INVOKEINTERFACE),
                            captureReference(GETFIELD)
                        );
                    }
                }
                    .setMethod(colorMultiplier)
                    .addXref(1, waterColorMultiplier)
                );

                addPatch(new BytecodePatch() {
                    @Override
                    public String getDescription() {
                        return "override water color";
                    }

                    @Override
                    public String getMatchExpression() {
                        return buildExpression(
                            reference(INVOKEINTERFACE, new InterfaceMethodRef("IBlockAccess", "getBiomeGenAt", "(II)LBiomeGenBase;")),
                            or(
                                build(reference(GETFIELD, waterColorMultiplier)), // vMC
                                build(reference(INVOKEVIRTUAL, getWaterColorMultiplier)) // forge
                            )

                        );
                    }

                    @Override
                    public byte[] getReplacementBytes() throws IOException {
                        return buildCode(
                            reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.COLORIZER_CLASS, "colorizeWater", "(Ljava/lang/Object;II)I"))
                        );
                    }
                }.targetMethod(colorMultiplier));
            } else {
                addClassSignature(new BytecodeSignature() {
                    @Override
                    public String getMatchExpression() {
                        return buildExpression(
                            ALOAD_1,
                            anyReference(INVOKEINTERFACE),
                            ILOAD_2,
                            any(0, 3),
                            ILOAD, 4,
                            any(0, 3),
                            anyReference(INVOKEVIRTUAL),
                            optional(build(
                                ASTORE, 5,
                                ALOAD, 5
                            )),
                            anyReference(GETFIELD)
                        );
                    }
                }.setMethod(colorMultiplier));

                addPatch(new BytecodePatch() {
                    @Override
                    public String getDescription() {
                        return "override water color";
                    }

                    @Override
                    public String getMatchExpression() {
                        return buildExpression(
                            reference(INVOKEVIRTUAL, new MethodRef("WorldChunkManager", "getBiomeGenAt", "(II)LBiomeGenBase;")),
                            any(0, 4),
                            reference(GETFIELD, waterColorMultiplier)
                        );
                    }

                    @Override
                    public byte[] getReplacementBytes() throws IOException {
                        return buildCode(
                            reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.COLORIZER_CLASS, "colorizeWater", "(Ljava/lang/Object;II)I"))
                        );
                    }
                }.targetMethod(colorMultiplier));
            }
        }
    }

    private class BlockCauldronMod extends ClassMod {
        BlockCauldronMod() {
            setParentClass("Block");

            for (final int i : new int[]{138, 154, 155}) {
                addClassSignature(new BytecodeSignature() {
                    @Override
                    public String getMatchExpression() {
                        if (getMethodInfo().getDescriptor().equals("(II)I")) {
                            return buildExpression(
                                push(i)
                            );
                        } else {
                            return null;
                        }
                    }
                });
            }
        }
    }

    private class ItemMod extends BaseMod.ItemMod {
        ItemMod() {
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
            }.setMethod(getColorFromDamage));
        }
    }

    private class ItemBlockMod extends ClassMod {
        ItemBlockMod() {
            final FieldRef blockID = new FieldRef(getDeobfClass(), "blockID", "I");

            setParentClass("Item");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        ILOAD_1,
                        push(256),
                        IADD,
                        anyReference(PUTFIELD)
                    );
                }
            }.matchConstructorOnly(true));

            addMemberMapper(new FieldMapper(blockID).accessFlag(AccessFlag.PRIVATE, true));

            addPatch(new AddMethodPatch(new MethodRef(getDeobfClass(), getColorFromDamage.getName(), getColorFromDamage.getType())) {
                @Override
                public byte[] generateMethod() throws BadBytecode, IOException {
                    byte[] code;
                    switch (getColorFromDamageVer) {
                        case 1:
                            code = buildCode(ILOAD_1, ICONST_0);
                            break;

                        case 2:
                            code = buildCode(ILOAD_1, ILOAD_2);
                            break;

                        default:
                            code = buildCode(ALOAD_1, ILOAD_2);
                            break;
                    }
                    return buildCode(
                        ALOAD_0,
                        code,
                        reference(INVOKESPECIAL, getColorFromDamage),
                        ALOAD_0,
                        reference(GETFIELD, blockID),
                        ILOAD_2,
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.COLORIZER_CLASS, "getItemColorFromDamage", "(III)I")),
                        IRETURN
                    );
                }
            });
        }
    }

    private class ItemRendererMod extends ClassMod {
        ItemRendererMod() {
            final FieldRef itemID = new FieldRef("ItemStack", "itemID", "I");
            final MethodRef renderItem = new MethodRef(getDeobfClass(), "renderItem", "(LEntityLiving;LItemStack;I)V");

            addClassSignature(new ConstSignature("/terrain.png"));
            addClassSignature(new ConstSignature("/gui/items.png"));
            addClassSignature(new ConstSignature("%blur%/misc/glint.png"));
            addClassSignature(new ConstSignature("/misc/mapbg.png"));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // par2ItemStack.itemID
                        ALOAD_2,
                        captureReference(GETFIELD),
                        or(
                            build(push(256)),
                            build(AALOAD)
                        ),

                        // ...
                        any(0, 400),

                        // GL11.glTranslatef(-0.9375f, -0.0625f, 0.0f);
                        push(-0.9375f),
                        push(-0.0625f),
                        FCONST_0,
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.GL11_CLASS, "glTranslatef", "(FFF)V"))
                    );
                }
            }
                .addXref(1, itemID)
                .setMethod(renderItem)
            );

            addPatch(new BytecodePatch.InsertAfter() {
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
                        FCONST_0,
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.GL11_CLASS, "glTranslatef", "(FFF)V"))
                    );
                }

                @Override
                public byte[] getInsertBytes() throws IOException {
                    return buildCode(
                        // if (par2ItemStack != null) {
                        ALOAD_2,
                        IFNULL, branch("A"),

                        // Colorizer.colorizeWaterBlockGL(par2ItemStack.itemID)
                        ALOAD_2,
                        reference(GETFIELD, itemID),
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.COLORIZER_CLASS, "colorizeWaterBlockGL", "(I)V")),

                        // }
                        label("A")
                    );
                }
            }.targetMethod(renderItem));
        }
    }

    private class PotionMod extends ClassMod {
        PotionMod() {
            addClassSignature(new ConstSignature("potion.moveSpeed"));
            addClassSignature(new ConstSignature("potion.moveSlowdown"));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    if (getMethodInfo().getDescriptor().startsWith("(Ljava/lang/String;)")) {
                        return buildExpression(
                            begin(),
                            ALOAD_0,
                            ALOAD_1,
                            captureReference(PUTFIELD),
                            ALOAD_0,
                            ARETURN,
                            end()
                        );
                    } else {
                        return null;
                    }
                }
            }
                .setMethodName("setPotionName")
                .addXref(1, new FieldRef(getDeobfClass(), "name", "Ljava/lang/String;"))
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
                .addXref(1, new FieldRef(getDeobfClass(), "id", "I"))
            );

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        ILOAD_3,
                        captureReference(PUTFIELD)
                    );
                }
            }
                .matchConstructorOnly(true)
                .addXref(1, new FieldRef(getDeobfClass(), "color", "I"))
            );

            addPatch(new MakeMemberPublicPatch(new FieldRef(getDeobfClass(), "name", "Ljava/lang/String;")));
            addPatch(new AddFieldPatch(new FieldRef(getDeobfClass(), "origColor", "I")));

            addPatch(new MakeMemberPublicPatch(new FieldRef(getDeobfClass(), "color", "I")) {
                @Override
                public int getNewFlags(int oldFlags) {
                    return super.getNewFlags(oldFlags) & ~AccessFlag.FINAL;
                }
            });

            addPatch(new BytecodePatch.InsertBefore() {
                @Override
                public String getDescription() {
                    return "map potions by name";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        ARETURN,
                        end()
                    );
                }

                @Override
                public byte[] getInsertBytes() throws IOException {
                    return buildCode(
                        ALOAD_0,
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.COLORIZER_CLASS, "setupPotion", "(LPotion;)V"))
                    );
                }
            }.targetMethod(new MethodRef(getDeobfClass(), "setPotionName", "(Ljava/lang/String;)LPotion;")));
        }
    }

    private class PotionHelperMod extends ClassMod {
        private static final int MAGIC = 0x385dc6;

        PotionHelperMod() {
            addClassSignature(new ConstSignature("potion.prefix.mundane"));
            addClassSignature(new ConstSignature(MAGIC));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override water bottle color";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // int i = 0x385dc6;
                        begin(),
                        push(MAGIC),
                        ISTORE_1
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        // int i = Colorizer.getWaterBottleColor();
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.COLORIZER_CLASS, "getWaterBottleColor", "()I")),
                        ISTORE_1
                    );
                }
            });
        }
    }

    private class ColorizerFoliageMod extends ClassMod {
        ColorizerFoliageMod() {
            setupColor("PINE", 0x619961, "Pine");
            setupColor("BIRCH", 0x80a755, "Birch");
            setupColor("FOLIAGE", 0x48b518, "Basic");
        }

        private void setupColor(final String index, final int color, final String name) {
            addClassSignature(new ConstSignature(color));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override default " + name.toLowerCase() + " foliage color";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        begin(),
                        push(color),
                        IRETURN,
                        end()
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        push(color),
                        reference(GETSTATIC, new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "COLOR_MAP_" + index, "I")),
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.COLORIZER_CLASS, "colorizeBiome", "(II)I")),
                        IRETURN
                    );
                }
            });
        }
    }

    private class BlockLeavesMod extends ClassMod {
        BlockLeavesMod() {
            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        begin(),
                        ALOAD_1,
                        ILOAD_2,
                        ILOAD_3,
                        ILOAD, 4,
                        captureReference(INVOKEINTERFACE),
                        ISTORE, 5,

                        ILOAD, 5,
                        subset(new byte[]{ICONST_1, ICONST_3}, true), // 1.1 uses (i & 1) == 1, 12w03a uses (i & 3) == 1
                        IAND,
                        ICONST_1,
                        IF_ICMPNE, any(2),
                        captureReference(INVOKESTATIC),
                        IRETURN,

                        ILOAD, 5,
                        subset(new byte[]{ICONST_2, ICONST_3}, true), // 1.1 uses (i & 2) == 2, 12w03a uses (i & 3) == 2
                        IAND,
                        ICONST_2,
                        IF_ICMPNE, any(2),

                        captureReference(INVOKESTATIC),
                        IRETURN
                    );
                }
            }
                .setMethod(new MethodRef(getDeobfClass(), "colorMultiplier", "(LIBlockAccess;III)I"))
                .addXref(1, new InterfaceMethodRef("IBlockAccess", "getBlockMetadata", "(III)I"))
                .addXref(2, new MethodRef("ColorizerFoliage", "getFoliageColorPine", "()I"))
                .addXref(3, new MethodRef("ColorizerFoliage", "getFoliageColorBirch", "()I"))
            );

            if (haveNewBiomes) {
                addClassSignature(new BytecodeSignature() {
                    @Override
                    public String getMatchExpression() {
                        return buildExpression(
                            ALOAD_1,
                            ILOAD_2,
                            anyILOAD,
                            IADD,
                            ILOAD, 4,
                            anyILOAD,
                            IADD,
                            captureReference(INVOKEINTERFACE),
                            captureReference(INVOKEVIRTUAL)
                        );
                    }
                }
                    .addXref(1, new InterfaceMethodRef("IBlockAccess", "getBiomeGenAt", "(II)LBiomeGenBase;"))
                    .addXref(2, new MethodRef("BiomeGenBase", "getFoliageColor", "()I"))
                );
            } else {
                addClassSignature(new BytecodeSignature() {
                    @Override
                    public String getMatchExpression() {
                        return buildExpression(
                            ALOAD_1,
                            captureReference(INVOKEINTERFACE),
                            ILOAD_2,
                            any(0, 3),
                            ILOAD, 4,
                            any(0, 3),
                            captureReference(INVOKEVIRTUAL),
                            ALOAD_1,
                            ILOAD_2,
                            any(0, 3),
                            ILOAD_3,
                            ILOAD, 4,
                            any(0, 3),
                            captureReference(INVOKEVIRTUAL)
                        );
                    }
                }
                    .addXref(1, new InterfaceMethodRef("IBlockAccess", "getWorldChunkManager", "()LWorldChunkManager;"))
                    .addXref(2, new MethodRef("WorldChunkManager", "getBiomeGenAt", "(II)LBiomeGenBase;"))
                    .addXref(3, new MethodRef("BiomeGenBase", "getFoliageColor", "(LIBlockAccess;III)I"))
                );
            }

            addFoliagePatch("PINE", "Pine");
            addFoliagePatch("BIRCH", "Birch");
        }

        private void addFoliagePatch(final String index, final String name) {
            addPatch(new BytecodePatch.InsertAfter() {
                @Override
                public String getDescription() {
                    return "override biome " + name.toLowerCase() + " foliage color";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        reference(INVOKESTATIC, new MethodRef("ColorizerFoliage", "getFoliageColor" + name, "()I"))
                    );
                }

                @Override
                public byte[] getInsertBytes() throws IOException {
                    return buildCode(
                        reference(GETSTATIC, new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "COLOR_MAP_" + index, "I")),
                        ILOAD_2,
                        ILOAD_3,
                        ILOAD, 4,
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.COLORIZER_CLASS, "colorizeBiomeWithBlending", "(IIIII)I"))
                    );
                }
            }.targetMethod(new MethodRef(getDeobfClass(), "colorMultiplier", "(LIBlockAccess;III)I")));
        }
    }

    private class WorldMod extends BaseMod.WorldMod {
        WorldMod() {
            setInterfaces("IBlockAccess");

            addMemberMapper(new MethodMapper(new MethodRef(getDeobfClass(), "getWorldChunkManager", "()LWorldChunkManager;")));

            addPatch(new BytecodePatch() {
                {
                    addPreMatchSignature(new BytecodeSignature() {
                        @Override
                        public String getMatchExpression() {
                            return buildExpression(
                                // f8 = (f4 * 0.3f + f5 * 0.59f + f6 * 0.11f) * 0.6f;
                                FLOAD, any(),
                                push(0.3f),
                                FMUL,
                                FLOAD, any(),
                                push(0.59f),
                                FMUL,
                                FADD,
                                FLOAD, any(),
                                push(0.11f),
                                FMUL,
                                FADD,
                                push(0.6f),
                                FMUL,
                                FSTORE, any()
                            );
                        }
                    });
                }

                @Override
                public String getDescription() {
                    return "override sky color";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // f4 = (float) (k >> 16 & 0xff) / 255.0f;
                        ILOAD, capture(any()),
                        push(16),
                        ISHR,
                        push(255),
                        IAND,
                        I2F,
                        push(255.0f),
                        FDIV,
                        FSTORE, capture(any()),

                        // f5 = (float) (k >> 8 & 0xff) / 255.0f;
                        ILOAD, backReference(1),
                        push(8),
                        ISHR,
                        push(255),
                        IAND,
                        I2F,
                        push(255.0f),
                        FDIV,
                        FSTORE, capture(any()),

                        // f6 = (float) (k & 0xff) / 255.0f;
                        ILOAD, backReference(1),
                        push(255),
                        IAND,
                        I2F,
                        push(255.0f),
                        FDIV,
                        FSTORE, capture(any())
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        // Colorizer.setupForFog(entity);
                        ALOAD_1,
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.COLORIZER_CLASS, "setupForFog", "(LEntity;)V")),

                        // if (Colorizer.computeSkyColor(this, f)) {
                        ALOAD_0,
                        FLOAD_2,
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.COLORIZER_CLASS, "computeSkyColor", "(LWorld;F)Z")),
                        IFEQ, branch("A"),

                        // f4 = Colorizer.setColor[0];
                        reference(GETSTATIC, new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "setColor", "[F")),
                        ICONST_0,
                        FALOAD,
                        FSTORE, getCaptureGroup(2),

                        // f5 = Colorizer.setColor[1];
                        reference(GETSTATIC, new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "setColor", "[F")),
                        ICONST_1,
                        FALOAD,
                        FSTORE, getCaptureGroup(3),

                        // f5 = Colorizer.setColor[2];
                        reference(GETSTATIC, new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "setColor", "[F")),
                        ICONST_2,
                        FALOAD,
                        FSTORE, getCaptureGroup(4),

                        // } else {
                        GOTO, branch("B"),
                        label("A"),

                        // ... original code ...
                        getMatch(),

                        // }
                        label("B")
                    );
                }
            });
        }
    }

    private class WorldProviderMod extends ClassMod {
        WorldProviderMod() {
            addClassSignature(new ConstSignature(0.06f));
            addClassSignature(new ConstSignature(0.09f));
            addClassSignature(new ConstSignature(0.91f));
            addClassSignature(new ConstSignature(0.94f));

            MethodRef getFogColor = new MethodRef(getDeobfClass(), "getFogColor", "(FF)LVec3D;");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        FLOAD, capture(any()),
                        FLOAD_3,
                        push(0.94f),
                        FMUL,
                        push(0.06f),
                        FADD,
                        FMUL,
                        FSTORE, backReference(1)
                    );
                }
            }.setMethod(getFogColor));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override fog color";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // f3 = 0.7529412f;
                        push(0.7529412f),
                        FSTORE, capture(any()),

                        // f4 = 0.84705883f;
                        push(0.84705883f),
                        FSTORE, capture(any()),

                        // f5 = 1.0f;
                        push(1.0f),
                        FSTORE, capture(any())
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        // if (Colorizer.computeFogColor(Colorizer.COLOR_MAP_FOG0)) {
                        reference(GETSTATIC, new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "COLOR_MAP_FOG0", "I")),
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.COLORIZER_CLASS, "computeFogColor", "(I)Z")),
                        IFEQ, branch("A"),

                        // f3 = Colorizer.setColor[0];
                        reference(GETSTATIC, new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "setColor", "[F")),
                        ICONST_0,
                        FALOAD,
                        FSTORE, getCaptureGroup(1),

                        // f4 = Colorizer.setColor[1];
                        reference(GETSTATIC, new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "setColor", "[F")),
                        ICONST_1,
                        FALOAD,
                        FSTORE, getCaptureGroup(2),

                        // f5 = Colorizer.setColor[2];
                        reference(GETSTATIC, new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "setColor", "[F")),
                        ICONST_2,
                        FALOAD,
                        FSTORE, getCaptureGroup(3),

                        // } else {
                        GOTO, branch("B"),
                        label("A"),

                        // ... original code ...
                        getMatch(),

                        // }
                        label("B")
                    );
                }
            }.targetMethod(getFogColor));
        }
    }

    private class WorldProviderHellMod extends ClassMod {
        private static final double MAGIC1 = 0.20000000298023224;
        private static final double MAGIC2 = 0.029999999329447746;

        WorldProviderHellMod() {
            setParentClass("WorldProvider");

            final MethodRef getFogColor = new MethodRef(getDeobfClass(), "getFogColor", "(FF)LVec3D;");
            final FieldRef netherFogColor = new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "netherFogColor", "[F");

            addClassSignature(new ConstSignature(MAGIC1));
            addClassSignature(new ConstSignature(MAGIC2));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(MAGIC1),
                        push(MAGIC2),
                        push(MAGIC2)
                    );
                }
            }.setMethod(getFogColor));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override nether fog color";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(MAGIC1),
                        push(MAGIC2),
                        push(MAGIC2)
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        // Colorizer.setColor[0], Colorizer.setColor[1], Colorizer.setColor[2]
                        reference(GETSTATIC, netherFogColor),
                        ICONST_0,
                        FALOAD,
                        F2D,

                        reference(GETSTATIC, netherFogColor),
                        ICONST_1,
                        FALOAD,
                        F2D,

                        reference(GETSTATIC, netherFogColor),
                        ICONST_2,
                        FALOAD,
                        F2D
                    );
                }
            }.targetMethod(getFogColor));
        }
    }

    private class WorldProviderEndMod extends ClassMod {
        WorldProviderEndMod() {
            setParentClass("WorldProvider");

            addClassSignature(new OrSignature(
                new ConstSignature(0x8080a0), // pre 12w23a
                new ConstSignature(0xa080a0)  // 12w23a+
            ));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        anyFLOAD,
                        F2D,
                        anyFLOAD,
                        F2D,
                        anyFLOAD,
                        F2D
                    );
                }
            });

            MethodRef getFogColor = new MethodRef(getDeobfClass(), "getFogColor", "(FF)LVec3D;");

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override end fog color";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        anyFLOAD,
                        F2D,
                        anyFLOAD,
                        F2D,
                        anyFLOAD,
                        F2D
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        reference(GETSTATIC, new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "endFogColor", "[F")),
                        ICONST_0,
                        FALOAD,
                        F2D,
                        reference(GETSTATIC, new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "endFogColor", "[F")),
                        ICONST_1,
                        FALOAD,
                        F2D,
                        reference(GETSTATIC, new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "endFogColor", "[F")),
                        ICONST_2,
                        FALOAD,
                        F2D
                    );
                }
            }.targetMethod(getFogColor));
        }
    }

    private class WorldChunkManagerMod extends ClassMod {
        WorldChunkManagerMod() {
            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ILOAD, 4,
                        ILOAD, 5,
                        IMUL,
                        NEWARRAY, T_FLOAT,
                        ASTORE_1
                    );
                }
            });

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        begin(),
                        optional(anyReference(INVOKESTATIC)),
                        ILOAD_1,
                        ILOAD_3,
                        ISUB,
                        ICONST_2,
                        ISHR,
                        ISTORE, 6
                    );
                }
            });

            addMemberMapper(new MethodMapper(new MethodRef(getDeobfClass(), "getBiomeGenAt", "(II)LBiomeGenBase;")));
        }
    }

    private class EntityMod extends ClassMod {
        EntityMod() {
            addClassSignature(new ConstSignature("tilecrack_"));
            addClassSignature(new OrSignature(
                new ConstSignature("random.splash"),
                new ConstSignature("liquid.splash") // 12w38a+
            ));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        begin(),

                        // prevPosX = posX = d;
                        ALOAD_0,
                        ALOAD_0,
                        DLOAD_1,
                        DUP2_X1,
                        captureReference(PUTFIELD),
                        captureReference(PUTFIELD),

                        // prevPosY = posY = d1;
                        ALOAD_0,
                        ALOAD_0,
                        DLOAD_3,
                        DUP2_X1,
                        captureReference(PUTFIELD),
                        captureReference(PUTFIELD),

                        // prevPosZ = posZ = d2;
                        ALOAD_0,
                        ALOAD_0,
                        DLOAD, 5,
                        DUP2_X1,
                        captureReference(PUTFIELD),
                        captureReference(PUTFIELD)
                    );
                }
            }
                .setMethod(new MethodRef(getDeobfClass(), "setPositionAndRotation", "(DDDFF)V"))
                .addXref(1, new FieldRef(getDeobfClass(), "posX", "D"))
                .addXref(2, new FieldRef(getDeobfClass(), "prevPosX", "D"))
                .addXref(3, new FieldRef(getDeobfClass(), "posY", "D"))
                .addXref(4, new FieldRef(getDeobfClass(), "prevPosY", "D"))
                .addXref(5, new FieldRef(getDeobfClass(), "posZ", "D"))
                .addXref(6, new FieldRef(getDeobfClass(), "prevPosZ", "D"))
            );

            addMemberMapper(new FieldMapper(new FieldRef(getDeobfClass(), "worldObj", "LWorld;")));
        }
    }

    private class EntityFXMod extends ClassMod {
        EntityFXMod() {
            setParentClass("Entity");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // setSize(0.2f, 0.2f);
                        ALOAD_0,
                        push(0.2f),
                        push(0.2f),
                        anyReference(INVOKEVIRTUAL)
                    );
                }
            }.matchConstructorOnly(true));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // particleRed = particleGreen = particleBlue = 1.0f;
                        ALOAD_0,
                        ALOAD_0,
                        ALOAD_0,
                        FCONST_1,
                        DUP_X1,
                        captureReference(PUTFIELD),
                        DUP_X1,
                        captureReference(PUTFIELD),
                        captureReference(PUTFIELD)
                    );
                }
            }
                .matchConstructorOnly(true)
                .addXref(1, new FieldRef(getDeobfClass(), "particleBlue", "F"))
                .addXref(2, new FieldRef(getDeobfClass(), "particleGreen", "F"))
                .addXref(3, new FieldRef(getDeobfClass(), "particleRed", "F"))
            );
        }
    }

    abstract private class WaterFXMod extends ClassMod {
        void addWaterColorPatch(final String name, final float[] particleColors) {
            addWaterColorPatch(name, particleColors, particleColors);
        }

        void addWaterColorPatch(final String name, final float[] origColors, final float[] newColors) {
            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override " + name + " color";
                }

                @Override
                public String getMatchExpression() {
                    if (origColors == null) {
                        return buildExpression(
                            RETURN
                        );
                    } else {
                        return buildExpression(
                            // particleRed = r;
                            ALOAD_0,
                            push(origColors[0]),
                            reference(PUTFIELD, new FieldRef(getDeobfClass(), "particleRed", "F")),

                            // particleGreen = g;
                            ALOAD_0,
                            push(origColors[1]),
                            reference(PUTFIELD, new FieldRef(getDeobfClass(), "particleGreen", "F")),

                            // particleBlue = b;
                            ALOAD_0,
                            push(origColors[2]),
                            reference(PUTFIELD, new FieldRef(getDeobfClass(), "particleBlue", "F"))
                        );
                    }
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        // if (Colorizer.computeWaterColor(i, j, k)) {
                        ALOAD_0,
                        reference(GETFIELD, new FieldRef(getDeobfClass(), "posX", "D")),
                        ALOAD_0,
                        reference(GETFIELD, new FieldRef(getDeobfClass(), "posY", "D")),
                        ALOAD_0,
                        reference(GETFIELD, new FieldRef(getDeobfClass(), "posZ", "D")),
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.COLORIZER_CLASS, "computeWaterColor", "(DDD)Z")),
                        IFEQ, branch("A"),

                        // particleRed = Colorizer.waterColor[0];
                        ALOAD_0,
                        reference(GETSTATIC, new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "waterColor", "[F")),
                        ICONST_0,
                        FALOAD,
                        reference(PUTFIELD, new FieldRef(getDeobfClass(), "particleRed", "F")),

                        // particleGreen = Colorizer.waterColor[1];
                        ALOAD_0,
                        reference(GETSTATIC, new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "waterColor", "[F")),
                        ICONST_1,
                        FALOAD,
                        reference(PUTFIELD, new FieldRef(getDeobfClass(), "particleGreen", "F")),

                        // particleBlue = Colorizer.waterColor[2];
                        ALOAD_0,
                        reference(GETSTATIC, new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "waterColor", "[F")),
                        ICONST_2,
                        FALOAD,
                        reference(PUTFIELD, new FieldRef(getDeobfClass(), "particleBlue", "F")),
                        GOTO, branch("B"),

                        // } else {
                        label("A"),

                        newColors == null ? new byte[]{} : buildCode(
                            // particleRed = r;
                            ALOAD_0,
                            push(newColors[0]),
                            reference(PUTFIELD, new FieldRef(getDeobfClass(), "particleRed", "F")),

                            // particleGreen = g;
                            ALOAD_0,
                            push(newColors[1]),
                            reference(PUTFIELD, new FieldRef(getDeobfClass(), "particleGreen", "F")),

                            // particleBlue = b;
                            ALOAD_0,
                            push(newColors[2]),
                            reference(PUTFIELD, new FieldRef(getDeobfClass(), "particleBlue", "F"))
                        ),

                        // }
                        label("B"),
                        (origColors == null ? new byte[]{(byte) RETURN} : new byte[]{})
                    );
                }
            }.matchConstructorOnly(true));
        }
    }

    private class EntityRainFXMod extends WaterFXMod {
        EntityRainFXMod() {
            setParentClass("EntityFX");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // 0.2f * (float) Math.random() + 0.1f
                        reference(INVOKESTATIC, new MethodRef("java/lang/Math", "random", "()D")),
                        D2F,
                        push(0.2f),
                        FMUL,
                        push(0.1f),
                        FADD,
                        F2D
                    );
                }
            }.matchConstructorOnly(true));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // 19 + rand.nextInt(4)
                        push(19),
                        ALOAD_0,
                        anyReference(GETFIELD),
                        ICONST_4,
                        reference(INVOKEVIRTUAL, new MethodRef("java/util/Random", "nextInt", "(I)I")),
                        IADD
                    );
                }
            }.matchConstructorOnly(true));

            addWaterColorPatch("rain drop", new float[]{1.0f, 1.0f, 1.0f}, new float[]{0.2f, 0.3f, 1.0f});
        }
    }

    private class EntityDropParticleFXMod extends WaterFXMod {
        EntityDropParticleFXMod() {
            setParentClass("EntityFX");

            MethodRef onUpdate = new MethodRef(getDeobfClass(), "onUpdate", "()V");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // particleRed = 0.2f;
                        ALOAD_0,
                        push(0.2f),
                        anyReference(PUTFIELD),

                        // particleGreen = 0.3f;
                        ALOAD_0,
                        push(0.3f),
                        anyReference(PUTFIELD),

                        // particleBlue = 1.0f;
                        ALOAD_0,
                        push(1.0f),
                        anyReference(PUTFIELD),

                        // ...
                        any(0, 30),

                        // 40 - age
                        push(40),
                        ALOAD_0,
                        captureReference(GETFIELD),
                        ISUB
                    );
                }
            }
                .setMethod(onUpdate)
                .addXref(1, new FieldRef(getDeobfClass(), "timer", "I"))
            );

            addWaterColorPatch("water drop", new float[]{0.0f, 0.0f, 1.0f}, new float[]{0.2f, 0.3f, 1.0f});

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "remove water drop color update";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // particleRed = 0.2f;
                        ALOAD_0,
                        push(0.2f),
                        reference(PUTFIELD, new FieldRef(getDeobfClass(), "particleRed", "F")),

                        // particleGreen = 0.3f;
                        ALOAD_0,
                        push(0.3f),
                        reference(PUTFIELD, new FieldRef(getDeobfClass(), "particleGreen", "F")),

                        // particleBlue = 1.0f;
                        ALOAD_0,
                        push(1.0f),
                        reference(PUTFIELD, new FieldRef(getDeobfClass(), "particleBlue", "F"))
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode();
                }
            }.targetMethod(onUpdate));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override lava drop color";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // particleRed = 1.0f;
                        ALOAD_0,
                        push(1.0f),
                        reference(PUTFIELD, new FieldRef(getDeobfClass(), "particleRed", "F")),

                        // particleGreen = 16.0f / (float)((40 - timer) + 16);
                        ALOAD_0,
                        push(16.0f),
                        any(0, 20),
                        reference(PUTFIELD, new FieldRef(getDeobfClass(), "particleGreen", "F")),

                        // particleBlue = 4.0f / (float)((40 - timer) + 8);
                        ALOAD_0,
                        push(4.0f),
                        any(0, 20),
                        reference(PUTFIELD, new FieldRef(getDeobfClass(), "particleBlue", "F"))
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        // if (Colorizer.computeLavaDropColor(40 - timer)) {
                        push(40),
                        ALOAD_0,
                        reference(GETFIELD, new FieldRef(getDeobfClass(), "timer", "I")),
                        ISUB,
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.COLORIZER_CLASS, "computeLavaDropColor", "(I)Z")),
                        IFEQ, branch("A"),

                        // particleRed = Colorizer.setColor[0];
                        ALOAD_0,
                        reference(GETSTATIC, new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "setColor", "[F")),
                        ICONST_0,
                        FALOAD,
                        reference(PUTFIELD, new FieldRef(getDeobfClass(), "particleRed", "F")),

                        // particleGreen = Colorizer.setColor[1];
                        ALOAD_0,
                        reference(GETSTATIC, new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "setColor", "[F")),
                        ICONST_1,
                        FALOAD,
                        reference(PUTFIELD, new FieldRef(getDeobfClass(), "particleGreen", "F")),

                        // particleBlue = Colorizer.setColor[2];
                        ALOAD_0,
                        reference(GETSTATIC, new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "setColor", "[F")),
                        ICONST_2,
                        FALOAD,
                        reference(PUTFIELD, new FieldRef(getDeobfClass(), "particleBlue", "F")),

                        // } else {
                        GOTO, branch("B"),

                        // ... original code ...
                        label("A"),
                        getMatch(),

                        // }
                        label("B")
                    );
                }
            }.targetMethod(onUpdate));
        }
    }

    private class EntitySplashFXMod extends WaterFXMod {
        EntitySplashFXMod() {
            setParentClass("EntityRainFX");

            addClassSignature(new ConstSignature(0.04f));
            addClassSignature(new ConstSignature(0.10000000000000001));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        ALOAD_0,
                        anyReference(INVOKEVIRTUAL),
                        ICONST_1,
                        IADD,
                        anyReference(INVOKEVIRTUAL)
                    );
                }
            }.matchConstructorOnly(true));

            addWaterColorPatch("splash", null);
        }
    }

    private class EntityBubbleFXMod extends WaterFXMod {
        EntityBubbleFXMod() {
            setParentClass("EntityFX");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // setParticleTextureIndex(32);
                        ALOAD_0,
                        push(32),
                        anyReference(INVOKEVIRTUAL),

                        // setSize(0.02F, 0.02F);
                        ALOAD_0,
                        push(0.02f),
                        push(0.02f),
                        anyReference(INVOKEVIRTUAL)
                    );
                }
            }.matchConstructorOnly(true));

            addWaterColorPatch("bubble", new float[]{1.0f, 1.0f, 1.0f});
        }
    }

    private class EntitySuspendFXMod extends ClassMod {
        EntitySuspendFXMod() {
            setParentClass("EntityFX");

            addClassSignature(new ConstSignature(0.4f));
            addClassSignature(new ConstSignature(0.7f));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        push(0.01f),
                        push(0.01f),
                        anyReference(INVOKEVIRTUAL)
                    );
                }
            }.matchConstructorOnly(true));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override underwater suspend particle color";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        push(0.4f),
                        reference(PUTFIELD, new FieldRef(getDeobfClass(), "particleRed", "F")),

                        ALOAD_0,
                        push(0.4f),
                        reference(PUTFIELD, new FieldRef(getDeobfClass(), "particleGreen", "F")),

                        ALOAD_0,
                        push(0.7f),
                        reference(PUTFIELD, new FieldRef(getDeobfClass(), "particleBlue", "F"))
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        push(0x6666b2),
                        reference(GETSTATIC, new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "COLOR_MAP_UNDERWATER", "I")),
                        DLOAD_2,
                        D2I,
                        DLOAD, 4,
                        D2I,
                        DLOAD, 6,
                        D2I,
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.COLORIZER_CLASS, "colorizeBiome", "(IIIII)I")),
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.COLORIZER_CLASS, "setColorF", "(I)V")),

                        ALOAD_0,
                        reference(GETSTATIC, new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "setColor", "[F")),
                        ICONST_0,
                        FALOAD,
                        reference(PUTFIELD, new FieldRef(getDeobfClass(), "particleRed", "F")),

                        ALOAD_0,
                        reference(GETSTATIC, new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "setColor", "[F")),
                        ICONST_1,
                        FALOAD,
                        reference(PUTFIELD, new FieldRef(getDeobfClass(), "particleGreen", "F")),

                        ALOAD_0,
                        reference(GETSTATIC, new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "setColor", "[F")),
                        ICONST_2,
                        FALOAD,
                        reference(PUTFIELD, new FieldRef(getDeobfClass(), "particleBlue", "F"))
                    );
                }
            });
        }
    }

    private class EntityPortalFXMod extends ClassMod {
        EntityPortalFXMod() {
            setParentClass("EntityFX");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // particleGreen *= 0.3f;
                        ALOAD_0,
                        DUP,
                        GETFIELD, capture(any(2)),
                        push(0.3f),
                        FMUL,
                        PUTFIELD, backReference(1),

                        // particleBlue *= 0.9f;
                        ALOAD_0,
                        DUP,
                        GETFIELD, capture(any(2)),
                        push(0.9f),
                        FMUL,
                        PUTFIELD, backReference(2)
                    );
                }
            }.matchConstructorOnly(true));

            addPortalPatch(0.9f, 0, "red");
            addPortalPatch(0.3f, 1, "green");

            addPatch(new BytecodePatch.InsertBefore() {
                @Override
                public String getDescription() {
                    return "override portal particle color (blue)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        RETURN
                    );
                }

                @Override
                public byte[] getInsertBytes() throws IOException {
                    return buildCode(
                        ALOAD_0,
                        reference(GETSTATIC, new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "portalColor", "[F")),
                        ICONST_2,
                        FALOAD,
                        reference(PUTFIELD, new FieldRef(getDeobfClass(), "particleBlue", "F"))
                    );
                }
            }.matchConstructorOnly(true));
        }

        private void addPortalPatch(final float origValue, final int index, final String color) {
            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override portal particle color (" + color + ")";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(origValue)
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        reference(GETSTATIC, new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "portalColor", "[F")),
                        push(index),
                        FALOAD
                    );
                }
            }.matchConstructorOnly(true));
        }
    }

    private class EntityAuraFXMod extends ClassMod {
        EntityAuraFXMod() {
            setParentClass("EntityFX");

            addClassSignature(new ConstSignature(0.019999999552965164));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        push(0.02f),
                        push(0.02f),
                        anyReference(INVOKEVIRTUAL)
                    );
                }
            }.matchConstructorOnly(true));

            addPatch(new AddMethodPatch(new MethodRef(getDeobfClass(), "colorize", "()LEntityAuraFX;")) {
                @Override
                public byte[] generateMethod() throws BadBytecode, IOException {
                    return buildCode(
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.COLORIZER_CLASS, "computeMyceliumParticleColor", "()Z")),
                        IFEQ, branch("A"),

                        ALOAD_0,
                        reference(GETSTATIC, new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "setColor", "[F")),
                        ICONST_0,
                        FALOAD,
                        reference(PUTFIELD, new FieldRef(getDeobfClass(), "particleRed", "F")),

                        ALOAD_0,
                        reference(GETSTATIC, new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "setColor", "[F")),
                        ICONST_1,
                        FALOAD,
                        reference(PUTFIELD, new FieldRef(getDeobfClass(), "particleGreen", "F")),

                        ALOAD_0,
                        reference(GETSTATIC, new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "setColor", "[F")),
                        ICONST_2,
                        FALOAD,
                        reference(PUTFIELD, new FieldRef(getDeobfClass(), "particleBlue", "F")),

                        label("A"),
                        ALOAD_0,
                        ARETURN
                    );
                }
            });
        }
    }

    private class BlockLilyPadMod extends ClassMod {
        private static final int MAGIC = 0x208030;

        BlockLilyPadMod() {
            addClassSignature(new ConstSignature(MAGIC));
            addClassSignature(new ConstSignature(0.5f));
            addClassSignature(new ConstSignature(0.015625f));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override lily pad color";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(MAGIC)
                    );
                }

                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.COLORIZER_CLASS, "getLilyPadColor", "()I"))
                    );
                }
            });
        }
    }

    private class EntityLivingMod extends ClassMod {
        EntityLivingMod() {
            setParentClass("Entity");

            addClassSignature(new ConstSignature("/mob/char.png"));
            addClassSignature(new ConstSignature("bubble"));
            addClassSignature(new ConstSignature("explode"));
        }
    }

    private class EntityRendererMod extends ClassMod {
        EntityRendererMod() {
            final boolean updateLightmapTakesFloat = getMinecraftVersion().compareTo("12w32a") >= 0;
            final MethodRef updateLightmap;
            final int updateLightmapOffset;
            if (updateLightmapTakesFloat) {
                updateLightmap = new MethodRef(getDeobfClass(), "updateLightmap", "(F)V");
                updateLightmapOffset = 1;
            } else {
                updateLightmap = new MethodRef(getDeobfClass(), "updateLightmap", "()V");
                updateLightmapOffset = 0;
            }
            final FieldRef mc = new FieldRef(getDeobfClass(), "mc", "LMinecraft;");
            final MethodRef updateFogColor = new MethodRef(getDeobfClass(), "updateFogColor", "(F)V");
            final FieldRef fogColorRed = new FieldRef(getDeobfClass(), "fogColorRed", "F");
            final FieldRef fogColorGreen = new FieldRef(getDeobfClass(), "fogColorGreen", "F");
            final FieldRef fogColorBlue = new FieldRef(getDeobfClass(), "fogColorBlue", "F");
            final FieldRef thePlayer = new FieldRef("Minecraft", "thePlayer", "LEntityClientPlayerMP;");
            final FieldRef nightVision = new FieldRef("Potion", "nightVision", "LPotion;");
            final MethodRef isPotionActive = new MethodRef("EntityClientPlayerMP", "isPotionActive", "(LPotion;)Z");
            final MethodRef getNightVisionStrength1 = new MethodRef(getDeobfClass(), "getNightVisionStrength1", "(LEntityPlayer;F)F");
            final MethodRef getNightVisionStrength = new MethodRef(getDeobfClass(), "getNightVisionStrength", "(F)F");

            addClassSignature(new ConstSignature("ambient.weather.rain"));
            addClassSignature(new ConstSignature("/terrain.png"));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // sun = world.func_35464_b(1.0F) * 0.95F + 0.05F;
                        registerLoadStore(ALOAD, 1 + updateLightmapOffset),
                        FCONST_1,
                        captureReference(INVOKEVIRTUAL),
                        push(0.95f),
                        FMUL,
                        push(0.05f),
                        FADD,
                        registerLoadStore(FSTORE, 3 + updateLightmapOffset),

                        // lightsun = world.worldProvider.lightBrightnessTable[i / 16] * sun;
                        registerLoadStore(ALOAD, 1 + updateLightmapOffset),
                        captureReference(GETFIELD),
                        captureReference(GETFIELD),
                        registerLoadStore(ILOAD, 2 + updateLightmapOffset),
                        BIPUSH, 16,
                        IDIV,
                        FALOAD,
                        registerLoadStore(FLOAD, 3 + updateLightmapOffset),
                        FMUL,
                        registerLoadStore(FSTORE, 4 + updateLightmapOffset),

                        // lighttorch = world.worldProvider.lightBrightnessTable[i % 16] * (torchFlickerX * 0.1F + 1.5F);
                        any(0, 20),
                        registerLoadStore(ILOAD, 2 + updateLightmapOffset),
                        BIPUSH, 16,
                        IREM,
                        FALOAD,
                        ALOAD_0,
                        captureReference(GETFIELD),

                        // ...
                        any(0, 200),

                        // if (world.lightningFlash > 0)
                        registerLoadStore(ALOAD, 1 + updateLightmapOffset),
                        captureReference(GETFIELD),
                        IFLE, any(2),

                        // ...
                        any(0, 300),

                        // if (world.worldProvider.worldType == 1) {
                        registerLoadStore(ALOAD, 1 + updateLightmapOffset),
                        backReference(2),
                        captureReference(GETFIELD),
                        ICONST_1,
                        IF_ICMPNE, any(2),

                        // ...
                        any(0, 200),

                        // gamma = mc.gameSettings.gammaSetting;
                        ALOAD_0,
                        captureReference(GETFIELD),
                        captureReference(GETFIELD),
                        captureReference(GETFIELD),
                        registerLoadStore(FSTORE, 15 + updateLightmapOffset),

                        // ...
                        any(0, 300),

                        // mc.renderEngine.createTextureFromBytes(lightmapColors, 16, 16, lightmapTexture);
                        ALOAD_0,
                        backReference(7),
                        captureReference(GETFIELD),
                        ALOAD_0,
                        captureReference(GETFIELD),
                        BIPUSH, 16,
                        BIPUSH, 16,
                        ALOAD_0,
                        captureReference(GETFIELD),
                        captureReference(INVOKEVIRTUAL),
                        RETURN
                    );
                }
            }
                .setMethod(updateLightmap)
                .addXref(1, new MethodRef("World", "getSunAngle", "(F)F"))
                .addXref(2, new FieldRef("World", "worldProvider", "LWorldProvider;"))
                .addXref(3, new FieldRef("WorldProvider", "lightBrightnessTable", "[F"))
                .addXref(4, new FieldRef(getDeobfClass(), "torchFlickerX", "F"))
                .addXref(5, new FieldRef("World", "lightningFlash", "I"))
                .addXref(6, new FieldRef("WorldProvider", "worldType", "I"))
                .addXref(7, mc)
                .addXref(8, new FieldRef("Minecraft", "gameSettings", "LGameSettings;"))
                .addXref(9, new FieldRef("GameSettings", "gammaSetting", "F"))
                .addXref(10, new FieldRef("Minecraft", "renderEngine", "LRenderEngine;"))
                .addXref(11, new FieldRef(getDeobfClass(), "lightmapColors", "[I"))
                .addXref(12, new FieldRef(getDeobfClass(), "lightmapTexture", "I"))
                .addXref(13, new MethodRef("RenderEngine", "createTextureFromBytes", "([IIII)V"))
            );

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // fogColorRed = 0.02f;
                        ALOAD_0,
                        push(0.02f),
                        captureReference(PUTFIELD),

                        // fogColorGreen = 0.02f;
                        ALOAD_0,
                        push(0.02f),
                        captureReference(PUTFIELD),

                        // fogColorBlue = 0.2f;
                        ALOAD_0,
                        push(0.2f),
                        captureReference(PUTFIELD)
                    );
                }
            }
                .setMethod(updateFogColor)
                .addXref(1, fogColorRed)
                .addXref(2, fogColorGreen)
                .addXref(3, fogColorBlue)
            );

            if (haveNightVision) {
                addClassSignature(new BytecodeSignature() {
                    @Override
                    public String getMatchExpression() {
                        return buildExpression(
                            // if (mc.thePlayer.isPotionActive(Potion.nightVision)) {
                            capture(build(
                                ALOAD_0,
                                captureReference(GETFIELD),
                                captureReference(GETFIELD),
                                captureReference(GETSTATIC),
                                captureReference(INVOKEVIRTUAL)
                            )),
                            IFEQ, any(2),

                            // var16 = getNightVisionStrength1(mc.thePlayer, var1);
                            capture(build(
                                ALOAD_0,
                                ALOAD_0,
                                backReference(2),
                                backReference(3),
                                FLOAD_1,
                                captureReference(INVOKESPECIAL)
                            )),
                            FSTORE, any()
                        );
                    }
                }
                    .setMethod(updateLightmap)
                    .addXref(2, mc)
                    .addXref(3, thePlayer)
                    .addXref(4, nightVision)
                    .addXref(5, isPotionActive)
                    .addXref(7, getNightVisionStrength1)
                );
            }

            addPatch(new AddMethodPatch(getNightVisionStrength) {
                @Override
                public byte[] generateMethod() throws BadBytecode, IOException {
                    if (haveNightVision) {
                        return buildCode(
                            ALOAD_0,
                            reference(GETFIELD, mc),
                            reference(GETFIELD, thePlayer),
                            reference(GETSTATIC, nightVision),
                            reference(INVOKEVIRTUAL, isPotionActive),
                            IFEQ, branch("A"),

                            ALOAD_0,
                            ALOAD_0,
                            reference(GETFIELD, mc),
                            reference(GETFIELD, thePlayer),
                            FLOAD_1,
                            reference(INVOKESPECIAL, getNightVisionStrength1),
                            FRETURN,

                            label("A"),
                            push(0.0f),
                            FRETURN
                        );
                    } else {
                        return buildCode(
                            push(0.0f),
                            FRETURN
                        );
                    }
                }
            });

            addPatch(new MakeMemberPublicPatch(new FieldRef(getDeobfClass(), "torchFlickerX", "F")));

            addPatch(new BytecodePatch.InsertAfter() {
                @Override
                public String getDescription() {
                    return "override lightmap";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        registerLoadStore(ASTORE, 1 + updateLightmapOffset)
                    );
                }

                @Override
                public byte[] getInsertBytes() throws IOException {
                    return buildCode(
                        // if (Lightmap.computeLightmap(this, world, partialTick)) {
                        ALOAD_0,
                        registerLoadStore(ALOAD, 1 + updateLightmapOffset),
                        updateLightmapTakesFloat ? buildCode(FLOAD_1) : push(0.0f),
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.LIGHTMAP_CLASS, "computeLightmap", "(LEntityRenderer;LWorld;F)Z")),
                        IFEQ, branch("A"),

                        // return;
                        RETURN,

                        // }
                        label("A")
                    );
                }
            }.targetMethod(updateLightmap));

            addPatch(new BytecodePatch.InsertBefore() {
                @Override
                public String getDescription() {
                    return "override fog color";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // f1 = 1.0f - Math.pow(f1, 0.25);
                        push(1.0f),
                        FLOAD, capture(any()),
                        F2D,
                        push(0.25),
                        reference(INVOKESTATIC, new MethodRef("java/lang/Math", "pow", "(DD)D")),
                        D2F,
                        FSUB,
                        FSTORE, backReference(1),

                        // ...
                        any(0, 100),

                        // fogColorBlue = vec3d1.zCoord;
                        ALOAD_0,
                        anyALOAD,
                        anyReference(GETFIELD),
                        D2F,
                        reference(PUTFIELD, fogColorBlue)
                    );
                }

                @Override
                public byte[] getInsertBytes() throws IOException {
                    return buildCode(
                        // Colorizer.setupForFog(entityliving);
                        ALOAD_3,
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.COLORIZER_CLASS, "setupForFog", "(LEntity;)V")),

                        // if (Colorizer.computeFogColor(world, f)) {
                        ALOAD_2,
                        FLOAD_1,
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.COLORIZER_CLASS, "computeFogColor", "(LWorld;F)Z")),
                        IFEQ, branch("A"),

                        // fogColorRed = Colorizer.setColor[0];
                        ALOAD_0,
                        reference(GETSTATIC, new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "setColor", "[F")),
                        ICONST_0,
                        FALOAD,
                        reference(PUTFIELD, fogColorRed),

                        // fogColorGreen = Colorizer.setColor[1];
                        ALOAD_0,
                        reference(GETSTATIC, new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "setColor", "[F")),
                        ICONST_1,
                        FALOAD,
                        reference(PUTFIELD, fogColorGreen),

                        // fogColorBlue = Colorizer.setColor[2];
                        ALOAD_0,
                        reference(GETSTATIC, new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "setColor", "[F")),
                        ICONST_2,
                        FALOAD,
                        reference(PUTFIELD, fogColorBlue),

                        // }
                        label("A")
                    );
                }
            }.targetMethod(updateFogColor));

            addPatch(new BytecodePatch.InsertAfter() {
                @Override
                public String getDescription() {
                    return "override underwater ambient color";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // fogColorRed = 0.02f;
                        ALOAD_0,
                        push(0.02f),
                        reference(PUTFIELD, fogColorRed),

                        // fogColorGreen = 0.02f;
                        ALOAD_0,
                        push(0.02f),
                        reference(PUTFIELD, fogColorGreen),

                        // fogColorBlue = 0.2f;
                        ALOAD_0,
                        push(0.2f),
                        reference(PUTFIELD, fogColorBlue)
                    );
                }

                @Override
                public byte[] getInsertBytes() throws IOException {
                    return buildCode(
                        // if (Colorizer.computeFogColor(Colorizer.COLOR_MAP_UNDERWATER)) {
                        reference(GETSTATIC, new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "COLOR_MAP_UNDERWATER", "I")),
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.COLORIZER_CLASS, "computeFogColor", "(I)Z")),
                        IFEQ, branch("A"),

                        // fogColorRed = Colorizer.setColor[0];
                        ALOAD_0,
                        reference(GETSTATIC, new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "setColor", "[F")),
                        ICONST_0,
                        FALOAD,
                        reference(PUTFIELD, fogColorRed),

                        // fogColorGreen = Colorizer.setColor[1];
                        ALOAD_0,
                        reference(GETSTATIC, new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "setColor", "[F")),
                        ICONST_1,
                        FALOAD,
                        reference(PUTFIELD, fogColorGreen),

                        // fogColorBlue = Colorizer.setColor[2];
                        ALOAD_0,
                        reference(GETSTATIC, new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "setColor", "[F")),
                        ICONST_2,
                        FALOAD,
                        reference(PUTFIELD, fogColorBlue),

                        // }
                        label("A")
                    );
                }
            }.targetMethod(updateFogColor));
        }
    }

    private abstract class RedstoneWireClassMod extends ClassMod {
        RedstoneWireClassMod(final String description, final MethodRef method) {
            addClassSignature(new BytecodeSignature() {
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

            addPatch(new BytecodePatch() {
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
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        ILOAD, getCaptureGroup(1),
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.COLORIZER_CLASS, "computeRedstoneWireColor", "(I)Z")),
                        IFEQ, branch("A"),

                        reference(GETSTATIC, new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "setColor", "[F")),
                        ICONST_0,
                        FALOAD,
                        FSTORE, getCaptureGroup(3),
                        reference(GETSTATIC, new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "setColor", "[F")),
                        ICONST_1,
                        FALOAD,
                        FSTORE, getCaptureGroup(4),
                        reference(GETSTATIC, new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "setColor", "[F")),
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
    }

    private class BlockRedstoneWireMod extends RedstoneWireClassMod {
        BlockRedstoneWireMod() {
            super("override redstone wire particle color", new MethodRef("BlockRedstoneWire", "randomDisplayTick", "(LWorld;IIILjava/util/Random;)V"));

            setParentClass("Block");

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
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        ALOAD_1,
                        ILOAD_2,
                        ILOAD_3,
                        ILOAD, 4,
                        push(0x800000),
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.COLORIZER_CLASS, "colorizeRedstoneWire", "(LIBlockAccess;IIII)I"))
                    );
                }
            }.targetMethod(new MethodRef(getDeobfClass(), "colorMultiplier", "(LIBlockAccess;III)I")));
        }
    }

    private class RenderBlocksMod extends RedstoneWireClassMod {
        RenderBlocksMod() {
            super("override redstone wire color", new MethodRef("RenderBlocks", "renderBlockRedstoneWire", "(LBlock;III)Z"));

            addClassSignature(new ConstSignature(0.1875));
            addClassSignature(new ConstSignature(0.01));

            final MethodRef renderBlockFallingSand = new MethodRef(getDeobfClass(), "renderBlockFallingSand", "(LBlock;LWorld;III" + (renderBlockFallingSandTakes4Ints ? "I" : "") + ")V");
            final int renderBlockFallingSandOffset = renderBlockFallingSandTakes4Ints ? 1 : 0;
            final MethodRef setColorOpaque_F = new MethodRef("Tessellator", "setColorOpaque_F", "(FFF)V");
            final MethodRef renderBlockFluids = new MethodRef(getDeobfClass(), "renderBlockFluids", "(LBlock;III)Z");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_1,
                        ALOAD_0,
                        captureReference(GETFIELD),
                        ILOAD_2,
                        ILOAD_3,
                        ILOAD, 4,
                        captureReference(INVOKEVIRTUAL),
                        ISTORE, 6
                    );
                }
            }
                .setMethod(renderBlockFluids)
                .addXref(1, new FieldRef(getDeobfClass(), "blockAccess", "LIBlockAccess;"))
                .addXref(2, new MethodRef("Block", "colorMultiplier", "(LIBlockAccess;III)I"))
            );

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD, 5,
                        FLOAD, 6,
                        FLOAD, 8,
                        FMUL,
                        FLOAD, 6,
                        FLOAD, 9,
                        FMUL,
                        FLOAD, 6,
                        FLOAD, 10,
                        FMUL,
                        captureReference(INVOKEVIRTUAL)
                    );
                }
            }.addXref(1, setColorOpaque_F));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        begin(),
                        push(0.5f),
                        FSTORE, 6 + renderBlockFallingSandOffset,
                        FCONST_1,
                        FSTORE, 7 + renderBlockFallingSandOffset,
                        push(0.8f),
                        FSTORE, 8 + renderBlockFallingSandOffset,
                        push(0.6f),
                        FSTORE, 9 + renderBlockFallingSandOffset
                    );
                }
            }.setMethod(renderBlockFallingSand));

            addMemberMapper(new MethodMapper(new MethodRef(getDeobfClass(), "renderBlockCauldron", "(LBlockCauldron;III)Z")));

            addPatch(new BytecodePatch.InsertAfter() {
                @Override
                public String getDescription() {
                    return "colorize cauldron water color";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(205),
                        ISTORE, any()
                    );
                }

                @Override
                public byte[] getInsertBytes() throws IOException {
                    return buildCode(
                        // Colorizer.computeWaterColor();
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.COLORIZER_CLASS, "computeWaterColor", "()V")),

                        // tessellator.setColorOpaque(Colorizer.waterColor[0], Colorizer.waterColor[1], Colorizer.waterColor[2]);
                        ALOAD, 5,
                        reference(GETSTATIC, new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "waterColor", "[F")),
                        ICONST_0,
                        FALOAD,
                        reference(GETSTATIC, new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "waterColor", "[F")),
                        ICONST_1,
                        FALOAD,
                        reference(GETSTATIC, new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "waterColor", "[F")),
                        ICONST_2,
                        FALOAD,
                        reference(INVOKEVIRTUAL, setColorOpaque_F)
                    );
                }
            }.targetMethod(new MethodRef(getDeobfClass(), "renderBlockCauldron", "(LBlockCauldron;III)Z")));

            addPatch(new BytecodePatch() {
                private boolean done;

                @Override
                public String getDescription() {
                    return "colorize falling sand and gravel";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // tessellator.setColorOpaque_F($1 * f5, $1 * f5, $1 * f5);
                        ALOAD, 10 + renderBlockFallingSandOffset,
                        capture(anyFLOAD),
                        FLOAD, 12 + renderBlockFallingSandOffset,
                        FMUL,
                        backReference(1),
                        FLOAD, 12 + renderBlockFallingSandOffset,
                        FMUL,
                        backReference(1),
                        FLOAD, 12 + renderBlockFallingSandOffset,
                        FMUL,
                        reference(INVOKEVIRTUAL, setColorOpaque_F)
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    byte[] extraCode;
                    if (done) {
                        extraCode = new byte[0];
                    } else {
                        done = true;
                        extraCode = buildCode(
                            // setColorF(Colorizer.colorizeBlock(block, i, j, k, 0));
                            ALOAD_1,
                            ILOAD_3,
                            ILOAD, 4,
                            ILOAD, 5,
                            ICONST_0,
                            reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.COLORIZER_CLASS, "colorizeBlock", "(LBlock;IIII)I")),
                            reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.COLORIZER_CLASS, "setColorF", "(I)V"))
                        );
                    }
                    return buildCode(
                        extraCode,

                        // tessellator.setColorOpaque_F(Colorizer.setColor[0] * f5, Colorizer.setColor[1] * f5, Colorizer.setColor[2] * f5);
                        ALOAD, 10 + renderBlockFallingSandOffset,
                        reference(GETSTATIC, new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "setColor", "[F")),
                        ICONST_0,
                        FALOAD,
                        FLOAD, 12 + renderBlockFallingSandOffset,
                        FMUL,
                        reference(GETSTATIC, new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "setColor", "[F")),
                        ICONST_1,
                        FALOAD,
                        FLOAD, 12 + renderBlockFallingSandOffset,
                        FMUL,
                        reference(GETSTATIC, new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "setColor", "[F")),
                        ICONST_2,
                        FALOAD,
                        FLOAD, 12 + renderBlockFallingSandOffset,
                        FMUL,
                        reference(INVOKEVIRTUAL, setColorOpaque_F)
                    );
                }
            }.targetMethod(renderBlockFallingSand));

            final int[] savedRegisters = new int[]{7, 8, 9}; // water shaders mod moves some local variables around

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "save water color registers";
                }

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
                        FSTORE, capture(any()),

                        // f1 = (float)(l >> 8 & 0xff) / 255F;
                        backReference(1),
                        push(8),
                        ISHR,
                        push(255),
                        IAND,
                        I2F,
                        push(255.0f),
                        FDIV,
                        FSTORE, capture(any()),

                        // f2 = (float)(l & 0xff) / 255F;
                        backReference(1),
                        push(255),
                        IAND,
                        I2F,
                        push(255.0f),
                        FDIV,
                        FSTORE, capture(any())
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    savedRegisters[0] = getCaptureGroup(2)[0] & 0xff;
                    savedRegisters[1] = getCaptureGroup(3)[0] & 0xff;
                    savedRegisters[2] = getCaptureGroup(4)[0] & 0xff;
                    Logger.log(Logger.LOG_CONST, "water color registers: %d %d %d", savedRegisters[0], savedRegisters[1], savedRegisters[2]);
                    return null;
                }
            }.targetMethod(renderBlockFluids));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "colorize bottom of water block";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // tessellator.setColorOpaque_F(f3 * f7, f3 * f7, f3 * f7);
                        ALOAD, 5,
                        FLOAD, capture(any()),
                        FLOAD, capture(any()),
                        FMUL,
                        FLOAD, backReference(1),
                        FLOAD, backReference(2),
                        FMUL,
                        FLOAD, backReference(1),
                        FLOAD, backReference(2),
                        FMUL,
                        reference(INVOKEVIRTUAL, setColorOpaque_F)
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        // tessellator.setColorOpaque_F(f3 * f7 * f, f3 * f7 * f1, f3 * f7 * f2);
                        ALOAD, 5,
                        FLOAD, getCaptureGroup(1),
                        FLOAD, getCaptureGroup(2),
                        FMUL,
                        FLOAD, savedRegisters[0],
                        FMUL,
                        FLOAD, getCaptureGroup(1),
                        FLOAD, getCaptureGroup(2),
                        FMUL,
                        FLOAD, savedRegisters[1],
                        FMUL,
                        FLOAD, getCaptureGroup(1),
                        FLOAD, getCaptureGroup(2),
                        FMUL,
                        FLOAD, savedRegisters[2],
                        FMUL,
                        reference(INVOKEVIRTUAL, setColorOpaque_F)
                    );
                }
            }.targetMethod(renderBlockFluids));
        }
    }

    private class EntityReddustFXMod extends ClassMod {
        EntityReddustFXMod() {
            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        reference(INVOKESTATIC, new MethodRef("java/lang/Math", "random", "()D")),
                        push(0.20000000298023224),
                        DMUL,
                        D2F,
                        push(0.8f),
                        FADD,
                        anyFLOAD,
                        FMUL,
                        anyFLOAD,
                        FMUL,
                        anyReference(PUTFIELD)
                    );
                }
            }.matchConstructorOnly(true));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override redstone particle color";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        FCONST_1,
                        FSTORE, 9,
                        reference(INVOKESTATIC, new MethodRef("java/lang/Math", "random", "()D"))
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        FCONST_1,
                        FSTORE, 9,

                        BIPUSH, 15,
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.COLORIZER_CLASS, "computeRedstoneWireColor", "(I)Z")),
                        IFEQ, branch("A"),

                        reference(GETSTATIC, new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "setColor", "[F")),
                        ICONST_0,
                        FALOAD,
                        FSTORE, 9,
                        reference(GETSTATIC, new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "setColor", "[F")),
                        ICONST_1,
                        FALOAD,
                        FSTORE, 10,
                        reference(GETSTATIC, new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "setColor", "[F")),
                        ICONST_2,
                        FALOAD,
                        FSTORE, 11,

                        label("A"),
                        reference(INVOKESTATIC, new MethodRef("java/lang/Math", "random", "()D"))
                    );
                }
            }.matchConstructorOnly(true));
        }
    }

    public class BlockStemMod extends ClassMod {
        BlockStemMod() {
            MethodRef getRenderColor = new MethodRef(getDeobfClass(), "getRenderColor", "(I)I");

            addClassSignature(new FixedBytecodeSignature(
                // j = i * 32;
                begin(),
                ILOAD_1,
                BIPUSH, 32,
                IMUL,
                ISTORE_2,

                // k = 255 - i * 8;
                SIPUSH, 0, 255,
                ILOAD_1,
                BIPUSH, 8,
                IMUL,
                ISUB,
                ISTORE_3,

                // l = i * 4;
                ILOAD_1,
                ICONST_4,
                IMUL,
                ISTORE, 4,

                // return j << 16 | k << 8 | l;
                ILOAD_2,
                BIPUSH, 16,
                ISHL,
                ILOAD_3,
                BIPUSH, 8,
                ISHL,
                IOR,
                ILOAD, 4,
                IOR,
                IRETURN,
                end()
            ).setMethod(getRenderColor));

            addPatch(new BytecodePatch.InsertBefore() {
                @Override
                public String getDescription() {
                    return "override pumpkin and melon stem color";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        IRETURN,
                        end()
                    );
                }

                @Override
                public byte[] getInsertBytes() throws IOException {
                    return buildCode(
                        ILOAD_1,
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.COLORIZER_CLASS, "colorizeStem", "(II)I"))
                    );
                }
            }.targetMethod(getRenderColor));
        }
    }

    private class RenderGlobalMod extends ClassMod {
        RenderGlobalMod() {
            addClassSignature(new ConstSignature("/environment/clouds.png"));

            final FieldRef mc = new FieldRef(getDeobfClass(), "mc", "LMinecraft;");
            final FieldRef gameSettings = new FieldRef("Minecraft", "gameSettings", "LGameSettings;");
            final FieldRef fancyGraphics = new FieldRef("GameSettings", "fancyGraphics", "Z");
            final MethodRef renderClouds = new MethodRef(getDeobfClass(), "renderClouds", "(F)V");
            final MethodRef renderSky = new MethodRef(getDeobfClass(), "renderSky", "(F)V");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // if (mc.gameSettings.fancyGraphics)
                        ALOAD_0,
                        captureReference(GETFIELD),
                        captureReference(GETFIELD),
                        captureReference(GETFIELD),
                        IFEQ_or_IFNE, any(2),

                        // ...
                        any(0, 100),

                        // var3 = 32;
                        // var4 = 256 / var3;
                        push(32),
                        anyISTORE,
                        push(256),
                        anyILOAD,
                        IDIV,
                        anyISTORE,

                        // ...
                        any(1, 50),

                        // GL11.glBindTexture(3553, this.i.b("/environment/clouds.png"));
                        push("/environment/clouds.png")
                    );
                }
            }
                .setMethod(renderClouds)
                .addXref(1, mc)
                .addXref(2, gameSettings)
                .addXref(3, fancyGraphics)
            );

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push("/misc/tunnel.png")
                    );
                }
            }.setMethod(renderSky));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override cloud type";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        capture(build(
                            ALOAD_0,
                            reference(GETFIELD, mc),
                            reference(GETFIELD, gameSettings),
                            reference(GETFIELD, fancyGraphics)
                        )),
                        capture(build(
                            IFEQ, any(2)
                        ))
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        getCaptureGroup(1),
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.COLORIZER_CLASS, "drawFancyClouds", "(Z)Z")),
                        getCaptureGroup(2)
                    );
                }
            }.targetMethod(renderClouds));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override end sky color";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(or(
                        build(push(0x181818)), // pre-12w23a
                        build(push(0x282828))  // 12w23a+
                    ));
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        reference(GETSTATIC, new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "endSkyColor", "I"))
                    );
                }
            }.targetMethod(renderSky));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override mycelium particle color";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(lookBehind(build(
                        // if (s.equals("townaura")) {
                        ALOAD_1,
                        push("townaura"),
                        reference(INVOKEVIRTUAL, new MethodRef("java/lang/String", "equals", "(Ljava/lang/Object;)Z")),
                        IFEQ, any(2),

                        // obj = new EntityAuraFX(worldObj, d, d1, d2, d3, d4, d5);
                        reference(NEW, new ClassRef("EntityAuraFX")),
                        DUP,
                        ALOAD_0,
                        anyReference(GETFIELD),
                        anyDLOAD,
                        anyDLOAD,
                        anyDLOAD,
                        anyDLOAD,
                        anyDLOAD,
                        anyDLOAD,
                        reference(INVOKESPECIAL, new MethodRef("EntityAuraFX", "<init>", "(LWorld;DDDDDD)V"))
                    ), true));
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        reference(INVOKEVIRTUAL, new MethodRef("EntityAuraFX", "colorize", "()LEntityAuraFX;"))
                    );
                }
            });
        }
    }

    private class EntityListMod extends ClassMod {
        EntityListMod() {
            addClassSignature(new ConstSignature("Skipping Entity with id "));

            final MethodRef addMapping = new MethodRef(getDeobfClass(), "addMapping", "(Ljava/lang/Class;Ljava/lang/String;III)V");

            addMemberMapper(new MethodMapper(addMapping).accessFlag(AccessFlag.STATIC, true));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "set up mapping for spawnable entities";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        begin()
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        ALOAD_1,
                        ILOAD_2,
                        ILOAD_3,
                        ILOAD, 4,
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.COLORIZER_CLASS, "setupSpawnerEgg", "(Ljava/lang/String;III)V"))
                    );
                }
            }.targetMethod(addMapping));
        }
    }

    private class ItemSpawnerEggMod extends ClassMod {
        ItemSpawnerEggMod() {
            final MethodRef getColorFromDamage2 = new MethodRef(getDeobfClass(), getColorFromDamage.getName(), getColorFromDamage.getType());
            final MethodRef getItemNameIS = new MethodRef(getDeobfClass(), "getItemNameIS", "(LItemStack;)Ljava/lang/String;");
            final MethodRef getItemDamage = new MethodRef("ItemStack", "getItemDamage", "()I");
            final MethodRef getEntityString = new MethodRef("EntityList", "getEntityString", "(I)Ljava/lang/String;");

            setParentClass("Item");

            addClassSignature(new ConstSignature(".name"));
            addClassSignature(new ConstSignature("entity."));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // s1 = EntityList.getEntityString(itemStack.getItemDamage());
                        ALOAD_1,
                        captureReference(INVOKEVIRTUAL),
                        captureReference(INVOKESTATIC),
                        ASTORE_3,
                        ALOAD_3
                    );
                }
            }
                .setMethod(getItemNameIS)
                .addXref(1, getItemDamage)
                .addXref(2, getEntityString)
            );

            addClassSignature(new OrSignature(
                new BytecodeSignature() {
                    @Override
                    public String getMatchExpression() {
                        return buildExpression(
                            // 64 + (i * 0x24faef & 0xc0)
                            BIPUSH, 64,
                            ILOAD_1,
                            push(0x24faef),
                            IMUL,
                            push(0xc0),
                            IAND,
                            IADD
                        );
                    }
                }.setMethod(getColorFromDamage),

                new BytecodeSignature() {
                    @Override
                    public String getMatchExpression() {
                        return buildExpression(
                            push(0xffffff),
                            IRETURN
                        );
                    }
                }.setMethod(getColorFromDamage)
            ));

            addPatch(new BytecodePatch.InsertBefore() {
                @Override
                public String getDescription() {
                    return "override spawner egg color";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        IRETURN
                    );
                }

                @Override
                public byte[] getInsertBytes() throws IOException {
                    byte[] code;
                    switch (getColorFromDamageVer) {
                        case 1:
                            code = buildCode(ILOAD_1, ICONST_0);
                            break;

                        case 2:
                            code = buildCode(ILOAD_1, ILOAD_2);
                            break;

                        default:
                            code = buildCode(ALOAD_1, reference(INVOKEVIRTUAL, getItemDamage), ILOAD_2);
                            break;
                    }
                    return buildCode(
                        code,
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.COLORIZER_CLASS, "colorizeSpawnerEgg", "(III)I"))
                    );
                }
            }.targetMethod(getColorFromDamage2));
        }
    }

    private class MapColorMod extends ClassMod {
        MapColorMod() {
            addClassSignature(new ConstSignature(0x7fb238));
            addClassSignature(new ConstSignature(0xf7e9a3));
            addClassSignature(new ConstSignature(0xa7a7a7));
            addClassSignature(new ConstSignature(0xff0000));
            addClassSignature(new ConstSignature(0xa0a0ff));

            addMemberMapper(new FieldMapper(new FieldRef(getDeobfClass(), "mapColorArray", "[LMapColor;")).accessFlag(AccessFlag.STATIC, true));
            addMemberMapper(new FieldMapper(new FieldRef(getDeobfClass(), "colorValue", "I"), new FieldRef(getDeobfClass(), "colorIndex", "I")).accessFlag(AccessFlag.STATIC, false));

            addPatch(new AddFieldPatch(new FieldRef(getDeobfClass(), "origColorValue", "I")));

            addPatch(new MakeMemberPublicPatch(new FieldRef(getDeobfClass(), "colorValue", "I")) {
                @Override
                public int getNewFlags(int oldFlags) {
                    return oldFlags & ~AccessFlag.FINAL;
                }
            });

            addPatch(new BytecodePatch.InsertAfter() {
                @Override
                public String getDescription() {
                    return "set map origColorValue";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        ILOAD_2,
                        reference(PUTFIELD, new FieldRef(getDeobfClass(), "colorValue", "I"))
                    );
                }

                @Override
                public byte[] getInsertBytes() throws IOException {
                    return buildCode(
                        ALOAD_0,
                        ILOAD_2,
                        reference(PUTFIELD, new FieldRef(getDeobfClass(), "origColorValue", "I"))
                    );
                }
            }.targetMethod(new MethodRef(getDeobfClass(), "<init>", "(II)V")));
        }
    }

    private class ItemDyeMod extends ClassMod {
        ItemDyeMod() {
            final FieldRef dyeColorNames = new FieldRef(getDeobfClass(), "dyeColorNames", "[Ljava/lang/String;");
            final FieldRef dyeColors = new FieldRef(getDeobfClass(), "dyeColors", "[I");

            setParentClass("Item");

            addClassSignature(new ConstSignature("black"));
            addClassSignature(new ConstSignature("purple"));
            addClassSignature(new ConstSignature("cyan"));

            addMemberMapper(new FieldMapper(dyeColorNames)
                .accessFlag(AccessFlag.PUBLIC, true)
                .accessFlag(AccessFlag.STATIC, true)
            );
            addMemberMapper(new FieldMapper(dyeColors)
                .accessFlag(AccessFlag.PUBLIC, true)
                .accessFlag(AccessFlag.STATIC, true)
            );
        }
    }

    private class ItemArmorMod extends ClassMod {
        private final int DEFAULT_LEATHER_COLOR = 0xa06540;

        ItemArmorMod() {
            addClassSignature(new ConstSignature("display"));
            addClassSignature(new ConstSignature("color"));
            addClassSignature(new ConstSignature(DEFAULT_LEATHER_COLOR));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override default leather armor color";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(DEFAULT_LEATHER_COLOR)
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        reference(GETSTATIC, new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "undyedLeatherColor", "I"))
                    );
                }
            });
        }
    }

    private class EntitySheepMod extends ClassMod {
        EntitySheepMod() {
            addClassSignature(new ConstSignature("/mob/sheep.png"));
            addClassSignature(new OrSignature(
                new ConstSignature("mob.sheep"),
                new ConstSignature("mob.sheep.say") // 12w38a+
            ));

            addMemberMapper(new FieldMapper(fleeceColorTable)
                .accessFlag(AccessFlag.PUBLIC, true)
                .accessFlag(AccessFlag.STATIC, true)
            );
        }
    }

    private class RenderWolfMod extends ClassMod {
        RenderWolfMod() {
            final MethodRef glColor3f = new MethodRef(MCPatcherUtils.GL11_CLASS, "glColor3f", "(FFF)V");
            final FieldRef collarColors = new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "collarColors", "[[F");

            setParentClass("RenderLiving");

            addClassSignature(new ConstSignature("/mob/wolf_collar.png"));
            addClassSignature(new ConstSignature(glColor3f));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override wolf collar colors";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        reference(GETSTATIC, fleeceColorTable)
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        reference(GETSTATIC, collarColors)
                    );
                }
            });
        }
    }

    private class RecipesDyedArmorMod extends ClassMod {
        RecipesDyedArmorMod() {
            final FieldRef armorColors = new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "armorColors", "[[F");

            addClassSignature(new ConstSignature(255.0f));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // var7 = (int)((float)var7 * var10 / var11);
                        ILOAD, capture(any()),
                        I2F,
                        FLOAD, capture(any()),
                        FMUL,
                        FLOAD, capture(any()),
                        FDIV,
                        F2I,
                        ISTORE, backReference(1),

                        // var8 = (int)((float)var8 * var10 / var11);
                        ILOAD, capture(any()),
                        I2F,
                        FLOAD, backReference(2),
                        FMUL,
                        FLOAD, backReference(3),
                        FDIV,
                        F2I,
                        ISTORE, backReference(4),

                        // var9 = (int)((float)var9 * var10 / var11);
                        ILOAD, capture(any()),
                        I2F,
                        FLOAD, backReference(2),
                        FMUL,
                        FLOAD, backReference(3),
                        FDIV,
                        F2I,
                        ISTORE, backReference(5)
                    );
                }
            });

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override armor dye colors";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        reference(GETSTATIC, fleeceColorTable)
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        reference(GETSTATIC, armorColors)
                    );
                }
            });
        }
    }

    private class FontRendererMod extends BaseMod.FontRendererMod {
        FontRendererMod() {
            final MethodRef renderString = new MethodRef(getDeobfClass(), "renderString", "(Ljava/lang/String;IIIZ)" + (renderStringReturnsInt ? "I" : "V"));
            final MethodRef glColor4f = new MethodRef(MCPatcherUtils.GL11_CLASS, "glColor4f", "(FFFF)V");
            final FieldRef colorCode = new FieldRef(getDeobfClass(), "colorCode", "[I");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        push(32),
                        NEWARRAY, T_INT,
                        captureReference(PUTFIELD)
                    );
                }
            }
                .matchConstructorOnly(true)
                .addXref(1, colorCode)
            );

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(0xff000000),
                        any(0, 100),
                        reference(INVOKESTATIC, glColor4f)
                    );
                }
            }.setMethod(renderString));

            addPatch(new BytecodePatch.InsertAfter() {
                @Override
                public String getDescription() {
                    return "override text color";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        ICONST_0,
                        anyReference(PUTFIELD)
                    );
                }

                @Override
                public byte[] getInsertBytes() throws IOException {
                    return buildCode(
                        ILOAD, 4,
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.COLORIZER_CLASS, "colorizeText", "(I)I")),
                        ISTORE, 4
                    );
                }
            }.targetMethod(renderString));

            addPatch(new BytecodePatch.InsertAfter() {
                @Override
                public String getDescription() {
                    return "override text color codes";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        reference(GETFIELD, colorCode),
                        capture(anyILOAD),
                        IALOAD
                    );
                }

                @Override
                public byte[] getInsertBytes() throws IOException {
                    return buildCode(
                        getCaptureGroup(1),
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.COLORIZER_CLASS, "colorizeText", "(II)I"))
                    );
                }
            });
        }
    }

    private class TileEntitySignRendererMod extends ClassMod {
        TileEntitySignRendererMod() {
            final MethodRef renderTileSignEntityAt = new MethodRef(getDeobfClass(), "renderTileSignEntityAt", "(LTileEntitySign;DDDF)V");
            final MethodRef glDepthMask = new MethodRef(MCPatcherUtils.GL11_CLASS, "glDepthMask", "(Z)V");

            addClassSignature(new ConstSignature(glDepthMask));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push("/item/sign.png")
                    );
                }
            }.setMethod(renderTileSignEntityAt));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override sign text color";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ICONST_0,
                        reference(INVOKESTATIC, glDepthMask),
                        ICONST_0,
                        capture(anyISTORE)
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        ICONST_0,
                        reference(INVOKESTATIC, glDepthMask),
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.COLORIZER_CLASS, "colorizeSignText", "()I")),
                        getCaptureGroup(1)
                    );
                }
            });
        }
    }

    private class RenderXPOrbMod extends ClassMod {
        RenderXPOrbMod() {
            final MethodRef render = new MethodRef(getDeobfClass(), "render", "(LEntityXPOrb;DDDFF)V");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push("/item/xporb.png")
                    );
                }
            }.setMethod(render));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override xp orb color";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        lookBehind(build(
                            // MathHelper.sin(f8 + 0.0F)
                            capture(anyFLOAD),
                            push(0.0f),
                            FADD,
                            anyReference(INVOKESTATIC),

                            // ...
                            any(0, 200)
                        ), true),

                        // tessellator.setColorRGBA_I(i1, 128);
                        capture(anyILOAD),
                        lookAhead(build(
                            push(128),
                            anyReference(INVOKEVIRTUAL)
                        ), true)
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        getCaptureGroup(2),
                        getCaptureGroup(1),
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.COLORIZER_CLASS, "colorizeXPOrb", "(IF)I"))
                    );
                }
            });
        }
    }
}
