package com.prupe.mcpatcher;

import javax.swing.*;
import java.io.File;

class DeleteProfileDialog {
    private JLabel textLabel;
    private JCheckBox v1CheckBox;
    private JCheckBox v2CheckBox;
    private JPanel panel;

    private final MinecraftVersion version;
    private final File v1File;
    private final File v2File;

    DeleteProfileDialog(final String profile) {
        if (Config.isDefaultProfile(profile)) {
            version = MinecraftVersion.parseVersion(profile);
            v1File = MinecraftInstallation.v1.getPatchedInstallation(version);
            v2File = MinecraftInstallation.v2.getPatchedInstallation(version);
        } else {
            version = null;
            v1File = null;
            v2File = null;
        }

        textLabel.setText("Delete saved profile \"" + profile + "\"?");

        if (v1File == null || !v1File.isFile()) {
            v1CheckBox.setVisible(false);
        } else {
            v1CheckBox.setText("Also delete file " + v1File);
            v1CheckBox.setSelected(MainForm.shift);
        }

        if (v2File == null || !v2File.isDirectory()) {
            v2CheckBox.setVisible(false);
        } else {
            v2CheckBox.setText("Also delete folder " + v2File);
            v2CheckBox.setSelected(MainForm.shift);
        }
    }

    JPanel getPanel() {
        return panel;
    }

    void deleteInstallations() {
        if (v1CheckBox.isSelected() && v1File != null) {
            MinecraftInstallation.v1.deletePatchedInstallation(version);
        }
        if (v2CheckBox.isSelected() && v2File != null) {
            MinecraftInstallation.v2.deletePatchedInstallation(version);
        }
    }
}
