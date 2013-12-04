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
    boolean checkEqual(ConstPool constPool, int tag) {
        return constPool.getTag(tag) == ConstPool.CONST_Fieldref &&
            constPool.getFieldrefClassName(tag).equals(className) &&
            constPool.getFieldrefName(tag).equals(name) &&
            constPool.getFieldrefType(tag).equals(type);
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
