package com.prupe.mcpatcher;

import com.prupe.mcpatcher.basemod.ProfilerMod;
import com.prupe.mcpatcher.launcher.version.Library;
import javassist.bytecode.AccessFlag;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.prupe.mcpatcher.BinaryRegex.begin;
import static com.prupe.mcpatcher.BytecodeMatcher.registerLoadStore;
import static javassist.bytecode.Opcode.*;

/**
 * Internal mod required by the patcher.  Responsible for injecting basic MCPatcher runtime classes
 * into minecraft.jar.
 */
public final class BaseMod extends Mod {
    private static final Pattern BLANK_PNG_PATTERN = Pattern.compile(MCPatcherUtils.BLANK_PNG_FORMAT.replaceFirst("%\\d*x", "(\\\\p{XDigit}+)"));

    private final boolean haveProfiler;

    BaseMod() {
        name = MCPatcherUtils.BASE_MOD;
        author = "MCPatcher";
        description = "Internal mod required by the patcher.";
        version = "1.4";
        configPanel = new ConfigPanel();
        clearDependencies();

        haveProfiler = getMinecraftVersion().compareTo("1.3") >= 0;

        if (getMinecraftVersion().compareTo("1.6") >= 0) {
            addClassMod(new MainMod());
        }
        addClassMod(new XMinecraftMod());
        addClassMod(new GameSettingsMod());
        if (haveProfiler) {
            addClassMod(new ProfilerMod(this));
        }

        addClassFile(MCPatcherUtils.UTILS_CLASS);
        addClassFile(MCPatcherUtils.LOGGER_CLASS);
        addClassFile(MCPatcherUtils.LOGGER_CLASS + "$1");
        addClassFile(MCPatcherUtils.LOGGER_CLASS + "$1$1");
        addClassFile(MCPatcherUtils.LOGGER_CLASS + "$ErrorLevel");
        addClassFile(MCPatcherUtils.CONFIG_CLASS);
        addClassFile(MCPatcherUtils.CONFIG_CLASS + "$ProfileEntry");
        addClassFile(MCPatcherUtils.CONFIG_CLASS + "$VersionEntry");
        addClassFile(MCPatcherUtils.CONFIG_CLASS + "$ModEntry");
        addClassFile(MCPatcherUtils.CONFIG_CLASS + "$FileEntry");
        addClassFile(MCPatcherUtils.JSON_UTILS_CLASS);
        if (haveProfiler) {
            addClassFile(MCPatcherUtils.PROFILER_API_CLASS);
        }
        addClassFile(MCPatcherUtils.MAL_CLASS);

        for (int i : new int[]{0, 0x80808080, 0xffffffff}) {
            addFile(String.format("%s" + MCPatcherUtils.BLANK_PNG_FORMAT, "assets/minecraft/", i));
        }
    }

    @Override
    public String[] getLoggingCategories() {
        return null;
    }

    @Override
    public InputStream openFile(String name) throws IOException {
        Matcher m = BLANK_PNG_PATTERN.matcher(name);
        if (m.find()) {
            BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            int color = 0;
            try {
                color = (int) Long.parseLong(m.group(1), 16);
            } catch (NumberFormatException e) {
                Logger.log(e);
            }
            for (int i = 0; i < image.getWidth(); i++) {
                for (int j = 0; j < image.getHeight(); j++) {
                    image.setRGB(i, j, color);
                }
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(image, "png", output);
            return new ByteArrayInputStream(output.toByteArray());
        } else {
            return super.openFile(name);
        }
    }

    @Override
    public void addExtraLibraries(List<Library> libraries) {
        libraries.add(new Library("com.google.code.gson:gson:2.2.4"));
    }

    class ConfigPanel extends ModConfigPanel {
        private JPanel panel;
        private JCheckBox fetchURLCheckBox;
        private JCheckBox profilingCheckBox;
        private JTable logTable;

        ConfigPanel() {
            fetchURLCheckBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Config.getInstance().fetchRemoteVersionList = fetchURLCheckBox.isSelected();
                }
            });

            profilingCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Config.getInstance().extraProfiling = profilingCheckBox.isSelected();
                }
            });

            logTable.setModel(new TableModel() {
                private List<String> getCategories() {
                    List<String> allCategories = new ArrayList<String>();
                    for (Mod mod : MCPatcher.modList.getAll()) {
                        String[] categories = mod.getLoggingCategories();
                        if (categories != null) {
                            for (String category : categories) {
                                if (category != null) {
                                    allCategories.add(category);
                                }
                            }
                        }
                    }
                    return allCategories;
                }

                private String getCategory(int rowIndex) {
                    List<String> categories = getCategories();
                    return rowIndex >= 0 && rowIndex < categories.size() ? categories.get(rowIndex) : null;
                }

                public int getRowCount() {
                    return getCategories().size();
                }

                public int getColumnCount() {
                    return 2;
                }

                public String getColumnName(int columnIndex) {
                    return null;
                }

                public Class<?> getColumnClass(int columnIndex) {
                    return String.class;
                }

                public boolean isCellEditable(int rowIndex, int columnIndex) {
                    return columnIndex == 1;
                }

                public Object getValueAt(int rowIndex, int columnIndex) {
                    String category = getCategory(rowIndex);
                    if (category == null) {
                        return null;
                    }
                    return columnIndex == 0 ? category : Config.getLogLevel(category);
                }

                public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
                    String category = getCategory(rowIndex);
                    if (columnIndex != 1 || category == null) {
                        return;
                    }
                    try {
                        Config.setLogLevel(category, Level.parse(aValue.toString()));
                    } catch (IllegalArgumentException e) {
                    }
                }

                public void addTableModelListener(TableModelListener l) {
                }

                public void removeTableModelListener(TableModelListener l) {
                }
            });

            JComboBox combo = new JComboBox();
            combo.addItem(Level.OFF);
            combo.addItem(Level.SEVERE);
            combo.addItem(Level.WARNING);
            combo.addItem(Level.INFO);
            combo.addItem(Level.CONFIG);
            combo.addItem(Level.FINE);
            combo.addItem(Level.FINER);
            combo.addItem(Level.FINEST);
            combo.addItem(Level.ALL);

            logTable.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(combo));
        }

        @Override
        public JPanel getPanel() {
            return panel;
        }

        @Override
        public String getPanelName() {
            return "General options";
        }

        @Override
        public void load() {
            Config config = Config.getInstance();
            fetchURLCheckBox.setSelected(config.fetchRemoteVersionList);
            profilingCheckBox.setSelected(config.extraProfiling);
            showAdvancedOption(profilingCheckBox);
        }

        @Override
        public void save() {
        }
    }

    private class MainMod extends ClassMod {
        MainMod() {
            addClassSignature(new FilenameSignature(ClassMap.classNameToFilename("net.minecraft.client.main.Main")));

            final MethodRef main = new MethodRef(getDeobfClass(), "main", "([Ljava/lang/String;)V");
            final MethodRef dumpCommandLine = new MethodRef(MCPatcherUtils.UTILS_CLASS, "dumpCommandLine", "([Ljava/lang/String;)V");

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "dump args";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        begin()
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // MCPatcherUtils.setCommandLine(args);
                        ALOAD_0,
                        reference(INVOKESTATIC, dumpCommandLine)
                    );
                }
            }.targetMethod(main));
        }
    }

    private class XMinecraftMod extends com.prupe.mcpatcher.basemod.MinecraftMod {
        XMinecraftMod() {
            super(BaseMod.this);

            if (haveProfiler) {
                addMemberMapper(new FieldMapper(new FieldRef(getDeobfClass(), "mcProfiler", "LProfiler;")));
            }

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "MCPatcherUtils.setMinecraft(this)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        begin(),
                        ALOAD_0,
                        reference(INVOKESPECIAL, new MethodRef("java.lang.Object", "<init>", "()V"))
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    // (Lnet/minecraft/src/Session;IIZZLjava/io/File;Ljava/io/File;Ljava/io/File;Ljava/net/Proxy;Ljava/lang/String;)V
                    List<String> descriptor = ConstPoolUtils.parseDescriptor(getMethodInfo().getDescriptor());
                    return buildCode(
                        getFileArgument(descriptor, 6),
                        getFileArgument(descriptor, 7),
                        push(getMinecraftVersion().getVersionString()),
                        push(MCPatcher.VERSION_STRING),
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.UTILS_CLASS, "setMinecraft", "(Ljava/io/File;Ljava/io/File;Ljava/lang/String;Ljava/lang/String;)V"))
                    );
                }

                private byte[] getFileArgument(List<String> descriptor, int index) {
                    if (descriptor.size() > index - 1 && "Ljava/io/File;".equals(descriptor.get(index - 1))) {
                        return registerLoadStore(ALOAD, index);
                    } else {
                        return buildCode(ACONST_NULL);
                    }
                }
            }
                .setInsertAfter(true)
                .matchConstructorOnly(true)
            );

            if (!haveGetInstance) {
                addPatch(new AddFieldPatch(instance, AccessFlag.PUBLIC | AccessFlag.STATIC));

                addPatch(new BytecodePatch() {
                    @Override
                    public String getDescription() {
                        return "set instance";
                    }

                    @Override
                    public String getMatchExpression() {
                        return buildExpression(
                            begin(),
                            ALOAD_0,
                            reference(INVOKESPECIAL, new MethodRef("java.lang.Object", "<init>", "()V"))
                        );
                    }

                    @Override
                    public byte[] getReplacementBytes() {
                        return buildCode(
                            ALOAD_0,
                            reference(PUTSTATIC, instance)
                        );
                    }
                }
                    .setInsertAfter(true)
                    .matchConstructorOnly(true)
                );

                addPatch(new AddMethodPatch(getInstance, AccessFlag.PUBLIC | AccessFlag.STATIC) {
                    @Override
                    public byte[] generateMethod() {
                        return buildCode(
                            reference(GETSTATIC, instance),
                            ARETURN
                        );
                    }
                });
            }
        }

        @Override
        public String getDeobfClass() {
            return "Minecraft";
        }
    }

    private class GameSettingsMod extends ClassMod {
        private static final String OPTIONS_TXT = "options.txt";

        GameSettingsMod() {
            final ClassRef fileClass = new ClassRef("java/io/File");
            final MethodRef fileConstructor = new MethodRef("java/io/File", "<init>", "(Ljava/io/File;Ljava/lang/String;)V");
            final MethodRef getOptionsTxt = new MethodRef(MCPatcherUtils.CONFIG_CLASS, "getOptionsTxt", "(Ljava/io/File;Ljava/lang/String;)Ljava/io/File;");

            setMultipleMatchesAllowed(true);

            addClassSignature(new ConstSignature(OPTIONS_TXT));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "use options.<version>.txt if present";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // new File(mcDir, "options.txt")
                        reference(NEW, fileClass),
                        DUP,
                        ALOAD_2,
                        push(OPTIONS_TXT),
                        reference(INVOKESPECIAL, fileConstructor)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // Config.getOptionsTxt(mcDir, "options.txt")
                        ALOAD_2,
                        push(OPTIONS_TXT),
                        reference(INVOKESTATIC, getOptionsTxt)
                    );
                }
            }.matchConstructorOnly(true));
        }
    }

    @Deprecated
    public static class MinecraftMod extends com.prupe.mcpatcher.basemod.MinecraftMod {
        public MinecraftMod(Mod mod) {
            super(mod);
        }

        public MinecraftMod mapWorldClient() {
            super.mapWorldClient();
            return this;
        }

        public MinecraftMod mapPlayer() {
            super.mapPlayer();
            return this;
        }

        public MinecraftMod addWorldGetter() {
            super.addWorldGetter();
            return this;
        }
    }
}
