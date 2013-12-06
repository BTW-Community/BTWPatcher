package com.prupe.mcpatcher;

import javassist.bytecode.ConstPool;

/**
 * Reference to a field within a class.
 *
 * @see JavaRef
 */
public class FieldRef extends JavaRef {
    public FieldRef(String className, String name, String type) {
        super(className, name, type);
    }

    @Override
    int getTag() {
        return ConstPool.CONST_Fieldref;
    }

    @Override
    boolean checkEqual(ConstPool constPool, int index) {
        return constPool.getFieldrefClassName(index).equals(className) &&
            constPool.getFieldrefName(index).equals(name) &&
            constPool.getFieldrefType(index).equals(type);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof FieldRef)) {
            return false;
        }
        FieldRef that = (FieldRef) o;
        return this.getClassName().equals(that.getClassName()) &&
            this.getName().equals(that.getName()) &&
            this.getType().equals(that.getType());
    }
}
