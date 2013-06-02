package com.prupe.mcpatcher;

import javassist.bytecode.ClassFile;

public class OrSignature extends ClassSignature {
    private ClassSignature[] signatures;

    public OrSignature(ClassMod classMod, ClassSignature... signatures) {
        super(classMod);
        this.signatures = signatures;
    }

    @Override
    public boolean match(String filename, ClassFile classFile, ClassMap tempClassMap) {
        for (ClassSignature signature : signatures) {
            if (signature.match(filename, classFile, tempClassMap)) {
                return true;
            }
        }
        return false;
    }
}
