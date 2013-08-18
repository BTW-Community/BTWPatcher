package com.prupe.mcpatcher;

import com.google.gson.JsonObject;
import com.prupe.mcpatcher.launcher.version.Library;

import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipFile;

class ModList {
    private final MinecraftVersion version;
    private final Map<String, BuiltInMod> builtInMods = new LinkedHashMap<String, BuiltInMod>();
    private final List<Mod> modsByIndex = new ArrayList<Mod>();
    private final Map<String, Mod> modsByName = new HashMap<String, Mod>();
    private boolean applied;

    private class ModDependencyException extends Exception {
        ModDependencyException(String s) {
            super(s);
        }
    }

    private static class BuiltInMod {
        final String name;
        final Class<? extends Mod> modClass;
        boolean internal;
        boolean experimental;

        BuiltInMod(LegacyVersionList.Mod entry, ClassLoader loader) throws ClassNotFoundException {
            this(entry.name, entry.className, loader);
            setInternal(entry.isInternal());
            setExperimental(entry.isExperimental());
        }

        BuiltInMod(String name, String className, ClassLoader loader) throws ClassNotFoundException {
            this(name, loader.loadClass(className).asSubclass(Mod.class));
        }

        BuiltInMod(String name, Class<? extends Mod> modClass) {
            this.name = name;
            this.modClass = modClass;
        }

        BuiltInMod setInternal(boolean internal) {
            this.internal = internal;
            return this;
        }

        BuiltInMod setExperimental(boolean experimental) {
            this.experimental = experimental;
            return this;
        }
    }

    ModList(MinecraftVersion version) throws Exception {
        this.version = version;
        register(new BuiltInMod(MCPatcherUtils.BASE_MOD, BaseMod.class).setInternal(true));
        boolean found = false;
        if (version.compareTo("13w18a") < 0) {
            LegacyVersionList list = getLegacyVersionList();
            for (LegacyVersionList.Entry entry : list.versions) {
                if (version.compareTo(entry.maxMinecraftVersion) <= 0) {
                    ClassLoader loader = getLegacyClassLoader(entry);
                    for (LegacyVersionList.Mod mod : entry.mods) {
                        register(new BuiltInMod(mod, loader));
                    }
                    found = true;
                    break;
                }
            }
        }
        if (!found) {
            ClassLoader loader = new JarClassLoader("/mods4.jar");
            register(new BuiltInMod(MCPatcherUtils.BASE_TEXTURE_PACK_MOD, "com.prupe.mcpatcher.mod.BaseTexturePackMod", loader).setInternal(true));
            register(new BuiltInMod(MCPatcherUtils.BASE_TILESHEET_MOD, "com.prupe.mcpatcher.mod.BaseTilesheetMod", loader).setInternal(true));
            register(new BuiltInMod(MCPatcherUtils.NBT_MOD, "com.prupe.mcpatcher.mod.NBTMod", loader).setInternal(true));
            register(new BuiltInMod(MCPatcherUtils.EXTENDED_HD, "com.prupe.mcpatcher.mod.ExtendedHD", loader));
            register(new BuiltInMod(MCPatcherUtils.RANDOM_MOBS, "com.prupe.mcpatcher.mod.RandomMobs", loader));
            register(new BuiltInMod(MCPatcherUtils.CUSTOM_COLORS, "com.prupe.mcpatcher.mod.CustomColors", loader));
            register(new BuiltInMod(MCPatcherUtils.CONNECTED_TEXTURES, "com.prupe.mcpatcher.mod.ConnectedTextures", loader));
            register(new BuiltInMod(MCPatcherUtils.BETTER_GLASS, "com.prupe.mcpatcher.mod.BetterGlass", loader));
            register(new BuiltInMod(MCPatcherUtils.BETTER_SKIES, "com.prupe.mcpatcher.mod.BetterSkies", loader));
            register(new BuiltInMod(MCPatcherUtils.CUSTOM_ITEM_TEXTURES, "com.prupe.mcpatcher.mod.CustomItemTextures", loader));
        }
        loadBuiltInMods(true);
    }

    private static LegacyVersionList getLegacyVersionList() throws Exception {
        LegacyVersionList list = null;
        File local = MCPatcherUtils.getMinecraftPath(LegacyVersionList.VERSIONS_JSON);
        if (Util.checkSignature(local, Util.JSON_SIGNATURE)) {
            list = JsonUtils.parseJson(local, LegacyVersionList.class);
        }
        if (list == null) {
            File local1 = new File("../mcpatcher-legacy/" + LegacyVersionList.VERSIONS_JSON);
            if (Util.checkSignature(local1, Util.JSON_SIGNATURE)) {
                list = JsonUtils.parseJson(local, LegacyVersionList.class);
            }
        }
        if (list == null) {
            Util.fetchURL(LegacyVersionList.VERSIONS_URL, local, true, Util.LONG_TIMEOUT, Util.JSON_SIGNATURE);
            list = JsonUtils.parseJson(local, LegacyVersionList.class);
            local.deleteOnExit();
        }
        return list;
    }

    private static ClassLoader getLegacyClassLoader(LegacyVersionList.Entry entry) throws Exception {
        File local;
        if (Util.devDir == null) {
            // run from command line in dev environment
            local = new File("../mcpatcher-legacy/out/artifacts/" + entry.id + entry.getResource());
        } else {
            // run from within IDE
            local = new File(Util.devDir, "../mcpatcher-legacy/out/artifacts/" + entry.id + entry.getResource());
        }
        if (Util.checkSignature(local, Util.JAR_SIGNATURE)) {
            return new URLClassLoader(new URL[]{local.toURI().toURL()});
        }

        Library library = new Library("com.prupe.mcpatcher:mcpatcher-legacy:" + entry.libraryVersion, LegacyVersionList.DEFAULT_BASE_URL);
        local = library.getPath(MCPatcherUtils.getMinecraftPath("libraries"));
        boolean forceRemote = false;
        if (local.isFile() && !MCPatcherUtils.isNullOrEmpty(entry.md5)) {
            String currentMD5 = Util.computeMD5(local);
            if (!entry.md5.equals(currentMD5)) {
                forceRemote = true;
            }
        }
        local.getParentFile().mkdirs();
        Util.fetchURL(entry.getURL(), local, forceRemote, Util.LONG_TIMEOUT, Util.JAR_SIGNATURE);
        return new URLClassLoader(new URL[]{local.toURI().toURL()});
    }

    private void register(BuiltInMod builtInMod) {
        try {
            Method preInit = builtInMod.modClass.getDeclaredMethod("preInitialize");
            preInit.invoke(null);
        } catch (NoSuchMethodException e) {
            // nothing
        } catch (Throwable e) {
            e.printStackTrace();
        }
        builtInMods.put(builtInMod.name, builtInMod);
    }

    void close() {
        for (Mod mod : modsByIndex) {
            mod.close();
        }
    }

    void loadBuiltInMods(boolean internal) {
        for (BuiltInMod builtInMod : builtInMods.values()) {
            if (!modsByName.containsKey(builtInMod.name) && (MCPatcher.experimentalMods || !builtInMod.experimental) && internal == builtInMod.internal) {
                addNoReplace(newModInstance(builtInMod));
            }
        }
    }

    void loadCustomMods(File directory) {
        if (directory.isDirectory()) {
            for (File f : directory.listFiles(new FileFilter() {
                public boolean accept(File pathname) {
                    return pathname.isFile() && pathname.getName().endsWith(".jar");
                }
            })) {
                try {
                    loadCustomModsFromJar(f);
                } catch (Throwable e) {
                    Logger.log(Logger.LOG_JAR, "Error loading mods from %s", f.getPath());
                    Logger.log(e);
                }
            }
        }
    }

    private void loadCustomModsFromJar(File file) throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        Logger.log(Logger.LOG_JAR, "Opening %s", file.getPath());
        final JarFile jar = new JarFile(file);
        URLClassLoader loader = new URLClassLoader(new URL[]{file.toURI().toURL()}, getClass().getClassLoader());
        for (JarEntry entry : Collections.list(jar.entries())) {
            if (!entry.isDirectory() && MinecraftJar.isClassFile(entry.getName())) {
                Mod mod = loadCustomMod(loader, ClassMap.filenameToClassName(entry.getName()));
                if (addNoReplace(mod)) {
                    Logger.log(Logger.LOG_MOD, "new %s()", mod.getClass().getName());
                    mod.customJar = file;
                }
            }
        }
    }

    private Mod loadCustomMod(File file, String className) {
        try {
            URLClassLoader loader = new URLClassLoader(new URL[]{file.toURI().toURL()}, getClass().getClassLoader());
            return loadCustomMod(loader, className);
        } catch (Throwable e) {
            Logger.log(e);
        }
        return null;
    }

    private Mod loadCustomMod(URLClassLoader loader, String className) throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        Class<?> cl = null;
        try {
            cl = loader.loadClass(className);
        } catch (NoClassDefFoundError e) {
            Logger.log(Logger.LOG_MOD, "WARNING: skipping %s: %s", className, e.toString());
        }
        if (cl != null && !cl.isInterface() && Mod.class.isAssignableFrom(cl)) {
            int flags = cl.getModifiers();
            if (!Modifier.isAbstract(flags) && Modifier.isPublic(flags)) {
                return newModInstance(cl.asSubclass(Mod.class));
            }
        }
        return null;
    }

    void loadImpliedMods(ProfileManager profileManager, UserInterface ui) throws Exception {
        Library forgeLibrary = profileManager.getForgeLibrary();
        if (forgeLibrary != null && !hasModSubclass(ForgeAdapter.class)) {
            ForgeAdapter forgeMod = new ForgeAdapter(ui, forgeLibrary);
            forgeMod.setEnabled(true);
            addFirstBuiltin(forgeMod);
        }
    }

    boolean hasModSubclass(Class<? extends Mod> modClass) {
        for (Mod mod : getAll()) {
            if (modClass.isAssignableFrom(mod.getClass())) {
                return true;
            }
        }
        return false;
    }

    List<Mod> getAll() {
        return modsByIndex;
    }

    List<Mod> getVisible() {
        List<Mod> visibleMods = new ArrayList<Mod>();
        for (Mod mod : modsByIndex) {
            if (!mod.internal || MCPatcher.showInternal) {
                visibleMods.add(mod);
            }
        }
        return visibleMods;
    }

    ArrayList<Mod> getSelected() {
        ArrayList<Mod> list = new ArrayList<Mod>();
        for (Mod mod : modsByIndex) {
            if (mod.okToApply() && mod.isEnabled()) {
                list.add(mod);
            }
        }
        return list;
    }

    HashMap<String, String> getReverseMap() {
        HashMap<String, String> map = new HashMap<String, String>();
        for (Mod mod : modsByIndex) {
            for (Map.Entry<String, String> entry : mod.getClassMap().getReverseClassMap().entrySet()) {
                map.put(entry.getKey(), entry.getValue());
            }
        }
        return map;
    }

    Mod get(String name) {
        return modsByName.get(name);
    }

    Mod get(int index) {
        return modsByIndex.get(index);
    }

    int size() {
        return modsByIndex.size();
    }

    void enableValidMods(boolean enableAll) {
        for (int i = modsByIndex.size() - 1; i >= 0; i--) {
            Mod mod = modsByIndex.get(i);
            boolean enabled = mod.okToApply();
            if (enabled) {
                if (enableAll) {
                    selectMod(mod, true);
                }
            } else {
                selectMod(mod, false);
            }
        }
    }

    void disableAll() {
        for (int i = modsByIndex.size() - 1; i >= 0; i--) {
            Mod mod = modsByIndex.get(i);
            selectMod(mod, false);
        }
    }

    boolean isApplied() {
        return applied;
    }

    void setApplied(boolean applied) {
        this.applied = applied;
    }

    void remove(Mod mod) {
        String name = mod.getName();
        for (int i = 0; i < modsByIndex.size(); i++) {
            if (modsByIndex.get(i) == mod) {
                modsByIndex.remove(i);
                modsByName.remove(name);
            }
        }
        mod.close();
    }

    int addFirst(Mod mod) {
        String name = mod.getName();
        Mod oldMod = modsByName.get(name);
        if (oldMod != null) {
            remove(oldMod);
        }
        modsByIndex.add(0, mod);
        modsByName.put(name, mod);
        return indexOfVisible(mod);
    }

    int addFirstBuiltin(Mod mod) {
        String name = mod.getName();
        Mod oldMod = modsByName.get(name);
        if (oldMod != null) {
            remove(oldMod);
        }
        int i;
        for (i = 0; i < modsByIndex.size(); i++) {
            oldMod = modsByIndex.get(i);
            if (!(oldMod instanceof ExternalMod) && indexOfVisible(oldMod) >= 0) {
                break;
            }
        }
        modsByIndex.add(i, mod);
        modsByName.put(name, mod);
        return indexOfVisible(mod);
    }

    int addLast(Mod mod) {
        String name = mod.getName();
        Mod oldMod = modsByName.get(name);
        if (oldMod != null) {
            remove(oldMod);
        }
        modsByIndex.add(mod);
        modsByName.put(name, mod);
        return indexOfVisible(mod);
    }

    int moveUp(int index, boolean toTop) {
        return move(index, -1, toTop);
    }

    int moveDown(int index, boolean toBottom) {
        return move(index, 1, toBottom);
    }

    private int move(int index, int direction, boolean allTheWay) {
        List<Mod> visibleMods = getVisible();
        int newIndex;
        if (!allTheWay) {
            newIndex = index + direction;
        } else if (direction < 0) {
            newIndex = 0;
        } else {
            newIndex = visibleMods.size() - 1;
        }
        if (index >= 0 && index < visibleMods.size() && newIndex >= 0 && newIndex < visibleMods.size() && newIndex != index) {
            List<Mod> mods = visibleMods.subList(Math.min(index, newIndex), Math.max(index, newIndex) + 1);
            for (int i = 0; i < modsByIndex.size(); i++) {
                for (int j = 0; j < mods.size(); j++) {
                    if (modsByIndex.get(i) == mods.get(j)) {
                        modsByIndex.set(i, mods.get((j + direction + mods.size()) % mods.size()));
                        break;
                    }
                }
            }
            index = newIndex;
        }
        return index;
    }

    int replace(Mod oldMod, Mod newMod) {
        int index = indexOf(oldMod);
        if (index >= 0 && oldMod.getName().equals(newMod.getName())) {
            modsByIndex.set(index, newMod);
            modsByName.put(newMod.getName(), newMod);
            oldMod.close();
            return indexOfVisible(newMod);
        } else {
            remove(oldMod);
            return addFirst(newMod);
        }
    }

    private boolean addNoReplace(Mod mod) {
        if (mod == null) {
            return false;
        }
        String name = mod.getName();
        if (modsByName.containsKey(name)) {
            Logger.log(Logger.LOG_MOD, "WARNING: duplicate mod %s ignored", name);
            return false;
        }
        modsByName.put(name, mod);
        modsByIndex.add(mod);
        mod.setEnabled(mod.defaultEnabled);
        mod.loadOptions();
        return true;
    }

    int indexOf(Mod mod) {
        for (int i = 0; i < modsByIndex.size(); i++) {
            if (mod == modsByIndex.get(i)) {
                return i;
            }
        }
        return -1;
    }

    int indexOfVisible(Mod mod) {
        List<Mod> visible = getVisible();
        for (int i = 0; i < visible.size(); i++) {
            if (mod == visible.get(i)) {
                return i;
            }
        }
        return -1;
    }

    void selectMod(Mod mod, boolean enable) {
        HashMap<Mod, Boolean> changes = new HashMap<Mod, Boolean>();
        try {
            if (enable) {
                enableMod(changes, mod, false);
            } else {
                disableMod(changes, mod, false);
            }
        } catch (ModDependencyException e) {
            Logger.log(e);
        }
        for (Map.Entry<Mod, Boolean> entry : changes.entrySet()) {
            mod = entry.getKey();
            mod.setEnabled(entry.getValue());
        }
        refreshInternalMods();
    }

    void refreshInternalMods() {
        outer:
        while (true) {
            for (int i = 0; i < modsByIndex.size() - 1; i++) {
                Mod mod1 = modsByIndex.get(i);
                Mod mod2 = modsByIndex.get(i + 1);
                if (mod1.internal && !mod2.internal && !dependsOn(mod2, mod1)) {
                    modsByIndex.set(i, mod2);
                    modsByIndex.set(i + 1, mod1);
                    continue outer;
                }
            }
            break;
        }
        if (!MCPatcher.showInternal) {
            for (Mod mod : modsByIndex) {
                if (mod.internal) {
                    mod.setEnabled(false);
                }
            }
        }
        HashMap<Mod, Boolean> changes = new HashMap<Mod, Boolean>();
        for (Mod mod : modsByIndex) {
            try {
                if (mod.internal) {
                    // nothing
                } else if (mod.isEnabled()) {
                    enableMod(changes, mod, false);
                } else {
                    disableMod(changes, mod, false);
                }
            } catch (ModDependencyException e) {
                Logger.log(e);
            }
        }
        for (Map.Entry<Mod, Boolean> entry : changes.entrySet()) {
            Mod mod = entry.getKey();
            mod.setEnabled(entry.getValue());
        }
    }

    private boolean dependsOn(Mod mod1, Mod mod2) {
        if (mod1 == null || mod2 == null) {
            return false;
        }
        if (mod1 == mod2) {
            return true;
        }
        for (Mod.Dependency dep : mod1.dependencies) {
            if (dep.required && !dep.name.equals(mod1.getName()) && dependsOn(modsByName.get(dep.name), mod2)) {
                return true;
            }
        }
        return false;
    }

    private void enableMod(HashMap<Mod, Boolean> inst, Mod mod, boolean recursive) throws ModDependencyException {
        if (mod == null) {
            return;
        }
        //Logger.log(Logger.LOG_MOD, "%senabling %s", (recursive ? " " : ""), mod.getName());
        if (!mod.okToApply()) {
            throw new ModDependencyException(mod.getName() + " cannot be applied");
        }
        if (inst.containsKey(mod)) {
            if (!inst.get(mod)) {
                throw new ModDependencyException(mod.getName() + " is both conflicting and required");
            }
            return;
        } else {
            inst.put(mod, true);
        }
        for (Mod.Dependency dep : mod.dependencies) {
            Mod dmod = modsByName.get(dep.name);
            if (dep.required) {
                if (dmod == null) {
                    throw new ModDependencyException("dependent mod " + dep.name + " not available");
                } else {
                    enableMod(inst, dmod, true);
                }
            } else {
                disableMod(inst, dmod, true);
            }
        }
        for (Mod dmod : modsByIndex) {
            if (dmod != mod) {
                for (Mod.Dependency dep : dmod.dependencies) {
                    if (dep.name.equals(mod.getName()) && !dep.required) {
                        disableMod(inst, dmod, true);
                    }
                }
            }
        }
    }

    private void disableMod(HashMap<Mod, Boolean> inst, Mod mod, boolean recursive) throws ModDependencyException {
        if (mod == null) {
            return;
        }
        //Logger.log(Logger.LOG_MOD, "%sdisabling %s", (recursive ? " " : ""), mod.getName());
        if (inst.containsKey(mod)) {
            if (inst.get(mod)) {
                throw new ModDependencyException(mod.getName() + " is both conflicting and required");
            }
            return;
        } else {
            inst.put(mod, false);
        }
        for (Mod dmod : modsByIndex) {
            if (dmod != mod) {
                for (Mod.Dependency dep : dmod.dependencies) {
                    if (dep.name.equals(mod.getName()) && dep.required) {
                        disableMod(inst, dmod, true);
                    }
                }
            }
        }
    }

    void loadSavedMods() {
        Config config = Config.getInstance();
        List<String> invalidEntries = new ArrayList<String>();
        for (Map.Entry<String, Config.ModEntry> entry : config.getSelectedVersion().mods.entrySet()) {
            String name = entry.getKey();
            Config.ModEntry modEntry = entry.getValue();
            String type = modEntry.type;
            boolean enabled = modEntry.enabled;
            Mod mod = null;
            if (name == null || type == null) {
                // nothing
            } else if (type.equals(Config.VAL_BUILTIN)) {
                BuiltInMod builtInMod = builtInMods.get(name);
                if (builtInMod != null && (MCPatcher.experimentalMods || !builtInMod.experimental)) {
                    mod = newModInstance(builtInMod);
                }
            } else if (type.equals(Config.VAL_EXTERNAL_ZIP)) {
                String path = modEntry.path;
                List<Config.FileEntry> files = modEntry.files;
                if (path != null && files != null) {
                    File file = new File(path);
                    if (file.isFile()) {
                        Map<String, String> fileMap = new LinkedHashMap<String, String>();
                        for (Config.FileEntry entry1 : files) {
                            fileMap.put(entry1.to, entry1.from);
                        }
                        try {
                            mod = new ExternalMod(new ZipFile(file), fileMap);
                        } catch (IOException e) {
                            Logger.log(e);
                        }
                    }
                }
            } else if (type.equals(Config.VAL_EXTERNAL_JAR)) {
                String path = modEntry.path;
                String className = modEntry.className;
                if (path != null && className != null) {
                    File file = new File(path);
                    if (file.exists()) {
                        mod = loadCustomMod(file, className);
                        if (mod != null) {
                            mod.customJar = file;
                        }
                    }
                }
            }
            if (mod == null) {
                invalidEntries.add(name);
            } else {
                addNoReplace(mod);
                mod.setEnabled(enabled);
            }
        }

        for (String name : invalidEntries) {
            config.getSelectedVersion().mods.remove(name);
        }
        refreshInternalMods();
    }

    private void updateModElement(Mod mod, Config.ModEntry modEntry) {
        modEntry.enabled = mod.okToApply() && mod.isEnabled();
        if (mod instanceof ExternalMod) {
            ExternalMod extmod = (ExternalMod) mod;
            modEntry.type = Config.VAL_EXTERNAL_ZIP;
            modEntry.path = extmod.zipFile.getName();
            modEntry.className = null;
            List<Config.FileEntry> files = new ArrayList<Config.FileEntry>();
            files.clear();
            for (Map.Entry<String, String> entry : extmod.fileMap.entrySet()) {
                files.add(new Config.FileEntry(entry.getValue(), entry.getKey()));
            }
            modEntry.files = files;
        } else if (mod.customJar == null) {
            modEntry.type = Config.VAL_BUILTIN;
            modEntry.path = null;
            modEntry.className = null;
            modEntry.files = null;
        } else {
            modEntry.type = Config.VAL_EXTERNAL_JAR;
            modEntry.path = mod.customJar.getPath();
            modEntry.className = mod.getClass().getCanonicalName();
            modEntry.files = null;
        }
    }

    void updateProperties() {
        Config.VersionEntry versionEntry = Config.getInstance().getSelectedVersion();
        Map<String, Config.ModEntry> oldEntries = new HashMap<String, Config.ModEntry>();
        oldEntries.putAll(versionEntry.mods);
        versionEntry.mods.clear();
        for (Mod mod : modsByIndex) {
            if (mod.internal) {
                continue;
            }
            Config.ModEntry modEntry = oldEntries.get(mod.getName());
            if (modEntry == null) {
                modEntry = new Config.ModEntry();
            }
            updateModElement(mod, modEntry);
            versionEntry.mods.put(mod.getName(), modEntry);
        }
    }

    private Mod newModInstance(Class<? extends Mod> modClass) {
        Mod mod;
        try {
            mod = modClass.getDeclaredConstructor().newInstance();
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
        try {
            Method supportsVersion = modClass.getDeclaredMethod("supportsVersion", MinecraftVersion.class);
            if (supportsVersion.getReturnType() == Boolean.TYPE && Modifier.isStatic(supportsVersion.getModifiers())) {
                boolean b = (Boolean) supportsVersion.invoke(null, version);
                if (!b) {
                    return null;
                }
            }
        } catch (NoSuchMethodException e) {
            // nothing
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
        return mod;
    }

    private Mod newModInstance(BuiltInMod builtInMod) {
        Mod mod = newModInstance(builtInMod.modClass);
        if (mod != null) {
            mod.internal = builtInMod.internal;
            mod.experimental = builtInMod.experimental;
        }
        return mod;
    }

    JsonObject getOverrideVersionJson() {
        JsonObject json = null;
        for (Mod mod : getSelected()) {
            JsonObject json1 = mod.getOverrideVersionJson();
            if (json1 != null) {
                json = json1;
            }
        }
        return json;
    }

    String getExtraJavaArguments() {
        List<String> cmdLine = new ArrayList<String>();
        for (Mod mod : getSelected()) {
            mod.addExtraJavaArguments(cmdLine);
        }
        StringBuilder sb = new StringBuilder();
        for (String s : cmdLine) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(s);
        }
        return sb.toString();
    }

    List<Library> getExtraLibraries() {
        List<Library> list = new ArrayList<Library>();
        for (Mod mod : getSelected()) {
            mod.addExtraLibraries(list);
        }
        return list;
    }
}
