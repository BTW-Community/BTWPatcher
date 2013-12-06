package com.prupe.mcpatcher;

import javassist.bytecode.ConstPool;

/**
 * Reference to a class.
 *
 * @see JavaRef
 */
public class ClassRef extends JavaRef {
    /**
     * @param className name of class
     */
    public ClassRef(String className) {
        super(className, null, null);
    }

    @Override
    int getTag() {
        return ConstPool.CONST_Class;
    }

    @Override
    boolean checkEqual(ConstPool constPool, int index) {
        return constPool.getClassInfo(index).equals(className);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof ClassRef)) {
            return false;
        }
        ClassRef that = (ClassRef) o;
        return this.getClassName().equals(that.getClassName());
    }
}
