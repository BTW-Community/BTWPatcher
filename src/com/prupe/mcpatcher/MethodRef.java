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
    int getTag() {
        return ConstPool.CONST_Methodref;
    }

    @Override
    public boolean checkEqual(ConstPool constPool, int index) {
        return constPool.getMethodrefClassName(index).equals(className) &&
            constPool.getMethodrefName(index).equals(name) &&
            constPool.getMethodrefType(index).equals(type);
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
