package com.prupe.mcpatcher;

import javassist.bytecode.ClassFile;

import java.util.*;

public class AncestorClassSignature extends ClassSignature {
    private final String baseClass;
    private String baseClassObf;
    private Map<String, SubclassInfo> subclassMap = new HashMap<String, SubclassInfo>();

    public AncestorClassSignature(ClassMod classMod, String baseClass) {
        super(classMod);
        this.baseClass = baseClass;
    }

    @Override
    public boolean match(String filename, ClassFile classFile, ClassMap tempClassMap) {
        String name = classFile.getName();
        SubclassInfo entry = subclassMap.get(name);
        if (entry == null) {
            entry = new SubclassInfo(classFile);
            subclassMap.put(name, entry);
        }
        return true;
    }

    @Override
    public boolean confirmMatch(String className) {
        if (baseClassObf == null) {
            baseClassObf = getClassMap().map(baseClass);
        }
        return isInstanceOf(className, baseClassObf);
    }

    private boolean isInstanceOf(String child, String base) {
        if (base == null) {
            return false;
        }
        if (child.equals(base)) {
            return true;
        }
        SubclassInfo entry = subclassMap.get(child);
        if (entry != null) {
            if (isInstanceOf(entry.parent, base)) {
                return true;
            }
            for (String i : entry.interfaces) {
                if (isInstanceOf(i, base)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static class SubclassInfo {
        final String parent;
        final Set<String> interfaces = new HashSet<String>();

        SubclassInfo(ClassFile classFile) {
            this.parent = classFile.getSuperclass();
            interfaces.addAll(Arrays.asList(classFile.getInterfaces()));
        }
    }
}
