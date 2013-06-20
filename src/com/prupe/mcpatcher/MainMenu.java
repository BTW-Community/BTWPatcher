package com.prupe.mcpatcher;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

class MainMenu {
    private final MainForm mainForm;

    final JMenuBar menuBar;

    private final JMenu file;
    private final JMenuItem origFile;
    private final JMenuItem outputFile;
    private final JMenuItem exit;

    private final JMenu mods;
    private final JMenuItem addMod;
    private final JMenuItem removeMod;
    private final JMenuItem enableAll;
    private final JMenuItem disableAll;
    private final JMenuItem moveUp;
    private final JMenuItem moveDown;

    private final JMenu profile;
    private final JMenuItem save;
    private final JMenuItem load;
    private final JMenuItem delete;

    private final JMenu game;
    private final JMenuItem patch;
    private final JMenuItem unpatch;
    private final JMenuItem test;

    private final JMenu convert;
    JMenuItem convertItem;

    MainMenu(MainForm mainForm1) {
        mainForm = mainForm1;

        menuBar = new JMenuBar();

        file = new JMenu("File");
        file.setMnemonic('F');
        menuBar.add(file);

        origFile = new JMenuItem("Select input file...");
        origFile.setMnemonic('i');
        copyActionListener(origFile, mainForm.origBrowseButton);
        file.add(origFile);

        outputFile = new JMenuItem("Select output file...");
        outputFile.setMnemonic('o');
        copyActionListener(outputFile, mainForm.outputBrowseButton);
        file.add(outputFile);

        file.addSeparator();

        exit = new JMenuItem("Exit");
        exit.setMnemonic('x');
        exit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(
                    new WindowEvent(mainForm.frame, WindowEvent.WINDOW_CLOSING)
                );
            }
        });
        file.add(exit);

        mods = new JMenu("Mods");
        mods.setMnemonic('M');
        menuBar.add(mods);

        addMod = new JMenuItem("Add...");
        addMod.setMnemonic('A');
        copyActionListener(addMod, mainForm.addButton);
        mods.add(addMod);

        removeMod = new JMenuItem("Remove");
        removeMod.setMnemonic('R');
        copyActionListener(removeMod, mainForm.removeButton);
        mods.add(removeMod);

        mods.addSeparator();

        enableAll = new JMenuItem("Enable all");
        enableAll.setMnemonic('E');
        mods.add(enableAll);
        enableAll.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ModList modList = MCPatcher.modList;
                if (modList != null) {
                    modList.enableValidMods(true);
                    mainForm.redrawModListCheckboxes();
                }
            }
        });

        disableAll = new JMenuItem("Disable all");
        disableAll.setMnemonic('D');
        mods.add(disableAll);
        disableAll.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ModList modList = MCPatcher.modList;
                if (modList != null) {
                    modList.disableAll();
                    mainForm.redrawModListCheckboxes();
                }
            }
        });

        mods.addSeparator();

        moveUp = new JMenuItem("Move up");
        moveUp.setMnemonic('u');
        copyActionListener(moveUp, mainForm.upButton);
        mods.add(moveUp);

        moveDown = new JMenuItem("Move down");
        moveDown.setMnemonic('d');
        copyActionListener(moveDown, mainForm.downButton);
        mods.add(moveDown);

        mods.addSeparator();

        profile = new JMenu("Profile");
        profile.setMnemonic('r');
        menuBar.add(profile);

        save = new JMenuItem("Save profile...");
        save.setMnemonic('S');
        save.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String profileName;
                for (int i = 0; ; i++) {
                    profileName = "Custom Profile";
                    if (i > 0) {
                        profileName += " " + i;
                    }
                    if (Config.instance.findProfileByName(profileName, false) == null) {
                        break;
                    }
                }
                Object result = JOptionPane.showInputDialog(
                    mainForm.frame,
                    "Enter a name for this profile:",
                    "Profile name",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    null,
                    profileName
                );
                if (result != null && result instanceof String && !result.equals("")) {
                    profileName = (String) result;
                    String currentProfile = Config.instance.getConfigValue(Config.TAG_SELECTED_PROFILE);
                    if (profileName.equals(currentProfile)) {
                        return;
                    }
                    if (Config.instance.findProfileByName(profileName, false) != null) {
                        int confirm = JOptionPane.showConfirmDialog(
                            mainForm.frame,
                            String.format("Profile \"%s\" exists.  Overwrite?", profileName),
                            "Confirm overwrite",
                            JOptionPane.YES_NO_OPTION
                        );
                        if (confirm != JOptionPane.YES_OPTION) {
                            return;
                        }
                        Config.instance.deleteProfile(profileName);
                    }
                    MCPatcher.modList.updateProperties();
                    Config.instance.selectProfile(profileName);
                    mainForm.updateControls();
                }
            }
        });
        profile.add(save);

        load = new JMenu("Select profile");
        load.setMnemonic('e');
        profile.add(load);

        delete = new JMenu("Delete profile");
        delete.setMnemonic('D');
        profile.add(delete);

        game = new JMenu("Game");
        game.setMnemonic('G');
        menuBar.add(game);

        patch = new JMenuItem("Patch");
        patch.setMnemonic('P');
        copyActionListener(patch, mainForm.patchButton);
        game.add(patch);

        unpatch = new JMenuItem("Unpatch");
        unpatch.setMnemonic('U');
        copyActionListener(unpatch, mainForm.undoButton);
        game.add(unpatch);

        game.addSeparator();

        test = new JMenuItem("Test Minecraft");
        test.setMnemonic('T');
        copyActionListener(test, mainForm.testButton);
        game.add(test);

        convert = new JMenu("Convert Texture Pack");
        convert.setMnemonic('C');
        convert.setForeground(Color.RED);
        menuBar.add(convert);

        convertItem = new JMenuItem("Convert 1.4 to 1.5...");
        convertItem.setMnemonic('5');
        convertItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                mainForm.showTexturePackConverter(15);
            }
        });
        convert.add(convertItem);

        convertItem = new JMenuItem("Convert 1.5 to 1.6...");
        convertItem.setMnemonic('6');
        convertItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                mainForm.showTexturePackConverter(16);
            }
        });
        convert.add(convertItem);

        updateControls(true);
    }

    private static void copyActionListener(JMenuItem item, final JButton button) {
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (button.isEnabled()) {
                    for (ActionListener listener : button.getActionListeners()) {
                        listener.actionPerformed(e);
                    }
                }
            }
        });
    }

    void updateControls(boolean busy) {
        file.setEnabled(!busy);
        mods.setEnabled(!busy);
        profile.setEnabled(!busy);
        game.setEnabled(!busy);
        convert.setEnabled(!busy);

        origFile.setEnabled(mainForm.origBrowseButton.isEnabled());
        outputFile.setEnabled(mainForm.outputBrowseButton.isEnabled());
        addMod.setEnabled(mainForm.addButton.isEnabled());
        removeMod.setEnabled(mainForm.removeButton.isEnabled());
        moveUp.setEnabled(mainForm.upButton.isEnabled());
        moveDown.setEnabled(mainForm.downButton.isEnabled());
        patch.setEnabled(mainForm.patchButton.isEnabled());
        unpatch.setEnabled(mainForm.undoButton.isEnabled());
        test.setEnabled(mainForm.testButton.isEnabled());

        load.removeAll();
        delete.removeAll();
        if (!busy && Config.instance != null) {
            ArrayList<String> profiles = Config.instance.getProfiles();
            Collections.sort(profiles, new Comparator<String>() {
                public int compare(String o1, String o2) {
                    MinecraftVersion v1 = null;
                    MinecraftVersion v2 = null;
                    if (Config.isDefaultProfile(o1)) {
                        v1 = MinecraftVersion.parseVersion(o1);
                    }
                    if (Config.isDefaultProfile(o2)) {
                        v2 = MinecraftVersion.parseVersion(o2);
                    }
                    if (v1 == null && v2 == null) {
                        return o1.compareTo(o2);
                    } else if (v1 == null) {
                        return 1;
                    } else if (v2 == null) {
                        return -1;
                    } else {
                        return v1.compareTo(v2);
                    }
                }
            });
            ButtonGroup buttonGroup = new ButtonGroup();
            final String currentProfile = Config.instance.getConfigValue(Config.TAG_SELECTED_PROFILE);
            for (final String profile : profiles) {
                JRadioButtonMenuItem item = new JRadioButtonMenuItem(profile, profile.equals(currentProfile));
                item.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        if (profile.equals(currentProfile)) {
                            return;
                        }
                        MCPatcher.modList.updateProperties();
                        Config.instance.selectProfile(profile);
                        boolean modsOk = false;
                        String version = Config.getVersionForDefaultProfile(profile);
                        if (version != null && !version.equals(MCPatcher.minecraft.getVersion().getProfileString())) {
                            File jar = MinecraftInstallation.getJarPathForVersion(version);
                            if (jar != null && jar.isFile()) {
                                try {
                                    modsOk = MCPatcher.setMinecraft(jar, false);
                                } catch (Exception e1) {
                                    e1.printStackTrace();
                                }
                            }
                        }
                        if (!modsOk) {
                            MCPatcher.getAllMods();
                        }
                        mainForm.updateModList();
                    }
                });
                buttonGroup.add(item);
                load.add(item);

                JMenuItem item1 = new JMenuItem(profile);
                item1.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        if (profile.equals(currentProfile)) {
                            return;
                        }
                        DeleteProfileDialog dialog = new DeleteProfileDialog(profile);
                        int result = JOptionPane.showConfirmDialog(
                            mainForm.frame,
                            dialog.getPanel(),
                            "Confirm profile delete",
                            JOptionPane.YES_NO_OPTION
                        );
                        if (result == JOptionPane.YES_OPTION) {
                            Config.instance.deleteProfile(profile);
                            dialog.deleteInstallations();
                            mainForm.updateControls();
                        }
                    }
                });
                item1.setEnabled(!profile.equals(currentProfile));
                delete.add(item1);
            }
        }
        if (load.getSubElements().length == 0) {
            JMenuItem item = new JMenuItem("(none)");
            item.setEnabled(false);
            load.add(item);
        } else {
            load.setEnabled(true);
        }
        if (delete.getSubElements().length == 0) {
            JMenuItem item = new JMenuItem("(none)");
            item.setEnabled(false);
            delete.add(item);
        } else {
            delete.setEnabled(true);
        }
    }
}
