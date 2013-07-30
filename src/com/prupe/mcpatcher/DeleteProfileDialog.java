package com.prupe.mcpatcher;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

class DeleteProfileDialog {
    private JLabel textLabel;
    private JPanel panel;
    private JPanel checkBoxPanel;

    private final List<String> versions = new ArrayList<String>();
    private final List<JCheckBox> checkBoxes = new ArrayList<JCheckBox>();

    private final ProfileManager profileManager;

    DeleteProfileDialog(ProfileManager profileManager) {
        this.profileManager = profileManager;
    }

    JPanel getPanel() {
        return panel;
    }

    void setProfile(String profileName, List<String> versions) {
        this.versions.clear();
        checkBoxes.clear();
        checkBoxPanel.removeAll();
        checkBoxPanel.setLayout(new BoxLayout(checkBoxPanel, BoxLayout.Y_AXIS));
        checkBoxPanel.add(new JLabel("Also remove the following version directories:"));

        for (String s : versions) {
            File dir = profileManager.getLocalVersionPath(s);
            if (dir != null && dir.isDirectory()) {
                this.versions.add(s);
                JCheckBox box = new JCheckBox(dir.toString(), false);
                checkBoxes.add(box);
                checkBoxPanel.add(box);
            }
        }

        textLabel.setText("Delete saved profile \"" + profileName + "\"?");

        if (checkBoxes.isEmpty()) {
            checkBoxPanel.setVisible(false);
        } else {
            checkBoxPanel.setVisible(true);
        }
    }

    void deleteInstallations() {
        for (int i = 0; i < checkBoxes.size(); i++) {
            if (checkBoxes.get(i).isSelected()) {
                profileManager.deleteLocalVersion(versions.get(i));
            }
        }
    }
}
