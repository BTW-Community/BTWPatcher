package com.prupe.mcpatcher;

import javax.swing.*;
import java.awt.*;

/**
 * Class that defines a GUI configuration screen for the mod.
 */
abstract public class ModConfigPanel {
    /**
     * Called by MCPatcher to get the mod's top-level GUI panel.
     *
     * @return JPanel
     */
    abstract public JPanel getPanel();

    /**
     * Can be overridden to specify a different name to be used in the border around the
     * mod's configuration UI.  If null, mod.getName() is used.
     *
     * @return String name
     * @see com.prupe.mcpatcher.Mod#getName()
     */
    public String getPanelName() {
        return null;
    }

    /**
     * Called by MCPatcher whenever the user switches <i>to</i> the Options panel.  Use this
     * to load the current settings into the UI.
     *
     * @see Config#getString(String, String, Object)
     */
    abstract public void load();

    /**
     * Called by MCPatcher whenever the user switches <i>away from</i> the Options panel.  Use this
     * to save changes made in the UI.
     *
     * @see Config#set(String, String, Object)
     */
    abstract public void save();

    /**
     * Call during #load() to determine whether advanced options normally hidden should be made visible.
     * Triggered by holding down shift when clicking the Options tab.
     *
     * @return true if extra options should be shown
     */
    protected boolean showAdvancedOptions() {
        return MainForm.shift;
    }

    /**
     * Convenience method to automatically show or hide an advanced option.  If shown, it will be highlighted in
     * a different color.
     * <p/>
     * Example,
     * <pre>
     *     public void load() {
     *         showAdvancedOption(myDebugCheckbox);
     *     }
     * </pre>
     *
     * @param component "advanced" gui option
     */
    protected void showAdvancedOption(JComponent component) {
        component.setVisible(showAdvancedOptions());
        component.setBackground(Color.WHITE);
        component.setForeground(Color.RED);
    }
}
