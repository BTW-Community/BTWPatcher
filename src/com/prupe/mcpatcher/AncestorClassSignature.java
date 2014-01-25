package com.prupe.mcpatcher;

import javassist.bytecode.ClassFile;

public class AncestorClassSignature extends ClassSignature {
    private final String baseClass;

    public AncestorClassSignature(ClassMod classMod, String baseClass) {
        super(classMod);
        this.baseClass = baseClass;
    }

    @Override
    public boolean match(String filename, ClassFile classFile, ClassMap tempClassMap) {
        return true;
    }

    @Override
    public boolean confirmMatch(String className) {
        if (classMod.mod.isInstanceOf(className, baseClass)) {
            getClassMap().addInheritance(baseClass, className, true);
            return true;
        } else {
            return false;
        }
    }
}
