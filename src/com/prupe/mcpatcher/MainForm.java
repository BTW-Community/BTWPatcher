package com.prupe.mcpatcher;

import com.prupe.mcpatcher.converter.TexturePackConverter;
import com.prupe.mcpatcher.converter.TexturePackConverter15;
import com.prupe.mcpatcher.converter.TexturePackConverter16;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

class MainForm extends UserInterface {
    private static final int TAB_MODS = 0;
    private static final int TAB_OPTIONS = 1;
    private static final int TAB_LOG = 2;
    private static final int TAB_CLASS_MAP = 3;
    private static final int TAB_PATCH_SUMMARY = 4;

    private static final Color MOD_BUSY_COLOR = new Color(192, 192, 192);
    private static final String MOD_DESC_FORMAT1 = "<html>" +
        "<table border=\"0\" cellspacing=\"0\" cellpadding=\"1\" style=\"padding-top: 2px; padding-bottom: 2px; font-weight: normal;\" width=\"%1$d\"><tr>" +
        "<td align=\"left\">%2$s<font size=\"5\"><b>%3$s</b></font></td>" +
        "<td align=\"right\"><b>%4$s</b></td>" +
        "</tr>";
    private static final String MOD_DESC_FORMAT2 =
        "<tr><td colspan=\"2\"><i>%1$s</i></td></tr>";
    private static final String MOD_DESC_FORMAT3 =
        "</table>" +
            "</html>";
    private static final String FORCE_CONTINUE_TEXT = "I WILL NOT COMPLAIN IF THIS DOESN'T WORK.";

    private static Image programIcon;

    private JPanel mainPanel;
    final JFrame frame;
    private int frameWidth = 518;

    private final MainMenu mainMenu;

    JButton testButton;
    JButton patchButton;
    JButton undoButton;
    private JTable modTable;
    private JLabel statusText;
    private JProgressBar progressBar;
    private JTabbedPane tabbedPane;
    private JTextArea logText;
    private JButton copyLogButton;
    private JTextArea classMap;
    private JTextArea patchResults;
    private JButton copyClassMapButton;
    private JButton copyPatchResultsButton;
    private JScrollPane modTableScrollPane;
    private JPanel optionsPanel;
    private JScrollPane optionsScrollPane;
    private JScrollPane logScrollPane;
    private JScrollPane classMapScrollPane;
    private JScrollPane patchSummaryScrollPane;
    JButton upButton;
    JButton addButton;
    JButton downButton;
    JButton removeButton;
    JCheckBox setSelectedCheckBox;
    private JComboBox origVersionComboBox;
    private JComboBox outputProfileComboBox;

    private AddModDialog addModDialog;

    static boolean shift;

    private boolean busy = true;
    private boolean updatingProfiles;
    private UIWorker workerThread = null;

    MainForm() {
        Toolkit.getDefaultToolkit().getSystemEventQueue().push(new EventQueue() {
            private boolean reenter;

            protected void dispatchEvent(AWTEvent event) {
                try {
                    if (event instanceof KeyEvent && (event.getID() == KeyEvent.KEY_PRESSED || event.getID() == KeyEvent.KEY_RELEASED)) {
                        KeyEvent keyEvent = (KeyEvent) event;
                        if (keyEvent.getKeyCode() == KeyEvent.VK_SHIFT) {
                            shift = (event.getID() == KeyEvent.KEY_PRESSED);
                        }
                    }
                    super.dispatchEvent(event);
                } catch (Throwable e) {
                    Logger.log(Logger.LOG_MAIN);
                    Logger.log(Logger.LOG_MAIN, "Unexpected error while handling UI event %s", event.toString());
                    Logger.log(e);
                    if (!reenter) {
                        reenter = true;
                        try {
                            setBusy(false);
                            tabbedPane.setSelectedIndex(TAB_LOG);
                            updateActiveTab();
                            cancelWorker();
                        } catch (Throwable e1) {
                            Logger.log(e1);
                        } finally {
                            reenter = false;
                        }
                    }
                }
            }
        });

        frame = new JFrame("MCPatcher " + MCPatcher.DISPLAY_VERSION_STRING);
        frame.setResizable(true);
        frame.setContentPane(mainPanel);
        setIconImage(frame);
        frame.addWindowListener(new WindowListener() {
            public void windowOpened(WindowEvent e) {
            }

            public void windowClosing(WindowEvent e) {
                cancelWorker();
                frame.setVisible(false);
                frame.dispose();
                MCPatcher.saveProperties();
                System.exit(0);
            }

            public void windowClosed(WindowEvent e) {
            }

            public void windowIconified(WindowEvent e) {
            }

            public void windowDeiconified(WindowEvent e) {
            }

            public void windowActivated(WindowEvent e) {
            }

            public void windowDeactivated(WindowEvent e) {
            }
        });
        frame.addComponentListener(new ComponentListener() {
            public void componentResized(ComponentEvent e) {
                frameWidth = (int) ((Component) e.getSource()).getSize().getWidth();
            }

            public void componentMoved(ComponentEvent e) {
            }

            public void componentShown(ComponentEvent e) {
            }

            public void componentHidden(ComponentEvent e) {
            }
        });
        frame.setMinimumSize(new Dimension(470, 488));
        frame.pack();

        outputProfileComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (updatingProfiles || e.getStateChange() != ItemEvent.SELECTED) {
                    return;
                }
                String profile = e.getItem().toString();
                ProfileManager profileManager = MCPatcher.profileManager;
                if (profileManager != null && !MCPatcherUtils.isNullOrEmpty(profile)) {
                    if (profileManager.getOutputProfiles().contains(profile)) {
                        profileManager.selectOutputProfile(profile);
                    } else {
                        profileManager.copyCurrentProfile(profile);
                    }
                }
                refreshMinecraft(false, false);
            }
        });

        origVersionComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (updatingProfiles) {
                    return;
                }
                ProfileManager profileManager = MCPatcher.profileManager;
                int index = origVersionComboBox.getSelectedIndex();
                java.util.List<String> versions = profileManager.getInputVersions();
                profileManager.selectInputVersion(versions.get(index));
                refreshMinecraft(false, false);
            }
        });

        setSelectedCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Config.getInstance().selectPatchedProfile = setSelectedCheckBox.isSelected();
            }
        });

        modTable.setRowSelectionAllowed(true);
        modTable.setColumnSelectionAllowed(false);
        modTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        setModList(null);
        modTableScrollPane.getViewport().setBackground(modTable.getBackground());
        modTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                setStatusText("");
                if (modTable.isEnabled()) {
                    int row = modTable.getSelectedRow();
                    int col = modTable.getSelectedColumn();
                    AbstractTableModel model = (AbstractTableModel) modTable.getModel();
                    Mod mod = (Mod) model.getValueAt(row, col);
                    if (col == 0 && mod != null && mod.okToApply()) {
                        MCPatcher.modList.selectMod(mod, !mod.isEnabled());
                    }
                    model.fireTableRowsUpdated(0, model.getRowCount());
                    if (e.getClickCount() == 2 && mod instanceof ExternalMod) {
                        ExternalMod extMod = (ExternalMod) mod;
                        addModDialog = new AddModDialog(mainPanel, extMod);
                        if (addModDialog.showFileListDialog() && addModDialog.getMod() == extMod) {
                            modTable.addRowSelectionInterval(row, row);
                            ModTextRenderer renderer = (ModTextRenderer) modTable.getColumnModel().getColumn(1).getCellRenderer();
                            renderer.resetRowHeights();
                            model.fireTableDataChanged();
                        }
                    }
                }
                super.mouseClicked(e);
            }
        });

        upButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int row = modTable.getSelectedRow();
                if (row >= 0) {
                    int newRow = MCPatcher.modList.moveUp(row, shift);
                    modTable.clearSelection();
                    AbstractTableModel model = (AbstractTableModel) modTable.getModel();
                    model.fireTableRowsUpdated(Math.min(row, newRow), Math.max(row, newRow));
                    ModTextRenderer renderer = (ModTextRenderer) modTable.getColumnModel().getColumn(1).getCellRenderer();
                    renderer.resetRowHeights();
                    modTable.addRowSelectionInterval(newRow, newRow);
                }
            }
        });

        downButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int row = modTable.getSelectedRow();
                if (row >= 0) {
                    int newRow = MCPatcher.modList.moveDown(row, shift);
                    modTable.clearSelection();
                    AbstractTableModel model = (AbstractTableModel) modTable.getModel();
                    model.fireTableRowsUpdated(Math.min(row, newRow), Math.max(row, newRow));
                    ModTextRenderer renderer = (ModTextRenderer) modTable.getColumnModel().getColumn(1).getCellRenderer();
                    renderer.resetRowHeights();
                    modTable.addRowSelectionInterval(newRow, newRow);
                }
            }
        });

        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    addModDialog = new AddModDialog(mainPanel);
                    final File path = addModDialog.showBrowseDialog();
                    if (path == null) {
                        return;
                    }
                    if (ExternalMod.isValidPath(path)) {
                        if (addModDialog.showFileListDialog()) {
                            addMod(addModDialog.getMod());
                        }
                    }
                } catch (Throwable e1) {
                    Logger.log(e1);
                } finally {
                    hideDialog();
                    updateControls();
                }
            }

            private void hideDialog() {
                if (addModDialog != null) {
                    addModDialog.setVisible(false);
                    addModDialog.dispose();
                    addModDialog = null;
                }
            }

            private void addMod(Mod mod) {
                if (mod != null) {
                    int row = MCPatcher.modList.addFirstBuiltin(mod);
                    mod.setEnabled(true);
                    modTable.clearSelection();
                    AbstractTableModel model = (AbstractTableModel) modTable.getModel();
                    model.fireTableRowsInserted(row, row);
                    ModTextRenderer renderer = (ModTextRenderer) modTable.getColumnModel().getColumn(1).getCellRenderer();
                    renderer.resetRowHeights();
                }
            }
        });

        removeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int row = modTable.getSelectedRow();
                Mod mod = (Mod) modTable.getModel().getValueAt(row, 0);
                if (mod instanceof ExternalMod || mod instanceof ForgeAdapter) {
                    MCPatcher.modList.remove(mod);
                    modTable.clearSelection();
                    AbstractTableModel model = (AbstractTableModel) modTable.getModel();
                    model.fireTableRowsDeleted(row, row);
                    if (row >= model.getRowCount()) {
                        row--;
                    }
                    modTable.addRowSelectionInterval(row, row);
                    ModTextRenderer renderer = (ModTextRenderer) modTable.getColumnModel().getColumn(1).getCellRenderer();
                    renderer.resetRowHeights();
                }
            }
        });

        patchButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                /*
                if (MCPatcher.minecraft.isModded()) {
                    while (true) {
                        String answer = JOptionPane.showInputDialog(frame,
                            getModWarningText(MCPatcher.minecraft.getVersion()) + "\n\n" +
                                "However, if you understand the risks and wish to continue patching anyway, type\n" +
                                "    " + FORCE_CONTINUE_TEXT + "\n" +
                                "in the box below with the exact same capitalization and punctuation.",
                            "Warning", JOptionPane.WARNING_MESSAGE
                        );
                        if (answer == null || answer.equals("")) {
                            return;
                        }
                        if (FORCE_CONTINUE_TEXT.equals(answer)) {
                            break;
                        }
                    }
                }
                */
                HashMap<String, ArrayList<Mod>> conflicts = MCPatcher.getConflicts();
                if (!conflicts.isEmpty()) {
                    ConflictDialog dialog = new ConflictDialog(conflicts);
                    int result = dialog.getResult(mainPanel);
                    if (result != JOptionPane.YES_OPTION) {
                        return;
                    }
                }
                runWorker(new UIWorker("Patch") {
                    @Override
                    void runImpl() throws Exception {
                        MCPatcher.patch();
                    }

                    @Override
                    void updateUI() {
                        if (interrupted) {
                            MCPatcher.unpatch();
                        }
                    }
                });
            }
        });

        undoButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setStatusText("");
                ProfileManager profileManager = MCPatcher.profileManager;
                profileManager.deleteProfile(profileManager.getOutputProfile(), true);
                MinecraftJar.setDefaultTexturePack();
                JOptionPane.showMessageDialog(frame,
                    String.format("Removed %s\nand reset texture pack to default.", profileManager.getOutputJar().getParentFile()),
                    "", JOptionPane.INFORMATION_MESSAGE);
                updateControls();
            }
        });

        testButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                tabbedPane.setSelectedIndex(TAB_LOG);
                runWorker(new UIWorker("Launch game") {
                    @Override
                    void runImpl() throws Exception {
                        setStatusText("Launching %s...", MCPatcher.minecraft.getOutputFile().getName());
                        MCPatcher.saveProperties();
                        MCPatcher.minecraft.run();
                    }
                });
            }
        });

        tabbedPane.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                updateActiveTab();
                MCPatcher.saveProperties();
            }
        });

        ((DefaultCaret) logText.getCaret()).setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        JTextAreaPrintStream output = new JTextAreaPrintStream(logText);
        System.setOut(output);
        System.setErr(output);
        copyLogButton.addActionListener(new CopyToClipboardListener(logText));

        ((DefaultCaret) classMap.getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
        copyClassMapButton.addActionListener(new CopyToClipboardListener(classMap));

        ((DefaultCaret) patchResults.getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
        copyPatchResultsButton.addActionListener(new CopyToClipboardListener(patchResults));

        modTableScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        optionsScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        logScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        classMapScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        patchSummaryScrollPane.getVerticalScrollBar().setUnitIncrement(16);

        mainMenu = new MainMenu(this);
        frame.setJMenuBar(mainMenu.menuBar);
        setSelectedCheckBox.requestFocusInWindow();
    }

    private static String getModWarningText(MinecraftVersion version) {
        return "Your minecraft.jar appears to be already modded.\n" +
            "It is highly recommended that you install mods via MCPatcher instead.\n" +
            " - Close MCPatcher.\n" +
            " - Re-download " + version.getVersionString() + " using the launcher.\n" +
            " - Run MCPatcher and select mods to add using the Add (+) button in the main window.\n" +
            "This will prevent most conflicts between MCPatcher and other mods.";
    }

    static void setIconImage(Window window) {
        try {
            if (programIcon == null) {
                programIcon = Toolkit.getDefaultToolkit().getImage(MainForm.class.getResource("/resources/icon.png"));
            }
            window.setIconImage(programIcon);
        } catch (Throwable e) {
        }
    }

    @Override
    boolean shouldExit() {
        return false;
    }

    @Override
    boolean go(ProfileManager profileManager) {
        refreshMinecraft(true, false);
        return true;
    }

    void show() {
        setSelectedCheckBox.setSelected(Config.getInstance().selectPatchedProfile);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    File chooseMinecraftDir(File minecraftDir) {
        PatcherException exception = new PatcherException.InstallationNotFound(minecraftDir);
        JOptionPane.showMessageDialog(null,
            exception.getMessageBoxText(),
            exception.getMessageBoxTitle(),
            JOptionPane.ERROR_MESSAGE
        );
        JFileChooser fd = new JFileChooser();
        fd.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fd.setFileHidingEnabled(false);
        fd.setDialogTitle("Select Minecraft directory");
        int result = fd.showDialog(null, null);
        if (result != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        return fd.getSelectedFile();
    }

    void showBetaWarning() {
        JOptionPane.showMessageDialog(frame,
            "This is a pre-release version of MCPatcher and is not intended\n" +
                "for general use.\n\n" +
                "Please make backups of your mods, save files, and texture packs\n" +
                "before using.  Report any problems in the thread for MCPatcher beta at\n" +
                PatcherException.FORUM_URL,
            "For testing only", JOptionPane.INFORMATION_MESSAGE
        );
    }

    void showTexturePackConverter(final int version) {
        JFileChooser fd = new JFileChooser();
        fd.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fd.setFileHidingEnabled(false);
        fd.setDialogTitle("Select texture pack");
        fd.setCurrentDirectory(MCPatcherUtils.getMinecraftPath("texturepacks"));
        fd.setAcceptAllFileFilterUsed(false);
        fd.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                String name = f.getName();
                return f.isDirectory() || (name.endsWith(".zip") && !name.startsWith(TexturePackConverter.MCPATCHER_CONVERT_PREFIX));
            }

            @Override
            public String getDescription() {
                return "texture packs (*.zip)";
            }
        });
        if (fd.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            final File selectedFile = fd.getSelectedFile();
            final TexturePackConverter converter = (version == 16 ? new TexturePackConverter16(selectedFile) : new TexturePackConverter15(selectedFile));
            if (converter.getOutputFile().exists()) {
                int result = JOptionPane.showConfirmDialog(frame,
                    String.format("This will overwrite\n%s\n\nContinue?", converter.getOutputFile().getAbsolutePath()),
                    "Confirm overwrite", JOptionPane.YES_NO_OPTION);
                if (result != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            tabbedPane.setSelectedIndex(TAB_LOG);
            runWorker(new UIWorker("Convert") {
                private boolean result;

                @Override
                void runImpl() {
                    result = converter.convert(MainForm.this);
                }

                @Override
                void updateUI() {
                    StringBuilder sb = new StringBuilder();
                    for (String s : converter.getMessages()) {
                        sb.append(s).append('\n');
                    }
                    if (!success() || !result) {
                        JOptionPane.showMessageDialog(frame, sb.toString(),
                            "Error converting " + selectedFile.getName(),
                            JOptionPane.ERROR_MESSAGE
                        );
                    } else if (sb.length() > 0) {
                        JOptionPane.showMessageDialog(frame, sb.toString(),
                            "Warnings while converting " + selectedFile.getName(),
                            JOptionPane.WARNING_MESSAGE
                        );
                    } else {
                        JOptionPane.showMessageDialog(frame,
                            "Successfully converted " + selectedFile.getName() + ".\n" +
                                converter.getOutputMessage(),
                            "Done",
                            JOptionPane.INFORMATION_MESSAGE
                        );
                    }
                }
            });
        }
    }

    void refreshMinecraft(final boolean refreshProfileManager, final boolean forceRemote) {
        final ProfileManager profileManager = MCPatcher.profileManager;
        if (profileManager == null) {
            return;
        }
        runWorker(new UIWorker("Analyze") {
            @Override
            void runImpl() throws Exception {
                if (refreshProfileManager) {
                    profileManager.refresh(MainForm.this, forceRemote, true);
                }
                runInEventThread(new Runnable() {
                    @Override
                    public void run() {
                        updateProfileLists(profileManager);
                    }
                });
                MCPatcher.refreshMinecraftPath();
                runInEventThread(new Runnable() {
                    @Override
                    public void run() {
                        setModList(null);
                    }
                });
                if (forceRemote) {
                    ModList.legacyVersionList = null;
                }
                MCPatcher.refreshModList();
                runInEventThread(new Runnable() {
                    @Override
                    public void run() {
                        setModList(MCPatcher.modList);
                    }
                });
                MCPatcher.checkModApplicability();
                MCPatcher.saveProperties();
            }

            @Override
            void updateUI() {
                redrawModList();
                if (success() && MCPatcher.minecraft.isModded()) {
                    JOptionPane.showMessageDialog(frame,
                        getModWarningText(MCPatcher.minecraft.getVersion()),
                        "Warning", JOptionPane.WARNING_MESSAGE
                    );
                }
            }
        });
    }

    private void cancelWorker() {
        if (workerThread != null && workerThread.isAlive()) {
            try {
                workerThread.interrupt();
                setStatusText("Waiting for current task to finish...");
                workerThread.join();
            } catch (InterruptedException e) {
                Logger.log(e);
            }
            setStatusText("");
        }
        workerThread = null;
    }

    private void runWorker(UIWorker worker) {
        setBusy(true);
        if (Thread.currentThread().equals(workerThread)) {
            worker.run();
        } else {
            cancelWorker();
            workerThread = worker;
            workerThread.start();
        }
    }

    private static void runInEventThread(Runnable task) {
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            SwingUtilities.invokeLater(task);
        }
    }

    void setBusy(final boolean busy) {
        this.busy = busy;
        runInEventThread(new Runnable() {
            @Override
            public void run() {
                if (!busy) {
                    setStatusText("");
                    updateProgress(0, 0);
                }
                updateControls();
            }
        });
    }

    @Override
    public void setStatusText(final String format, final Object... params) {
        runInEventThread(new Runnable() {
            @Override
            public void run() {
                statusText.setText(String.format(format, params));
            }
        });
    }

    @Override
    public void updateProgress(final int value, final int max) {
        runInEventThread(new Runnable() {
            @Override
            public void run() {
                if (max > 0) {
                    progressBar.setVisible(true);
                    progressBar.setMinimum(0);
                    progressBar.setMaximum(max);
                    progressBar.setValue(value);
                } else {
                    progressBar.setVisible(false);
                    progressBar.setMinimum(0);
                    progressBar.setMaximum(1);
                    progressBar.setValue(0);
                }
            }
        });
    }

    void updateProfileLists(ProfileManager profileManager) {
        try {
            File path;
            updatingProfiles = true;

            StringBuilder sb = new StringBuilder();

            outputProfileComboBox.removeAllItems();
            if (profileManager != null) {
                for (String s : profileManager.getOutputProfiles()) {
                    outputProfileComboBox.addItem(s);
                }
                outputProfileComboBox.setSelectedIndex(profileManager.getSelectedOutputProfileIndex());
            }

            origVersionComboBox.removeAllItems();
            if (profileManager != null) {
                for (String s : profileManager.getInputVersions()) {
                    origVersionComboBox.addItem(s);
                }
                origVersionComboBox.setSelectedIndex(profileManager.getSelectedInputVersionIndex());
                path = profileManager.getInputJar();
                if (path != null) {
                    sb.append("<b>Input file:</b> ");
                    sb.append(path.getAbsolutePath());
                }
                path = profileManager.getOutputJar();
                if (path != null) {
                    if (sb.length() > 0) {
                        sb.append("<br>");
                    }
                    sb.append("<b>Output file:</b> ");
                    sb.append(path.getAbsolutePath());
                }
            }

            if (sb.length() > 0) {
                origVersionComboBox.setToolTipText("<html>" + sb.toString());
            } else {
                origVersionComboBox.setToolTipText(null);
            }
        } finally {
            updatingProfiles = false;
        }
    }

    void updateControls() {
        ProfileManager profileManager = MCPatcher.profileManager;
        String currentVersion = null;
        if (profileManager != null && profileManager.isReady()) {
            currentVersion = profileManager.getInputVersion();
        }
        if (MCPatcherUtils.isNullOrEmpty(currentVersion)) {
            frame.setTitle("MCPatcher " + MCPatcher.DISPLAY_VERSION_STRING);
        } else {
            frame.setTitle("MCPatcher " + MCPatcher.DISPLAY_VERSION_STRING + " [Minecraft " + currentVersion + "]");
        }
        boolean inputOk = false;
        boolean outputOk = false;
        if (profileManager != null && profileManager.isReady()) {
            File path = profileManager.getInputJar();
            inputOk = path != null && path.isFile();
            path = profileManager.getOutputJar();
            outputOk = path != null && path.isFile();
        }
        outputProfileComboBox.setEnabled(!busy);
        origVersionComboBox.setEnabled(!busy);
        setSelectedCheckBox.setEnabled(!busy);
        setSelectedCheckBox.setSelected(Config.getInstance().selectPatchedProfile);
        modTable.setEnabled(!busy && inputOk);
        upButton.setEnabled(!busy && inputOk);
        downButton.setEnabled(!busy && inputOk);
        addButton.setEnabled(!busy && inputOk);
        removeButton.setEnabled(!busy && inputOk);
        testButton.setEnabled(!busy && outputOk && MCPatcherUtils.getMinecraftPath().equals(MCPatcherUtils.getDefaultGameDir()));
        patchButton.setEnabled(!busy && inputOk);
        undoButton.setEnabled(!busy && outputOk);
        tabbedPane.setEnabled(!busy);

        updateActiveTab();
        mainMenu.updateControls(busy);
    }

    private void updateActiveTab() {
        if (tabbedPane.getSelectedIndex() != TAB_OPTIONS) {
            saveOptions();
        }
        switch (tabbedPane.getSelectedIndex()) {
            case TAB_OPTIONS:
                loadOptions();
                break;

            case TAB_CLASS_MAP:
                showClassMaps();
                break;

            case TAB_PATCH_SUMMARY:
                showPatchResults();
                break;

            default:
                break;
        }
    }

    private void saveOptions() {
        if (MCPatcher.modList != null) {
            for (Mod mod : MCPatcher.modList.getAll()) {
                if (mod.configPanel != null) {
                    try {
                        mod.configPanel.save();
                    } catch (Throwable e) {
                        Logger.log(e);
                    }
                }
            }
        }
    }

    private void loadOptions() {
        optionsPanel.removeAll();
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
        if (MCPatcher.modList != null) {
            for (Mod mod : MCPatcher.modList.getAll()) {
                try {
                    if (mod.configPanel == null) {
                        continue;
                    }
                    mod.loadOptions();
                    JPanel panel = mod.configPanel.getPanel();
                    if (panel == null) {
                        continue;
                    }
                    String name = mod.configPanel.getPanelName();
                    if (name == null) {
                        name = mod.getName();
                    }
                    if (panel.getParent() != null) {
                        panel.getParent().remove(panel);
                    }
                    panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), name));
                    optionsPanel.add(panel);
                    optionsPanel.add(Box.createRigidArea(new Dimension(1, 16)));
                } catch (Throwable e) {
                    Logger.log(e);
                }
            }
        }
        optionsPanel.validate();
    }

    private void showClassMaps() {
        classMap.setText("");
        JTextAreaPrintStream out = new JTextAreaPrintStream(classMap);
        MCPatcher.showClassMaps(out, shift);
        out.close();
    }

    private void showPatchResults() {
        patchResults.setText("");
        JTextAreaPrintStream out = new JTextAreaPrintStream(patchResults);
        MCPatcher.showPatchResults(out);
        out.close();
    }

    void setModList(final ModList modList) {
        modTable.setModel(new DefaultTableModel() {
            public int getRowCount() {
                return modList == null ? 0 : modList.getVisible().size();
            }

            public int getColumnCount() {
                return 2;
            }

            public String getColumnName(int columnIndex) {
                return null;
            }

            public Class<?> getColumnClass(int columnIndex) {
                return Mod.class;
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return false;
            }

            public Object getValueAt(int rowIndex, int columnIndex) {
                if (modList == null || rowIndex < 0) {
                    return null;
                } else {
                    java.util.List<Mod> visible = modList.getVisible();
                    return rowIndex < visible.size() ? visible.get(rowIndex) : null;
                }
            }

            public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            }
        });
        redrawModList();
    }

    void redrawModList() {
        modTable.getColumnModel().getColumn(0).setCellRenderer(new ModCheckBoxRenderer());
        modTable.getColumnModel().getColumn(1).setCellRenderer(new ModTextRenderer());
        AbstractTableModel model = (AbstractTableModel) modTable.getModel();
        model.fireTableDataChanged();
    }

    void redrawModListCheckboxes() {
        AbstractTableModel model = (AbstractTableModel) modTable.getModel();
        model.fireTableChanged(new TableModelEvent(model, 0, modTable.getRowCount(), 0));
    }

    private class ModCheckBoxRenderer extends JCheckBox implements TableCellRenderer {
        private boolean widthSet = false;

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (!(value instanceof Mod)) {
                Logger.log(Logger.LOG_GUI, "table cell %d,%d is of wrong type %s",
                    row, column, value == null ? "(null)" : value.getClass().getName()
                );
                return this;
            }
            Mod mod = (Mod) value;
            if (!table.isEnabled() || !mod.okToApply()) {
                setBackground(table.getBackground());
                setForeground(MOD_BUSY_COLOR);
            } else if (row == table.getSelectedRow()) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            } else {
                setBackground(table.getBackground());
                setForeground(table.getForeground());
            }
            setSelected(mod.isEnabled());
            setEnabled(table.isEnabled() && mod.okToApply());
            if (!widthSet) {
                TableColumn col = table.getColumnModel().getColumn(column);
                double width = getPreferredSize().getWidth();
                col.setMinWidth((int) width);
                col.setMaxWidth((int) (1.5 * width));
                col.setPreferredWidth((int) (1.5 * width));
                widthSet = true;
            }
            return this;
        }
    }

    private class ModTextRenderer extends JLabel implements TableCellRenderer {
        private final HashMap<Integer, Integer> rowSizeFull = new HashMap<Integer, Integer>();
        private final HashMap<Integer, Integer> rowSizeShort = new HashMap<Integer, Integer>();

        private String htmlEscape(String s) {
            return s == null ? "" : s.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");
        }

        public void resetRowHeights() {
            rowSizeFull.clear();
            rowSizeShort.clear();
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (!(value instanceof Mod)) {
                Logger.log(Logger.LOG_GUI, "table cell %d,%d is of wrong type %s",
                    row, column, value == null ? "(null)" : value.getClass().getName()
                );
                return this;
            }
            Mod mod = (Mod) value;
            HashMap<Integer, Integer> rowSize = rowSizeFull;
            boolean rowSelected = (row == table.getSelectedRow());
            StringBuilder sb = new StringBuilder();

            sb.append(String.format(MOD_DESC_FORMAT1,
                Math.max(frameWidth - 75, 350),
                (mod.experimental ? "<font color=\"red\" size=\"3\">(Experimental)</font> " : ""),
                htmlEscape(mod.getName()),
                htmlEscape(mod.getVersion())
            ));
            if (rowSelected) {
                rowSize = rowSizeShort;
                sb.append(String.format(MOD_DESC_FORMAT2, htmlEscape(mod.getDescription())));
            }
            sb.append(MOD_DESC_FORMAT3);
            setText(sb.toString());

            if (!table.isEnabled() || !mod.okToApply()) {
                setBackground(table.getBackground());
                setForeground(MOD_BUSY_COLOR);
            } else if (rowSelected) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            } else {
                setBackground(table.getBackground());
                setForeground(table.getForeground());
            }
            setEnabled(table.isEnabled() && mod.okToApply());

            int h;
            if (rowSize.containsKey(row)) {
                h = rowSize.get(row);
            } else {
                h = (int) getPreferredSize().getHeight();
                rowSize.put(row, h);
            }
            if (h != table.getRowHeight(row)) {
                table.setRowHeight(row, h);
            }

            ArrayList<String> errors = mod.getErrors();
            sb = new StringBuilder();
            if (!table.isEnabled()) {
            } else if (errors.size() == 0) {
                String author = htmlEscape(mod.getAuthor());
                String website = htmlEscape(mod.getWebsite());
                if (author.length() > 0 || website.length() > 0) {
                    sb.append("<html>");
                    if (mod.getAuthor().length() > 0) {
                        sb.append(String.format("Author: %s<br>", author));
                    }
                    if (mod.getWebsite().length() > 0) {
                        sb.append(String.format("Website: <a href=\"%1$s\">%1$s</a><br>", website));
                    }
                    sb.append("</html>");
                }
            } else {
                sb.append("<html><b>");
                sb.append(htmlEscape(mod.getName()));
                sb.append(" cannot be applied:</b><br>");
                for (String s : errors) {
                    sb.append("&nbsp;");
                    sb.append(s);
                    sb.append("<br>");
                }
                sb.append("</html>");
            }
            setToolTipText(sb.length() == 0 ? null : sb.toString());

            setOpaque(true);
            return this;
        }
    }

    private class CopyToClipboardListener implements ActionListener {
        final JTextArea textArea;

        public CopyToClipboardListener(JTextArea textArea) {
            this.textArea = textArea;
        }

        public void actionPerformed(ActionEvent e) {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                new StringSelection("[spoiler][code]\n" + textArea.getText() + "[/code][/spoiler]\n"), null
            );
        }
    }

    abstract private class UIWorker extends Thread {
        protected Throwable error;
        protected boolean interrupted;

        UIWorker(String name) {
            super(name);
        }

        @Override
        final public void run() {
            error = null;
            interrupted = false;
            try {
                runImpl();
            } catch (InterruptedException e) {
                interrupted = true;
            } catch (Throwable e) {
                Logger.log(e);
                error = e;
            } finally {
                runInEventThread(new Runnable() {
                    @Override
                    public void run() {
                        finish();
                    }
                });
            }
        }

        final void finish() {
            updateUI();
            setBusy(false);
            if (error != null) {
                tabbedPane.setSelectedIndex(TAB_LOG);
                showErrorMessage();
            }
            if (interrupted) {
                Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
            }
        }

        private void showErrorMessage() {
            if (error instanceof PatcherException) {
                JOptionPane.showMessageDialog(frame,
                    ((PatcherException) error).getMessageBoxText(),
                    ((PatcherException) error).getMessageBoxTitle(),
                    JOptionPane.ERROR_MESSAGE
                );
            } else if (error != null) {
                JOptionPane.showMessageDialog(frame,
                    "An unexpected error occurred.\n\n" +
                        error + "\n\n" +
                        "Check the Log tab for more information.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
                );
            }
        }

        boolean success() {
            return error == null && !interrupted;
        }

        void updateUI() {
        }

        abstract void runImpl() throws Exception;
    }
}
