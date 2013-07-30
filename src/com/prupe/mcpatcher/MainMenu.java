package com.prupe.mcpatcher;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collections;

class MainMenu {
    private final MainForm mainForm;

    final JMenuBar menuBar;

    private final JMenu file;
    private final JMenuItem refresh;
    private final JMenuItem exit;

    private final JMenu mods;
    private final JMenuItem addMod;
    private final JMenuItem removeMod;
    private final JMenuItem enableAll;
    private final JMenuItem disableAll;
    private final JMenuItem moveUp;
    private final JMenuItem moveDown;

    private final JMenu profile;
    private final JMenuItem select;
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

        refresh = new JMenuItem("Refresh profile list");
        refresh.setMnemonic('R');
        refresh.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mainForm.refreshProfileManager();
            }
        });
        file.add(refresh);

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

        select = new JMenu("Select input profile");
        select.setMnemonic('S');
        profile.add(select);

        delete = new JMenu("Delete output profile");
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

        addMod.setEnabled(mainForm.addButton.isEnabled());
        removeMod.setEnabled(mainForm.removeButton.isEnabled());
        moveUp.setEnabled(mainForm.upButton.isEnabled());
        moveDown.setEnabled(mainForm.downButton.isEnabled());
        patch.setEnabled(mainForm.patchButton.isEnabled());
        unpatch.setEnabled(mainForm.undoButton.isEnabled());
        test.setEnabled(mainForm.testButton.isEnabled());

        Config config = Config.getInstance();
        final ProfileManager profileManager = MCPatcher.profileManager;

        select.removeAll();
        if (!busy) {
            java.util.List<String> profiles = new ArrayList<String>();
            profiles.addAll(profileManager.getInputProfiles());
            Collections.sort(profiles);

            ButtonGroup buttonGroup = new ButtonGroup();
            for (final String profile : profiles) {
                JRadioButtonMenuItem item = new JRadioButtonMenuItem(profile);
                item.setSelected(profile.equals(profileManager.getInputProfile()));
                item.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        profileManager.selectInputProfile(profile);
                    }
                });
                select.add(item);
                buttonGroup.add(item);
            }
        }
        addIfEmpty(select);

        delete.removeAll();
        if (!busy) {
            java.util.List<String> profiles = new ArrayList<String>();
            profiles.addAll(profileManager.getOutputProfiles());
            Collections.sort(profiles);
            final String currentProfile = profileManager.getOutputProfile();
            final DeleteProfileDialog dialog = new DeleteProfileDialog(profileManager);

            for (final String profile : profiles) {
                final Config.ProfileEntry profileEntry = config.profiles.get(profile);
                if (profileEntry == null) {
                    continue;
                }
                JMenuItem item = new JMenuItem(profile);
                item.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        if (profile.equals(currentProfile)) {
                            return;
                        }
                        ArrayList<String> versions = new ArrayList<String>();
                        versions.addAll(profileEntry.versions.keySet());
                        dialog.setProfile(profile, versions);
                        int result = JOptionPane.showConfirmDialog(
                            mainForm.frame,
                            dialog.getPanel(),
                            "Confirm profile delete",
                            JOptionPane.YES_NO_OPTION
                        );
                        if (result == JOptionPane.YES_OPTION) {
                            profileManager.deleteProfile(profile, false);
                            dialog.deleteInstallations();
                            mainForm.updateProfileLists(profileManager);
                            mainForm.updateControls();
                        }
                    }
                });
                item.setEnabled(!profile.equals(Config.MCPATCHER_PROFILE_NAME) && !profile.equals(currentProfile));
                delete.add(item);
            }
        }
        addIfEmpty(delete);
    }

    private static void addIfEmpty(JMenuItem menu) {
        MenuElement[] subElements = menu.getSubElements();
        if (subElements == null || subElements.length == 0 || subElements[0].getSubElements().length == 0) {
            JMenuItem item = new JMenuItem("(none)");
            item.setEnabled(false);
            menu.add(item);
        }
    }
}
