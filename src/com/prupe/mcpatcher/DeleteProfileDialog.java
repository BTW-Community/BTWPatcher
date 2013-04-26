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
            v1File = MinecraftJarV1.getInstallation(version);
            v2File = MinecraftJarV2.getInstallation(version);
        } else {
            version = null;
            v1File = null;
            v2File = null;
        }

        textLabel.setText("Delete saved profile \"" + profile + "\"?");

        if (v1File == null) {
            v1CheckBox.setVisible(false);
        } else {
            v1CheckBox.setText("Also delete file " + v1File);
            v1CheckBox.setSelected(MainForm.shift);
        }

        if (v2File == null) {
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
            MinecraftJarV1.deleteInstallation(version);
        }
        if (v2CheckBox.isSelected() && v2File != null) {
            MinecraftJarV2.deleteInstallation(version);
        }
    }
}
