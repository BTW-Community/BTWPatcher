package com.prupe.mcpatcher.mod;

import com.prupe.mcpatcher.*;
import com.prupe.mcpatcher.mal.BaseTexturePackMod;
import javassist.bytecode.AccessFlag;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static com.prupe.mcpatcher.BinaryRegex.*;
import static com.prupe.mcpatcher.BytecodeMatcher.*;
import static javassist.bytecode.Opcode.*;

public class CustomColors extends Mod {
    private static final MethodRef colorizeBlock1 = new MethodRef(MCPatcherUtils.COLORIZE_BLOCK_CLASS, "colorizeBlock", "(LBlock;)Z");
    private static final MethodRef colorizeBlock2 = new MethodRef(MCPatcherUtils.COLORIZE_BLOCK_CLASS, "colorizeBlock", "(LBlock;I)Z");
    private static final MethodRef colorizeBlock3 = new MethodRef(MCPatcherUtils.COLORIZE_BLOCK_CLASS, "colorizeBlock", "(LBlock;LIBlockAccess;III)Z");
    private static final MethodRef getNewColorMultiplier = new MethodRef(MCPatcherUtils.COLORIZE_BLOCK_CLASS, "getColorMultiplier", "(LBlock;III)I");
    private static final MethodRef colorizeRedstoneWire = new MethodRef(MCPatcherUtils.COLORIZE_BLOCK_CLASS, "colorizeRedstoneWire", "(LIBlockAccess;IIII)I");
    private static final MethodRef colorizeStem = new MethodRef(MCPatcherUtils.COLORIZE_BLOCK_CLASS, "colorizeStem", "(ILBlock;I)I");
    private static final MethodRef colorizeWaterBlockGL = new MethodRef(MCPatcherUtils.COLORIZE_BLOCK_CLASS, "colorizeWaterBlockGL", "(LBlock;)V");
    private static final MethodRef colorizeSpawnerEgg = new MethodRef(MCPatcherUtils.COLORIZE_ITEM_CLASS, "colorizeSpawnerEgg", "(III)I");
    private static final MethodRef colorizeText1 = new MethodRef(MCPatcherUtils.COLORIZE_WORLD_CLASS, "colorizeText", "(I)I");
    private static final MethodRef colorizeText2 = new MethodRef(MCPatcherUtils.COLORIZE_WORLD_CLASS, "colorizeText", "(II)I");
    private static final MethodRef colorizeSignText = new MethodRef(MCPatcherUtils.COLORIZE_WORLD_CLASS, "colorizeSignText", "()I");
    private static final MethodRef colorizeXPOrb = new MethodRef(MCPatcherUtils.COLORIZE_ENTITY_CLASS, "colorizeXPOrb", "(IF)I");
    private static final MethodRef computeUnderwaterColor = new MethodRef(MCPatcherUtils.COLORIZE_WORLD_CLASS, "computeUnderwaterColor", "()Z");
    private static final MethodRef computeFogColor = new MethodRef(MCPatcherUtils.COLORIZE_WORLD_CLASS, "computeFogColor", "(LWorld;F)Z");
    private static final MethodRef computeSkyColor = new MethodRef(MCPatcherUtils.COLORIZE_WORLD_CLASS, "computeSkyColor", "(LWorld;F)Z");
    private static final MethodRef computeLavaDropColor = new MethodRef(MCPatcherUtils.COLORIZE_ENTITY_CLASS, "computeLavaDropColor", "(I)Z");
    private static final MethodRef computeWaterColor1 = new MethodRef(MCPatcherUtils.COLORIZE_BLOCK_CLASS, "computeWaterColor", "(III)Z");
    private static final MethodRef computeWaterColor2 = new MethodRef(MCPatcherUtils.COLORIZE_BLOCK_CLASS, "computeWaterColor", "()V");
    private static final MethodRef computeMyceliumParticleColor = new MethodRef(MCPatcherUtils.COLORIZE_ENTITY_CLASS, "computeMyceliumParticleColor", "()Z");
    private static final MethodRef computeRedstoneWireColor = new MethodRef(MCPatcherUtils.COLORIZE_BLOCK_CLASS, "computeRedstoneWireColor", "(I)Z");
    private static final MethodRef getWaterBottleColor = new MethodRef(MCPatcherUtils.COLORIZE_ITEM_CLASS, "getWaterBottleColor", "()I");
    private static final MethodRef getLilyPadColor = new MethodRef(MCPatcherUtils.COLORIZE_BLOCK_CLASS, "getLilyPadColor", "()I");
    private static final MethodRef setupPotion = new MethodRef(MCPatcherUtils.COLORIZE_ITEM_CLASS, "setupPotion", "(LPotion;)V");
    private static final MethodRef setupForFog = new MethodRef(MCPatcherUtils.COLORIZE_WORLD_CLASS, "setupForFog", "(LEntity;)V");
    private static final MethodRef setupSpawnerEgg = new MethodRef(MCPatcherUtils.COLORIZE_ITEM_CLASS, "setupSpawnerEgg", "(Ljava/lang/String;III)V");
    private static final MethodRef setColorF = new MethodRef(MCPatcherUtils.COLORIZER_CLASS, "setColorF", "(I)V");
    private static final MethodRef drawFancyClouds = new MethodRef(MCPatcherUtils.COLORIZE_WORLD_CLASS, "drawFancyClouds", "(Z)Z");

    private static final FieldRef setColor = new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "setColor", "[F");
    private static final FieldRef blockColor = new FieldRef(MCPatcherUtils.COLORIZE_BLOCK_CLASS, "blockColor", "I");
    private static final FieldRef endFogColor = new FieldRef(MCPatcherUtils.COLORIZE_WORLD_CLASS, "endFogColor", "[F");
    private static final FieldRef endSkyColor = new FieldRef(MCPatcherUtils.COLORIZE_WORLD_CLASS, "endSkyColor", "I");
    private static final FieldRef netherFogColor = new FieldRef(MCPatcherUtils.COLORIZE_WORLD_CLASS, "netherFogColor", "[F");
    private static final FieldRef portalColor = new FieldRef(MCPatcherUtils.COLORIZE_ENTITY_CLASS, "portalColor", "[F");
    private static final FieldRef armorColors = new FieldRef(MCPatcherUtils.COLORIZE_ENTITY_CLASS, "armorColors", "[[F");
    private static final FieldRef collarColors = new FieldRef(MCPatcherUtils.COLORIZE_ENTITY_CLASS, "collarColors", "[[F");
    private static final FieldRef undyedLeatherColor = new FieldRef(MCPatcherUtils.COLORIZE_ENTITY_CLASS, "undyedLeatherColor", "I");

    private static final MethodRef getColorFromDamage = new MethodRef("Item", "getColorFromDamage", "(LItemStack;I)I");
    private static final FieldRef fleeceColorTable = new FieldRef("EntitySheep", "fleeceColorTable", "[[F");
    private static final MethodRef getBlockColor = new MethodRef("Block", "getBlockColor", "()I");
    private static final MethodRef getRenderColor = new MethodRef("Block", "getRenderColor", "(I)I");
    private static final MethodRef colorMultiplier = new MethodRef("Block", "colorMultiplier", "(LIBlockAccess;III)I");

    public CustomColors() {
        name = MCPatcherUtils.CUSTOM_COLORS;
        author = "MCPatcher";
        description = "Gives texture packs control over hardcoded colors in the game.";
        version = "1.6";

        addDependency(MCPatcherUtils.BASE_TEXTURE_PACK_MOD);
        addDependency(MCPatcherUtils.BLOCK_API_MOD);
        addDependency(MCPatcherUtils.BIOME_API_MOD);

        configPanel = new ConfigPanel();

        addClassMod(new BaseMod.MinecraftMod(this).mapWorldClient());
        addClassMod(new BaseMod.IBlockAccessMod(this));
        addClassMod(new BaseMod.TessellatorMod(this));

        addClassMod(new BlockMod());
        addClassMod(new BlockFluidMod());
        addClassMod(new BlockLeavesMod());
        addClassMod(new BlockLeavesBaseMod());
        addClassMod(new BlockLilyPadMod());
        addClassMod(new BlockTallGrassMod());
        addClassMod(new BlockFlowerMod());
        addClassMod(new BlockVineMod());
        addClassMod(new BlockCauldronMod());

        addClassMod(new BaseMod.BiomeGenBaseMod(this));
        addClassMod(new ItemMod());
        addClassMod(new ItemBlockMod());
        addClassMod(new ItemRendererMod());

        addClassMod(new PotionMod());
        addClassMod(new PotionHelperMod());

        addClassMod(new WorldMod());
        addClassMod(new BaseMod.WorldClientMod(this));
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

        addClassMod(new EntityLivingBaseMod());
        addClassMod(new EntityRendererMod());

        addClassMod(new BlockRedstoneWireMod());
        addClassMod(new RenderBlocksMod());
        addClassMod(new EntityReddustFXMod());

        addClassMod(new RenderGlobalMod());

        addClassMod(new BlockStemMod());

        addClassMod(new MapColorMod());

        addClassMod(new ItemDyeMod());
        addClassMod(new EntitySheepMod());

        addClassMod(new ItemArmorMod());
        addClassMod(new RenderWolfMod());
        addClassMod(new RecipesDyedArmorMod());

        addClassMod(new EntityListMod());
        addClassMod(new ItemSpawnerEggMod());

        addClassMod(new FontRendererMod());
        addClassMod(new TileEntitySignRendererMod());

        addClassMod(new RenderXPOrbMod());

        addClassFile(MCPatcherUtils.COLORIZER_CLASS);
        addClassFile(MCPatcherUtils.COLORIZER_CLASS + "$1");
        addClassFile(MCPatcherUtils.COLORIZE_WORLD_CLASS);
        addClassFile(MCPatcherUtils.COLORIZE_ITEM_CLASS);
        addClassFile(MCPatcherUtils.COLORIZE_ENTITY_CLASS);
        addClassFile(MCPatcherUtils.COLORIZE_BLOCK_CLASS);
        addClassFile(MCPatcherUtils.COLOR_MAP_CLASS);
        addClassFile(MCPatcherUtils.COLOR_MAP_CLASS + "$V1");
        addClassFile(MCPatcherUtils.COLOR_MAP_CLASS + "$V2");
        addClassFile(MCPatcherUtils.LIGHTMAP_CLASS);

        BaseTexturePackMod.earlyInitialize(3, MCPatcherUtils.COLORIZER_CLASS, "init");
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
                    Config.set(MCPatcherUtils.CUSTOM_COLORS, "water", waterCheckBox.isSelected());
                }
            });

            swampCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Config.set(MCPatcherUtils.CUSTOM_COLORS, "swamp", swampCheckBox.isSelected());
                }
            });

            treeCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Config.set(MCPatcherUtils.CUSTOM_COLORS, "tree", treeCheckBox.isSelected());
                }
            });

            potionCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Config.set(MCPatcherUtils.CUSTOM_COLORS, "potion", potionCheckBox.isSelected());
                }
            });

            particleCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Config.set(MCPatcherUtils.CUSTOM_COLORS, "particle", particleCheckBox.isSelected());
                }
            });

            lightmapCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Config.set(MCPatcherUtils.CUSTOM_COLORS, "lightmaps", lightmapCheckBox.isSelected());
                }
            });

            cloudsCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Config.set(MCPatcherUtils.CUSTOM_COLORS, "clouds", cloudsCheckBox.isSelected());
                }
            });

            redstoneCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Config.set(MCPatcherUtils.CUSTOM_COLORS, "redstone", redstoneCheckBox.isSelected());
                }
            });

            stemCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Config.set(MCPatcherUtils.CUSTOM_COLORS, "stem", stemCheckBox.isSelected());
                }
            });

            eggCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Config.set(MCPatcherUtils.CUSTOM_COLORS, "egg", eggCheckBox.isSelected());
                }
            });

            mapCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Config.set(MCPatcherUtils.CUSTOM_COLORS, "map", mapCheckBox.isSelected());
                }
            });

            dyeCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Config.set(MCPatcherUtils.CUSTOM_COLORS, "dye", dyeCheckBox.isSelected());
                }
            });

            fogCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Config.set(MCPatcherUtils.CUSTOM_COLORS, "fog", fogCheckBox.isSelected());
                }
            });

            otherBlockCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Config.set(MCPatcherUtils.CUSTOM_COLORS, "otherBlocks", otherBlockCheckBox.isSelected());
                }
            });

            textCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Config.set(MCPatcherUtils.CUSTOM_COLORS, "text", textCheckBox.isSelected());
                }
            });

            xpOrbCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Config.set(MCPatcherUtils.CUSTOM_COLORS, "xporb", xpOrbCheckBox.isSelected());
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
                    Config.set(MCPatcherUtils.CUSTOM_COLORS, "fogBlendRadius", value);
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
                    Config.set(MCPatcherUtils.CUSTOM_COLORS, "blockBlendRadius", value);
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
            waterCheckBox.setSelected(Config.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "water", true));
            swampCheckBox.setSelected(Config.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "swamp", true));
            treeCheckBox.setSelected(Config.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "tree", true));
            potionCheckBox.setSelected(Config.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "potion", true));
            particleCheckBox.setSelected(Config.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "particle", true));
            lightmapCheckBox.setSelected(Config.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "lightmaps", true));
            cloudsCheckBox.setSelected(Config.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "clouds", true));
            redstoneCheckBox.setSelected(Config.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "redstone", true));
            stemCheckBox.setSelected(Config.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "stem", true));
            eggCheckBox.setSelected(Config.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "egg", true));
            mapCheckBox.setSelected(Config.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "map", true));
            dyeCheckBox.setSelected(Config.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "dye", true));
            fogCheckBox.setSelected(Config.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "fog", true));
            otherBlockCheckBox.setSelected(Config.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "otherBlocks", true));
            textCheckBox.setSelected(Config.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "text", true));
            xpOrbCheckBox.setSelected(Config.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "xporb", true));
            fogBlendRadiusSpinner.setValue(Config.getInt(MCPatcherUtils.CUSTOM_COLORS, "fogBlendRadius", 7));
            blockBlendRadiusSpinner.setValue(Config.getInt(MCPatcherUtils.CUSTOM_COLORS, "blockBlendRadius", 1));
        }

        @Override
        public void save() {
        }
    }

    private void addBlockColorPatch(com.prupe.mcpatcher.ClassMod classMod) {
        classMod.addPatch(new com.prupe.mcpatcher.BytecodePatch(classMod) {
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
                if (desc.contains("III)")) {
                    newMethod = colorizeBlock3;
                    args = new byte[]{ALOAD_1, ILOAD_2, ILOAD_3, ILOAD, 4};
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

    private class BlockMod extends BaseMod.BlockMod {
        BlockMod() {
            super(CustomColors.this);

            match0xffffff(getBlockColor);
            match0xffffff(getRenderColor);
            match0xffffff(colorMultiplier);

            addBlockColorPatch(this);
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

    private class BlockCauldronMod extends ClassMod {
        BlockCauldronMod() {
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
                new ConstSignature("_" + s)
            ));
        }
    }

    private class BlockFluidMod extends ClassMod {
        BlockFluidMod() {
            setParentClass("Block");

            addClassSignature(new ConstSignature("splash"));
            addClassSignature(new ConstSignature("liquid.water"));

            final MethodRef colorMultiplier = new MethodRef(getDeobfClass(), "colorMultiplier", "(LIBlockAccess;III)I");
            final FieldRef waterColorMultiplier = new FieldRef("BiomeGenBase", "waterColorMultiplier", "I");

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

            addBlockColorPatch(this);
        }
    }

    private class BlockLeavesBaseMod extends ClassMod {
        BlockLeavesBaseMod() {
            setParentClass("Block");
            addPrerequisiteClass("BlockLeaves");
        }
    }

    private class BlockLeavesMod extends ClassMod {
        BlockLeavesMod() {
            setParentClass("BlockLeavesBase");

            addLeafType("oak");
            addLeafType("spruce");
            addLeafType("birch");
            addLeafType("jungle");

            addBlockColorPatch(this);
        }

        private void addLeafType(String type) {
            addClassSignature(new ConstSignature(type));
            addClassSignature(new ConstSignature("leaves_" + type));
            addClassSignature(new ConstSignature("leaves_" + type + "_opaque"));
        }
    }

    private class BlockFlowerMod extends ClassMod {
        BlockFlowerMod() {
            setParentClass("Block");
            addPrerequisiteClass("BlockLilyPad");
        }
    }

    private class BlockLilyPadMod extends ClassMod {
        BlockLilyPadMod() {
            setParentClass("BlockFlower");

            addClassSignature(new ConstSignature(0x208030));
            addClassSignature(new ConstSignature(0.5f));
            addClassSignature(new ConstSignature(0.015625f));

            addBlockColorPatch(this);

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override fixed lily pad color";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(0x208030)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        reference(INVOKESTATIC, getLilyPadColor)
                    );
                }
            }.targetMethod(getBlockColor, getRenderColor, colorMultiplier));
        }
    }

    private class BlockTallGrassMod extends ClassMod {
        BlockTallGrassMod() {
            setParentClass("BlockFlower");

            addClassSignature(new ConstSignature("deadbush"));
            addClassSignature(new ConstSignature("tallgrass"));
            addClassSignature(new ConstSignature("fern"));
            addClassSignature(new ConstSignature("stone").negate(true));

            addBlockColorPatch(this);
        }
    }

    private class BlockVineMod extends ClassMod {
        private final MethodRef setBlockBoundsBasedOnState = new MethodRef(getDeobfClass(), "setBlockBoundsBasedOnState", "(LIBlockAccess;III)V");

        BlockVineMod() {
            setParentClass("Block");

            addClassSignature(new ConstSignature(0.0625f));
            addClassSignature(new ConstSignature(0.9375f));

            addAndSignature(2, 0.0625f);
            addAndSignature(8, 0.9375f);

            addBlockColorPatch(this);
        }

        private void addAndSignature(final int mask, final float bounds) {
            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        capture(anyILOAD),
                        push(mask),
                        IAND,
                        any(0, 10),
                        push(bounds)
                    );
                }
            }.setMethod(setBlockBoundsBasedOnState));
        }
    }

    private class ItemMod extends BaseMod.ItemMod {
        ItemMod() {
            super(CustomColors.this);

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
            setParentClass("Item");

            final MethodRef onItemUse = new MethodRef(getDeobfClass(), "onItemUse", "(LItemStack;LEntityPlayer;LWorld;IIIIFFF)Z");

            addClassSignature(new ConstSignature(0.5f));
            addClassSignature(new ConstSignature(0.8f));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(7),
                        IAND
                    );
                }
            }.setMethod(onItemUse));

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

            if (getMinecraftVersion().compareTo("13w36a") < 0) {
                final FieldRef blocksList = new FieldRef("Block", "blocksList", "[LBlock;");
                final FieldRef blockID = new FieldRef(getDeobfClass(), "blockID", "I");

                addClassSignature(new BytecodeSignature() {
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
                }
                    .matchConstructorOnly(true)
                    .addXref(1, blockID)
                );

                addPatch(new ItemBlockPatch() {
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

                addPatch(new ItemBlockPatch() {
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

        abstract private class ItemBlockPatch extends AddMethodPatch {
            public ItemBlockPatch() {
                super(getColorFromDamage);
            }

            @Override
            public byte[] generateMethod() {
                return buildCode(
                    // Block block = ...;
                    getBlockOnStack(),
                    ASTORE_3,

                    // if (block != null) {
                    ALOAD_3,
                    IFNULL, branch("A"),

                    // return block.getRenderColor(damage);
                    ALOAD_3,
                    ILOAD_2,
                    reference(INVOKEVIRTUAL, getRenderColor),
                    IRETURN,

                    // } else {
                    label("A"),

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

    private class ItemRendererMod extends ClassMod {
        ItemRendererMod() {
            final MethodRef renderItem = new MethodRef(getDeobfClass(), "renderItem", "(LEntityLivingBase;LItemStack;I)V");
            final MethodRef glTranslatef = new MethodRef(MCPatcherUtils.GL11_CLASS, "glTranslatef", "(FFF)V");
            final boolean haveItemID = getMinecraftVersion().compareTo("13w36a") < 0;
            final FieldRef itemID = haveItemID ? new FieldRef("ItemStack", "itemID", "I") : null;

            addClassSignature(new ConstSignature("textures/misc/enchanted_item_glint.png"));
            addClassSignature(new ConstSignature("textures/map/map_background.png"));

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
                }
                    .addXref(1, itemID)
                    .setMethod(renderItem)
                );
            }

            addPatch(new BytecodePatch() {
                private int blockRegister;

                {
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
            }
                .setInsertAfter(true)
                .targetMethod(renderItem)
            );
        }
    }

    private class PotionMod extends ClassMod {
        PotionMod() {
            final FieldRef potionID = new FieldRef(getDeobfClass(), "id", "I");
            final FieldRef color = new FieldRef(getDeobfClass(), "color", "I");
            final FieldRef origColor = new FieldRef(getDeobfClass(), "origColor", "I");
            final FieldRef potionName = new FieldRef(getDeobfClass(), "name", "Ljava/lang/String;");
            final MethodRef setPotionName = new MethodRef(getDeobfClass(), "setPotionName", "(Ljava/lang/String;)LPotion;");

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
                .setMethod(setPotionName)
                .addXref(1, potionName)
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
                .addXref(1, potionID)
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
                .addXref(1, color)
            );

            addPatch(new MakeMemberPublicPatch(potionName));
            addPatch(new AddFieldPatch(origColor));

            addPatch(new MakeMemberPublicPatch(color) {
                @Override
                public int getNewFlags(int oldFlags) {
                    return super.getNewFlags(oldFlags) & ~AccessFlag.FINAL;
                }
            });

            addPatch(new BytecodePatch() {
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
                public byte[] getReplacementBytes() {
                    return buildCode(
                        ALOAD_0,
                        reference(INVOKESTATIC, setupPotion)
                    );
                }
            }
                .setInsertBefore(true)
                .targetMethod(setPotionName)
            );
        }
    }

    private class PotionHelperMod extends ClassMod {
        private static final int MAGIC = 0x385dc6;

        PotionHelperMod() {
            final FieldRef potionColorCache = new FieldRef(getDeobfClass(), "potionColorCache", "Ljava/util/HashMap;");
            final MethodRef getPotionColor = new MethodRef(getDeobfClass(), "getPotionColor", "(IZ)I");
            final MethodRef integerValueOf = new MethodRef("java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;");
            final MethodRef hashMapContains = new MethodRef("java/util/HashMap", "containsKey", "(Ljava/lang/Object;)Z");

            addClassSignature(new ConstSignature("potion.prefix.mundane"));
            addClassSignature(new ConstSignature(MAGIC));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        captureReference(GETSTATIC),
                        ILOAD_0,
                        reference(INVOKESTATIC, integerValueOf),
                        reference(INVOKEVIRTUAL, hashMapContains)
                    );
                }
            }
                .setMethod(getPotionColor)
                .addXref(1, potionColorCache)
            );

            addPatch(new MakeMemberPublicPatch(potionColorCache));

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
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // int i = ColorizeItem.getWaterBottleColor();
                        reference(INVOKESTATIC, getWaterBottleColor),
                        ISTORE_1
                    );
                }
            });
        }
    }

    private class WorldMod extends BaseMod.WorldMod {
        WorldMod() {
            super(CustomColors.this);
            setInterfaces("IBlockAccess");

            final MethodRef getWorldChunkManager = new MethodRef(getDeobfClass(), "getWorldChunkManager", "()LWorldChunkManager;");

            addMemberMapper(new MethodMapper(getWorldChunkManager));

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
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // ColorizeWorld.setupForFog(entity);
                        ALOAD_1,
                        reference(INVOKESTATIC, setupForFog),

                        // if (ColorizeWorld.computeSkyColor(this, f)) {
                        ALOAD_0,
                        FLOAD_2,
                        reference(INVOKESTATIC, computeSkyColor),
                        IFEQ, branch("A"),

                        // f4 = Colorizer.setColor[0];
                        reference(GETSTATIC, setColor),
                        ICONST_0,
                        FALOAD,
                        FSTORE, getCaptureGroup(2),

                        // f5 = Colorizer.setColor[1];
                        reference(GETSTATIC, setColor),
                        ICONST_1,
                        FALOAD,
                        FSTORE, getCaptureGroup(3),

                        // f5 = Colorizer.setColor[2];
                        reference(GETSTATIC, setColor),
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

            final MethodRef getFogColor = new MethodRef(getDeobfClass(), "getFogColor", "(FF)LVec3D;");

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
        }
    }

    private class WorldProviderHellMod extends ClassMod {
        private static final double MAGIC1 = 0.20000000298023224;
        private static final double MAGIC2 = 0.029999999329447746;

        WorldProviderHellMod() {
            setParentClass("WorldProvider");

            final MethodRef getFogColor = new MethodRef(getDeobfClass(), "getFogColor", "(FF)LVec3D;");

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
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // ColorizeWorld.netherFogColor[0], ColorizeWorld.netherFogColor[1], ColorizeWorld.netherFogColor[2]
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

            final MethodRef getFogColor = new MethodRef(getDeobfClass(), "getFogColor", "(FF)LVec3D;");

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
                public byte[] getReplacementBytes() {
                    return buildCode(
                        reference(GETSTATIC, endFogColor),
                        ICONST_0,
                        FALOAD,
                        F2D,
                        reference(GETSTATIC, endFogColor),
                        ICONST_1,
                        FALOAD,
                        F2D,
                        reference(GETSTATIC, endFogColor),
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
            final FieldRef particleRed = new FieldRef(getDeobfClass(), "particleRed", "F");
            final FieldRef particleGreen = new FieldRef(getDeobfClass(), "particleGreen", "F");
            final FieldRef particleBlue = new FieldRef(getDeobfClass(), "particleBlue", "F");
            final FieldRef posX = new FieldRef(getDeobfClass(), "posX", "D");
            final FieldRef posY = new FieldRef(getDeobfClass(), "posY", "D");
            final FieldRef posZ = new FieldRef(getDeobfClass(), "posZ", "D");

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
                            reference(PUTFIELD, particleRed),

                            // particleGreen = g;
                            ALOAD_0,
                            push(origColors[1]),
                            reference(PUTFIELD, particleGreen),

                            // particleBlue = b;
                            ALOAD_0,
                            push(origColors[2]),
                            reference(PUTFIELD, particleBlue)
                        );
                    }
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // if (ColorizeBlock.computeWaterColor((int) this.posX, (int) this.posY, (int) this.posZ)) {
                        ALOAD_0,
                        reference(GETFIELD, posX),
                        D2I,
                        ALOAD_0,
                        reference(GETFIELD, posY),
                        D2I,
                        ALOAD_0,
                        reference(GETFIELD, posZ),
                        D2I,
                        reference(INVOKESTATIC, computeWaterColor1),
                        IFEQ, branch("A"),

                        // particleRed = Colorizer.setColor[0];
                        ALOAD_0,
                        reference(GETSTATIC, setColor),
                        ICONST_0,
                        FALOAD,
                        reference(PUTFIELD, particleRed),

                        // particleGreen = Colorizer.setColor[1];
                        ALOAD_0,
                        reference(GETSTATIC, setColor),
                        ICONST_1,
                        FALOAD,
                        reference(PUTFIELD, particleGreen),

                        // particleBlue = Colorizer.setColor[2];
                        ALOAD_0,
                        reference(GETSTATIC, setColor),
                        ICONST_2,
                        FALOAD,
                        reference(PUTFIELD, particleBlue),
                        GOTO, branch("B"),

                        // } else {
                        label("A"),

                        newColors == null ? new byte[]{} : buildCode(
                            // particleRed = r;
                            ALOAD_0,
                            push(newColors[0]),
                            reference(PUTFIELD, particleRed),

                            // particleGreen = g;
                            ALOAD_0,
                            push(newColors[1]),
                            reference(PUTFIELD, particleGreen),

                            // particleBlue = b;
                            ALOAD_0,
                            push(newColors[2]),
                            reference(PUTFIELD, particleBlue)
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

            final MethodRef random = new MethodRef("java/lang/Math", "random", "()D");
            final MethodRef nextInt = new MethodRef("java/util/Random", "nextInt", "(I)I");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // 0.2f * (float) Math.random() + 0.1f
                        reference(INVOKESTATIC, random),
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
                        push(4),
                        reference(INVOKEVIRTUAL, nextInt),
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

            final FieldRef particleRed = new FieldRef(getDeobfClass(), "particleRed", "F");
            final FieldRef particleGreen = new FieldRef(getDeobfClass(), "particleGreen", "F");
            final FieldRef particleBlue = new FieldRef(getDeobfClass(), "particleBlue", "F");
            final FieldRef timer = new FieldRef(getDeobfClass(), "timer", "I");
            final MethodRef onUpdate = new MethodRef(getDeobfClass(), "onUpdate", "()V");

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
                        reference(PUTFIELD, particleRed),

                        // particleGreen = 0.3f;
                        ALOAD_0,
                        push(0.3f),
                        reference(PUTFIELD, particleGreen),

                        // particleBlue = 1.0f;
                        ALOAD_0,
                        push(1.0f),
                        reference(PUTFIELD, particleBlue)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
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
                        reference(PUTFIELD, particleRed),

                        // particleGreen = 16.0f / (float)((40 - timer) + 16);
                        ALOAD_0,
                        push(16.0f),
                        any(0, 20),
                        reference(PUTFIELD, particleGreen),

                        // particleBlue = 4.0f / (float)((40 - timer) + 8);
                        ALOAD_0,
                        push(4.0f),
                        any(0, 20),
                        reference(PUTFIELD, particleBlue)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // if (Colorizer.computeLavaDropColor(40 - timer)) {
                        push(40),
                        ALOAD_0,
                        reference(GETFIELD, timer),
                        ISUB,
                        reference(INVOKESTATIC, computeLavaDropColor),
                        IFEQ, branch("A"),

                        // particleRed = Colorizer.setColor[0];
                        ALOAD_0,
                        reference(GETSTATIC, setColor),
                        ICONST_0,
                        FALOAD,
                        reference(PUTFIELD, particleRed),

                        // particleGreen = Colorizer.setColor[1];
                        ALOAD_0,
                        reference(GETSTATIC, setColor),
                        ICONST_1,
                        FALOAD,
                        reference(PUTFIELD, particleGreen),

                        // particleBlue = Colorizer.setColor[2];
                        ALOAD_0,
                        reference(GETSTATIC, setColor),
                        ICONST_2,
                        FALOAD,
                        reference(PUTFIELD, particleBlue),

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

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        DLOAD, 8,
                        anyReference(PUTFIELD),

                        ALOAD_0,
                        DLOAD, 10,
                        push(0.10000000000000001),
                        DADD,
                        anyReference(PUTFIELD),

                        ALOAD_0,
                        DLOAD, 12,
                        anyReference(PUTFIELD)
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

            final FieldRef particleRed = new FieldRef(getDeobfClass(), "particleRed", "F");
            final FieldRef particleGreen = new FieldRef(getDeobfClass(), "particleGreen", "F");
            final FieldRef particleBlue = new FieldRef(getDeobfClass(), "particleBlue", "F");

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
                        reference(PUTFIELD, particleRed),

                        ALOAD_0,
                        push(0.4f),
                        reference(PUTFIELD, particleGreen),

                        ALOAD_0,
                        push(0.7f),
                        reference(PUTFIELD, particleBlue)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // ColorizeEntity.computeSuspendColor(0x6666b2, (int) x, (int) y, (int) z);
                        push(0x6666b2),
                        DLOAD_2,
                        D2I,
                        DLOAD, 4,
                        D2I,
                        DLOAD, 6,
                        D2I,
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.COLORIZE_ENTITY_CLASS, "computeSuspendColor", "(IIII)V")),

                        // this.particleRed = Colorizer.setColor[0];
                        ALOAD_0,
                        reference(GETSTATIC, setColor),
                        push(0),
                        FALOAD,
                        reference(PUTFIELD, particleRed),

                        // this.particleGreen = Colorizer.setColor[1];
                        ALOAD_0,
                        reference(GETSTATIC, setColor),
                        push(1),
                        FALOAD,
                        reference(PUTFIELD, particleGreen),

                        // this.particleBlue = Colorizer.setColor[2];
                        ALOAD_0,
                        reference(GETSTATIC, setColor),
                        push(2),
                        FALOAD,
                        reference(PUTFIELD, particleBlue)
                    );
                }
            }.matchConstructorOnly(true));
        }
    }

    private class EntityPortalFXMod extends ClassMod {
        EntityPortalFXMod() {
            setParentClass("EntityFX");

            final FieldRef particleBlue = new FieldRef(getDeobfClass(), "particleBlue", "F");

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

            addPatch(new BytecodePatch() {
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
                public byte[] getReplacementBytes() {
                    return buildCode(
                        ALOAD_0,
                        reference(GETSTATIC, portalColor),
                        ICONST_2,
                        FALOAD,
                        reference(PUTFIELD, particleBlue)
                    );
                }
            }
                .setInsertBefore(true)
                .matchConstructorOnly(true)
            );
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
                public byte[] getReplacementBytes() {
                    return buildCode(
                        reference(GETSTATIC, portalColor),
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

            final FieldRef particleRed = new FieldRef(getDeobfClass(), "particleRed", "F");
            final FieldRef particleGreen = new FieldRef(getDeobfClass(), "particleGreen", "F");
            final FieldRef particleBlue = new FieldRef(getDeobfClass(), "particleBlue", "F");

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
                public byte[] generateMethod() {
                    return buildCode(
                        reference(INVOKESTATIC, computeMyceliumParticleColor),
                        IFEQ, branch("A"),

                        ALOAD_0,
                        reference(GETSTATIC, setColor),
                        ICONST_0,
                        FALOAD,
                        reference(PUTFIELD, particleRed),

                        ALOAD_0,
                        reference(GETSTATIC, setColor),
                        ICONST_1,
                        FALOAD,
                        reference(PUTFIELD, particleGreen),

                        ALOAD_0,
                        reference(GETSTATIC, setColor),
                        ICONST_2,
                        FALOAD,
                        reference(PUTFIELD, particleBlue),

                        label("A"),
                        ALOAD_0,
                        ARETURN
                    );
                }
            });
        }
    }

    private class EntityLivingBaseMod extends BaseMod.EntityLivingBaseMod {
        public EntityLivingBaseMod() {
            super(CustomColors.this);

            final FieldRef worldObj = new FieldRef(getDeobfClass(), "worldObj", "LWorld;");
            final FieldRef overridePotionColor = new FieldRef(getDeobfClass(), "overridePotionColor", "I");
            final MethodRef updatePotionEffects = new MethodRef(getDeobfClass(), "updatePotionEffects", "()V");
            final MethodRef integerValueOf = new MethodRef("java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(600),
                        IREM
                    );
                }
            }.setMethod(updatePotionEffects));

            addPatch(new AddFieldPatch(overridePotionColor));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override potion effect colors around players (part 1)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // if (this.potionsNeedUpdate) {
                        lookBehind(build(
                            ALOAD_0,
                            GETFIELD, capture(any(2)),
                            IFEQ, any(2)
                        ), true),

                        // if (!this.worldObj.isRemote) {
                        ALOAD_0,
                        reference(GETFIELD, worldObj),
                        captureReference(GETFIELD),
                        IFNE, any(2)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                    );
                }
            }.targetMethod(updatePotionEffects));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override potion effect colors around players (part 2)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // this.dataWatcher.updateObject(7, Integer.valueOf(...));
                        ALOAD_0,
                        anyReference(GETFIELD),
                        push(7),
                        capture(any(1, 3)),
                        reference(INVOKESTATIC, integerValueOf),
                        anyReference(INVOKEVIRTUAL)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // this.overridePotionColor = ...;
                        ALOAD_0,
                        getCaptureGroup(1),
                        reference(PUTFIELD, overridePotionColor)
                    );
                }
            }
                .targetMethod(updatePotionEffects)
                .setInsertAfter(true)
            );

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override potion effect colors around players (part 3)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // this.dataWatcher.getWatchableObjectInt(7)
                        ALOAD_0,
                        anyReference(GETFIELD),
                        push(7),
                        anyReference(INVOKEVIRTUAL)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // ColorizeEntity.getPotionEffectColor(..., this)
                        ALOAD_0,
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.COLORIZE_ENTITY_CLASS, "getPotionEffectColor", "(ILEntityLivingBase;)I"))
                    );
                }
            }
                .setInsertAfter(true)
                .targetMethod(updatePotionEffects)
            );
        }
    }

    private class EntityRendererMod extends ClassMod {
        EntityRendererMod() {
            final MethodRef updateLightmap = new MethodRef(getDeobfClass(), "updateLightmap", "(F)V");
            final FieldRef mc = new FieldRef(getDeobfClass(), "mc", "LMinecraft;");
            final MethodRef updateFogColor = new MethodRef(getDeobfClass(), "updateFogColor", "(F)V");
            final FieldRef fogColorRed = new FieldRef(getDeobfClass(), "fogColorRed", "F");
            final FieldRef fogColorGreen = new FieldRef(getDeobfClass(), "fogColorGreen", "F");
            final FieldRef fogColorBlue = new FieldRef(getDeobfClass(), "fogColorBlue", "F");
            final FieldRef lightmapColors = new FieldRef(getDeobfClass(), "lightmapColors", "[I");
            final FieldRef lightmapTexture = new FieldRef(getDeobfClass(), "lightmapTexture", "LDynamicTexture;");
            final FieldRef needLightmapUpdate = new FieldRef(getDeobfClass(), "needLightmapUpdate", "Z");
            final FieldRef thePlayer = new FieldRef("Minecraft", "thePlayer", "LEntityClientPlayerMP;");
            final FieldRef nightVision = new FieldRef("Potion", "nightVision", "LPotion;");
            final MethodRef isPotionActive = new MethodRef("EntityClientPlayerMP", "isPotionActive", "(LPotion;)Z");
            final MethodRef getNightVisionStrength1 = new MethodRef(getDeobfClass(), "getNightVisionStrength1", "(LEntityPlayer;F)F");
            final MethodRef getNightVisionStrength = new MethodRef(getDeobfClass(), "getNightVisionStrength", "(F)F");
            final MethodRef reloadTexture = new MethodRef("DynamicTexture", "reload", "()V");

            addClassSignature(new ConstSignature("ambient.weather.rain"));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // sun = world.func_35464_b(1.0F) * 0.95F + 0.05F;
                        ALOAD_2,
                        push(1.0f),
                        captureReference(INVOKEVIRTUAL),
                        push(0.95f),
                        FMUL,
                        push(0.05f),
                        FADD,
                        FSTORE, 4,

                        // lightsun = world.worldProvider.lightBrightnessTable[i / 16] * sun;
                        ALOAD_2,
                        captureReference(GETFIELD),
                        captureReference(GETFIELD),
                        ILOAD_3,
                        BIPUSH, 16,
                        IDIV,
                        FALOAD,
                        FLOAD, 4,
                        FMUL,
                        FSTORE, 5,

                        // lighttorch = world.worldProvider.lightBrightnessTable[i % 16] * (torchFlickerX * 0.1F + 1.5F);
                        any(0, 20),
                        ILOAD_3,
                        BIPUSH, 16,
                        IREM,
                        FALOAD,
                        ALOAD_0,
                        captureReference(GETFIELD),

                        // ...
                        any(0, 200),

                        // if (world.lightningFlash > 0)
                        ALOAD_2,
                        captureReference(GETFIELD),
                        IFLE, any(2),

                        // ...
                        any(0, 300),

                        // if (world.worldProvider.worldType == 1) {
                        ALOAD_2,
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
                        FSTORE, 16,

                        // ...
                        any(0, 300),

                        // this.lightmapColors[i] = ...;
                        ALOAD_0,
                        captureReference(GETFIELD),
                        any(0, 50),
                        IASTORE,

                        // ...
                        any(0, 20),

                        // this.lightmapTexture.load();
                        // this.needLightmapUpdate = false;
                        ALOAD_0,
                        captureReference(GETFIELD),
                        captureReference(INVOKEVIRTUAL),
                        ALOAD_0,
                        push(0),
                        captureReference(PUTFIELD),
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
                .addXref(10, lightmapColors)
                .addXref(11, lightmapTexture)
                .addXref(12, reloadTexture)
                .addXref(13, needLightmapUpdate)
            );

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // fogColorRed = 0.02f;
                        ALOAD_0,
                        push(0.02f),
                        capture(optional(build( // 13w16a+
                            anyFLOAD,
                            FADD
                        ))),
                        captureReference(PUTFIELD),

                        // fogColorGreen = 0.02f;
                        ALOAD_0,
                        push(0.02f),
                        backReference(1),
                        captureReference(PUTFIELD),

                        // fogColorBlue = 0.2f;
                        ALOAD_0,
                        push(0.2f),
                        backReference(1),
                        captureReference(PUTFIELD)
                    );
                }
            }
                .setMethod(updateFogColor)
                .addXref(2, fogColorRed)
                .addXref(3, fogColorGreen)
                .addXref(4, fogColorBlue)
            );

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

            addPatch(new AddMethodPatch(getNightVisionStrength) {
                @Override
                public byte[] generateMethod() {
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
                }
            });

            addPatch(new MakeMemberPublicPatch(new FieldRef(getDeobfClass(), "torchFlickerX", "F")));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override lightmap";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ASTORE_2
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // if (Lightmap.computeLightmap(this, world, this.lightmapColors, partialTick)) {
                        ALOAD_0,
                        ALOAD_2,
                        ALOAD_0,
                        reference(GETFIELD, lightmapColors),
                        FLOAD_1,
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.LIGHTMAP_CLASS, "computeLightmap", "(LEntityRenderer;LWorld;[IF)Z")),
                        IFEQ, branch("A"),

                        // this.lightmapTexture.load();
                        // this.needLightmapUpdate = false;
                        ALOAD_0,
                        reference(GETFIELD, lightmapTexture),
                        reference(INVOKEVIRTUAL, reloadTexture),
                        ALOAD_0,
                        push(0),
                        reference(PUTFIELD, needLightmapUpdate),

                        // return;
                        RETURN,

                        // }
                        label("A")
                    );
                }
            }
                .setInsertAfter(true)
                .targetMethod(updateLightmap)
            );

            addPatch(new BytecodePatch() {
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
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // ColorizeWorld.setupForFog(entityliving);
                        ALOAD_3,
                        reference(INVOKESTATIC, setupForFog),

                        // if (ColorizeWorld.computeFogColor(world, f)) {
                        ALOAD_2,
                        FLOAD_1,
                        reference(INVOKESTATIC, computeFogColor),
                        IFEQ, branch("A"),

                        // fogColorRed = Colorizer.setColor[0];
                        ALOAD_0,
                        reference(GETSTATIC, setColor),
                        ICONST_0,
                        FALOAD,
                        reference(PUTFIELD, fogColorRed),

                        // fogColorGreen = Colorizer.setColor[1];
                        ALOAD_0,
                        reference(GETSTATIC, setColor),
                        ICONST_1,
                        FALOAD,
                        reference(PUTFIELD, fogColorGreen),

                        // fogColorBlue = Colorizer.setColor[2];
                        ALOAD_0,
                        reference(GETSTATIC, setColor),
                        ICONST_2,
                        FALOAD,
                        reference(PUTFIELD, fogColorBlue),

                        // }
                        label("A")
                    );
                }
            }
                .setInsertBefore(true)
                .targetMethod(updateFogColor)
            );

            addPatch(new BytecodePatch() {
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
                        capture(optional(build( // 13w16a+
                            anyFLOAD,
                            FADD
                        ))),
                        reference(PUTFIELD, fogColorRed),

                        // fogColorGreen = 0.02f;
                        ALOAD_0,
                        push(0.02f),
                        backReference(1),
                        reference(PUTFIELD, fogColorGreen),

                        // fogColorBlue = 0.2f;
                        ALOAD_0,
                        push(0.2f),
                        backReference(1),
                        reference(PUTFIELD, fogColorBlue)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // if (ColorizeWorld.computeUnderwaterColor()) {
                        reference(INVOKESTATIC, computeUnderwaterColor),
                        IFEQ, branch("A"),

                        // fogColorRed = Colorizer.setColor[0];
                        ALOAD_0,
                        reference(GETSTATIC, setColor),
                        push(0),
                        FALOAD,
                        getCaptureGroup(1),
                        reference(PUTFIELD, fogColorRed),

                        // fogColorGreen = Colorizer.setColor[1];
                        ALOAD_0,
                        reference(GETSTATIC, setColor),
                        push(1),
                        FALOAD,
                        getCaptureGroup(1),
                        reference(PUTFIELD, fogColorGreen),

                        // fogColorBlue = Colorizer.setColor[2];
                        ALOAD_0,
                        reference(GETSTATIC, setColor),
                        push(2),
                        FALOAD,
                        getCaptureGroup(1),
                        reference(PUTFIELD, fogColorBlue),

                        // }
                        label("A")
                    );
                }
            }
                .setInsertAfter(true)
                .targetMethod(updateFogColor)
            );
        }
    }

    private void setupRedstoneWire(com.prupe.mcpatcher.ClassMod classMod, final String description, final MethodRef method) {
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

    private class BlockRedstoneWireMod extends ClassMod {
        BlockRedstoneWireMod() {
            setParentClass("Block");

            final MethodRef randomDisplayTick = new MethodRef("BlockRedstoneWire", "randomDisplayTick", "(LWorld;IIILjava/util/Random;)V");

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
                        ILOAD_2,
                        ILOAD_3,
                        ILOAD, 4,
                        push(0x800000),
                        reference(INVOKESTATIC, colorizeRedstoneWire)
                    );
                }
            }.targetMethod(new MethodRef(getDeobfClass(), "colorMultiplier", "(LIBlockAccess;III)I")));
        }
    }

    private class RenderBlocksMod extends BaseMod.RenderBlocksMod {
        private final FieldRef tessellator = new FieldRef("Tessellator", "instance", "LTessellator;");
        private final MethodRef renderBlockFluids = new MethodRef(getDeobfClass(), "renderBlockFluids", "(LBlock;III)Z");
        private final MethodRef setColorOpaque_F = new MethodRef("Tessellator", "setColorOpaque_F", "(FFF)V");
        private final MethodRef addVertexWithUV = new MethodRef("Tessellator", "addVertexWithUV", "(DDDDD)V");

        RenderBlocksMod() {
            super(CustomColors.this);

            addClassSignature(new ConstSignature(0.1875));
            addClassSignature(new ConstSignature(0.01));

            final MethodRef renderBlockByRenderType = new MethodRef(getDeobfClass(), "renderBlockByRenderType", "(LBlock;III)Z");
            final MethodRef renderBlockFallingSand = new MethodRef(getDeobfClass(), "renderBlockFallingSand", "(LBlock;LWorld;IIII)V");
            final MethodRef renderBlockCauldron = new MethodRef(getDeobfClass(), "renderBlockCauldron", "(LBlockCauldron;III)Z");
            final MethodRef renderBlockRedstoneWire = new MethodRef(getDeobfClass(), "renderBlockRedstoneWire", "(LBlock;III)Z");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // renderType == 4 ? this.renderBlockFluids(block, i, j, k) : ...
                        ILOAD, 5,
                        push(4),
                        IF_ICMPNE, any(2),
                        ALOAD_0,
                        ALOAD_1,
                        ILOAD_2,
                        ILOAD_3,
                        ILOAD, 4,
                        captureReference(INVOKEVIRTUAL),
                        subset(new int[]{GOTO, IRETURN}, true)
                    );
                }
            }
                .setMethod(renderBlockByRenderType)
                .addXref(1, renderBlockFluids)
            );

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        begin(),
                        push(0.5f),
                        FSTORE, 7,
                        FCONST_1,
                        FSTORE, 8,
                        push(0.8f),
                        FSTORE, 9,
                        push(0.6f),
                        FSTORE, 10
                    );
                }
            }.setMethod(renderBlockFallingSand));

            setupRedstoneWire(this, "override redstone wire color", renderBlockRedstoneWire);

            addMemberMapper(new MethodMapper(renderBlockCauldron));

            addPatch(new TessellatorPatch() {
                @Override
                public String getDescription() {
                    return "colorize cauldron water";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push("water_still"),
                        anyReference(INVOKESTATIC),
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
            }
                .setInsertAfter(true)
                .targetMethod(renderBlockCauldron)
            );

            addPatch(new TessellatorPatch() {
                private boolean done;

                @Override
                public String getDescription() {
                    return "colorize falling sand and gravel";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // tessellator.setColorOpaque_F($1 * f5, $1 * f5, $1 * f5);
                        ALOAD, tessellatorRegister,
                        capture(anyFLOAD),
                        capture(anyFLOAD),
                        FMUL,
                        backReference(1),
                        backReference(2),
                        FMUL,
                        backReference(1),
                        backReference(2),
                        FMUL,
                        reference(INVOKEVIRTUAL, setColorOpaque_F)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    byte[] extraCode;
                    if (done) {
                        extraCode = new byte[0];
                    } else {
                        done = true;
                        extraCode = buildCode(
                            // setColorF(ColorizeBlock.getColorMultiplier(block, i, j, k));
                            ALOAD_1,
                            ILOAD_3,
                            ILOAD, 4,
                            ILOAD, 5,
                            reference(INVOKESTATIC, getNewColorMultiplier),
                            reference(INVOKESTATIC, setColorF)
                        );
                    }
                    return buildCode(
                        extraCode,

                        // tessellator.setColorOpaque_F(Colorizer.setColor[0] * f5, Colorizer.setColor[1] * f5, Colorizer.setColor[2] * f5);
                        ALOAD, tessellatorRegister,
                        reference(GETSTATIC, setColor),
                        push(0),
                        FALOAD,
                        getCaptureGroup(2),
                        FMUL,
                        reference(GETSTATIC, setColor),
                        push(1),
                        FALOAD,
                        getCaptureGroup(2),
                        FMUL,
                        reference(GETSTATIC, setColor),
                        push(2),
                        FALOAD,
                        getCaptureGroup(2),
                        FMUL,
                        reference(INVOKEVIRTUAL, setColorOpaque_F)
                    );
                }
            }.targetMethod(renderBlockFallingSand));

            addPatch(new TessellatorPatch() {
                private int[] waterRegisters;

                {
                    addPreMatchSignature(new BytecodeSignature() {
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
                        ALOAD, tessellatorRegister,
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
                        ALOAD, tessellatorRegister,
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

            setupBTW();
            setupBiomeSmoothing();
        }

        private void setupBiomeSmoothing() {
            final FieldRef enableAO = new FieldRef(getDeobfClass(), "enableAO", "Z");
            final MethodRef setupBiomeSmoothing1 = new MethodRef(MCPatcherUtils.COLORIZE_BLOCK_CLASS, "setupBiomeSmoothing1", "(LRenderBlocks;LBlock;LIBlockAccess;IIIIZZFFFFFFF)Z");
            final MethodRef setupBiomeSmoothing2 = new MethodRef(MCPatcherUtils.COLORIZE_BLOCK_CLASS, "setupBiomeSmoothing2", "(LRenderBlocks;LBlock;LIBlockAccess;IIIIZZFFFFFFF)V");

            final String[] vertexNames = new String[]{"TopLeft", "BottomLeft", "BottomRight", "TopRight"};
            final String[] colorNames = new String[]{"Red", "Green", "Blue"};
            final FieldRef[] vertexColorFields = new FieldRef[12];

            addClassSignature(new BytecodeSignature() {
                {
                    addXref(1, enableAO);

                    int i = 0;
                    for (String v : vertexNames) {
                        for (String c : colorNames) {
                            vertexColorFields[i] = new FieldRef(getDeobfClass(), "color" + c + v, "F");
                            i++;
                        }
                    }
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // if (this.enableAO)
                        ALOAD_0,
                        captureReference(GETFIELD),
                        IFEQ_or_IFNE, any(2),

                        nonGreedy(any(0, 200)),

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
                    // this.brightnessxxx
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
                        repeat(anyDLOAD, 5),
                        captureReference(INVOKEVIRTUAL)
                    );
                }
            });

            for (FieldRef field : vertexColorFields) {
                addPatch(new MakeMemberPublicPatch(field));
            }

            addPatch(new BytecodePatch() {
                private int patchCount;
                private int useColor;

                {
                    addPreMatchSignature(new BytecodeSignature() {
                        @Override
                        public String getMatchExpression() {
                            return buildExpression(
                                push(1),
                                capture(anyISTORE),
                                any(0, 50),
                                push(0),
                                backReference(1)
                            );
                        }

                        @Override
                        public boolean afterMatch() {
                            useColor = extractRegisterNum(getCaptureGroup(1));
                            return true;
                        }
                    });
                }

                @Override
                public String getDescription() {
                    return "smooth biome colors";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // this.colorRedTopLeft *= topLeft;
                        getSubExpression(0),
                        any(0, 80),
                        // this.colorRedBottomLeft *= bottomLeft;
                        getSubExpression(3),
                        any(0, 80),
                        // this.colorRedBottomRight *= bottomRight;
                        getSubExpression(6),
                        any(0, 80),
                        // this.colorBlueTopRight *= topRight;
                        getSubExpression(11)
                    );
                }

                private String getSubExpression(int index) {
                    return build(
                        ALOAD_0,
                        DUP,
                        reference(GETFIELD, vertexColorFields[index]),
                        capture(anyFLOAD),
                        FMUL,
                        reference(PUTFIELD, vertexColorFields[index])
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // if (!ColorizeBlock.setupBiomeSmoothing1(this, block, this.blockAccess,
                        //                                         i, j, k, face,
                        //                                         useColor, true, r, g, b,
                        //                                         topLeft, bottomLeft, bottomRight, topRight)) {
                        ALOAD_0,
                        ALOAD_1,
                        ALOAD_0,
                        reference(GETFIELD, blockAccess),

                        ILOAD_2,
                        ILOAD_3,
                        ILOAD, 4,
                        push(patchCount++ % 6),

                        ILOAD, useColor,
                        push(1),
                        FLOAD, 5,
                        FLOAD, 6,
                        FLOAD, 7,

                        getCaptureGroup(1),
                        getCaptureGroup(2),
                        getCaptureGroup(3),
                        getCaptureGroup(4),

                        reference(INVOKESTATIC, setupBiomeSmoothing1),
                        IFNE, branch("A"),

                        // ...
                        getMatch(),

                        // }
                        label("A")
                    );
                }
            });

            addPatch(new BytecodePatch() {
                private int patchCount;
                private int faceRegister;

                {
                    addPreMatchSignature(new BytecodeSignature() {
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
                        capture(anyFLOAD),
                        FMUL,

                        backReference(1),
                        capture(anyFLOAD),
                        FMUL,

                        backReference(1),
                        capture(anyFLOAD),
                        FMUL,

                        reference(INVOKEVIRTUAL, setColorOpaque_F)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    byte[] faceCode;
                    switch (patchCount++) {
                        case 0:
                            faceCode = new byte[]{ICONST_1}; // top face
                            break;

                        case 1:
                            faceCode = new byte[]{ICONST_0}; // bottom face
                            break;

                        case 2:
                            faceCode = buildCode(
                                registerLoadStore(ILOAD, faceRegister),
                                push(2),
                                IADD
                            ); // other faces
                            break;

                        default:
                            return null;
                    }
                    return buildCode(
                        // ColorizeBlock.setupBiomeSmoothing2(this, block, this.blockAccess,
                        //                                    i, j, k, face + 6,
                        //                                    useColor, false, r, g, b,
                        //                                    topLeft, bottomLeft, bottomRight, topRight);
                        ALOAD_0,
                        ALOAD_1,
                        ALOAD_0,
                        reference(GETFIELD, blockAccess),

                        ILOAD_2,
                        ILOAD_3,
                        ILOAD, 4,
                        faceCode,
                        push(6),
                        IADD,

                        push(1),
                        push(0),
                        getCaptureGroup(2),
                        getCaptureGroup(3),
                        getCaptureGroup(4),

                        getCaptureGroup(1),
                        DUP,
                        DUP,
                        DUP,

                        reference(INVOKESTATIC, setupBiomeSmoothing2)
                    );
                }
            }.targetMethod(renderBlockFluids));

            addPatch(new BytecodePatch() {
                private int patchCount;

                @Override
                public String getDescription() {
                    return "smooth biome colors (water part 2)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // tessellator.addVertexWithUV(...);
                        capture(anyALOAD),
                        nonGreedy(any(0, 40)),
                        reference(INVOKEVIRTUAL, addVertexWithUV)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // tessellator.setColorOpaque_F(this.colorRedxxx, this.colorGreenxxx, this.colorBluexxx);
                        getCaptureGroup(1),
                        getVertex(patchCount++),
                        reference(INVOKEVIRTUAL, setColorOpaque_F)
                    );
                }

                private byte[] getVertex(int index) {
                    return buildCode(
                        getVertexColor(index, 0),
                        getVertexColor(index, 1),
                        getVertexColor(index, 2)
                    );
                }

                private byte[] getVertexColor(int vertex, int channel) {
                    return buildCode(
                        ALOAD_0,
                        reference(GETFIELD, new FieldRef(getDeobfClass(), "color" + colorNames[channel % 3] + vertexNames[vertex % 4], "F"))
                    );
                }
            }
                .setInsertBefore(true)
                .targetMethod(renderBlockFluids)
            );

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "smooth biome colors (water part 3)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // this.renderFaceYNeg(block, (double) i, (double) j + var32, (double) k, this.getBlockIconFromSide(block, 0));
                        ALOAD_0,
                        ALOAD_1,
                        ILOAD_2,
                        I2D,
                        ILOAD_3,
                        I2D,
                        anyDLOAD,
                        DADD,
                        ILOAD, 4,
                        I2D,
                        ALOAD_0,
                        ALOAD_1,
                        push(0),
                        anyReference(INVOKEVIRTUAL),
                        anyReference(INVOKEVIRTUAL)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // this.enableAO = true; TODO: check isAmbientOcclusionEnabled()
                        ALOAD_0,
                        push(1),
                        reference(PUTFIELD, enableAO),

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

        private void setupBTW() {
            final MethodRef renderBlock = new MethodRef(getDeobfClass(), "RenderStandardFullBlock", "(LBlock;III)Z");
            final MethodRef renderBlockAO = new MethodRef(getDeobfClass(), "RenderStandardFullBlockWithAmbientOcclusion", "(LBlock;III)Z");
            final MethodRef renderBlockCM = new MethodRef(getDeobfClass(), "RenderStandardFullBlockWithColorMultiplier", "(LBlock;III)Z");

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "setup block color multiplier (btw)";
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
                        ALOAD_1,
                        ALOAD_0,
                        reference(GETFIELD, blockAccess),
                        ILOAD_2,
                        ILOAD_3,
                        ILOAD, 4,
                        reference(INVOKEVIRTUAL, colorMultiplier),
                        reference(INVOKESTATIC, setColorF)
                    );
                }
            }.targetMethod(renderBlock));

            addPatch(new BytecodePatch() {
                private int patchCount;

                @Override
                public String getDescription() {
                    return "use block color multiplier (btw)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(or(
                        build(push(0.5f)),
                        build(push(0.6f)),
                        build(push(0.8f)),
                        build(push(1.0f))
                    ));
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        reference(GETSTATIC, setColor),
                        push(patchCount++ % 3),
                        FALOAD,
                        FMUL
                    );
                }
            }
                .setInsertAfter(true)
                .targetMethod(renderBlockAO, renderBlockCM)
            );
        }

        abstract private class TessellatorPatch extends BytecodePatch {
            protected int tessellatorRegister;

            {
                addPreMatchSignature(new BytecodeSignature() {
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
    }

    private class EntityReddustFXMod extends ClassMod {
        EntityReddustFXMod() {
            final MethodRef random = new MethodRef("java/lang/Math", "random", "()D");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        reference(INVOKESTATIC, random),
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
                        push(1.0f),
                        FSTORE, 9,
                        reference(INVOKESTATIC, random)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        push(1.0f),
                        FSTORE, 9,

                        push(15),
                        reference(INVOKESTATIC, computeRedstoneWireColor),
                        IFEQ, branch("A"),

                        reference(GETSTATIC, setColor),
                        push(0),
                        FALOAD,
                        FSTORE, 9,
                        reference(GETSTATIC, setColor),
                        push(1),
                        FALOAD,
                        FSTORE, 10,
                        reference(GETSTATIC, setColor),
                        push(2),
                        FALOAD,
                        FSTORE, 11,

                        label("A"),
                        reference(INVOKESTATIC, random)
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

            addPatch(new BytecodePatch() {
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
                public byte[] getReplacementBytes() {
                    return buildCode(
                        ALOAD_0,
                        ILOAD_1,
                        reference(INVOKESTATIC, colorizeStem)
                    );
                }
            }
                .setInsertBefore(true)
                .targetMethod(getRenderColor)
            );
        }
    }

    private class RenderGlobalMod extends ClassMod {
        RenderGlobalMod() {
            final FieldRef clouds = new FieldRef(getDeobfClass(), "clouds", "LResourceLocation;");
            final FieldRef mc = new FieldRef(getDeobfClass(), "mc", "LMinecraft;");
            final FieldRef gameSettings = new FieldRef("Minecraft", "gameSettings", "LGameSettings;");
            final FieldRef fancyGraphics = new FieldRef("GameSettings", "fancyGraphics", "Z");
            final MethodRef renderClouds = new MethodRef(getDeobfClass(), "renderClouds", "(F)V");
            final MethodRef renderSky = new MethodRef(getDeobfClass(), "renderSky", "(F)V");
            final MethodRef glRotatef = new MethodRef(MCPatcherUtils.GL11_CLASS, "glRotatef", "(FFFF)V");

            addClassSignature(new BaseMod.ResourceLocationSignature(this, clouds, "textures/environment/clouds.png"));

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

                        // ...(RenderGlobal.clouds);
                        captureReference(GETSTATIC)
                    );
                }
            }
                .setMethod(renderClouds)
                .addXref(1, mc)
                .addXref(2, gameSettings)
                .addXref(3, fancyGraphics)
                .addXref(4, clouds)
            );

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(90.0f),
                        push(1.0f),
                        push(0.0f),
                        push(0.0f),
                        reference(INVOKESTATIC, glRotatef)
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
                public byte[] getReplacementBytes() {
                    return buildCode(
                        getCaptureGroup(1),
                        reference(INVOKESTATIC, drawFancyClouds),
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
                public byte[] getReplacementBytes() {
                    return buildCode(
                        reference(GETSTATIC, endSkyColor)
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
                public byte[] getReplacementBytes() {
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
                public byte[] getReplacementBytes() {
                    return buildCode(
                        ALOAD_1,
                        ILOAD_2,
                        ILOAD_3,
                        ILOAD, 4,
                        reference(INVOKESTATIC, setupSpawnerEgg)
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

            addPatch(new BytecodePatch() {
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
                public byte[] getReplacementBytes() {
                    return buildCode(
                        ALOAD_1,
                        reference(INVOKEVIRTUAL, getItemDamage),
                        ILOAD_2,
                        reference(INVOKESTATIC, colorizeSpawnerEgg)
                    );
                }
            }
                .setInsertBefore(true)
                .targetMethod(getColorFromDamage2)
            );
        }
    }

    private class MapColorMod extends ClassMod {
        MapColorMod() {
            final FieldRef mapColorArray = new FieldRef(getDeobfClass(), "mapColorArray", "[LMapColor;");
            final FieldRef colorValue = new FieldRef(getDeobfClass(), "colorValue", "I");
            final FieldRef colorIndex = new FieldRef(getDeobfClass(), "colorIndex", "I");
            final FieldRef origColorValue = new FieldRef(getDeobfClass(), "origColorValue", "I");

            addClassSignature(new ConstSignature(0x7fb238));
            addClassSignature(new ConstSignature(0xf7e9a3));
            addClassSignature(new ConstSignature(0xa7a7a7));
            addClassSignature(new ConstSignature(0xff0000));
            addClassSignature(new ConstSignature(0xa0a0ff));

            addMemberMapper(new FieldMapper(mapColorArray).accessFlag(AccessFlag.STATIC, true));
            addMemberMapper(new FieldMapper(colorValue, colorIndex).accessFlag(AccessFlag.STATIC, false));

            addPatch(new AddFieldPatch(origColorValue));

            addPatch(new MakeMemberPublicPatch(colorValue) {
                @Override
                public int getNewFlags(int oldFlags) {
                    return oldFlags & ~AccessFlag.FINAL;
                }
            });

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "set map origColorValue";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        ILOAD_2,
                        reference(PUTFIELD, colorValue)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        ALOAD_0,
                        ILOAD_2,
                        reference(PUTFIELD, origColorValue)
                    );
                }
            }
                .setInsertAfter(true)
                .targetMethod(new MethodRef(getDeobfClass(), "<init>", "(II)V"))
            );
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
                public byte[] getReplacementBytes() {
                    return buildCode(
                        reference(GETSTATIC, undyedLeatherColor)
                    );
                }
            });
        }
    }

    private class EntitySheepMod extends ClassMod {
        EntitySheepMod() {
            addClassSignature(new ConstSignature("mob.sheep.say"));

            addMemberMapper(new FieldMapper(fleeceColorTable)
                .accessFlag(AccessFlag.PUBLIC, true)
                .accessFlag(AccessFlag.STATIC, true)
            );
        }
    }

    private class RenderWolfMod extends ClassMod {
        RenderWolfMod() {
            final MethodRef glColor3f = new MethodRef(MCPatcherUtils.GL11_CLASS, "glColor3f", "(FFF)V");

            setParentClass("RenderLivingEntity");

            addClassSignature(new ConstSignature("textures/entity/wolf/wolf_collar.png"));
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
                public byte[] getReplacementBytes() {
                    return buildCode(
                        reference(GETSTATIC, collarColors)
                    );
                }
            });
        }
    }

    private class RecipesDyedArmorMod extends ClassMod {
        RecipesDyedArmorMod() {
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
                public byte[] getReplacementBytes() {
                    return buildCode(
                        reference(GETSTATIC, armorColors)
                    );
                }
            });
        }
    }

    private class FontRendererMod extends BaseMod.FontRendererMod {
        FontRendererMod() {
            super(CustomColors.this);

            final MethodRef renderString = new MethodRef(getDeobfClass(), "renderString", "(Ljava/lang/String;IIIZ)I");
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

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override text color";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ILOAD, 4,
                        push(0xfc000000),
                        IAND
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        ILOAD, 4,
                        reference(INVOKESTATIC, colorizeText1),
                        ISTORE, 4
                    );
                }
            }
                .setInsertBefore(true)
                .targetMethod(renderString)
            );

            addPatch(new BytecodePatch() {
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
                public byte[] getReplacementBytes() {
                    return buildCode(
                        getCaptureGroup(1),
                        reference(INVOKESTATIC, colorizeText2)
                    );
                }
            }.setInsertAfter(true));
        }
    }

    private class TileEntitySignRendererMod extends ClassMod {
        TileEntitySignRendererMod() {
            final FieldRef sign = new FieldRef(getDeobfClass(), "sign", "LResourceLocation;");
            final MethodRef glDepthMask = new MethodRef(MCPatcherUtils.GL11_CLASS, "glDepthMask", "(Z)V");

            addClassSignature(new ConstSignature(glDepthMask));
            addClassSignature(new BaseMod.ResourceLocationSignature(this, sign, "textures/entity/sign.png"));

            addPatch(new BytecodePatch() {
                {
                    addPreMatchSignature(new BytecodeSignature() {
                        @Override
                        public String getMatchExpression() {
                            return buildExpression(
                                reference(GETSTATIC, sign)
                            );
                        }
                    });
                }

                @Override
                public String getDescription() {
                    return "override sign text color";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(0),
                        reference(INVOKESTATIC, glDepthMask),
                        push(0),
                        capture(anyISTORE)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        push(0),
                        reference(INVOKESTATIC, glDepthMask),
                        reference(INVOKESTATIC, colorizeSignText),
                        getCaptureGroup(1)
                    );
                }
            });
        }
    }

    private class RenderXPOrbMod extends ClassMod {
        RenderXPOrbMod() {
            addClassSignature(new ConstSignature("textures/entity/experience_orb.png"));

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
                public byte[] getReplacementBytes() {
                    return buildCode(
                        getCaptureGroup(2),
                        getCaptureGroup(1),
                        reference(INVOKESTATIC, colorizeXPOrb)
                    );
                }
            });
        }
    }
}
