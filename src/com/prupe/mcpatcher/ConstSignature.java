package com.prupe.mcpatcher;

import javassist.bytecode.ClassFile;

/**
 * ClassSignature that matches if the class's constant pool contains the given value or reference.
 */
public class ConstSignature extends ClassSignature {
    private final Object value;
    private final int tag;

    /**
     * Constructor
     *
     * @param value can be a constant (float, double, String) or JavaRef
     */
    public ConstSignature(ClassMod classMod, Object value) {
        super(classMod);
        this.value = value;
        tag = ConstPoolUtils.getTag(value);
    }

    @Override
    public boolean match(String filename, ClassFile classFile, ClassMap tempClassMap) {
        return ConstPoolUtils.find(classFile.getConstPool(), value, tag) >= 0;
    }
}
