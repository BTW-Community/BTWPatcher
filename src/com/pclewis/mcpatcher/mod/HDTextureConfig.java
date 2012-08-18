package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.MCPatcherUtils;
import com.pclewis.mcpatcher.ModConfigPanel;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

public class HDTextureConfig extends ModConfigPanel {
    private JPanel panel;
    private JComboBox waterCombo;
    private JComboBox lavaCombo;
    private JComboBox fireCombo;
    private JComboBox portalCombo;
    private JCheckBox textureCacheCheckBox;
    private JCheckBox shrinkGLMemoryCheckBox;
    private JComboBox otherCombo;

    private AnimationComboListener[] comboListeners;

    HDTextureConfig() {
        comboListeners = new AnimationComboListener[4];
        comboListeners[0] = new AnimationComboListener(waterCombo, "Water");
        comboListeners[1] = new AnimationComboListener(lavaCombo, "Lava");
        comboListeners[2] = new AnimationComboListener(fireCombo, "Fire");
        comboListeners[3] = new AnimationComboListener(portalCombo, "Portal");

        waterCombo.addItemListener(comboListeners[0]);
        lavaCombo.addItemListener(comboListeners[1]);
        fireCombo.addItemListener(comboListeners[2]);
        portalCombo.addItemListener(comboListeners[3]);

        otherCombo.addItem("Not Animated");
        otherCombo.addItem("Custom Animated");
        otherCombo.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    switch (otherCombo.getSelectedIndex()) {
                        case 0:
                            MCPatcherUtils.set(MCPatcherUtils.HD_TEXTURES, "customOther", false);
                            break;

                        case 1:
                            MCPatcherUtils.set(MCPatcherUtils.HD_TEXTURES, "customOther", true);
                            break;

                        default:
                            break;
                    }
                }
            }
        });

        textureCacheCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                MCPatcherUtils.set(MCPatcherUtils.HD_TEXTURES, "useTextureCache", textureCacheCheckBox.isSelected());
            }
        });

        shrinkGLMemoryCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                MCPatcherUtils.set(MCPatcherUtils.HD_TEXTURES, "reclaimGLMemory", shrinkGLMemoryCheckBox.isSelected());
            }
        });
    }

    @Override
    public JPanel getPanel() {
        return panel;
    }

    @Override
    public void load() {
        for (AnimationComboListener listener : comboListeners) {
            listener.load();
        }
        otherCombo.setSelectedIndex(MCPatcherUtils.getBoolean(MCPatcherUtils.HD_TEXTURES, "customOther", true) ? 1 : 0);
        boolean is64bit = false;
        try {
            String datamodel = System.getProperty("sun.arch.data.model"); // sun-specific, but gets the arch of the jvm
            String arch = System.getProperty("os.arch"); // generic, but gets the arch of the os, not the jvm (may be a 32-bit jvm on a 64-bit os)
            if (datamodel != null) {
                is64bit = (Integer.parseInt(datamodel) >= 64);
            } else if (arch != null) {
                is64bit = arch.contains("64");
            } else {
                is64bit = false;
            }
        } catch (Throwable e) {
        }
        textureCacheCheckBox.setSelected(MCPatcherUtils.getBoolean(MCPatcherUtils.HD_TEXTURES, "useTextureCache", is64bit));
        shrinkGLMemoryCheckBox.setSelected(MCPatcherUtils.getBoolean(MCPatcherUtils.HD_TEXTURES, "reclaimGLMemory", false));
    }

    @Override
    public void save() {
    }

    private static class AnimationComboListener implements ItemListener {
        private static final int OPT_DEFAULT = 0;
        private static final int OPT_NOT_ANIMATED = 1;
        private static final int OPT_CUSTOM_ANIMATED = 2;

        final private JComboBox comboBox;
        final private String animatedTag;
        final private String customTag;

        public AnimationComboListener(JComboBox comboBox, String tag) {
            this.comboBox = comboBox;
            customTag = "custom" + tag;
            animatedTag = "animated" + tag;
            comboBox.addItem("Default");
            comboBox.addItem("Not Animated");
            comboBox.addItem("Custom Animated");
        }

        public void itemStateChanged(ItemEvent e) {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                boolean custom;
                boolean anim;
                switch (comboBox.getSelectedIndex()) {
                    case OPT_DEFAULT:
                        custom = false;
                        anim = true;
                        break;

                    case OPT_NOT_ANIMATED:
                        custom = false;
                        anim = false;
                        break;

                    case OPT_CUSTOM_ANIMATED:
                        custom = true;
                        anim = true;
                        break;

                    default:
                        return;
                }
                MCPatcherUtils.set(MCPatcherUtils.HD_TEXTURES, customTag, custom);
                MCPatcherUtils.set(MCPatcherUtils.HD_TEXTURES, animatedTag, anim);
            }
        }

        public void load() {
            if (MCPatcherUtils.getBoolean(MCPatcherUtils.HD_TEXTURES, customTag, true)) {
                comboBox.setSelectedIndex(OPT_CUSTOM_ANIMATED);
            } else if (MCPatcherUtils.getBoolean(MCPatcherUtils.HD_TEXTURES, animatedTag, true)) {
                comboBox.setSelectedIndex(OPT_DEFAULT);
            } else {
                comboBox.setSelectedIndex(OPT_NOT_ANIMATED);
            }
        }
    }
}
