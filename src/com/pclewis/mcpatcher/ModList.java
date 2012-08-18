package com.pclewis.mcpatcher;

import com.pclewis.mcpatcher.mod.*;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipFile;

class ModList {
    private Vector<Mod> modsByIndex = new Vector<Mod>();
    private HashMap<String, Mod> modsByName = new HashMap<String, Mod>();
    private boolean applied;

    private static BuiltInMod[] builtInMods = new BuiltInMod[]{
        new BuiltInMod(MCPatcherUtils.HD_TEXTURES, HDTexture.class, false),
        new BuiltInMod(MCPatcherUtils.HD_FONT, HDFont.class, false),
        new BuiltInMod(MCPatcherUtils.BETTER_GRASS, BetterGrass.class, false),
        new BuiltInMod(MCPatcherUtils.RANDOM_MOBS, RandomMobs.class, false),
        new BuiltInMod(MCPatcherUtils.CUSTOM_COLORS, CustomColors.class, false),
        new BuiltInMod(MCPatcherUtils.CONNECTED_TEXTURES, ConnectedTextures.class, false),
        new BuiltInMod(MCPatcherUtils.BETTER_SKIES, BetterSkies.class, false),
        new BuiltInMod(MCPatcherUtils.BETTER_GLASS, BetterGlass.class, true),
        new BuiltInMod(MCPatcherUtils.GLSL_SHADERS, GLSLShader.class, true),
    };

    Mod baseMod;
    Mod texturePackMod;

    ModList() {
        MinecraftVersion version = MCPatcher.minecraft.getVersion();
        baseMod = new BaseMod(version);
        baseMod.internal = true;
        texturePackMod = new BaseTexturePackMod(version);
        texturePackMod.internal = true;
        addNoReplace(baseMod);
        addNoReplace(texturePackMod);
    }

    void close() {
        for (Mod mod : modsByIndex) {
            mod.close();
        }
    }

    void loadBuiltInMods() {
        for (BuiltInMod builtInMod : builtInMods) {
            if (!modsByName.containsKey(builtInMod.name) && (MCPatcher.experimentalMods || !builtInMod.experimental)) {
                addNoReplace(newModInstance(builtInMod.modClass));
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

    Vector<Mod> getAll() {
        return modsByIndex;
    }

    Vector<Mod> getVisible() {
        Vector<Mod> visibleMods = new Vector<Mod>();
        for (Mod mod : modsByIndex) {
            if (!mod.internal) {
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
        mod.setRefs();
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
        mod.setRefs();
        return indexOfVisible(mod);
    }

    int moveUp(int index, boolean toTop) {
        return move(index, -1, toTop);
    }

    int moveDown(int index, boolean toBottom) {
        return move(index, 1, toBottom);
    }

    private int move(int index, int direction, boolean allTheWay) {
        Vector<Mod> visibleMods = getVisible();
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
        mod.setRefs();
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
        Vector<Mod> visible = getVisible();
        for (int i = 0; i < visible.size(); i++) {
            if (mod == visible.get(i)) {
                return i;
            }
        }
        return -1;
    }

    private class ModDependencyException extends Exception {
        ModDependencyException(String s) {
            super(s);
        }
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

    private void refreshInternalMods() {
        modsByIndex.remove(baseMod);
        modsByIndex.remove(texturePackMod);
        modsByIndex.add(0, baseMod);
        modsByIndex.add(1, texturePackMod);
        outer:
        while (true) {
            for (int i = 0; i < modsByIndex.size() - 1; i++) {
                Mod mod1 = modsByIndex.get(i);
                Mod mod2 = modsByIndex.get(i + 1);
                if (mod1.internal && !dependsOn(mod2, mod1)) {
                    modsByIndex.set(i, mod2);
                    modsByIndex.set(i + 1, mod1);
                    continue outer;
                }
            }
            break;
        }
        for (Mod mod : modsByIndex) {
            if (mod.internal) {
                mod.setEnabled(false);
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
        Config config = MCPatcherUtils.config;
        Element mods = config.getMods();
        if (mods == null) {
            return;
        }
        NodeList list = mods.getElementsByTagName(Config.TAG_MOD);
        ArrayList<Element> invalidEntries = new ArrayList<Element>();
        for (int i = 0; i < list.getLength(); i++) {
            Element element = (Element) list.item(i);
            String name = config.getText(element, Config.TAG_NAME);
            String type = config.getText(element, Config.TAG_TYPE);
            String enabled = config.getText(element, Config.TAG_ENABLED);
            Mod mod = null;
            if (name == null || type == null) {
                invalidEntries.add(element);
            } else if (type.equals(Config.VAL_BUILTIN)) {
                for (BuiltInMod builtInMod : builtInMods) {
                    if (name.equals(builtInMod.name) && (MCPatcher.experimentalMods || !builtInMod.experimental)) {
                        mod = newModInstance(builtInMod.modClass);
                    }
                }
                if (mod == null) {
                    invalidEntries.add(element);
                }
            } else if (type.equals(Config.VAL_EXTERNAL_ZIP)) {
                String path = config.getText(element, Config.TAG_PATH);
                Element files = config.getElement(element, Config.TAG_FILES);
                if (path != null && files != null) {
                    File file = new File(path);
                    if (file.exists()) {
                        HashMap<String, String> fileMap = new HashMap<String, String>();
                        NodeList fileNodes = files.getElementsByTagName(Config.TAG_FILE);
                        for (int j = 0; j < fileNodes.getLength(); j++) {
                            Element fileElem = (Element) fileNodes.item(j);
                            String from = config.getText(fileElem, Config.TAG_FROM);
                            String to = config.getText(fileElem, Config.TAG_TO);
                            if (from != null && to != null) {
                                fileMap.put(to, from);
                            }
                        }
                        try {
                            mod = new ExternalMod(new ZipFile(file), fileMap);
                        } catch (IOException e) {
                            Logger.log(e);
                        }
                    }
                } else {
                    invalidEntries.add(element);
                }
            } else if (type.equals(Config.VAL_EXTERNAL_JAR)) {
                String path = config.getText(element, Config.TAG_PATH);
                String className = config.getText(element, Config.TAG_CLASS);
                if (path != null && className != null) {
                    File file = new File(path);
                    if (file.exists()) {
                        mod = loadCustomMod(file, className);
                        if (mod != null) {
                            mod.customJar = file;
                        }
                    }
                } else {
                    invalidEntries.add(element);
                }
            } else {
                invalidEntries.add(element);
            }
            if (mod != null) {
                if (addNoReplace(mod)) {
                    if (enabled != null) {
                        mod.setEnabled(Boolean.parseBoolean(enabled));
                    }
                }
            }
        }
        for (Element element : invalidEntries) {
            mods.removeChild(element);
        }
        refreshInternalMods();
    }

    private void updateModElement(Mod mod, Element element) {
        Config config = MCPatcherUtils.config;
        if (mod instanceof ExternalMod) {
            ExternalMod extmod = (ExternalMod) mod;
            config.setText(element, Config.TAG_TYPE, Config.VAL_EXTERNAL_ZIP);
            config.setText(element, Config.TAG_PATH, extmod.zipFile.getName());
            Element files = config.getElement(element, Config.TAG_FILES);
            while (files.hasChildNodes()) {
                files.removeChild(files.getFirstChild());
            }
            for (Map.Entry<String, String> entry : extmod.fileMap.entrySet()) {
                Element fileElem = config.xml.createElement(Config.TAG_FILE);
                Element pathElem = config.xml.createElement(Config.TAG_FROM);
                pathElem.appendChild(config.xml.createTextNode(entry.getValue()));
                fileElem.appendChild(pathElem);
                pathElem = config.xml.createElement(Config.TAG_TO);
                pathElem.appendChild(config.xml.createTextNode(entry.getKey()));
                fileElem.appendChild(pathElem);
                files.appendChild(fileElem);
            }
        } else if (mod.customJar == null) {
            config.setText(element, Config.TAG_TYPE, Config.VAL_BUILTIN);
        } else {
            config.setText(element, Config.TAG_TYPE, Config.VAL_EXTERNAL_JAR);
            config.setText(element, Config.TAG_PATH, mod.customJar.getPath());
            config.setText(element, Config.TAG_CLASS, mod.getClass().getCanonicalName());
        }
    }

    private Element defaultModElement(Mod mod) {
        Config config = MCPatcherUtils.config;
        Element mods = config.getMods();
        if (mods == null) {
            return null;
        }
        Element element = config.getMod(mod.getName());
        config.setText(element, Config.TAG_ENABLED, Boolean.toString(mod.defaultEnabled));
        updateModElement(mod, element);
        return element;
    }

    void updateProperties() {
        Config config = MCPatcherUtils.config;
        Element mods = config.getMods();
        if (mods == null) {
            return;
        }
        HashMap<String, Element> oldElements = new HashMap<String, Element>();
        while (mods.hasChildNodes()) {
            Node node = mods.getFirstChild();
            if (node instanceof Element) {
                Element element = (Element) node;
                String name = config.getText(element, Config.TAG_NAME);
                if (name != null) {
                    oldElements.put(name, element);
                }
            }
            mods.removeChild(node);
        }
        for (Mod mod : modsByIndex) {
            if (mod.internal) {
                continue;
            }
            Element element = oldElements.get(mod.getName());
            if (element == null) {
                defaultModElement(mod);
            } else {
                config.setText(element, Config.TAG_ENABLED, Boolean.toString(mod.isEnabled() && mod.okToApply()));
                updateModElement(mod, element);
                mods.appendChild(element);
                oldElements.remove(mod.getName());
            }
        }
    }

    static boolean isExperimental(String name) {
        for (BuiltInMod builtInMod : builtInMods) {
            if (builtInMod.name.equals(name)) {
                return builtInMod.experimental;
            }
        }
        return false;
    }

    static Mod newModInstance(Class<? extends Mod> modClass) {
        Mod mod = null;
        try {
            mod = modClass.getConstructor(MinecraftVersion.class).newInstance(MCPatcher.minecraft.getVersion());
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            try {
                mod = modClass.newInstance();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
        return mod;
    }

    private static class BuiltInMod {
        String name;
        Class<? extends Mod> modClass;
        boolean experimental;

        BuiltInMod(String name, Class<? extends Mod> modClass, boolean experimental) {
            this.name = name;
            this.modClass = modClass;
            this.experimental = experimental;
        }
    }
}
