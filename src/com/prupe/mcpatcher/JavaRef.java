package com.prupe.mcpatcher;

import javassist.bytecode.ConstPool;

import java.util.ArrayList;

/**
 * Base class for a reference to a class, method, or field.  Used to generate bytecode for
 * referencing classes and members.
 *
 * @see ClassMap#map(JavaRef)
 * @see ClassMod#reference(int, JavaRef)
 */
abstract public class JavaRef {
    protected final String className;
    protected final String name;
    protected final String type;
    protected ArrayList<String> parsedDescriptor;
    private final int hashCode;

    public JavaRef(String className, String name, String type) {
        this.className = className.replace('/', '.');
        this.name = name;
        this.type = type;
        hashCode = this.className.hashCode() +
            (name == null ? 0 : name.hashCode()) +
            (type == null ? 0 : type.hashCode());
    }

    public String getClassName() {
        return className;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public ArrayList<String> getParsedDescriptor() {
        if (parsedDescriptor == null && type != null) {
            parsedDescriptor = ConstPoolUtils.parseDescriptor(type);
        }
        return parsedDescriptor;
    }

    abstract int getTag();

    abstract boolean checkEqual(ConstPool constPool, int index);

    @Override
    public String toString() {
        return String.format("%s{className='%s', name='%s', type='%s'}", getClass().getName(), className, name, type);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }
}
