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
    int getTag() {
        return ConstPool.CONST_InterfaceMethodref;
    }

    @Override
    boolean checkEqual(ConstPool constPool, int index) {
        return constPool.getInterfaceMethodrefClassName(index).equals(className) &&
            constPool.getInterfaceMethodrefName(index).equals(name) &&
            constPool.getInterfaceMethodrefType(index).equals(type);
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
