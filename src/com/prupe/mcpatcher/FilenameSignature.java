package com.prupe.mcpatcher;

import javassist.bytecode.ClassFile;

/**
 * ClassSignature that matches by filename.
 */
public class FilenameSignature extends ClassSignature {
    protected String filename;

    public FilenameSignature(ClassMod classMod, String filename) {
        super(classMod);
        this.filename = filename;
    }

    @Override
    public boolean match(String filename, ClassFile classFile, ClassMap tempClassMap) {
        return filename.equals(this.filename);
    }
}
