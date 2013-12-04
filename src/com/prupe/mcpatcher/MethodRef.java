package com.prupe.mcpatcher;

import javassist.bytecode.ConstPool;

/**
 * Reference to a class method.
 */
public class MethodRef extends JavaRef {
    public MethodRef(String className, String name, String type) {
        super(className, name, type);
    }

    @Override
    public boolean checkEqual(ConstPool constPool, int tag) {
        return constPool.getTag(tag) == ConstPool.CONST_Methodref &&
            constPool.getMethodrefClassName(tag).equals(className) &&
            constPool.getMethodrefName(tag).equals(name) &&
            constPool.getMethodrefType(tag).equals(type);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof MethodRef)) {
            return false;
        }
        MethodRef that = (MethodRef) o;
        return this.getClassName().equals(that.getClassName()) &&
            this.getName().equals(that.getName()) &&
            this.getType().equals(that.getType());
    }
}
