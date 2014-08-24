package com.prupe.mcpatcher.mod.cc;

import com.prupe.mcpatcher.*;
import com.prupe.mcpatcher.basemod.*;
import com.prupe.mcpatcher.basemod.ext18.IBlockStateMod;
import com.prupe.mcpatcher.basemod.ext18.PositionMod;
import com.prupe.mcpatcher.basemod.ext18.RenderUtilsMod;
import com.prupe.mcpatcher.mal.TexturePackAPIMod;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class CustomColors extends Mod {
    static final MethodRef computeRedstoneWireColor = new MethodRef(MCPatcherUtils.COLORIZE_BLOCK_CLASS, "computeRedstoneWireColor", "(I)Z");

    static final MethodRef setColorF = new MethodRef(MCPatcherUtils.COLORIZER_CLASS, "setColorF", "(I)V");
    static final FieldRef setColor = new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "setColor", "[F");

    static final MethodRef getColorFromDamage = new MethodRef("Item", "getColorFromDamage", "(LItemStack;I)I");

    public CustomColors() {
        name = MCPatcherUtils.CUSTOM_COLORS;
        author = "MCPatcher";
        description = "Gives texture packs control over hardcoded colors in the game.";
        version = "1.9";

        addDependency(MCPatcherUtils.TEXTURE_PACK_API_MOD);
        addDependency(MCPatcherUtils.BLOCK_API_MOD);
        addDependency(MCPatcherUtils.BIOME_API_MOD);

        configPanel = new ConfigPanel();

        addClassMod(new MinecraftMod(this).mapWorldClient());
        addClassMod(new IBlockAccessMod(this));
        addClassMod(new TessellatorMod(this));
        if (!ResourceLocationMod.haveClass()) {
            addClassMod(new IconMod(this));
        }
        ResourceLocationMod.setup(this);
        PositionMod.setup(this);
        RenderUtilsMod.setup(this);
        addClassMod(new BiomeGenBaseMod(this));

        addClassFiles("com.prupe.mcpatcher.cc.*");
        removeAddedClassFile(MCPatcherUtils.COLORIZE_BLOCK18_CLASS + "*");

        if (IBlockStateMod.haveClass()) {
            removeAddedClassFiles(MCPatcherUtils.COLORIZE_BLOCK_CLASS + "*");
        } else {
            CC_Block.setup(this);
        }
        CC_Item.setup(this);
        CC_Entity.setup(this);
        CC_World.setup(this);

        TexturePackAPIMod.earlyInitialize(3, MCPatcherUtils.COLORIZER_CLASS, "init");
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
        private JCheckBox smoothBiomesCheckBox;
        private JCheckBox testBiomeColorsCheckBox;
        private JSpinner yVarianceSpinner;

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
                    Config.set(MCPatcherUtils.CUSTOM_COLORS, "blockBlendRadius2", value);
                    Config.remove(MCPatcherUtils.CUSTOM_COLORS, "blockBlendRadius");
                    blockBlendRadiusSpinner.setValue(value);
                }
            });

            yVarianceSpinner.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    int value = 0;
                    try {
                        value = Integer.parseInt(yVarianceSpinner.getValue().toString());
                        value = Math.min(Math.max(0, value), 255);
                    } catch (NumberFormatException e1) {
                    }
                    Config.set(MCPatcherUtils.CUSTOM_COLORS, "yVariance", value);
                    yVarianceSpinner.setValue(value);
                }
            });

            smoothBiomesCheckBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Config.set(MCPatcherUtils.CUSTOM_COLORS, "smoothBiomes", smoothBiomesCheckBox.isSelected());
                }
            });

            testBiomeColorsCheckBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Config.set(MCPatcherUtils.CUSTOM_COLORS, "testColorSmoothing", testBiomeColorsCheckBox.isSelected());
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
            blockBlendRadiusSpinner.setValue(Config.getInt(MCPatcherUtils.CUSTOM_COLORS, "blockBlendRadius2", 4));
            yVarianceSpinner.setValue(Config.getInt(MCPatcherUtils.CUSTOM_COLORS, "yVariance", 0));
            smoothBiomesCheckBox.setSelected(Config.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "smoothBiomes", true));
            testBiomeColorsCheckBox.setSelected(Config.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "testColorSmoothing", false));
            showAdvancedOption(testBiomeColorsCheckBox);
        }

        @Override
        public void save() {
        }
    }

    public static MinecraftVersion getMinecraftVersion() {
        return Mod.getMinecraftVersion();
    }
}
