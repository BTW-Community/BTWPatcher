package com.prupe.mcpatcher;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base class for all mods.
 *
 * @see #Mod()
 */
public abstract class Mod {
    /**
     * Name of the mod as displayed in the MCPatcher UI
     */
    protected String name = "";
    /**
     * Author of the mod
     */
    protected String author = "";
    /**
     * URL of website or forum thread where mod is described
     */
    protected String website = "";
    /**
     * Mod version number as displayed in the MCPatcher UI
     */
    protected String version = "";
    /**
     * Brief description of the mod as displayed in the MCPatcher UI
     */
    protected String description = "";
    /**
     * Whether mod is checked by default in the UI
     */
    protected boolean defaultEnabled = true;
    /**
     * Optional GUI config screen
     */
    protected ModConfigPanel configPanel = null;

    final List<ClassMod> classMods = new ArrayList<ClassMod>();
    final List<String> filesToAdd = new ArrayList<String>();
    final ClassMap classMap = new ClassMap();
    private final List<String> errors = new ArrayList<String>();
    private boolean enabled;
    boolean internal;
    boolean experimental;
    File customJar;
    final List<Dependency> dependencies = new ArrayList<Dependency>();
    final Map<String, String> filesAdded = new HashMap<String, String>();

    /**
     * Initialize mod.
     * <p/>
     * During initialization, the mod should<br>
     * - Assign values for basic mod information:
     * <pre>
     *     name = "Herobrine";
     *     author = "him@example.com";
     *     description = "Adds Herobrine to the game";
     *     version = "1.6";
     *     website = "http://www.example.com/";
     * </pre>
     * - Add any needed filenames to the filesToReplace or filesToAdd lists.  The mod's openFile
     * method should return a valid InputStream for the files listed here.
     * <pre>
     *     filesToReplace.add("gui/background.png");
     *     filesToAdd.add("HerobrineAI.class");
     * </pre>
     * - Create and add ClassMod objects to the classMods list.  Each ClassMod may in turn have
     * multiple ClassSignatures, ClassPatches.
     * <pre>
     *     class EntityMod extends ClassMod {
     *         ...
     *     }
     *     ...
     *     classMods.add(new EntityMod());
     * </pre>
     * - Specify which directories in minecraft.jar should be considered for patching.
     * By default, everything in vanilla minecraft.jar is considered for patching, including the
     * sound libs.  Most mods will want to restrict themselves to files in the root of the jar,
     * which reduces accidental bytecode matches and helps prevent mods from stepping on each
     * other:
     * <pre>
     *     allowedDirs.clear();
     *     allowedDirs.add("");
     * </pre>
     * <p/>
     * See HDFont.java (simple) and HDTexture.java (much more complex) for full examples.
     */
    public Mod() {
        addDependency(MCPatcherUtils.BASE_MOD);
    }

    public String getName() {
        return name;
    }

    public String getAuthor() {
        return author;
    }

    public String getWebsite() {
        return website;
    }

    public String getVersion() {
        return version;
    }

    public String getDescription() {
        return description;
    }

    public String[] getLoggingCategories() {
        return new String[]{getName()};
    }

    /**
     * Hook for doing any cleanup (closing files, etc.) when a Mod is deleted.
     */
    public void close() {
    }

    /**
     * @return mapping between readable class, method, and field names and obfuscated names
     */
    public ClassMap getClassMap() {
        return classMap;
    }

    /**
     * @return current minecraft version
     */
    public static MinecraftVersion getMinecraftVersion() {
        return (MCPatcher.minecraft == null ? null : MCPatcher.minecraft.getVersion());
    }

    void setRefs() {
        for (ClassMod classMod : getClassMods()) {
            classMod.mod = this;
            classMod.bestMatch = null;
            classMod.bestMatchCount = 0;
            for (ClassSignature classSignature : classMod.classSignatures) {
                classSignature.setClassMod(classMod);
            }
            for (ClassPatch classPatch : classMod.patches) {
                classPatch.setClassMod(classMod);
            }
        }
    }

    void resetCounts() {
        filesAdded.clear();
        for (ClassMod classMod : getClassMods()) {
            for (ClassPatch classPatch : classMod.patches) {
                classPatch.numMatches.clear();
            }
        }
    }

    List<ClassMod> getClassMods() {
        return classMods;
    }

    boolean okToApply() {
        return errors.size() == 0 && getErrors().size() == 0;
    }

    /**
     * Add an error message for display to the user.  Any errors will cause the mod to
     * be greyed out in the UI and the message to be displayed as part of the tooltip.
     *
     * @param error descriptive error message
     */
    public final void addError(String error) {
        errors.add(error);
    }

    /**
     * Remove all patches and files to add leaving only signatures and mappings.  Useful for
     * inheriting another mod's ClassMap without also getting its patches.
     *
     * @return this
     */
    public Mod clearPatches() {
        for (ClassMod classMod : classMods) {
            classMod.patches.clear();
        }
        filesToAdd.clear();
        return this;
    }

    /**
     * Add a class mod.
     *
     * @param classMod class mod
     */
    public void addClassMod(ClassMod classMod) {
        classMod.mod = this;
        classMods.add(classMod);
    }

    /**
     * Add a file to be injected into minecraft.jar.
     *
     * @param filename name of file
     */
    public void addFile(String filename) {
        filesToAdd.add(filename);
    }

    /**
     * Add a class file to be injected into minecraft.jar.  The class map will be applied to reobfuscate the class
     * as it is injected
     *
     * @param className name of class (fully qualified using . notation)
     */
    public void addClassFile(String className) {
        addFile(ClassMap.classNameToFilename(className));
    }

    ArrayList<String> getErrors() {
        ArrayList<String> errors = new ArrayList<String>(this.errors);
        for (ClassMod classMod : classMods) {
            if (!classMod.okToApply()) {
                for (String s : classMod.errors) {
                    errors.add(String.format("%s: %s", classMod.getDeobfClass(), s));
                }
            }
        }
        return errors;
    }

    void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    boolean isEnabled() {
        return enabled;
    }

    void loadOptions() {
        if (configPanel != null) {
            configPanel.load();
        }
    }

    /**
     * Called by MCPatcher for each file in filesToAdd and filesToReplace.  Default implementation
     * gets the class resource.
     *
     * @param name name of file to open
     * @return a valid input stream, or null
     * @throws IOException I/O error
     * @see #filesToAdd
     */
    public InputStream openFile(String name) throws IOException {
        InputStream inputStream = null;
        URL url = getClass().getResource(name);
        if (url != null) {
            url = new URL(url.toString().replaceAll("!(?=.*!)", "%21"));
            inputStream = url.openStream();
        }
        if (inputStream == null) {
            Logger.log(Logger.LOG_MAIN, "WARNING: openStream failed, retrying with getResourceAsStream");
            inputStream = getClass().getResourceAsStream(name);
        }
        if (inputStream == null) {
            Logger.log(Logger.LOG_MAIN, "WARNING: getResourceAsStream failed, retrying with getContextClassLoader");
            inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(name);
        }
        if (inputStream == null) {
            Logger.log(Logger.LOG_MAIN, "WARNING: getContextClassLoader failed, giving up");
        }
        return inputStream;
    }

    /**
     * Indicates that this mod <i>requires</i> another mod to function.  Whenever the specified
     * mod is unchecked in the GUI, this mod will be unchecked too.  Whenever this mod is checked
     * in the GUI, the specified mod will be checked also.  If the specified mod is not available
     * at all, then this mod will be unchecked and greyed out in the GUI.
     * <p/>
     * If Mod A calls <code>addDependency(B)</code>:<br>
     * check A -> check B<br>
     * uncheck B -> uncheck A<br>
     * B unavailable -> A greyed out<br>
     *
     * @param name name of required mod
     */
    public void addDependency(String name) {
        dependencies.add(new Dependency(name, true));
    }

    /**
     * Indicates that this mod <i>conflicts</i> with another mod.  Whenever the specified mod is
     * checked in the GUI, this mod will be unchecked.  Whenever this mod is checked in the GUI,
     * the specified mod will be unchecked.  If the specified mod is not available at all, then
     * there is no effect.
     * <p/>
     * If Mod A calls <code>addConflict(B)</code>:<br>
     * check A -> uncheck B<br>
     * check B -> uncheck A<br>
     * B unavailable -> no effect<br>
     *
     * @param name name of conflicting mod
     */
    public void addConflict(String name) {
        dependencies.add(new Dependency(name, false));
    }

    /**
     * Clear all dependencies and conflicts.
     */
    public void clearDependencies() {
        dependencies.clear();
    }

    class Dependency {
        String name;
        boolean required;

        Dependency(String name, boolean required) {
            this.name = name;
            this.required = required;
        }
    }
}
