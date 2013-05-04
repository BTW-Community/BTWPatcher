package com.prupe.mcpatcher;

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
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

class MainForm {
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
    private static final String PADDING = "                                                                  ";

    private static Image programIcon;

    private JPanel mainPanel;
    JFrame frame;
    private int frameWidth = 518;

    private MainMenu mainMenu;

    private JTextField origField;
    JButton origBrowseButton;
    private JTextField outputField;
    JButton outputBrowseButton;
    JButton testButton;
    JButton patchButton;
    JButton undoButton;
    private JTable modTable;
    private JLabel statusText;
    private JButton refreshButton;
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

    private AddModDialog addModDialog;

    static boolean shift;

    private boolean busy = true;
    private Thread workerThread = null;

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
                    e.printStackTrace();
                    if (!reenter) {
                        reenter = true;
                        try {
                            setBusy(false);
                            tabbedPane.setSelectedIndex(TAB_LOG);
                            updateActiveTab();
                            cancelWorker();
                        } catch (Throwable e1) {
                            e.printStackTrace();
                        } finally {
                            reenter = false;
                        }
                    }
                }
            }
        });

        frame = new JFrame("Minecraft Patcher " + MCPatcher.VERSION_STRING);
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

        origBrowseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setStatusText("");
                File selectedFile = null;
                if (shift) {
                    String text = (String) JOptionPane.showInputDialog(
                        frame, "Enter path to new input file:" + PADDING, "Input file", JOptionPane.QUESTION_MESSAGE,
                        null, null, origField.getText()
                    );
                    if (text != null && !text.equals("")) {
                        selectedFile = new File(text);
                        if (!selectedFile.isFile()) {
                            selectedFile = null;
                        }
                    }
                } else {
                    JFileChooser fd = new JFileChooser();
                    fd.setFileSelectionMode(JFileChooser.FILES_ONLY);
                    fd.setFileHidingEnabled(false);
                    fd.setDialogTitle("Select input file");
                    fd.setCurrentDirectory(MCPatcherUtils.getMinecraftPath("bin"));
                    fd.setAcceptAllFileFilterUsed(false);
                    fd.setFileFilter(new FileFilter() {
                        @Override
                        public boolean accept(File f) {
                            return f.isDirectory() || f.getName().toLowerCase().endsWith(".jar");
                        }

                        @Override
                        public String getDescription() {
                            return "*.jar";
                        }
                    });
                    if (fd.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                        selectedFile = fd.getSelectedFile();
                    }
                }
                if (selectedFile != null) {
                    if (MCPatcher.setMinecraft(selectedFile, false)) {
                        MCPatcher.saveProperties();
                        updateModList();
                    } else {
                        showCorruptJarError(selectedFile);
                    }
                }
                updateControls();
            }
        });

        outputBrowseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setStatusText("");
                File selectedFile = null;
                if (shift) {
                    String text = (String) JOptionPane.showInputDialog(
                        frame, "Enter path to new output file:" + PADDING, "Output file", JOptionPane.QUESTION_MESSAGE,
                        null, null, outputField.getText()
                    );
                    if (text != null && !text.equals("")) {
                        selectedFile = new File(text);
                        if (selectedFile.getParentFile() == null || !selectedFile.getParentFile().isDirectory()) {
                            selectedFile = null;
                        }
                    }
                } else {
                    JFileChooser fd = new JFileChooser();
                    fd.setFileSelectionMode(JFileChooser.FILES_ONLY);
                    fd.setFileHidingEnabled(false);
                    fd.setDialogTitle("Select output file");
                    fd.setCurrentDirectory(MCPatcherUtils.getMinecraftPath("bin"));
                    fd.setSelectedFile(MCPatcherUtils.getMinecraftPath("bin", "minecraft.jar"));
                    fd.setAcceptAllFileFilterUsed(false);
                    fd.setFileFilter(new FileFilter() {
                        @Override
                        public boolean accept(File f) {
                            return f.isDirectory() || f.getName().toLowerCase().endsWith(".jar");
                        }

                        @Override
                        public String getDescription() {
                            return "*.jar";
                        }
                    });
                    if (fd.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
                        selectedFile = fd.getSelectedFile();
                    }
                }
                if (selectedFile != null) {
                    MCPatcher.minecraft.setOutputFile(selectedFile);
                }
                updateControls();
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
                        addModDialog.setLocationRelativeTo(frame);
                        addModDialog.setVisible(true);
                        if (addModDialog.getMod() == extMod) {
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
                    if (!addModDialog.showBrowseDialog(mainPanel)) {
                        return;
                    }
                    addModDialog.setLocationRelativeTo(frame);
                    addModDialog.setVisible(true);
                    Mod mod = addModDialog.getMod();
                    if (mod != null) {
                        int row = MCPatcher.modList.addFirstBuiltin(mod);
                        mod.setEnabled(true);
                        modTable.clearSelection();
                        AbstractTableModel model = (AbstractTableModel) modTable.getModel();
                        model.fireTableRowsInserted(row, row);
                        ModTextRenderer renderer = (ModTextRenderer) modTable.getColumnModel().getColumn(1).getCellRenderer();
                        renderer.resetRowHeights();
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
        });

        removeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int row = modTable.getSelectedRow();
                Mod mod = (Mod) modTable.getModel().getValueAt(row, 0);
                if (mod instanceof ExternalMod) {
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
            class PatchThread implements Runnable {
                public void run() {
                    try {
                        if (!MCPatcher.patch()) {
                            tabbedPane.setSelectedIndex(TAB_LOG);
                            JOptionPane.showMessageDialog(frame,
                                "There was an error during patching.  " +
                                    "See log for more information.  " +
                                    "Your original minecraft.jar has been restored.",
                                "Error", JOptionPane.ERROR_MESSAGE
                            );
                        }
                    } catch (Throwable e) {
                        Logger.log(e);
                        tabbedPane.setSelectedIndex(TAB_LOG);
                    } finally {
                        setBusy(false);
                    }
                }
            }

            public void actionPerformed(ActionEvent e) {
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
                HashMap<String, ArrayList<Mod>> conflicts = MCPatcher.getConflicts();
                if (!conflicts.isEmpty()) {
                    ConflictDialog dialog = new ConflictDialog(conflicts);
                    int result = dialog.getResult(mainPanel);
                    if (result != JOptionPane.YES_OPTION) {
                        return;
                    }
                }
                setBusy(true);
                setStatusText("Patching %s...", MCPatcher.minecraft.getOutputFile().getName());
                runWorker(new PatchThread());
            }
        });

        undoButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setStatusText("");
                try {
                    MCPatcher.minecraft.restoreBackup();
                    MinecraftJar.setDefaultTexturePack();
                    JOptionPane.showMessageDialog(frame, "Restored original minecraft jar and reset texture pack to default.", "", JOptionPane.INFORMATION_MESSAGE);
                } catch (IOException e1) {
                    Logger.log(e1);
                    JOptionPane.showMessageDialog(frame, "Failed to restore minecraft jar from backup:\n\n" + e1.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
                updateControls();
            }
        });

        refreshButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setStatusText("");
                MCPatcher.getAllMods();
                updateModList();
            }
        });

        testButton.addActionListener(new ActionListener() {
            class MinecraftThread implements Runnable {
                public void run() {
                    MCPatcher.minecraft.run();
                    setBusy(false);
                }
            }

            public void actionPerformed(ActionEvent e) {
                tabbedPane.setSelectedIndex(TAB_LOG);
                setBusy(true);
                setStatusText("Launching %s...", MCPatcher.minecraft.getOutputFile().getName());
                MCPatcher.saveProperties();
                runWorker(new MinecraftThread());
            }
        });

        tabbedPane.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                updateActiveTab();
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
    }

    private static String getModWarningText(MinecraftVersion version) {
        return "Your minecraft.jar appears to be already modded.\n" +
            "It is highly recommended that you install mods via MCPatcher instead.\n" +
            " - Close MCPatcher.\n" +
            " - Delete both minecraft.jar and minecraft-" + version + ".jar.\n" +
            (version.isPrerelease() ?
                " - Re-download the snapshot from mojang.com.\n" :
                " - Re-download the game using the launcher.\n") +
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

    void show() {
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    File chooseMinecraftDir(File minecraftDir) {
        JOptionPane.showMessageDialog(null,
            "Minecraft not found in\n" +
                minecraftDir.getPath() + "\n\n" +
                "If the game is installed somewhere else, please select the game\n" +
                "folder (the one containing bin, resources, saves, etc., subfolders).",
            "Minecraft not found", JOptionPane.ERROR_MESSAGE
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
                "http://www.minecraftforum.net/topic/1496369-",
            "For testing only", JOptionPane.INFORMATION_MESSAGE
        );
    }

    void showCorruptJarError(File defaultMinecraft) {
        if (defaultMinecraft.exists()) {
            tabbedPane.setSelectedIndex(TAB_LOG);
            JOptionPane.showMessageDialog(frame,
                "There was an error opening minecraft.jar. This may be because:\n" +
                    " - You selected the launcher jar and not the main minecraft.jar in the bin folder.\n" +
                    " - You selected a texture pack and not minecraft.jar.\n" +
                    " - The file has already been patched.\n" +
                    " - There was an update that this patcher cannot handle.\n" +
                    " - There is another, conflicting mod applied.\n" +
                    " - The jar file is invalid or corrupt.\n" +
                    "\n" +
                    "You can re-download the original minecraft.jar by using the Force Update\n" +
                    "button in the Minecraft Launcher.\n",
                "Invalid or Corrupt minecraft.jar", JOptionPane.ERROR_MESSAGE
            );
        } else {
            JOptionPane.showMessageDialog(frame,
                "Could not find minecraft.jar in\n" +
                    defaultMinecraft.getParentFile().getPath() + "\n" +
                    "\n" +
                    "Use the Browse button to select a different\n" +
                    "input file before patching.",
                "Missing minecraft.jar", JOptionPane.ERROR_MESSAGE
            );
        }
    }

    void showTexturePackConverter() {
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
                return name.endsWith(".zip") && !name.startsWith("mcpatcher-converted-");
            }

            @Override
            public String getDescription() {
                return "texture packs (*.zip)";
            }
        });
        if (fd.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            final File selectedFile = fd.getSelectedFile();
            cancelWorker();
            setBusy(true);
            runWorker(new Runnable() {
                public void run() {
                    final TexturePackConverter converter = new TexturePackConverter(selectedFile);
                    boolean result = converter.convert(MCPatcher.ui);
                    StringBuilder sb = new StringBuilder();
                    for (String s : converter.getMessages()) {
                        sb.append(s).append('\n');
                    }
                    setBusy(false);
                    if (!result) {
                        JOptionPane.showMessageDialog(frame, sb.toString(),
                            "Error converting " + selectedFile.getName(),
                            JOptionPane.ERROR_MESSAGE
                        );
                        tabbedPane.setSelectedIndex(TAB_LOG);
                    } else if (sb.length() > 0) {
                        JOptionPane.showMessageDialog(frame, sb.toString(),
                            "Warnings while converting " + selectedFile.getName(),
                            JOptionPane.WARNING_MESSAGE
                        );
                        tabbedPane.setSelectedIndex(TAB_LOG);
                    } else {
                        JOptionPane.showMessageDialog(frame,
                            "Successfully converted " + selectedFile.getName() + "\n" +
                                "New texture pack is called " + converter.getOutputFile().getName(),
                            "Done",
                            JOptionPane.INFORMATION_MESSAGE
                        );
                    }
                }
            });
        }
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

    private void runWorker(Runnable runnable) {
        cancelWorker();
        workerThread = new Thread(runnable);
        workerThread.start();
    }

    synchronized void setBusy(boolean busy) {
        this.busy = busy;
        if (!busy) {
            setStatusText("");
            updateProgress(0, 0);
        }
        updateControls();
    }

    synchronized void setStatusText(String format, Object... params) {
        statusText.setText(String.format(format, params));
    }

    synchronized void updateProgress(int value, int max) {
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

    void updateControls() {
        String currentProfile = Config.instance.getConfigValue(Config.TAG_SELECTED_PROFILE);
        if (currentProfile == null || currentProfile.equals("")) {
            frame.setTitle("MCPatcher " + MCPatcher.VERSION_STRING);
        } else {
            frame.setTitle("MCPatcher " + MCPatcher.VERSION_STRING + " [" + currentProfile + "]");
        }
        if (MCPatcher.minecraft == null) {
            origField.setText("");
            outputField.setText("");
        } else {
            origField.setText(MCPatcher.minecraft.getInputFile().getPath());
            outputField.setText(MCPatcher.minecraft.getOutputFile().getPath());
        }
        origField.setToolTipText(origField.getText());
        outputField.setToolTipText(outputField.getText());
        boolean outputSet = !outputField.getText().equals("");
        File orig = new File(origField.getText());
        File output = new File(outputField.getText());
        boolean origOk = orig.exists();
        boolean outputOk = output.exists();
        origBrowseButton.setEnabled(!busy);
        outputBrowseButton.setEnabled(!busy);
        modTable.setEnabled(!busy && origOk && outputSet);
        upButton.setEnabled(!busy && origOk);
        downButton.setEnabled(!busy && origOk);
        addButton.setEnabled(!busy && origOk);
        removeButton.setEnabled(!busy && origOk);
        refreshButton.setEnabled(!busy && origOk);
        testButton.setEnabled(!busy && outputOk && MCPatcherUtils.getMinecraftPath().equals(MCPatcherUtils.getDefaultGameDir()));
        patchButton.setEnabled(!busy && origOk && !output.equals(orig));
        undoButton.setEnabled(!busy && origOk && !output.equals(orig));
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
        MCPatcher.saveProperties();
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
        MCPatcher.showClassMaps(out);
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

    void updateModList() {
        setBusy(true);
        setStatusText("Analyzing %s...", MCPatcher.minecraft.getInputFile().getName());
        runWorker(new Runnable() {
            public void run() {
                try {
                    MCPatcher.getApplicableMods();
                    if (MCPatcher.minecraft.isModded()) {
                        JOptionPane.showMessageDialog(frame,
                            getModWarningText(MCPatcher.minecraft.getVersion()),
                            "Warning", JOptionPane.WARNING_MESSAGE
                        );
                    }
                } catch (InterruptedException e) {
                } catch (IOException e) {
                    Logger.log(e);
                    showCorruptJarError(MCPatcher.minecraft.getInputFile());
                }
                redrawModList();
                setBusy(false);
            }
        });
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
        private HashMap<Integer, Integer> rowSizeFull = new HashMap<Integer, Integer>();
        private HashMap<Integer, Integer> rowSizeShort = new HashMap<Integer, Integer>();

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
        JTextArea textArea;

        public CopyToClipboardListener(JTextArea textArea) {
            this.textArea = textArea;
        }

        public void actionPerformed(ActionEvent e) {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                new StringSelection("[spoiler][code]\n" + textArea.getText() + "[/code][/spoiler]\n"), null
            );
        }
    }
}
