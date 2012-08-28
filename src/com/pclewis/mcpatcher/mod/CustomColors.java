package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.*;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.ClassFile;
import javassist.bytecode.MethodInfo;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

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

        if (minecraftVersion.compareTo("Beta 1.9 Prerelease 4") < 0) {
            addError("Requires Minecraft Beta 1.9 or newer.");
            return;
        }

        configPanel = new ConfigPanel();

        classMods.add(new MinecraftMod(minecraftVersion));
        classMods.add(new IBlockAccessMod());
        classMods.add(new BlockMod());

        classMods.add(new BiomeGenBaseMod());
        classMods.add(new BiomeGenSwampMod());
        classMods.add(new BlockFluidMod());
        classMods.add(new BlockCauldronMod());
        classMods.add(new ItemMod());
        classMods.add(new ItemBlockMod());
        classMods.add(new ItemRendererMod());

        classMods.add(new PotionMod());
        classMods.add(new PotionHelperMod());

        classMods.add(new ColorizerFoliageMod());
        classMods.add(new BlockLeavesMod());

        classMods.add(new WorldMod());
        if (haveNewWorld) {
            classMods.add(new BaseMod.WorldServerMod(minecraftVersion));
        }
        classMods.add(new WorldProviderMod());
        classMods.add(new WorldProviderHellMod());
        classMods.add(new WorldProviderEndMod());
        classMods.add(new WorldChunkManagerMod());
        classMods.add(new EntityMod());
        classMods.add(new EntityFXMod());
        classMods.add(new EntityRainFXMod());
        classMods.add(new EntityDropParticleFXMod());
        classMods.add(new EntitySplashFXMod());
        classMods.add(new EntityBubbleFXMod());
        classMods.add(new EntitySuspendFXMod());
        classMods.add(new EntityPortalFXMod());
        classMods.add(new EntityAuraFXMod());

        classMods.add(new EntityLivingMod());
        classMods.add(new EntityRendererMod());

        classMods.add(new BlockLilyPadMod());

        classMods.add(new BlockRedstoneWireMod());
        classMods.add(new RenderBlocksMod());
        classMods.add(new EntityReddustFXMod());

        classMods.add(new RenderGlobalMod());

        classMods.add(new BlockStemMod());

        classMods.add(new MapColorMod());

        classMods.add(new ItemDyeMod());
        classMods.add(new EntitySheepMod());

        if (haveSpawnerEggs) {
            classMods.add(new EntityListMod());
            classMods.add(new ItemSpawnerEggMod());
        }

        if (haveFontColor) {
            classMods.add(new FontRendererMod());
            classMods.add(new TileEntitySignRendererMod());
        }

        classMods.add(new RenderXPOrbMod());

        filesToAdd.add(ClassMap.classNameToFilename(MCPatcherUtils.COLORIZER_CLASS));
        filesToAdd.add(ClassMap.classNameToFilename(MCPatcherUtils.COLORIZER_CLASS + "$1"));
        filesToAdd.add(ClassMap.classNameToFilename(MCPatcherUtils.COLOR_MAP_CLASS));
        filesToAdd.add(ClassMap.classNameToFilename(MCPatcherUtils.BIOME_HELPER_CLASS));
        filesToAdd.add(ClassMap.classNameToFilename(MCPatcherUtils.BIOME_HELPER_CLASS + "$Stub"));
        filesToAdd.add(ClassMap.classNameToFilename(MCPatcherUtils.BIOME_HELPER_CLASS + "$Old"));
        filesToAdd.add(ClassMap.classNameToFilename(MCPatcherUtils.BIOME_HELPER_CLASS + "$New"));
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

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(65),
                        reference(INVOKESTATIC, new MethodRef("org/lwjgl/input/Keyboard", "isKeyDown", "(I)Z")),
                        IFEQ, BinaryRegex.any(2),
                        reference(INVOKESTATIC, new MethodRef("org/lwjgl/opengl/Display", "update", "()V"))
                    );
                }
            }.setMethod(runGameLoop));

            addWorldGetter(minecraftVersion);

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "set up block access";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        BinaryRegex.begin()
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

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    if (getMethodInfo().getDescriptor().equals(getRenderColor.getType())) {
                        return buildExpression(
                            BinaryRegex.begin(),
                            push(0xffffff),
                            IRETURN,
                            BinaryRegex.end()
                        );
                    } else {
                        return null;
                    }
                }
            }.setMethod(getRenderColor));

            patches.add(new BytecodePatch() {
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

            patches.add(new BytecodePatch() {
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
                memberMappers.add(new MethodMapper(new MethodRef(getDeobfClass(), "getBiomeGenAt", "(II)LBiomeGenBase;")));
            }
        }
    }

    private class BiomeGenBaseMod extends ClassMod {
        BiomeGenBaseMod() {
            classSignatures.add(new ConstSignature("Ocean"));
            classSignatures.add(new ConstSignature("Plains"));
            classSignatures.add(new ConstSignature("Desert"));

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    if (getMethodInfo().isConstructor()) {
                        return buildExpression(
                            ALOAD_0,
                            push(0xffffff),
                            BytecodeMatcher.captureReference(PUTFIELD)
                        );
                    } else {
                        return null;
                    }
                }
            }.addXref(1, new FieldRef(getDeobfClass(), "waterColorMultiplier", "I")));

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    if (getMethodInfo().isConstructor()) {
                        return buildExpression(
                            ALOAD_0,
                            ILOAD_1,
                            BytecodeMatcher.captureReference(PUTFIELD)
                        );
                    } else {
                        return null;
                    }
                }
            }.addXref(1, new FieldRef(getDeobfClass(), "biomeID", "I")));

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    if (getMethodInfo().isConstructor()) {
                        return buildExpression(
                            ALOAD_0,
                            push(0.5f),
                            BytecodeMatcher.captureReference(PUTFIELD),
                            ALOAD_0,
                            push(0.5f),
                            BytecodeMatcher.captureReference(PUTFIELD)
                        );
                    } else {
                        return null;
                    }
                }
            }
                .addXref(1, new FieldRef(getDeobfClass(), "temperature", "F"))
                .addXref(2, new FieldRef(getDeobfClass(), "rainfall", "F"))
            );

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    if (getMethodInfo().getDescriptor().startsWith("(Ljava/lang/String;)")) {
                        return buildExpression(
                            BinaryRegex.begin(),
                            ALOAD_0,
                            ALOAD_1,
                            BytecodeMatcher.captureReference(PUTFIELD),
                            ALOAD_0,
                            ARETURN,
                            BinaryRegex.end()
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
                classSignatures.add(new BytecodeSignature() {
                    @Override
                    public String getMatchExpression() {
                        return buildExpression(
                            // d = MathHelper.clampf(getTemperature(), 0.0f, 1.0f);
                            BinaryRegex.begin(),
                            ALOAD_0,
                            BytecodeMatcher.captureReference(INVOKEVIRTUAL),
                            FCONST_0,
                            FCONST_1,
                            BytecodeMatcher.anyReference(INVOKESTATIC),
                            F2D,
                            DSTORE_1,

                            // d1 = MathHelper.clampf(getRainfall(), 0.0f, 1.0f);
                            ALOAD_0,
                            BytecodeMatcher.captureReference(INVOKEVIRTUAL),
                            FCONST_0,
                            FCONST_1,
                            BytecodeMatcher.anyReference(INVOKESTATIC),
                            F2D,
                            DSTORE_3,

                            // return Colorizerxxx.yyy(d, d1);
                            DLOAD_1,
                            DLOAD_3,
                            BytecodeMatcher.anyReference(INVOKESTATIC),
                            IRETURN
                        );
                    }
                }
                    .addXref(1, new MethodRef(getDeobfClass(), "getTemperaturef", "()F"))
                    .addXref(2, new MethodRef(getDeobfClass(), "getRainfallf", "()F"))
                );

                memberMappers.add(new MethodMapper(new MethodRef(getDeobfClass(), "getGrassColor", "()I"), new MethodRef(getDeobfClass(), "getFoliageColor", "()I"))
                    .accessFlag(AccessFlag.PUBLIC, true)
                    .accessFlag(AccessFlag.STATIC, false)
                    .accessFlag(AccessFlag.FINAL, false)
                );
            } else {
                classSignatures.add(new BytecodeSignature() {
                    @Override
                    public String getMatchExpression() {
                        return buildExpression(
                            // d = iblockaccess.getWorldChunkManager().getTemperature(i, j, k);
                            ALOAD_1,
                            BytecodeMatcher.anyReference(INVOKEINTERFACE),
                            ILOAD_2,
                            ILOAD_3,
                            ILOAD, 4,
                            BytecodeMatcher.captureReference(INVOKEVIRTUAL),
                            F2D,
                            DSTORE, 5,

                            // d1 = iblockaccess.getWorldChunkManager().getRainfall(i, k);
                            ALOAD_1,
                            BytecodeMatcher.anyReference(INVOKEINTERFACE),
                            ILOAD_2,
                            ILOAD, 4,
                            BytecodeMatcher.captureReference(INVOKEVIRTUAL),
                            F2D,
                            DSTORE, 7
                        );
                    }
                }
                    .addXref(1, new MethodRef("WorldChunkManager", "getTemperature", "(III)F"))
                    .addXref(2, new MethodRef("WorldChunkManager", "getRainfall", "(II)F"))
                );

                memberMappers.add(new MethodMapper(new MethodRef(getDeobfClass(), "getGrassColor", "(LIBlockAccess;III)I"), new MethodRef(getDeobfClass(), "getFoliageColor", "(LIBlockAccess;III)I")));
            }

            memberMappers.add(new FieldMapper(new FieldRef(getDeobfClass(), "color", "I")).accessFlag(AccessFlag.PUBLIC, true));

            patches.add(new BytecodePatch.InsertBefore() {
                @Override
                public String getDescription() {
                    return "map biomes by name";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        ARETURN,
                        BinaryRegex.end()
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
            parentClass = "BiomeGenBase";

            classSignatures.add(new ConstSignature(MAGIC1));
            classSignatures.add(new ConstSignature(MAGIC2));
            classSignatures.add(new OrSignature(
                new ConstSignature(MAGIC3_A),
                new ConstSignature(MAGIC3_B)
            ));

            addSwampColorPatch("SWAMP_GRASS", "Grass");
            addSwampColorPatch("SWAMP_FOLIAGE", "Foliage");
        }

        private void addSwampColorPatch(final String index, final String name) {
            if (haveNewBiomes) {
                patches.add(new BytecodePatch() {
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
                patches.add(new BytecodePatch.InsertAfter() {
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
            classSignatures.add(new ConstSignature("splash"));
            classSignatures.add(new ConstSignature("liquid.water"));

            final MethodRef colorMultiplier = new MethodRef(getDeobfClass(), "colorMultiplier", "(LIBlockAccess;III)I");
            final FieldRef waterColorMultiplier = new FieldRef("BiomeGenBase", "waterColorMultiplier", "I");

            if (haveNewBiomes) {
                classSignatures.add(new BytecodeSignature() {
                    @Override
                    public String getMatchExpression() {
                        return buildExpression(
                            ALOAD_1,
                            ILOAD_2,
                            BytecodeMatcher.anyILOAD,
                            IADD,
                            ILOAD, 4,
                            BytecodeMatcher.anyILOAD,
                            IADD,
                            BytecodeMatcher.anyReference(INVOKEINTERFACE),
                            BytecodeMatcher.captureReference(GETFIELD)
                        );
                    }
                }
                    .setMethod(colorMultiplier)
                    .addXref(1, waterColorMultiplier)
                );

                patches.add(new BytecodePatch() {
                    @Override
                    public String getDescription() {
                        return "override water color";
                    }

                    @Override
                    public String getMatchExpression() {
                        return buildExpression(
                            reference(INVOKEINTERFACE, new InterfaceMethodRef("IBlockAccess", "getBiomeGenAt", "(II)LBiomeGenBase;")),
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
            } else {
                classSignatures.add(new BytecodeSignature() {
                    @Override
                    public String getMatchExpression() {
                        return buildExpression(
                            ALOAD_1,
                            BytecodeMatcher.anyReference(INVOKEINTERFACE),
                            ILOAD_2,
                            BinaryRegex.any(0, 3),
                            ILOAD, 4,
                            BinaryRegex.any(0, 3),
                            BytecodeMatcher.anyReference(INVOKEVIRTUAL),
                            BinaryRegex.optional(BinaryRegex.build(
                                ASTORE, 5,
                                ALOAD, 5
                            )),
                            BytecodeMatcher.anyReference(GETFIELD)
                        );
                    }
                }.setMethod(colorMultiplier));

                patches.add(new BytecodePatch() {
                    @Override
                    public String getDescription() {
                        return "override water color";
                    }

                    @Override
                    public String getMatchExpression() {
                        return buildExpression(
                            reference(INVOKEVIRTUAL, new MethodRef("WorldChunkManager", "getBiomeGenAt", "(II)LBiomeGenBase;")),
                            BinaryRegex.any(0, 4),
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
            parentClass = "Block";

            for (final int i : new int[]{138, 154, 155}) {
                classSignatures.add(new BytecodeSignature() {
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
            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        BinaryRegex.begin(),
                        push(0xffffff),
                        IRETURN,
                        BinaryRegex.end()
                    );
                }
            }.setMethod(getColorFromDamage));
        }
    }

    private class ItemBlockMod extends ClassMod {
        ItemBlockMod() {
            final FieldRef blockID = new FieldRef(getDeobfClass(), "blockID", "I");

            parentClass = "Item";

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    if (getMethodInfo().isConstructor()) {
                        return buildExpression(
                            ALOAD_0,
                            ILOAD_1,
                            push(256),
                            IADD,
                            BytecodeMatcher.anyReference(PUTFIELD)
                        );
                    } else {
                        return null;
                    }
                }
            });

            memberMappers.add(new FieldMapper(blockID).accessFlag(AccessFlag.PRIVATE, true));

            patches.add(new AddMethodPatch(new MethodRef(getDeobfClass(), getColorFromDamage.getName(), getColorFromDamage.getType())) {
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

            classSignatures.add(new ConstSignature("/terrain.png"));
            classSignatures.add(new ConstSignature("/gui/items.png"));
            classSignatures.add(new ConstSignature("%blur%/misc/glint.png"));
            classSignatures.add(new ConstSignature("/misc/mapbg.png"));

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // par2ItemStack.itemID
                        ALOAD_2,
                        BytecodeMatcher.captureReference(GETFIELD),
                        BinaryRegex.or(
                            BinaryRegex.build(push(256)),
                            BinaryRegex.build(AALOAD)
                        ),

                        // ...
                        BinaryRegex.any(0, 400),

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

            patches.add(new BytecodePatch.InsertAfter() {
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
            classSignatures.add(new ConstSignature("potion.moveSpeed"));
            classSignatures.add(new ConstSignature("potion.moveSlowdown"));

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    if (getMethodInfo().getDescriptor().startsWith("(Ljava/lang/String;)")) {
                        return buildExpression(
                            BinaryRegex.begin(),
                            ALOAD_0,
                            ALOAD_1,
                            BytecodeMatcher.captureReference(PUTFIELD),
                            ALOAD_0,
                            ARETURN,
                            BinaryRegex.end()
                        );
                    } else {
                        return null;
                    }
                }
            }
                .setMethodName("setPotionName")
                .addXref(1, new FieldRef(getDeobfClass(), "name", "Ljava/lang/String;"))
            );

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    if (getMethodInfo().isConstructor()) {
                        return buildExpression(
                            ALOAD_0,
                            ILOAD_1,
                            BytecodeMatcher.captureReference(PUTFIELD)
                        );
                    } else {
                        return null;
                    }
                }
            }.addXref(1, new FieldRef(getDeobfClass(), "id", "I")));

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    if (getMethodInfo().isConstructor()) {
                        return buildExpression(
                            ALOAD_0,
                            ILOAD_3,
                            BytecodeMatcher.captureReference(PUTFIELD)
                        );
                    } else {
                        return null;
                    }
                }
            }.addXref(1, new FieldRef(getDeobfClass(), "color", "I")));

            patches.add(new MakeMemberPublicPatch(new FieldRef(getDeobfClass(), "name", "Ljava/lang/String;")));
            patches.add(new AddFieldPatch(new FieldRef(getDeobfClass(), "origColor", "I")));

            patches.add(new MakeMemberPublicPatch(new FieldRef(getDeobfClass(), "color", "I")) {
                @Override
                public int getNewFlags(int oldFlags) {
                    return super.getNewFlags(oldFlags) & ~AccessFlag.FINAL;
                }
            });

            patches.add(new BytecodePatch.InsertBefore() {
                @Override
                public String getDescription() {
                    return "map potions by name";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        ARETURN,
                        BinaryRegex.end()
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
            classSignatures.add(new ConstSignature("potion.prefix.mundane"));
            classSignatures.add(new ConstSignature(MAGIC));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override water bottle color";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // int i = 0x385dc6;
                        BinaryRegex.begin(),
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
            classSignatures.add(new ConstSignature(color));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override default " + name.toLowerCase() + " foliage color";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        BinaryRegex.begin(),
                        push(color),
                        IRETURN,
                        BinaryRegex.end()
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
            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        BinaryRegex.begin(),
                        ALOAD_1,
                        ILOAD_2,
                        ILOAD_3,
                        ILOAD, 4,
                        BytecodeMatcher.captureReference(INVOKEINTERFACE),
                        ISTORE, 5,

                        ILOAD, 5,
                        BinaryRegex.subset(new byte[]{ICONST_1, ICONST_3}, true), // 1.1 uses (i & 1) == 1, 12w03a uses (i & 3) == 1
                        IAND,
                        ICONST_1,
                        IF_ICMPNE, BinaryRegex.any(2),
                        BytecodeMatcher.captureReference(INVOKESTATIC),
                        IRETURN,

                        ILOAD, 5,
                        BinaryRegex.subset(new byte[]{ICONST_2, ICONST_3}, true), // 1.1 uses (i & 2) == 2, 12w03a uses (i & 3) == 2
                        IAND,
                        ICONST_2,
                        IF_ICMPNE, BinaryRegex.any(2),

                        BytecodeMatcher.captureReference(INVOKESTATIC),
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
                classSignatures.add(new BytecodeSignature() {
                    @Override
                    public String getMatchExpression() {
                        return buildExpression(
                            ALOAD_1,
                            ILOAD_2,
                            BytecodeMatcher.anyILOAD,
                            IADD,
                            ILOAD, 4,
                            BytecodeMatcher.anyILOAD,
                            IADD,
                            BytecodeMatcher.captureReference(INVOKEINTERFACE),
                            BytecodeMatcher.captureReference(INVOKEVIRTUAL)
                        );
                    }
                }
                    .addXref(1, new InterfaceMethodRef("IBlockAccess", "getBiomeGenAt", "(II)LBiomeGenBase;"))
                    .addXref(2, new MethodRef("BiomeGenBase", "getFoliageColor", "()I"))
                );
            } else {
                classSignatures.add(new BytecodeSignature() {
                    @Override
                    public String getMatchExpression() {
                        return buildExpression(
                            ALOAD_1,
                            BytecodeMatcher.captureReference(INVOKEINTERFACE),
                            ILOAD_2,
                            BinaryRegex.any(0, 3),
                            ILOAD, 4,
                            BinaryRegex.any(0, 3),
                            BytecodeMatcher.captureReference(INVOKEVIRTUAL),
                            ALOAD_1,
                            ILOAD_2,
                            BinaryRegex.any(0, 3),
                            ILOAD_3,
                            ILOAD, 4,
                            BinaryRegex.any(0, 3),
                            BytecodeMatcher.captureReference(INVOKEVIRTUAL)
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
            patches.add(new BytecodePatch.InsertAfter() {
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
            interfaces = new String[]{"IBlockAccess"};

            MethodRef getSkyColor = new MethodRef(getDeobfClass(), "getSkyColor", "(LEntity;F)LVec3D;");

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // f8 = (f4 * 0.3f + f5 * 0.59f + f6 * 0.11f) * 0.6f;
                        FLOAD, BinaryRegex.any(),
                        push(0.3f),
                        FMUL,
                        FLOAD, BinaryRegex.any(),
                        push(0.59f),
                        FMUL,
                        FADD,
                        FLOAD, BinaryRegex.any(),
                        push(0.11f),
                        FMUL,
                        FADD,
                        push(0.6f),
                        FMUL,
                        FSTORE, BinaryRegex.any()
                    );
                }
            }.setMethod(getSkyColor));

            memberMappers.add(new MethodMapper(new MethodRef(getDeobfClass(), "getWorldChunkManager", "()LWorldChunkManager;")));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override sky color";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // f4 = (float) (k >> 16 & 0xff) / 255.0f;
                        ILOAD, BinaryRegex.capture(BinaryRegex.any()),
                        push(16),
                        ISHR,
                        push(255),
                        IAND,
                        I2F,
                        push(255.0f),
                        FDIV,
                        FSTORE, BinaryRegex.capture(BinaryRegex.any()),

                        // f5 = (float) (k >> 8 & 0xff) / 255.0f;
                        ILOAD, BinaryRegex.backReference(1),
                        push(8),
                        ISHR,
                        push(255),
                        IAND,
                        I2F,
                        push(255.0f),
                        FDIV,
                        FSTORE, BinaryRegex.capture(BinaryRegex.any()),

                        // f6 = (float) (k & 0xff) / 255.0f;
                        ILOAD, BinaryRegex.backReference(1),
                        push(255),
                        IAND,
                        I2F,
                        push(255.0f),
                        FDIV,
                        FSTORE, BinaryRegex.capture(BinaryRegex.any())
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
            }.targetMethod(getSkyColor));
        }
    }

    private class WorldProviderMod extends ClassMod {
        WorldProviderMod() {
            classSignatures.add(new ConstSignature(0.06f));
            classSignatures.add(new ConstSignature(0.09f));
            classSignatures.add(new ConstSignature(0.91f));
            classSignatures.add(new ConstSignature(0.94f));

            MethodRef getFogColor = new MethodRef(getDeobfClass(), "getFogColor", "(FF)LVec3D;");

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        FLOAD, BinaryRegex.capture(BinaryRegex.any()),
                        FLOAD_3,
                        push(0.94f),
                        FMUL,
                        push(0.06f),
                        FADD,
                        FMUL,
                        FSTORE, BinaryRegex.backReference(1)
                    );
                }
            }.setMethod(getFogColor));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override fog color";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // f3 = 0.7529412f;
                        push(0.7529412f),
                        FSTORE, BinaryRegex.capture(BinaryRegex.any()),

                        // f4 = 0.84705883f;
                        push(0.84705883f),
                        FSTORE, BinaryRegex.capture(BinaryRegex.any()),

                        // f5 = 1.0f;
                        push(1.0f),
                        FSTORE, BinaryRegex.capture(BinaryRegex.any())
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
        WorldProviderHellMod() {
            parentClass = "WorldProvider";

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(0.20000000298023224),
                        push(0.029999999329447746),
                        push(0.029999999329447746)
                    );
                }
            });

            MethodRef getFogColor = new MethodRef(getDeobfClass(), "getFogColor", "(FF)LVec3D;");

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override nether fog color";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(0.20000000298023224),
                        push(0.029999999329447746),
                        push(0.029999999329447746)
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        // Colorizer.setColor[0], Colorizer.setColor[1], Colorizer.setColor[2]
                        reference(GETSTATIC, new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "netherFogColor", "[F")),
                        ICONST_0,
                        FALOAD,
                        F2D,

                        reference(GETSTATIC, new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "netherFogColor", "[F")),
                        ICONST_1,
                        FALOAD,
                        F2D,

                        reference(GETSTATIC, new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "netherFogColor", "[F")),
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
            parentClass = "WorldProvider";

            classSignatures.add(new OrSignature(
                new ConstSignature(0x8080a0), // pre 12w23a
                new ConstSignature(0xa080a0)  // 12w23a+
            ));

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        BytecodeMatcher.anyFLOAD,
                        F2D,
                        BytecodeMatcher.anyFLOAD,
                        F2D,
                        BytecodeMatcher.anyFLOAD,
                        F2D
                    );
                }
            });

            MethodRef getFogColor = new MethodRef(getDeobfClass(), "getFogColor", "(FF)LVec3D;");

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override end fog color";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        BytecodeMatcher.anyFLOAD,
                        F2D,
                        BytecodeMatcher.anyFLOAD,
                        F2D,
                        BytecodeMatcher.anyFLOAD,
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
            classSignatures.add(new BytecodeSignature() {
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

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        BinaryRegex.begin(),
                        ILOAD_1,
                        ILOAD_3,
                        ISUB,
                        ICONST_2,
                        ISHR,
                        ISTORE, 6
                    );
                }
            });

            memberMappers.add(new MethodMapper(new MethodRef(getDeobfClass(), "getBiomeGenAt", "(II)LBiomeGenBase;")));
        }
    }

    private class EntityMod extends ClassMod {
        EntityMod() {
            classSignatures.add(new ConstSignature("tilecrack_"));
            classSignatures.add(new ConstSignature("random.splash"));

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        BinaryRegex.begin(),

                        // prevPosX = posX = d;
                        ALOAD_0,
                        ALOAD_0,
                        DLOAD_1,
                        DUP2_X1,
                        BytecodeMatcher.captureReference(PUTFIELD),
                        BytecodeMatcher.captureReference(PUTFIELD),

                        // prevPosY = posY = d1;
                        ALOAD_0,
                        ALOAD_0,
                        DLOAD_3,
                        DUP2_X1,
                        BytecodeMatcher.captureReference(PUTFIELD),
                        BytecodeMatcher.captureReference(PUTFIELD),

                        // prevPosZ = posZ = d2;
                        ALOAD_0,
                        ALOAD_0,
                        DLOAD, 5,
                        DUP2_X1,
                        BytecodeMatcher.captureReference(PUTFIELD),
                        BytecodeMatcher.captureReference(PUTFIELD)
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

            memberMappers.add(new FieldMapper(new FieldRef(getDeobfClass(), "worldObj", "LWorld;")));
        }
    }

    private class EntityFXMod extends ClassMod {
        EntityFXMod() {
            parentClass = "Entity";

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    if (getMethodInfo().isConstructor()) {
                        return buildExpression(
                            // setSize(0.2f, 0.2f);
                            ALOAD_0,
                            push(0.2f),
                            push(0.2f),
                            BytecodeMatcher.anyReference(INVOKEVIRTUAL)
                        );
                    } else {
                        return null;
                    }
                }
            });

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    if (getMethodInfo().isConstructor()) {
                        return buildExpression(
                            // particleRed = particleGreen = particleBlue = 1.0f;
                            ALOAD_0,
                            ALOAD_0,
                            ALOAD_0,
                            FCONST_1,
                            DUP_X1,
                            BytecodeMatcher.captureReference(PUTFIELD),
                            DUP_X1,
                            BytecodeMatcher.captureReference(PUTFIELD),
                            BytecodeMatcher.captureReference(PUTFIELD)
                        );
                    } else {
                        return null;
                    }
                }
            }
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
            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override " + name + " color";
                }

                @Override
                public String getMatchExpression() {
                    if (!getMethodInfo().isConstructor()) {
                        return null;
                    } else if (origColors == null) {
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
            });
        }
    }

    private class EntityRainFXMod extends WaterFXMod {
        EntityRainFXMod() {
            parentClass = "EntityFX";

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    if (getMethodInfo().isConstructor()) {
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
                    } else {
                        return null;
                    }
                }
            });

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    if (getMethodInfo().isConstructor()) {
                        return buildExpression(
                            // 19 + rand.nextInt(4)
                            push(19),
                            ALOAD_0,
                            BytecodeMatcher.anyReference(GETFIELD),
                            ICONST_4,
                            reference(INVOKEVIRTUAL, new MethodRef("java/util/Random", "nextInt", "(I)I")),
                            IADD
                        );
                    } else {
                        return null;
                    }
                }
            });

            addWaterColorPatch("rain drop", new float[]{1.0f, 1.0f, 1.0f}, new float[]{0.2f, 0.3f, 1.0f});
        }
    }

    private class EntityDropParticleFXMod extends WaterFXMod {
        EntityDropParticleFXMod() {
            parentClass = "EntityFX";

            MethodRef onUpdate = new MethodRef(getDeobfClass(), "onUpdate", "()V");

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // particleRed = 0.2f;
                        ALOAD_0,
                        push(0.2f),
                        BytecodeMatcher.anyReference(PUTFIELD),

                        // particleGreen = 0.3f;
                        ALOAD_0,
                        push(0.3f),
                        BytecodeMatcher.anyReference(PUTFIELD),

                        // particleBlue = 1.0f;
                        ALOAD_0,
                        push(1.0f),
                        BytecodeMatcher.anyReference(PUTFIELD),

                        // ...
                        BinaryRegex.any(0, 30),

                        // 40 - age
                        push(40),
                        ALOAD_0,
                        BytecodeMatcher.captureReference(GETFIELD),
                        ISUB
                    );
                }
            }
                .setMethod(onUpdate)
                .addXref(1, new FieldRef(getDeobfClass(), "timer", "I"))
            );

            addWaterColorPatch("water drop", new float[]{0.0f, 0.0f, 1.0f}, new float[]{0.2f, 0.3f, 1.0f});

            patches.add(new BytecodePatch() {
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

            patches.add(new BytecodePatch() {
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
                        BinaryRegex.any(0, 20),
                        reference(PUTFIELD, new FieldRef(getDeobfClass(), "particleGreen", "F")),

                        // particleBlue = 4.0f / (float)((40 - timer) + 8);
                        ALOAD_0,
                        push(4.0f),
                        BinaryRegex.any(0, 20),
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
            parentClass = "EntityRainFX";

            classSignatures.add(new ConstSignature(0.04f));
            classSignatures.add(new ConstSignature(0.10000000000000001));

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    if (getMethodInfo().isConstructor()) {
                        return buildExpression(
                            ALOAD_0,
                            ALOAD_0,
                            BytecodeMatcher.anyReference(INVOKEVIRTUAL),
                            ICONST_1,
                            IADD,
                            BytecodeMatcher.anyReference(INVOKEVIRTUAL)
                        );
                    } else {
                        return null;
                    }
                }
            });

            addWaterColorPatch("splash", null);
        }
    }

    private class EntityBubbleFXMod extends WaterFXMod {
        EntityBubbleFXMod() {
            parentClass = "EntityFX";

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    if (getMethodInfo().isConstructor()) {
                        return buildExpression(
                            // setParticleTextureIndex(32);
                            ALOAD_0,
                            push(32),
                            BytecodeMatcher.anyReference(INVOKEVIRTUAL),

                            // setSize(0.02F, 0.02F);
                            ALOAD_0,
                            push(0.02f),
                            push(0.02f),
                            BytecodeMatcher.anyReference(INVOKEVIRTUAL)
                        );
                    } else {
                        return null;
                    }
                }
            });

            addWaterColorPatch("bubble", new float[]{1.0f, 1.0f, 1.0f});
        }
    }

    private class EntitySuspendFXMod extends ClassMod {
        EntitySuspendFXMod() {
            parentClass = "EntityFX";

            classSignatures.add(new ConstSignature(0.4f));
            classSignatures.add(new ConstSignature(0.7f));

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    if (getMethodInfo().isConstructor()) {
                        return buildExpression(
                            ALOAD_0,
                            push(0.01f),
                            push(0.01f),
                            BytecodeMatcher.anyReference(INVOKEVIRTUAL)
                        );
                    } else {
                        return null;
                    }
                }
            });

            patches.add(new BytecodePatch() {
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
            parentClass = "EntityFX";

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    if (getMethodInfo().isConstructor()) {
                        return buildExpression(
                            // particleGreen *= 0.3f;
                            ALOAD_0,
                            DUP,
                            GETFIELD, BinaryRegex.capture(BinaryRegex.any(2)),
                            push(0.3f),
                            FMUL,
                            PUTFIELD, BinaryRegex.backReference(1),

                            // particleBlue *= 0.9f;
                            ALOAD_0,
                            DUP,
                            GETFIELD, BinaryRegex.capture(BinaryRegex.any(2)),
                            push(0.9f),
                            FMUL,
                            PUTFIELD, BinaryRegex.backReference(2)
                        );
                    } else {
                        return null;
                    }
                }
            });

            addPortalPatch(0.9f, 0, "red");
            addPortalPatch(0.3f, 1, "green");

            patches.add(new BytecodePatch.InsertBefore() {
                @Override
                public String getDescription() {
                    return "override portal particle color (blue)";
                }

                @Override
                public String getMatchExpression() {
                    if (getMethodInfo().isConstructor()) {
                        return buildExpression(
                            RETURN
                        );
                    } else {
                        return null;
                    }
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
            });
        }

        private void addPortalPatch(final float origValue, final int index, final String color) {
            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override portal particle color (" + color + ")";
                }

                @Override
                public String getMatchExpression() {
                    if (getMethodInfo().isConstructor()) {
                        return buildExpression(
                            push(origValue)
                        );
                    } else {
                        return null;
                    }
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        reference(GETSTATIC, new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "portalColor", "[F")),
                        push(index),
                        FALOAD
                    );
                }
            });
        }
    }

    private class EntityAuraFXMod extends ClassMod {
        EntityAuraFXMod() {
            parentClass = "EntityFX";

            classSignatures.add(new ConstSignature(0.019999999552965164));

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    if (getMethodInfo().isConstructor()) {
                        return buildExpression(
                            ALOAD_0,
                            push(0.02f),
                            push(0.02f),
                            BytecodeMatcher.anyReference(INVOKEVIRTUAL)
                        );
                    } else {
                        return null;
                    }
                }
            });

            patches.add(new AddMethodPatch(new MethodRef(getDeobfClass(), "colorize", "()LEntityAuraFX;")) {
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
            classSignatures.add(new ConstSignature(MAGIC));
            classSignatures.add(new ConstSignature(0.5f));
            classSignatures.add(new ConstSignature(0.015625f));

            patches.add(new BytecodePatch() {
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
            parentClass = "Entity";

            classSignatures.add(new ConstSignature("/mob/char.png"));
            classSignatures.add(new ConstSignature("bubble"));
            classSignatures.add(new ConstSignature("explode"));
        }
    }

    private class EntityRendererMod extends ClassMod {
        private byte[] isNightVisionActive = new byte[]{(byte) ICONST_0};
        private byte[] getNightVisionCode = new byte[]{(byte) FCONST_0};

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

            classSignatures.add(new ConstSignature("ambient.weather.rain"));
            classSignatures.add(new ConstSignature("/terrain.png"));

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // sun = world.func_35464_b(1.0F) * 0.95F + 0.05F;
                        BytecodeMatcher.registerLoadStore(ALOAD, 1 + updateLightmapOffset),
                        FCONST_1,
                        BytecodeMatcher.captureReference(INVOKEVIRTUAL),
                        push(0.95f),
                        FMUL,
                        push(0.05f),
                        FADD,
                        BytecodeMatcher.registerLoadStore(FSTORE, 3 + updateLightmapOffset),

                        // lightsun = world.worldProvider.lightBrightnessTable[i / 16] * sun;
                        BytecodeMatcher.registerLoadStore(ALOAD, 1 + updateLightmapOffset),
                        BytecodeMatcher.captureReference(GETFIELD),
                        BytecodeMatcher.captureReference(GETFIELD),
                        BytecodeMatcher.registerLoadStore(ILOAD, 2 + updateLightmapOffset),
                        BIPUSH, 16,
                        IDIV,
                        FALOAD,
                        BytecodeMatcher.registerLoadStore(FLOAD, 3 + updateLightmapOffset),
                        FMUL,
                        BytecodeMatcher.registerLoadStore(FSTORE, 4 + updateLightmapOffset),

                        // lighttorch = world.worldProvider.lightBrightnessTable[i % 16] * (torchFlickerX * 0.1F + 1.5F);
                        BinaryRegex.any(0, 20),
                        BytecodeMatcher.registerLoadStore(ILOAD, 2 + updateLightmapOffset),
                        BIPUSH, 16,
                        IREM,
                        FALOAD,
                        ALOAD_0,
                        BytecodeMatcher.captureReference(GETFIELD),

                        // ...
                        BinaryRegex.any(0, 200),

                        // if (world.lightningFlash > 0)
                        BytecodeMatcher.registerLoadStore(ALOAD, 1 + updateLightmapOffset),
                        BytecodeMatcher.captureReference(GETFIELD),
                        IFLE, BinaryRegex.any(2),

                        // ...
                        BinaryRegex.any(0, 200),

                        // if (world.worldProvider.worldType == 1) {
                        BytecodeMatcher.registerLoadStore(ALOAD, 1 + updateLightmapOffset),
                        BinaryRegex.backReference(2),
                        BytecodeMatcher.captureReference(GETFIELD),
                        ICONST_1,
                        IF_ICMPNE, BinaryRegex.any(2),

                        // ...
                        BinaryRegex.any(0, 200),

                        // gamma = mc.gameSettings.gammaSetting;
                        ALOAD_0,
                        BytecodeMatcher.captureReference(GETFIELD),
                        BytecodeMatcher.captureReference(GETFIELD),
                        BytecodeMatcher.captureReference(GETFIELD),
                        BytecodeMatcher.registerLoadStore(FSTORE, 15 + updateLightmapOffset),

                        // ...
                        BinaryRegex.any(0, 300),

                        // mc.renderEngine.createTextureFromBytes(lightmapColors, 16, 16, lightmapTexture);
                        ALOAD_0,
                        BinaryRegex.backReference(7),
                        BytecodeMatcher.captureReference(GETFIELD),
                        ALOAD_0,
                        BytecodeMatcher.captureReference(GETFIELD),
                        BIPUSH, 16,
                        BIPUSH, 16,
                        ALOAD_0,
                        BytecodeMatcher.captureReference(GETFIELD),
                        BytecodeMatcher.captureReference(INVOKEVIRTUAL),
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
                .addXref(7, new FieldRef(getDeobfClass(), "mc", "LMinecraft;"))
                .addXref(8, new FieldRef("Minecraft", "gameSettings", "LGameSettings;"))
                .addXref(9, new FieldRef("GameSettings", "gammaSetting", "F"))
                .addXref(10, new FieldRef("Minecraft", "renderEngine", "LRenderEngine;"))
                .addXref(11, new FieldRef(getDeobfClass(), "lightmapColors", "[I"))
                .addXref(12, new FieldRef(getDeobfClass(), "lightmapTexture", "I"))
                .addXref(13, new MethodRef("RenderEngine", "createTextureFromBytes", "([IIII)V"))
            );

            final MethodRef updateFogColor = new MethodRef(getDeobfClass(), "updateFogColor", "(F)V");
            final FieldRef fogColorRed = new FieldRef(getDeobfClass(), "fogColorRed", "F");
            final FieldRef fogColorGreen = new FieldRef(getDeobfClass(), "fogColorGreen", "F");
            final FieldRef fogColorBlue = new FieldRef(getDeobfClass(), "fogColorBlue", "F");
            final MethodRef getNightVisionStrength1 = new MethodRef(getDeobfClass(), "getNightVisionStrength1", "(LEntityPlayer;F)F");
            final MethodRef getNightVisionStrength = new MethodRef(getDeobfClass(), "getNightVisionStrength", "(F)F");

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // fogColorRed = 0.02f;
                        ALOAD_0,
                        push(0.02f),
                        BytecodeMatcher.captureReference(PUTFIELD),

                        // fogColorGreen = 0.02f;
                        ALOAD_0,
                        push(0.02f),
                        BytecodeMatcher.captureReference(PUTFIELD),

                        // fogColorBlue = 0.2f;
                        ALOAD_0,
                        push(0.2f),
                        BytecodeMatcher.captureReference(PUTFIELD)
                    );
                }
            }
                .setMethod(updateFogColor)
                .addXref(1, fogColorRed)
                .addXref(2, fogColorGreen)
                .addXref(3, fogColorBlue)
            );

            if (haveNightVision) {
                classSignatures.add(new BytecodeSignature() {
                    @Override
                    public String getMatchExpression() {
                        return buildExpression(
                            // if (mc.thePlayer.isPotionActive(Potion.nightVision)) {
                            BinaryRegex.capture(BinaryRegex.build(
                                ALOAD_0,
                                BytecodeMatcher.captureReference(GETFIELD),
                                BytecodeMatcher.captureReference(GETFIELD),
                                BytecodeMatcher.captureReference(GETSTATIC),
                                BytecodeMatcher.captureReference(INVOKEVIRTUAL)
                            )),
                            IFEQ, BinaryRegex.any(2),

                            // var16 = getNightVisionStrength1(mc.thePlayer, var1);
                            BinaryRegex.capture(BinaryRegex.build(
                                ALOAD_0,
                                ALOAD_0,
                                BinaryRegex.backReference(2),
                                BinaryRegex.backReference(3),
                                FLOAD_1,
                                BytecodeMatcher.captureReference(INVOKESPECIAL)
                            )),
                            FSTORE, BinaryRegex.any()
                        );
                    }

                    @Override
                    public void afterMatch(ClassFile classFile, MethodInfo methodInfo) {
                        isNightVisionActive = matcher.getCaptureGroup(1);
                        getNightVisionCode = matcher.getCaptureGroup(6);
                    }
                }
                    .setMethod(updateLightmap)
                    .addXref(2, new FieldRef(getDeobfClass(), "mc", "LMinecraft;"))
                    .addXref(3, new FieldRef("Minecraft", "thePlayer", "LEntityClientPlayerMP;"))
                    .addXref(4, new FieldRef("Potion", "nightVision", "LPotion;"))
                    .addXref(5, new MethodRef("EntityClientPlayerMP", "isPotionActive", "(LPotion;)Z"))
                    .addXref(7, getNightVisionStrength1)
                );
            }

            patches.add(new AddMethodPatch(getNightVisionStrength) {
                @Override
                public byte[] generateMethod() throws BadBytecode, IOException {
                    return buildCode(
                        isNightVisionActive,
                        IFEQ, branch("A"),

                        getNightVisionCode,
                        FRETURN,

                        label("A"),
                        push(0.0f),
                        FRETURN
                    );
                }
            });

            patches.add(new MakeMemberPublicPatch(new FieldRef(getDeobfClass(), "torchFlickerX", "F")));

            patches.add(new BytecodePatch.InsertAfter() {
                @Override
                public String getDescription() {
                    return "override lightmap";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        BytecodeMatcher.registerLoadStore(ASTORE, 1 + updateLightmapOffset)
                    );
                }

                @Override
                public byte[] getInsertBytes() throws IOException {
                    return buildCode(
                        // if (Colorizer.computeLightmap(this, world)) {
                        ALOAD_0,
                        BytecodeMatcher.registerLoadStore(ALOAD, 1 + updateLightmapOffset),
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.COLORIZER_CLASS, "computeLightmap", "(LEntityRenderer;LWorld;)Z")),
                        IFEQ, branch("A"),

                        // return;
                        RETURN,

                        // }
                        label("A")
                    );
                }
            }.targetMethod(updateLightmap));

            patches.add(new BytecodePatch.InsertBefore() {
                @Override
                public String getDescription() {
                    return "override fog color";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // f1 = 1.0f - Math.pow(f1, 0.25);
                        push(1.0f),
                        FLOAD, BinaryRegex.capture(BinaryRegex.any()),
                        F2D,
                        push(0.25),
                        reference(INVOKESTATIC, new MethodRef("java/lang/Math", "pow", "(DD)D")),
                        D2F,
                        FSUB,
                        FSTORE, BinaryRegex.backReference(1),

                        // ...
                        BinaryRegex.any(0, 100),

                        // fogColorBlue = vec3d1.zCoord;
                        ALOAD_0,
                        BytecodeMatcher.anyALOAD,
                        BytecodeMatcher.anyReference(GETFIELD),
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

            patches.add(new BytecodePatch.InsertAfter() {
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
            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // f = (float) l / 15.0f;
                        BytecodeMatcher.anyILOAD,
                        I2F,
                        push(15.0f),
                        FDIV,
                        BytecodeMatcher.anyFSTORE,

                        // f1 = f * 0.6f + 0.4f;
                        BytecodeMatcher.anyFLOAD,
                        push(0.6f),
                        FMUL,
                        push(0.4f),
                        FADD,
                        BytecodeMatcher.anyFSTORE
                    );
                }
            }.setMethod(method));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return description;
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ILOAD, BinaryRegex.capture(BinaryRegex.any()),
                        I2F,
                        push(15.0f),
                        FDIV,
                        FSTORE, BinaryRegex.capture(BinaryRegex.any()),

                        FLOAD, BinaryRegex.backReference(2),
                        push(0.6f),
                        FMUL,
                        push(0.4f),
                        FADD,
                        FSTORE, BinaryRegex.capture(BinaryRegex.any()),

                        BinaryRegex.any(0, 10),

                        FLOAD, BinaryRegex.backReference(2),
                        FLOAD, BinaryRegex.backReference(2),
                        FMUL,
                        push(0.7f),
                        FMUL,
                        push(0.5f),
                        FSUB,
                        FSTORE, BinaryRegex.capture(BinaryRegex.any()),

                        FLOAD, BinaryRegex.backReference(2),
                        FLOAD, BinaryRegex.backReference(2),
                        FMUL,
                        push(0.6f),
                        FMUL,
                        push(0.7f),
                        FSUB,
                        FSTORE, BinaryRegex.capture(BinaryRegex.any())
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

            parentClass = "Block";

            classSignatures.add(new ConstSignature("reddust"));

            patches.add(new BytecodePatch() {
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

            classSignatures.add(new ConstSignature(0.1875));
            classSignatures.add(new ConstSignature(0.01));

            final MethodRef renderBlockFallingSand = new MethodRef(getDeobfClass(), "renderBlockFallingSand", "(LBlock;LWorld;III" + (renderBlockFallingSandTakes4Ints ? "I" : "") + ")V");
            final int renderBlockFallingSandOffset = renderBlockFallingSandTakes4Ints ? 1 : 0;
            final MethodRef setColorOpaque_F = new MethodRef("Tessellator", "setColorOpaque_F", "(FFF)V");
            final MethodRef renderBlockFluids = new MethodRef(getDeobfClass(), "renderBlockFluids", "(LBlock;III)Z");

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_1,
                        ALOAD_0,
                        BytecodeMatcher.captureReference(GETFIELD),
                        ILOAD_2,
                        ILOAD_3,
                        ILOAD, 4,
                        BytecodeMatcher.captureReference(INVOKEVIRTUAL),
                        ISTORE, 6
                    );
                }
            }
                .setMethod(renderBlockFluids)
                .addXref(1, new FieldRef(getDeobfClass(), "blockAccess", "LIBlockAccess;"))
                .addXref(2, new MethodRef("Block", "colorMultiplier", "(LIBlockAccess;III)I"))
            );

            classSignatures.add(new BytecodeSignature() {
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
                        BytecodeMatcher.captureReference(INVOKEVIRTUAL)
                    );
                }
            }.addXref(1, setColorOpaque_F));

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        BinaryRegex.begin(),
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

            memberMappers.add(new MethodMapper(new MethodRef(getDeobfClass(), "renderBlockCauldron", "(LBlockCauldron;III)Z")));

            patches.add(new BytecodePatch.InsertAfter() {
                @Override
                public String getDescription() {
                    return "colorize cauldron water color";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(205),
                        ISTORE, BinaryRegex.any()
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

            patches.add(new BytecodePatch() {
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
                        BinaryRegex.capture(BytecodeMatcher.anyFLOAD),
                        FLOAD, 12 + renderBlockFallingSandOffset,
                        FMUL,
                        BinaryRegex.backReference(1),
                        FLOAD, 12 + renderBlockFallingSandOffset,
                        FMUL,
                        BinaryRegex.backReference(1),
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

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "save water color registers";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // f = (float)(l >> 16 & 0xff) / 255F;
                        BinaryRegex.capture(BytecodeMatcher.anyILOAD),
                        push(16),
                        ISHR,
                        push(255),
                        IAND,
                        I2F,
                        push(255.0f),
                        FDIV,
                        FSTORE, BinaryRegex.capture(BinaryRegex.any()),

                        // f1 = (float)(l >> 8 & 0xff) / 255F;
                        BinaryRegex.backReference(1),
                        push(8),
                        ISHR,
                        push(255),
                        IAND,
                        I2F,
                        push(255.0f),
                        FDIV,
                        FSTORE, BinaryRegex.capture(BinaryRegex.any()),

                        // f2 = (float)(l & 0xff) / 255F;
                        BinaryRegex.backReference(1),
                        push(255),
                        IAND,
                        I2F,
                        push(255.0f),
                        FDIV,
                        FSTORE, BinaryRegex.capture(BinaryRegex.any())
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

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "colorize bottom of water block";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // tessellator.setColorOpaque_F(f3 * f7, f3 * f7, f3 * f7);
                        ALOAD, 5,
                        FLOAD, BinaryRegex.capture(BinaryRegex.any()),
                        FLOAD, BinaryRegex.capture(BinaryRegex.any()),
                        FMUL,
                        FLOAD, BinaryRegex.backReference(1),
                        FLOAD, BinaryRegex.backReference(2),
                        FMUL,
                        FLOAD, BinaryRegex.backReference(1),
                        FLOAD, BinaryRegex.backReference(2),
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
            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    if (getMethodInfo().isConstructor()) {
                        return buildExpression(
                            ALOAD_0,
                            reference(INVOKESTATIC, new MethodRef("java/lang/Math", "random", "()D")),
                            push(0.20000000298023224),
                            DMUL,
                            D2F,
                            push(0.8f),
                            FADD,
                            BytecodeMatcher.anyFLOAD,
                            FMUL,
                            BytecodeMatcher.anyFLOAD,
                            FMUL,
                            BytecodeMatcher.anyReference(PUTFIELD)
                        );
                    } else {
                        return null;
                    }
                }
            });

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override redstone particle color";
                }

                @Override
                public String getMatchExpression() {
                    if (getMethodInfo().isConstructor()) {
                        return buildExpression(
                            FCONST_1,
                            FSTORE, 9,
                            reference(INVOKESTATIC, new MethodRef("java/lang/Math", "random", "()D"))
                        );
                    } else {
                        return null;
                    }
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
            });
        }
    }

    public class BlockStemMod extends ClassMod {
        BlockStemMod() {
            MethodRef getRenderColor = new MethodRef(getDeobfClass(), "getRenderColor", "(I)I");

            classSignatures.add(new FixedBytecodeSignature(
                // j = i * 32;
                BinaryRegex.begin(),
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
                BinaryRegex.end()
            ).setMethod(getRenderColor));

            patches.add(new BytecodePatch.InsertBefore() {
                @Override
                public String getDescription() {
                    return "override pumpkin and melon stem color";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        IRETURN,
                        BinaryRegex.end()
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
            classSignatures.add(new ConstSignature("/environment/clouds.png"));

            MethodRef renderClouds = new MethodRef(getDeobfClass(), "renderClouds", "(F)V");
            MethodRef renderSky = new MethodRef(getDeobfClass(), "renderSky", "(F)V");

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(32),
                        BytecodeMatcher.anyISTORE,
                        push(256),
                        BytecodeMatcher.anyILOAD,
                        IDIV,
                        BytecodeMatcher.anyISTORE,
                        BinaryRegex.any(1, 50),
                        push("/environment/clouds.png")
                    );
                }
            }.setMethod(renderClouds));

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push("/misc/tunnel.png")
                    );
                }
            }.setMethod(renderSky));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override cloud type";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        BinaryRegex.lookBehind(BinaryRegex.any(21), false),
                        BinaryRegex.capture(BinaryRegex.build(
                            ALOAD_0,
                            BytecodeMatcher.anyReference(GETFIELD),
                            BytecodeMatcher.anyReference(GETFIELD),
                            BytecodeMatcher.anyReference(GETFIELD)
                        )),
                        BinaryRegex.capture(BinaryRegex.build(
                            IFEQ, BinaryRegex.any(2)
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

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override end sky color";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(BinaryRegex.or(
                        BinaryRegex.build(push(0x181818)), // pre-12w23a
                        BinaryRegex.build(push(0x282828))  // 12w23a+
                    ));
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        reference(GETSTATIC, new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "endSkyColor", "I"))
                    );
                }
            }.targetMethod(renderSky));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override mycelium particle color";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(BinaryRegex.lookBehind(BinaryRegex.build(
                        // if (s.equals("townaura")) {
                        ALOAD_1,
                        push("townaura"),
                        reference(INVOKEVIRTUAL, new MethodRef("java/lang/String", "equals", "(Ljava/lang/Object;)Z")),
                        IFEQ, BinaryRegex.any(2),

                        // obj = new EntityAuraFX(worldObj, d, d1, d2, d3, d4, d5);
                        reference(NEW, new ClassRef("EntityAuraFX")),
                        DUP,
                        ALOAD_0,
                        BytecodeMatcher.anyReference(GETFIELD),
                        BytecodeMatcher.anyDLOAD,
                        BytecodeMatcher.anyDLOAD,
                        BytecodeMatcher.anyDLOAD,
                        BytecodeMatcher.anyDLOAD,
                        BytecodeMatcher.anyDLOAD,
                        BytecodeMatcher.anyDLOAD,
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
            classSignatures.add(new ConstSignature("Skipping Entity with id "));

            final MethodRef addMapping = new MethodRef(getDeobfClass(), "addMapping", "(Ljava/lang/Class;Ljava/lang/String;III)V");

            memberMappers.add(new MethodMapper(addMapping).accessFlag(AccessFlag.STATIC, true));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "set up mapping for spawnable entities";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        BinaryRegex.begin()
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

            parentClass = "Item";

            classSignatures.add(new ConstSignature(".name"));
            classSignatures.add(new ConstSignature("entity."));

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // s1 = EntityList.getEntityString(itemStack.getItemDamage());
                        ALOAD_1,
                        BytecodeMatcher.captureReference(INVOKEVIRTUAL),
                        BytecodeMatcher.captureReference(INVOKESTATIC),
                        ASTORE_3,
                        ALOAD_3
                    );
                }
            }
                .setMethod(getItemNameIS)
                .addXref(1, getItemDamage)
                .addXref(2, getEntityString)
            );

            classSignatures.add(new OrSignature(
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

            patches.add(new BytecodePatch.InsertBefore() {
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
            classSignatures.add(new ConstSignature(0x7fb238));
            classSignatures.add(new ConstSignature(0xf7e9a3));
            classSignatures.add(new ConstSignature(0xa7a7a7));
            classSignatures.add(new ConstSignature(0xff0000));
            classSignatures.add(new ConstSignature(0xa0a0ff));

            memberMappers.add(new FieldMapper(new FieldRef(getDeobfClass(), "mapColorArray", "[LMapColor;")).accessFlag(AccessFlag.STATIC, true));
            memberMappers.add(new FieldMapper(new FieldRef(getDeobfClass(), "colorValue", "I"), new FieldRef(getDeobfClass(), "colorIndex", "I")).accessFlag(AccessFlag.STATIC, false));

            patches.add(new AddFieldPatch(new FieldRef(getDeobfClass(), "origColorValue", "I")));

            patches.add(new MakeMemberPublicPatch(new FieldRef(getDeobfClass(), "colorValue", "I")) {
                @Override
                public int getNewFlags(int oldFlags) {
                    return oldFlags & ~AccessFlag.FINAL;
                }
            });

            patches.add(new BytecodePatch.InsertAfter() {
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

            parentClass = "Item";

            classSignatures.add(new ConstSignature("black"));
            classSignatures.add(new ConstSignature("purple"));
            classSignatures.add(new ConstSignature("cyan"));

            memberMappers.add(new FieldMapper(dyeColorNames)
                .accessFlag(AccessFlag.PUBLIC, true)
                .accessFlag(AccessFlag.STATIC, true)
            );
            memberMappers.add(new FieldMapper(dyeColors)
                .accessFlag(AccessFlag.PUBLIC, true)
                .accessFlag(AccessFlag.STATIC, true)
            );
        }
    }

    private class EntitySheepMod extends ClassMod {
        EntitySheepMod() {
            final FieldRef fleeceColorTable = new FieldRef(getDeobfClass(), "fleeceColorTable", "[[F");

            classSignatures.add(new ConstSignature("/mob/sheep.png"));
            classSignatures.add(new ConstSignature("mob.sheep"));

            memberMappers.add(new FieldMapper(fleeceColorTable)
                .accessFlag(AccessFlag.PUBLIC, true)
                .accessFlag(AccessFlag.STATIC, true)
            );
        }
    }

    private class FontRendererMod extends BaseMod.FontRendererMod {
        FontRendererMod() {
            final MethodRef renderString = new MethodRef(getDeobfClass(), "renderString", "(Ljava/lang/String;IIIZ)" + (renderStringReturnsInt ? "I" : "V"));
            final MethodRef glColor4f = new MethodRef(MCPatcherUtils.GL11_CLASS, "glColor4f", "(FFFF)V");
            final FieldRef colorCode = new FieldRef(getDeobfClass(), "colorCode", "[I");

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    if (getMethodInfo().isConstructor()) {
                        return buildExpression(
                            ALOAD_0,
                            push(32),
                            NEWARRAY, T_INT,
                            BytecodeMatcher.captureReference(PUTFIELD)
                        );
                    } else {
                        return null;
                    }
                }
            }.addXref(1, colorCode));

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(0xff000000),
                        BinaryRegex.any(0, 100),
                        reference(INVOKESTATIC, glColor4f)
                    );
                }
            }.setMethod(renderString));

            patches.add(new BytecodePatch.InsertAfter() {
                @Override
                public String getDescription() {
                    return "override text color";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        ICONST_0,
                        BytecodeMatcher.anyReference(PUTFIELD)
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

            patches.add(new BytecodePatch.InsertAfter() {
                @Override
                public String getDescription() {
                    return "override text color codes";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        reference(GETFIELD, colorCode),
                        BinaryRegex.capture(BytecodeMatcher.anyILOAD),
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

            classSignatures.add(new ConstSignature(glDepthMask));

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push("/item/sign.png")
                    );
                }
            }.setMethod(renderTileSignEntityAt));

            patches.add(new BytecodePatch() {
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
                        BinaryRegex.capture(BytecodeMatcher.anyISTORE)
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

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push("/item/xporb.png")
                    );
                }
            }.setMethod(render));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override xp orb color";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        BinaryRegex.lookBehind(BinaryRegex.build(
                            // MathHelper.sin(f8 + 0.0F)
                            BinaryRegex.capture(BytecodeMatcher.anyFLOAD),
                            push(0.0f),
                            FADD,
                            BytecodeMatcher.anyReference(INVOKESTATIC),

                            // ...
                            BinaryRegex.any(0, 200)
                        ), true),

                        // tessellator.setColorRGBA_I(i1, 128);
                        BinaryRegex.capture(BytecodeMatcher.anyILOAD),
                        BinaryRegex.lookAhead(BinaryRegex.build(
                            push(128),
                            BytecodeMatcher.anyReference(INVOKEVIRTUAL)
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
