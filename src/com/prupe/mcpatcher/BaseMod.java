package com.prupe.mcpatcher;

import com.prupe.mcpatcher.basemod.PositionMod;
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

import static com.prupe.mcpatcher.BinaryRegex.*;
import static com.prupe.mcpatcher.BytecodeMatcher.*;
import static javassist.bytecode.Opcode.*;

/**
 * Internal mod required by the patcher.  Responsible for injecting MCPatcherUtils classes
 * into minecraft.jar.
 * <p/>
 * Also provides a collection of commonly used ClassMods as public static inner classes that
 * can be instantiated or extended as needed.
 */
public final class BaseMod extends Mod {
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
            addClassMod(new XMainMod());
        }
        addClassMod(new XMinecraftMod());
        addClassMod(new XGameSettingsMod());
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

        addFile("assets/minecraft/" + MCPatcherUtils.BLANK_PNG);
    }

    @Override
    public String[] getLoggingCategories() {
        return null;
    }

    @Override
    public InputStream openFile(String name) throws IOException {
        if (name.endsWith(MCPatcherUtils.BLANK_PNG)) {
            BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
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

    private class XMainMod extends ClassMod {
        XMainMod() {
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

        @Override
        public String getDeobfClass() {
            return "Main";
        }
    }

    private class XMinecraftMod extends MinecraftMod {
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

    private class XGameSettingsMod extends ClassMod {
        private static final String OPTIONS_TXT = "options.txt";

        XGameSettingsMod() {
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

        @Override
        public String getDeobfClass() {
            return "GameSettings";
        }
    }

    /**
     * Matches Minecraft class and maps the getInstance method.
     */
    public static class MinecraftMod extends com.prupe.mcpatcher.ClassMod {
        protected final FieldRef instance = new FieldRef(getDeobfClass(), "instance", "LMinecraft;");
        protected final MethodRef getInstance = new MethodRef(getDeobfClass(), "getInstance", "()LMinecraft;");
        protected final boolean haveGetInstance;

        public MinecraftMod(Mod mod) {
            super(mod);
            haveGetInstance = getMinecraftVersion().compareTo("1.3") >= 0;

            if (getMinecraftVersion().compareTo("13w16a") >= 0) {
                addClassSignature(new ConstSignature("textures/gui/title/mojang.png"));
                addClassSignature(new ConstSignature("crash-reports"));
            } else {
                addClassSignature(new FilenameSignature("net/minecraft/client/Minecraft.class"));
            }

            if (haveGetInstance) {
                addMemberMapper(new MethodMapper(getInstance)
                    .accessFlag(AccessFlag.PUBLIC, true)
                    .accessFlag(AccessFlag.STATIC, true)
                );
            }
        }

        public MinecraftMod mapWorldClient() {
            addMemberMapper(new FieldMapper(new FieldRef(getDeobfClass(), "theWorld", "LWorldClient;")));
            return this;
        }

        public MinecraftMod mapPlayer() {
            addMemberMapper(new FieldMapper(new FieldRef(getDeobfClass(), "thePlayer", "LEntityClientPlayerMP;")));
            return this;
        }

        public MinecraftMod addWorldGetter() {
            final MethodRef getWorld = new MethodRef(getDeobfClass(), "getWorld", "()LWorld;");

            if (getMinecraftVersion().compareTo("12w18a") >= 0) {
                final FieldRef worldServer = new FieldRef(getDeobfClass(), "worldServer", "LWorldServer;");
                final FieldRef world = new FieldRef("WorldServer", "world", "LWorld;");

                addMemberMapper(new FieldMapper(worldServer));

                addPatch(new AddMethodPatch(getWorld) {
                    @Override
                    public byte[] generateMethod() {
                        return buildCode(
                            ALOAD_0,
                            reference(GETFIELD, worldServer),
                            reference(GETFIELD, world),
                            ARETURN
                        );
                    }
                });
            } else {
                final FieldRef theWorld = new FieldRef(getDeobfClass(), "theWorld", "LWorld;");

                addMemberMapper(new FieldMapper(theWorld));

                addPatch(new AddMethodPatch(getWorld) {
                    @Override
                    public byte[] generateMethod() {
                        return buildCode(
                            ALOAD_0,
                            reference(GETFIELD, theWorld),
                            ARETURN
                        );
                    }
                });
            }
            return this;
        }
    }

    /**
     * Maps Profiler class and start/endSection methods.
     */
    public static class ProfilerMod extends com.prupe.mcpatcher.ClassMod {
        public ProfilerMod(Mod mod) {
            super(mod);

            addClassSignature(new ConstSignature("root"));
            addClassSignature(new ConstSignature("[UNKNOWN]"));
            addClassSignature(new ConstSignature(100.0));

            final MethodRef startSection = new MethodRef(getDeobfClass(), "startSection", "(Ljava/lang/String;)V");
            final MethodRef endSection = new MethodRef(getDeobfClass(), "endSection", "()V");
            final MethodRef endStartSection = new MethodRef(getDeobfClass(), "endStartSection", "(Ljava/lang/String;)V");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        captureReference(INVOKEVIRTUAL),
                        ALOAD_0,
                        ALOAD_1,
                        captureReference(INVOKEVIRTUAL)
                    );
                }
            }
                .setMethod(endStartSection)
                .addXref(1, endSection)
                .addXref(2, startSection)
            );
        }
    }

    /**
     * Matches Tessellator class and instance and maps several commonly used rendering methods.
     */
    public static class TessellatorMod extends com.prupe.mcpatcher.ClassMod {
        protected final MethodRef draw = new MethodRef(getDeobfClass(), "draw", "()I");
        protected final MethodRef startDrawingQuads = new MethodRef(getDeobfClass(), "startDrawingQuads", "()V");
        protected final MethodRef startDrawing = new MethodRef(getDeobfClass(), "startDrawing", "(I)V");
        protected final MethodRef addVertexWithUV = new MethodRef(getDeobfClass(), "addVertexWithUV", "(DDDDD)V");
        protected final MethodRef addVertex = new MethodRef(getDeobfClass(), "addVertex", "(DDD)V");
        protected final MethodRef setTextureUV = new MethodRef(getDeobfClass(), "setTextureUV", "(DD)V");
        protected final MethodRef setColorOpaque_F = new MethodRef(getDeobfClass(), "setColorOpaque_F", "(FFF)V");
        protected final FieldRef instance = new FieldRef(getDeobfClass(), "instance", "LTessellator;");

        public TessellatorMod(Mod mod) {
            super(mod);

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push("Not tesselating!")
                    );
                }
            }.setMethod(draw));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        push(7),
                        captureReference(INVOKEVIRTUAL),
                        RETURN
                    );
                }
            }
                .setMethod(startDrawingQuads)
                .addXref(1, startDrawing)
            );

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        DLOAD, 7,
                        DLOAD, 9,
                        captureReference(INVOKEVIRTUAL),

                        ALOAD_0,
                        DLOAD_1,
                        DLOAD_3,
                        DLOAD, 5,
                        captureReference(INVOKEVIRTUAL),

                        RETURN
                    );
                }
            }
                .setMethod(addVertexWithUV)
                .addXref(1, setTextureUV)
                .addXref(2, addVertex)
            );

            addMemberMapper(new FieldMapper(instance).accessFlag(AccessFlag.STATIC, true));
            addMemberMapper(new MethodMapper(setColorOpaque_F));
        }
    }

    /**
     * Matches IBlockAccess interface and maps getBlockId, getBlockMetadata methods.
     */
    public static class IBlockAccessMod extends com.prupe.mcpatcher.ClassMod {
        protected final boolean haveBlockRegistry;
        protected final boolean methodsRemoved;

        public IBlockAccessMod(Mod mod) {
            super(mod);
            haveBlockRegistry = getMinecraftVersion().compareTo("13w36a") >= 0;
            methodsRemoved = getMinecraftVersion().compareTo("13w38b") >= 0;
            final String d = PositionMod.getPositionDescriptor();

            addClassSignature(new InterfaceSignature(
                haveBlockRegistry ?
                    new InterfaceMethodRef(getDeobfClass(), "getBlock", "(" + d + ")LBlock;") :
                    new InterfaceMethodRef(getDeobfClass(), "getBlockId", "(III)I"),
                new InterfaceMethodRef(getDeobfClass(), "getBlockTileEntity", "(" + d + ")LTileEntity;"),
                new InterfaceMethodRef(getDeobfClass(), "getLightBrightnessForSkyBlocks", "(" + d + "I)I"),
                methodsRemoved ?
                    null : new InterfaceMethodRef(getDeobfClass(), "getBrightness", "(IIII)F"),
                methodsRemoved ?
                    null : new InterfaceMethodRef(getDeobfClass(), "getLightBrightness", "(III)F"),
                new InterfaceMethodRef(getDeobfClass(), "getBlockMetadata", "(" + d + ")I"),
                methodsRemoved ?
                    null : new InterfaceMethodRef(getDeobfClass(), "getBlockMaterial", "(III)LMaterial;"),
                methodsRemoved ?
                    null : new InterfaceMethodRef(getDeobfClass(), "isBlockOpaqueCube", "(III)Z"),
                methodsRemoved ?
                    null : new InterfaceMethodRef(getDeobfClass(), "isBlockNormalCube", "(III)Z"),
                new InterfaceMethodRef(getDeobfClass(), "isAirBlock", "(" + d + ")Z"),
                new InterfaceMethodRef(getDeobfClass(), "getBiomeGenAt", "(" + d.replaceFirst("^I", "") + ")LBiomeGenBase;"),
                new InterfaceMethodRef(getDeobfClass(), "getHeight", "()I"),
                new InterfaceMethodRef(getDeobfClass(), "extendedLevelsInChunkCache", "()Z"),
                methodsRemoved ?
                    null : new InterfaceMethodRef(getDeobfClass(), "doesBlockHaveSolidTopSurface", "(III)Z"),
                PositionMod.havePositionClass() ?
                    null : new InterfaceMethodRef(getDeobfClass(), "getWorldVec3Pool", "()LVec3Pool;"),
                new InterfaceMethodRef(getDeobfClass(), "isBlockProvidingPowerTo",
                    PositionMod.havePositionClass() ? "(LPosition;LDirection;)I" : "(IIII)I")
            ).setInterfaceOnly(true));
        }
    }

    /**
     * Matches Block class and maps blockID and blockList fields.
     */
    public static class BlockMod extends com.prupe.mcpatcher.ClassMod {
        protected final boolean haveBlockRegistry;

        private static final ArrayList<BlockSubclassEntry> subclasses = new ArrayList<BlockSubclassEntry>() {
            {
                // autogenerated by blockids.pl -- do not edit
                // (block id, field class, field name, field subclass, block name)
                add(new BlockSubclassEntry(1, "Block", "stone", "BlockStone", "stone"));
                add(new BlockSubclassEntry(2, "BlockGrass", "grass", "BlockGrass", "grass"));
                add(new BlockSubclassEntry(3, "Block", "dirt", "BlockDirt", "dirt"));
                add(new BlockSubclassEntry(4, "Block", "cobblestone", "Block", "stonebrick"));
                add(new BlockSubclassEntry(5, "Block", "planks", "BlockWood", "wood"));
                add(new BlockSubclassEntry(6, "Block", "sapling", "BlockSapling", "sapling"));
                add(new BlockSubclassEntry(7, "Block", "bedrock", "Block", "bedrock"));
                add(new BlockSubclassEntry(8, "BlockFluid", "waterMoving", "BlockFlowing", "water"));
                add(new BlockSubclassEntry(9, "Block", "waterStill", "BlockStationary", "water"));
                add(new BlockSubclassEntry(10, "BlockFluid", "lavaMoving", "BlockFlowing", "lava"));
                add(new BlockSubclassEntry(11, "Block", "lavaStill", "BlockStationary", "lava"));
                add(new BlockSubclassEntry(12, "Block", "sand", "BlockSand", "sand"));
                add(new BlockSubclassEntry(13, "Block", "gravel", "BlockGravel", "gravel"));
                add(new BlockSubclassEntry(14, "Block", "oreGold", "BlockOre", "oreGold"));
                add(new BlockSubclassEntry(15, "Block", "oreIron", "BlockOre", "oreIron"));
                add(new BlockSubclassEntry(16, "Block", "oreCoal", "BlockOre", "oreCoal"));
                add(new BlockSubclassEntry(17, "Block", "wood", "BlockLog", "log"));
                add(new BlockSubclassEntry(18, "BlockLeaves", "leaves", "BlockLeaves", "leaves"));
                add(new BlockSubclassEntry(19, "Block", "sponge", "BlockSponge", "sponge"));
                add(new BlockSubclassEntry(20, "Block", "glass", "BlockGlass", "glass"));
                add(new BlockSubclassEntry(21, "Block", "oreLapis", "BlockOre", "oreLapis"));
                add(new BlockSubclassEntry(22, "Block", "blockLapis", "Block", "blockLapis"));
                add(new BlockSubclassEntry(23, "Block", "dispenser", "BlockDispenser", "dispenser"));
                add(new BlockSubclassEntry(24, "Block", "sandStone", "BlockSandStone", "sandStone"));
                add(new BlockSubclassEntry(25, "Block", "music", "BlockNote", "musicBlock"));
                add(new BlockSubclassEntry(26, "Block", "bed", "BlockBed", "bed"));
                add(new BlockSubclassEntry(27, "Block", "railPowered", "BlockRailPowered", "goldenRail"));
                add(new BlockSubclassEntry(28, "Block", "railDetector", "BlockDetectorRail", "detectorRail"));
                add(new BlockSubclassEntry(29, "BlockPistonBase", "pistonStickyBase", "BlockPistonBase", "pistonStickyBase"));
                add(new BlockSubclassEntry(30, "Block", "web", "BlockWeb", "web"));
                add(new BlockSubclassEntry(31, "BlockTallGrass", "tallGrass", "BlockTallGrass", "tallgrass"));
                add(new BlockSubclassEntry(32, "BlockDeadBush", "deadBush", "BlockDeadBush", "deadbush"));
                add(new BlockSubclassEntry(33, "BlockPistonBase", "pistonBase", "BlockPistonBase", "pistonBase"));
                add(new BlockSubclassEntry(34, "BlockPistonExtension", "pistonExtension", "BlockPistonExtension", "unnamedBlock34"));
                add(new BlockSubclassEntry(35, "Block", "cloth", "BlockColored", "cloth"));
                add(new BlockSubclassEntry(36, "BlockPistonMoving", "pistonMoving", "BlockPistonMoving", "unnamedBlock36"));
                add(new BlockSubclassEntry(37, "BlockFlower", "plantYellow", "BlockFlower", "flower"));
                add(new BlockSubclassEntry(38, "BlockFlower", "plantRed", "BlockFlower", "rose"));
                add(new BlockSubclassEntry(39, "BlockFlower", "mushroomBrown", "BlockMushroom", "mushroom"));
                add(new BlockSubclassEntry(40, "BlockFlower", "mushroomRed", "BlockMushroom", "mushroom"));
                add(new BlockSubclassEntry(41, "Block", "blockGold", "BlockOreStorage", "blockGold"));
                add(new BlockSubclassEntry(42, "Block", "blockIron", "BlockOreStorage", "blockIron"));
                add(new BlockSubclassEntry(43, "BlockHalfSlab", "stoneDoubleSlab", "BlockStep", "stoneSlab"));
                add(new BlockSubclassEntry(44, "BlockHalfSlab", "stoneSingleSlab", "BlockStep", "stoneSlab"));
                add(new BlockSubclassEntry(45, "Block", "brick", "Block", "brick"));
                add(new BlockSubclassEntry(46, "Block", "tnt", "BlockTNT", "tnt"));
                add(new BlockSubclassEntry(47, "Block", "bookShelf", "BlockBookshelf", "bookshelf"));
                add(new BlockSubclassEntry(48, "Block", "cobblestoneMossy", "Block", "stoneMoss"));
                add(new BlockSubclassEntry(49, "Block", "obsidian", "BlockObsidian", "obsidian"));
                add(new BlockSubclassEntry(50, "Block", "torchWood", "BlockTorch", "torch"));
                add(new BlockSubclassEntry(51, "BlockFire", "fire", "BlockFire", "fire"));
                add(new BlockSubclassEntry(52, "Block", "mobSpawner", "BlockMobSpawner", "mobSpawner"));
                add(new BlockSubclassEntry(53, "Block", "stairsWoodOak", "BlockStairs", "stairsWood"));
                add(new BlockSubclassEntry(54, "BlockChest", "chest", "BlockChest", "chest"));
                add(new BlockSubclassEntry(55, "BlockRedstoneWire", "redstoneWire", "BlockRedstoneWire", "redstoneDust"));
                add(new BlockSubclassEntry(56, "Block", "oreDiamond", "BlockOre", "oreDiamond"));
                add(new BlockSubclassEntry(57, "Block", "blockDiamond", "BlockOreStorage", "blockDiamond"));
                add(new BlockSubclassEntry(58, "Block", "workbench", "BlockWorkbench", "workbench"));
                add(new BlockSubclassEntry(59, "Block", "crops", "BlockCrops", "crops"));
                add(new BlockSubclassEntry(60, "Block", "tilledField", "BlockFarmland", "farmland"));
                add(new BlockSubclassEntry(61, "Block", "furnaceIdle", "BlockFurnace", "furnace"));
                add(new BlockSubclassEntry(62, "Block", "furnaceBurning", "BlockFurnace", "furnace"));
                add(new BlockSubclassEntry(63, "Block", "signPost", "BlockSign", "sign"));
                add(new BlockSubclassEntry(64, "Block", "doorWood", "BlockDoor", "doorWood"));
                add(new BlockSubclassEntry(65, "Block", "ladder", "BlockLadder", "ladder"));
                add(new BlockSubclassEntry(66, "Block", "rail", "BlockRail", "rail"));
                add(new BlockSubclassEntry(67, "Block", "stairsCobblestone", "BlockStairs", "stairsStone"));
                add(new BlockSubclassEntry(68, "Block", "signWall", "BlockSign", "sign"));
                add(new BlockSubclassEntry(69, "Block", "lever", "BlockLever", "lever"));
                add(new BlockSubclassEntry(70, "Block", "pressurePlateStone", "BlockPressurePlate", "pressurePlate"));
                add(new BlockSubclassEntry(71, "Block", "doorIron", "BlockDoor", "doorIron"));
                add(new BlockSubclassEntry(72, "Block", "pressurePlatePlanks", "BlockPressurePlate", "pressurePlate"));
                add(new BlockSubclassEntry(73, "Block", "oreRedstone", "BlockRedstoneOre", "oreRedstone"));
                add(new BlockSubclassEntry(74, "Block", "oreRedstoneGlowing", "BlockRedstoneOre", "oreRedstone"));
                add(new BlockSubclassEntry(75, "Block", "torchRedstoneIdle", "BlockRedstoneTorch", "notGate"));
                add(new BlockSubclassEntry(76, "Block", "torchRedstoneActive", "BlockRedstoneTorch", "notGate"));
                add(new BlockSubclassEntry(77, "Block", "stoneButton", "BlockButtonStone", "button"));
                add(new BlockSubclassEntry(78, "Block", "snow", "BlockSnow", "snow"));
                add(new BlockSubclassEntry(79, "Block", "ice", "BlockIce", "ice"));
                add(new BlockSubclassEntry(80, "Block", "blockSnow", "BlockSnowBlock", "snow"));
                add(new BlockSubclassEntry(81, "Block", "cactus", "BlockCactus", "cactus"));
                add(new BlockSubclassEntry(82, "Block", "blockClay", "BlockClay", "clay"));
                add(new BlockSubclassEntry(83, "Block", "reed", "BlockReed", "reeds"));
                add(new BlockSubclassEntry(84, "Block", "jukebox", "BlockJukeBox", "jukebox"));
                add(new BlockSubclassEntry(85, "Block", "fence", "BlockFence", "fence"));
                add(new BlockSubclassEntry(86, "Block", "pumpkin", "BlockPumpkin", "pumpkin"));
                add(new BlockSubclassEntry(87, "Block", "netherrack", "BlockNetherrack", "hellrock"));
                add(new BlockSubclassEntry(88, "Block", "slowSand", "BlockSoulSand", "hellsand"));
                add(new BlockSubclassEntry(89, "Block", "glowStone", "BlockGlowStone", "lightgem"));
                add(new BlockSubclassEntry(90, "BlockPortal", "portal", "BlockPortal", "portal"));
                add(new BlockSubclassEntry(91, "Block", "pumpkinLantern", "BlockPumpkin", "litpumpkin"));
                add(new BlockSubclassEntry(92, "Block", "cake", "BlockCake", "cake"));
                add(new BlockSubclassEntry(93, "BlockRedstoneRepeater", "redstoneRepeaterIdle", "BlockRedstoneRepeater", "diode"));
                add(new BlockSubclassEntry(94, "BlockRedstoneRepeater", "redstoneRepeaterActive", "BlockRedstoneRepeater", "diode"));
                add(new BlockSubclassEntry(95, "Block", "lockedChest", "BlockLockedChest", "lockedchest"));
                add(new BlockSubclassEntry(96, "Block", "trapdoor", "BlockTrapDoor", "trapdoor"));
                add(new BlockSubclassEntry(97, "Block", "silverfish", "BlockSilverfish", "monsterStoneEgg"));
                add(new BlockSubclassEntry(98, "Block", "stoneBrick", "BlockStoneBrick", "stonebricksmooth"));
                add(new BlockSubclassEntry(99, "Block", "mushroomCapBrown", "BlockMushroomCap", "mushroom"));
                add(new BlockSubclassEntry(100, "Block", "mushroomCapRed", "BlockMushroomCap", "mushroom"));
                add(new BlockSubclassEntry(101, "Block", "fenceIron", "BlockPane", "fenceIron"));
                add(new BlockSubclassEntry(102, "Block", "thinGlass", "BlockPane", "thinGlass"));
                add(new BlockSubclassEntry(103, "Block", "melon", "BlockMelon", "melon"));
                add(new BlockSubclassEntry(104, "Block", "pumpkinStem", "BlockStem", "pumpkinStem"));
                add(new BlockSubclassEntry(105, "Block", "melonStem", "BlockStem", "pumpkinStem"));
                add(new BlockSubclassEntry(106, "Block", "vine", "BlockVine", "vine"));
                add(new BlockSubclassEntry(107, "Block", "fenceGate", "BlockFenceGate", "fenceGate"));
                add(new BlockSubclassEntry(108, "Block", "stairsBrick", "BlockStairs", "stairsBrick"));
                add(new BlockSubclassEntry(109, "Block", "stairsStoneBrick", "BlockStairs", "stairsStoneBrickSmooth"));
                add(new BlockSubclassEntry(110, "BlockMycelium", "mycelium", "BlockMycelium", "mycel"));
                add(new BlockSubclassEntry(111, "Block", "waterlily", "BlockLilyPad", "waterlily"));
                add(new BlockSubclassEntry(112, "Block", "netherBrick", "Block", "netherBrick"));
                add(new BlockSubclassEntry(113, "Block", "netherFence", "BlockFence", "netherFence"));
                add(new BlockSubclassEntry(114, "Block", "stairsNetherBrick", "BlockStairs", "stairsNetherBrick"));
                add(new BlockSubclassEntry(115, "Block", "netherStalk", "BlockNetherStalk", "netherStalk"));
                add(new BlockSubclassEntry(116, "Block", "enchantmentTable", "BlockEnchantmentTable", "enchantmentTable"));
                add(new BlockSubclassEntry(117, "Block", "brewingStand", "BlockBrewingStand", "brewingStand"));
                add(new BlockSubclassEntry(118, "BlockCauldron", "cauldron", "BlockCauldron", "cauldron"));
                add(new BlockSubclassEntry(119, "Block", "endPortal", "BlockEndPortal", "unnamedBlock119"));
                add(new BlockSubclassEntry(120, "Block", "endPortalFrame", "BlockEndPortalFrame", "endPortalFrame"));
                add(new BlockSubclassEntry(121, "Block", "whiteStone", "Block", "whiteStone"));
                add(new BlockSubclassEntry(122, "Block", "dragonEgg", "BlockDragonEgg", "dragonEgg"));
                add(new BlockSubclassEntry(123, "Block", "redstoneLampIdle", "BlockRedstoneLight", "redstoneLight"));
                add(new BlockSubclassEntry(124, "Block", "redstoneLampActive", "BlockRedstoneLight", "redstoneLight"));
                add(new BlockSubclassEntry(125, "BlockHalfSlab", "woodDoubleSlab", "BlockWoodSlab", "woodSlab"));
                add(new BlockSubclassEntry(126, "BlockHalfSlab", "woodSingleSlab", "BlockWoodSlab", "woodSlab"));
                add(new BlockSubclassEntry(127, "Block", "cocoaPlant", "BlockCocoa", "cocoa"));
                add(new BlockSubclassEntry(128, "Block", "stairsSandStone", "BlockStairs", "stairsSandStone"));
                add(new BlockSubclassEntry(129, "Block", "oreEmerald", "BlockOre", "oreEmerald"));
                add(new BlockSubclassEntry(130, "Block", "enderChest", "BlockEnderChest", "enderChest"));
                add(new BlockSubclassEntry(131, "BlockTripWireSource", "tripWireSource", "BlockTripWireSource", "tripWireSource"));
                add(new BlockSubclassEntry(132, "Block", "tripWire", "BlockTripWire", "tripWire"));
                add(new BlockSubclassEntry(133, "Block", "blockEmerald", "BlockOreStorage", "blockEmerald"));
                add(new BlockSubclassEntry(134, "Block", "stairsWoodSpruce", "BlockStairs", "stairsWoodSpruce"));
                add(new BlockSubclassEntry(135, "Block", "stairsWoodBirch", "BlockStairs", "stairsWoodBirch"));
                add(new BlockSubclassEntry(136, "Block", "stairsWoodJungle", "BlockStairs", "stairsWoodJungle"));
                add(new BlockSubclassEntry(137, "Block", "commandBlock", "BlockCommandBlock", "commandBlock"));
                add(new BlockSubclassEntry(138, "BlockBeacon", "beacon", "BlockBeacon", "beacon"));
                add(new BlockSubclassEntry(139, "Block", "cobblestoneWall", "BlockWall", "cobbleWall"));
                add(new BlockSubclassEntry(140, "Block", "flowerPot", "BlockFlowerPot", "flowerPot"));
                add(new BlockSubclassEntry(141, "Block", "carrot", "BlockCarrot", "carrots"));
                add(new BlockSubclassEntry(142, "Block", "potato", "BlockPotato", "potatoes"));
                add(new BlockSubclassEntry(143, "Block", "woodenButton", "BlockButtonWood", "button"));
                add(new BlockSubclassEntry(144, "Block", "skull", "BlockSkull", "skull"));
                add(new BlockSubclassEntry(145, "Block", "anvil", "BlockAnvil", "anvil"));
                add(new BlockSubclassEntry(146, "Block", "chestTrapped", "BlockChest", "chestTrap"));
                add(new BlockSubclassEntry(147, "Block", "pressurePlateGold", "BlockPressurePlateWeighted", "weightedPlate_light"));
                add(new BlockSubclassEntry(148, "Block", "pressurePlateIron", "BlockPressurePlateWeighted", "weightedPlate_heavy"));
                add(new BlockSubclassEntry(149, "BlockComparator", "redstoneComparatorIdle", "BlockComparator", "comparator"));
                add(new BlockSubclassEntry(150, "BlockComparator", "redstoneComparatorActive", "BlockComparator", "comparator"));
                add(new BlockSubclassEntry(151, "BlockDaylightDetector", "daylightSensor", "BlockDaylightDetector", "daylightDetector"));
                add(new BlockSubclassEntry(152, "Block", "blockRedstone", "BlockPoweredOre", "blockRedstone"));
                add(new BlockSubclassEntry(153, "Block", "oreNetherQuartz", "BlockOre", "netherquartz"));
                add(new BlockSubclassEntry(154, "BlockHopper", "hopperBlock", "BlockHopper", "hopper"));
                add(new BlockSubclassEntry(155, "Block", "blockNetherQuartz", "BlockQuartz", "quartzBlock"));
                add(new BlockSubclassEntry(156, "Block", "stairsNetherQuartz", "BlockStairs", "stairsQuartz"));
                add(new BlockSubclassEntry(157, "Block", "railActivator", "BlockRailPowered", "activatorRail"));
                add(new BlockSubclassEntry(158, "Block", "dropper", "BlockDropper", "dropper"));
                add(new BlockSubclassEntry(159, "Block", "clayHardenedStained", "BlockColored", "clayHardenedStained"));
                // block id 160
                // block id 161
                // block id 162
                // block id 163
                // block id 164
                // block id 165
                // block id 166
                // block id 167
                // block id 168
                // block id 169
                add(new BlockSubclassEntry(170, "Block", "hayBlock", "BlockHay", "hayBlock"));
                add(new BlockSubclassEntry(171, "Block", "woolCarpet", "BlockCarpet", "woolCarpet"));
                add(new BlockSubclassEntry(172, "Block", "clayHardened", "Block", "clayHardened"));
                add(new BlockSubclassEntry(173, "Block", "blockCoal", "Block", "blockCoal"));
            }
        };

        public BlockMod(Mod mod) {
            super(mod);
            haveBlockRegistry = getMinecraftVersion().compareTo("13w36a") >= 0;

            if (haveBlockRegistry) {
                addClassSignature(new ConstSignature("stone"));
                addClassSignature(new ConstSignature("grass"));
                addClassSignature(new ConstSignature("dirt"));
                addClassSignature(new ConstSignature(".name"));
            } else {
                addClassSignature(new ConstSignature(" is already occupied by "));

                addMemberMapper(new FieldMapper(new FieldRef(getDeobfClass(), "blockID", "I"))
                    .accessFlag(AccessFlag.PUBLIC, true)
                    .accessFlag(AccessFlag.STATIC, false)
                    .accessFlag(AccessFlag.FINAL, true)
                );

                addMemberMapper(new FieldMapper(new FieldRef(getDeobfClass(), "blocksList", "[LBlock;"))
                    .accessFlag(AccessFlag.PUBLIC, true)
                    .accessFlag(AccessFlag.STATIC, true)
                    .accessFlag(AccessFlag.FINAL, true)
                );
            }
        }

        protected void addBlockSignatures() {
            for (BlockSubclassEntry entry : subclasses) {
                addBlockSignature(entry.blockID, entry.fieldClass, entry.fieldName, entry.className, entry.blockName);
            }
        }

        protected void addBlockSignature(String name) {
            for (BlockSubclassEntry entry : subclasses) {
                if (entry.className.equals(name) || entry.blockName.equals(name) || entry.fieldName.equals(name)) {
                    addBlockSignature(entry.blockID, entry.fieldClass, entry.fieldName, entry.className, entry.blockName);
                    return;
                }
            }
            throw new RuntimeException("unknown Block subclass: " + name);
        }

        protected void addBlockSignature(int blockID) {
            for (BlockSubclassEntry entry : subclasses) {
                if (entry.blockID == blockID) {
                    addBlockSignature(entry.blockID, entry.fieldClass, entry.fieldName, entry.className, entry.blockName);
                    return;
                }
            }
            throw new RuntimeException("unknown Block subclass: block ID" + blockID);
        }

        protected void addBlockSignature(final int blockID, final String fieldClass, final String fieldName, final String className, final String blockName) {
            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        captureReference(NEW),
                        DUP,
                        blockID == 35 ? "" : push(blockID),
                        nonGreedy(any(0, 60)),
                        blockName.startsWith("unnamedBlock") ? "" : build(
                            push(blockName),
                            anyReference(INVOKEVIRTUAL)
                        ),
                        nonGreedy(any(0, 20)),
                        captureReference(PUTSTATIC)
                    );
                }
            }
                .matchStaticInitializerOnly(true)
                .addXref(1, new ClassRef(className))
                .addXref(2, new FieldRef(getDeobfClass(), fieldName, "L" + fieldClass + ";"))
            );
        }

        private static class BlockSubclassEntry {
            final int blockID;
            final String fieldClass;
            final String fieldName;
            final String className;
            final String blockName;

            BlockSubclassEntry(int blockID, String fieldClass, String fieldName, String className, String blockName) {
                this.blockID = blockID;
                this.fieldClass = fieldClass;
                this.fieldName = fieldName;
                this.className = className;
                this.blockName = blockName;
            }
        }
    }

    /**
     * Matches Item class.
     */
    public static class ItemMod extends com.prupe.mcpatcher.ClassMod {
        protected final boolean haveItemRegistry;

        protected final MethodRef getItemName = new MethodRef(getDeobfClass(), "getItemName", "()Ljava/lang/String;");

        public ItemMod(Mod mod) {
            super(mod);
            haveItemRegistry = getMinecraftVersion().compareTo("13w36a") >= 0;

            if (haveItemRegistry) {
                addClassSignature(new ConstSignature("iron_shovel"));
                addClassSignature(new ConstSignature("iron_pickaxe"));
                addClassSignature(new ConstSignature("iron_axe"));
                addClassSignature(new ConstSignature(".name"));
            } else {
                addClassSignature(new ConstSignature("CONFLICT @ "));
                addClassSignature(new ConstSignature("coal"));
            }

            addMemberMapper(new MethodMapper(getItemName)
                .accessFlag(AccessFlag.PUBLIC, true)
                .accessFlag(AccessFlag.STATIC, false)
            );
        }
    }

    /**
     * Matches World class.
     */
    public static class WorldMod extends com.prupe.mcpatcher.ClassMod {
        public WorldMod(Mod mod) {
            super(mod);
            setInterfaces("IBlockAccess");

            addClassSignature(new ConstSignature("ambient.cave.cave"));
            addClassSignature(new ConstSignature(0x3c6ef35f));
        }
    }

    public static class WorldClientMod extends com.prupe.mcpatcher.ClassMod {
        public WorldClientMod(Mod mod) {
            super(mod);
            setParentClass("World");

            addClassSignature(new ConstSignature("MpServer"));
        }
    }

    public static class WorldRendererMod extends com.prupe.mcpatcher.ClassMod {
        protected final FieldRef posX = new FieldRef(getDeobfClass(), "posX", "I");
        protected final FieldRef posY = new FieldRef(getDeobfClass(), "posY", "I");
        protected final FieldRef posZ = new FieldRef(getDeobfClass(), "posZ", "I");
        protected final FieldRef[] pos = new FieldRef[]{posX, posY, posZ};
        protected final MethodRef updateRenderer;

        public WorldRendererMod(Mod mod) {
            super(mod);

            final MethodRef glNewList = new MethodRef(MCPatcherUtils.GL11_CLASS, "glNewList", "(II)V");
            final MethodRef glTranslatef = new MethodRef(MCPatcherUtils.GL11_CLASS, "glTranslatef", "(FFF)V");

            updateRenderer = new MethodRef(getDeobfClass(), "updateRenderer",
                getMinecraftVersion().compareTo("13w41a") < 0 ? "()V" : "(LEntityLivingBase;)V"
            );

            addClassSignature(new ConstSignature(glNewList));
            addClassSignature(new ConstSignature(glTranslatef));
            addClassSignature(new ConstSignature(1.000001f));

            addClassSignature(new BytecodeSignature() {
                {
                    setMethod(updateRenderer);
                    for (int i = 0; i < pos.length; i++) {
                        addXref(i + 1, pos[i]);
                    }
                }

                @Override
                public String getMatchExpression() {
                    String exp0 = "";
                    String exp1 = "";
                    for (int i = 0; i < 3; i++) {
                        exp0 += build(
                            // i/j/k0 = this.posX/Y/Z;
                            ALOAD_0,
                            captureReference(GETFIELD),
                            anyISTORE
                        );
                        exp1 += build(
                            // i/j/k1 = this.posX/Y/Z + 16;
                            ALOAD_0,
                            backReference(i + 1),
                            push(16),
                            IADD,
                            anyISTORE
                        );
                    }
                    return buildExpression(
                        exp0,
                        exp1
                    );
                }
            });
        }
    }

    public static class EntityLivingBaseMod extends com.prupe.mcpatcher.ClassMod {
        public EntityLivingBaseMod(Mod mod) {
            super(mod);
            setParentClass("Entity");

            addClassSignature(new ConstSignature("HealF"));
            addClassSignature(new ConstSignature("Health"));
            addClassSignature(new ConstSignature("ActiveEffects"));
        }
    }

    public static class EntityLivingMod extends com.prupe.mcpatcher.ClassMod {
        public EntityLivingMod(Mod mod) {
            super(mod);
            setParentClass("EntityLivingBase");

            addClassSignature(new ConstSignature("explode"));
            addClassSignature(new ConstSignature("CanPickUpLoot"));
            addClassSignature(new ConstSignature("PersistenceRequired"));
            addClassSignature(new ConstSignature("Equipment"));
        }
    }

    public static class ResourceLocationMod extends com.prupe.mcpatcher.ClassMod {
        protected final MethodRef getNamespace = new MethodRef(getDeobfClass(), "getNamespace", "()Ljava/lang/String;");
        protected final MethodRef getPath = new MethodRef(getDeobfClass(), "getPath", "()Ljava/lang/String;");

        public ResourceLocationMod(Mod mod) {
            super(mod);

            final MethodRef indexOf = new MethodRef("java/lang/String", "indexOf", "(I)I");

            addClassSignature(new ConstSignature("minecraft"));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(58),
                        reference(INVOKEVIRTUAL, indexOf)
                    );
                }
            });

            addMemberMapper(new MethodMapper(getPath, getNamespace)
                .accessFlag(AccessFlag.PUBLIC, true)
                .accessFlag(AccessFlag.STATIC, false)
            );
        }
    }

    public static class ResourceLocationSignature extends BytecodeSignature {
        protected static final ClassRef resourceLocationClass = new ClassRef("ResourceLocation");
        protected static final MethodRef resourceLocationInit1 = new MethodRef("ResourceLocation", "<init>", "(Ljava/lang/String;)V");
        protected final FieldRef mappedField;
        protected final String path;

        public ResourceLocationSignature(com.prupe.mcpatcher.ClassMod classMod, FieldRef mappedField, String path) {
            super(classMod);
            this.mappedField = mappedField;
            this.path = path;

            matchStaticInitializerOnly(true);
            addXref(1, resourceLocationClass);
            addXref(2, resourceLocationInit1);
            addXref(3, mappedField);
        }

        @Override
        public String getMatchExpression() {
            return buildExpression(
                captureReference(NEW),
                DUP,
                push(getResourcePath()),
                captureReference(INVOKESPECIAL),
                captureReference(PUTSTATIC)
            );
        }

        protected String getResourcePath() {
            return path;
        }
    }

    public static class ResourceMod extends com.prupe.mcpatcher.ClassMod {
        public ResourceMod(Mod mod) {
            super(mod);

            addClassSignature(new InterfaceSignature(
                getMinecraftVersion().compareTo("13w25a") >= 0 ? null : new InterfaceMethodRef(getDeobfClass(), "getAddress", "()LResourceLocation;"),
                new InterfaceMethodRef(getDeobfClass(), "getInputStream", "()Ljava/io/InputStream;"),
                new InterfaceMethodRef(getDeobfClass(), "isPresent", "()Z"),
                new InterfaceMethodRef(getDeobfClass(), "getMCMeta", "(Ljava/lang/String;)LMetadataSection;")
            ));
        }
    }

    /*
     * Matches FontRenderer class and maps charWidth, fontTextureName, and spaceWidth fields.
     */
    public static class FontRendererMod extends com.prupe.mcpatcher.ClassMod {
        public FontRendererMod(Mod mod) {
            super(mod);

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        begin(),
                        ALOAD_0,
                        anyReference(INVOKESPECIAL),
                        ALOAD_0,
                        push(256),
                        NEWARRAY, T_INT,
                        captureReference(PUTFIELD)
                    );
                }
            }
                .matchConstructorOnly(true)
                .addXref(1, new FieldRef(getDeobfClass(), "charWidth", "[I"))
            );

            addClassSignature(new OrSignature(
                new ConstSignature("0123456789abcdef"),
                new ConstSignature("0123456789abcdefk"),
                new ConstSignature("font/glyph_sizes.bin")
            ));
        }
    }

    /**
     * Matches RenderBlocks class.
     */
    public static class RenderBlocksMod extends com.prupe.mcpatcher.ClassMod {
        protected final MethodRef renderStandardBlockWithAmbientOcclusion = new MethodRef(getDeobfClass(), "renderStandardBlockWithAmbientOcclusion", "(LBlock;IIIFFF)Z");
        protected final FieldRef renderAllFaces = new FieldRef(getDeobfClass(), "renderAllFaces", "Z");
        protected final FieldRef blockAccess = new FieldRef(getDeobfClass(), "blockAccess", "LIBlockAccess;");
        protected final MethodRef shouldSideBeRendered = new MethodRef("Block", "shouldSideBeRendered", "(LIBlockAccess;IIII)Z");

        protected final com.prupe.mcpatcher.BytecodeSignature grassTopSignature;
        protected int useColorRegister;

        public RenderBlocksMod(Mod mod) {
            super(mod);

            final MethodRef strEquals = new MethodRef("java/lang/String", "equals", "(Ljava/lang/Object;)Z");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(0x0f000f)
                    );
                }
            }.setMethod(renderStandardBlockWithAmbientOcclusion));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        captureReference(GETFIELD),
                        IFNE, any(2),
                        ALOAD_1,
                        ALOAD_0,
                        captureReference(GETFIELD),
                        ILOAD_2,
                        ILOAD_3,
                        push(1),
                        ISUB,
                        ILOAD, 4,
                        push(0),
                        captureReference(INVOKEVIRTUAL),
                        IFEQ, any(2)
                    );
                }
            }
                .setMethod(renderStandardBlockWithAmbientOcclusion)
                .addXref(1, renderAllFaces)
                .addXref(2, blockAccess)
                .addXref(3, shouldSideBeRendered)
            );

            addClassSignature(new ConstSignature(0.1875));
            addClassSignature(new ConstSignature(0.01));

            grassTopSignature = new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push("grass_top"),
                        reference(INVOKEVIRTUAL, strEquals),
                        IFEQ, any(2),
                        // useColor = false;
                        push(0),
                        capture(anyISTORE),
                        GOTO, any(2)
                    );
                }

                @Override
                public boolean afterMatch() {
                    useColorRegister = extractRegisterNum(getCaptureGroup(1));
                    return true;
                }
            };
        }
    }

    /**
     * Maps TextureUtilsClass (1.6).
     */
    public static class TextureUtilMod extends com.prupe.mcpatcher.ClassMod {
        protected final MethodRef glTexSubImage2D = new MethodRef(MCPatcherUtils.GL11_CLASS, "glTexSubImage2D", "(IIIIIIIILjava/nio/IntBuffer;)V");
        protected final MethodRef glTexParameteri = new MethodRef(MCPatcherUtils.GL11_CLASS, "glTexParameteri", "(III)V");
        protected final MethodRef glTexParameterf = new MethodRef(MCPatcherUtils.GL11_CLASS, "glTexParameterf", "(IIF)V");
        protected final MethodRef glTexImage2D = new MethodRef(MCPatcherUtils.GL11_CLASS, "glTexImage2D", "(IIIIIIIILjava/nio/IntBuffer;)V");

        public TextureUtilMod(Mod mod) {
            super(mod);

            addClassSignature(new ConstSignature(glTexImage2D));
            addClassSignature(new ConstSignature(glTexSubImage2D));
            addClassSignature(new OrSignature(
                new ConstSignature(glTexParameteri),
                new ConstSignature(glTexParameterf)
            ));
        }
    }

    /**
     * Maps TextureObject interface (1.6+).
     */
    public static class TextureObjectMod extends com.prupe.mcpatcher.ClassMod {
        public TextureObjectMod(Mod mod) {
            super(mod);

            addClassSignature(new InterfaceSignature(
                new InterfaceMethodRef(getDeobfClass(), "load", "(LResourceManager;)V"),
                new InterfaceMethodRef(getDeobfClass(), "getGLTexture", "()I")
            ).setInterfaceOnly(true));
        }
    }

    /**
     * Maps AbstractTexture class (1.6+).
     */
    public static class AbstractTextureMod extends com.prupe.mcpatcher.ClassMod {
        protected final FieldRef glTextureId = new FieldRef(getDeobfClass(), "glTextureId", "I");
        protected final MethodRef getGLTextureId = new MethodRef(getDeobfClass(), "getGlTextureId", "()I");

        public AbstractTextureMod(Mod mod) {
            super(mod);
            setInterfaces("TextureObject");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        push(-1),
                        anyReference(PUTFIELD)
                    );
                }
            }.matchConstructorOnly(true));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        begin(),
                        ALOAD_0,
                        GETFIELD, capture(any(2)),
                        push(-1),
                        IF_ICMPNE, any(2),

                        ALOAD_0,
                        captureReference(INVOKESTATIC),
                        PUTFIELD, backReference(1)
                    );
                }
            }
                .setMethod(getGLTextureId)
                .addXref(2, new MethodRef("TextureUtil", "newGLTexture", "()I"))
            );

            addMemberMapper(new FieldMapper(glTextureId));
        }
    }

    /**
     * Maps Texture class and various fields and methods.
     */
    public static class TextureMod extends com.prupe.mcpatcher.ClassMod {
        protected final FieldRef glTextureId = new FieldRef(getDeobfClass(), "glTextureId", "I");
        protected final FieldRef rgb = new FieldRef(getDeobfClass(), "rgb", "[I");
        protected final MethodRef getGlTextureId = new MethodRef(getDeobfClass(), "getGlTextureId", "()I");
        protected final MethodRef getWidth = new MethodRef(getDeobfClass(), "getWidth", "()I");
        protected final MethodRef getHeight = new MethodRef(getDeobfClass(), "getHeight", "()I");
        protected final MethodRef getRGB = new MethodRef(getDeobfClass(), "getRGB", "()[I");

        public TextureMod(Mod mod) {
            super(mod);
            setParentClass("AbstractTexture");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        ILOAD_1,
                        ILOAD_2,
                        IMUL,
                        NEWARRAY, T_INT,
                        captureReference(PUTFIELD)
                    );
                }
            }
                .matchConstructorOnly(true)
                .addXref(1, rgb)
            );

            addMemberMapper(new MethodMapper(getRGB));
        }
    }

    /**
     * Maps SimpleTexture class (1.6+).
     */
    public static class SimpleTextureMod extends com.prupe.mcpatcher.ClassMod {
        protected final FieldRef textureName = new FieldRef(getDeobfClass(), "address", "LResourceLocation;");
        protected final MethodRef load = new MethodRef(getDeobfClass(), "load", "(LResourceManager;)V");

        public SimpleTextureMod(Mod mod) {
            super(mod);
            setParentClass("AbstractTexture");

            final MethodRef imageRead = new MethodRef("javax/imageio/ImageIO", "read", "(Ljava/io/InputStream;)Ljava/awt/image/BufferedImage;");

            addClassSignature(new ConstSignature("texture"));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        reference(INVOKESTATIC, imageRead),
                        anyASTORE
                    );
                }
            }.setMethod(load));

            addMemberMapper(new FieldMapper(textureName));
        }
    }

    /**
     * Maps TextureAtlasSprite class.
     */
    public static class TextureAtlasSpriteMod extends com.prupe.mcpatcher.ClassMod {
        protected final FieldRef textureName = new FieldRef(getDeobfClass(), "textureName", "Ljava/lang/String;");

        public TextureAtlasSpriteMod(Mod mod) {
            super(mod);
            setInterfaces("Icon");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(repeat(build(
                        push(0.009999999776482582),
                        anyILOAD,
                        I2D,
                        DDIV,
                        D2F,
                        anyFSTORE
                    ), 2));
                }
            });

            addMemberMapper(new FieldMapper(textureName));
        }
    }

    /**
     * Maps DynamicTexture class.
     */
    public static class DynamicTextureMod extends com.prupe.mcpatcher.ClassMod {
        protected final MethodRef getRGB = new MethodRef(getDeobfClass(), "getRGB", "()[I");

        public DynamicTextureMod(Mod mod) {
            super(mod);
            setParentClass("AbstractTexture");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        ILOAD_1,
                        ILOAD_2,
                        IMUL,
                        NEWARRAY, T_INT,
                        anyReference(PUTFIELD)
                    );
                }
            }
                .matchConstructorOnly(true)
                .setMethod(new MethodRef(getDeobfClass(), "<init>", "(II)V"))
            );

            addMemberMapper(new MethodMapper(getRGB));
        }
    }

    /**
     * Maps TextureAtlas class.
     */
    public static class TextureAtlasMod extends com.prupe.mcpatcher.ClassMod {
        protected final FieldRef basePath = new FieldRef(getDeobfClass(), "basePath", "Ljava/lang/String;");
        protected final FieldRef texturesByName = new FieldRef(getDeobfClass(), "texturesByName", "Ljava/util/Map;");
        protected final MethodRef refreshTextures1 = new MethodRef(getDeobfClass(), "refreshTextures1", "(LResourceManager;)V");
        protected final MethodRef refreshTextures2 = new MethodRef(getDeobfClass(), "refreshTextures2", "(LResourceManager;)V");
        protected final MethodRef registerIcon = new MethodRef(getDeobfClass(), "registerIcon", "(Ljava/lang/String;)LIcon;");

        public TextureAtlasMod(Mod mod) {
            super(mod);
            setParentClass("AbstractTexture");
            setInterfaces("TickableTextureObject", "IconRegister");

            final InterfaceMethodRef mapEntrySet = new InterfaceMethodRef("java/util/Map", "entrySet", "()Ljava/util/Set;");
            final InterfaceMethodRef setIterator = new InterfaceMethodRef("java/util/Set", "iterator", "()Ljava/util/Iterator;");

            addClassSignature(new ConstSignature("missingno"));
            addClassSignature(new ConstSignature(".png"));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        captureReference(GETFIELD),
                        reference(INVOKEINTERFACE, mapEntrySet),
                        reference(INVOKEINTERFACE, setIterator),
                        anyASTORE
                    );
                }
            }
                .setMethod(refreshTextures2)
                .addXref(1, texturesByName)
            );

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        ALOAD_2,
                        captureReference(PUTFIELD)
                    );
                }
            }
                .matchConstructorOnly(true)
                .addXref(1, basePath)
            );

            addMemberMapper(new MethodMapper(registerIcon));
            addMemberMapper(new MethodMapper(refreshTextures1, refreshTextures2));
        }
    }

    /**
     * Maps Icon interface.
     */
    public static class IconMod extends com.prupe.mcpatcher.ClassMod {
        public IconMod(Mod mod) {
            super(mod);

            final InterfaceMethodRef getWidth = new InterfaceMethodRef(getDeobfClass(), "getWidth", "()I");
            final InterfaceMethodRef getHeight = new InterfaceMethodRef(getDeobfClass(), "getHeight", "()I");
            final InterfaceMethodRef getMinU = new InterfaceMethodRef(getDeobfClass(), "getMinU", "()F");
            final InterfaceMethodRef getMaxU = new InterfaceMethodRef(getDeobfClass(), "getMaxU", "()F");
            final InterfaceMethodRef getInterpolatedU = new InterfaceMethodRef(getDeobfClass(), "getInterpolatedU", "(D)F");
            final InterfaceMethodRef getMinV = new InterfaceMethodRef(getDeobfClass(), "getMinV", "()F");
            final InterfaceMethodRef getMaxV = new InterfaceMethodRef(getDeobfClass(), "getMaxV", "()F");
            final InterfaceMethodRef getInterpolatedV = new InterfaceMethodRef(getDeobfClass(), "getInterpolatedV", "(D)F");
            final InterfaceMethodRef getIconName = new InterfaceMethodRef(getDeobfClass(), "getIconName", "()Ljava/lang/String;");

            addClassSignature(new InterfaceSignature(
                getWidth,
                getHeight,
                getMinU,
                getMaxU,
                getInterpolatedU,
                getMinV,
                getMaxV,
                getInterpolatedV,
                getIconName
            ).setInterfaceOnly(true));
        }
    }

    public static class NBTTagCompoundMod extends com.prupe.mcpatcher.ClassMod {
        private final InterfaceMethodRef containsKey = new InterfaceMethodRef("java/util/Map", "containsKey", "(Ljava/lang/Object;)Z");
        private final InterfaceMethodRef mapRemove = new InterfaceMethodRef("java/util/Map", "remove", "(Ljava/lang/Object;)Ljava/lang/Object;");
        protected final FieldRef tagMap = new FieldRef(getDeobfClass(), "tagMap", "Ljava/util/Map;");

        public NBTTagCompoundMod(Mod mod) {
            super(mod);
            setParentClass("NBTBase");

            addClassSignature(new OrSignature(
                new ConstSignature(new ClassRef("java.util.HashMap")),
                new ConstSignature(new ClassRef("java.util.Map"))
            ));
            if (getMinecraftVersion().compareTo("13w36a") >= 0) {
                addClassSignature(new ConstSignature(new InterfaceMethodRef("java/io/DataInput", "readByte", "()B")));
                addClassSignature(new ConstSignature(new InterfaceMethodRef("java/io/DataInput", "readUTF", "()Ljava/lang/String;")));
            } else {
                addClassSignature(new ConstSignature(":["));
                addClassSignature(new ConstSignature(":"));
                addClassSignature(new ConstSignature(","));
                addClassSignature(new ConstSignature("]"));
            }

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        captureReference(GETFIELD),
                        ALOAD_1,
                        reference(INVOKEINTERFACE, containsKey),
                        IRETURN
                    );
                }
            }
                .setMethod(new MethodRef(getDeobfClass(), "hasKey", "(Ljava/lang/String;)Z"))
                .addXref(1, tagMap)
            );

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        captureReference(GETFIELD),
                        ALOAD_1,
                        reference(INVOKEINTERFACE, mapRemove)
                    );
                }
            }
                .setMethod(new MethodRef(getDeobfClass(), "removeTag", "(Ljava/lang/String;)V"))
                .addXref(1, tagMap)
            );

            mapNBTMethod("Byte", "B");
            mapNBTMethod("ByteArray", "[B");
            mapNBTMethod("Double", "D");
            mapNBTMethod("Float", "F");
            mapNBTMethod("IntArray", "[I");
            mapNBTMethod("Integer", "I");
            mapNBTMethod("Long", "J");
            mapNBTMethod("Short", "S");
            mapNBTMethod("String", "Ljava/lang/String;");

            addMemberMapper(new MethodMapper(null, new MethodRef(getDeobfClass(), "getBoolean", "(Ljava/lang/String;)Z")));
            addMemberMapper(new MethodMapper(new MethodRef(getDeobfClass(), "setBoolean", "(Ljava/lang/String;Z)V")));
            addMemberMapper(new MethodMapper(new MethodRef(getDeobfClass(), "getCompoundTag", "(Ljava/lang/String;)L" + getDeobfClass() + ";")));
            addMemberMapper(new MethodMapper(new MethodRef(getDeobfClass(), "getTag", "(Ljava/lang/String;)LNBTBase;")));
        }

        public NBTTagCompoundMod mapGetTagList() {
            addMemberMapper(new MethodMapper(new MethodRef(getDeobfClass(), "getTagList", "(Ljava/lang/String;)LNBTTagList;")));
            return this;
        }

        protected void mapNBTMethod(String type, String desc) {
            final MethodRef get = new MethodRef(getDeobfClass(), "get" + type, "(Ljava/lang/String;)" + desc);
            final MethodRef set = new MethodRef(getDeobfClass(), "set" + type, "(Ljava/lang/String;" + desc + ")V");

            addMemberMapper(new MethodMapper(get));
            addMemberMapper(new MethodMapper(set));
        }
    }

    public static class NBTTagListMod extends com.prupe.mcpatcher.ClassMod {
        public NBTTagListMod(Mod mod) {
            super(mod);
            setParentClass("NBTBase");

            final boolean haveTagAt = getMinecraftVersion().compareTo("13w36a") < 0;

            final FieldRef data = new FieldRef(getDeobfClass(), "data", "Ljava/util/List;");
            final MethodRef tagCount = new MethodRef(getDeobfClass(), "tagCount", "()I");
            final MethodRef removeTag = new MethodRef(getDeobfClass(), "removeTag", "(I)LNBTBase;");
            final MethodRef tagAt = new MethodRef(getDeobfClass(), "tagAt", "(I)LNBTBase;");
            final InterfaceMethodRef listSize = new InterfaceMethodRef("java/util/List", "size", "()I");
            final InterfaceMethodRef listRemove = new InterfaceMethodRef("java/util/List", "remove", "(I)Ljava/lang/Object;");
            final InterfaceMethodRef listGet = new InterfaceMethodRef("java/util/List", "get", "(I)Ljava/lang/Object;");

            if (haveTagAt) {
                addClassSignature(new ConstSignature(" entries of type "));
            } else {
                addClassSignature(new ConstSignature("["));
                addClassSignature(new ConstSignature("]"));
            }

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        reference(INVOKEINTERFACE, listSize)
                    );
                }
            }.setMethod(tagCount));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        reference(INVOKEINTERFACE, listRemove)
                    );
                }
            }.setMethod(removeTag));

            if (haveTagAt) {
                addClassSignature(new BytecodeSignature() {
                    @Override
                    public String getMatchExpression() {
                        return buildExpression(
                            reference(INVOKEINTERFACE, listGet)
                        );
                    }
                }.setMethod(tagAt));
            } else {
                addMemberMapper(new FieldMapper(data));

                addPatch(new AddMethodPatch(tagAt) {
                    @Override
                    public byte[] generateMethod() {
                        return buildCode(
                            ALOAD_0,
                            reference(GETFIELD, data),
                            ILOAD_1,
                            reference(INVOKEINTERFACE, listGet),
                            reference(CHECKCAST, new ClassRef("NBTBase")),
                            ARETURN
                        );
                    }
                }.allowDuplicate(true));
            }
        }
    }

    public static class BiomeGenBaseMod extends com.prupe.mcpatcher.ClassMod {
        protected final FieldRef biomeList = new FieldRef(getDeobfClass(), "biomeList", "[LBiomeGenBase;");
        protected final FieldRef biomeID = new FieldRef(getDeobfClass(), "biomeID", "I");
        protected final FieldRef biomeName = new FieldRef(getDeobfClass(), "biomeName", "Ljava/lang/String;");
        protected final MethodRef setBiomeName = new MethodRef(getDeobfClass(), "setBiomeName", "(Ljava/lang/String;)LBiomeGenBase;");

        public BiomeGenBaseMod(Mod mod) {
            super(mod);

            addClassSignature(new ConstSignature("Ocean"));
            addClassSignature(new ConstSignature("Plains"));
            addClassSignature(new ConstSignature("Desert"));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        ILOAD_1,
                        captureReference(PUTFIELD)
                    );
                }
            }
                .matchConstructorOnly(true)
                .addXref(1, biomeID)
            );

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        begin(),
                        ALOAD_0,
                        ALOAD_1,
                        captureReference(PUTFIELD),
                        ALOAD_0,
                        ARETURN,
                        end()
                    );
                }
            }
                .setMethod(setBiomeName)
                .addXref(1, biomeName)
            );

            addMemberMapper(new FieldMapper(biomeList)
                .accessFlag(AccessFlag.STATIC, true)
            );

        }
    }
}
