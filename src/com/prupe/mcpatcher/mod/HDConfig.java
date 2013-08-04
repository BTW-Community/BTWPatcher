package com.prupe.mcpatcher.mod;

import com.prupe.mcpatcher.Config;
import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.ModConfigPanel;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class HDConfig extends ModConfigPanel {
    private static final String TAG_FONT = "hdFont";
    private static final String TAG_NON_FONT_WIDTH = "nonHDFontWidth";
    private static final String TAG_ANIMATIONS = "animations";
    private static final String TAG_COMPASS = "fancyCompass";
    private static final String TAG_CLOCK = "fancyClock";
    private static final String TAG_MIPMAP = "mipmap";
    private static final String TAG_MAX_MIPMAP_LEVEL = "maxMipmapLevel";
    private static final String TAG_LOD_BIAS = "lodBias";
    private static final String TAG_ANISOTROPIC_FILTERING = "anisotropicFiltering";
    private static final String TAG_ANTI_ALIASING = "antiAliasing";
    private static final String TAG_GL13 = "useGL13";
    private static final String TAG_SCRATCH_TEXTURE = "useScratchTexture";
    private static final String TAG_DEBUG_BORDER = "debugBorder";

    private JPanel panel;
    private JCheckBox fontCheckBox;
    private JCheckBox fontWidthCheckBox;
    private JCheckBox animationCheckBox;
    private JCheckBox compassCheckBox;
    private JCheckBox clockCheckBox;
    private JCheckBox mipmapCheckBox;
    private JSpinner mipmapLevelSpinner;
    private JSpinner lodBiasSpinner;
    private JSpinner anisoSpinner;
    private JSpinner aaSpinner;
    private JCheckBox gl13CheckBox;
    private JCheckBox scratchTextureCheckBox;
    private JCheckBox debugBorderCheckBox;

    HDConfig() {
        fontCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                boolean selected = fontCheckBox.isSelected();
                Config.set(MCPatcherUtils.EXTENDED_HD, TAG_FONT, selected);
                fontWidthCheckBox.setEnabled(selected);
            }
        });

        fontWidthCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Config.set(MCPatcherUtils.EXTENDED_HD, TAG_NON_FONT_WIDTH, fontWidthCheckBox.isSelected());
            }
        });

        animationCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Config.set(MCPatcherUtils.EXTENDED_HD, TAG_ANIMATIONS, animationCheckBox.isSelected());
            }
        });

        compassCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Config.set(MCPatcherUtils.EXTENDED_HD, TAG_COMPASS, compassCheckBox.isSelected());
            }
        });

        clockCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Config.set(MCPatcherUtils.EXTENDED_HD, TAG_CLOCK, clockCheckBox.isSelected());
            }
        });

        mipmapCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Config.set(MCPatcherUtils.EXTENDED_HD, TAG_MIPMAP, mipmapCheckBox.isSelected());
            }
        });

        gl13CheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Config.set(MCPatcherUtils.EXTENDED_HD, TAG_GL13, gl13CheckBox.isSelected());
            }
        });

        scratchTextureCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Config.set(MCPatcherUtils.EXTENDED_HD, TAG_SCRATCH_TEXTURE, scratchTextureCheckBox.isSelected());
            }
        });

        debugBorderCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Config.set(MCPatcherUtils.EXTENDED_HD, TAG_DEBUG_BORDER, debugBorderCheckBox.isSelected());
            }
        });

        mipmapLevelSpinner.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                int value = 4;
                try {
                    value = Integer.parseInt(mipmapLevelSpinner.getValue().toString());
                    value = Math.min(Math.max(0, value), 9);
                } catch (NumberFormatException e1) {
                }
                Config.set(MCPatcherUtils.EXTENDED_HD, TAG_MAX_MIPMAP_LEVEL, value);
                mipmapLevelSpinner.setValue(value);
            }
        });

        lodBiasSpinner.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                int value = 0;
                try {
                    value = Integer.parseInt(lodBiasSpinner.getValue().toString());
                    value = Math.min(Math.max(-9, value), 9);
                } catch (NumberFormatException e1) {
                }
                Config.set(MCPatcherUtils.EXTENDED_HD, TAG_LOD_BIAS, value);
                lodBiasSpinner.setValue(value);
            }
        });

        anisoSpinner.addChangeListener(new ChangeListener() {
            private int lastValue = Config.getInt(MCPatcherUtils.EXTENDED_HD, TAG_ANISOTROPIC_FILTERING, 1);

            public void stateChanged(ChangeEvent e) {
                int value = 1;
                try {
                    value = Integer.parseInt(anisoSpinner.getValue().toString());
                    if (value > lastValue) {
                        value = lastValue * 2;
                    } else if (value < lastValue) {
                        value = lastValue / 2;
                    }
                    if (value <= 1) {
                        value = 1;
                    } else if (value <= 2) {
                        value = 2;
                    } else if (value <= 4) {
                        value = 4;
                    } else if (value <= 8) {
                        value = 8;
                    } else {
                        value = 16;
                    }
                } catch (NumberFormatException e1) {
                }
                Config.set(MCPatcherUtils.EXTENDED_HD, TAG_ANISOTROPIC_FILTERING, value);
                anisoSpinner.setValue(value);
                lastValue = value;
            }
        });

        aaSpinner.addChangeListener(new ChangeListener() {
            private int lastValue = Config.getInt(MCPatcherUtils.EXTENDED_HD, TAG_ANTI_ALIASING, 0);

            public void stateChanged(ChangeEvent e) {
                int value = 0;
                try {
                    value = Integer.parseInt(aaSpinner.getValue().toString());
                    if (value > lastValue) {
                        value = (lastValue + 2) & ~0x1;
                    } else if (value < lastValue) {
                        value = Math.max(lastValue - 2, 0) & ~0x1;
                    }
                    value = Math.min(Math.max(0, value), 16);
                } catch (NumberFormatException e1) {
                }
                Config.set(MCPatcherUtils.EXTENDED_HD, TAG_ANTI_ALIASING, value);
                aaSpinner.setValue(value);
                lastValue = value;
            }
        });
    }

    @Override
    public JPanel getPanel() {
        return panel;
    }

    @Override
    public void load() {
        boolean hdFont = Config.getBoolean(MCPatcherUtils.EXTENDED_HD, TAG_FONT, true);
        fontCheckBox.setSelected(hdFont);
        fontWidthCheckBox.setEnabled(hdFont);
        fontWidthCheckBox.setSelected(Config.getBoolean(MCPatcherUtils.EXTENDED_HD, TAG_NON_FONT_WIDTH, false));
        animationCheckBox.setSelected(Config.getBoolean(MCPatcherUtils.EXTENDED_HD, TAG_ANIMATIONS, true));
        compassCheckBox.setSelected(Config.getBoolean(MCPatcherUtils.EXTENDED_HD, TAG_COMPASS, true));
        clockCheckBox.setSelected(Config.getBoolean(MCPatcherUtils.EXTENDED_HD, TAG_CLOCK, true));
        mipmapCheckBox.setSelected(Config.getBoolean(MCPatcherUtils.EXTENDED_HD, TAG_MIPMAP, false));
        mipmapLevelSpinner.setValue(Config.getInt(MCPatcherUtils.EXTENDED_HD, TAG_MAX_MIPMAP_LEVEL, 3));
        lodBiasSpinner.setValue(Config.getInt(MCPatcherUtils.EXTENDED_HD, TAG_LOD_BIAS, 0));
        anisoSpinner.setValue(Config.getInt(MCPatcherUtils.EXTENDED_HD, TAG_ANISOTROPIC_FILTERING, 1));
        aaSpinner.setValue(Config.getInt(MCPatcherUtils.EXTENDED_HD, TAG_ANTI_ALIASING, 0));
        gl13CheckBox.setSelected(Config.getBoolean(MCPatcherUtils.EXTENDED_HD, TAG_GL13, true));
        scratchTextureCheckBox.setSelected(Config.getBoolean(MCPatcherUtils.EXTENDED_HD, TAG_SCRATCH_TEXTURE, true));
        debugBorderCheckBox.setSelected(Config.getBoolean(MCPatcherUtils.EXTENDED_HD, TAG_DEBUG_BORDER, false));

        showAdvancedOption(gl13CheckBox);
        showAdvancedOption(scratchTextureCheckBox);
        showAdvancedOption(debugBorderCheckBox);
    }

    @Override
    public void save() {
    }
}
