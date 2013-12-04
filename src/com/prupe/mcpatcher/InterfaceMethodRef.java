package com.prupe.mcpatcher;

import javassist.bytecode.ConstPool;

/**
 * Reference to an interface method.
 *
 * @see JavaRef
 */
public class InterfaceMethodRef extends JavaRef {
    public InterfaceMethodRef(String className, String name, String type) {
        super(className, name, type);
    }

    @Override
    boolean checkEqual(ConstPool constPool, int tag) {
        return constPool.getTag(tag) == ConstPool.CONST_InterfaceMethodref &&
            constPool.getInterfaceMethodrefClassName(tag).equals(className) &&
            constPool.getInterfaceMethodrefName(tag).equals(name) &&
            constPool.getInterfaceMethodrefType(tag).equals(type);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof InterfaceMethodRef)) {
            return false;
        }
        InterfaceMethodRef that = (InterfaceMethodRef) o;
        return this.getClassName().equals(that.getClassName()) &&
            this.getName().equals(that.getName()) &&
            this.getType().equals(that.getType());
    }
}
