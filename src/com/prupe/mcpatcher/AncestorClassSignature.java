package com.prupe.mcpatcher;

import javassist.bytecode.ClassFile;

import java.util.*;

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
        return classMod.mod.isInstanceOf(className, baseClass);
    }
}
